# Local Find

Local-first Android phone finder for trusted nearby devices. Find your phone on the same Wi-Fi — no cloud, no accounts, no location tracking.

## What This Is

- An **Android app** that runs a local HTTP server on port 8888.
- A **Chrome extension** that sends commands over the LAN (flash, stop, find).
- Designed for home, dorm, and office — anywhere you misplace your phone but it's still on the same Wi-Fi.

## What This Is NOT

- Not an anti-theft or stolen-device tracker.
- Not cloud-based. No accounts, no location history, no telemetry.
- No iOS support. Android only.
- No Play Store or Chrome Web Store distribution (debug-signed for now).

## Current Capabilities

| Feature | Description |
|---------|-------------|
| Phone flash | Strobe / steady flashlight via Chrome extension or curl |
| Stop all | Immediate stop for ring and flash |
| One-click find | Ring + flash in sequence (smoke tests use flash only) |
| Pairing mode | Time-limited pairing window on Android |
| Add phone | Chrome extension adds phone via IP + pairing mode acceptance |
| Multi-device list | Save and switch between multiple paired phones |
| Delete with revoke | Deleting a device on Chrome revokes the Android-side paired token |
| Old 8-char token | Admin/fallback token for command endpoints |
| Local protection | Optional PIN or WebAuthn for sensitive Chrome extension actions |

## Quick Start

See the full guide: **[docs/MVP_P_INSTALL_AND_RELEASE.md](docs/MVP_P_INSTALL_AND_RELEASE.md)**

1. **Install the Android APK** — build with Android Studio or `adb install`.
2. **Start the service** — open the app, tap Start Service. Note the 8-char token.
3. **Load the Chrome extension** — `chrome://extensions` → Developer mode → Load unpacked → select `chrome-extension/`.
4. **Enable pairing mode** on the phone.
5. **Enter the phone's IP and port** in the extension popup, click "Check Phone", then "Request Pairing".
6. **Accept** the pairing request on the phone.
7. **Click "Flash"** to start the strobe, **"Stop All"** to stop.

## Repository Structure

```
android/               Android app (Kotlin, Ktor/Netty, Gradle)
chrome-extension/      Chrome extension (Manifest V3, vanilla JS)
docs/                  Design plans, closeouts, install guide
```

## Release Package

The current deliverable package is at:
```
D:\local-find-release\local-find-mvp-p2.zip    (23.4 MB, 12 files)
```

It contains the debug APK, Chrome extension files, and all documentation. Attach this zip to GitHub Releases. See **[docs/MVP_P_RELEASE_CLOSEOUT.md](docs/MVP_P_RELEASE_CLOSEOUT.md)** for the smoke test report.

## Security Model

- **8-character global token** — displayed on the phone screen. Admin/fallback key for all command endpoints.
- **Paired controlToken** — 43-character random token issued per controller on pairing acceptance. Used by the Chrome extension for day-to-day commands.
- **Revocation** — deleting a paired device on Chrome calls `POST /pairing/revoke`. The revoked token immediately returns 401.
- **Chrome storage** — `chrome.storage.local` is plaintext on disk. Not a hardware key store. Pair only on trusted computers.
- **Local protection** — optional PIN (PBKDF2 hashed) or WebAuthn (platform authenticator) for sensitive extension actions. "Stop All" always works without verification.

## Known Limitations

- Must type the phone's LAN IP at least once. No QR code, no auto-discovery.
- Debug APK only. No Play Store signing.
- Chrome extension loaded unpacked. Not on Chrome Web Store.
- Android-only. No iOS.
- Single LAN only. No internet relay.
- No Android UI for managing paired controllers (list/revoke available via API only).
- 8-char token is high-privilege — it can impersonate or revoke any paired controller.

## Documentation

| Document | Contents |
|----------|----------|
| [MVP_P_INSTALL_AND_RELEASE.md](docs/MVP_P_INSTALL_AND_RELEASE.md) | Full install, usage, troubleshooting, smoke test |
| [MVP_P_RELEASE_CLOSEOUT.md](docs/MVP_P_RELEASE_CLOSEOUT.md) | P.3 smoke test results, deliverable status |
| [MVP_L_CLOSEOUT.md](docs/MVP_L_CLOSEOUT.md) | L series feature closeout, security model, backlog |
| [MVP_L_PAIRING_PLAN.md](docs/MVP_L_PAIRING_PLAN.md) | Original L series pairing design plan |
| [chrome-extension/README.md](chrome-extension/README.md) | Chrome extension details |

## Language

The Android app and Chrome extension support **English** and **简体中文**. Select your language from the dropdown in the app or extension header. The browser remote-control page accepts `?lang=en` or `?lang=zh`.

## Security Warnings

- Local Find is intended for **trusted local networks only**.
- **No cloud server** is used. All communication is LAN-only.
- **Do not expose** the HTTP server to the public internet.
- **Pair only trusted devices.** Revoke any device you no longer recognize.
- **Reset the token** if you suspect it has leaked.
- WebAuthn/PIN protects local browser control where available.
- Some Android vendors may restrict background service behavior; battery optimization settings may be required.

## Build & Test

```powershell
# Android
cd android
$env:JAVA_HOME = "D:\android studio\jbr"
.\gradlew.bat assembleDebug
# APK: android\app\build\outputs\apk\debug\app-debug.apk

# Chrome extension static checks
node --check chrome-extension\popup.js
# Load in Chrome: chrome://extensions → Developer mode → Load unpacked → chrome-extension\
```

## Roadmap

- **GitHub Release** with attached zip
- **Release-signed APK** for easier installation
- **UI polish** — Android pairing mode UX, Chrome popup layout
- **QR code / short-code pairing** — no more typing IP addresses
- **LAN auto-discovery** — NSD/mDNS from Chrome
- **Android controller management UI** — list and revoke paired controllers from the phone

## Tags

Key tags for this repository:

| Tag | Description |
|-----|-------------|
| `mvp-l5-ok` | L series pairing + multi-device feature complete |
| `mvp-p1-ok` | Install and release guide |
| `mvp-p4-ok` | Release closeout (smoke test passed) |
