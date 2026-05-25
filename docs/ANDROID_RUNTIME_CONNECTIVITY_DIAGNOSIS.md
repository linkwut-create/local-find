# Android Runtime Connectivity Diagnosis

Diagnosis date: 2026-05-25

Scope: A-DIAG.0 read-only diagnosis for the current Android APK / runtime connectivity regression. This document records repository state, Android changes since the frozen `mvp-u5-ok` release, static manifest/server evidence, and the next runtime checks needed to explain why external devices time out when opening the phone HTTP service.

This stage does not modify Android code, Chrome extension code, manifests, Gradle files, `MainActivity`, `HttpServerManager`, or `FindPhoneForegroundService`. It does not build, install, uninstall, upload, move tags, modify the `mvp-u5-ok` GitHub Release, restore Android I.0 WIP stash, or run destructive adb commands.

## Known Symptom

- Chrome extension and the older tablet app can still connect to each other, so the Chrome extension and the desktop side are unlikely to be the primary cause.
- The same phone previously connected successfully.
- After installing the current new APK on the phone, the phone can open its own HTTP page / service.
- A computer cannot open `http://<phone-ip>:8888`.
- A tablet cannot open `http://<phone-ip>:8888`.
- This narrows the issue to the current phone, the new Android app runtime, service listening, inbound access, token/auth state, package migration state, or phone OS/network restrictions.

## Repository Baseline

Command:

```text
git status --short
```

Result:

```text

```

Command:

```text
git describe --tags --dirty
```

Result:

```text
mvp-u5-ok-23-g2c8bb80
```

Command:

```text
git log --oneline -12
```

Result:

```text
2c8bb80 tools: add Chrome Web Store screenshot profile helper
848724d docs: add manual Chrome Web Store screenshot capture instructions
ae423ef docs: choose Chrome Web Store screenshot capture strategy
1ab80e4 docs: prepare safe Chrome Web Store screenshot preflight
5708e20 docs: record local unpacked Chrome extension validation
bcc2845 docs: plan local unpacked Chrome extension validation
2439539 docs: plan Chrome extension package dry run
2d0f119 docs: plan Chrome Web Store contact and privacy readiness
4b6fb2d docs: plan Chrome Web Store screenshots
9eb2824 chore: add Chrome extension icons
cb6edab docs: decide Chrome Web Store host permission strategy
edc78c7 docs: plan Chrome Web Store listing and disclosure
```

## Android Diff Since `mvp-u5-ok`

Command:

```text
git diff --stat mvp-u5-ok..HEAD -- android
```

Result:

```text
 android/app/build.gradle.kts                       | 45 ++++++++++++++++++++--
 android/app/src/main/AndroidManifest.xml           |  3 +-
 .../linkwutcreate}/localfind/MainActivity.kt       | 24 ++++++------
 .../localfind/auth/PairingTokenManager.kt          |  2 +-
 .../localfind/auth/RemoteDeviceTokenStore.kt       |  4 +-
 .../localfind/hardware/FlashlightController.kt     |  2 +-
 .../localfind/hardware/RingController.kt           |  2 +-
 .../localfind/model/LocalDeviceIdentity.kt         |  2 +-
 .../localfind/model/PairedControllerToken.kt       |  2 +-
 .../localfind/model/PairingRequest.kt              |  2 +-
 .../localfind/server/HardwareCommandDispatcher.kt  |  6 +--
 .../localfind/server/HttpServerManager.kt          | 16 ++++----
 .../localfind/server/NsdAdvertiser.kt              |  2 +-
 .../localfind/server/NsdDiscoveryManager.kt        |  2 +-
 .../localfind/server/RemoteControlClient.kt        |  2 +-
 .../service/FindPhoneForegroundService.kt          | 30 +++++++--------
 .../localfind/store/LocalDeviceIdentityStore.kt    |  4 +-
 .../localfind/store/PairedControllerTokenStore.kt  |  4 +-
 .../localfind/store/PairingRequestStore.kt         |  4 +-
 .../localfind/ui/LocalFindStrings.kt               |  2 +-
 .../linkwutcreate}/localfind/ui/MainScreen.kt      | 20 +++++-----
 .../linkwutcreate}/localfind/util/NetworkUtil.kt   |  2 +-
 .../linkwutcreate}/localfind/util/QrCodeUtil.kt    |  2 +-
 23 files changed, 110 insertions(+), 74 deletions(-)
```

Command:

```text
git diff --name-only mvp-u5-ok..HEAD -- android
```

Result:

