const DEFAULT_PORT = "8888";
const FIND_PHONE_STEPS = [
  { method: "POST", path: "/command/ring/start", label: "响铃" },
  { method: "POST", path: "/command/flash/strobe/start", label: "闪光" }
];

const COMMANDS = {
  status: { method: "GET", path: "/status", label: "检查状态" },
  "find-phone": { method: "SEQUENCE", label: "一键找手机", success: "已开始一键找手机：响铃 + 闪光" },
  "ring-stop": { method: "POST", path: "/command/ring/stop", label: "停止响铃", success: "已停止响铃" },
  "flash-start": { method: "POST", path: "/command/flash/strobe/start", label: "开始闪光", success: "已开始闪光" },
  "flash-stop": { method: "POST", path: "/command/flash/stop", label: "停止闪光", success: "已停止闪光" },
  "stop-all": { method: "POST", path: "/command/stop-all", label: "停止全部", success: "已停止全部" }
};

const NETWORK_ERROR_MESSAGE = "无法连接手机服务。请检查手机和电脑是否在同一 Wi-Fi、手机服务是否启动、IP/端口是否正确。";

const hostInput = document.getElementById("host");
const portInput = document.getElementById("port");
const tokenInput = document.getElementById("token");
const rememberTokenInput = document.getElementById("remember-token");
const clearSavedTokenButton = document.getElementById("clear-saved-token");
const resultOutput = document.getElementById("result");
const endpointPreview = document.getElementById("endpoint-preview");
const deviceHost = document.getElementById("device-host");
const devicePort = document.getElementById("device-port");
const deviceTokenStatus = document.getElementById("device-token-status");
const lastSuccess = document.getElementById("last-success");
const buttons = Array.from(document.querySelectorAll("[data-command]"));

let lastSuccessAt = "";

document.addEventListener("DOMContentLoaded", init);

function init() {
  chrome.storage.local.get(
    { host: "", port: DEFAULT_PORT, rememberToken: false, savedToken: "", lastSuccessAt: "" },
    ({ host, port, rememberToken, savedToken, lastSuccessAt: savedLastSuccessAt }) => {
      hostInput.value = host || "";
      portInput.value = String(port || DEFAULT_PORT);
      rememberTokenInput.checked = rememberToken === true;
      tokenInput.value = rememberToken === true ? savedToken || "" : "";
      lastSuccessAt = savedLastSuccessAt || "";
      updateEndpointPreview();
      updateDeviceCard();
    }
  );

  hostInput.addEventListener("input", handleConnectionInput);
  hostInput.addEventListener("blur", normalizeConnectionFields);
  portInput.addEventListener("input", handleConnectionInput);
  tokenInput.addEventListener("input", saveTokenIfRemembered);
  rememberTokenInput.addEventListener("change", handleRememberTokenChange);
  clearSavedTokenButton.addEventListener("click", clearSavedToken);

  buttons.forEach((button) => {
    button.addEventListener("click", () => handleButtonClick(button.dataset.command));
  });
}

function handleConnectionInput() {
  syncPortFromHost();
  updateEndpointPreview();
  saveConnectionSettings();
  updateDeviceCard();
}

function saveConnectionSettings() {
  const connection = parseConnectionInput();

  chrome.storage.local.set({
    host: connection.host,
    port: connection.port
  });
}

function normalizeConnectionFields() {
  const connection = parseConnectionInput();
  hostInput.value = connection.host;
  portInput.value = connection.port;
  updateEndpointPreview();
  saveConnectionSettings();
  updateDeviceCard();
}

function handleRememberTokenChange() {
  if (rememberTokenInput.checked) {
    chrome.storage.local.set({
      rememberToken: true,
      savedToken: tokenInput.value
    });
    updateDeviceCard();
    return;
  }

  chrome.storage.local.set({ rememberToken: false }, () => {
    chrome.storage.local.remove("savedToken", updateDeviceCard);
  });
}

