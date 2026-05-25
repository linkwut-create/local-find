# Chrome Web Store Readiness Audit

Audit date: 2026-05-25

Scope: read-only Chrome Extension / Chrome Web Store readiness audit for `D:\local-find`.

This audit did not modify Android code, Chrome extension code, release artifacts, tags, or the published GitHub Release. The only intended repository change for CWS.0 is this document.

## Repository State

Commands recorded during the audit:

```text
git status --short
```

Result:

```text

```

```text
git describe --tags --dirty
```

Result:

```text
mvp-u5-ok-10-g53d71a7
```

```text
git log --oneline -8
```

Result:

```text
53d71a7 docs: add Google Play asset validation helper
96bf30f docs: draft Google Play asset source notes
6bea73a docs: add Google Play asset capture kit
188a469 docs: plan Google Play store assets
12b5c00 docs: draft Google Play listing and safety materials
5544343 build: add release signing config for Play readiness
0eacb4d build: target Android 15 for Play readiness
be9789e build: migrate Android package identity
```

Note: the user-provided latest known commit was `96bf30f`; the current audited HEAD is `53d71a7`.

## Chrome Extension File Inventory

`chrome-extension/` exists and currently contains:

```text
i18n.js
manifest.json
popup.css
popup.html
popup.js
README.md
```

Main UI files are present:

- `popup.html`
- `popup.js`
- `popup.css`
- `i18n.js`

`manifest.json` exists.

No extension icons were found under `chrome-extension/`. No PNG, JPEG, SVG, ICO, or WebP assets were found in the extension directory.

No packaged extension artifacts or obvious temporary files were found under `chrome-extension/`:

- no `.zip`
- no `.crx`
- no `.pem` / `.key`
- no `dist/`
- no `build/`
- no `node_modules/`
- no `.DS_Store` / `Thumbs.db`

`chrome-extension/README.md` explains unpacked installation via `chrome://extensions`, Developer mode, and selecting the `chrome-extension` directory.

## Manifest Audit

Manifest file: `chrome-extension/manifest.json`

```json
{
  "manifest_version": 3,
  "name": "Local Find",
  "version": "0.1.0",
  "description": "Minimal popup controller for the Local Find Android HTTP service.",
  "action": {
    "default_title": "Local Find",
    "default_popup": "popup.html"
  },
  "permissions": [
    "storage"
  ],
  "host_permissions": [
    "http://*/*"
  ]
}
```

Manifest fields:

| Field | Current value |
| --- | --- |
| `manifest_version` | `3` |
| `name` | `Local Find` |
| `version` | `0.1.0` |
| `description` | `Minimal popup controller for the Local Find Android HTTP service.` |
| `action.default_title` | `Local Find` |
| `action.default_popup` | `popup.html` |
| `permissions` | `storage` |
| `host_permissions` | `http://*/*` |
| `icons` | absent |
| `background.service_worker` | absent |
| `content_scripts` | absent |
| `externally_connectable` | absent |
| `web_accessible_resources` | absent |

Manifest readiness judgment:

- The extension is still Manifest V3.
- The only extension API permission is `storage`, which matches local persistence for host, port, language, pairing records, tokens, and local protection settings.
- `host_permissions` is broad: `http://*/*`. This does not match `<all_urls>` or HTTPS wildcards, but it still grants access to any HTTP origin, not only private LAN ranges.
- No `tabs`, `downloads`, `cookies`, `webRequest`, `scripting`, `nativeMessaging`, `history`, or clipboard permissions are requested.
- No background worker or content script is declared, which lowers page-content and browsing-history risk.
- No remote code loading was found. `popup.html` loads only local `i18n.js` and `popup.js`; no external script URL was found.

The broad `http://*/*` host permission is the main manifest review risk. It supports user-entered LAN phone addresses but may require a Chrome Web Store justification or a narrower future design.

## Privacy And Data Handling Audit

Observed behavior from `chrome-extension/README.md`, `popup.js`, `popup.html`, and `PRIVACY.md`:

