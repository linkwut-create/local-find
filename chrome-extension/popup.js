const DEFAULT_PORT = "8888";
const FIND_PHONE_STEPS = [
  { method: "POST", path: "/command/ring/start", label: "响铃" },
  { method: "POST", path: "/command/flash/strobe/start", label: "闪光" }
];
const PROTECTED_COMMANDS = new Set(["find-phone", "flash-start"]);
const MIN_PIN_LENGTH = 4;
const MAX_PIN_LENGTH = 12;
const PIN_HASH_ITERATIONS = 100000;

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
const localPinInput = document.getElementById("local-pin");
const setLocalPinButton = document.getElementById("set-local-pin");
const disableProtectionButton = document.getElementById("disable-protection");
const registerWebAuthnButton = document.getElementById("register-webauthn");
const testWebAuthnButton = document.getElementById("test-webauthn");
const resultOutput = document.getElementById("result");
const endpointPreview = document.getElementById("endpoint-preview");
const deviceHost = document.getElementById("device-host");
const devicePort = document.getElementById("device-port");
const deviceTokenStatus = document.getElementById("device-token-status");
const lastSuccess = document.getElementById("last-success");
const protectionStatus = document.getElementById("protection-status");
const webauthnStatus = document.getElementById("webauthn-status");
const pinModal = document.getElementById("pin-modal");
const pinModalMessage = document.getElementById("pin-modal-message");
const verifyPinInput = document.getElementById("verify-pin");
const confirmPinButton = document.getElementById("confirm-pin");
const cancelPinButton = document.getElementById("cancel-pin");
const buttons = Array.from(document.querySelectorAll("[data-command]"));

let lastSuccessAt = "";
let rememberTokenState = false;
let protectionEnabled = false;
let localPinSalt = "";
let localPinHash = "";
let webauthnEnabled = false;
let webauthnCredentialId = "";

document.addEventListener("DOMContentLoaded", init);

