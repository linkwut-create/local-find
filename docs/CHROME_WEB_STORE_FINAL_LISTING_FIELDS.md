# Chrome Web Store Final Listing Fields

## Purpose

This document summarizes the current status of Chrome Web Store final listing fields.

CWS.15 does not upload, package, or submit the extension for review.

## Confirmed Fields

- Developer account: registered
- Support email: `linkwut@gmail.com`
- Screenshots: 4 polished 1280x800 en-US screenshots committed

## Candidate Fields

Privacy policy URL candidate:

```text
https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md
```

This candidate is recorded because root `PRIVACY.md` exists.

Current coverage check result: candidate source exists but still needs final owner reachability check and coverage polish before upload approval. `PRIVACY.md` covers Android app, Chrome extension local storage at a general level, local-first / LAN communication, paired device metadata, host/port, token / pairing data, no Local Find cloud server upload, and user deletion / revocation path. It does not explicitly name `chrome.storage.local`, no browsing history access, no webpage content access, or no cookies access.

## Fields Still Needing Final Owner Check

- public privacy policy URL reachability without login
- public privacy policy coverage polish for explicit Chrome Web Store wording
- final extension zip
- final package validation
- final upload approval
- review submission approval

## Do-Not-Upload Note

- CWS.15 does not upload.
- CWS.15 does not submit for review.
- CWS.15 does not generate final extension zip.
