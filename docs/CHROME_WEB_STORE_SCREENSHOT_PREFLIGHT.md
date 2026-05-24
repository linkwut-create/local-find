# Chrome Web Store Screenshot Preflight

Preflight date: 2026-05-25

Scope: CWS.9 safety preflight before real Chrome Web Store screenshot capture. This document does not commit real screenshots, generate PNG/JPEG assets, create an extension zip, upload to Chrome Web Store, modify Chrome extension code, modify Android code, change `host_permissions`, move tags, or modify the `mvp-u5-ok` GitHub Release.

## Purpose

This is the safety check before capturing real Chrome Web Store screenshots.

The goal is to avoid putting real device names, real IP addresses, tokens, controller ids, pairing secrets, or private browser content into store listing screenshots.

CWS.9 does not commit real screenshots.

## Current Observed Risk

Current owner/tester validation shows:

- Owner/tester can open the Local Find popup.
- Existing manual screenshots show the popup UI rendering normally.
- Existing manual screenshots show real device name and real LAN IP.
- These screenshots should not be used directly as Chrome Web Store listing assets.

Current owner screenshots are validation evidence only, not store assets.

## Sensitive Fields To Avoid

Do not include any of the following in Chrome Web Store screenshots:

- real LAN IP;
- real public IP;
- real device name;
- real owner name;
- real phone model if the user does not want it public;
- real token;
- control token;
- controller id;
- pairing secret;
- Wi-Fi SSID;
- phone number;
- browser tabs with private content;
- notifications;
- unrelated extensions or account identifiers.

## Recommended Safe Demo Values

Use safe demo values when the UI needs visible device or network fields:

| Field | Recommended value |
| --- | --- |
| Device name | `Demo Phone` |
| Paired device name | `Local Find Phone` |
| Host | `192.168.1.108` |
| Port | `8888` |
| Token | blank, masked, or not visible |
| Controller id | not visible |
| Last connected | use neutral/demo date only if already shown by UI; otherwise avoid relying on timestamps |

## Capture Strategy Options

### Option A: Use Current UI But Redact Or Crop Sensitive Values After Capture

Pros:

- Fastest.

Cons:

- Risk of missed sensitive data.
- Edited screenshots may look less clean.
- Redaction/cropping may reduce confidence that the screenshot represents the real UI.

Use only if screenshots can be cropped cleanly and all sensitive values are fully removed.

### Option B: Clear Extension Storage And Manually Enter Safe Demo Values

Pros:

- Cleaner screenshot.
- Less redaction risk.
- More realistic than heavy post-capture editing.

Cons:

- May lose current paired-device state unless backed up.
- Requires careful setup before capture.

Use when a separate Chrome profile is not practical.

### Option C: Use A Separate Chrome Profile For Screenshot Capture

Pros:

- Cleanest.
- Avoids personal browser state.
- Avoids unrelated extensions/tabs.
- Reduces risk of leaking account identifiers, tabs, notifications, or real paired devices.

Cons:

- Requires setup.
- May need reloading the unpacked extension and demo configuration.

Recommended default if practical.

## Recommendation

Prefer Option C if practical.

Otherwise use Option B.

Avoid Option A unless only cropped screenshots are needed and sensitive values are fully removed.

## Manual Pre-Capture Checklist

Before capturing final store screenshots:

- Use clean Chrome profile or clean test environment.
- Load unpacked from `D:\local-find\chrome-extension`.
- Pin extension if needed.
- Use English UI for first screenshot set.
- Confirm no real paired phone info is visible.
- Confirm no real token/controller id is visible.
- Confirm no private browser tab content is visible.
- Confirm DevTools is closed for store screenshots unless explicitly documenting validation.
- Use consistent window size and scaling.
- Capture only the extension popup / relevant browser area.
- Do not capture OS notifications.

## Screenshots Still Expected

Expected first English screenshot set from CWS.4:

```text
01-popup-paired-device.png
02-controller-actions.png
03-pairing-or-manual-host.png
04-language-switching.png
```

Target paths remain:

```text
store-assets/chrome-web-store/screenshots/en-US/01-popup-paired-device.png
store-assets/chrome-web-store/screenshots/en-US/02-controller-actions.png
store-assets/chrome-web-store/screenshots/en-US/03-pairing-or-manual-host.png
store-assets/chrome-web-store/screenshots/en-US/04-language-switching.png
```

Do not add these files until the owner chooses a safe capture strategy and approves real screenshot capture.

## CWS.9 Decision

- Do not commit current owner screenshots as store assets.
- Do not generate final screenshot assets in CWS.9.
- Proceed to CWS.10 only after owner chooses screenshot capture strategy.

## CWS.10 Decision

CWS.10 selected the final screenshot capture strategy:

- use a separate clean Chrome profile for final screenshot capture.

Decision record:

- `docs/CHROME_WEB_STORE_SCREENSHOT_CAPTURE_STRATEGY.md`

The demo-state checklist moved to the CWS.10 strategy document.

Current owner/tester screenshots remain validation evidence only and must not be used as store assets because they include real device name and real LAN IP.

CWS.10 does not generate final screenshots, commit PNG/JPEG files, generate a zip, upload to Chrome Web Store, or modify extension code.

## Remaining Blockers Before Upload

Chrome Web Store upload remains blocked by:

- real Chrome Web Store screenshots still missing;
- support email still TODO;
- public privacy policy URL still TODO / owner decision;
- final extension zip not generated;
- Chrome Web Store developer account/upload not done.
