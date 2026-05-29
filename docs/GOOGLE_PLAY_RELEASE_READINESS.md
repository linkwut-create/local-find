# Google Play Release Readiness Audit

Audit date: 2026-05-29 (updated PLAY.2A)

Scope: read-only Google Play developer account, Android app configuration, and release artifact readiness audit for `D:\local-find`.

This audit did not modify Android code, Chrome extension code, release artifacts, tags, or the published GitHub Release. The only intended repository changes for PLAY.0 and PLAY.1 are this document, `GOOGLE_PLAY_RELEASE_PLAN.md`, `GOOGLE_PLAY_DEVELOPER_ACCOUNT_STATUS.md`, and related project status updates.

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
mvp-u5-ok-38-g14584df
```

```text
git log --oneline -3
```

Result:

```text
14584df docs: plan Google Play release readiness
e5d2098 docs: record Chrome Web Store pending review status
7604f9f docs: prepare Chrome Web Store manual upload checklist
```

Repository is clean except for untracked `screenshots-draft/`. Android I.0 WIP stash is preserved at `stash@{0}`.

## Google Play Developer Account

| Item | Status |
|------|--------|
| Google Play developer account | Registered |
| Account verification | Verified by owner |
| Account type | **Personal** — confirmed by owner via Play Console (2026-05-29) |
| Production access | **Not directly available** — requires closed testing + application (confirmed via Play Console Dashboard, 2026-05-29) |

### Account type confirmed: Personal

Owner confirmed via Play Console > Developer Account > Account details that the account type is **Personal**.

- **Personal account policy**: requires 12+ testers for 14+ days of closed testing before production access, per Google Play policy for new personal developer accounts created after November 2023.
- **Production path confirmed** (Play Console Dashboard): complete app setup → complete closed testing → apply for production access.
- Internal testing is available as the first testing path.

### Required owner confirmations

1. ~~Confirm Google Play developer account type (personal or organization).~~ **DONE: Personal.**
2. ~~Confirm whether production access requires closed testing.~~ **DONE: Yes — closed testing required, then apply for production.**
3. Confirm that the account can create internal testing, closed testing, and production tracks. (Internal testing confirmed available.)

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

| Variable | Purpose | Status |
|----------|---------|--------|
| `LOCAL_FIND_UPLOAD_STORE_FILE` | Upload keystore file path | **Set** |
| `LOCAL_FIND_UPLOAD_STORE_PASSWORD` | Keystore password | **Set** (value not recorded) |
| `LOCAL_FIND_UPLOAD_KEY_ALIAS` | Key alias | **Set** — `localfind-upload` |
| `LOCAL_FIND_UPLOAD_KEY_PASSWORD` | Key password | **Set** (value not recorded) |

`hasReleaseSigningConfig` is `true` when all four values are non-blank. All four are set → `hasReleaseSigningConfig` = **true**.

Source: `android/app/build.gradle.kts:10-55`.

Signing readiness details (PLAY.1 findings):

- `android/local.properties` exists and is gitignored.
- All 4 signing variables are present.
- Keystore file exists at the configured path: `local-find-upload.jks` (2796 bytes, created 2026-05-24).
- No `.jks`, `.keystore`, or `.pem` files are committed to the repository.
- Key alias: `localfind-upload`.
- **TODO: owner to confirm** this key was not previously used for a published app on this account.

Signing readiness verdict: **Signing config is complete. Keystore present. All signing variables set. Release AAB signing is ready.**

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
| 512x512 app icon | **Missing** | Target defined in `GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md`; not produced |
| 1024x500 feature graphic | **Missing** | Target defined; not produced |
| Phone screenshots | **Missing** | Target defined; not captured |
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
| `docs/GOOGLE_PLAY_DEVELOPER_ACCOUNT_STATUS.md` | **PLAY.1** — account type, production access, signing readiness |
| `docs/GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md` | **NEW (PLAY.2A)** — asset production targets, directory structure, alt texts |

## Risk Assessment

| Finding | Severity | Evidence | Recommended next step |
|---------|----------|----------|----------------------|
| ~~Google Play account type not confirmed~~ | ~~blocker~~ → **resolved** | Account type confirmed as Personal by owner (2026-05-29) | **Resolved PLAY.1A** |
| Production access path not confirmed | ~~blocker~~ → **resolved** | Play Console Dashboard confirms: closed testing required, then apply for production | **Resolved PLAY.1B** |
| ~~Upload keystore existence not confirmed~~ | ~~blocker~~ → **resolved** | Keystore file exists at configured path; all 4 signing variables set; `local.properties` gitignored | **Resolved PLAY.1** |
| Custom app icon missing | **blocker** | Manifest uses `@android:drawable/ic_menu_search` (system default) | Produce per `GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md` (PLAY.2B) |
| Feature graphic missing | **blocker** | Play requires 1024x500 feature graphic | Produce per production plan (PLAY.2C) |
| Phone screenshots missing | **blocker** | Play requires phone screenshots | Capture per production plan (PLAY.2D) |
| Data Safety form not completed | **blocker** | Draft answers exist but form not filled in Play Console | Complete Data Safety form before first upload |
| Foreground Service declaration not submitted | **warning** | Draft text exists; Play may require video evidence for specialUse FGS | Prepare FGS declaration and optional demo video |
| App content declarations not completed | **warning** | Content rating, ads, target audience not declared | Complete content declarations in Play Console |
| Category and tags not decided | **warning** | No Play category documented yet | Decide category (likely Tools or Productivity) |
| Upload key uniqueness not confirmed | **info** | Key `localfind-upload` must be unique per Play policy | Owner confirms key was not used for another published app |
| `usesCleartextTraffic=true` | **info** | Required for local HTTP; may trigger Play review questions | Document justification in FGS/permissions declaration |
| Foreground service type is `specialUse` | **info** | Requires stronger justification than `dataSync` or `location` | Prepare detailed FGS declaration with video evidence |
| No cloud dependency | **info** | App is local-only, which simplifies Data Safety and privacy | Preserve; document in Data Safety form |
| versionCode=1 | **info** | First release uses versionCode 1 | Increment for each subsequent upload |
| Target SDK 35 meets Play target API level requirements | **info** | Play requires new apps target API 34+ as of August 2025 | Good; maintain for next target level deadline |

## Prerequisites Before Any Play Upload

1. ~~Owner confirms Google Play account type.~~ **DONE: Personal (PLAY.1A)** ~~Owner confirms production access path.~~ **DONE: closed testing required (PLAY.1B)**
2. ~~Owner confirms upload keystore exists and signing variables are set.~~ **DONE (PLAY.1)**
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

## PLAY.1 Summary

- **Signing config**: Confirmed. All 4 variables set, keystore exists, `hasReleaseSigningConfig` = true.
- **Account type**: **Personal** — confirmed by owner via Play Console (2026-05-29).
- **Production access**: Not directly available — requires closed testing (12+ testers, 14+ days) → apply for production. Confirmed via Play Console Dashboard (2026-05-29).
- **Internal testing**: Available as first testing path.
- Detailed account status tracked in `docs/GOOGLE_PLAY_DEVELOPER_ACCOUNT_STATUS.md`.

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
