# Chrome Web Store Assets

This directory is for Chrome Web Store listing assets and capture planning for the Local Find Chrome extension.

## Directory Purpose

Use this directory for store-listing assets that are outside the extension package itself, such as screenshots and optional promotional images.

CWS.4 creates the screenshot directory structure only. It does not add final screenshot files.

## Screenshots Directory

Screenshot planning lives under:

```text
store-assets/chrome-web-store/screenshots/
```

The default first screenshot set is English:

```text
store-assets/chrome-web-store/screenshots/en-US/
```

Optional Simplified Chinese localized screenshots may be prepared later:

```text
store-assets/chrome-web-store/screenshots/zh-CN/
```

## Icons

Extension package icons are currently stored under:

```text
chrome-extension/icons/
```

They are package icons referenced by `chrome-extension/manifest.json`, not store listing assets under `store-assets/`.

## CWS.4 Boundary

CWS.4 does not:

- generate screenshots;
- generate an extension zip;
- upload to Chrome Web Store;
- upload to Google Play;
- modify Android code;
- modify Chrome extension functionality;
- modify `chrome-extension/manifest.json`;
- change `host_permissions`;
- move tags or modify the `mvp-u5-ok` GitHub Release.
