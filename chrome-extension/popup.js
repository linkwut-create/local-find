const DEFAULT_PORT = "8888";

const COMMANDS = {
  status: { method: "GET", path: "/status", label: "检查状态" },
  "ring-start": { method: "POST", path: "/command/ring/start", label: "开始响铃" },
  "ring-stop": { method: "POST", path: "/command/ring/stop", label: "停止响铃" },
  "flash-start": { method: "POST", path: "/command/flash/strobe/start", label: "开始闪光" },
  "flash-stop": { method: "POST", path: "/command/flash/stop", label: "停止闪光" },
  "stop-all": { method: "POST", path: "/command/stop-all", label: "停止全部" }
};

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
  portInput.addEventListener("input", handleConnectionInput);

  buttons.forEach((button) => {
    button.addEventListener("click", () => handleButtonClick(button.dataset.command));
  });
}

function handleConnectionInput() {
  updateEndpointPreview();
  saveConnectionSettings();
}

function saveConnectionSettings() {
  chrome.storage.local.set({
    host: hostInput.value.trim(),
    port: getPort()
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

    const response = await sendRequest(command);
    const bodyText = await response.text();
    const body = parseJson(bodyText);

    if (!response.ok) {
      const message = body?.message || bodyText || response.statusText || "请求失败";
      throw new Error(`${response.status} ${message}`);
    }

    if (command.method === "GET" && body) {
      showResult(formatStatus(body), false);
    } else {
      showResult(`${command.label}成功`, false);
    }
  } catch (error) {
    showResult(error.message || "请求失败", true);
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

  return fetch(`${getBaseUrl()}${command.path}`, request);
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
  const host = normalizeHost(hostInput.value);
  const port = getPort();

  if (!host) {
    throw new Error("请输入 host");
  }

  if (!isValidPort(port)) {
    throw new Error("请输入有效端口");
  }

  return `http://${host}:${port}`;
}

function normalizeHost(value) {
  return value
    .trim()
    .replace(/^https?:\/\//i, "")
    .replace(/\/.*$/, "")
    .replace(/:\d+$/, "");
}

function getPort() {
  return String(portInput.value || DEFAULT_PORT).trim();
}

function isValidPort(port) {
  const numericPort = Number(port);
  return Number.isInteger(numericPort) && numericPort >= 1 && numericPort <= 65535;
}

function updateEndpointPreview() {
  const host = normalizeHost(hostInput.value) || "HOST";
  const port = isValidPort(getPort()) ? getPort() : DEFAULT_PORT;
  endpointPreview.textContent = `http://${host}:${port}`;
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
  const ring = status.ring_active === true ? "响铃中" : "未响铃";
  const flash = status.flash_mode || "off";
  const service = status.service || status.status || "online";
  return `状态: ${service} | ${ring} | 闪光: ${flash}`;
}
