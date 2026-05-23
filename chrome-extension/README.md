# Local Find Chrome Extension MVP-K.1

This directory contains the minimal Chrome extension control entry for the Local Find Android HTTP service.

K.1 focuses on the home, dorm, and office scenario: before going out, when the Android phone cannot be found nearby, use the computer's Chrome extension to make the phone ring or flash.

## Scope

- Adds only a Chrome extension popup.
- Uses the existing Android HTTP protocol.
- Does not implement pairing, QR codes, Native Messaging, a local PC service, cloud services, accounts, location, or background scanning.
- Does not persist the token.

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
5. Click `开始响铃` to make the phone ring, then click `停止全部` after finding it.
6. Use `开始闪光` when visual feedback is useful.
7. Use `打开诊断页` to open the Android service page in the browser.

The token field is `type=password`; it is only read from the popup input when sending POST commands and is not written to storage or console.

The host field accepts plain hosts such as `192.168.1.108` and also tolerates pasted URLs such as `http://192.168.1.108:8888/`. Host and port are saved in `chrome.storage.local`; the token is not saved.

## Troubleshooting

- Make sure the computer and Android phone are on the same Wi-Fi.
- Make sure the Local Find service is running on the Android phone.
- Check that the host is the phone's current LAN IP address.
- Check that the port is correct. The default is `8888`.
- If a command returns 401, enter the current phone token again. The phone token may have been reset.
- If status or commands fail, open the browser diagnostic page from the popup and confirm the Android service responds.