- The extension uses `chrome.storage.local`.
- It can store LAN host, port, selected device id, language preference, paired devices, pairing metadata, `controllerId`, `controlToken`, legacy saved token when the user opts in, local PIN salt/hash, WebAuthn credential reference fields, and last-success timestamps.
- Legacy token saving is opt-in through `rememberToken`.
- Paired-device `controlToken` is saved after phone-side pairing acceptance.
- Local PIN is hashed with PBKDF2 and stored with salt, not saved in plain text.
- The extension sends local HTTP requests to user-entered phone hosts, including status, pairing, revoke, ring, flash, and stop commands.
- No evidence was found that the extension reads webpage content.
- No content script or background worker is declared.
- No evidence was found that it accesses browser history, cookies, tabs, downloads, or webRequest data.
- No evidence was found that it sends data to a cloud service or remote internet server.
- Communication is designed for LAN/local-network Android service endpoints.

Existing `PRIVACY.md` covers the local-first model, no cloud account, no uploads, no SMS, no background location tracking, local storage of device name / LAN IP / port / pairing tokens / saved devices / browser protection settings, and user control for deletion/revocation.

Chrome Web Store privacy disclosure is still needed because the extension stores local device metadata and tokens and has host access to HTTP origins. Even if no data is collected server-side, the store listing should explicitly disclose:

- local-only storage in `chrome.storage.local`;
- saved paired phone records;
- LAN IP/port;
- control tokens / optional legacy token;
- no webpage content access;
- no browser history access;
- no cloud upload by Local Find;
- same-LAN HTTP communication with user-entered Android devices.

`PRIVACY.md` is a good base, but a Chrome Web Store-specific privacy paragraph or hosted public privacy policy URL should be prepared before submission.

## Store Listing Readiness

| Item | Current status | Notes |
| --- | --- | --- |
| Extension title | Present | Manifest name is `Local Find`. |
| Short description | Present but draft | Manifest description exists; may need CWS-length polish. |
| Detailed description | Partial | README has enough source material, but no CWS-specific listing draft exists. |
| Category | Missing | Likely candidate: Productivity or Utilities, pending store strategy. |
| Language | Partial | Extension supports English and Simplified Chinese; default store listing language still needs selection. |
| 128x128 icon | Missing | No extension image assets found. |
| Screenshots | Missing | No Chrome Web Store screenshots found. |
| Small promotional tile | Missing / optional | Prepare if desired. |
| Marquee / large promotional images | Missing / optional | Prepare only if using promotional placements. |
| Support email | Missing | Existing Google Play docs still mark support email as TODO. |
| Privacy policy URL | Missing | `PRIVACY.md` exists locally, but public HTTPS URL is TODO. |
| GitHub repo URL | Present | `https://github.com/linkwut-create/local-find` appears in docs. |
| GitHub Release URL | Present | `https://github.com/linkwut-create/local-find/releases/tag/mvp-u5-ok`. Do not modify the release. |

## Packaging Readiness

This audit did not generate a zip.

Future Chrome extension package should include the extension source files from `chrome-extension/`:

- `manifest.json`
- `popup.html`
- `popup.js`
- `popup.css`
- `i18n.js`
- extension icons once added
- any required static assets added later

`chrome-extension/README.md` is useful for repository documentation but is not required for the store package. Include or exclude it deliberately.

Future package must exclude:

- `android/`
- `.git/` and all Git metadata
- `docs/`
- `store-assets/`
- `tools/`
- `local.properties`
- signing files and keystores
- `app-release.aab`
- release APK/AAB artifacts
- `node_modules/` if ever added
- test outputs, local LLM output, and audit output such as `.local_llm_out/` or `.mcp_audit/`

Because the current package source is a flat `chrome-extension/` directory with no build system, packaging risk is manageable once icons and listing assets exist.

## Risk Assessment

