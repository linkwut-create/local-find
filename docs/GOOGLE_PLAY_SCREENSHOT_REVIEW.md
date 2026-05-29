# Google Play Screenshot Accuracy Review

Review date: 2026-05-29

Phase: PLAY.2D-R — Accuracy and policy review of AI-generated screenshots.

## Review basis

The 4 screenshots at `store-assets/google-play/screenshots/en-US/` were generated via Imagen (AI image generation). This review compares them against the actual Android app UI code at `android/app/src/main/java/io/github/linkwutcreate/localfind/ui/MainScreen.kt` and `LocalFindStrings.kt`.

Google Play policy (per [Best practices for your store listing](https://support.google.com/googleplay/android-developer/answer/13393723)): screenshots and graphic assets must **accurately reflect app functionality**. AI-generated screenshots are acceptable only if they accurately represent the actual app experience. Misleading screenshots risk rejection.

---

## File audit

| # | File | Dimensions | Format | Size | Alpha | Portrait |
|---|------|-----------|--------|------|-------|----------|
| 1 | `01-phone-service-start.png` | 1080x1920 | PNG RGB | ~1.06 MB | No | Yes |
| 2 | `02-controller-connected.png` | 1080x1920 | PNG RGB | ~1.48 MB | No | Yes |
| 3 | `03-qr-pairing.png` | 1080x1920 | PNG RGB | ~1.34 MB | No | Yes |
| 4 | `04-language-settings.png` | 1080x1920 | PNG RGB | ~1.26 MB | No | Yes |

All 4 files exist, are 1080x1920 portrait PNG with no alpha — format compliance passes.

---

## Privacy audit

| Check | 01 | 02 | 03 | 04 |
|-------|----|----|----|-----|
| No real IP | Pass (none shown) | Pass (192.168.1.100 — mock) | Pass | Pass |
| No real device name | Pass | Pass ("My Laptop" — generic) | Pass | Pass |
| No token/credential | Pass | Pass | Pass (mock QR) | Pass |
| No Google account/email/phone | Pass | Pass | Pass | Pass |
| No private notification | Pass | Pass | Pass | Pass |
| No browser tabs/pages | Pass | Pass | Pass | Pass |
| No real QR credential | N/A | N/A | Pass (mock pattern) | N/A |

Privacy check: **ALL PASS**. No real personal data, credentials, or identifiers.

---

## Accuracy audit (vs actual Android app code)

### Screenshot 01: Service Start

| Aspect | Generated screenshot | Actual app (`FinderModeScreen`) | Match? |
|--------|---------------------|-------------------------------|--------|
| Theme | Dark navy, custom | System theme (light or dark), Material 3 default | **PARTIAL** — only accurate in device dark mode |
| Title | "Local Find" + subtitle | "Local Find" in TopAppBar, no subtitle | **MISMATCH** — no subtitle in real app |
| Tabs | "Find Me, Devices, Settings" | "Find Me" / "Controller" (2 tabs only) | **MISMATCH** — 3 tabs vs 2, "Devices" vs "Controller", "Settings" tab does not exist |
| Start button | Large circular play button | Standard Material 3 `Button` with "Start Service" text | **MISMATCH** — different button style |
| Content density | Minimal (1 button + status) | Very dense: How to Use, Service Controls, Status, Pairing, QR Code, Security, Background, Device Test | **MISMATCH** — missing most real UI |

### Screenshot 02: Controller Connected

| Aspect | Generated screenshot | Actual app (`ControllerModeScreen` / `RemoteControlPanel`) | Match? |
|--------|---------------------|----------------------------------------------------------|--------|
| Theme | Dark navy | System theme | **PARTIAL** |
| Screen title | "Devices" | "Local Find" (same TopAppBar as always) | **MISMATCH** — TopAppBar title stays "Local Find" |
| Device card | Simple card: "My Laptop — Connected" + "Revoke Access" | Complex `RemoteControlPanel`: connection status bar, Find Phone/Stop buttons, device info, authorization, hardware controls | **MISMATCH** — generated is oversimplified; missing the actual RemoteControlPanel layout |
| Green status dot | Present | Real app has colored status bar (green/blue/orange/red) spanning full card width | **MISMATCH** — different visual treatment |

### Screenshot 03: QR Scanner

| Aspect | Generated screenshot | Actual app (`QrScannerScreen`) | Match? |
|--------|---------------------|-------------------------------|--------|
| Camera viewfinder | Present | Present (CameraX preview) | **OK** |
| Corner brackets | Present | Present (Canvas-drawn white brackets) | **OK** |
| Dimmed overlay | Present | Present (45% alpha black) | **OK** |
| Hint text | "Scan QR code to pair a new controller" | "Align QR code inside the frame" | **MISMATCH** — different text |
| Manual entry button | "Enter code manually" | No such button on scanner screen; manual entry is on the controller tab | **MISMATCH** — button doesn't exist on scanner |
| QR code displayed | Mock QR in viewfinder | Real QR scanned by camera, not shown as overlay | **MISMATCH** — QR isn't overlaid on scanner; it's captured by camera |

### Screenshot 04: Language Settings

| Aspect | Generated screenshot | Actual app (TopAppBar dropdown) | Match? |
|--------|---------------------|-------------------------------|--------|
| UI pattern | **Full-screen settings page** with back arrow | **Dropdown menu** in TopAppBar | **CRITICAL MISMATCH** — completely different UI |
| Title | "Language" | "Local Find" (TopAppBar unchanged) | **MISMATCH** |
| Language count | 5: English, 简体中文, 日本語, 한국어, Español | 3: System, English, 简体中文 | **CRITICAL MISMATCH** — 5 vs 3 languages |
| "System" option | Not shown | Present and selectable | **MISMATCH** — missing "System" option |
| "日本語", "한국어", "Español" | Shown | **Do not exist** in `LocalFindStrings.kt` | **CRITICAL MISMATCH** — advertising languages the app does not support |

---

## Overall accuracy verdict

| Screenshot | Accuracy |
|------------|----------|
| 01-phone-service-start | **LOW** — wrong tabs, missing most UI content, wrong button style |
| 02-controller-connected | **LOW** — wrong title, oversimplified device card, missing RemoteControlPanel |
| 03-qr-pairing | **MEDIUM** — visual elements close but text and button wrong |
| 04-language-settings | **CRITICAL** — entirely wrong UI pattern, advertises languages the app does not support |

---

## Verdict: BLOCKED

**The 4 AI-generated screenshots do NOT accurately represent the actual Local Find Android app UI.** They show an idealized, simplified, and in some cases (language screen) entirely fabricated interface.

### Specific blocking issues

1. **Screenshot 04 advertises 3 languages the app does not support** (日本語, 한국어, Español). This is a clear policy violation — Google Play prohibits misleading metadata.
2. **Wrong navigation structure**: generated screenshots show 3 tabs (Find Me, Devices, Settings); the real app has 2 (Find Me, Controller).
3. **Missing UI density**: the real app is far more functional and dense than the screenshots suggest, possibly under-representing its capabilities.
4. **Theme inconsistency**: the dark navy theme is only accurate when the device is in dark mode.

### Next step

**PLAY.2D2**: Capture real screenshots from an Android emulator or physical device running the actual app. Specific capture targets:

| # | Screen | How to reach |
|---|--------|-------------|
| 1 | Find Me tab — service started, showing status | Start service in FinderModeScreen, scroll to show service status card |
| 2 | Remote Control Panel — connected to device | Connect to a paired device, show Find Phone + hardware control cards |
| 3 | QR Scanner — camera active | Tap "Scan QR Code" on controller tab, show viewfinder with corner brackets |
| 4 | Language dropdown — expanded | Tap language selector in TopAppBar, show dropdown with System/English/简体中文 |

### Policy note

Google Play requires: "Your store listing, including the title, description, icon, screenshots, and feature graphic, must accurately reflect your app or game's content and functionality." ([Best practices for your store listing](https://support.google.com/googleplay/android-developer/answer/13393723))

Do NOT upload the current AI-generated screenshots to Play Console. Replace with real captures before PLAY.5 (internal testing upload).

---

## Remaining PLAY.2 blockers

| # | Blocker | Status |
|---|---------|--------|
| 4 | App icon | Resolved (PLAY.2B) |
| 5 | Feature graphic | Resolved (PLAY.2C) |
| 6 | Phone screenshots | **BLOCKED — replace with real captures (PLAY.2D2)** |
| — | Manifest launcher icon update | Deferred (PLAY.2E) |

---

## Constraints (this review)

- Did not modify Android code.
- Did not modify Chrome extension code.
- Did not build APK/AAB.
- Did not upload to Google Play.
- Did not replace screenshots.
- Did not modify AndroidManifest.xml.
