# MVP-P Install & Release Guide

Current version: `mvp-l5-ok`

## 1. What This Is

Local Find is a minimal Android + Chrome Extension tool for finding a misplaced phone on the same LAN. The phone runs an HTTP server; the Chrome extension sends commands (flash, stop) over Wi-Fi. No cloud, no accounts, no location tracking.

Two components:
- **Android app** — HTTP server on port 8888, flash control, pairing mode.
- **Chrome extension** — popup UI, paired-device list, one-click find/flash/stop.

## 2. Android APK

### Build

```powershell
cd D:\local-find\android
$env:JAVA_HOME = "D:\android studio\jbr"
.\gradlew.bat assembleDebug
```

### APK location

```
android\app\build\outputs\apk\debug\app-debug.apk
```

### Install on phone

```powershell
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

Or transfer the APK to the phone and open it in a file manager.

### First launch

1. Open the **Local Find** app on the phone.
2. Grant camera permission (needed for flashlight).
3. Grant notification permission (Android 13+).
4. Tap **Start Service**. The app shows a foreground notification with the phone's LAN IP and port.
5. The app displays an **8-character Token** — note this. It is your admin/fallback key.
6. Optionally, disable battery optimization for the app to keep the service alive.

### Service lifecycle

- The service runs as a foreground service with a persistent notification.
- The notification shows the current LAN IP and port (e.g., `192.168.1.108:8888`).
- A 15-second watchdog auto-restarts the server if it crashes.
- The service acquires a partial wake lock and Wi-Fi lock.
- To stop, tap **Stop Service** in the app or swipe away the notification.

## 3. Chrome Extension

### Load

1. Open `chrome://extensions` in Chrome.
2. Enable **Developer mode** (toggle, top right).
3. Click **Load unpacked**.
4. Select the `chrome-extension` directory:
   ```
   D:\local-find\chrome-extension
   ```
5. The **Local Find** extension appears in your extensions list.

### Files

```
chrome-extension/
  manifest.json   — Manifest V3
  popup.html      — Popup UI
  popup.js        — All logic: commands, pairing, devices, protection
  popup.css       — Styling
  README.md       — Extension-specific docs
```

### Permissions

- `storage` — save paired devices and settings in `chrome.storage.local`.
- `http://*/*` — call the Android HTTP server on user-entered LAN addresses.
- No `tabs`, `history`, `cookies`, `scripting`, or `<all_urls>`.

### Pin the extension

Click the puzzle icon in Chrome toolbar, find Local Find, click the pin icon. The popup is now one click away.

## 4. First-Time Pairing

### Prerequisites

- Phone and computer on the **same Wi-Fi / LAN**.
- Phone: Local Find service running.
- Phone: **pairing mode enabled** (open the app, tap "Enable Pairing Mode").

### Steps

1. **Find the phone's IP**
   - On the phone, the Local Find notification shows the IP (e.g., `10.128.21.95`).
   - Or check the app's main screen.

2. **Open Chrome extension popup**
   - In the "Add Phone" section, enter the phone's IP and port (default `8888`).
   - Tip: you can paste `http://10.128.21.95:8888/` — the extension parses it.

3. **Click "Check Phone"**
   - Confirms the phone is reachable, shows device name, ID, and pairing mode status.
   - If it says `pairingMode: false`, enable pairing mode on the phone first.

4. **Click "Request Pairing"**
   - Sends a pairing request with your extension's unique `controllerId` and a nonce.

5. **Accept on the phone**
   - The phone shows the incoming pairing request.
   - Tap **Accept**.

6. **Pairing complete**
   - The extension polls `/pairing/status` until accepted.
   - On acceptance, the device is saved to `devices[]` with its `controlToken`.
   - The new device becomes the **current target**.

7. **Ready**
   - Use "Find Phone", "Flash", or "Stop All" to control the phone.

### What gets stored