function init() {
  chrome.storage.local.get(
    {
      host: "",
      port: DEFAULT_PORT,
      rememberToken: false,
      savedToken: "",
      lastSuccessAt: "",
      protectionEnabled: false,
      localPinSalt: "",
      localPinHash: "",
      webauthnEnabled: false,
      webauthnCredentialId: ""
    },
    ({
      host,
      port,
      rememberToken,
      savedToken,
      lastSuccessAt: savedLastSuccessAt,
      protectionEnabled: savedProtectionEnabled,
      localPinSalt: savedLocalPinSalt,
      localPinHash: savedLocalPinHash,
      webauthnEnabled: savedWebAuthnEnabled,
      webauthnCredentialId: savedWebAuthnCredentialId
    }) => {
      hostInput.value = host || "";
      portInput.value = String(port || DEFAULT_PORT);
      rememberTokenState = rememberToken === true;
      rememberTokenInput.checked = rememberTokenState;
      tokenInput.value = rememberToken === true ? savedToken || "" : "";
      lastSuccessAt = savedLastSuccessAt || "";
      protectionEnabled = savedProtectionEnabled === true;
      localPinSalt = savedLocalPinSalt || "";
      localPinHash = savedLocalPinHash || "";
      webauthnEnabled = savedWebAuthnEnabled === true;
      webauthnCredentialId = savedWebAuthnCredentialId || "";
      updateEndpointPreview();
      updateDeviceCard();
      updateProtectionStatus();
      updateWebAuthnStatus();
    }
  );

  hostInput.addEventListener("input", handleConnectionInput);
  hostInput.addEventListener("blur", normalizeConnectionFields);
  portInput.addEventListener("input", handleConnectionInput);
  tokenInput.addEventListener("input", saveTokenIfRemembered);
  rememberTokenInput.addEventListener("change", handleRememberTokenChange);
  clearSavedTokenButton.addEventListener("click", clearSavedToken);
  setLocalPinButton.addEventListener("click", setLocalPin);
  disableProtectionButton.addEventListener("click", disableProtection);
  registerWebAuthnButton.addEventListener("click", registerWebAuthn);
  testWebAuthnButton.addEventListener("click", testWebAuthn);

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

async function handleRememberTokenChange() {
  const shouldRemember = rememberTokenInput.checked;

  try {
    await requireLocalVerification("验证后才能修改 Token 保存设置。");

    if (shouldRemember) {
      rememberTokenState = true;
      rememberTokenInput.checked = true;
      chrome.storage.local.set({
        rememberToken: true,
        savedToken: tokenInput.value
      });
      updateDeviceCard();
      return;
    }

    rememberTokenState = false;
    rememberTokenInput.checked = false;
    chrome.storage.local.set({ rememberToken: false }, () => {
      chrome.storage.local.remove("savedToken", updateDeviceCard);
    });
  } catch (error) {
    rememberTokenInput.checked = rememberTokenState;
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function saveTokenIfRemembered() {
  if (!rememberTokenInput.checked) {
    return;
  }

  try {
    await requireLocalVerification("验证后才能保存 Token。");
    chrome.storage.local.set({
      rememberToken: true,
      savedToken: tokenInput.value
    });
    updateDeviceCard();
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function clearSavedToken() {
  try {
    await requireLocalVerification("验证后才能清除已保存 Token。");
    tokenInput.value = "";
    rememberTokenInput.checked = false;
    rememberTokenState = false;
    chrome.storage.local.set({ rememberToken: false }, () => {
      chrome.storage.local.remove("savedToken", () => {
        updateDeviceCard();
        showResult("已清除已保存 Token", false);
      });
    });
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function setLocalPin() {
  const pin = localPinInput.value;

  try {
    if (protectionEnabled) {
      await requireLocalVerification("验证当前 PIN 后才能更新保护 PIN。");
    }

    validatePin(pin);
    const salt = createSalt();
    const hash = await hashPin(pin, salt);
    protectionEnabled = true;
    localPinSalt = salt;
    localPinHash = hash;
    chrome.storage.local.set({
      protectionEnabled: true,
      localPinSalt: salt,
      localPinHash: hash
    });
    localPinInput.value = "";
    updateProtectionStatus();
    showResult("已开启本地保护锁", false);
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function disableProtection() {
  if (!protectionEnabled) {
    showResult("本地保护锁未开启", false);
    return;
  }

  try {
    await requireLocalVerification("请输入当前 PIN 以关闭保护锁。");
    protectionEnabled = false;
    localPinSalt = "";
    localPinHash = "";
    chrome.storage.local.set({ protectionEnabled: false }, () => {
      chrome.storage.local.remove(["localPinSalt", "localPinHash"], () => {
        updateProtectionStatus();
        showResult("已关闭本地保护锁", false);
      });
    });
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function registerWebAuthn() {
  try {
    ensureWebAuthnSupport();
    const credential = await navigator.credentials.create({
      publicKey: {
        challenge: randomBytes(32),
        rp: {
          name: "Local Find"
        },
        user: {
          id: randomBytes(32),
          name: "local-find-extension-user",
          displayName: "Local Find"
        },
        pubKeyCredParams: [
          { type: "public-key", alg: -7 },
          { type: "public-key", alg: -257 }
        ],
        authenticatorSelection: {
          authenticatorAttachment: "platform",
          userVerification: "required"
        },
        timeout: 60000,
        attestation: "none"
      }
    });

    if (!credential || !credential.rawId) {
      throw new Error("系统验证注册未返回凭据");
    }

    webauthnEnabled = true;
    webauthnCredentialId = arrayBufferToBase64Url(credential.rawId);
    chrome.storage.local.set({
      webauthnEnabled: true,
      webauthnCredentialId
    });
    updateWebAuthnStatus();
    showResult("系统验证已注册", false);
  } catch (error) {
    showResult(getWebAuthnErrorMessage(error, "系统验证注册失败"), true);
  }
}

async function testWebAuthn() {
  try {
    ensureWebAuthnSupport();
    if (!webauthnEnabled || !webauthnCredentialId) {
      throw new Error("请先注册系统验证");
    }

    await navigator.credentials.get({
      publicKey: {
        challenge: randomBytes(32),
        allowCredentials: [
          {
            type: "public-key",
            id: base64UrlToBytes(webauthnCredentialId)
          }
        ],
        userVerification: "required",
        timeout: 60000
      }
    });

    showResult("系统验证通过", false);
  } catch (error) {
    showResult(getWebAuthnErrorMessage(error, "系统验证失败"), true);
  }
}

function ensureWebAuthnSupport() {
  if (!navigator.credentials || typeof PublicKeyCredential === "undefined") {
    throw new Error("当前浏览器/环境不支持 WebAuthn 平台验证");
  }
}

async function requireProtectedCommand(commandName) {
  if (!PROTECTED_COMMANDS.has(commandName)) {
    return;
  }

  await requireLocalVerification("请输入本地保护 PIN 以继续。");
}

async function requireLocalVerification(message) {
  if (!protectionEnabled) {
    return;
  }

  const pin = await promptForPin(message);
  const isValid = await verifyPin(pin);
  if (!isValid) {
    throw new Error("本地验证失败");
  }
}

function promptForPin(message) {
  pinModalMessage.textContent = message || "请输入本地保护 PIN。";
  verifyPinInput.value = "";
  pinModal.classList.remove("hidden");
  verifyPinInput.focus();

  return new Promise((resolve, reject) => {
    const cleanup = () => {
      confirmPinButton.removeEventListener("click", handleConfirm);
      cancelPinButton.removeEventListener("click", handleCancel);
      verifyPinInput.removeEventListener("keydown", handleKeyDown);
      pinModal.classList.add("hidden");
    };

    const handleConfirm = () => {
      const pin = verifyPinInput.value;
      cleanup();
      resolve(pin);
    };

    const handleCancel = () => {
      cleanup();
      reject(new Error("已取消本地验证"));
    };

    const handleKeyDown = (event) => {
      if (event.key === "Enter") {
        handleConfirm();
      } else if (event.key === "Escape") {
        handleCancel();
      }
    };

    confirmPinButton.addEventListener("click", handleConfirm);
    cancelPinButton.addEventListener("click", handleCancel);
    verifyPinInput.addEventListener("keydown", handleKeyDown);
  });
}

async function verifyPin(pin) {
  if (!pin || !localPinSalt || !localPinHash) {
    return false;
  }

  const hash = await hashPin(pin, localPinSalt);
  return hash === localPinHash;
}

function validatePin(pin) {
  if (pin.length < MIN_PIN_LENGTH || pin.length > MAX_PIN_LENGTH) {
    throw new Error("PIN 需要为 4-12 位");
  }
}

function createSalt() {
  return bytesToBase64(randomBytes(16));
}

async function hashPin(pin, salt) {
  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(pin),
    "PBKDF2",
    false,
    ["deriveBits"]
  );
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      salt: base64ToBytes(salt),
      iterations: PIN_HASH_ITERATIONS,
      hash: "SHA-256"
    },
    keyMaterial,
    256
  );
  return bytesToBase64(new Uint8Array(derivedBits));
}

function bytesToBase64(bytes) {
  let value = "";
  bytes.forEach((byte) => {
    value += String.fromCharCode(byte);
  });
  return btoa(value);
}

function base64ToBytes(value) {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
}

function randomBytes(length) {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return bytes;
}

function arrayBufferToBase64Url(buffer) {
  return bytesToBase64Url(new Uint8Array(buffer));
}

function bytesToBase64Url(bytes) {
  return bytesToBase64(bytes)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function base64UrlToBytes(value) {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  return base64ToBytes(padded);
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
      validateCommandInputs({ method: "POST" });
      await requireProtectedCommand(commandName);
      await runFindPhoneSequence();
      saveLastSuccessAt();
      showResult(command.success, false);
    } else {
      if (PROTECTED_COMMANDS.has(commandName)) {
        validateCommandInputs(command);
        await requireProtectedCommand(commandName);
      }
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

function updateProtectionStatus() {
  protectionStatus.textContent = protectionEnabled ? "已开启" : "未开启";
  protectionStatus.classList.toggle("enabled", protectionEnabled);
  disableProtectionButton.disabled = !protectionEnabled;
}

function updateWebAuthnStatus() {
  webauthnStatus.textContent = webauthnEnabled && webauthnCredentialId ? "已注册" : "未注册";
  webauthnStatus.classList.toggle("enabled", webauthnEnabled && Boolean(webauthnCredentialId));
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

function getWebAuthnErrorMessage(error, fallback) {
  const message = error?.message || "";

  if (message === "当前浏览器/环境不支持 WebAuthn 平台验证" || message === "请先注册系统验证") {
    return message;
  }

  if (error?.name === "NotAllowedError") {
    return "已取消系统验证";
  }

  return `${fallback}：${message || error?.name || "未知错误"}`;
}
