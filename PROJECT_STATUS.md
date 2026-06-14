# Local Find Project Status

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

Status: Submitted for review / pending review.

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

No changes to Chrome extension code, manifest, assets, or package while review is pending unless Chrome Web Store requests changes.

## Google Play Release

Status: **PLAY.7B-FIX16K - 16 KB compatible replacement AAB built locally; not uploaded.**

| Field | Value |
|-------|-------|
| `applicationId` | `io.github.linkwutcreate.localfind` |
| `versionCode` | `2` |
| `versionName` | `"1.0.1"` |
| `targetSdk` | `35` |
| Developer account | Registered, verified by owner |
| Account type | **Personal** — confirmed (2026-05-29) |
| Production access | **Approved** - app can create a production release |
| Production path | App setup -> closed testing -> production access application -> approved |
| Closed testing required | **Satisfied** - 12+ testers for at least 14 days |
| Closed testing release | **Published** |
| Testers Community report | **Available** |
| Tester feedback | No critical crashes or blocking bugs reported |
| Future improvements | Onboarding, help/FAQ, store listing, feedback/rating entry |
| Internal testing | Available |
| Upload keystore | **Confirmed** — exists, all 4 signing variables set |
| Key alias | `localfind-upload` |
| Release AAB signing | `hasReleaseSigningConfig` = true |
| Previous AAB | `versionCode 1` — uploaded to testing, blocked for production by 16 KB page-size requirement |
| Replacement AAB | **Built and signed locally; not uploaded** |
| Replacement AAB path | `android/app/build/outputs/bundle/release/app-release.aab` |
| Replacement AAB size | 20,484,249 bytes |
| Replacement AAB SHA256 | `447178CCCC6AE1DEAF26320A35E48338CD5D0127CF1C427140DDA527D19ECBFC` |
| 16 KB verification | **PASS** — 64-bit ELF alignment, `PAGE_ALIGNMENT_16K`, and `zipalign -P 16` |
| Internal testing | **Smoke test PASS** — 8/8 checks passed, no blocker |
| Play Console forms | **Completed (draft)** — not submitted |
| Production release | **Not created** — waiting for approval to upload `versionCode 2` |

Next: obtain explicit approval before uploading the `versionCode 2` AAB or creating the production release.

Reference docs:
- `docs/GOOGLE_PLAY_PRODUCTION_ACCESS_APPLICATION.md`
- `docs/GOOGLE_PLAY_AAB_BUILD_AUDIT.md`
- `docs/GOOGLE_PLAY_RELEASE_READINESS.md`
- `docs/GOOGLE_PLAY_RELEASE_PLAN.md`
- `docs/GOOGLE_PLAY_DEVELOPER_ACCOUNT_STATUS.md`
- `docs/GOOGLE_PLAY_STORE_ASSET_PRODUCTION_PLAN.md` (new)

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
