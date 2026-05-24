# Google Play Asset Capture Kit

This directory is a placeholder for future Google Play store assets. It intentionally does not contain real screenshots, generated feature graphics, generated icons, release AABs, signing files, or secrets.

Guidance:

- Follow `docs/GOOGLE_PLAY_ASSET_CAPTURE_GUIDE.md`.
- Follow `docs/GOOGLE_PLAY_ASSET_VALIDATION.md` before uploading final image assets.
- Keep English screenshots under `screenshots/en-US/`.
- Keep optional Simplified Chinese screenshots under `screenshots/zh-CN/` if a localized listing is prepared later.
- Keep the 512x512 icon under `icon/`.
- Keep the 1024x500 feature graphic under `feature-graphic/`.
- Do not commit `app-release.aab`, `local.properties`, keystores, passwords, or Play Console exports.

## Validation

Current pre-asset validation mode:

```powershell
python tools/validate_google_play_assets.py --allow-missing
```

See `docs/GOOGLE_PLAY_ASSET_VALIDATION.md` for strict mode and validation details.

Expected future paths:

```text
store-assets/google-play/icon/local-find-icon-512.png
store-assets/google-play/feature-graphic/local-find-feature-1024x500.png
store-assets/google-play/screenshots/en-US/01-find-me-service.png
store-assets/google-play/screenshots/en-US/02-controller-devices.png
store-assets/google-play/screenshots/en-US/03-qr-pairing.png
store-assets/google-play/screenshots/en-US/04-language-settings.png
```
