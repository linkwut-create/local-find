# Google Play Release AAB Build Audit

## PLAY.7B-FIX16K Build Audit

Audit date: 2026-06-14

Status: **16 KB compatible signed release AAB built and verified locally. Not uploaded.**

### Production blocker

Google Play rejected the existing `versionCode 1` bundle with:

> Your app does not support 16 KB memory page sizes.

The existing AAB contained third-party native libraries from ML Kit and CameraX:

- `libbarhopper_v3.so`
- `libimage_processing_util_jni.so`

The blocking 64-bit library was CameraX 1.3.4's `libimage_processing_util_jni.so`, whose ELF `PT_LOAD` segments used 4096-byte alignment for `arm64-v8a` and `x86_64`.

The project does not contain CMake, ndk-build, or native source code. No NDK is configured, so NDK r28 and custom linker flags do not apply to this project. The native libraries are prebuilt dependency artifacts.

### Fix

| Component | Previous | Updated |
|-----------|----------|---------|
| Android Gradle Plugin | 8.2.2 | 8.7.3 |
| Gradle | 8.2 | 8.9 |
| CameraX | 1.3.4 | 1.5.3 |
| `versionCode` | 1 | 2 |
| `versionName` | 1.0 | 1.0.1 |

Gradle distribution verification is pinned to the official Gradle 8.9 SHA256:

`d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab`

### Build result

Command:

```powershell
cd android
.\gradlew.bat clean bundleRelease --console=plain
```

Result: **BUILD SUCCESSFUL**

| Field | Value |
|-------|-------|
| Output path | `android/app/build/outputs/bundle/release/app-release.aab` |
| File size | 20,484,249 bytes |
| SHA256 | `447178CCCC6AE1DEAF26320A35E48338CD5D0127CF1C427140DDA527D19ECBFC` |
| `versionCode` | `2` |
| `versionName` | `"1.0.1"` |
| Signed | Yes; `jarsigner -verify` exit code 0 |
| Uploaded to Google Play | **No** |

### 16 KB verification

- All `arm64-v8a` and `x86_64` native libraries have ELF `PT_LOAD` alignment of 16384 bytes.
- CameraX 1.5.3's `libimage_processing_util_jni.so` and `libsurface_util_jni.so` are 16 KB aligned.
- ML Kit's 64-bit `libbarhopper_v3.so` is 16 KB aligned.
- Bundletool 1.17.1 reports `PAGE_ALIGNMENT_16K` for uncompressed native libraries.
- A universal APK generated locally from the AAB passed:

```text
zipalign -c -P 16 -v 4 universal.apk
Verification successful
```

The 32-bit ML Kit `libbarhopper_v3.so` variants retain 4096-byte ELF alignment. Android devices using 16 KB memory pages are 64-bit targets; both 64-bit variants pass.

### Test result

`.\gradlew.bat testReleaseUnitTest --console=plain` completed successfully. The project currently has no release unit test sources, so the test task reported `NO-SOURCE`.

`.\gradlew.bat lintRelease --console=plain` did not complete within the 10-minute command limit and produced no lint report. The timed-out wrapper and Gradle daemon were stopped. Lint is therefore not recorded as passing.

### Release boundary

- Do not upload the old `versionCode 1` AAB.
- Do not upload or submit the new AAB during PLAY.7B-FIX16K.
- Do not create a production release until explicitly approved.
- Do not commit the AAB, `local.properties`, keystores, or signing material.

Audit date: 2026-05-29

Phase: PLAY.4B — Release AAB build confirmed. Signed AAB produced and audited.

## Build identity

| Field | Value | Source |
|-------|-------|--------|
| `applicationId` | `io.github.linkwutcreate.localfind` | `build.gradle.kts:35` |
| `namespace` | `io.github.linkwutcreate.localfind` | `build.gradle.kts:31` |
| `versionCode` | `1` | `build.gradle.kts:38` |
| `versionName` | `"1.0"` | `build.gradle.kts:39` |
| `compileSdk` | `35` | `build.gradle.kts:32` |
| `targetSdk` | `35` | `build.gradle.kts:37` |
| `minSdk` | `26` | `build.gradle.kts:36` |

Build identity is correct for first Play upload.

## Signing configuration

| Check | Status |
|-------|--------|
| `local.properties` exists | Yes — `android/local.properties`, gitignored |
| `LOCAL_FIND_UPLOAD_STORE_FILE` | Set — keystore exists at configured path |
| `LOCAL_FIND_UPLOAD_STORE_PASSWORD` | Set |
| `LOCAL_FIND_UPLOAD_KEY_ALIAS` | Set — `localfind-upload` |
| `LOCAL_FIND_UPLOAD_KEY_PASSWORD` | Set |
| `hasReleaseSigningConfig` | `true` |
| Gradle `signingConfigs.release` conditional | Creates release signing config when all 4 vars present |
| `buildTypes.release.signingConfig` | Wired to `signingConfigs.getByName("release")` |
| `isMinifyEnabled` | `false` (no R8 shrinking) |
| ProGuard | Using `proguard-android-optimize.txt` + `proguard-rules.pro` |

**Verdict**: Signing is fully configured. The `bundleRelease` task will produce a signed AAB.

## Build result (2026-05-29)

| Field | Value |
|-------|-------|
| Build result | **SUCCESS** |
| Build environment | Android Studio (bundled JDK) |
| Output path | `android/app/build/outputs/bundle/release/app-release.aab` |
| File size | 20,231,282 bytes (~19.3 MB) |
| SHA256 | `DD86A3466DDFF385757FF4B7D8679ECF59CD9289898C4D21C78B201DFC7B4341` |
| `versionCode` | `1` |
| `versionName` | `"1.0"` |
| Signed | Yes — `localfind-upload` key |
| Tracked by Git | **No** — gitignored (build/ directory excluded) |
| Commit risk | None — `git status --short` clean |

## Post-build verification (completed 2026-05-29)

- [x] AAB exists at expected path
- [x] File size recorded (20,231,282 bytes)
- [x] SHA256 recorded (`DD86...4341`)
- [x] `git check-ignore` confirms AAB path is covered by `.gitignore`
- [x] `git status --short` shows no untracked AAB
- [x] No signing material or local.properties was committed

## Files NOT to commit

- `android/app/build/outputs/bundle/release/app-release.aab`
- `android/local.properties`
- Any `.jks` or `.keystore` files
- Signing passwords or key material

## Constraints

- Do NOT upload AAB to Google Play yet (deferred to PLAY.5).
- Do NOT submit for review.
- Do NOT move the `mvp-u5-ok` tag.
- Do NOT modify the GitHub MVP-U.5 Release.
