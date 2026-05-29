# Google Play Real Screenshot Capture TODO

Created: 2026-05-29

Status: **BLOCKED** — cannot auto-capture in current environment. Owner must capture manually.

## Why this exists

PLAY.2D-R (`docs/GOOGLE_PLAY_SCREENSHOT_REVIEW.md`) found the 4 AI-generated screenshots at `store-assets/google-play/screenshots/en-US/` are inaccurate vs the actual app UI. This file documents how to replace them with real captures.

## Environment limitation

The current machine has no Android SDK (`ANDROID_HOME` / `ANDROID_SDK_ROOT` not set, `adb` not found). Cannot build `installDebug`, launch an emulator, or capture real device screenshots automatically. `gradlew` exists but is unusable without the SDK.

## What the owner needs to do

### Prerequisites

- Android Studio installed (or standalone Android SDK + emulator)
- The `D:\local-find` project open in Android Studio
- A device emulator with API 26+ (e.g., Pixel 6, API 34) or a physical Android device connected via USB with USB debugging enabled

### Capture steps

#### 1. Build and install

```bash
cd android
./gradlew installDebug
```

Or in Android Studio: Run > Run 'app' (debug, on emulator or device).

#### 2. Set English UI

In the app, tap the language dropdown (top bar) and select "English" (not "System" and not "简体中文") so all screenshots are in English.

#### 3. Capture screenshot 01: Find Me / service start

1. Navigate to the **Find Me** tab.
2. Tap **Start Service**.
3. Wait for the service status to show "Running" (green indicator).
4. Scroll down slightly to show the Service Status card (IP, port, etc.).
5. Capture screenshot.
6. Save as `store-assets/google-play/screenshots/en-US/01-phone-service-start.png`.

#### 4. Capture screenshot 02: Controller connected

1. On a second device/browser, open the LAN URL shown in the Find Me tab and pair as a controller.
2. Navigate to the **Controller** tab.
3. Tap the paired device to open the **RemoteControlPanel** (full-screen).
4. Verify the connection status bar shows green "Online".
5. Capture screenshot showing the Find Phone / Stop buttons and device info.
6. Save as `store-assets/google-play/screenshots/en-US/02-controller-connected.png`.

> If no second device is available, capture the Controller tab showing the "Saved Devices" or "Device Discovery" section as a fallback. Replace the filename if the content changes significantly.

#### 5. Capture screenshot 03: QR pairing

1. Navigate to the **Controller** tab.
2. Tap **Scan QR Code** (this opens the `QrScannerScreen` full-screen camera viewfinder).
3. Point the camera at any QR code (a printed test code or another screen) — the QR content does not matter because it's just a demo; the scanner overlay and corner brackets are what need to be shown.
4. Capture screenshot showing the viewfinder, dimmed overlay, corner brackets, and "Align QR code inside the frame" text.
5. Save as `store-assets/google-play/screenshots/en-US/03-qr-pairing.png`.

> Ensure the QR code being scanned is a mock/test code, not a real pairing credential.

#### 6. Capture screenshot 04: Language settings

1. In the **TopAppBar**, tap the language dropdown.
2. The dropdown menu shows: **System**, **English**, **简体中文**.
3. Capture screenshot while the dropdown is expanded.
4. Save as `store-assets/google-play/screenshots/en-US/04-language-settings.png`.

> Do NOT show unsupported languages. The real app only has 3 options.

### Post-capture

1. Verify each PNG is portrait (1080x1920 preferred; at minimum, crop/resize to portrait phone aspect ratio).
2. Privacy check (review the actual captured data):
   - [ ] No real IP address visible
   - [ ] No real device name visible (use "Demo Controller" or mock names)
   - [ ] No token visible (mask or hide pairing tokens)
   - [ ] No personal notification, email, Google account, phone number
   - [ ] No browser tabs visible
   - [ ] QR code content is mock/test only
3. Run: `git add store-assets/google-play/screenshots/en-US/*.png`
4. Update this file to mark the TODO as DONE.
5. Update `PROJECT_STATUS.md` and related docs to reflect PLAY.2D2 completion.
6. Commit with message: `assets: replace Google Play screenshots with real app captures`

## The AI-generated files that need replacing

These 4 files exist but are BLOCKED — replace them with real captures:

| File | Current status |
|------|---------------|
| `store-assets/google-play/screenshots/en-US/01-phone-service-start.png` | AI-generated — LOW accuracy |
| `store-assets/google-play/screenshots/en-US/02-controller-connected.png` | AI-generated — LOW accuracy |
| `store-assets/google-play/screenshots/en-US/03-qr-pairing.png` | AI-generated — MEDIUM accuracy |
| `store-assets/google-play/screenshots/en-US/04-language-settings.png` | AI-generated — CRITICAL inaccuracy |

## Constraints

- Do NOT use AI to generate replacement screenshots.
- Do NOT modify AndroidManifest.xml during this step.
- Do NOT build release AAB.
- Do NOT upload to Google Play until all 4 real screenshots are committed.
