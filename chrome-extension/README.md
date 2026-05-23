# Local Find Chrome Extension MVP-L.4

This directory contains the minimal Chrome extension control entry for the Local Find Android HTTP service.

L.4 adds remote revocation: deleting a paired phone in the extension now calls the Android `/pairing/revoke` endpoint first so the phone-side paired token is invalidated. The user still enters a phone IP address when pairing a new phone, confirms the request on the phone, and then selects a saved paired phone as the command target.

## Scope

- Adds only a Chrome extension popup.
- Uses the existing Android HTTP protocol.
- Supports adding one Android phone through phone-side pairing mode and confirmation.
- Shows historical paired phones from `chrome.storage.local.devices[]`.
- Supports switching `selectedDeviceId` from the paired-phone list.
- Does not implement automatic discovery, QR codes, Native Messaging, a local PC service, cloud services, accounts, location, or background scanning.
- Keeps legacy host/port/token storage for compatibility.

## Files

- `manifest.json` - Manifest V3 extension declaration.
- `popup.html` - Popup UI.
- `popup.js` - Request handling, pairing flow, paired-device list rendering, switching, local host/port compatibility, and selected-device persistence.
- `popup.css` - Popup styling.

## Permissions

The extension declares:

- `storage` so host and port can be saved in `chrome.storage.local`.
- `http://*/*` host permission so the popup can call an Android device at a user-entered LAN host and port.

It does not request `history`, `cookies`, `tabs`, `scripting`, `webRequest`, `clipboardRead`, or `<all_urls>`.

## Usage

1. Open `chrome://extensions`.
2. Enable Developer mode.
3. Load unpacked and select this `chrome-extension` directory.
4. On the Android phone, open Local Find and enable computer plugin pairing mode.
5. In the popup's `添加手机` section, enter the phone host and port, then click `检查手机`.
6. Click `请求配对`, then accept the request on the phone.
7. After acceptance, the extension stores the phone in `devices[]` and uses it as `selectedDeviceId`.
8. Use `已配对手机` to review saved paired phones and switch the current target.
9. Recommended path: open the extension, click `一键找手机`, let the phone ring and flash, then click `停止全部` after finding the phone.
10. Use `开始闪光` when visual feedback is useful.
11. Use `打开诊断页` to open the Android service page in the browser.

`一键找手机` is equivalent to starting ring plus strobe flash in sequence.

The legacy token field is `type=password`; legacy POST requests read the token only from that input. Paired-device POST requests use the saved per-device `controlToken`. Tokens are not written to console and are never placed in URLs.

The host field accepts plain hosts such as `192.168.1.108` and also tolerates pasted URLs such as `http://192.168.1.108:8888/`. Host and port are saved in `chrome.storage.local`.

The current device card shows the selected paired device name, `host:port`, pairing/token status, and per-device `lastSuccessAt`. If no selected paired device is available, it shows the current manual host/port mode.

The `已配对手机` list reads from `chrome.storage.local.devices[]`. Each item shows name, `host:port`, `pairedAt`, `lastSuccessAt`, and whether it is the current target. Clicking an item or `设为当前` updates `selectedDeviceId`; later commands use that device token when host, port, and token are available.

The `删除` button revokes the Android-side paired token via `POST /pairing/revoke` before removing the device from Chrome extension local storage. If the phone is offline or the revoke request fails, the user can choose to delete only the local record. If the device record is missing `controllerId` (pre-L.4 pairing), revoke is skipped and only local deletion is performed.

## Pairing

- L.3 keeps manual seed discovery: the user enters the phone IP address and port when adding a phone.
- `检查手机` calls `GET /device-info` and shows device name, device id, `pairingMode`, and service state.
- If `pairingMode=false`, the popup prompts the user to enable computer plugin pairing mode in the phone app.
- `请求配对` calls `POST /pairing/request` with the persistent `controllerId`, `controllerName: "Chrome on Windows"`, `controllerType: "chrome_extension"`, and a nonce.
- The popup polls `GET /pairing/status?requestId=...` until the request is accepted, rejected, or expired.
- On acceptance, the popup saves or updates the device in `chrome.storage.local.devices[]`, sets `selectedDeviceId`, and refreshes the paired-phone list.
- L.3 provides local paired-phone deletion in the Chrome extension. L.4 adds remote revocation via `POST /pairing/revoke` during deletion so the Android-side paired token is invalidated.

## Local Protection PIN

- The local PIN protection lock is a Chrome extension local PIN, not the Windows or macOS system password.
- PIN is not saved in plain text. The extension uses Web Crypto PBKDF2 with a generated salt and saves `localPinSalt`, `localPinHash`, and `protectionEnabled`.
- Protection is off by default. Setting a PIN turns `protectionEnabled` on.
- After protection is enabled, `一键找手机`, `开始闪光`, saved-token changes, and `清除已保存 Token` require the selected protection method. `停止全部` stays immediately available so the user can stop the phone after finding it.
- After protection is enabled, every sensitive action requires fresh verification.
- Closing the protection lock requires the selected protection method.

## Protection Methods

- K.6 supports three protection methods: local PIN, system verification, and system verification with local PIN fallback.
- Local PIN remains the default, so existing users continue with the K.4 behavior.
- System verification uses WebAuthn / the browser platform authenticator. It is not the Windows or macOS login password.
- Registration or verification may trigger Windows Hello or another platform authenticator, depending on the browser and operating system.
- System verification stores only the minimal credential reference fields, including `webauthnEnabled`, `webauthnCredentialId`, and `protectionMethod`.
- It does not save any system password and does not connect WebAuthn to any network service.
- Every sensitive action triggers a fresh verification; no verified state is saved.
- `停止全部` always stays immediately available and does not require PIN or system verification.

## Token Storage

- Legacy Token is not saved by default.
- Legacy Token is saved only after the user checks `记住此电脑上的 Token`.
- Use token saving only on a trusted private computer.
- Do not save the token on public computers or shared computers.
- Use `清除已保存 Token` to clear the input, remove `savedToken`, and turn off `rememberToken`.
- If the phone token is reset, enter the new token and check `记住此电脑上的 Token` again if you still want this computer to remember it.
- Paired-device `controlToken` is saved in `devices[]` after phone-side acceptance so commands can target the selected paired phone.
- The folded `高级：手动 Host/Token 兼容模式` section is only a fallback for old 8-character Token compatibility or temporary debugging. Paired devices are preferred whenever the selected device has host, port, and token.

## Troubleshooting

- Make sure the computer and Android phone are on the same Wi-Fi.
- Make sure the Local Find service is running on the Android phone.
- Before pairing, enable computer plugin pairing mode in the Android app and confirm the incoming request on the phone.
- Check that the host is the phone's current LAN IP address.
- Check that the port is correct. The default is `8888`.
- If a command returns 401, enter the current phone token again. The phone token may have been reset, and any saved token should be updated or cleared.
- If status or commands fail, open the browser diagnostic page from the popup and confirm the Android service responds.
- If `一键找手机` cannot run, check host, port, token, Android service status, and that the computer and phone are on the same Wi-Fi.
