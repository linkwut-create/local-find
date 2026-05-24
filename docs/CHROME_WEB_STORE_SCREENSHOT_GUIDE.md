# Chrome Web Store Screenshot Guide

Guide date: 2026-05-25

Scope: CWS.4 screenshot capture kit for the Local Find Chrome extension. This guide plans screenshot capture only. It does not add real screenshots, generate an extension zip, upload to Chrome Web Store, change Android code, change Chrome extension functionality, or change `host_permissions`.

## Purpose

Prepare a repeatable screenshot plan for Chrome Web Store listing readiness.

The screenshots should show the Local Find Chrome extension as a local-first browser controller for the Local Find Android app, with emphasis on paired or user-entered local network control.

## Current Status

- Chrome extension package icons were added in CWS.3.
- `chrome-extension/manifest.json` includes the `icons` field.
- `host_permissions` remains `http://*/*`.
- No Chrome Web Store screenshots have been captured or committed.
- CWS.4 creates screenshot directories and placeholder files only.
- CWS.4 does not generate real PNG/JPEG/SVG screenshot assets.

## Screenshot Scenarios

Prepare the English screenshot set first.

### 1. `01-popup-paired-device.png`

Goal: show the Chrome popup configured with a paired device.

Message to communicate:

- Local Find Chrome controller can target a paired Android device.

Recommended capture state:

- Popup open.
- Paired Phones section visible.
- A demo paired device selected.
- Demo local address shown with safe placeholder-style values.

### 2. `02-controller-actions.png`

Goal: show ring, flash, stop, and related control buttons.

Message to communicate:

- Popup-initiated local control.

Recommended capture state:

- Popup open.
- Find Phone and Stop All Alerts controls visible.
- Secondary actions such as Flash, Stop Flash, Status, and Diagnostics visible if layout allows.
- No unrelated browser or desktop notifications visible.

### 3. `03-pairing-or-manual-host.png`

Goal: show pairing, host, and port configuration.

Message to communicate:

- Works with user-entered or paired local network Android address.

Recommended capture state:

- Add Phone or Manual Host/Token section visible.
- Demo host and port values only.
- No real token.
- No real controller id.

### 4. `04-language-switching.png`

Goal: show English and Simplified Chinese language switching.

Message to communicate:

- Extension supports English and Simplified Chinese UI.

Recommended capture state:

- Language selector visible.
- Use this screenshot to show the language selector, not to mix two locales in the same screenshot set.

## Language Strategy

- English screenshots are the default first listing set.
- `zh-CN` screenshots may be prepared later as localized listing assets.
- Do not mix English and Chinese within the same screenshot set.
- Do not add marketing overlays unless they are localized consistently.
- If localized overlays are added later, keep wording aligned with the store listing and privacy disclosure.

## Screenshot Hygiene Checklist

Before capture and before committing final screenshots, confirm:

- No real token.
- No real controller id.
- No real personal device owner name.
- No private phone number.
- No real Wi-Fi SSID.
- Avoid real public IP.
- Prefer demo LAN-looking values if needed.
- No browser tabs showing private content.
- No unrelated extensions.
- No notification popups.
- No exaggerated privacy/security claims.
- No claims that Chrome extension performs cloud tracking, SMS recovery, or background location tracking.

Suggested demo values:

- Host: `192.168.1.108` or another non-personal demo LAN-looking value.
- Port: `8888`.
- Device name: `Demo Phone` or `Local Find Phone`.
- Token fields: blank, masked, or omitted.

## File Naming Convention

Default English screenshot set:

```text
store-assets/chrome-web-store/screenshots/en-US/01-popup-paired-device.png
store-assets/chrome-web-store/screenshots/en-US/02-controller-actions.png
store-assets/chrome-web-store/screenshots/en-US/03-pairing-or-manual-host.png
store-assets/chrome-web-store/screenshots/en-US/04-language-switching.png
```

Optional later Simplified Chinese screenshot set:

```text
store-assets/chrome-web-store/screenshots/zh-CN/01-popup-paired-device.png
store-assets/chrome-web-store/screenshots/zh-CN/02-controller-actions.png
store-assets/chrome-web-store/screenshots/zh-CN/03-pairing-or-manual-host.png
store-assets/chrome-web-store/screenshots/zh-CN/04-language-switching.png
```

Do not commit final screenshot files until a later CWS phase explicitly approves real screenshot capture.

## Pre-Upload Checklist

Before Chrome Web Store upload:

- Final English screenshots exist in `store-assets/chrome-web-store/screenshots/en-US/`.
- Optional localized screenshots, if prepared, exist in `store-assets/chrome-web-store/screenshots/zh-CN/`.
- All screenshots follow the hygiene checklist.
- Screenshot language matches the listing locale.
- Screenshots show the actual extension UI, not speculative mockups.
- No private user data or real secrets appear.
- No host-permission, privacy, cloud, SMS, or background-location claims contradict the listing or privacy disclosure.
- Required package icons remain under `chrome-extension/icons/`.
- No extension zip is generated until the packaging phase explicitly starts.
- No Chrome Web Store upload is performed until separately approved.
