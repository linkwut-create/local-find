# Google Play Listing Draft

Status: Draft material for Google Play Console. Do not upload until Play Console account verification, release QA, and policy review are complete.

## App name

Local Find

## Short description

Local-first Android phone finder with LAN pairing and Chrome extension control.

## Full description

Local Find helps you find a nearby Android phone on the same local network. It is designed for trusted places such as a home, office, dorm, or lab where your phone is nearby but misplaced.

Use Local Find to:

- Find a nearby Android phone over the local network.
- Pair trusted devices with a QR code or local token.
- Trigger ring, flashlight strobe, or stop all actions.
- Control paired phones from a Chrome extension on the same LAN.

Local Find is local-first:

- No cloud account.
- No SMS.
- No background location.
- Local network only.
- No remote internet tracking or relay.

Local Find is not an anti-theft product and is not intended for remote phone recovery. It is a local-network utility for finding a phone you already control.

## Privacy policy summary

Based on `PRIVACY.md`:

- Local Find does not perform server-side data collection.
- Local Find does not upload device data, tokens, pairing data, saved devices, or usage data to a developer-operated server.
- Device names, LAN IP addresses, pairing tokens, control tokens, and saved device records are stored locally on the user's devices.
- Camera access is used only for QR pairing.
- Network permissions are used only for local network discovery, pairing, and control.
- Users can remove saved devices and revoke paired controllers when supported by the paired Android device.

Public privacy policy URL required before Play submission:

- TODO: Add public HTTPS URL for `PRIVACY.md`.

## Data Safety draft

Google Play Console draft answers:

| Question | Draft answer |
|----------|--------------|
| Does the app collect user data? | No, if "collect" means transmitting data off the user's device to the developer or a third party. |
| Does the app share user data? | No. |
| Does the app upload data to a server? | No developer-operated cloud or server upload. |
| Is data encrypted in transit? | Local LAN HTTP is currently cleartext. Do not claim internet transport encryption. |
| Can users delete data? | Yes. Users can delete saved devices locally and revoke paired controllers when reachable. |

Local-only data involved:

- Device name.
- LAN IP address and port.
- Pairing token.
- Control token.
- Saved device records.

Policy note: Local-only storage still needs to be described consistently in the privacy policy and store listing, even if it is not reported as collected server-side data in the Data Safety form.

## Permissions explanation

| Permission | Purpose |
|------------|---------|
| `CAMERA` | Used only to scan QR codes for local pairing. |
| `INTERNET` | Allows local HTTP communication between the Android app and trusted controllers on the LAN. |
| `ACCESS_NETWORK_STATE` | Checks whether network connectivity is available for local control. |
| `ACCESS_WIFI_STATE` | Reads local Wi-Fi/network state needed for LAN discovery and local connection details. |
| `POST_NOTIFICATIONS` | Shows service/status notifications on Android versions that require notification permission. |
| `FOREGROUND_SERVICE` | Keeps the user-started local find service active while the phone is available for LAN control. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Declares the special-use foreground service for a user-started local phone finder service. |
| `USE_BIOMETRIC` | Supports local device authentication for protected actions where available. |
| `WAKE_LOCK` | Helps keep the service responsive while user-started find actions are active. |

## Foreground Service declaration draft

Local Find uses a foreground service so the phone being found can keep a local network listener active after the user starts the service.

Foreground service purpose:

- The user explicitly starts Local Find on the Android phone.
- The phone keeps a local LAN listener available for paired controllers.
- A paired controller can trigger local find actions such as ring, flashlight strobe, and stop all.
- The service is only for finding a nearby phone on the local network.

What the foreground service does not do:

- It does not perform background location tracking.
- It does not perform remote internet tracking.
- It does not upload data to a server.
- It does not use SMS.
- It does not provide anti-theft recovery.

Recommended Play Console evidence:

- Short demo video showing the user opening Local Find, starting the service, pairing locally, triggering a find action, and stopping the action.
- Explanation that the persistent notification represents the active local LAN service.

## Store assets checklist

- 512x512 app icon.
- 1024x500 feature graphic.
- Phone screenshots.
- Optional short demo video.
- Public privacy policy URL.
- Support email.
- App category and tags.
- Content rating questionnaire.
- Data Safety form.
- Foreground service declaration and demo evidence.

## Closed testing checklist

- Create internal testing track for first upload and smoke validation.
- Create closed testing track before production access.
- If required by the Play account policy, recruit at least 12 testers for at least 14 days.
- Provide tester instructions:
  - Install the Android app from the test track.
  - Start the Local Find service on the phone.
  - Pair a trusted controller on the same LAN.
  - Test ring, flashlight strobe, and stop all.
  - Delete saved devices and revoke paired controllers.
  - Report device model, Android version, network type, and any failure screenshots.
- Do not request production access until the closed testing requirement and policy forms are complete.
