# Chrome Web Store Submission Status

Status date: 2026-05-26

## Purpose

This document records the Chrome Web Store manual upload and review submission status for Local Find.

CWS.20 does not modify Android code, Chrome extension code, the extension zip, screenshots, privacy policy URL, listing fields, tags, or the published GitHub Release.

## Submission Status

| Field | Value |
| --- | --- |
| Chrome Web Store submission status | pending review / 待审核 |
| Submitted extension | Local Find |
| Extension ID | `nadcejbdnkaihkgddojlokjcfdak` |
| Upload package previously validated | `dist/chrome-web-store/local-find-chrome-extension.zip` |
| Submission method | Manual owner action in Chrome Web Store Developer Dashboard |

The upload and Submit review action were performed manually by the owner, not by Codex.

## Review-Pending Boundary

No further package, manifest, screenshot, privacy policy, or listing field changes should be made while Chrome Web Store review is pending unless Chrome Web Store requests changes.

If the extension is approved:

- record the approval status;
- record the public or unlisted Chrome Web Store URL;
- keep any release/tag changes separate unless explicitly approved.

If Chrome Web Store rejects the submission or requests changes:

- capture the exact Chrome Web Store message first;
- do not modify package, listing, privacy policy, permissions, screenshots, or code until the message is recorded and the next change scope is explicitly approved.

## Frozen Items

- `mvp-u5-ok` tag remains frozen.
- Published GitHub Release remains unchanged.
- `dist/chrome-web-store/local-find-chrome-extension.zip` is not modified in CWS.20.
- `chrome-extension/manifest.json` is not modified in CWS.20.
- `chrome-extension/popup.js` is not modified in CWS.20.
- Chrome Web Store upload is not repeated in CWS.20.
- Chrome Web Store review submission is not repeated in CWS.20.
