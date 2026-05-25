# Chrome Web Store Privacy Disclosure Draft

Draft date: 2026-05-25

Scope: Chrome Web Store-specific privacy disclosure draft for the Local Find Chrome extension. This document is planning-only and does not publish a policy, upload a listing, or change extension behavior.

## Public Listing Fields

| Field | Draft value |
| --- | --- |
| Public privacy policy URL | Candidate: `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md`, coverage polish completed in CWS.16, pending final owner reachability check |
| Support email | `linkwut@gmail.com` |

## Short Privacy Summary

Local Find is designed for local-first control of the Local Find Android app from Chrome. The extension stores settings locally in Chrome and communicates with user-entered or paired Android device addresses on the user's local network.

The extension does not read webpage content, access browsing history, access cookies, use the `tabs` permission, or send data to Local Find cloud servers.

## Local Storage Disclosure

The Chrome extension stores settings locally using `chrome.storage.local`.

The extension may store:

- LAN host.
- LAN port.
- Paired device metadata.
- Controller id.
- Control token.
- Optional legacy saved token.
- Language preference.
- Local protection settings.

This storage supports pairing, local control, language selection, and local protection behavior in the extension popup.

## Network Communication Disclosure

The extension communicates with user-entered or paired Local Find Android device addresses on the local network.

Expected communication pattern:

- The user enters a host/port or uses paired local device data.
- The extension sends HTTP requests from the popup to the Local Find Android service address.
- The requests are intended for local network device control.

The extension does not send data to Local Find cloud servers.

## Chrome Data Access Disclosure

The Chrome extension:

- Does not read webpage content.
- Does not access browsing history.
- Does not access cookies.
- Does not use the `tabs` permission.
- Does not use `downloads`, `webRequest`, `scripting`, or `nativeMessaging` permissions.
- Does not declare content scripts.
- Does not declare a background service worker.

## User Control

Users can clear extension storage through Chrome extension/browser storage controls.

Where supported by the Local Find Android app, users can also revoke pairing from the app. Pairing revocation support should be described consistently with the Android app behavior available at submission time.

## Draft Chrome Web Store Disclosure Text

```text
Local Find stores extension settings locally in Chrome using chrome.storage.local. This may include the local network host and port for your Android device, paired device metadata, controller id, control token, optional legacy saved token, language preference, and local protection settings.

The extension communicates with user-entered or paired Local Find Android device addresses on your local network. It does not send data to Local Find cloud servers.

The extension does not read webpage content, access browsing history, access cookies, or use the tabs permission.

You can clear extension storage in Chrome. Where supported by the Local Find Android app, you can revoke pairing from the app.
```

## Open Items Before Submission

- Verify the public privacy policy URL is reachable without login.
- Keep public privacy policy wording aligned with the Android app and Chrome extension behavior.
- Keep the privacy disclosure aligned with the final `host_permissions` decision.

## CWS.5 Public Policy URL Readiness

The public privacy policy URL remains TODO.

Candidate first-pass URL:

```text
https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md
```

CWS.5 read-only coverage check found that `PRIVACY.md` covers the local-first model, no cloud account, no Local Find cloud upload, no SMS, no background location tracking, local storage, LAN host/port, paired device metadata, control tokens, browser local protection settings, and user deletion/revocation paths.

The GitHub `PRIVACY.md` URL may be acceptable for the first readiness pass if the owner chooses it and verifies public reachability before submission.

Final public privacy policy URL requires manual owner decision.

## CWS.15 Contact And Policy Candidate Note

Support email:

```text
linkwut@gmail.com
```

The owner confirmed this email for Chrome Web Store support/contact use.

Root `PRIVACY.md` exists, so the current public privacy policy URL candidate is:

```text
https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md
```

Final URL use still requires owner reachability check without login.

CWS.15 did not modify `PRIVACY.md`. At that point, root `PRIVACY.md` coverage was mostly adequate but still needed explicit Chrome Web Store polish before final upload approval for:

- `chrome.storage.local`;
- no browsing history access;
- no webpage content access;
- no cookies access.

## CWS.16 Public Policy Coverage Note

CWS.16 polished root `PRIVACY.md` for Chrome Web Store privacy coverage.

`PRIVACY.md` now explicitly covers:

- Chrome extension local settings stored with `chrome.storage.local`;
- extension data examples including host, port, paired-device metadata, language preference, local protection settings, and pairing/control token data if configured;
- no browsing history access;
- no webpage content access;
- no cookies access;
- no content script injection into webpages;
- no Local Find cloud server upload from the extension;
- LAN requests to user-entered or paired local Android device addresses;
- user removal, revocation, deletion, and uninstall behavior at a policy level.

Final public URL use still requires an owner reachability check without login.
