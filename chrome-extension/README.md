# Local Find Chrome Extension MVP-K.2

This directory contains the minimal Chrome extension control entry for the Local Find Android HTTP service.

K.2 focuses on the home, dorm, and office scenario: before going out, when the Android phone cannot be found nearby, use the computer's Chrome extension to make the phone ring or flash quickly. On a trusted private computer, the user can explicitly choose to remember the token.

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
6. Click `开始响铃` to make the phone ring, then click `停止全部` after finding it.
7. Use `开始闪光` when visual feedback is useful.
8. Use `打开诊断页` to open the Android service page in the browser.

The token field is `type=password`; POST requests read the token only from that input. The token is not written to console and is never placed in URLs.

The host field accepts plain hosts such as `192.168.1.108` and also tolerates pasted URLs such as `http://192.168.1.108:8888/`. Host and port are saved in `chrome.storage.local`.

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
