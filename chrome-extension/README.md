# Local Find Chrome Extension MVP-K.6

This directory contains the minimal Chrome extension control entry for the Local Find Android HTTP service.

K.6 focuses on the home, dorm, and office scenario: before going out, when the Android phone cannot be found nearby, open the computer's Chrome extension and click one primary action to make the phone ring and flash. On a trusted private computer, the user can explicitly choose to remember the token and protect sensitive actions with a local PIN, WebAuthn system verification, or WebAuthn with local PIN fallback.

## Scope

- Adds only a Chrome extension popup.
- Uses the existing Android HTTP protocol.
- Does not implement pairing, QR codes, Native Messaging, a local PC service, cloud services, accounts, location, or background scanning.
- Does not persist the token by default.

## Files

- `manifest.json` - Manifest V3 extension declaration.
- `popup.html` - Popup UI.
- `popup.js` - Request handling and local host/port persistence.
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
4. Enter the Android device host, port, and current token.
5. On a trusted private computer only, optionally check `记住此电脑上的 Token`.
6. Recommended path: open the extension, click `一键找手机`, let the phone ring and flash, then click `停止全部` after finding the phone.
7. Use `开始闪光` when visual feedback is useful.
8. Use `打开诊断页` to open the Android service page in the browser.

`一键找手机` is equivalent to starting ring plus strobe flash in sequence.

The token field is `type=password`; POST requests read the token only from that input. The token is not written to console and is never placed in URLs.

The host field accepts plain hosts such as `192.168.1.108` and also tolerates pasted URLs such as `http://192.168.1.108:8888/`. Host and port are saved in `chrome.storage.local`.

The current device card shows host, port, token saved status, and the last successful control command time. It stores only `lastSuccessAt`, not command logs and not token history.

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

- Token is not saved by default.
- Token is saved only after the user checks `记住此电脑上的 Token`.
- Use token saving only on a trusted private computer.
- Do not save the token on public computers or shared computers.
- Use `清除已保存 Token` to clear the input, remove `savedToken`, and turn off `rememberToken`.
- If the phone token is reset, enter the new token and check `记住此电脑上的 Token` again if you still want this computer to remember it.

## Troubleshooting

- Make sure the computer and Android phone are on the same Wi-Fi.
- Make sure the Local Find service is running on the Android phone.
- Check that the host is the phone's current LAN IP address.
- Check that the port is correct. The default is `8888`.
- If a command returns 401, enter the current phone token again. The phone token may have been reset, and any saved token should be updated or cleared.
- If status or commands fail, open the browser diagnostic page from the popup and confirm the Android service responds.
- If `一键找手机` cannot run, check host, port, token, Android service status, and that the computer and phone are on the same Wi-Fi.
