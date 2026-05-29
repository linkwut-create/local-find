# Google Play Release Plan

Plan date: 2026-05-29

Status: PLAY.3B — Play Console forms completed by owner. Do not build AAB, upload to Google Play, or submit for review until each phase is explicitly approved by the owner.

## Release path overview

```
PLAY.0: Readiness audit ✓
  │
  ├─ PLAY.1: Account and signing confirmation ✓
  │   ├─ Confirm account type (personal / organization) → ✓ Personal
  │   ├─ Confirm production access requirements → ✓ Closed testing required
  │   ├─ Confirm upload keystore exists and signing variables are set → ✓ DONE
  │   └─ Document findings → ✓ DONE
  │
  ├─ PLAY.2: Store assets ✓ (all complete)
  │   ├─ PLAY.2A: Define asset production targets and directory structure ✓
  │   ├─ PLAY.2B: Create 512x512 custom app icon ✓
  │   ├─ PLAY.2C: Create 1024x500 feature graphic ✓
  │   ├─ PLAY.2D: Capture phone screenshots (minimum 4) ✓ (real captures)
  │   ├─ PLAY.2D-R: Accuracy review → AI screenshots inaccurate ✓
  │   ├─ PLAY.2D2: Real screenshot capture → owner captured ✓
  │   ├─ PLAY.2D3: Validate and commit real screenshots ✓
  │   ├─ PLAY.2E: Update AndroidManifest.xml icon reference ✓
  │   └─ PLAY.2F: Closeout — all assets committed ✓
  │
  ├─ PLAY.3: Play Console forms ✓ (completed in draft by owner)
  │   ├─ PLAY.3A: Prepare all form answers ✓
  │   ├─ PLAY.3B: Owner completes forms in Play Console ✓
  │   ├─ Complete Data Safety form ✓
  │   ├─ Complete Foreground Service declaration ✓
  │   ├─ Complete App content declarations ✓
  │   ├─ Set category and tags ✓
  │   └─ Confirm privacy policy URL and support email in Play Console ✓
  │
  ├─ PLAY.4: Build release AAB
  │   ├─ Verify versionCode and versionName
  │   ├─ Verify signing config
  │   ├─ Build release AAB
  │   ├─ Validate AAB (size, contents, signing)
  │   └─ Do NOT commit the AAB
  │
  ├─ PLAY.5: Internal testing
  │   ├─ Create internal testing track in Play Console
  │   ├─ Upload first AAB to internal testing
  │   ├─ Smoke test: install, start service, pair, trigger actions
  │   └─ Fix issues before advancing
  │
  ├─ PLAY.6: Closed testing (if required)
  │   ├─ Create closed testing track
  │   ├─ Recruit testers (12+ for 14+ days if personal account)
  │   ├─ Collect tester feedback
  │   └─ Address tester-reported issues
  │
  └─ PLAY.7: Production release
      ├─ Owner approval required
      ├─ Promote tested AAB to production
      ├─ Submit for review
      └─ Record production release status
```

## PLAY.0: Readiness Audit

Status: **COMPLETE**

- [x] Read-only audit of Android app configuration.
- [x] Identify all blockers and warnings.
- [x] Document existing Play-related docs.
- [x] Create `GOOGLE_PLAY_RELEASE_READINESS.md`.
- [x] Create `GOOGLE_PLAY_RELEASE_PLAN.md`.
- [x] Update `PROJECT_STATUS.md`.
- [x] Commit docs.

Deliverables:
- `docs/GOOGLE_PLAY_RELEASE_READINESS.md`
- `docs/GOOGLE_PLAY_RELEASE_PLAN.md`

No code changes, no builds, no uploads.

## PLAY.1: Account and Signing Confirmation

Status: **COMPLETE**

Goal: resolve the three blocker unknowns identified in the readiness audit.

### Tasks

1. **Confirm account type**
   - [x] Owner opens Play Console > Developer Account > Account details.
   - [x] Record whether the account is "Personal" or "Organization".
   - [x] Document the finding in `GOOGLE_PLAY_RELEASE_READINESS.md` and `docs/GOOGLE_PLAY_DEVELOPER_ACCOUNT_STATUS.md`.
   - Result: **Personal** (confirmed 2026-05-29).