function saveTokenIfRemembered() {
  if (!rememberTokenInput.checked) {
    return;
  }

  chrome.storage.local.set({
    rememberToken: true,
    savedToken: tokenInput.value
  });
  updateDeviceCard();
}

function clearSavedToken() {
  tokenInput.value = "";
  rememberTokenInput.checked = false;
  chrome.storage.local.set({ rememberToken: false }, () => {
    chrome.storage.local.remove("savedToken", () => {
      updateDeviceCard();
      showResult("已清除已保存 Token", false);
    });
  });
}

async function handleButtonClick(commandName) {
  if (commandName === "diagnostics") {
    openDiagnosticsPage();
    return;
  }

  const command = COMMANDS[commandName];
  if (!command) {
    showResult("未知命令", true);
    return;
  }

  try {
    setBusy(true);
    showResult(`${command.label}中...`, false);

    if (command.method === "SEQUENCE") {
      await runFindPhoneSequence();
      saveLastSuccessAt();
      showResult(command.success, false);
    } else {
      const { body } = await sendCheckedRequest(command);
      if (command.method === "GET") {
        showResult(body ? formatStatus(body) : "状态检查成功", false);
      } else {
        saveLastSuccessAt();
        showResult(command.success || `${command.label}成功`, false);
      }
    }
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  } finally {
    setBusy(false);
  }
}

async function runFindPhoneSequence() {
  validateCommandInputs({ method: "POST" });

  try {
    await sendCheckedRequest(FIND_PHONE_STEPS[0]);
  } catch (error) {
    throw new Error(`响铃启动失败：${getFriendlyErrorMessage(error)}`);
  }

  try {
    await sendCheckedRequest(FIND_PHONE_STEPS[1]);
  } catch (error) {
    throw new Error(`响铃已开始，但闪光启动失败：${getFriendlyErrorMessage(error)}`);
  }
}

async function sendCheckedRequest(command) {
  const response = await sendRequest(command);
  const bodyText = await response.text();
  const body = parseJson(bodyText);

  if (!response.ok) {
    throw new Error(getHttpErrorMessage(response, body, bodyText));
  }

  return { response, body, bodyText };
}

async function sendRequest(command) {
  const request = {
    method: command.method,
    cache: "no-store"
  };

  validateCommandInputs(command);

  if (command.method === "POST") {
    const token = tokenInput.value;
    request.headers = {
      "X-LocalFind-Token": token
    };
  }

  const url = `${getBaseUrl()}${command.path}`;

  try {
    return await fetch(url, request);
  } catch {
    throw new Error(NETWORK_ERROR_MESSAGE);
  }
}

function openDiagnosticsPage() {
  try {
    window.open(`${getBaseUrl()}/`, "_blank", "noopener");
    showResult("已打开诊断页", false);
  } catch (error) {
    showResult(error.message || "无法打开诊断页", true);
  }
}

function getBaseUrl() {
  const { host, port } = parseConnectionInput();

  if (!host) {
    throw new Error("请输入 host");
  }

  if (!isValidPort(port)) {
    throw new Error("请输入有效端口");
  }

  return `http://${host}:${port}`;
}

function validateCommandInputs(command) {
  const { host, port } = parseConnectionInput();
  const missing = [];

  if (!host) {
    missing.push("host");
  }

  if (!isValidPort(port)) {
    missing.push("port");
  }

  if (command.method === "POST" && !tokenInput.value) {
    missing.push("token");
  }

  if (missing.length > 0) {
    throw new Error(`请补全 ${missing.join("、")}`);
  }
}

function parseConnectionInput() {
  const parsed = parseHostValue(hostInput.value);
  const port = parsed.port || getPort();

  return {
    host: parsed.host,
    port
  };
}

