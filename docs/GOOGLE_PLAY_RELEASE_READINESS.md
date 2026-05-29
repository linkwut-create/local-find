# Google Play Release Readiness Audit

Audit date: 2026-05-29

Scope: read-only Google Play developer account, Android app configuration, and release artifact readiness audit for `D:\local-find`.

This audit did not modify Android code, Chrome extension code, release artifacts, tags, or the published GitHub Release. The only intended repository changes for PLAY.0 are this document, `GOOGLE_PLAY_RELEASE_PLAN.md`, and related project status updates.

## Repository State

Commands recorded during the audit:

```text
git status --short
```

Result:

```text
?? screenshots-draft/
```

```text
git describe --tags --dirty
```

Result:

```text
mvp-u5-ok-37-ge5d2098
```

```text
git log --oneline -10
```

Result:

```text
e5d2098 docs: record Chrome Web Store pending review status
7604f9f docs: prepare Chrome Web Store manual upload checklist
4abc181 build: add Chrome Web Store extension zip candidate
5a3fd6f docs: verify Chrome Web Store privacy URL reachability
8ea2642 docs: polish privacy policy for Chrome Web Store
3473c4f docs: finalize Chrome Web Store contact fields
7d0b8b2 assets: add polished Chrome Web Store screenshots
6506984 docs: preflight Chrome Web Store screenshot assets
e7ef42b docs: record Chrome Web Store developer account status
bff4162 docs: record Chrome Web Store screenshot draft review
```

Repository is clean except for untracked `screenshots-draft/`. Android I.0 WIP stash is preserved at `stash@{0}`.

## Google Play Developer Account

| Item | Status |
|------|--------|
| Google Play developer account | Registered |
| Account verification | Verified by owner |
| Account type | **Not confirmed** — personal vs organization account affects testing requirements |
| Production access | **Not confirmed** — personal accounts created after November 2023 may require closed testing before production |

### Account type implications

- **Personal account**: likely requires 12+ testers for 14+ days of closed testing before production access, per Google Play policy for new personal developer accounts.
- **Organization account**: may have direct production access or different testing requirements.
- Owner must confirm account type in Play Console > Developer Account > Account details.

### Required owner confirmations

1. Confirm Google Play developer account type (personal or organization).
2. Confirm whether production access requires closed testing.
3. Confirm that the account can create internal testing, closed testing, and production tracks.

## Android App Configuration

### Build identity

| Field | Current value |
|-------|---------------|
| `applicationId` | `io.github.linkwutcreate.localfind` |
| `namespace` | `io.github.linkwutcreate.localfind` |
| `versionCode` | `1` |
| `versionName` | `"1.0"` |
| `compileSdk` | `35` |
| `targetSdk` | `35` |
| `minSdk` | `26` |

Source: `android/app/build.gradle.kts:31-39`.

### Signing configuration

Release signing is conditionally configured from environment variables or `local.properties`:

| Variable | Purpose |
|----------|---------|
| `LOCAL_FIND_UPLOAD_STORE_FILE` | Upload keystore file path |
| `LOCAL_FIND_UPLOAD_STORE_PASSWORD` | Keystore password |
| `LOCAL_FIND_UPLOAD_KEY_ALIAS` | Key alias |
| `LOCAL_FIND_UPLOAD_KEY_PASSWORD` | Key password |

`hasReleaseSigningConfig` is `true` only when all four values are non-blank. Without them, release builds will not have a signing config set in the Gradle build file.

Source: `android/app/build.gradle.kts:10-55`.

No `.jks`, `.keystore`, or `.pem` files are committed to the repository.

Signing readiness verdict: **Signing config is wired but keystore existence and path are not confirmed.** Owner must confirm that the upload keystore exists at the configured path and that the signing variables are set before any release AAB build.

### Manifest declarations

Source: `android/app/src/main/AndroidManifest.xml`.

| Permission / declaration | Purpose |
|--------------------------|---------|
| `INTERNET` | Local HTTP server and LAN communication |
| `ACCESS_NETWORK_STATE` | Network connectivity checks |
| `ACCESS_WIFI_STATE` | Wi-Fi state for LAN discovery |
| `CAMERA` | QR code scanning and flashlight |
| `POST_NOTIFICATIONS` | Service notification (Android 13+) |
| `FOREGROUND_SERVICE` | Keep local find service active (Android 9+) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Special-use FGS (Android 14+) |
| `USE_BIOMETRIC` | Local device authentication |
| `WAKE_LOCK` | Keep CPU awake during find actions |

Foreground service declaration:

- Service class: `.service.FindPhoneForegroundService`
- `foregroundServiceType`: `specialUse`
- Subtype description: `"Find lost phone controlled via local Wi-Fi HTTP API"`

Other manifest notes:

- `android:icon` and `android:roundIcon` use `@android:drawable/ic_menu_search` (system default search icon). A custom app icon is needed for Play Store.
- `android:usesCleartextTraffic="true"` is declared for local HTTP communication.

### Dependencies

Key dependencies from `android/gradle/libs.versions.toml`:

| Dependency | Version |
|------------|---------|
| AGP | 8.2.2 |
| Kotlin | 1.9.22 |
| Compose BOM | 2023.10.01 |
| Ktor Server | 2.3.8 |
| CameraX | 1.3.4 |
| ZXing | 3.5.4 |
| MLKit Barcode | 17.3.0 |
| Biometric | 1.1.0 |

## Store Listing Readiness

