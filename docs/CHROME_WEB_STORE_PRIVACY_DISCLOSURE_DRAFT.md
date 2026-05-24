# Chrome Web Store Privacy Disclosure Draft

Draft date: 2026-05-25

Scope: Chrome Web Store-specific privacy disclosure draft for the Local Find Chrome extension. This document is planning-only and does not publish a policy, upload a listing, or change extension behavior.

## Public Listing Fields

| Field | Draft value |
| --- | --- |
| Public privacy policy URL | TODO |
| Support email | TODO |

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

- Choose and publish a privacy policy URL.
- Choose a support email.
- Ensure the public privacy policy covers both the Android app and Chrome extension behavior, or add a Chrome extension-specific section.
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