2. **Confirm production access requirements**
   - [x] Owner checks Play Console > Dashboard or Play Console > Publishing overview for any "Complete closed testing" requirement banner.
   - [x] Result: "To publish to all users, you need to complete app setup, complete closed testing, and apply for production access."
   - [x] Closed testing is mandatory before production.
   - [x] Internal testing is available.
   - [ ] Confirm closed testing track availability once app setup progresses.

3. **Confirm upload keystore**
   - [x] `local.properties` exists and is gitignored.
   - [x] All 4 signing variables present.
   - [x] Keystore file exists at configured path (local-find-upload.jks, 2796 bytes, created 2026-05-24).
   - [x] Key alias: `localfind-upload`.
   - [x] No `.jks` or `.keystore` committed to repo.
   - [x] **Owner confirmed**: this key (`localfind-upload`) has not been used for any other Google Play app. This is the first Play app using this upload key (2026-05-29).

### Findings (2026-05-29)

**Signing config**: Complete.
- `android/local.properties` exists (gitignored).
- All 4 `LOCAL_FIND_UPLOAD_*` variables set.
- Keystore file `local-find-upload.jks` exists on disk (2796 bytes).
- Key alias: `localfind-upload`.
- Gradle `hasReleaseSigningConfig` = true.
- No key material committed to repository.

**Account type**: **Personal** — confirmed by owner via Play Console (2026-05-29).

**Production access**: TODO — owner to confirm in Play Console.

### Deliverables
- [x] `docs/GOOGLE_PLAY_DEVELOPER_ACCOUNT_STATUS.md` (created)
- [x] `GOOGLE_PLAY_RELEASE_READINESS.md` (updated with signing findings)
- [x] `GOOGLE_PLAY_RELEASE_PLAN.md` (this document, updated)
- [ ] `PROJECT_STATUS.md` (pending update)
- [ ] Owner to fill in account type and production access TODOs

### Constraints
- Do NOT commit `local.properties`.
- Do NOT commit keystore files.
- Do NOT commit passwords or key material.
- Do NOT modify Android code.

## PLAY.2 (current): Store Assets

Goal: produce all required Play Store graphic assets.

### PLAY.2A: Asset Production Targets (this sub-phase)

Status: **IN PROGRESS**

- [x] Define production targets for all 4 required assets.
- [x] Create placeholder directory structure under `store-assets/google-play/`.
- [x] Write alt text drafts for accessibility.
- [x] Document per-asset concept, format, constraints, avoid list.
- [x] Define privacy checklist for screenshots.
- [x] Create `docs/GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md`.
- [ ] Commit PLAY.2A docs.

Deliverable: `docs/GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md`

### PLAY.2B: App Icon

- Produce 512x512 PNG per targets in `GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md`.
- Commit to `store-assets/google-play/icon/`.
- Do NOT modify AndroidManifest.xml yet (deferred to PLAY.2E).

### PLAY.2C: Feature Graphic

- Produce 1024x500 PNG/JPEG per targets.
- Commit to `store-assets/google-play/feature-graphic/`.

### PLAY.2D: Phone Screenshots

- Capture minimum 4 screenshots per targets.
- English screenshots first → `store-assets/google-play/screenshots/en-US/`.
- Privacy review pass required before commit.

### PLAY.2E: Manifest Icon Update

- Replace `@android:drawable/ic_menu_search` with custom icon references.
- Add adaptive icon layers (`ic_launcher.xml`, `ic_launcher_round.xml`).
- Commit manifest change with icon resources.

### PLAY.2F: PLAY.2 Closeout

- All 4 assets committed.
- Manifest icon updated.
- Docs updated.
- Commit: `docs: add Google Play store assets`.

### Constraints
- Do NOT modify Chrome extension code.
- Do NOT build APK/AAB.
- Do NOT commit `local.properties`, keystores, or passwords.

## PLAY.3: Play Console Forms

Goal: complete all required Play Console declarations before first upload.

### Tasks

