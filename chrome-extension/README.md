# Local Find Chrome Extension MVP-K.0

This directory contains the minimal Chrome extension control entry for the Local Find Android HTTP service.

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
5. Use the popup buttons to call the existing HTTP endpoints.

The token field is `type=password`; it is only read from the popup input when sending POST commands and is not written to storage or console.
