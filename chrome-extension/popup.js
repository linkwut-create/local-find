const DEFAULT_PORT = "8888";
const FIND_PHONE_STEPS = [
  { method: "POST", path: "/command/ring/start", label: "Ring" },
  { method: "POST", path: "/command/flash/strobe/start", label: "Flash" }
];
const PROTECTED_COMMANDS = new Set(["find-phone", "flash-start"]);
const MIN_PIN_LENGTH = 4;
const MAX_PIN_LENGTH = 12;
const PIN_HASH_ITERATIONS = 100000;
const PROTECTION_METHOD_PIN = "pin";
const PROTECTION_METHOD_WEBAUTHN = "webauthn";
const PROTECTION_METHOD_WEBAUTHN_OR_PIN = "webauthn-or-pin";
const CONTROLLER_NAME = "Chrome on Windows";
const CONTROLLER_TYPE = "chrome_extension";
const PAIRING_POLL_INTERVAL_MS = 2000;

const COMMANDS = {
  status: { method: "GET", path: "/status", label: "Check Status" },
  "find-phone": { method: "SEQUENCE", label: "Find Phone", success: "Find Phone: Ring + Flash" },
  "ring-stop": { method: "POST", path: "/command/ring/stop", label: "Stop Ring", success: "Ring stopped" },
  "flash-start": { method: "POST", path: "/command/flash/strobe/start", label: "Flash", success: "Flash started" },
  "flash-stop": { method: "POST", path: "/command/flash/stop", label: "Stop Flash", success: "Flash stopped" },
  "stop-all": { method: "POST", path: "/command/stop-all", label: "Stop All Alerts", success: "All alerts stopped" }
};

const NETWORK_ERROR_MESSAGE = "Cannot reach phone. Check Wi-Fi, service status, IP, and port.";

const hostInput = document.getElementById("host");
const portInput = document.getElementById("port");
const tokenInput = document.getElementById("token");
const rememberTokenInput = document.getElementById("remember-token");
const clearSavedTokenButton = document.getElementById("clear-saved-token");
const localPinInput = document.getElementById("local-pin");
const setLocalPinButton = document.getElementById("set-local-pin");
const disableProtectionButton = document.getElementById("disable-protection");
const protectionMethodSelect = document.getElementById("protection-method");
const registerWebAuthnButton = document.getElementById("register-webauthn");
const testWebAuthnButton = document.getElementById("test-webauthn");
const resultOutput = document.getElementById("result");
const endpointPreview = document.getElementById("endpoint-preview");
const deviceName = document.getElementById("device-name");
const deviceAddress = document.getElementById("device-address");
const deviceTokenStatus = document.getElementById("device-token-status");
const lastSuccess = document.getElementById("last-success");
const pairedDevicesList = document.getElementById("paired-devices-list");
const pairingHostInput = document.getElementById("pairing-host");
const pairingPortInput = document.getElementById("pairing-port");
const checkPhoneButton = document.getElementById("check-phone");
const requestPairingButton = document.getElementById("request-pairing");
const pairingStatusOutput = document.getElementById("pairing-status");
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
let protectionMethod = PROTECTION_METHOD_PIN;
let devices = [];
let selectedDeviceId = "";
let controllerId = "";
let pairingPollTimer = null;

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
      webauthnCredentialId: "",
      protectionMethod: PROTECTION_METHOD_PIN,
      devices: [],
      selectedDeviceId: "",
      controllerId: ""
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
      webauthnCredentialId: savedWebAuthnCredentialId,
      protectionMethod: savedProtectionMethod,
      devices: savedDevices,
      selectedDeviceId: savedSelectedDeviceId,
      controllerId: savedControllerId
    }) => {
      hostInput.value = host || "";
      portInput.value = String(port || DEFAULT_PORT);
      pairingHostInput.value = host || "";
      pairingPortInput.value = String(port || DEFAULT_PORT);
      rememberTokenState = rememberToken === true;
      rememberTokenInput.checked = rememberTokenState;
      tokenInput.value = rememberToken === true ? savedToken || "" : "";
      lastSuccessAt = savedLastSuccessAt || "";
      protectionEnabled = savedProtectionEnabled === true;
      localPinSalt = savedLocalPinSalt || "";
      localPinHash = savedLocalPinHash || "";
      webauthnEnabled = savedWebAuthnEnabled === true;
      webauthnCredentialId = savedWebAuthnCredentialId || "";
      protectionMethod = normalizeProtectionMethod(savedProtectionMethod);
      devices = normalizeDevices(savedDevices);
      selectedDeviceId = getUsableSelectedDeviceId(savedSelectedDeviceId, devices);
      controllerId = savedControllerId || "";
      protectionMethodSelect.value = protectionMethod;
      updateEndpointPreview();
      updateDeviceCard();
      updatePairedDevicesList();
      updateProtectionStatus();
      updateWebAuthnStatus();
      updateProtectionMethodOptions();
    }
  );

  hostInput.addEventListener("input", handleConnectionInput);
  hostInput.addEventListener("blur", normalizeConnectionFields);
  portInput.addEventListener("input", handleConnectionInput);
  tokenInput.addEventListener("input", saveTokenIfRemembered);
  rememberTokenInput.addEventListener("change", handleRememberTokenChange);
  clearSavedTokenButton.addEventListener("click", clearSavedToken);
  pairedDevicesList.addEventListener("click", handlePairedDevicesClick);
  pairedDevicesList.addEventListener("keydown", handlePairedDevicesKeyDown);
  pairingHostInput.addEventListener("input", handlePairingInput);
  pairingHostInput.addEventListener("blur", normalizePairingFields);
  pairingPortInput.addEventListener("input", handlePairingInput);
  checkPhoneButton.addEventListener("click", checkPhone);
  requestPairingButton.addEventListener("click", requestPairing);
  setLocalPinButton.addEventListener("click", setLocalPin);
  disableProtectionButton.addEventListener("click", disableProtection);
  protectionMethodSelect.addEventListener("change", handleProtectionMethodChange);
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

