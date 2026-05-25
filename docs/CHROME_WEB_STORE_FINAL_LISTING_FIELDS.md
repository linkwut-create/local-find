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

Current coverage check result: candidate exists and coverage polish completed in CWS.16. `PRIVACY.md` covers Android app, Chrome extension local settings stored with `chrome.storage.local`, local-first / LAN communication, paired device metadata, host/port, language preference, local protection settings, token / pairing data when configured, no browsing history access, no webpage content access, no cookies access, no content script injection, no Local Find cloud server upload, and user deletion / revocation / uninstall behavior.

Final owner reachability check is still required before using the candidate URL for upload approval.

## Fields Still Needing Final Owner Check

- public privacy policy URL reachability without login
- final extension zip
- final package validation
- final upload approval
- review submission approval

## Do-Not-Upload Note

- CWS.16 does not upload.
- CWS.16 does not submit for review.
- CWS.16 does not generate final extension zip.
