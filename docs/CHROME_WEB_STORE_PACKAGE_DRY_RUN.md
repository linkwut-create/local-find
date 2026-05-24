# Chrome Web Store Package Dry Run

Plan date: 2026-05-25

Scope: CWS.6 package dry-run checklist and local validation plan for the Local Find Chrome extension. This document does not generate a formal extension zip, upload to Chrome Web Store, modify Android code, change Chrome extension functionality, change `host_permissions`, move `mvp-u5-ok`, or modify the GitHub Release.

## Purpose

This is the Chrome Web Store packaging preflight dry-run plan.

The goal is to define what a future package should include, what it must exclude, and what local checks should run before any package/upload phase begins.

CWS.6 does not:

- generate a formal extension zip;
- upload to Chrome Web Store;
- upload to Google Play;
- modify the `mvp-u5-ok` release or tag;
- modify `chrome-extension/manifest.json`;
- modify `chrome-extension/popup.js`;
- change extension behavior.

## Current Package Source

Package source directory:

```text
chrome-extension/
```

Manifest:

```text
chrome-extension/manifest.json
```

Popup files:

```text
chrome-extension/popup.html
chrome-extension/popup.js
chrome-extension/popup.css
chrome-extension/i18n.js
```

Icons:

```text
chrome-extension/icons/icon-16.png
chrome-extension/icons/icon-32.png
chrome-extension/icons/icon-48.png
chrome-extension/icons/icon-128.png
```

Repository helper documentation:

```text
chrome-extension/README.md
```

## Required Include List

Future Chrome Web Store package should contain only the extension package inputs needed at runtime:

```text
manifest.json
popup.html
popup.js
popup.css
i18n.js
icons/icon-16.png
icons/icon-32.png
icons/icon-48.png
icons/icon-128.png
```

`README.md` inclusion:

- Optional.
- Recommended store package default: do not include `README.md` unless a later package review decides it is useful.
- Keep `chrome-extension/README.md` in the repository for unpacked-install and developer notes.

## Required Exclude List

Future Chrome Web Store zip must exclude:

```text
android/
docs/
store-assets/
tools/
.git/
.github/
app-release.aab
local.properties
signing.properties
node_modules/
.local_llm_out/
.mcp_audit/
```

Future zip must also exclude:

- APK/AAB/build outputs.
- `*.jks`
- `*.keystore`
- `*.p12`
- temporary `*.zip`
- temporary `*.crx`
- temporary `*.pem`
- temporary `*.key`
- editor temp files.
- OS metadata files such as `.DS_Store` and `Thumbs.db`.

Signing files, local machine files, release artifacts, and repository metadata must never be included in the Chrome Web Store package.

## Local Validation Plan

Before a future package is generated, run local preflight checks against `chrome-extension/`.

Manifest checks:

- `manifest.json` parses as JSON.
- `manifest_version` is `3`.
- `name` exists.
- `version` exists.
- `description` exists.
- `action.default_popup` points to `popup.html`.
- `permissions` still only includes `storage`.
- `host_permissions` still matches the approved CWS.2 decision unless changed by a separate approved phase.
- No accidental `content_scripts`.
- No accidental `background.service_worker`.
- No accidental `externally_connectable`.
- No accidental `web_accessible_resources`.

Icon checks:

- `icons` field exists.
- `icons.16` points to `icons/icon-16.png`.
- `icons.32` points to `icons/icon-32.png`.
- `icons.48` points to `icons/icon-48.png`.
- `icons.128` points to `icons/icon-128.png`.
- All referenced icon files exist.
- Icon dimensions are exactly 16x16, 32x32, 48x48, and 128x128.
- PNG files are browser-readable.

Popup/resource checks:

- `popup.html` exists.
- `popup.js` exists.
- `popup.css` exists.
- `i18n.js` exists.
- `popup.html` only loads local extension scripts.
- No remote script loading is introduced.
- No background worker or content script is introduced accidentally.

Package content checks:

- Future package contains only the approved include list.
- Future package does not contain Android files.
- Future package does not contain release artifacts.
- Future package does not contain signing files.
- Future package does not contain local-only files.
- Future package does not contain repository metadata.
- Future package does not contain docs or store listing assets unless a later packaging decision explicitly changes the include list.

Suggested read-only commands before packaging:

```powershell
Get-Content -Encoding UTF8 chrome-extension\manifest.json | ConvertFrom-Json | Out-Null
Get-ChildItem chrome-extension -Recurse -File
git status --short
git diff --stat
git diff --name-only
git diff --check
```

If a validation script is added in a later phase, it should remain read-only and must not create a zip.

## Manual Browser Test Plan

In Chrome Developer Mode, manually load `chrome-extension/` as an unpacked extension.

Manual test checklist:

- Load unpacked succeeds.
- Extension icon appears.
- Popup opens.
- Language selector works.
- Manual host/port fields still render.
- Paired-device UI still renders if saved state exists.
- No console syntax error appears on popup open.
- No unexpected permission prompts appear beyond manifest permissions.
- The extension continues to show the expected local-network controller UI.

Do not use this manual test as an upload step. It is a local unpacked-extension validation pass only.

## Remaining Blockers Before Upload

Chrome Web Store upload should not start while any of the following remain unresolved:

- Real CWS screenshots still not captured.
- Support email still TODO.
- Public privacy policy URL still TODO / owner decision.
- Chrome Web Store developer account may be required for upload.
- No final zip generated yet.
- No Chrome Web Store upload yet.

Additional readiness notes:

- `host_permissions` remains `http://*/*` under the CWS.2 first-submission decision.
- Any host-permission change must be handled in a separate approved phase.
- Any package-generation phase must keep the `mvp-u5-ok` tag and GitHub Release frozen unless explicitly reopened.

## Next Phase Recommendation

Recommended next phase:

- CWS.7: local unpacked extension validation.

Alternative later phase:

- CWS.7: create dry-run zip in an ignored local output directory.

Do not upload to Chrome Web Store in CWS.7 unless a separate explicit upload task is approved.
