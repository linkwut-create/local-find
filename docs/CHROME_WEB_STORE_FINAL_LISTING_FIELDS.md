# Chrome Web Store Final Listing Fields

## Purpose

This document summarizes the current status of Chrome Web Store final listing fields.

CWS.15 does not upload, package, or submit the extension for review.

## Confirmed Fields

- Developer account: registered
- Support email: `linkwut@gmail.com`
- Screenshots: 4 polished 1280x800 en-US screenshots committed
- Final extension zip candidate: `dist/chrome-web-store/local-find-chrome-extension.zip`
- Manual upload checklist: `docs/CHROME_WEB_STORE_MANUAL_UPLOAD_CHECKLIST.md`
- Chrome Web Store submission status: pending review / 待审核
- Chrome Web Store extension ID: `nadcejbdnkaihkgddojlokjcfdak`

## Accepted Fields For First Upload Attempt

Privacy policy URL:

```text
https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md
```

This URL is accepted for the first Chrome Web Store upload attempt.

Current coverage check result: candidate exists and coverage polish completed in CWS.16. `PRIVACY.md` covers Android app, Chrome extension local settings stored with `chrome.storage.local`, local-first / LAN communication, paired device metadata, host/port, language preference, local protection settings, token / pairing data when configured, no browsing history access, no webpage content access, no cookies access, no content script injection, no Local Find cloud server upload, and user deletion / revocation / uninstall behavior.

Reachability check result: PASS in CWS.17. The URL returned HTTP 200 OK, required no login, and displayed `PRIVACY.md` content.

## Current Review Status

- Owner manually uploaded and submitted the extension for review in Chrome Web Store Developer Dashboard.
- Submitted extension: Local Find.
- Current status: pending review / 待审核.
- Codex did not perform the upload or Submit review action.
- No further package/listing changes should be made while review is pending unless Chrome Web Store requests changes.
- If approved, record the approval and public or unlisted Chrome Web Store URL.
- If rejected or changes are requested, capture the exact Chrome Web Store message before modifying anything.

## Do-Not-Upload Note

- CWS.16 does not upload.
- CWS.16 does not submit for review.
- CWS.16 does not generate final extension zip.
- CWS.17 verified privacy policy URL reachability but still does not upload.
- CWS.17 still requires final upload approval before any Chrome Web Store upload.
- CWS.18 generated and validated `dist/chrome-web-store/local-find-chrome-extension.zip` as a manual upload candidate.
- CWS.18 still requires explicit owner approval before any Chrome Web Store upload.
- CWS.19 prepared `docs/CHROME_WEB_STORE_MANUAL_UPLOAD_CHECKLIST.md`.
- CWS.19 does not grant upload approval; upload still requires an explicit owner command.
- CWS.20 records owner manual upload and Submit review action. Current Chrome Web Store status is pending review / 待审核.
- CWS.20 does not modify package, listing, manifest, screenshots, privacy policy URL, tag, or GitHub Release.
