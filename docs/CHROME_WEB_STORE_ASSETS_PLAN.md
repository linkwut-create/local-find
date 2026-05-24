# Chrome Web Store Assets Plan

Plan date: 2026-05-25

Scope: asset planning for Chrome Web Store submission readiness. This document is planning-only and does not add icons, screenshots, promotional images, packages, or uploads.

## Required Before Submission

The following assets must be prepared before Chrome Web Store submission:

- 128x128 extension icon.
- At least one Chrome Web Store screenshot.
- Optional promotional tile assets if the store listing strategy needs them.

Current CWS.0 status:

- No extension icon files were found under `chrome-extension/`.
- No Chrome Web Store screenshots were found.
- `chrome-extension/manifest.json` does not currently include an `icons` field.

## Extension Package Icon Plan

Future extension package icon files:

```text
chrome-extension/icons/icon-16.png
chrome-extension/icons/icon-32.png
chrome-extension/icons/icon-48.png
chrome-extension/icons/icon-128.png
```

The icon should be simple at small sizes, visually distinct from the Android app assets if needed, and recognizable as the Local Find browser controller.

This CWS.1 plan does not add real PNG, JPEG, or SVG files.

## Manifest Plan

Future CWS.2 or CWS.3 work can add the extension icon declarations to `chrome-extension/manifest.json` after real icon files exist.

Planned manifest block:

```json
"icons": {
  "16": "icons/icon-16.png",
  "32": "icons/icon-32.png",
  "48": "icons/icon-48.png",
  "128": "icons/icon-128.png"
}
```

Do not modify `manifest.json` during CWS.1.

## Screenshot Plan

Prepare screenshots that show the extension's real browser popup workflow without exposing private data.

Recommended screenshot set:

1. Popup configured with paired device.
2. Popup controller actions.
3. Language switching.
4. Pairing/token explanation if useful.

Minimum submission path:

- Capture one clean screenshot of the popup in a paired or representative configured state.
- Add additional screenshots only if they clarify pairing, local control, or language support.

## Optional Promotional Assets

Promotional tile assets are optional for the current readiness path.

Prepare them only after the required icon and screenshot assets are complete, and only if the Chrome Web Store listing strategy benefits from them.

## Asset Hygiene

Screenshots and promotional images must avoid:

- Private IP addresses if avoidable.
- Real tokens.
- Real device owner names.
- Notification content.
- Private browser page content.
- Exaggerated privacy or security claims.
- Claims that imply cloud tracking, SMS recovery, or background location tracking by the Chrome extension.

Use representative placeholder values when a screenshot needs host, port, device, or pairing information.

## Packaging Impact

When packaging begins in a later CWS phase, include only extension assets required by the Chrome extension package.

Expected package additions once created:

- `chrome-extension/icons/icon-16.png`
- `chrome-extension/icons/icon-32.png`
- `chrome-extension/icons/icon-48.png`
- `chrome-extension/icons/icon-128.png`

Do not include Android release artifacts, signing files, local machine files, or docs unless a later packaging checklist explicitly decides otherwise.
