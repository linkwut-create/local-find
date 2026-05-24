# Google Play Asset Capture Guide

Status: Capture kit draft only. This guide prepares file structure and review rules for later Google Play store assets. It does not include real screenshots, generated images, release packages, or Play uploads.

Source for asset constraints:

- Google Play Console Help, "Preview assets to showcase your app": https://support.google.com/googleplay/android-developer/answer/9866151

## Official asset constraints

### App icon

- 512x512.
- 32-bit PNG with alpha.
- Maximum 1024 KB.
- Do not include misleading badges, ranking claims, prices, promotions, or store-category text.
- Do not imply Google Play endorsement.

### Feature graphic

- 1024x500.
- JPEG or 24-bit PNG.
- No alpha.
- Avoid tiny details that disappear on smaller placements.
- Keep key elements near the center.
- Avoid Google Play badges, store logos, ranking claims, pricing, or install/download calls to action.

### Phone screenshots

- JPEG or 24-bit PNG.
- No alpha.
- Minimum dimension: 320 px.
- Maximum dimension: 3840 px.
- Maximum dimension must not be more than 2x the minimum dimension.
- Prepare at least 4 portrait screenshots for app recommendation eligibility.
- Preferred portrait size: 1080x1920 or higher 9:16.

## Screenshot capture scenario

Default screenshot set: English first.

| File | Screen | Main message |
|------|--------|--------------|
| `store-assets/google-play/screenshots/en-US/01-find-me-service.png` | Find Me / finder-side service start page | Device can be made findable on the local network. |
| `store-assets/google-play/screenshots/en-US/02-controller-devices.png` | Controller / connected devices page | Local controller can trigger a finding action. |
| `store-assets/google-play/screenshots/en-US/03-qr-pairing.png` | QR pairing scanner page | LAN pairing and token-based local pairing. |
| `store-assets/google-play/screenshots/en-US/04-language-settings.png` | Language settings page | English and Simplified Chinese language switching. |

Capture notes:

- Use a test phone and test controller.
- Use stable demo device names.
- Use local-demo-looking values where IP addresses are visible.
- Avoid showing real tokens or private controller IDs.
- Capture the actual app UI, not a marketing mockup.

## Language strategy

- Default store screenshots should be English.
- Optional `zh-CN` screenshots may be prepared later as localized listing assets.
- Do not mix English and Chinese in the same screenshot set.
- Do not add extra marketing overlays unless they are localized consistently.
- Avoid time-sensitive words such as "new", "latest", "limited", "discount", and "free".

## Screenshot hygiene checklist

- No personal phone number.
- No real IP address if avoidable; use local-demo-looking values when possible.
- No real Wi-Fi SSID.
- No private tokens.
- No notifications.
- Clean status bar.
- Battery and Wi-Fi indicators are acceptable.
- No unrelated background apps.
- No Play Store badge.
- No install/download call to action.
- No ranking claims.
- No cloud-account claim beyond what the app actually supports.
- No SMS claim beyond saying no SMS is required.
- No background-location implication unless actually implemented and declared.

## File naming convention

Required asset paths:

```text
store-assets/google-play/icon/local-find-icon-512.png
store-assets/google-play/feature-graphic/local-find-feature-1024x500.png
store-assets/google-play/screenshots/en-US/01-find-me-service.png
store-assets/google-play/screenshots/en-US/02-controller-devices.png
store-assets/google-play/screenshots/en-US/03-qr-pairing.png
store-assets/google-play/screenshots/en-US/04-language-settings.png
```

Optional later localized screenshots:

```text
store-assets/google-play/screenshots/zh-CN/01-find-me-service.png
store-assets/google-play/screenshots/zh-CN/02-controller-devices.png
store-assets/google-play/screenshots/zh-CN/03-qr-pairing.png
store-assets/google-play/screenshots/zh-CN/04-language-settings.png
```

## Feature graphic concept

Use one conservative concept:

- Dark or neutral background.
- App icon, phone outline, and local network motif.
- Short text only if needed: "Find your phone on your local network".
- Avoid exaggerated privacy or security promises.
- Avoid saying "no tracking" unless the app behavior and privacy policy support it precisely.
- Avoid Google Play badge or Android robot unless license-safe.

## Icon guidance

- Use a simple phone plus signal/ring motif.
- Must remain legible at small sizes.
- Avoid text inside the icon.
- Avoid shield or lock imagery unless the app is primarily a security product.
- Avoid misleading notification dots or download symbols.
- Keep enough padding for adaptive icon masks if the source is later reused in Android.

## Validation checklist before later upload

- File dimensions checked.
- PNG/JPEG format checked.
- No alpha for feature graphic and screenshots.
- Icon max size checked.
- Screenshot language consistency checked.
- Privacy policy URL ready.
- Support email ready.
- GitHub repo URL ready.
- GitHub release URL ready.
- Play Console account verification complete before upload.
- No `app-release.aab`, `local.properties`, keystore, passwords, or generated build outputs committed.