async function handlePairedDevicesClick(event) {
  const deleteButton = event.target.closest("[data-delete-device-id]");
  if (deleteButton) {
    event.stopPropagation();
    await deleteDevice(deleteButton.dataset.deleteDeviceId);
    return;
  }

  const button = event.target.closest("[data-select-device-id]");
  if (!button) {
    return;
  }

  await selectDevice(button.dataset.selectDeviceId);
}

async function handlePairedDevicesKeyDown(event) {
  if (event.target.closest("[data-delete-device-id]")) {
    return;
  }

  if (event.key !== "Enter" && event.key !== " ") {
    return;
  }

  const item = event.target.closest("[data-select-device-id]");
  if (!item) {
    return;
  }

  event.preventDefault();
  await selectDevice(item.dataset.selectDeviceId);
}

async function selectDevice(deviceId) {
  if (!devices.some((device) => device.id === deviceId)) {
    showResult("Device not found", true);
    return;
  }

  selectedDeviceId = deviceId;
  await setStorage({ selectedDeviceId });
  updateEndpointPreview();
  updateDeviceCard();
  updatePairedDevicesList();
  showResult("Switched current phone", false);
}

async function deleteDevice(deviceId) {
  const device = devices.find((candidate) => candidate.id === deviceId);
  if (!device) {
    showResult("Device not found", true);
    return;
  }

  const deviceName = device.name || "Android Phone";
  const canRevoke = Boolean(device.controllerId && device.host && isValidPort(device.port) && device.token);
  const confirmMessage = canRevoke
    ? "Remove paired phone \"" + deviceName + "\"?\n\nPhone-side authorization will be revoked, then local record deleted."
    : "Remove paired phone \"" + deviceName + "\"?\n\nThis record lacks revocation info (controllerId/host/port/token). Local delete only.";

  const confirmed = window.confirm(confirmMessage);
  if (!confirmed) {
    return;
  }

  try {
    await requireSensitiveVerification("Verify to remove paired phone.");

    if (canRevoke) {
      const revoked = await tryRevokeDevice(device);
      if (!revoked) {
        const localOnly = window.confirm(
          "Cannot revoke phone-side authorization (phone offline or token invalid). Delete local record only?"
        );
        if (!localOnly) {
          return;
        }
      }
    }

    devices = devices.filter((candidate) => candidate.id !== deviceId);
    if (selectedDeviceId === deviceId) {
      selectedDeviceId = devices[0]?.id || "";
    }

    await setStorage({ devices, selectedDeviceId });
    updateEndpointPreview();
    updateDeviceCard();
    updatePairedDevicesList();
    showResult(canRevoke ? "Revoked phone authorization and removed local device" : "Local device removed", false);
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function tryRevokeDevice(device) {
  const url = `http://${device.host}:${device.port}/pairing/revoke`;
  try {
    const { body } = await sendJsonRequest(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-LocalFind-Token": device.token
      },
      body: JSON.stringify({ controllerId: device.controllerId })
    });
    return body?.ok === true && body?.revoked === true;
  } catch (error) {
    return false;
  }
}