| Item | Status | Notes |
|------|--------|-------|
| App name | Present | `Local Find` |
| Short description | Draft | `docs/GOOGLE_PLAY_LISTING_DRAFT.md` |
| Full description | Draft | `docs/GOOGLE_PLAY_LISTING_DRAFT.md` |
| 512x512 app icon | **Missing** | Current manifest uses system default `ic_menu_search`; custom icon not committed |
| 1024x500 feature graphic | **Missing** | Concept direction documented in `GOOGLE_PLAY_ASSETS_PLAN.md`, not produced |
| Phone screenshots | **Missing** | Plan exists in `GOOGLE_PLAY_ASSET_CAPTURE_GUIDE.md`, not captured |
| Short demo video | **Missing / optional** | May be required for foreground service declaration review |
| Privacy policy URL | **Available** | `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md` — reachability verified for CWS; same URL is candidate for Play |
| Support email | Present | `linkwut@gmail.com` — same as confirmed for CWS |
| GitHub repo URL | Present | `https://github.com/linkwut-create/local-find` |
| Data Safety form | **Not completed** | Draft answers in `GOOGLE_PLAY_LISTING_DRAFT.md` |
| Foreground Service declaration | **Not completed** | Draft text exists; may need demo video evidence |
| App content declarations | **Not completed** | Content rating questionnaire, ads declaration, etc. |
| Category and tags | **Not decided** | Not yet documented for Play |

## Existing Google Play Documentation

| Document | Status |
|----------|--------|
| `docs/GOOGLE_PLAY_ASSETS_PLAN.md` | Asset plan with checklist |
| `docs/GOOGLE_PLAY_ASSET_CAPTURE_GUIDE.md` | Screenshot capture guide |
| `docs/GOOGLE_PLAY_ASSET_PRODUCTION_NOTES.md` | Production notes and frozen release boundary |
| `docs/GOOGLE_PLAY_ASSET_VALIDATION.md` | Asset validation checklist |
| `docs/GOOGLE_PLAY_LISTING_DRAFT.md` | Listing text, Data Safety draft, permissions table, FGS draft, closed testing checklist |

## Risk Assessment

| Finding | Severity | Evidence | Recommended next step |
|---------|----------|----------|----------------------|
| Google Play account type not confirmed | **blocker** | Owner stated verification passed but account type (personal/organization) unknown | Owner confirms account type in Play Console |
| Production access path not confirmed | **blocker** | Personal accounts may require closed testing before production | Owner checks production access requirements in Play Console |
| Upload keystore existence not confirmed | **blocker** | Signing config reads from env/local.properties; no `.jks` committed | Owner confirms keystore file exists and signing variables are set |
| Custom app icon missing | **blocker** | Manifest uses `@android:drawable/ic_menu_search` (system default) | Create 512x512 custom app icon for Play, replace manifest icon reference |
| Feature graphic missing | **blocker** | Play requires 1024x500 feature graphic | Produce feature graphic from concept in `GOOGLE_PLAY_ASSETS_PLAN.md` |
| Phone screenshots missing | **blocker** | Play requires phone screenshots | Capture per `GOOGLE_PLAY_ASSET_CAPTURE_GUIDE.md` |
| Data Safety form not completed | **blocker** | Draft answers exist but form not filled in Play Console | Complete Data Safety form before first upload |
| Foreground Service declaration not submitted | **warning** | Draft text exists; Play may require video evidence for specialUse FGS | Prepare FGS declaration and optional demo video |
| App content declarations not completed | **warning** | Content rating, ads, target audience not declared | Complete content declarations in Play Console |
| Category and tags not decided | **warning** | No Play category documented yet | Decide category (likely Tools or Productivity) |
| `usesCleartextTraffic=true` | **info** | Required for local HTTP; may trigger Play review questions | Document justification in FGS/permissions declaration |
| Foreground service type is `specialUse` | **info** | Requires stronger justification than `dataSync` or `location` | Prepare detailed FGS declaration with video evidence |
| No cloud dependency | **info** | App is local-only, which simplifies Data Safety and privacy | Preserve; document in Data Safety form |
| versionCode=1 | **info** | First release uses versionCode 1 | Increment for each subsequent upload |
| Target SDK 35 meets Play target API level requirements | **info** | Play requires new apps target API 34+ as of August 2025 | Good; maintain for next target level deadline |

## Prerequisites Before Any Play Upload

1. Owner confirms Google Play account type and production access path.
2. Owner confirms upload keystore exists and signing variables are set.
3. Custom app icon (512x512) created and referenced in manifest.
4. Feature graphic (1024x500) produced.
5. Phone screenshots (minimum 4) captured, reviewed for privacy, and committed.
6. Data Safety form completed in Play Console, matching `PRIVACY.md`.
7. Foreground Service declaration completed with justification text and optional demo video.
8. App content declarations completed (content rating questionnaire, ads declaration, target audience).
9. Privacy policy URL confirmed reachable for Play review.
10. Support email confirmed.
11. Category and tags decided.
12. Internal testing track created for first upload and smoke validation.

## Constraints

This audit respects the following constraints:

- Do not modify Android code.
- Do not modify Chrome extension code.
- Do not build APK/AAB.
- Do not upload to Google Play.
- Do not submit for review.
- Do not modify signing files.
- Do not commit `local.properties`.
- Do not commit `app-release.aab`.
- Do not commit `.jks` / `.keystore`.
- Do not move the `mvp-u5-ok` tag.
- Do not modify the published GitHub Release.
- Do not restore the Android I.0 WIP stash.
- Do not reset the repository.
