# Privacy

Local Find is designed as a local-first Android phone finder for trusted devices on the same local network.

## Data handling

- Local Find does not require or use a cloud account.
- Local Find does not upload app data, device data, tokens, pairing data, or usage data to a cloud service.
- Local Find does not use SMS.
- Local Find does not use background location tracking.
- Local Find does not provide remote internet relay or cloud-based phone finding.

## Permissions

- Camera access is used only for scanning QR codes during local pairing.
- Local network access is used for local device discovery, pairing, and control between the Android app and trusted controllers on the same LAN.

## Local storage

The following data may be stored locally on the user's own devices:

- Device name
- LAN IP address and port
- Pairing tokens and control tokens
- Saved paired devices
- Local browser protection settings, where supported

This data is stored locally by the Android app and/or Chrome extension. It is not uploaded by Local Find.

## Chrome extension data handling

The Local Find Chrome extension stores local settings using `chrome.storage.local`.

Stored extension data may include:

- Host
- Port
- Paired-device metadata
- Language preference
- Local protection settings
- Pairing token data, if configured
- Control token data, if configured

The Chrome extension:

- Does not access browsing history.
- Does not read webpage content.
- Does not access cookies.
- Does not inject content scripts into webpages.
- Does not upload Local Find data to a Local Find cloud server.

LAN requests are sent to a user-entered or paired local Android device address.

## User control

Users can remove saved devices from the controller. When supported and reachable, deleting a saved controller can also revoke the paired controller token on the Android device.

Users can remove paired devices or clear extension data from browser extension storage.

Users can revoke or delete paired device data in the app where supported.

Uninstalling the Chrome extension removes extension-local data according to browser behavior.

Uninstalling the Android app removes app-local data according to Android behavior.

Users should pair only trusted devices and should reset or revoke tokens if a device is lost, shared, or no longer trusted.

## Scope

Local Find is an MVP testing project. It is not a Play Store production build, not a Chrome Web Store production release, and not an anti-theft or stolen-device recovery service.