function handlePairingInput() {
  syncPairingPortFromHost();
}

function normalizePairingFields() {
  const connection = parsePairingInput();
  pairingHostInput.value = connection.host;
  pairingPortInput.value = connection.port;
}

async function checkPhone() {
  try {
    setPairingBusy(true);
    const target = parsePairingInput();
    validateConnectionTarget(target);

    const { body } = await sendJsonRequest(`${getBaseUrlForTarget(target)}/device-info`);
    pairingStatusOutput.textContent = formatDeviceInfo(body);
  } catch (error) {
    pairingStatusOutput.textContent = getFriendlyErrorMessage(error);
    pairingStatusOutput.classList.add("error");
  } finally {
    setPairingBusy(false);
  }
}

async function requestPairing() {
  try {
    clearPairingPoll();
    setPairingBusy(true);
    const target = parsePairingInput();
    validateConnectionTarget(target);
    const id = await getOrCreateControllerId();
    const nonce = createUuid();

    const { body } = await sendJsonRequest(`${getBaseUrlForTarget(target)}/pairing/request`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        controllerId: id,
        controllerName: CONTROLLER_NAME,
        controllerType: CONTROLLER_TYPE,
        nonce
      })
    });

    if (!body?.requestId) {
      throw new Error("Pairing request did not return requestId");
    }

    pairingStatusOutput.classList.remove("error");
    pairingStatusOutput.textContent = "Waiting for phone confirmation";
    pollPairingStatus(target, body.requestId);
  } catch (error) {
    setPairingBusy(false);
    pairingStatusOutput.textContent = getFriendlyErrorMessage(error);
    pairingStatusOutput.classList.add("error");
  }
}

function pollPairingStatus(target, requestId) {
  const run = async () => {
    try {
      const url = `${getBaseUrlForTarget(target)}/pairing/status?requestId=${encodeURIComponent(requestId)}`;
      const { response, body } = await sendJsonRequest(url, { allowedStatuses: [404] });
      if (response.status === 404 && body?.status !== "expired") {
        throw new Error(getHttpErrorMessage(response, body, ""));
      }
      const status = body?.status || "pending";

      if (status === "pending") {
        pairingStatusOutput.classList.remove("error");
        pairingStatusOutput.textContent = "Waiting for phone confirmation";
        pairingPollTimer = window.setTimeout(run, PAIRING_POLL_INTERVAL_MS);
        return;
      }

      clearPairingPoll();
      setPairingBusy(false);

      if (status === "accepted") {
        await saveAcceptedDevice(body, target);
        pairingStatusOutput.classList.remove("error");
        pairingStatusOutput.textContent = "Paired successfully, device saved";
        return;
      }

      pairingStatusOutput.classList.toggle("error", status === "rejected" || status === "expired");
      pairingStatusOutput.textContent = getPairingTerminalMessage(status);
    } catch (error) {
      clearPairingPoll();
      setPairingBusy(false);
      pairingStatusOutput.textContent = getFriendlyErrorMessage(error);
      pairingStatusOutput.classList.add("error");
    }
  };

  run();
}

function clearPairingPoll() {
  if (pairingPollTimer) {
    window.clearTimeout(pairingPollTimer);
    pairingPollTimer = null;
  }
}

