# Google Play Developer Account Status

Audit date: 2026-05-29 (updated PLAY.1C)

Phase: PLAY.1 — Account and signing confirmation. **COMPLETE.**

## App Identity

| Item | Status |
|------|--------|
| App name in Play Console | **Local Find** — created |
| `applicationId` | `io.github.linkwutcreate.localfind` |
| App status in Play Console | Setup in progress / not yet submitted |

## Account Identity

| Item | Status |
|------|--------|
| Google Play developer account | Registered |
| Account verification | Verified by owner |
| Account type | **Personal** — confirmed by owner via Play Console (2026-05-29) |
| D-U-N-S number | N/A (personal account) |

### Account type confirmed

Owner confirmed via Play Console > Developer Account > Account details that the account type is **Personal**.

### Implications

As a **personal account**, the following Google Play policies apply:

- **Closed testing required**: Personal accounts created after November 2023 require 12+ testers for 14+ days of closed testing before production access.
- Organization-only features (D-U-N-S verification, organization display name) are not applicable.

## Production Access

| Item | Status |
|------|--------|
| Production access granted | **No** — requires closed testing + application |
| Closed testing required | **Yes — confirmed** — Play Console Dashboard explicitly states closed testing required before production |
| Production path | Complete app setup → Complete closed testing (12+ testers, 14+ days) → Apply for production access |
| Internal testing track available | **Yes** — available as first testing path |
| Closed testing track available | **TODO: owner to confirm** (likely available after app setup is complete) |
| Production track available | **No** — locked until closed testing + application process complete |

### Production access confirmed

Owner checked Play Console > Dashboard. The dashboard explicitly states (translated): "To publish to all users, you need to complete app setup, complete closed testing, and apply for production access."

This confirms:
- Direct production access is NOT available for this personal account.
- Closed testing is MANDATORY before production.
- The path is: **app setup → closed testing → apply for production access**.

### Policy reference

Google Play policy (effective November 2023): new personal developer accounts must complete a closed testing period (12+ testers, 14+ days minimum) before production access is granted. Organization accounts and accounts created before November 2023 may not have this requirement.

Updated Google Play Console help: https://support.google.com/googleplay/android-developer/answer/14151465

## Signing Readiness

| Item | Status |
|------|--------|
| `local.properties` exists | **Yes** — `android/local.properties`, gitignored |
| `LOCAL_FIND_UPLOAD_STORE_FILE` | **Set** — keystore file exists at configured path |
| `LOCAL_FIND_UPLOAD_STORE_PASSWORD` | **Set** (value not recorded) |
| `LOCAL_FIND_UPLOAD_KEY_ALIAS` | **Set** — `localfind-upload` |
| `LOCAL_FIND_UPLOAD_KEY_PASSWORD` | **Set** (value not recorded) |
| All 4 signing variables present | **Yes** (`hasReleaseSigningConfig` = true) |
| Keystore file exists on disk | **Yes** — `local-find-upload.jks` (2796 bytes, created 2026-05-24) |
| `.jks` / `.keystore` in repo | **None** |
| Key alias uniqueness | **Confirmed** — `localfind-upload` has not been used for any other Google Play app. This is the first Play app using this upload key (owner confirmed 2026-05-29). |

Signing readiness verdict: **Signing config is complete and keystore is present. Key uniqueness confirmed.** The Gradle `hasReleaseSigningConfig` check will pass. Release AAB build signing is fully ready.

## Environment Check (2026-05-29)

Commands run:

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

`android/local.properties` is tracked by `.gitignore` — `git check-ignore` confirms it is excluded.

## Open TODOs (owner action required)

1. ~~Confirm account type~~ — **DONE: Personal** (confirmed 2026-05-29).
2. ~~Confirm production access requirements~~ — **DONE: closed testing required, then apply for production** (confirmed 2026-05-29 via Play Console Dashboard).
3. ~~Confirm key uniqueness~~ — **DONE: confirmed not used for any other Play app** (owner confirmed 2026-05-29).
4. **Confirm closed testing track availability** — verify in Play Console > Testing > Closed testing.

## Constraints (this phase)

- Do NOT commit `local.properties`.
- Do NOT commit keystore files.
- Do NOT record passwords or key material.
- Do NOT modify Android code.
- Do NOT build APK/AAB.