```json
{
  "id": "fb79ad10-...",          // device UUID
  "name": "MEIZU 21",            // device model
  "type": "android_phone",
  "host": "10.128.21.95",
  "port": "8888",
  "token": "oiiFw7NSGIff...",    // paired controlToken (43 chars)
  "controllerId": "1cfbf5cb-...", // extension's controller UUID
  "pairedAt": "2026-05-23T...",
  "lastSuccessAt": ""
}
```

## 5. Daily Use

### With a paired device selected

1. Open the Chrome extension popup.
2. The **current device card** shows the selected paired phone name and address.
3. Click:
   - **Find Phone** — ring + flash in sequence.
   - **Flash** — strobe flash only.
   - **Stop All** — stop both ring and flash immediately.
4. The **Paired Phones** list shows all saved devices. Click a device or "Set Current" to switch targets.

### Manual fallback mode

When no paired device is selected:
- Enter the phone's IP, port, and the **8-character token** (from the phone screen) in the manual fields.
- Commands use this token via `X-LocalFind-Token` header.
- Set the token as "remembered" only on a trusted private computer.

### Local protection

Optional: set a **local PIN** or register **system verification** (WebAuthn/Windows Hello) to protect sensitive actions:
- Find Phone and Flash require verification.
- Changing saved token requires verification.
- Deleting a paired device requires verification.
- **Stop All is always available** without verification, so you can stop a ringing/flashing phone immediately.

## 6. Deleting a Paired Device

1. In the **Paired Phones** list, click **Delete** on a device.
2. A confirmation dialog explains that phone-side revocation will be attempted.
3. Complete local PIN / WebAuthn verification (if enabled).
4. The extension calls `POST /pairing/revoke` with the device's `controllerId`.
5. On success: phone revokes the paired token; extension removes the local record.
6. If the deleted device was the current target, fallback to next device or manual mode.

### If the phone is offline

- The revoke request fails (network error or timeout).
- The extension asks: "Cannot revoke phone-side authorization. Delete only the local record?"
- **Confirm** → local delete only. The phone still has the stale paired token; re-pair to get a fresh one.
- **Cancel** → keep the device record.

### Pre-L.4 records

- If the device record lacks `controllerId` (paired before L.4), revoke is skipped.
- The extension asks: "This old record lacks revocation info. Delete only the local record?"
- To properly revoke: delete the old record, then re-pair to get a revocable record.

## 7. Minimal Smoke Test

Do **not test ring**. Test flash only.

### Prerequisites

- Phone app running, pairing mode enabled (for new pairing).
- Chrome extension loaded.
- Phone IP known (check notification or app).

### Test 1 — Old token command

```powershell
$PHONE = "http://<phone-ip>:8888"
$TOKEN = "<8-char-token-from-phone-screen>"

# Should return {"success":true}
curl -X POST "$PHONE/command/flash/strobe/start" -H "X-LocalFind-Token: $TOKEN"

# Should return {"success":true}
curl -X POST "$PHONE/command/stop-all" -H "X-LocalFind-Token: $TOKEN"
```

### Test 2 — Pairing

1. Enable pairing mode on phone.
2. In Chrome extension, enter phone IP, click "Check Phone".
3. Click "Request Pairing".
4. Accept on phone.
5. Verify device appears in Paired Phones list.

### Test 3 — Paired device commands

1. Select the newly paired device as current.
2. Click **Flash** — phone flashlight should strobe.
3. Click **Stop All** — flashlight should stop.

### Test 4 — Delete with revoke

1. Click **Delete** on the paired device.
2. Confirm the dialog.
3. Complete verification if prompted.
4. Verify device is removed from the list.
5. Verify old 8-char token still works (re-run Test 1).

### Test 5 — Delete with phone offline

1. Pair a second device.
2. Stop the phone's service (or toggle Wi-Fi off).
3. Click **Delete** on the second device.
4. Extension should show "Cannot revoke" dialog.
5. Choose "Delete local only".
6. Verify device is removed from the list.

