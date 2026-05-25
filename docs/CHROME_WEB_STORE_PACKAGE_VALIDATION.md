# Chrome Web Store Package Validation

Validation date: 2026-05-26

## Purpose

This document records the Chrome Web Store extension zip dry-run and package validation for CWS.18.

CWS.18 does not upload to Chrome Web Store and does not submit the extension for review.

## Zip Output

```text
dist/chrome-web-store/local-find-chrome-extension.zip
```

## Packaging Source

```text
chrome-extension/
```

The zip was generated from an explicit package whitelist:

- `manifest.json`
- `popup.html`
- `popup.css`
- `popup.js`
- `i18n.js`
- `icons/icon-16.png`
- `icons/icon-32.png`
- `icons/icon-48.png`
- `icons/icon-128.png`

The zip root does not contain the `chrome-extension/` directory itself.

## Validation Checklist

| Check | Result | Evidence |
| --- | --- | --- |
| zip exists | PASS | `dist/chrome-web-store/local-find-chrome-extension.zip` exists |
| manifest.json at zip root | PASS | zip entry `manifest.json` present at root |
| manifest.json parses as JSON | PASS | zip entry parsed with PowerShell `ConvertFrom-Json` |
| required extension files present | PASS | manifest, popup files, `i18n.js`, and icon PNGs present |
| forbidden files absent | PASS | no forbidden zip entries found |
| screenshots-draft absent | PASS | no `screenshots-draft/` entry |
| store-assets absent | PASS | no `store-assets/` entry |
| docs absent | PASS | no `docs/` entry |
| Android files absent | PASS | no `android/` entry |
| secrets absent | PASS | no secret files found |
| .git absent | PASS | no `.git/` entry |
| local.properties absent | PASS | no `local.properties` entry |
| app-release.aab absent | PASS | no `app-release.aab` entry |
| .jks/.pem/.key absent | PASS | no `.jks`, `.pem`, or `.key` entries |
| zip size | PASS | 28,952 bytes |

Zip entries:

```text
i18n.js
icons/icon-128.png
icons/icon-16.png
icons/icon-32.png
icons/icon-48.png
manifest.json
popup.css
popup.html
popup.js
```

## Manifest Summary

| Field | Value |
| --- | --- |
| `manifest_version` | `3` |
| `name` | `Local Find` |
| `version` | `0.1.0` |
| `permissions` | `storage` |
| `host_permissions` | `http://*/*` |

`host_permissions` remains the previously documented Chrome Web Store strategy for local HTTP access and was not changed in CWS.18.

## Decision

Package validation result: PASS FOR MANUAL UPLOAD CANDIDATE.

Upload still requires explicit owner approval.

CWS.18 does not upload, submit for review, modify Chrome extension code, modify Android code, change `manifest.json`, change `popup.js`, change `host_permissions`, modify screenshots, move the `mvp-u5-ok` tag, or modify the published GitHub Release.
