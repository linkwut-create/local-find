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

Status: PLAY.5B — Internal testing release installable.

| Field | Value |
|-------|-------|
| `applicationId` | `io.github.linkwutcreate.localfind` |
| `versionCode` | `1` |
| `versionName` | `"1.0"` |
| `targetSdk` | `35` |
| Developer account | Registered, verified by owner |
| Account type | **Personal** — confirmed (2026-05-29) |
| Production access | Not directly available — requires closed testing + application |
| Production path | App setup → closed testing (12+ testers, 14+ days) → apply for production |
| Closed testing required | **Yes — confirmed** (Play Console Dashboard) |
| Internal testing | Available |
| Upload keystore | **Confirmed** — exists, all 4 signing variables set |
| Key alias | `localfind-upload` |
| Release AAB signing | `hasReleaseSigningConfig` = true |
| **Release AAB** | **Uploaded** — internal testing track, `1.0-internal-1`, installable |
| AAB SHA256 | `DD86A3466DDFF385757FF4B7D8679ECF59CD9289898C4D21C78B201DFC7B4341` |
| Internal testing | **Installable** — 1 tester, owner confirmed |
| Play Console forms | **Completed (draft)** — not submitted |
| Production | Not requested |

Next: PLAY.5C — internal testing smoke test.

Reference docs:
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
