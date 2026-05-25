# Chrome Web Store Final Listing Fields

## Purpose

This document summarizes the current status of Chrome Web Store final listing fields.

CWS.15 does not upload, package, or submit the extension for review.

## Confirmed Fields

- Developer account: registered
- Support email: `linkwut@gmail.com`
- Screenshots: 4 polished 1280x800 en-US screenshots committed

## Accepted Fields For First Upload Attempt

Privacy policy URL:

```text
https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md
```

This URL is accepted for the first Chrome Web Store upload attempt.

Current coverage check result: candidate exists and coverage polish completed in CWS.16. `PRIVACY.md` covers Android app, Chrome extension local settings stored with `chrome.storage.local`, local-first / LAN communication, paired device metadata, host/port, language preference, local protection settings, token / pairing data when configured, no browsing history access, no webpage content access, no cookies access, no content script injection, no Local Find cloud server upload, and user deletion / revocation / uninstall behavior.

Reachability check result: PASS in CWS.17. The URL returned HTTP 200 OK, required no login, and displayed `PRIVACY.md` content.

## Fields Still Needing Final Owner Check

- final extension zip
- final package validation
- final upload approval
- review submission approval

## Do-Not-Upload Note

- CWS.16 does not upload.
- CWS.16 does not submit for review.
- CWS.16 does not generate final extension zip.
- CWS.17 verified privacy policy URL reachability but still does not upload.
- CWS.17 still requires final upload approval before any Chrome Web Store upload.
