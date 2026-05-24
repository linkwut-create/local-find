# Chrome Web Store Manual Screenshot Capture

Scope: CWS.11A manual clean-profile screenshot capture instructions. This document does not commit screenshots, generate an extension zip, upload to Chrome Web Store, modify Chrome extension code, modify Android code, change `host_permissions`, move tags, or modify the `mvp-u5-ok` GitHub Release.

## Purpose

This is the manual screenshot capture procedure for Chrome Web Store readiness.

Screenshots should be captured in a separate clean Chrome profile so the final listing assets do not include personal browser state, unrelated extensions, private tabs, real device identifiers, real network identifiers, or notifications.

CWS.11A does not submit images, generate a zip, or upload to Chrome Web Store.

## Clean Chrome Profile Setup

1. Open the Chrome profile selector.
2. Create a new profile, for example `Local Find Screenshots`.
3. Do not sign into a personal Google account unless necessary.
4. Disable or avoid unrelated extensions.
5. Open:

```text
chrome://extensions
```

6. Enable Developer mode.
7. Click Load unpacked and select:

```text
D:\local-find\chrome-extension
```

8. Pin the Local Find extension if needed.
9. Open the Local Find popup.
10. Set the UI language to English.

## Demo Data Setup

Use safe demo values:

| Field | Value |
| --- | --- |
| Device name | `Demo Phone` |
| Paired device name | `Local Find Phone` |
| Host | `192.168.1.108` |
| Port | `8888` |
| Token | blank, masked, or not visible |
| Controller id | not visible |

Do not show:

- real device name;
- real LAN IP;
- real token;
- real controller id;
- private browser tab;
- personal account identifier;
- OS or browser notification.

## Screenshot Checklist

Before each screenshot, confirm:

- DevTools closed;
- no private browser tab visible;
- no notification visible;
- no real IP visible;
- no real device name visible;
- no real token visible;
- UI language is English;
- crop area is consistent;
- popup is fully visible.

## Target Screenshots

Prepare four English-first screenshots:

1. `01-popup-paired-device.png`

   Show the popup configured with a paired or demo device.

2. `02-controller-actions.png`

   Show ring, flash, stop, and status controls.

3. `03-pairing-or-manual-host.png`

   Show the manual host/port or pairing setup area.

4. `04-language-switching.png`

   Show the language selector.

## Temporary Output Location

Save draft screenshots outside the repository first, for example:

```text
D:\local-find-screenshots-draft\
```

Do not save initial drafts directly to:

```text
store-assets/chrome-web-store/screenshots/en-US/
```

Reasons:

- allow manual review first;
- avoid accidentally committing images that contain private data;
- move approved files into the store-assets path only in a later CWS.11B screenshot commit phase.

## Review Before Commit

Before screenshots are committed:

- owner sends screenshots for review;
- check privacy leakage;
- check visual clarity;
- check language consistency;
- check that no real device or network identifiers are visible;
- only after review, commit approved screenshots to the store-assets path.

## CWS.11A Boundary

CWS.11A does not:

- commit screenshots;
- generate an extension zip;
- upload to Chrome Web Store;
- change Chrome extension code;
- change `chrome-extension/manifest.json`;
- change `host_permissions`.