| Finding | Severity | Evidence | Recommended next step |
| --- | --- | --- | --- |
| Chrome Web Store icon and screenshot assets are missing. | blocker | No image assets were found under `chrome-extension/`; no Chrome Web Store asset directory exists. | CWS.1 should create a listing/assets plan and define 128x128 icon plus screenshots. |
| `manifest.json` lacks an `icons` field. | blocker | Manifest contains MV3 metadata, action, permissions, and host permissions only. | Add package icons before store submission. |
| `host_permissions` uses broad `http://*/*`. | warning | Manifest host permissions are `http://*/*`. | Decide whether to keep with strong store justification or redesign toward narrower/user-activated host access if possible. |
| Chrome Web Store privacy disclosure is not yet prepared. | warning | `PRIVACY.md` exists, but public privacy policy URL is TODO in store-readiness docs. Extension stores LAN host/port and tokens locally. | Prepare a public privacy policy URL and Chrome Web Store-specific disclosure text. |
| Support email is missing. | warning | Existing Google Play planning docs mark support email as TODO. | Define support/contact email before CWS submission. |
| Store listing details are incomplete. | warning | README has source copy, but no CWS listing draft, category, or finalized language strategy exists. | Draft title, short description, detailed description, category, language, and support fields. |
| Extension does not request high-risk Chrome APIs. | info | Manifest requests only `storage` plus `http://*/*`; no tabs/history/cookies/webRequest/scripting/nativeMessaging. | Preserve this low-permission API surface. |
| No content scripts or background worker are declared. | info | Manifest has no `content_scripts` or `background.service_worker`. | Preserve unless a future CWS plan explicitly needs them. |
| No remote code loading was found. | info | `popup.html` loads only local `i18n.js` and `popup.js`; no external script URL found. | Keep all executable code bundled with the extension. |
| Packaging source is simple and isolated. | info | `chrome-extension/` contains only six flat files and no build artifacts. | Package only the extension directory when CWS packaging begins. |

## Next-Step Recommendation

CWS.1 should be a **listing/assets plan** with a parallel **manifest/privacy cleanup plan** section.

Recommended CWS.1 tasks:

1. Define Chrome Web Store listing copy: title, short description, detailed description, category, language, support email, privacy policy URL, GitHub repo URL, and GitHub Release URL.
2. Plan required assets: 128x128 icon and screenshots first; promotional tiles only if needed.
3. Decide how to handle `http://*/*` host permission:
   - keep it with explicit justification for user-entered LAN Android devices; or
   - explore a narrower permission model if Chrome extension constraints allow it.
4. Add Chrome Web Store-specific privacy wording based on `PRIVACY.md`.
5. Prepare a packaging checklist that packages only `chrome-extension/` contents and excludes Android/release/signing/local files.

Do not generate a package, upload to Chrome Web Store, move `mvp-u5-ok`, or modify the existing GitHub Release during CWS.1 planning.

## CWS.5 Contact And Privacy Readiness Note

CWS.5 added:

- `docs/CHROME_WEB_STORE_CONTACT_AND_PRIVACY_READINESS.md`

Current CWS.5 status:

- support email remains a manual owner decision;
- public privacy policy URL remains a manual owner decision;
- candidate first-pass privacy URL is `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md`, pending owner decision and final public reachability check;
- Chrome Web Store upload should not start until support email and public privacy policy URL are final.

CWS.5 did not modify `PRIVACY.md`, publish a webpage, generate an extension zip, upload to Chrome Web Store, modify the GitHub Release, or change Chrome/Android code.

## CWS.6 Package Dry-Run Note

CWS.6 added:

- `docs/CHROME_WEB_STORE_PACKAGE_DRY_RUN.md`

Current CWS.6 status:

- package source remains `chrome-extension/`;
- future package include list is limited to `manifest.json`, popup files, `i18n.js`, and extension icons;
- `README.md` is recorded as optional for a future store package and may remain repository-only;
- Android, docs, store-assets, tools, Git metadata, local files, signing files, release artifacts, and temporary zip/crx/pem/key files are explicitly excluded from any future package;
- local validation should check manifest JSON, MV3, icon references/dimensions, unchanged CWS.2 host-permission decision, local-only popup scripts, and absence of accidental background/content scripts;
- manual browser validation should use Chrome Developer Mode with `chrome-extension/` loaded unpacked.

CWS.6 did not add a validation script, create a zip, upload to Chrome Web Store, modify `chrome-extension/manifest.json`, modify `chrome-extension/popup.js`, modify Android code, move tags, or modify the `mvp-u5-ok` GitHub Release.

