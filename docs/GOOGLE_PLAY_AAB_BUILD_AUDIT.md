# Google Play Release AAB Build Audit

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