1. **Data Safety form**
   - Use draft answers from `GOOGLE_PLAY_LISTING_DRAFT.md` as starting point.
   - Confirm with `PRIVACY.md` that the local-only data handling is accurately described.
   - Key answers:
     - Data collection: No (no data transmitted off-device to developer or third party).
     - Data sharing: No.
     - Encryption in transit: Cleartext LAN HTTP — do not claim internet transport encryption.
     - Data deletion: Yes — users can delete saved devices and revoke controllers.

2. **Foreground Service declaration**
   - Use draft text from `GOOGLE_PLAY_LISTING_DRAFT.md`.
   - Upload demo video if Play Console requires it for `specialUse` FGS review.
   - The video should show: opening app → starting service → persistent notification appears → pairing locally → triggering find action → stopping action.

3. **App content declarations**
   - Content rating questionnaire.
   - Ads declaration: No ads.
   - Target audience: General audience (no age restrictions).
   - Any in-app purchases or paid features: No.

4. **Store listing fields**
   - Category: Productivity (recommended) or Tools.
   - Tags: utilities, networking, device-finder (as applicable).
   - Confirm privacy policy URL: `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md`.
   - Confirm support email: `linkwut@gmail.com`.

### Deliverables
- Completed Data Safety form (in Play Console).
- Completed FGS declaration (in Play Console).
- Completed app content declarations (in Play Console).
- Store listing draft finalized.

### Constraints
- Do NOT upload APK/AAB yet.
- Do NOT submit for review.
- All form entries should match `PRIVACY.md` and the app's actual behavior.

## PLAY.4: Build Release AAB

Goal: produce a signed release AAB for Play upload.

### Prerequisites
- PLAY.1 complete (signing confirmed ✓, account type and production access TODO).
- PLAY.2 complete (icon updated in manifest).
- PLAY.3 complete (forms ready).

### Tasks

1. **Verify build identity**
   - Confirm `versionCode` and `versionName` are correct.
   - For first upload: `versionCode = 1`, `versionName = "1.0"`.

2. **Verify signing**
   - Confirm all four signing environment variables or `local.properties` values are set.
   - Confirm keystore file exists at the configured path.

3. **Build**
   - Run: `./gradlew bundleRelease` from the `android/` directory.
   - Output: `android/app/build/outputs/bundle/release/app-release.aab`.

4. **Validate AAB**
   - Check AAB size.
   - Verify signing with `jarsigner -verify` or `bundletool`.
   - Confirm AAB is not committed to the repository.

### Deliverables
- `app-release.aab` at build output path (not committed).

### Constraints
- Do NOT commit `app-release.aab`.
- Do NOT commit `local.properties`.
- Do NOT upload to Play Console yet.
- Do NOT move the `mvp-u5-ok` tag.
- Do NOT modify the GitHub MVP-U.5 Release.

## PLAY.5: Internal Testing

Goal: smoke test the release AAB via internal testing track before broader exposure.

### Prerequisites
- PLAY.4 complete (AAB built and validated).
- Play Console internal testing track created.

### Tasks

