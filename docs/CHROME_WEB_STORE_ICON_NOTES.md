# Chrome Web Store Icon Notes

Notes date: 2026-05-25

Scope: CWS.3 readiness icon notes for the Local Find Chrome extension. This document records icon and manifest metadata changes only.

## Icon Files Added

The following Chrome extension package icon files were added:

```text
chrome-extension/icons/icon-16.png
chrome-extension/icons/icon-32.png
chrome-extension/icons/icon-48.png
chrome-extension/icons/icon-128.png
```

These files provide the required extension package icon sizes for Chrome Web Store readiness testing.

## Manifest Icons Field Added

`chrome-extension/manifest.json` now declares:

```json
"icons": {
  "16": "icons/icon-16.png",
  "32": "icons/icon-32.png",
  "48": "icons/icon-48.png",
  "128": "icons/icon-128.png"
}
```

No other manifest fields were intentionally changed.

## Icon Concept

The CWS.3 icon concept is a simple Local Find readiness mark:

- phone silhouette;
- ring/signal arcs;
- local-network controller feel;
- no text;
- no Google Play badge;
- no Chrome logo;
- no Android robot;
- no shield or lock motif.

The design is conservative and meant to represent a local phone finder/controller without implying a security product.

## Readiness Status

These are readiness icons, not final brand assets. A later visual-design pass can replace them if a stronger brand system is desired.

CWS.3 did not:

- change `host_permissions`;
- change popup behavior;
- modify `popup.js`;
- modify Android code;
- add screenshots;
- add promotional assets;
- generate an extension zip;
- upload to Chrome Web Store;
- upload to Google Play;
- move tags or modify the `mvp-u5-ok` GitHub Release.

## Remaining Asset Work

Chrome Web Store screenshots are still missing and remain the next asset blocker before a complete store listing package test.