function parsePairingInput() {
  const parsed = parseHostValue(pairingHostInput.value);
  const port = parsed.port || String(pairingPortInput.value || DEFAULT_PORT).trim();

  return {
    host: parsed.host,
    port
  };
}

function syncPairingPortFromHost() {
  const parsed = parseHostValue(pairingHostInput.value);
  if (parsed.port && isValidPort(parsed.port)) {
    pairingPortInput.value = parsed.port;
  }
}

function validateConnectionTarget(target) {
  const missing = [];

  if (!target.host) {
    missing.push("host");
  }

  if (!isValidPort(target.port)) {
    missing.push("port");
  }

  if (missing.length > 0) {
    throw new Error(`Missing:  ${missing.join("、")}`);
  }
}

function formatDeviceInfo(info) {
  const pairingMode = info?.pairingMode === true;
  const lines = [
    `device name: ${info?.name || "No response"}`,
    `device id: ${info?.id || "No response"}`,
    `pairingMode: ${pairingMode ? "true" : "false"}`,
    `service: ${info?.service || "No response"}`
  ];

  if (!pairingMode) {
    lines.push("Enable pairing mode on the phone first.");
  }

  pairingStatusOutput.classList.remove("error");
  return lines.join("\n");
}

function getPairingTerminalMessage(status) {
  if (status === "rejected") {
    return "Phone rejected";
  }

  if (status === "expired") {
    return "Pairing request expired";
  }

  return `Pairing request ended: ：${status || "unknown"}`;
}

async function saveAcceptedDevice(pairingResult, target) {
  const pairedDevice = pairingResult?.device || {};
  const controlToken = pairingResult?.controlToken || "";

  if (!pairedDevice.id || !controlToken) {
    throw new Error("Pairing response missing device or controlToken");
  }

  const now = new Date().toISOString();
  const nextDevice = {
    id: pairedDevice.id,
    name: pairedDevice.name || "Android Phone",
    type: pairedDevice.type || "android_phone",
    host: pairedDevice.host || target.host,
    port: String(pairedDevice.port || target.port || DEFAULT_PORT),
    token: controlToken,
    controllerId: controllerId || "",
    pairedAt: now,
    lastSuccessAt: ""
  };

  const existing = devices.find((device) => device.id === nextDevice.id);
  if (existing) {
    nextDevice.pairedAt = existing.pairedAt || now;
    nextDevice.lastSuccessAt = existing.lastSuccessAt || "";
  }

  devices = upsertDevice(devices, nextDevice);
  selectedDeviceId = nextDevice.id;
  await setStorage({
    devices,
    selectedDeviceId
  });

  updateEndpointPreview();
  updateDeviceCard();
  updatePairedDevicesList();
}

function upsertDevice(currentDevices, nextDevice) {
  const withoutDevice = currentDevices.filter((device) => device.id !== nextDevice.id);
  return [...withoutDevice, nextDevice];
}

function normalizeDevices(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .filter((device) => device && device.id)
    .map((device) => ({
      id: String(device.id),
      name: device.name || "Android Phone",
      type: device.type || "android_phone",
      host: device.host || "",
      port: String(device.port || DEFAULT_PORT),
      token: device.token || "",
      controllerId: device.controllerId || "",
      pairedAt: device.pairedAt || "",
      lastSuccessAt: device.lastSuccessAt || ""
    }));
}

function getUsableSelectedDeviceId(value, currentDevices) {
  const id = value || "";
  return currentDevices.some((device) => device.id === id) ? id : "";
}

function getSelectedDevice() {
  return devices.find((device) => device.id === selectedDeviceId) || null;
}

function getSelectedCommandTarget() {
  const selectedDevice = getSelectedDevice();

  if (!selectedDevice || !selectedDevice.host || !isValidPort(selectedDevice.port) || !selectedDevice.token) {
    return null;
  }

  return {
    host: selectedDevice.host,
    port: selectedDevice.port,
    token: selectedDevice.token,
    deviceId: selectedDevice.id
  };
}

async function getOrCreateControllerId() {
  if (controllerId) {
    return controllerId;
  }

  controllerId = createUuid();
  await setStorage({ controllerId });
  return controllerId;
}

