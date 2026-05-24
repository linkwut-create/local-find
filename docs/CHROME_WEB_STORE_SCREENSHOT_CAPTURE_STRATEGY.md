# Chrome Web Store Screenshot Capture Strategy

Decision date: 2026-05-25

Scope: CWS.10 screenshot capture strategy and demo-state checklist for Chrome Web Store readiness. This document does not generate final screenshots, commit PNG/JPEG files, create an extension zip, upload to Chrome Web Store, modify Chrome extension code, modify Android code, change `host_permissions`, move tags, or modify the `mvp-u5-ok` GitHub Release.

## Decision

Chosen strategy: use a separate clean Chrome profile for final screenshot capture.

Do not use current owner/tester screenshots as store assets.

Do not use screenshots containing:

- real device name;
- real LAN IP;
- real token;
- controller id;
- private browser content.

## Why Clean Chrome Profile

A separate clean Chrome profile is the preferred screenshot capture strategy because it:

- avoids personal browser state;
- avoids unrelated extensions;
- avoids private tabs;
- avoids current real paired-device data;
- reduces risk of leaking account identifiers or notifications;
- produces cleaner store screenshots.

## Clean Profile Setup Checklist

Before final screenshot capture:

1. Create or open a separate Chrome profile for Local Find screenshot capture.
2. Do not sign into a personal Google account if not necessary.
3. Disable unrelated extensions.
4. Open only neutral tabs.
5. Load unpacked extension from:

```text
D:\local-find\chrome-extension
```

6. Pin Local Find extension if needed.
7. Set UI language to English for first screenshot set.
8. Do not connect to a real personal paired phone unless explicitly approved.
9. Use safe demo values.

## Safe Demo Values

Use the following demo state for screenshot setup:

| Field | Safe demo value |
| --- | --- |
| Device name | `Demo Phone` |
| Paired device name | `Local Find Phone` |
| Host | `192.168.1.108` |
| Port | `8888` |
| Token | blank, masked, or not visible |
| Controller id | not visible |
| Last connected | avoid if possible; if visible, use neutral/demo-looking date only |

## Screenshot Target Set

Target directory:

```text
store-assets/chrome-web-store/screenshots/en-US/
```

Target English screenshot files:

```text
01-popup-paired-device.png
02-controller-actions.png
03-pairing-or-manual-host.png
04-language-switching.png
```

Full planned paths:

```text
store-assets/chrome-web-store/screenshots/en-US/01-popup-paired-device.png
store-assets/chrome-web-store/screenshots/en-US/02-controller-actions.png
store-assets/chrome-web-store/screenshots/en-US/03-pairing-or-manual-host.png
store-assets/chrome-web-store/screenshots/en-US/04-language-switching.png
```

## Before-Capture Checklist

Before each final screenshot:

- Chrome profile is clean.
- UI language is English.
- No real IP visible.
- No real device name visible.
- No real token visible.
- No controller id visible.
- No personal tab/account/notification visible.
- DevTools closed.
- Popup UI visible.
- Window scale consistent.
- Screenshot crop area consistent.

## CWS.10 Decision Boundary

CWS.10 does not:

- generate final screenshots;
- commit PNG/JPEG files;
- generate an extension zip;
- upload to Chrome Web Store;
- modify extension code.

## Next Phase Recommendation

Recommended next phase:

- CWS.11: capture and commit safe English screenshot assets.

Alternative if the owner does not want image assets committed yet:

- CWS.11: capture screenshots outside the repository and verify manually first.
