# Android Runtime Connectivity Closeout

Closeout date: 2026-05-26

## Purpose

This document closes the Android runtime connectivity diagnosis for the recent Local Find phone HTTP service timeout report.

This stage records the current evidence and conclusion only. It does not modify Android code, Chrome extension code, manifests, Gradle files, `MainActivity`, `HttpServerManager`, or `FindPhoneForegroundService`. It does not build, generate APK/AAB artifacts, install or uninstall apps, clear app data, upload to Google Play or Chrome Web Store, move `mvp-u5-ok`, modify the GitHub Release, restore Android I.0 WIP stash, reset the repository, or handle secrets.

## Evidence Summary

A-DIAG.0 static diagnosis found:

- Current source and `mvp-u5-ok` both use Ktor with `host = "0.0.0.0"`.
- `GET /device-info` is a public endpoint and does not require a token.
- Static source did not support the hypothesis that the server changed to localhost-only binding.

A-DIAG.1 initial runtime collection found:

- adb could see serial `461QYFFT225UP`, but the device state was `unauthorized`.
- Package, listener, IP, and logcat evidence could not be collected in that pass.

A-DIAG.1B authorized runtime collection found:

- Device: `461QYFFT225UP device`.
- Installed package: `io.github.linkwutcreate.localfind`.
- Old package: `com.example.localfind` was not installed.
- APK state: `versionCode=1`, `versionName=1.0`, `targetSdk=35`, `DEBUGGABLE`, `TEST_ONLY`.
- Wi-Fi interface: `wlan0`.
- Wi-Fi IP: `10.128.21.95/17`.
- Listener: `*:8888` / `[::]:8888`.
- `netstat` showed `:8888 ESTABLISHED` connections after reproduction.
- User confirmed external connection succeeded.
- Logcat was saved to `D:\local-find-android-connectivity-logcat.txt`.

## Conclusion

Current runtime evidence does not support a localhost-only bind bug.

Current runtime evidence does not support an old/new package coexistence conflict.

Current runtime evidence does not support starting A-FIX.0 for bind address or `NetworkUtil`.

The previous timeout was not reproduced after adb authorization and manual service start. Treat current Android runtime connectivity as restored unless the issue recurs with fresh evidence.

## If Timeout Recurs

Do not immediately fix code. First capture fresh evidence from the failure state.

Required fresh checks during the failure:

```powershell
$ADB = "C:\Users\Zero\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $ADB shell "ip addr"
& $ADB shell "toybox netstat -an | grep 8888"
& $ADB shell "ss -ltn | grep 8888"
& $ADB logcat -d > D:\local-find-android-connectivity-logcat.txt
```

Record at the same time:

- phone Wi-Fi IP;
- App displayed IP;
- listener state;
- whether `ESTABLISHED` exists for `:8888`;
- whether external access generated request or error logs;
- whether the phone was awake and Local Find service was started.

Interpretation:

- If the listener disappears, investigate service lifecycle, foreground service behavior, watchdog behavior, and startup logs.
- If the listener is `*:8888` / `[::]:8888` but no `ESTABLISHED` connection appears, investigate network path, LAN isolation, VPN, campus Wi-Fi, enterprise Wi-Fi, or phone inbound restrictions.
- If request logs appear but handler/auth fails, investigate route, auth, token, and handler behavior.
- If the App displayed IP and `wlan0` IP differ, then enter a focused `NetworkUtil` fix plan.

## Resume Recommendation

Chrome Web Store screenshot work can resume.

Keep this Android runtime closeout record in place before final packaging or upload so the release trail explains why no Android code fix was started.

Recommended next slice: `CWS.11C record screenshot draft review`. Keep first-pass screenshot drafts outside the repository and do not commit screenshot assets unless final images pass owner review for privacy, visual clarity, language consistency, and absence of real device/network identifiers.