```text
android/app/build.gradle.kts
android/app/src/main/AndroidManifest.xml
android/app/src/main/java/io/github/linkwutcreate/localfind/MainActivity.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/auth/PairingTokenManager.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/auth/RemoteDeviceTokenStore.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/hardware/FlashlightController.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/hardware/RingController.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/model/LocalDeviceIdentity.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/model/PairedControllerToken.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/model/PairingRequest.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/server/HardwareCommandDispatcher.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/server/HttpServerManager.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/server/NsdAdvertiser.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/server/NsdDiscoveryManager.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/server/RemoteControlClient.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/service/FindPhoneForegroundService.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/store/LocalDeviceIdentityStore.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/store/PairedControllerTokenStore.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/store/PairingRequestStore.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/ui/LocalFindStrings.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/ui/MainScreen.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/util/NetworkUtil.kt
android/app/src/main/java/io/github/linkwutcreate/localfind/util/QrCodeUtil.kt
```

Command:

```text
git diff --find-renames --summary mvp-u5-ok..HEAD -- android
```

Result:

```text
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/MainActivity.kt (95%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/auth/PairingTokenManager.kt (94%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/auth/RemoteDeviceTokenStore.kt (97%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/hardware/FlashlightController.kt (98%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/hardware/RingController.kt (97%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/model/LocalDeviceIdentity.kt (74%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/model/PairedControllerToken.kt (78%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/model/PairingRequest.kt (85%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/server/HardwareCommandDispatcher.kt (94%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/server/HttpServerManager.kt (98%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/server/NsdAdvertiser.kt (98%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/server/NsdDiscoveryManager.kt (99%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/server/RemoteControlClient.kt (98%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/service/FindPhoneForegroundService.kt (93%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/store/LocalDeviceIdentityStore.kt (93%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/store/PairedControllerTokenStore.kt (96%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/store/PairingRequestStore.kt (97%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/ui/LocalFindStrings.kt (99%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/ui/MainScreen.kt (99%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/util/NetworkUtil.kt (95%)
rename android/app/src/main/java/{com/example => io/github/linkwutcreate}/localfind/util/QrCodeUtil.kt (96%)
```

## Static Android Findings

### Package and SDK migration

- `android/app/build.gradle.kts` now uses `namespace = "io.github.linkwutcreate.localfind"` and `applicationId = "io.github.linkwutcreate.localfind"`.
- `compileSdk` and `targetSdk` are now `35`.
- At `mvp-u5-ok`, the Gradle namespace/applicationId were `com.example.localfind`, with `compileSdk = 34` and `targetSdk = 34`.
- The manifest no longer declares a `package` attribute; package identity comes from Gradle namespace/applicationId.
- The current APK package and launcher activity should therefore be `io.github.linkwutcreate.localfind` / `io.github.linkwutcreate.localfind.MainActivity`.

### Manifest / package audit

Current manifest evidence:

```text
2: <manifest xmlns:android="http://schemas.android.com/apk/res/android">
5: <uses-permission android:name="android.permission.INTERNET" />
6: <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
7: <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
18: <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
21: <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
36: android:usesCleartextTraffic="true">
38: <activity
39: android:name=".MainActivity"
40: android:exported="true"
49: <service
50: android:name=".service.FindPhoneForegroundService"
52: android:exported="false"
53: android:foregroundServiceType="specialUse">
55: android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
```

Interpretation:

