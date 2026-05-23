const DEFAULT_PORT = "8888";

const COMMANDS = {
  status: { method: "GET", path: "/status", label: "检查状态" },
  "ring-start": { method: "POST", path: "/command/ring/start", label: "开始响铃", success: "已开始响铃" },
  "ring-stop": { method: "POST", path: "/command/ring/stop", label: "停止响铃", success: "已停止响铃" },
  "flash-start": { method: "POST", path: "/command/flash/strobe/start", label: "开始闪光", success: "已开始闪光" },
  "flash-stop": { method: "POST", path: "/command/flash/stop", label: "停止闪光", success: "已停止闪光" },
  "stop-all": { method: "POST", path: "/command/stop-all", label: "停止全部", success: "已停止全部" }
};

const NETWORK_ERROR_MESSAGE = "无法连接手机服务。请检查手机和电脑是否在同一 Wi-Fi、手机服务是否启动、IP/端口是否正确。";

const hostInput = document.getElementById("host");
const portInput = document.getElementById("port");
const tokenInput = document.getElementById("token");
const resultOutput = document.getElementById("result");
const endpointPreview = document.getElementById("endpoint-preview");
const buttons = Array.from(document.querySelectorAll("[data-command]"));

document.addEventListener("DOMContentLoaded", init);

function init() {
  chrome.storage.local.get({ host: "", port: DEFAULT_PORT }, ({ host, port }) => {
    hostInput.value = host || "";
    portInput.value = String(port || DEFAULT_PORT);
    updateEndpointPreview();
  });

  hostInput.addEventListener("input", handleConnectionInput);
  hostInput.addEventListener("blur", normalizeConnectionFields);
  portInput.addEventListener("input", handleConnectionInput);

  buttons.forEach((button) => {
    button.addEventListener("click", () => handleButtonClick(button.dataset.command));
  });
}

function handleConnectionInput() {
  syncPortFromHost();
  updateEndpointPreview();
  saveConnectionSettings();
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

    const response = await sendRequest(command);
    const bodyText = await response.text();
    const body = parseJson(bodyText);

    if (!response.ok) {
      throw new Error(getHttpErrorMessage(response, body, bodyText));
    }

    if (command.method === "GET" && body) {
      showResult(formatStatus(body), false);
    } else {
      showResult(command.success || `${command.label}成功`, false);
    }
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  } finally {
    setBusy(false);
  }
}

async function sendRequest(command) {
  const request = {
    method: command.method,
    cache: "no-store"
  };

  if (command.method === "POST") {
    const token = tokenInput.value;
    if (!token) {
      throw new Error("请输入 token");
    }
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
