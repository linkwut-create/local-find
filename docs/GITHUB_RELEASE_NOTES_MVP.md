# GitHub Release Notes — Local Find MVP

## Release Title

**Local Find MVP — Local-first Android Phone Finder**

## Suggested Tag

`mvp-r2-ok` (or the latest tag at release time)

## Release Asset

Upload the following zip as the release attachment:

```
D:\local-find-release\local-find-mvp-p2.zip    (23.4 MB, 12 files)
```

This is a local file path. When creating the GitHub Release, drag or select this file as the binary attachment.

## Summary

Local Find is a local-first tool for finding a misplaced Android phone on the same Wi-Fi network. It consists of an Android app that runs a local HTTP server and a Chrome extension that sends commands (flash, stop) over the LAN.

- **No cloud accounts.** Everything runs on your local network.
- **No location tracking.** The phone's physical location is never collected.
- **No iOS.** Android only.
- **Not anti-theft.** This is a "where did I leave my phone" tool, not a stolen-device tracker.

## Included in Package

```
android/LocalFind-Android-debug.apk       Android debug APK
chrome-extension/                          Chrome extension (unpacked)
docs/                                      Install guide, closeouts, plan
RELEASE_MANIFEST.txt                       Release manifest
SHA256SUMS.txt                             SHA256 checksums
```

## Key Features

- **Phone flash** — strobe or steady flashlight triggerable from Chrome or curl.
- **Stop all** — immediately stop ring and flash.
- **One-click find** — ring + flash in sequence.
- **Pairing mode** — time-limited Android pairing window for adding trusted controllers.
- **Multi-device list** — save multiple paired phones in the Chrome extension.
- **Delete with revocation** — deleting a paired device calls `POST /pairing/revoke` so the Android-side paired token is invalidated. If the phone is offline, the user can choose local-only deletion.
- **8-character fallback token** — displayed on the phone screen. Works as admin/fallback for all command endpoints.
- **Local protection** — optional PIN (PBKDF2 hashed) or WebAuthn (platform authenticator) for sensitive Chrome extension actions. "Stop All" always works without verification.

## Installation (Short)

See the full guide: `docs/MVP_P_INSTALL_AND_RELEASE.md` inside the zip.

1. **Install the APK** — `adb install -r LocalFind-Android-debug.apk` or open the file on the phone.
2. **Start the service** — open the Local Find app, grant camera permission, tap Start Service. Note the 8-character token on screen.
3. **Load the Chrome extension** — `chrome://extensions` → Developer mode → Load unpacked → select `chrome-extension/`.
4. **Enable pairing mode** on the phone (5-minute window).
5. **Enter the phone's IP and port** in the extension popup (e.g., `192.168.1.108`, port `8888`).
6. **Click "Check Phone"**, then **"Request Pairing"**.
7. **Accept** the pairing request on the phone.
8. Use **"Flash"** to strobe the flashlight, **"Stop All"** to stop.

## Smoke Test Status (P.3)

All tests passed against MEIZU 21 running the release APK.

| # | Test | Result |
|---|------|--------|
| 1 | `GET /ping` | `{"ok":true}` |
| 2 | `GET /device-info` | Device identity returned |
| 3 | Old token curl flash | `{"success":true}` |
| 4 | Old token curl stop-all | `{"success":true}` |
| 5 | Paired controlToken flash | `{"success":true}` |
| 6 | Old token revoke | `{"ok":true,"revoked":true}` |
| 7 | Revoked controlToken flash | `401` |
| 8 | Old token flash (after revoke) | `{"success":true}` |
| 9 | Old token stop-all (after revoke) | `{"success":true}` |

Static checks: `popup.js` syntax OK, `manifest.json` parse OK, zip excluded-file audit clean.
Flash only; ring excluded from test scope.

## Known Limitations

- **Debug APK** — not Play Store signed. Install via adb or file manager with "allow unknown sources".
- **Unpacked Chrome extension** — loaded in Developer mode. Not on Chrome Web Store.
- **Manual IP entry** — must type the phone's LAN IP at least once. No QR code or auto-discovery yet.
- **Android-only** — no iOS support.
- **Single LAN** — phone and computer must be on the same Wi-Fi. No internet relay.
- **Chrome storage is plaintext** — `chrome.storage.local` is not encrypted at rest. Pair only on trusted computers.
- **8-char token is high-privilege** — it can impersonate or revoke any paired controller.
- **No Android controller management UI** — paired controllers can be listed and revoked via API, but there is no UI for this on the phone yet.

## Checksums

Full SHA256 checksums are in `SHA256SUMS.txt` inside the zip. Verify with:

```
certutil -hashfile LocalFind-Android-debug.apk SHA256
```

Key entry:
```
A263CA7EEAE54B0670164A33B860931C0CA8E157F5784B38A967D440B3E126B2  android/LocalFind-Android-debug.apk
```

## Recommended Next Work

- **Release-signed APK** — for easier installation without "unknown sources" friction.
- **UI polish** — Android pairing mode UX, Chrome popup layout improvements.
- **QR code / short-code pairing** — no more typing IP addresses.
- **LAN auto-discovery** — NSD/mDNS from Chrome to find phones without knowing the IP.
- **Android controller management UI** — list and revoke paired controllers from the phone.

## References

- Full install guide: `docs/MVP_P_INSTALL_AND_RELEASE.md`
- Release closeout: `docs/MVP_P_RELEASE_CLOSEOUT.md`
- L series feature closeout: `docs/MVP_L_CLOSEOUT.md`
- Repository: `README.md`
