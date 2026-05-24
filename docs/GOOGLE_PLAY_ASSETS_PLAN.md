# Google Play Store Assets Plan

Status: Draft plan for Google Play store assets. Do not upload to Play Console until account verification, release QA, and policy review are complete.

Reference links:

- GitHub repo URL: https://github.com/linkwut-create/local-find
- GitHub Release URL: https://github.com/linkwut-create/local-find/releases/tag/mvp-u5-ok
- Privacy policy public URL: TODO
- Support email: TODO

## Required assets

Prepare the following before Play upload:

- App name: Local Find.
- Short description from `docs/GOOGLE_PLAY_LISTING_DRAFT.md`.
- Full description from `docs/GOOGLE_PLAY_LISTING_DRAFT.md`.
- 512x512 app icon.
- 1024x500 feature graphic.
- Phone screenshots.
- Optional short demo video.
- Privacy policy public URL.
- Support email.
- GitHub repo URL.
- GitHub Release URL.
- Data Safety draft.
- Foreground Service declaration draft.
- Closed testing instructions.

## Screenshot plan

Use English screenshots first. Capture clean, realistic phone screens with no personal data, real home IPs, or real tokens. Use test device names and redacted or dummy tokens where possible.

Minimum screenshot set:

| # | Screen | Purpose | Notes |
|---|--------|---------|-------|
| 1 | Find Me / finder service start page | Shows the Android phone can start the local find service. | Capture service status, local-only wording, and stop controls if visible. |
| 2 | Controller / connected device page | Shows a paired controller can connect to a local phone. | Use a test device name and avoid exposing a real LAN IP if possible. |
| 3 | QR pairing scanner page | Shows camera-based local pairing. | Use a sample QR code or framing UI without exposing a real token. |
| 4 | Language settings page | Shows language selection. | Capture English mode for the primary listing. |

Optional screenshots:

- Find action in progress, showing ring or flashlight state.
- Stop All action available after a find action starts.
- Saved devices list with a test phone record.
- Pairing request accepted state.

Screenshot quality checklist:

- Use portrait phone screenshots.
- Keep status bar clean.
- Avoid notification clutter.
- Avoid mixed English and Chinese in the same screenshot.
- Avoid real IP addresses, real tokens, personal device names, or private network details.
- Show the current release identity only if it is already user-facing and accurate.

## Screenshot language strategy

Primary strategy:

- English screenshots first.
- Use one language per screenshot.
- Do not mix English and Chinese text in the same screenshot.
- Use English screenshots for the default Play listing unless a localized Chinese listing is also prepared.

Optional Chinese strategy:

- Add separate Simplified Chinese screenshots only if a Chinese store listing is created.
- Keep the Chinese screenshots parallel to the English set: service start, controller connection, QR scanner, language settings.
- Do not use bilingual screenshots as a substitute for localization.

## Feature graphic concept

Required size: 1024x500.

Concept direction:

- Show Local Find as a local-network phone finder, not an anti-theft product.
- Visual idea: a phone on the left with a subtle local Wi-Fi/LAN signal and a desktop or Chrome controller on the right.
- Include the product name "Local Find".
- Emphasize "Local network phone finder" or similar short copy.
- Avoid cloud imagery, map pins, GPS tracking visuals, police/security themes, or stolen-phone recovery claims.
- Avoid showing real device data, tokens, IP addresses, or QR contents.

Recommended copy options:

- "Find your phone on your local network"
- "LAN-only phone finder"
- "Local pairing. Local control."

## Icon guidance

Required size: 512x512.

Icon goals:

- Simple, readable at small sizes.
- Use a phone plus search/radar/flash cue.
- Avoid cloud, GPS pin, shield, police, or anti-theft imagery.
- Avoid text inside the icon.
- Use a clean vector source that can export 512x512 PNG.
- Keep enough padding for adaptive icon masking.

Deliverables:

- 512x512 Play icon PNG.
- Optional adaptive icon foreground/background source for Android app polish.
- Source file for future edits.

## Support/contact checklist

Prepare before Play upload:

- Support email: TODO.
- Privacy policy public HTTPS URL: TODO.
- GitHub repo URL: https://github.com/linkwut-create/local-find.
- GitHub Release URL: https://github.com/linkwut-create/local-find/releases/tag/mvp-u5-ok.
- Security reporting route: GitHub security/private vulnerability reporting if enabled, or GitHub issues for non-sensitive reports.
- Tester feedback route for closed testing.

Support response scope:

- Local network setup help.
- Pairing and token reset guidance.
- Device compatibility issues.
- Permission explanation.
- Privacy and Data Safety questions.

## Pre-upload checklist

Before uploading to Google Play Console:

- Play Console phone verification is complete.
- Final app package name is `io.github.linkwutcreate.localfind`.
- Release AAB is generated from the intended commit.
- Release AAB is signed with the upload key.
- No keystore, passwords, `local.properties`, or AAB files are committed.
- 512x512 app icon is ready.
- 1024x500 feature graphic is ready.
- English phone screenshots are ready.
- Optional Chinese screenshots are separated from English screenshots.
- Privacy policy has a public HTTPS URL.
- Support email is ready.
- Data Safety form matches `PRIVACY.md`.
- Foreground Service declaration matches the app behavior.
- Demo video is ready if required for foreground service review.
- Closed testing plan is ready.
- Tester instructions are ready.
- GitHub Release URL is available for reference, but Play upload uses the release AAB, not the GitHub MVP-U.5 debug package.