## CWS.7 Local Unpacked Validation Note

CWS.7 added:

- `docs/CHROME_WEB_STORE_LOCAL_UNPACKED_VALIDATION.md`

Current CWS.7 status:

- local unpacked validation plan was added;
- repository baseline and static manifest/file checks were recorded;
- `chrome-extension/manifest.json` parses as JSON;
- `manifest_version` remains `3`;
- `permissions` remains `storage` only;
- `host_permissions` remains `http://*/*`;
- extension icons and popup files are present;
- no background service worker, content scripts, or high-risk browser-data permissions were found;
- `popup.html` loads local `i18n.js` and `popup.js` scripts.

Upload remains blocked by missing real Chrome Web Store screenshots, TODO support email, TODO public privacy policy URL / owner decision, missing final extension zip, and no Chrome Web Store developer account/upload step.

CWS.7 did not launch Chrome automatically, generate a zip, upload to Chrome Web Store, modify `chrome-extension/manifest.json`, modify `chrome-extension/popup.js`, modify Android code, move tags, or modify the `mvp-u5-ok` GitHub Release.

## CWS.8 Manual Unpacked Validation Result Note

CWS.8 updated:

- `docs/CHROME_WEB_STORE_LOCAL_UNPACKED_VALIDATION.md`

Manual unpacked validation result: PASS.

Owner/tester reported:

- Chrome unpacked extension loaded manually.
- Popup rendered.
- Popup DevTools Console opened.
- No console errors were observed.
- No console warnings were observed.
- DevTools showed No Issues.
- Real Android control was not tested.
- Real pairing flow was not tested.

Upload remains blocked by missing real Chrome Web Store screenshots, TODO support email, TODO public privacy policy URL / owner decision, missing final extension zip, and no Chrome Web Store developer account/upload step.

CWS.8 did not modify Chrome extension code, modify Android code, change `host_permissions`, generate a zip, upload to Chrome Web Store, move tags, or modify the `mvp-u5-ok` GitHub Release.

## CWS.9 Screenshot Preflight Note

CWS.9 added:

- `docs/CHROME_WEB_STORE_SCREENSHOT_PREFLIGHT.md`

Current CWS.9 status:

- screenshot safety preflight was added;
- current owner screenshots are validation evidence only, not store assets;
- current owner screenshots should not be committed or submitted because they show real device name and real LAN IP;
- final screenshot capture should use a clean Chrome profile if practical, or clean extension storage with safe demo values;
- CWS.9 does not generate final screenshot assets.

Upload remains blocked by missing final Chrome Web Store screenshots, TODO support email, TODO public privacy policy URL / owner decision, missing final extension zip, and no Chrome Web Store developer account/upload step.

CWS.9 did not modify Chrome extension code, modify Android code, change `host_permissions`, add real screenshot assets, generate a zip, upload to Chrome Web Store, move tags, or modify the `mvp-u5-ok` GitHub Release.

## CWS.10 Screenshot Capture Strategy Note

CWS.10 added:

- `docs/CHROME_WEB_STORE_SCREENSHOT_CAPTURE_STRATEGY.md`

Current CWS.10 status:

- screenshot strategy selected;
- chosen strategy is a separate clean Chrome profile for final screenshot capture;
- current owner/tester screenshots remain validation evidence only, not store assets;
- safe demo values and before-capture checklist were recorded;
- target screenshot directory remains `store-assets/chrome-web-store/screenshots/en-US/`;
- target screenshot files remain `01-popup-paired-device.png`, `02-controller-actions.png`, `03-pairing-or-manual-host.png`, and `04-language-switching.png`;
- CWS.10 does not generate final screenshot assets.

Upload remains blocked by missing final Chrome Web Store screenshots, TODO support email, TODO public privacy policy URL / owner decision, missing final extension zip, and no Chrome Web Store developer account/upload step.

CWS.10 did not modify Chrome extension code, modify Android code, change `host_permissions`, add real screenshot assets, generate a zip, upload to Chrome Web Store, move tags, or modify the `mvp-u5-ok` GitHub Release.

## CWS.11A Manual Screenshot Capture Instructions Note

