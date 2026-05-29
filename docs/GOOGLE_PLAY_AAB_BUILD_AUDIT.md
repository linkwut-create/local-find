# Google Play Release AAB Build Audit

Audit date: 2026-05-29

Phase: PLAY.4A — Build release AAB audit and readiness check.

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

## Build environment

| Check | Status |
|-------|--------|
| Android SDK | Present — `C:\Users\Zero\AppData\Local\Android\Sdk` |
| SDK platforms | android-34, android-35, android-36.1 |
| Build tools | 34.0.0, 36.1.0, 37.0.0 |
| `local.properties` `sdk.dir` | Set to SDK path above |
| **JDK / JAVA_HOME** | **MISSING** — no JDK found in system PATH or JAVA_HOME |

**Environment limitation**: Cannot run `./gradlew bundleRelease` in current environment because no JDK is available. Owner must build from a machine with JDK 17+ installed (Android Studio includes a bundled JDK).

## Build command (owner to run)

```bash
cd android
./gradlew bundleRelease
```

Expected output:
```
android/app/build/outputs/bundle/release/app-release.aab
```

## Expected AAB output

| Field | Expected value |
|-------|---------------|
| Output path | `android/app/build/outputs/bundle/release/app-release.aab` |
| Signed | Yes — `localfind-upload` key |
| `versionCode` | `1` |
| `versionName` | `"1.0"` |

## Post-build verification (owner to perform)

After `bundleRelease` succeeds, run these checks:

### 1. Verify AAB exists
```bash
ls -la android/app/build/outputs/bundle/release/app-release.aab
```

### 2. Record file size and SHA256
```bash
# On Windows (PowerShell):
(Get-FileHash android/app/build/outputs/bundle/release/app-release.aab -Algorithm SHA256).Hash

# The SHA256 hash should be recorded here:
# SHA256: <owner to fill>
# File size: <owner to fill>
```

### 3. Verify signing
```bash
# Using bundletool (if installed):
bundletool validate --bundle android/app/build/outputs/bundle/release/app-release.aab

# Or using jarsigner:
jarsigner -verify -verbose android/app/build/outputs/bundle/release/app-release.aab
```

### 4. Verify AAB is NOT tracked by git
```bash
git status --short
# app-release.aab should NOT appear (it's covered by .gitignore or was never staged)
```

### 5. Verify .gitignore covers the AAB
```bash
git check-ignore android/app/build/outputs/bundle/release/app-release.aab
# Should print the path (indicating it's ignored)
```

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
