# Chrome Web Store Screenshot Draft Review

## Purpose

This document records the Chrome Web Store screenshot draft review result.

The draft screenshots remain outside the repository.

This phase does not commit PNG, JPEG, or SVG files.

## Draft Location

```text
D:\local-find-screenshots-draft\
```

## Draft Screenshot Set

- `01-popup-paired-device.png`
- `02-controller-actions.png`
- `03-pairing-or-manual-host.png`
- `04-language-switching.png`

## Review Result

Screenshot draft review result: PASS FOR DRAFT

## Per-Screenshot Review

### 1. `01-popup-paired-device.png`

- status: fallback acceptable
- shows main popup state / Current Device / controls / Paired Phones / Add Phone top
- limitation: uses Manual / N/A state rather than a true paired demo device

### 2. `02-controller-actions.png`

- status: acceptable
- shows Find Phone / Stop All Alerts / Flash / Stop Flash / Status / Diagnostics

### 3. `03-pairing-or-manual-host.png`

- status: acceptable
- shows Add Phone / IP Address / Port / Send Pairing Request
- safe demo value 192.168.1.108 / 8888 may be used where visible
- no real token shown

### 4. `04-language-switching.png`

- status: acceptable
- shows language selector with System / English / 简体中文

## Privacy Review

- no real device name
- no real LAN IP
- no real token
- no controller id
- no private browser content
- no notifications
- English UI
- HOST:8888 placeholder accepted as safer than real IP

## Known Limitations

- screenshots are not yet committed as store assets
- HOST:8888 is visually less polished than a real-looking demo IP
- 01 screenshot is fallback because it does not show a true paired demo device
- Chrome Web Store acceptance is not guaranteed
- screenshots may still need resizing/cropping/polish before final upload

## Decision

- Do not replace HOST:8888 with real IP.
- Keep current drafts as safe screenshot candidates.
- Do not commit screenshots in CWS.11C.
- Next phase may either:
  - A. commit reviewed screenshot assets to store-assets path, or
  - B. do an image polish/crop/size pass before commit.