CWS.11A added:

- `docs/CHROME_WEB_STORE_MANUAL_SCREENSHOT_CAPTURE.md`

Current CWS.11A status:

- manual clean-profile screenshot capture instructions were added;
- screenshots should be captured in a separate clean Chrome profile;
- English UI remains the first screenshot set target;
- safe demo values remain `Demo Phone`, `Local Find Phone`, `192.168.1.108`, and port `8888`;
- draft screenshots should be saved outside the repository first, for example `D:\local-find-screenshots-draft\`;
- draft screenshots should be reviewed for privacy leakage, visual clarity, language consistency, and absence of real device or network identifiers before any commit;
- CWS.11A does not generate or commit final screenshot assets.

Upload remains blocked by missing final Chrome Web Store screenshots, TODO support email, TODO public privacy policy URL / owner decision, missing final extension zip, and no Chrome Web Store developer account/upload step.

CWS.11A did not modify Chrome extension code, modify Android code, modify `chrome-extension/manifest.json`, modify `chrome-extension/popup.js`, change `host_permissions`, add real screenshot assets, generate a zip, upload to Chrome Web Store, move tags, or modify the `mvp-u5-ok` GitHub Release.

## CWS.11B Screenshot Profile Helper Note

CWS.11B added:

- `tools/open_cws_screenshot_profile.ps1`

CWS.11B updated:

- `docs/CHROME_WEB_STORE_MANUAL_SCREENSHOT_CAPTURE.md`

Current CWS.11B status:

- local helper script was added for opening a clean Chrome profile;
- the helper is intended to load the Local Find unpacked extension from `D:\local-find\chrome-extension`;
- the helper creates or reuses `D:\local-find-cws-chrome-profile` and `D:\local-find-screenshots-draft` when manually run;
- draft screenshots should still be saved outside the repository first;
- the helper does not write to browser extension storage, click the popup, or take screenshots automatically.

Upload remains blocked by missing final Chrome Web Store screenshots, TODO support email, TODO public privacy policy URL / owner decision, missing final extension zip, and no Chrome Web Store developer account/upload step.

CWS.11B did not modify Chrome extension code, modify Android code, modify `chrome-extension/manifest.json`, modify `chrome-extension/popup.js`, change `host_permissions`, add real screenshot assets, generate a zip, upload to Chrome Web Store, move tags, or modify the `mvp-u5-ok` GitHub Release.

## Android Runtime Connectivity Closeout Note

A-DIAG.2 added:

- `docs/ANDROID_RUNTIME_CONNECTIVITY_CLOSEOUT.md`

A-DIAG.2 updated:

- `docs/ANDROID_RUNTIME_CONNECTIVITY_RUNTIME_EVIDENCE.md`

Current Android runtime connectivity status:

- A-DIAG.0 found no static evidence for a localhost-only server bind; current source and `mvp-u5-ok` both use Ktor `host = "0.0.0.0"`, and `/device-info` is public.
- A-DIAG.1 was blocked by adb `unauthorized`.
- A-DIAG.1B succeeded after adb authorization for `461QYFFT225UP`.
- Runtime evidence showed only `io.github.linkwutcreate.localfind` installed; `com.example.localfind` was not installed.
- Runtime evidence showed `wlan0` IP `10.128.21.95/17`, listener `*:8888` / `[::]:8888`, and `:8888 ESTABLISHED` connections.
- The user confirmed external connection succeeded.
- Android runtime connectivity is currently cleared.
- No Android code fix branch was started, and CWS screenshot work may resume.

Upload remains blocked by missing final Chrome Web Store screenshots, TODO support email, TODO public privacy policy URL, missing final extension zip, and no Chrome Web Store developer account/upload step.

A-DIAG.2 did not modify Chrome extension code, modify Android code, modify `chrome-extension/manifest.json`, modify `chrome-extension/popup.js`, modify Android manifest/Gradle/MainActivity/HttpServerManager, generate APK/AAB artifacts, install or uninstall apps, clear app data, generate a Chrome extension zip, upload to Chrome Web Store, move tags, or modify the `mvp-u5-ok` GitHub Release.
