# Local Find Project Status

## 1.1.1 Publish (2026-09-25)

Status: Play, Chrome Web Store and GitHub all updated for 1.1.1 in one session.

| Field | Value |
|-------|-------|
| GitHub Release | [v1.1.1](https://github.com/linkwut-create/local-find/releases/tag/v1.1.1) |
| GitHub assets | `local-find-1.1.1-release.apk`, `local-find-chrome-extension-1.1.1.zip`, `SHA256SUMS.txt` (no AAB, no signing material) |
| `master` push | `git rev-list --count origin/master..master` = 0 after push |
| Google Play | `versionCode 4` (1.1.1) submitted to production track for review |
| Chrome Web Store | `1.1.1` package submitted for review; `0.1.0` confirmed published (not pending) |

Full narrative, screenshots and verification commands: `docs/evidence/2026-09-25-publish-1.1.1.md`.
See the Chrome Web Store and Google Play sections below for the corrected historical record.

## MVP-U.5 GitHub Release Closeout

Status: Released on GitHub as a prerelease.

| Field | Value |
|-------|-------|
| Release title | Local Find MVP-U.5 |
| Tag | `mvp-u5-ok` |
| Release URL | https://github.com/linkwut-create/local-find/releases/tag/mvp-u5-ok |
| Asset | `local-find-mvp-u5.zip` |
| Asset SHA256 | `81764E96AD9648CCC3369F54CDB6113DCFB342BEBC9A42D314337B2EB59FB371` |
| Release scope | MVP testing release, not a Play Store production build |

Closeout checks before this docs-only commit:

- Local working tree was clean.
- `git describe --tags --dirty` returned `mvp-u5-ok`.
- `mvp-u5-ok` pointed at `HEAD`.
- Android I.0 WIP stash remained present: `stash@{0}: On master: wip android i0 pairing model before pc endpoint`.
- GitHub Release existed at the URL above.
- Release asset `local-find-mvp-u5.zip` was uploaded with the verified SHA256 above.

No Android source, Chrome extension source, release package, or tag was changed as part of this closeout.

## Chrome Web Store Release

Status: `0.1.0` **published** (not pending — corrected 2026-09-25, see below); `1.1.1` submitted for review 2026-09-25.

| Field | Value |
|-------|-------|
| Extension ID | `nadcejbdnkaihkgddojlokjcfdak` |
| Submission date | 2026-05-28 |
| Package | `dist/chrome-web-store/local-find-chrome-extension.zip` |
| Status | Pending review / 待审核 |
| Support email | `linkwut@gmail.com` |
| Privacy policy URL | `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md` |

Reference docs:
- `docs/CHROME_WEB_STORE_READINESS_AUDIT.md`
- `docs/CHROME_WEB_STORE_SUBMISSION_STATUS.md`

✅ **Resolved 2026-09-25**: the `0.1.0` submission's actual review outcome was
checked in the CWS developer console — status is **"已发布 - 公开发布" (Published,
publicly available)**, not pending as previously recorded. The `1.1.1` package
(`local-find-release/local-find-chrome-extension-1.1.1.zip`) was uploaded as a
new draft package and submitted for review the same day; console shows
"待审核" (pending review) with auto-publish-on-approval enabled. Permissions
unchanged (`storage`, host permission), so no new permission justification was
needed; listing description was checked against current behavior and needed no
changes. Evidence: `docs/evidence/2026-09-25-publish-1.1.1.md`.

## Google Play Release

Status: **`versionCode 3` (1.1.0) passed review and is the live production release** (confirmed
2026-09-25: production track shows "有效"/Active, 177 countries, 9 installs).
**`versionCode 4` (1.1.1) uploaded and submitted for review on 2026-09-25** — see the
2026-09-25 section below for the submission record.

| Field | Value |
|-------|-------|
| `applicationId` | `io.github.linkwutcreate.localfind` |
| `versionCode` (live production) | `2` / `versionName "1.0.1"` / `targetSdk 35` — released 2026-06-14 |
| `versionCode` (submitted for review) | `3` / `versionName "1.1.0"` / `targetSdk 36` — uploaded and submitted 2026-09-13 |
| Developer account | Registered, verified by owner |
| Account type | **Personal** — confirmed (2026-05-29) |
| Production access | **Approved** - app can create a production release |
| Upload key | **Reset 2026-09-10** via Play App Signing (original upload key's password was unrecoverable). New key alias `localfind-upload`, cert SHA-256 `32:59:7A:5D:D1:FD:AA:B3:D5:7D:6D:FD:9F:E9:03:93:FF:D4:FE:29:E2:8F:09:C2:F9:7C:9C:A0:CB:C0:A8:53`. Details: `android/local-find-secrets/SIGNING_KEY_INFO.md` (gitignored). |
| Release AAB signing | `hasReleaseSigningConfig` = true; keystore path resolved relative to `android/` (fixed 2026-09-10, see `docs/android16/AUTOMATED_TEST_REPORT.md` §6.3) |
| `versionCode 3` upload | **Completed 2026-09-13** — 0 errors, 2 advisory warnings (missing deobfuscation/native-debug-symbol files, don't block release). Full runbook and cooldown-period gotcha: `docs/android16/PLAY_UPLOAD_RUNBOOK.md` |
| `versionCode 3` review status | **Passed review, live in production** (confirmed 2026-09-25) |
| `versionCode 4` (1.1.1) upload | **Completed 2026-09-25** — 0 errors, 2 advisory warnings (missing deobfuscation/native-debug-symbol files — build has none, ignored per runbook), submitted for review same day. Version notes in en-US/zh-CN. Details: `docs/evidence/2026-09-25-publish-1.1.1.md`. |
| Closed testing required | **Satisfied** - 12+ testers for at least 14 days |
| Closed testing release | **Published** |
| Testers Community report | **Available** |
| Tester feedback | No critical crashes or blocking bugs reported |

Next: await Google's review outcome for `versionCode 4` (1.1.1).

Reference docs:
- `docs/GOOGLE_PLAY_PRODUCTION_ACCESS_APPLICATION.md`
- `docs/GOOGLE_PLAY_AAB_BUILD_AUDIT.md`
- `docs/GOOGLE_PLAY_RELEASE_READINESS.md`
- `docs/GOOGLE_PLAY_RELEASE_PLAN.md`
- `docs/GOOGLE_PLAY_DEVELOPER_ACCOUNT_STATUS.md`
- `docs/GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md`
- `docs/android16/PLAY_UPLOAD_RUNBOOK.md` (new, 2026-09-10/13)
- `docs/release/1.1.0-readiness.md` (new, 2026-09-24)

### Google Play Release Blockers

| # | Blocker | Status |
|---|---------|--------|
| 1 | ~~Account type not confirmed~~ | **RESOLVED — Personal** |
| 2 | ~~Production access path not confirmed~~ | **RESOLVED — closed testing required** |
| 3 | ~~Upload keystore existence not confirmed~~ | **RESOLVED (PLAY.1)** |
| — | ~~Upload key uniqueness owner confirmation~~ | **RESOLVED — first Play app with this key** |
| 4 | ~~Custom app icon (512x512) missing~~ | **RESOLVED — produced (PLAY.2B)** |
| 5 | ~~Feature graphic (1024x500) missing~~ | **RESOLVED — produced (PLAY.2C)** |
| 6 | ~~Phone screenshots inaccurate (AI-generated)~~ | **RESOLVED — real captures (PLAY.2D3)** |
| 7 | ~~Data Safety form not completed~~ | **Resolved (PLAY.3B)** |
| 8 | ~~Foreground Service declaration not submitted~~ | **Resolved (PLAY.3B)** |
| 9 | ~~App content declarations not completed~~ | **Resolved (PLAY.3B)** |
| 10 | ~~Category and tags not decided~~ | **Resolved (PLAY.3A)** |

No Android code, Chrome extension code, APK/AAB, signing files, or tags are changed as part of PLAY.2A.
