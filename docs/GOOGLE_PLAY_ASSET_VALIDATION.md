# Google Play Asset Validation

## Purpose

`tools/validate_google_play_assets.py` checks future Google Play image assets against the file format and dimension constraints recorded in the asset planning docs.

This helper is intended for local validation before uploading assets to Google Play Console. It does not generate images and does not upload anything.

## Expected files

The default asset root is `store-assets/google-play`.

Expected files:

```text
store-assets/google-play/icon/local-find-icon-512.png
store-assets/google-play/feature-graphic/local-find-feature-1024x500.png
store-assets/google-play/screenshots/en-US/01-find-me-service.png
store-assets/google-play/screenshots/en-US/02-controller-devices.png
store-assets/google-play/screenshots/en-US/03-qr-pairing.png
store-assets/google-play/screenshots/en-US/04-language-settings.png
```

## Strict mode

Use strict mode after final assets exist:

```powershell
python tools/validate_google_play_assets.py
```

Strict mode reports missing files as `MISSING` and exits with code `1` when any required asset is absent.

## Current pre-asset mode

Use `--allow-missing` while the repository contains only planning files:

```powershell
python tools/validate_google_play_assets.py --allow-missing
```

In this mode, missing files are still reported as `MISSING`, but missing expected assets do not fail the command.

To validate a custom asset root:

```powershell
python tools/validate_google_play_assets.py --root store-assets/google-play
```

## What the script checks

- Expected asset paths.
- PNG signature and IHDR width, height, bit depth, and color type.
- JPEG SOF width and height.
- App icon:
  - PNG only.
  - Exactly 512x512.
  - Maximum 1024 KB.
  - PNG alpha required.
- Feature graphic:
  - PNG or JPEG.
  - Exactly 1024x500.
  - PNG alpha forbidden.
  - JPEG treated as no alpha.
- Phone screenshots:
  - PNG or JPEG.
  - PNG alpha forbidden.
  - JPEG treated as no alpha.
  - Minimum dimension at least 320 px.
  - Maximum dimension at most 3840 px.
  - Maximum dimension no more than 2x the minimum dimension.
  - Non-portrait screenshots produce `WARNING`, not `ERROR`.

## What the script deliberately does not check

- Visual quality.
- Marketing copy accuracy.
- Policy interpretation.
- Play Console upload success.
- Whether screenshots contain private information.
- Whether screenshots match a localized Play listing.
- Whether the feature graphic or icon is aesthetically acceptable.

## Reminder

- Final Play upload waits until Play Console verification is complete.
- `mvp-u5-ok` tag and the GitHub Release remain frozen.
- Do not commit `app-release.aab`, `local.properties`, keystores, passwords, or generated build outputs.