function parseHostValue(value) {
  const raw = value.trim();
  if (!raw) {
    return { host: "", port: "" };
  }

  const withScheme = /^https?:\/\//i.test(raw) ? raw : `http://${raw}`;

  try {
    const url = new URL(withScheme);
    return {
      host: url.hostname,
      port: url.port
    };
  } catch {
    const withoutScheme = raw.replace(/^https?:\/\//i, "");
    const withoutPath = withoutScheme.replace(/\/.*$/, "");
    const match = withoutPath.match(/^(.+):(\d+)$/);
    return {
      host: match ? match[1] : withoutPath,
      port: match ? match[2] : ""
    };
  }
}

function syncPortFromHost() {
  const parsed = parseHostValue(hostInput.value);
  if (parsed.port && isValidPort(parsed.port)) {
    portInput.value = parsed.port;
  }
}

function getPort() {
  return String(portInput.value || DEFAULT_PORT).trim();
}

function isValidPort(port) {
  const numericPort = Number(port);
  return Number.isInteger(numericPort) && numericPort >= 1 && numericPort <= 65535;
}

function updateEndpointPreview() {
  const { host, port } = parseConnectionInput();
  const displayHost = host || "HOST";
  const displayPort = isValidPort(port) ? port : DEFAULT_PORT;
  endpointPreview.textContent = `http://${displayHost}:${displayPort}`;
}

function updateDeviceCard() {
  const { host, port } = parseConnectionInput();
  deviceHost.textContent = host || "未设置";
  devicePort.textContent = isValidPort(port) ? port : DEFAULT_PORT;
  deviceTokenStatus.textContent = rememberTokenInput.checked && tokenInput.value ? "已保存" : "未保存";
  lastSuccess.textContent = `上次成功：${lastSuccessAt ? formatDateTime(lastSuccessAt) : "暂无"}`;
}

function saveLastSuccessAt() {
  lastSuccessAt = new Date().toISOString();
  chrome.storage.local.set({ lastSuccessAt });
  updateDeviceCard();
}

function formatDateTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "暂无";
  }

  const year = date.getFullYear();
  const month = padDatePart(date.getMonth() + 1);
  const day = padDatePart(date.getDate());
  const hours = padDatePart(date.getHours());
  const minutes = padDatePart(date.getMinutes());
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function padDatePart(value) {
  return String(value).padStart(2, "0");
}

function setBusy(isBusy) {
  buttons.forEach((button) => {
    button.disabled = isBusy;
  });
}

function showResult(message, isError) {
  resultOutput.textContent = message;
  resultOutput.classList.toggle("error", Boolean(isError));
}

function parseJson(text) {
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function formatStatus(status) {
  const service = status.service || status.status || "online";
  const ring = formatRingStatus(status.ring_active);
  const flash = status.flash_mode || "off";

  return [
    "状态检查成功",
    `service: ${service}`,
    `ring_active: ${ring}`,
    `flash_mode: ${flash}`
  ].join("\n");
}

function formatRingStatus(value) {
  if (value === true) {
    return "true (响铃中)";
  }

  if (value === false) {
    return "false (未响铃)";
  }

  return "未返回";
}

function getHttpErrorMessage(response, body, bodyText) {
  if (response.status === 401) {
    return "Token 错误或手机 Token 已重置。";
  }

  if (response.status === 404) {
    return "接口不存在 (404)。请确认手机服务已启动，并且当前 Android 版本支持该控制接口。";
  }

  if (response.status >= 500) {
    return `手机服务返回错误 (${response.status})。请确认手机端 Local Find 服务状态，或打开诊断页查看。`;
  }

  const serverMessage = body?.message || body?.error || bodyText || response.statusText || "请求未完成";
  return `请求失败 (${response.status})。${serverMessage}`;
}

function getFriendlyErrorMessage(error) {
  const message = error?.message || "";

  if (message === NETWORK_ERROR_MESSAGE) {
    return message;
  }

  if (message.includes("Failed to fetch") || message.includes("NetworkError")) {
    return NETWORK_ERROR_MESSAGE;
  }

  return message || "请求失败。请检查 IP、端口和手机服务状态。";
}
