# Changelog

## Local Find 1.1.0 - 2026-09-24

Release candidate build. `versionCode 3`, `versionName 1.1.0`, `targetSdk 36`
(Android 16 / API 36 compatible). Not uploaded to Google Play or the Chrome
Web Store by this build; see `docs/release/1.1.0-readiness.md` for the
upload steps and current status.

### Highlights

- **Background/lock-screen findability**: the app now remembers the user's
  explicit choice to keep the finder service running and restores it after
  reboot, an app update, the launcher task being dismissed, or the system
  killing the service — reopening the app also repairs a stopped service.
  Explicit "Stop Service" clears that choice.
- **Changed-IP recovery**: a paired phone's foreground service periodically
  broadcasts its current LAN address (no control token in the payload). The
  Chrome extension verifies a candidate address against the phone's
  persistent device ID via `/device-info` before trusting it — first through
  an optional local discovery bridge (mDNS + beacon + bounded subnet scan),
  then a same-/24 in-popup scan as a fallback.
- **Identity-bound trust**: saved device tokens and addresses are now keyed
  by the phone's persistent device ID, not by host:port. A DHCP change can no
  longer silently hand a stale token's trust to whatever device later
  occupies the old IP.
- **NSD hardening for Android 16**: service registration now retries with
  backoff, holds a multicast lock, and drops the trailing dot in the service
  type that Android 16's resolver rejects.
- **Rewritten background-permission guidance** (English/Chinese) covering
  OEM auto-start/anti-freeze settings (Xiaomi, Huawei, Oppo, Meizu) and the
  "stop then start once" step needed after changing them.
- **Release signing fixed**: the keystore path is now resolved relative to
  the (ASCII) `android/` project root instead of an absolute path, which
  Java reads as ISO-8859-1 and mangled this repository's non-ASCII directory
  name.
- **Chrome extension version aligned to `1.1.0`** (was `0.1.0`); new
  `net-utils.js` unit-testable via `node --test` (9 tests, no new
  dependency) for the address-recovery pure functions.

### Package scope

- Android release AAB, signed with the same upload key already registered
  with Play App Signing (see readiness doc for hashes).
- Chrome extension as unpacked Manifest V3 extension, also packaged as
  `local-find-release/local-find-chrome-extension-1.1.0.zip` for manual
  loading/inspection (not submitted anywhere by this build).
- No cloud account, SMS, background location, or internet relay.

## Local Find MVP-U.5 - 2026-05-24

GitHub prerelease published: https://github.com/linkwut-create/local-find/releases/tag/mvp-u5-ok

### Release identity

- Tag: `mvp-u5-ok`
- Title: Local Find MVP-U.5
- Asset: `local-find-mvp-u5.zip`
- SHA256: `81764E96AD9648CCC3369F54CDB6113DCFB342BEBC9A42D314337B2EB59FB371`

### Highlights

- Added language switching: System, English, and Simplified Chinese.
- Added localized strings for Android, Chrome extension, and browser control page.
- Improved Chrome popup layout.
- Improved Android Find Me service controls.
- Improved Android Controller saved-device management.
- Added QR scanner framing UI.
- Preserved LAN-only pairing, token control, revoke, and stop-all behavior.

### Package scope

- Android debug APK for MVP testing.
- Chrome extension as unpacked Manifest V3 extension.
- README, docs, release manifest, and SHA256SUMS.
- No cloud account, SMS, background location, or internet relay.

This is an MVP testing release, not a Play Store production build.
