# Chrome Web Store Listing Draft

Draft date: 2026-05-25

Scope: Chrome Web Store listing copy plan for the Local Find Chrome extension. This document is planning-only and does not modify the extension package, Android app, GitHub Release, or Chrome Web Store listing.

## Extension Title

Local Find

## Short Description Draft

Control your Local Find Android phone finder from Chrome on your local network.

## Detailed Description Draft

Local Find is a local-first Android phone finder with a Chrome controller for personal use on your own local network.

Use the Chrome extension with the Local Find Android app to send phone-finder controls from your browser to your paired Android device. The extension is designed for same-network control, not cloud tracking or account-based device management.

Key points:

- Works with the Local Find Android app.
- Controls a paired Android device over the local network.
- Does not require a cloud account.
- Does not use SMS.
- Uses pairing and token-based local control.
- Intended for personal/local use with devices you control.
- Does not claim background location tracking from Chrome.
- Any Android-side behavior should be described precisely in Android app documentation and privacy materials, not overstated in the Chrome listing.

Suggested detailed description:

```text
Local Find is a local-first Android phone finder with a Chrome controller for your local network.

After pairing with the Local Find Android app, the Chrome extension can send local control requests to your Android device, such as phone-finder actions supported by the app. Local Find is intended for personal use with your own devices on a trusted local network.

Local Find does not require a cloud account and does not use SMS. The Chrome extension stores its settings locally in the browser and communicates with the Android app at user-entered or paired local network addresses.

Use this extension only with devices you own or have permission to manage.
```

## Category Recommendation

Recommended primary category: Productivity.

Reasoning:

- Productivity fits a practical personal device-control workflow.
- The extension is a browser controller that helps the user complete a real-world task quickly.
- It avoids presenting the product as a general system utility with broad device-management guarantees.

Alternative category: Utilities.

Tradeoff:

- Utilities also fits because the extension is a small controller/helper.
- Productivity may be clearer for a phone-finder workflow tied to a user task rather than browser/system maintenance.

Recommendation: start with Productivity unless Chrome Web Store category constraints or review feedback make Utilities a better fit.

## Language Strategy

English first.

Simplified Chinese can be added later after the English listing, screenshots, and privacy disclosure are stable.

Do not mix English and Simplified Chinese in the same store listing text. Keep each locale internally consistent.

## Support And Contact Fields

| Field | Draft value |
| --- | --- |
| Support email | TODO |
| Privacy policy URL | TODO |
| GitHub repo URL | `https://github.com/linkwut-create/local-find` |
| GitHub Release URL | `https://github.com/linkwut-create/local-find/releases/tag/mvp-u5-ok` |

Do not modify the existing `mvp-u5-ok` GitHub Release for this listing draft.

## CWS.5 Contact And Privacy Readiness

CWS.5 records support email and public privacy policy URL options in:

- `docs/CHROME_WEB_STORE_CONTACT_AND_PRIVACY_READINESS.md`

The support/contact fields remain TODO until the owner makes final decisions.

Do not submit the Chrome Web Store listing until these fields are final:

- support email;
- public privacy policy URL.

CWS.5 recommends resolving both fields before any package/upload phase begins.

## Pre-Submission Copy Checks

- Avoid claiming cloud sync, account recovery, SMS-based features, or web-page awareness.
- Avoid claiming Chrome performs background location tracking.
- Keep privacy language aligned with the Chrome Web Store privacy disclosure.
- Keep host-permission justification aligned with the manifest permission strategy.
- Use plain, user-facing wording for pairing, token-based local control, and same-network use.
