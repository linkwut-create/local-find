# Chrome Web Store Local Unpacked Validation

Validation record date: 2026-05-25

Scope: CWS.7 local unpacked Chrome extension validation record and manual checklist. This stage uses documentation and read-only checks only. It does not launch Chrome automatically, generate a zip, upload to Chrome Web Store, modify Android code, modify Chrome extension functionality, change `host_permissions`, move tags, or modify the `mvp-u5-ok` GitHub Release.

## Purpose

This is the local unpacked extension validation record before any Chrome Web Store upload.

The goal is to confirm that the `chrome-extension/` directory has the required local loading test inputs and a clear manual validation checklist.

CWS.7 does not:

- upload to Chrome Web Store;
- generate an extension zip;
- change code;
- modify `chrome-extension/manifest.json`;
- modify `chrome-extension/popup.js`;
- perform real Android phone control.

## Repository Baseline

Command:

```text
git status --short
```

Result:

```text

```

Command:

```text
git describe --tags --dirty
```

Result:

```text
mvp-u5-ok-17-g2439539
```

Command:

```text
git log --oneline -5
```

Result:

```text
2439539 docs: plan Chrome extension package dry run
2d0f119 docs: plan Chrome Web Store contact and privacy readiness
4b6fb2d docs: plan Chrome Web Store screenshots
9eb2824 chore: add Chrome extension icons
cb6edab docs: decide Chrome Web Store host permission strategy
```

## Static Validation

Read-only static validation was performed against `chrome-extension/`.

| Check | Result |
| --- | --- |
| `chrome-extension/manifest.json` parses as JSON | Pass |
| `manifest_version` | `3` |
| `name` exists | `Local Find` |
| `version` exists | `0.1.0` |
| `description` exists | `Minimal popup controller for the Local Find Android HTTP service.` |
| `action.default_popup` | `popup.html` |
| `icons` field exists | Pass |
| `icons/icon-16.png` exists | Pass |
| `icons/icon-32.png` exists | Pass |
| `icons/icon-48.png` exists | Pass |
| `icons/icon-128.png` exists | Pass |
| `popup.html` exists | Pass |
| `popup.js` exists | Pass |
| `popup.css` exists | Pass |
| `i18n.js` exists | Pass |
| `permissions` | `storage` only |
| `host_permissions` | `http://*/*` |
| `background.service_worker` | absent |
| `content_scripts` | absent |
| `tabs` permission | absent |
| `history` permission | absent |
| `cookies` permission | absent |
| `webRequest` permission | absent |
| `scripting` permission | absent |
| `nativeMessaging` permission | absent |
| Remote script loading | not found |

`popup.html` loads local extension scripts:

```html
<script src="i18n.js"></script>
<script src="popup.js"></script>
```

The `http://HOST:8888` text in `popup.html` is endpoint preview UI text, not a remote script.

## Manual Chrome Validation Checklist

Manual validation should be performed by the owner/tester in Chrome Developer Mode:

1. Open Chrome.
2. Navigate to `chrome://extensions`.
3. Enable Developer mode.
4. Click Load unpacked.
5. Select:

```text
D:\local-find\chrome-extension
```

6. Confirm the extension loads successfully.
7. Confirm the extension icon appears.
8. Click the extension popup.
9. Confirm the popup opens.
10. Confirm the language selector is visible.
11. Confirm host/port or paired-device UI is visible.
12. Open DevTools for the popup console.
13. Confirm there is no syntax/runtime error.
14. Do not perform real phone control unless the user explicitly requests an Android integration test.

## Expected Result

Expected local unpacked validation result:

- Load unpacked succeeds.
- Popup opens.
- Icon appears.
- UI renders.
- No console syntax error.
- No unexpected permission prompt beyond manifest permissions.

## Known Not Tested In CWS.7

CWS.7 does not test:

- real Android phone control;
- real pairing flow;
- Chrome Web Store upload;
- extension zip upload;
- screenshots capture;
- support email finalization;
- privacy URL finalization.

## Remaining Blockers Before Upload

Chrome Web Store upload remains blocked by:

- real Chrome Web Store screenshots still missing;
- support email still TODO;
- public privacy policy URL still TODO / owner decision;
- final extension zip not generated;
- Chrome Web Store developer account/upload not done.

## Next Step

Recommended next phase:

- CWS.8: owner-assisted manual unpacked validation result capture, or final screenshot/contact/privacy resolution before any packaging/upload step.

## CWS.8 Manual Validation Result

### Manual Validation Summary

Manual validation was completed by the owner/tester.

Recorded observations:

- Chrome unpacked extension loaded manually by owner/tester.
- Popup opened successfully.
- Popup UI rendered.
- Language selector visible.
- Host/port and paired-device UI visible.
- Popup DevTools Console opened successfully.
- Console showed no red errors.
- Console showed no yellow warnings.
- DevTools showed No Issues.
- No real Android phone control was performed.
- No real pairing flow was performed.

### Result

Manual unpacked validation result: PASS

### Evidence Notes

- Validation was based on owner-provided screenshots and observation.
- Screenshot showed popup DevTools at `chrome-extension://.../popup.html`.
- Console panel was empty.
- DevTools header showed No Issues.
- Earlier popup screenshot showed Local Find UI rendering with current device card, control buttons, paired phone section, and language selector.

### Known Limitations

- This does not prove Chrome Web Store acceptance.
- This does not test uploaded zip behavior.
- This does not test real Android control.
- This does not test fresh-user first-run flow.
- This does not test reviewer environment.
- This does not resolve screenshots/support email/privacy URL blockers.

### Remaining Blockers Before Upload

Chrome Web Store upload remains blocked by:

- Real Chrome Web Store screenshots still missing.
- Support email still TODO.
- Public privacy policy URL still TODO / owner decision.
- Final extension zip not generated.
- Chrome Web Store developer account/upload not done.