1. **Create internal testing track** in Play Console.
2. **Upload AAB** to internal testing track.
3. **Add internal testers** (owner's Google account at minimum).
4. **Install and smoke test**:
   - Install from internal testing link.
   - Start Local Find service.
   - Pair with Chrome extension on same LAN.
   - Trigger ring, flashlight strobe, stop all.
   - Test QR pairing flow.
   - Test language switching.
   - Test saved devices and revoke.
5. **Fix issues** before advancing to closed testing or production.

### Deliverables
- Internal testing track with uploaded AAB.
- Smoke test results recorded.

### Constraints
- Do NOT promote to production.
- Do NOT submit for production review.
- Fixes to Android code require a new AAB build and re-upload to the track.

## PLAY.6: Closed Testing (if required)

Goal: satisfy Google Play closed testing requirement before production access.

### Prerequisites
- PLAY.5 complete (internal testing smoke test passed).
- Account type confirmed as "Personal" and closed testing requirement confirmed.
- OR: owner decides to run closed testing regardless of policy requirement.

### Tasks

1. **Create closed testing track** in Play Console.
2. **Recruit testers** (12+ if personal account policy requires it).
3. **Upload AAB** to closed testing track (same or incremented version).
4. **Provide tester instructions**:
   - Install from closed testing invite link.
   - Start Local Find service on phone.
   - Pair trusted controller on same LAN.
   - Test ring, flashlight strobe, stop all.
   - Delete saved devices and revoke paired controllers.
   - Report: device model, Android version, network type, any issues or failures.
5. **Run for required duration** (14 days minimum if personal account policy).
6. **Collect and address feedback**.
7. **Apply for production access** once requirements are met.

### Deliverables
- Closed testing track with uploaded AAB.
- Tester recruitment and instructions.
- Tester feedback summary.
- Production access granted (or application submitted).

### Constraints
- Do NOT promote to production until testing requirement is satisfied and owner approves.
- All tester data should be handled per privacy expectations (testers' device info stays with developer, not published).

## PLAY.7: Production Release

Goal: publish Local Find on Google Play.

### Prerequisites
- PLAY.6 complete (closed testing required, if applicable) OR confirmed not required.
- PLAY.5 complete (internal testing smoke test passed).
- Owner explicitly approves production release.

### Tasks

1. **Final review**
   - All Play Console forms complete and accurate.
   - All assets uploaded and correct.
   - Privacy policy URL reachable.
   - Support email confirmed.

2. **Create production track** and upload (or promote) the tested AAB.

3. **Submit for review**
   - Owner manually clicks "Submit for review" in Play Console.
   - Do not submit until all prerequisites are met and owner approves.

4. **Post-submission**
   - Record submission status and date.
   - Do not modify app, AAB, or listing while review is pending.
   - If approved: record the public Play Store URL.
   - If rejected or changes requested: capture the exact Play Console message before modifying anything.

### Deliverables
- Production track with submitted AAB.
- Submission status documented.

### Constraints
- Do NOT submit for production review without owner's explicit approval.
- Do NOT modify the published GitHub MVP-U.5 Release.
- Do NOT move the `mvp-u5-ok` tag.

## Release Blockers Summary

| # | Blocker | Phase | Status |
|---|---------|-------|--------|
| 1 | ~~Google Play account type not confirmed~~ | PLAY.1 | **RESOLVED — Personal** |
| 2 | ~~Production access path not confirmed~~ | PLAY.1 | **RESOLVED — closed testing required** |
| 3 | ~~Upload keystore existence not confirmed~~ | PLAY.1 | **RESOLVED** |
| 4 | Custom app icon missing | PLAY.2 | Open |
| 5 | Feature graphic missing | PLAY.2 | Open |
| 6 | Phone screenshots missing | PLAY.2 | Open |
| 7 | Data Safety form not completed | PLAY.3 | **Resolved PLAY.3B** |
| 8 | Foreground Service declaration not submitted | PLAY.3 | **Resolved PLAY.3B** |
| 9 | App content declarations not completed | PLAY.3 | **Resolved PLAY.3B** |
| 10 | Category and tags not decided | PLAY.3 | **Resolved PLAY.3A** |

## Post-Release Considerations

After production release:
- Document the Play Store listing URL.
- Create a `GOOGLE_PLAY_RELEASE_CLOSEOUT.md`.
- Update `PROJECT_STATUS.md` with Play release status.
- Update `CHANGELOG.md` if maintained.
- Do NOT remove or replace the GitHub MVP-U.5 Release.
- Do NOT rebase or reset the repository to "clean up" pre-Play history.

## Constraints (All Phases)

These constraints apply to EVERY phase of the Google Play release path:

- Do NOT modify Chrome extension code.
- Do NOT build APK (only AAB for Play).
- Do NOT commit `local.properties`.
- Do NOT commit `app-release.aab`.
- Do NOT commit `.jks` or `.keystore` files.
- Do NOT commit signing passwords or key material in any file.
- Do NOT move the `mvp-u5-ok` tag.
- Do NOT modify the published GitHub MVP-U.5 Release.
- Do NOT restore the Android I.0 WIP stash unless explicitly directed.
- Do NOT reset or force-push the repository.
- Do NOT submit for production review without owner's explicit approval.