function createUuid() {
  if (crypto.randomUUID) {
    return crypto.randomUUID();
  }

  const bytes = randomBytes(16);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

async function sendJsonRequest(url, options = {}) {
  const allowedStatuses = options.allowedStatuses || [];
  const request = {
    method: options.method || "GET",
    cache: "no-store",
    headers: options.headers || {},
    body: options.body
  };

  try {
    const response = await fetch(url, request);
    const bodyText = await response.text();
    const body = parseJson(bodyText);

    if (!response.ok && !allowedStatuses.includes(response.status)) {
      throw new Error(getHttpErrorMessage(response, body, bodyText));
    }

    return { response, body, bodyText };
  } catch (error) {
    if (error?.message) {
      throw error;
    }
    throw new Error(NETWORK_ERROR_MESSAGE);
  }
}

function setPairingBusy(isBusy) {
  checkPhoneButton.disabled = isBusy;
  requestPairingButton.disabled = isBusy;
}

function setStorage(values) {
  return new Promise((resolve) => {
    chrome.storage.local.set(values, resolve);
  });
}

async function handleRememberTokenChange() {
  const shouldRemember = rememberTokenInput.checked;

  try {
    await requireSensitiveVerification("Verify to change token save setting.");

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
    await requireSensitiveVerification("Verify to save token.");
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
    await requireSensitiveVerification("Verify to clear saved token.");
    tokenInput.value = "";
    rememberTokenInput.checked = false;
    rememberTokenState = false;
    chrome.storage.local.set({ rememberToken: false }, () => {
      chrome.storage.local.remove("savedToken", () => {
        updateDeviceCard();
        showResult("ClearedSaved Token", false);
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
      await requireSensitiveVerification("Verify current PIN to update protection PIN.");
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
    showResult("Protection enabled", false);
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function disableProtection() {
  if (!protectionEnabled) {
    showResult("Protection disabled", false);
    return;
  }

  try {
    await requireSensitiveVerification("Enter current PIN to disable protection.");
    protectionEnabled = false;
    localPinSalt = "";
    localPinHash = "";
    chrome.storage.local.set({ protectionEnabled: false }, () => {
      chrome.storage.local.remove(["localPinSalt", "localPinHash"], () => {
        updateProtectionStatus();
        showResult("Protection lock disabled", false);
      });
    });
  } catch (error) {
    showResult(getFriendlyErrorMessage(error), true);
  }
}

async function handleProtectionMethodChange() {
  const nextMethod = normalizeProtectionMethod(protectionMethodSelect.value);

  if (requiresWebAuthn(nextMethod) && !hasWebAuthnCredential()) {
    protectionMethodSelect.value = protectionMethod;
    showResult("Register WebAuthn first", true);
    return;
  }

  protectionMethod = nextMethod;
  chrome.storage.local.set({ protectionMethod });
  showResult(`Protection method set to: ${getProtectionMethodLabel(protectionMethod)}`, false);
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
      throw new Error("WebAuthn registration returned no credential");
    }

    webauthnEnabled = true;
    webauthnCredentialId = arrayBufferToBase64Url(credential.rawId);
    chrome.storage.local.set({
      webauthnEnabled: true,
      webauthnCredentialId
    });
    updateWebAuthnStatus();
    updateProtectionMethodOptions();
    showResult("WebAuthnRegistered", false);
  } catch (error) {
    showResult(getWebAuthnErrorMessage(error, "WebAuthn registration failed"), true);
  }
}

async function testWebAuthn() {
  try {
    ensureWebAuthnSupport();
    if (!webauthnEnabled || !webauthnCredentialId) {
      throw new Error("Register WebAuthn first");
    }

    await performWebAuthnVerification();

    showResult("WebAuthn verified", false);
  } catch (error) {
    showResult(getWebAuthnErrorMessage(error, "WebAuthn verification failed"), true);
  }
}

function ensureWebAuthnSupport() {
  if (!navigator.credentials || typeof PublicKeyCredential === "undefined") {
    throw new Error("WebAuthn not supported in this browser");
  }
}

async function performWebAuthnVerification() {
  ensureWebAuthnSupport();
  if (!hasWebAuthnCredential()) {
    throw new Error("Register WebAuthn first");
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
}

function hasWebAuthnCredential() {
  return webauthnEnabled && Boolean(webauthnCredentialId);
}

function requiresWebAuthn(method) {
  return method === PROTECTION_METHOD_WEBAUTHN || method === PROTECTION_METHOD_WEBAUTHN_OR_PIN;
}

function normalizeProtectionMethod(method) {
  if (
    method === PROTECTION_METHOD_WEBAUTHN ||
    method === PROTECTION_METHOD_WEBAUTHN_OR_PIN ||
    method === PROTECTION_METHOD_PIN
  ) {
    return method;
  }

  return PROTECTION_METHOD_PIN;
}

function getProtectionMethodLabel(method) {
  if (method === PROTECTION_METHOD_WEBAUTHN) {
    return "WebAuthn";
  }

  if (method === PROTECTION_METHOD_WEBAUTHN_OR_PIN) {
    return "WebAuthn, fallback to PIN";
  }

  return "Local PIN";
}

async function requireProtectedCommand(commandName) {
  if (!PROTECTED_COMMANDS.has(commandName)) {
    return;
  }

  await requireSensitiveVerification("Verify to continue.");
}

async function requireSensitiveVerification(message) {
  if (!protectionEnabled) {
    return;
  }

  if (protectionMethod === PROTECTION_METHOD_WEBAUTHN) {
    await requireWebAuthnForSensitiveAction();
    return;
  }

  if (protectionMethod === PROTECTION_METHOD_WEBAUTHN_OR_PIN) {
    try {
      await requireWebAuthnForSensitiveAction();
      return;
    } catch {
      await requireLocalVerification("WebAuthn failed. Enter local PIN to continue.");
      return;
    }
  }

  await requireLocalVerification(message || "Enter protection PIN to continue.");
}

async function requireWebAuthnForSensitiveAction() {
  if (!hasWebAuthnCredential()) {
    throw new Error("Register WebAuthn first");
  }

  try {
    await performWebAuthnVerification();
  } catch (error) {
    throw new Error(getWebAuthnErrorMessage(error, "WebAuthn verification failed"));
  }
}

async function requireLocalVerification(message) {
  if (!protectionEnabled) {
    return;
  }

  const pin = await promptForPin(message);
  const isValid = await verifyPin(pin);
  if (!isValid) {
    throw new Error("Local verification failed");
  }
}

function promptForPin(message) {
  pinModalMessage.textContent = message || "Enter your protection PIN.";
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
      reject(new Error("Local verification cancelled"));
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
    throw new Error("PIN must be 4-12 digits");
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
    showResult("Unknown command", true);
    return;
  }

  try {
    setBusy(true);
    showResult(`${command.label}...`, false);

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
        showResult(body ? formatStatus(body) : "Status OK", false);
      } else {
        saveLastSuccessAt();
        showResult(command.success || `${command.label} succeeded`, false);
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
    throw new Error(`Ring start failed: ${getFriendlyErrorMessage(error)}`);
  }

  try {
    await sendCheckedRequest(FIND_PHONE_STEPS[1]);
  } catch (error) {
    throw new Error(`Ring started, but flash failed: ${getFriendlyErrorMessage(error)}`);
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
  const target = getCommandTarget(command);
  const request = {
    method: command.method,
    cache: "no-store"
  };

  validateCommandInputs(command, target);

  if (command.method === "POST") {
    request.headers = {
      "X-LocalFind-Token": target.token
    };
  }

  const url = `${getBaseUrlForTarget(target)}${command.path}`;

  try {
    return await fetch(url, request);
  } catch {
    throw new Error(NETWORK_ERROR_MESSAGE);
  }
}

function openDiagnosticsPage() {
  try {
    window.open(`${getBaseUrl()}/`, "_blank", "noopener");
    showResult("Diagnostics page opened", false);
  } catch (error) {
    showResult(error.message || "Cannot open diagnostics page", true);
  }
}

function getBaseUrl() {
  return getBaseUrlForTarget(getDisplayTarget());
}

function getBaseUrlForTarget(target) {
  const { host, port } = target;

  if (!host) {
    throw new Error("Enter host");
  }

  if (!isValidPort(port)) {
    throw new Error("Enter valid port");
  }

  return `http://${host}:${port}`;
}

function getDisplayTarget() {
  const selectedDevice = getSelectedDevice();

  if (selectedDevice?.host && isValidPort(selectedDevice.port)) {
    return {
      host: selectedDevice.host,
      port: selectedDevice.port
    };
  }

  return parseConnectionInput();
}

function getCommandTarget(command) {
  const selectedTarget = getSelectedCommandTarget();
  if (selectedTarget) {
    return selectedTarget;
  }

  const manual = parseConnectionInput();
  return {
    host: manual.host,
    port: manual.port,
    token: command.method === "POST" ? tokenInput.value : "",
    deviceId: ""
  };
}

function validateCommandInputs(command, target = getCommandTarget(command)) {
  const missing = [];

  if (!target.host) {
    missing.push("host");
  }

  if (!isValidPort(target.port)) {
    missing.push("port");
  }

  if (command.method === "POST" && !target.token) {
    missing.push("token");
  }

  if (missing.length > 0) {
    throw new Error(`Missing:  ${missing.join("、")}`);
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
  const { host, port } = getDisplayTarget();
  const displayHost = host || "HOST";
  const displayPort = isValidPort(port) ? port : DEFAULT_PORT;
  endpointPreview.textContent = `http://${displayHost}:${displayPort}`;
}

function updateDeviceCard() {
  const selectedDevice = getSelectedDevice();

  if (selectedDevice) {
    deviceName.textContent = selectedDevice.name || "Android Phone";
    deviceAddress.textContent = formatAddress(selectedDevice.host, selectedDevice.port);
    deviceTokenStatus.textContent = selectedDevice.token ? "Paired" : "Missing token";
    lastSuccess.textContent = "Last Connected: " + (selectedDevice.lastSuccessAt ? formatDateTime(selectedDevice.lastSuccessAt) : "--");
    return;
  }

  const { host, port } = parseConnectionInput();
  deviceName.textContent = "Manual";
  deviceAddress.textContent = host ? formatAddress(host, port) : "N/A";
  deviceTokenStatus.textContent = rememberTokenInput.checked && tokenInput.value ? "Legacy token saved" : "Manual host/port/token";
  lastSuccess.textContent = `Last Connected:${lastSuccessAt ? formatDateTime(lastSuccessAt) : "--"}`;
}

function updatePairedDevicesList() {
  pairedDevicesList.replaceChildren();

  if (devices.length === 0) {
    const empty = document.createElement("p");
    empty.className = "paired-devices-empty";
    empty.textContent = "No paired phones. Enable pairing mode on the phone to add one.";
    pairedDevicesList.append(empty);
    return;
  }

  devices.forEach((device) => {
    const isSelected = device.id === selectedDeviceId;
    const item = document.createElement("div");
    item.className = "paired-device-item";
    item.classList.toggle("selected", isSelected);
    item.dataset.selectDeviceId = device.id;
    item.tabIndex = 0;
    item.setAttribute("role", "button");
    item.setAttribute("aria-current", isSelected ? "true" : "false");

    const summary = document.createElement("div");
    summary.className = "paired-device-summary";

    const title = document.createElement("strong");
    title.textContent = device.name || "Android Phone";

    const status = document.createElement("span");
    status.className = "paired-device-status";
    status.classList.toggle("selected", isSelected);
    status.textContent = isSelected ? "Current" : "Inactive";

    summary.append(title, status);

    const address = document.createElement("div");
    address.className = "paired-device-address";
    address.textContent = formatAddress(device.host, device.port);

    const meta = document.createElement("div");
    meta.className = "paired-device-meta";
    meta.append(
      createDeviceMetaLine("Paired", device.pairedAt),
      createDeviceMetaLine("Last success", device.lastSuccessAt)
    );

    const actions = document.createElement("div");
    actions.className = "paired-device-actions";

    const selectAction = document.createElement("button");
    selectAction.type = "button";
    selectAction.className = "set-current-device";
    selectAction.dataset.selectDeviceId = device.id;
    selectAction.disabled = isSelected;
    selectAction.textContent = isSelected ? "Current" : "Set Current";

    const deleteAction = document.createElement("button");
    deleteAction.type = "button";
    deleteAction.className = "delete-paired-device";
    deleteAction.dataset.deleteDeviceId = device.id;
    deleteAction.textContent = "Delete";
    deleteAction.setAttribute("aria-label", `Delete ${device.name || "Android Phone"}`);

    actions.append(selectAction, deleteAction);

    item.append(summary, address, meta, actions);
    pairedDevicesList.append(item);
  });
}

function createDeviceMetaLine(label, value) {
  const line = document.createElement("span");
  line.textContent = `${label}: ${value ? formatDateTime(value) : "--"}`;
  return line;
}

function formatAddress(host, port) {
  const displayHost = host || "N/A";
  const displayPort = isValidPort(port) ? port : DEFAULT_PORT;
  return `${displayHost}:${displayPort}`;
}

function updateProtectionStatus() {
  protectionStatus.textContent = protectionEnabled ? "On" : "Off";
  protectionStatus.classList.toggle("enabled", protectionEnabled);
  disableProtectionButton.disabled = !protectionEnabled;
}

function updateWebAuthnStatus() {
  webauthnStatus.textContent = hasWebAuthnCredential() ? "Registered" : "Not registered";
  webauthnStatus.classList.toggle("enabled", hasWebAuthnCredential());
}

function updateProtectionMethodOptions() {
  const hasCredential = hasWebAuthnCredential();
  Array.from(protectionMethodSelect.options).forEach((option) => {
    if (requiresWebAuthn(option.value)) {
      option.disabled = !hasCredential;
    }
  });

  if (requiresWebAuthn(protectionMethod) && !hasCredential) {
    protectionMethod = PROTECTION_METHOD_PIN;
    protectionMethodSelect.value = protectionMethod;
    chrome.storage.local.set({ protectionMethod });
  }
}

function saveLastSuccessAt() {
  const now = new Date().toISOString();
  const selectedTarget = getSelectedCommandTarget();

  if (selectedTarget) {
    devices = devices.map((device) => (
      device.id === selectedTarget.deviceId
        ? { ...device, lastSuccessAt: now }
        : device
    ));
    chrome.storage.local.set({ devices });
  } else {
    lastSuccessAt = now;
    chrome.storage.local.set({ lastSuccessAt });
  }

  updateDeviceCard();
  updatePairedDevicesList();
}

function formatDateTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "--";
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
    "Status OK",
    `service: ${service}`,
    `ring_active: ${ring}`,
    `flash_mode: ${flash}`
  ].join("\n");
}

function formatRingStatus(value) {
  if (value === true) {
    return "true (Ringing)";
  }

  if (value === false) {
    return "false (Silent)";
  }

  return "No response";
}

function getHttpErrorMessage(response, body, bodyText) {
  if (response.status === 401) {
    return "Token incorrect or phone token was reset.";
  }

  if (response.status === 404) {
    return "Endpoint not found (404). Check phone service is running.";
  }

  if (response.status === 403 && (body?.message || "").includes("Pairing mode")) {
    return "Enable pairing mode on the phone first.";
  }

  if (response.status >= 500) {
    return `Phone service error (${response.status})。Check phone Local Find service status, or open diagnostics.`;
  }

  const serverMessage = body?.message || body?.error || bodyText || response.statusText || "Request incomplete";
  return `Request failed (${response.status})。${serverMessage}`;
}

function getFriendlyErrorMessage(error) {
  const message = error?.message || "";

  if (message === NETWORK_ERROR_MESSAGE) {
    return message;
  }

  if (message.includes("Failed to fetch") || message.includes("NetworkError")) {
    return NETWORK_ERROR_MESSAGE;
  }

  return message || "Request failed. Check IP, port, and phone service status.";
}

function getWebAuthnErrorMessage(error, fallback) {
  const message = error?.message || "";

  if (message === "WebAuthn not supported in this browser" || message === "Register WebAuthn first") {
    return message;
  }

  if (error?.name === "NotAllowedError") {
    return "WebAuthn cancelled";
  }

  return `${fallback}：${message || error?.name || "Unknown error"}`;
}