- `MainActivity` is exported and remains the launcher activity.
- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_SPECIAL_USE` are present.
- `usesCleartextTraffic="true"` is present.
- The foreground service is declared as non-exported with `foregroundServiceType="specialUse"`.
- The service declaration and FGS special-use type already existed at `mvp-u5-ok`; the more relevant runtime change is the app now targets SDK 35.

### HTTP server bind and route audit

Current `HttpServerManager.kt` evidence:

```text
73: fun getPort(): Int = 8888
79: return embeddedServer(Netty, port = getPort(), host = "0.0.0.0") {
84: // GET /ping - Minimal connectivity test, no locks or tokens
89: // GET /device-info - Public device identity for explicit user-driven pairing.
90: get("/device-info") {
128: post("/pairing/request") {
199: get("/pairing/controllers") {
218: post("/pairing/revoke") {
588: private suspend fun PipelineContext<Unit, ApplicationCall>.authenticate(body: suspend () -> Unit) {
589: val headerToken = call.request.headers["X-LocalFind-Token"]
590: val queryToken = call.request.queryParameters["token"]
593: val matchesGlobalToken = validToken != null && (headerToken == validToken || queryToken == validToken)
594: val matchesPairedControllerToken = pairedControllerTokenStore.isValidToken(headerToken)
600: HttpStatusCode.Unauthorized
686: Log.d("HttpServerManager", "Ktor server listening on port ${getPort()}")
```

Comparison to `mvp-u5-ok`:

- `HttpServerManager.kt` is a 98% rename from `com.example.localfind` to `io.github.linkwutcreate.localfind`.
- The old file also used `embeddedServer(Netty, port = getPort(), host = "0.0.0.0")`.
- Static source does not support the hypothesis that the server code changed from `0.0.0.0` to `127.0.0.1` / `localhost`.
- `GET /ping`, `GET /device-info`, and `GET /pairing/status` are unauthenticated.
- Command endpoints and controller list/revoke endpoints are authenticated with `X-LocalFind-Token` or `?token=`.
- Because `/device-info` is public, a timeout from another device is more consistent with listen/interface/routing/firewall/OS behavior than with token rejection. A token/auth bug would usually produce an HTTP response such as `401`, not a TCP timeout, unless the client is testing only protected endpoints.

### Foreground service audit

Current `FindPhoneForegroundService.kt` evidence:

```text
103: currentIp = NetworkUtil.getLocalIpAddress()
104: registerNetworkCallback()
127: connectivityManager.registerNetworkCallback(networkRequest, it)
156: startForeground(
159: ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
168: acquireWakeLock()
169: acquireWifiLock()
170: startWatchdog()
172: currentIp = NetworkUtil.getLocalIpAddress()
174: httpServerManager?.start()
213: private fun startWatchdog() {
227: Log.w("ForegroundService", "Watchdog detected server down ($status), attempting restart...")
259: private fun acquireWifiLock() {
337: httpServerManager?.start()
387: val contentText = "Running | IP: $ipText | Port: 8888"
```

Comparison to `mvp-u5-ok`:

- `FindPhoneForegroundService.kt` is a 93% rename from `com.example.localfind` to `io.github.linkwutcreate.localfind`.
- Targeted diff inspection found only package/import changes in this service between `mvp-u5-ok` and `HEAD`.
- The service still starts foreground mode, acquires wake/wifi locks, starts the watchdog, starts the HTTP server, and shows the IP/port in the notification.

### NetworkUtil audit

Current `NetworkUtil.kt`:

```text
12: fun getLocalIpAddress(): String? {
14: val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
16: if (!networkInterface.isUp || networkInterface.isLoopback) continue
19: if (!address.isLoopbackAddress && address is Inet4Address) {
20: return address.hostAddress
```

Interpretation:

- The displayed IP is selected from the first active non-loopback IPv4 interface.
- This can be wrong on devices with multiple active interfaces, VPN, hotspot, cellular, Wi-Fi Direct, or vendor-specific virtual adapters.
- If the app displays a non-Wi-Fi IPv4 address while the server is listening on all interfaces, the phone can still self-open but external devices may time out against the displayed IP.

### Token/auth and pairing model audit

- `GET /device-info` is public and should not require a token.
- `GET /pairing/status` without `requestId` is public.
- `POST /pairing/request` requires pairing mode but not an existing token.
- `GET /pairing/controllers`, `POST /pairing/revoke`, and command endpoints require either the legacy global token or a paired-controller token.
- Static source still supports the old global token path plus paired-controller token path through `X-LocalFind-Token`.
- Token/auth remains relevant for command failures, but it is a weaker explanation for a raw timeout on `/device-info`.

## adb Read-Only Collection

Command:

```text
adb devices
```

Result:

```text
adb : command not found in PATH
```

`adb.exe` was found at:

```text
C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

Command:

```text
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices
```

Result:

```text
List of devices attached

* daemon not running; starting now at tcp:5037
* daemon started successfully
```

No attached device was listed, so the following read-only commands were not executed:

```powershell
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell pm list packages | findstr local
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell dumpsys package io.github.linkwutcreate.localfind
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell dumpsys package com.example.localfind
```

When a device is attached and authorized, collect both package states. The old package may still coexist with the new package because `applicationId` changed from `com.example.localfind` to `io.github.linkwutcreate.localfind`.

## Runtime Logcat Capture Instructions

Use these only after attaching and authorizing the phone with adb. These commands do not install or uninstall the app.

1. Clear logcat:

```powershell
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' logcat -c
```

2. On the phone, open Local Find.
3. Start Find Me / the controlled-device service.
4. On the phone itself, open:

```text
http://<phone-ip>:8888/device-info
```

5. From the computer or tablet, open:

```text
http://<phone-ip>:8888/device-info
```

6. Capture logs:

```powershell
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' logcat -d > D:\local-find-android-connectivity-logcat.txt
```

Priority filters / search terms:

```text
LocalFind
HttpServerManager
MainActivity
FindPhoneForegroundService
ForegroundService
Pairing
token
auth
8888
exception
bind
server
device-info
Ktor
Netty
```

Signals to look for:

- `Ktor server listening on port 8888`
- `Error starting Ktor server`
- foreground service start failures
- watchdog restart loops
- IP change logs
- permission or foreground service exceptions
- request logs or absence of request logs when the external client times out

## Port / Listening Diagnosis Instructions

Run these only after the app service is started on the phone. They are read-only.

```powershell
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell "ip addr"
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell "toybox netstat -an | grep 8888"
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell "ss -ltn | grep 8888"
& 'C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell "dumpsys connectivity"
```

Expected interpretations:

- `0.0.0.0:8888` or `:::8888` listening: the server is bound broadly; focus on phone IP selection, LAN client isolation, phone firewall/vendor network controls, or package/runtime state.
- `127.0.0.1:8888` listening: the runtime is localhost-only despite static source saying `0.0.0.0`; inspect Ktor/Netty runtime behavior and server startup path.
- No `8888` listener: the local page may be stale, opened by a different package/process, or the server starts and stops quickly; inspect logcat and package state.
- Phone IP in `ip addr` differs from the app-displayed IP: `NetworkUtil` may be choosing the wrong active IPv4 interface.

## Hypothesis Table

| Hypothesis | Evidence for | Evidence against | How to confirm | Severity / likelihood |
| --- | --- | --- | --- | --- |
| New APK changed HTTP server bind address to localhost only. | Symptom fits local self-access plus external timeout. | Current and `mvp-u5-ok` source both use `host = "0.0.0.0"` in Ktor. | Run `toybox netstat -an \| grep 8888` or `ss -ltn \| grep 8888` while service is active. | High severity / low-to-medium likelihood. |
| New APK service is started but not bound to the Wi-Fi interface. | Phone can self-open but LAN devices time out; `NetworkUtil` may display the first non-loopback IPv4, not necessarily Wi-Fi. | Ktor binding to `0.0.0.0` should normally include Wi-Fi. | Compare app-displayed IP with `adb shell ip addr`; check listener address; test `/ping` and `/device-info` from the exact Wi-Fi IP. | High severity / medium likelihood. |
| New targetSdk / foreground service behavior causes service to run differently on phone. | targetSdk changed from 34 to 35; phone-specific OS restrictions could affect foreground service/runtime. | Manifest service type/permissions and service code are mostly unchanged from `mvp-u5-ok`; phone local access suggests the service can start at least sometimes. | Capture logcat around start; inspect `dumpsys package`; inspect foreground service exceptions and watchdog logs. | High severity / medium likelihood. |
| Token/auth regression rejects external requests before response. | Pairing/auth code is active; command endpoints require `X-LocalFind-Token`; old/new package migration can create fresh tokens. | `/device-info` and `/ping` are public; auth rejection should return HTTP `401`, not a TCP timeout. | Test `/ping`, `/device-info`, `/status`, then a protected command; compare timeout vs `401`. | Medium severity / low likelihood for `/device-info` timeout; medium for command failures. |
| Package migration left old/new package state conflict. | applicationId changed from `com.example.localfind` to `io.github.linkwutcreate.localfind`; Android Studio previously mixed old package with new activity in a run configuration error. | Current APK package/activity are internally consistent, and the app installs. | Run `pm list packages | findstr local`; inspect `dumpsys package` for both package IDs; verify launcher and running process package. | Medium severity / medium likelihood. |
| Phone OS blocks external inbound access despite local self-access. | Both computer and tablet time out against the phone, while phone self-access works; this points to device-level inbound filtering, AP/client isolation, VPN/private DNS/security app, or vendor restrictions. | The phone previously worked, and old tablet app still works in the environment. | Test same APK on another phone or old APK on same phone; inspect Wi-Fi network mode, hotspot/VPN/private network settings, and listener address. | High severity / medium-to-high likelihood. |
| Plugin/browser issue. | Always possible if only Chrome extension failed. | Computer browser and tablet also cannot reach `phone-ip:8888`, while Chrome extension works with old tablet app. | Direct browser/curl test to `/ping` and `/device-info` from multiple LAN clients. | Low severity / low likelihood. |

## Immediate Recommendation

1. If Android diff or future runtime logs show server/auth changes, run targeted logcat and inspect the changed files before any code fix.
2. If no server/auth diff explains the timeout, compare the old working tablet APK against the current APK.
3. Strongest A/B test: after explicit user approval, install the old working tablet APK on the same phone. This distinguishes APK regression from phone OS/network restrictions.
4. Do not continue Chrome Web Store screenshot/package work until the Android runtime connectivity issue is understood.

## Current Conclusion

Static repository evidence does not currently show a source-level regression from `0.0.0.0` to localhost-only binding, and `/device-info` should be public. The highest-value next check is runtime evidence from the affected phone: package state, actual listener address, actual phone Wi-Fi IP, and logcat while an external device times out.