## 8. Troubleshooting

### Chrome extension can't reach phone

1. Confirm phone and computer are on the **same Wi-Fi network**.
2. Confirm the phone's Local Find notification shows a LAN IP (not `127.0.0.1` or `0.0.0.0`).
3. Ping the phone from the computer: `ping <phone-ip>`.
4. Try opening `http://<phone-ip>:8888/ping` in Chrome — should show `{"ok":true}`.
5. Check that Windows Firewall or other security software is not blocking outbound port 8888.
6. Some corporate/guest Wi-Fi networks isolate clients — try a personal hotspot.

### 401 Unauthorized

- The token may have changed (phone app restart regenerates it).
- Check the current 8-character token on the phone screen.
- For paired devices: the paired controlToken may have been revoked. Delete and re-pair.
- The phone token is 8 uppercase alphanumeric characters.

### Phone service stops unexpectedly

- Check that battery optimization is disabled for Local Find.
- On some phones (Xiaomi, Huawei, Oppo, etc.), enable "Auto-start" and disable "Background restrictions".
- The app uses a wake lock and Wi-Fi lock, but aggressive OEM power saving may still kill it.

### Flashlight doesn't work

- Grant camera permission to the app (Android requires this for flashlight).
- Some phones don't support strobe flash — try the steady flash command.

### Pairing mode expired

- Pairing mode has a 5-minute timeout. Re-enable it on the phone.
- While pairing mode is active, the `/device-info` response shows `"pairingMode":true`.

### NSD/mDNS not resolving

- This is expected. The current version does not use NSD from Chrome.
- Always enter the phone's IP address directly.

## 9. Current Limitations

- **Manual IP entry**: must type the phone's LAN IP at least once. No QR code or auto-discovery.
- **Android service only**: no iOS support.
- **Debug APK only**: no release signing, no Play Store distribution.
- **Single network**: phone and computer must be on the same LAN. No internet relay.
- **No controller management UI on Android**: paired controllers can only be listed/revoked via API.
- **One-at-a-time revoke**: no "revoke all" endpoint.
- **Chrome storage is plaintext**: `chrome.storage.local` is not encrypted at rest.
- **8-char token is high-privilege**: it can impersonate or revoke any paired controller.
- **No re-pair button**: if a paired token is lost, delete and re-pair.
- **No automatic background scanning**: the extension does not scan the LAN for phones.

## 10. Release Package

### Include

```
android/app/build/outputs/apk/debug/app-debug.apk
chrome-extension/
  manifest.json
  popup.html
  popup.js
  popup.css
  README.md
docs/
  MVP_L_PAIRING_PLAN.md
  MVP_L_CLOSEOUT.md
  MVP_P_INSTALL_AND_RELEASE.md
README.md
```

### Do NOT include

```
.local_llm_out/
.mcp_audit/
android/-H/
*.patch files
node_modules/
.gradle/
android/app/build/          (except the APK output)
android/.gradle/
*.iml
.idea/
local.properties
```

### Zip command (when ready)

```powershell
cd D:\local-find
Compress-Archive -Path @(
  "android\app\build\outputs\apk\debug\app-debug.apk",
  "chrome-extension\manifest.json",
  "chrome-extension\popup.html",
  "chrome-extension\popup.js",
  "chrome-extension\popup.css",
  "chrome-extension\README.md",
  "docs\MVP_L_PAIRING_PLAN.md",
  "docs\MVP_L_CLOSEOUT.md",
  "docs\MVP_P_INSTALL_AND_RELEASE.md"
) -DestinationPath "localfind-mvp-l5-ok.zip"
```

## References

- L series plan: `docs/MVP_L_PAIRING_PLAN.md`
- L series closeout: `docs/MVP_L_CLOSEOUT.md`
- Chrome extension details: `chrome-extension/README.md`
- Android source: `android/`
