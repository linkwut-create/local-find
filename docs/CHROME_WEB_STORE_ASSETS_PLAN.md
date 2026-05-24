# Chrome Web Store Assets Plan

Plan date: 2026-05-25

Scope: asset planning for Chrome Web Store submission readiness. CWS.3 added readiness icons and the manifest `icons` field; screenshots, promotional images, packages, and uploads remain out of scope.

## Required Before Submission

The following assets must be prepared before Chrome Web Store submission:

- 128x128 extension icon. Added in CWS.3.
- At least one Chrome Web Store screenshot.
- Optional promotional tile assets if the store listing strategy needs them.

Current CWS.3 status:

- Extension package icons exist under `chrome-extension/icons/`.
- No Chrome Web Store screenshots were found.
- `chrome-extension/manifest.json` includes an `icons` field.

## Extension Package Icon Plan

Extension package icon files added in CWS.3:

```text
chrome-extension/icons/icon-16.png
chrome-extension/icons/icon-32.png
chrome-extension/icons/icon-48.png
chrome-extension/icons/icon-128.png
```

The icon should remain simple at small sizes, visually distinct from the Android app assets if needed, and recognizable as the Local Find browser controller.

The CWS.3 icons are readiness icons and can be replaced later if a final brand-design pass is desired.

## Manifest Plan

CWS.3 added the extension icon declarations to `chrome-extension/manifest.json`.

Current manifest block:

```json
"icons": {
  "16": "icons/icon-16.png",
  "32": "icons/icon-32.png",
  "48": "icons/icon-48.png",
  "128": "icons/icon-128.png"
}
```

Do not change unrelated manifest fields when replacing or redesigning icons later.

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

Prepare them only after the required screenshot assets are complete, and only if the Chrome Web Store listing strategy benefits from them.

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

Expected package icon files:

- `chrome-extension/icons/icon-16.png`
- `chrome-extension/icons/icon-32.png`
- `chrome-extension/icons/icon-48.png`
- `chrome-extension/icons/icon-128.png`

Do not include Android release artifacts, signing files, local machine files, or docs unless a later packaging checklist explicitly decides otherwise.
