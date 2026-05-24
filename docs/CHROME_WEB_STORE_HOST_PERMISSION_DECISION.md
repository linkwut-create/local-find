# Chrome Web Store Host Permission Decision

Decision date: 2026-05-25

Scope: CWS.2 document-only decision for the first Chrome Web Store readiness pass. This document does not modify `chrome-extension/manifest.json`, `chrome-extension/popup.js`, Android code, package artifacts, tags, or releases.

## Current Permission Surface

Current `chrome-extension/manifest.json`:

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

Recorded permission surface:

| Area | Current value |
| --- | --- |
| `permissions` | `storage` |
| `host_permissions` | `http://*/*` |
| `action.default_title` | `Local Find` |
| `action.default_popup` | `popup.html` |
| `background.service_worker` | absent |
| `content_scripts` | absent |
| `externally_connectable` | absent |
| `web_accessible_resources` | absent |

Sensitive Chrome API permissions remain absent:

- `tabs`
- `history`
- `cookies`
- `webRequest`
- `scripting`
- `nativeMessaging`

`popup.html` loads only local extension scripts:

```html
<script src="i18n.js"></script>
<script src="popup.js"></script>
```

No remote script loading was found.

## HTTP Request Call Points

`chrome-extension/popup.js` contains these HTTP request paths:

| Area | Evidence | Target source |
| --- | --- | --- |
| Pairing revoke | `http://${device.host}:${device.port}/pairing/revoke` | saved paired device record |
| Device info check | `${getBaseUrlForTarget(target)}/device-info` | pairing host/port input |
| Pairing request | `${getBaseUrlForTarget(target)}/pairing/request` | pairing host/port input |
| Pairing status poll | `${getBaseUrlForTarget(target)}/pairing/status?...` | pairing host/port input |
| Control commands | `${getBaseUrlForTarget(target)}${command.path}` | selected paired device or manual host/port input |
| Diagnostics page | `${getBaseUrl()}/?lang=${lang}` | selected paired device or manual host/port input |

The command paths are fixed local-service paths:

```text
GET  /status
POST /command/ring/start
POST /command/ring/stop
POST /command/flash/strobe/start
POST /command/flash/stop
POST /command/stop-all
```

`getBaseUrlForTarget(target)` validates that a host exists and the port is valid, then returns:

```js
`http://${host}:${port}`
```

The target comes from either:

- a selected paired device record with saved `host` and `port`; or
- manual host/port fields parsed from user input.

## Why `http://*/*` Exists

The Local Find Chrome extension is a browser controller for the Local Find Android app.

The Android service address may vary by:

- device;
- Wi-Fi network;
- local IP or hostname;
- service port;
- whether the user is in paired-device mode or manual legacy mode.

The popup needs to send HTTP control requests to user-entered or paired Local Find Android service addresses. Because those addresses are not fixed at development time, the current manifest uses:

```json
"host_permissions": [
  "http://*/*"
]
```

## Risk Assessment

`http://*/*` is broader than the ideal Local Find product intent.

Risk details:

- It is not limited to LAN HTTP origins.
- Chrome Web Store review may require a precise explanation for broad HTTP host access.
- The install/review permission posture may look broader than the popup-only local controller behavior.
- Listing and privacy disclosure text must avoid implying that the extension reads webpage content or monitors arbitrary websites.

Chrome Web Store policy context:

- Chrome Web Store permission policy expects extensions to request the narrowest permissions needed for implemented features.
- Chrome extension documentation treats `host_permissions` as the manifest field for match-pattern host access, including extension-page `fetch()` use cases.
- Optional host permission mechanisms exist, but applying them to arbitrary user-entered LAN service addresses would require a separate functional design.

References:

- https://developer.chrome.com/docs/webstore/program-policies/permissions/
- https://developer.chrome.com/docs/extensions/mv3/declare_permissions/
- https://developer.chrome.com/docs/extensions/reference/api/permissions/

## Mitigating Facts

Current mitigating facts:

- No content scripts.
- No background worker.
- No `tabs` permission.
- No `history` permission.
- No `cookies` permission.
- No `webRequest` permission.
- No `scripting` permission.
- No `nativeMessaging` permission.
- Popup-initiated only for control requests.
- No remote script loading.
- User-entered or paired device address.
- No cloud server upload by Local Find.
- Only `storage` is requested as an extension API permission.

These facts do not make `http://*/*` narrow. They support the first-submission decision to justify the existing permission rather than rushing a functional redesign.

## Options Considered

### Option A: Keep `http://*/*` For First Chrome Web Store Readiness Pass

Keep the current host permission for the first readiness pass and prepare a precise reviewer justification.

Pros:

- Preserves the current Local Find LAN controller behavior.
- Supports arbitrary user-entered Android host/port combinations.
- Supports saved paired-device records without a new permission prompt design.
- Avoids risky functional changes before first submission readiness.
- Matches the current popup request implementation.

Cons:

- Broader than ideal.
- Includes non-LAN HTTP origins.
- May draw Chrome Web Store reviewer questions.
- Requires careful listing and privacy disclosure wording.

Implementation risk: low. This option is document-only for CWS.2 and does not change extension behavior.

Review risk: medium. The permission is broad, but the extension has a narrow API surface and a clear local-network controller justification.

### Option B: Try To Narrow Host Permissions Before First Submission

Attempt to replace `http://*/*` with a narrower host permission model before first submission.

Pros:

- Could better align manifest permissions with local-network intent.
- Could reduce user concern from broad permission warnings.
- Could reduce reviewer friction if a narrow model still supports the product.

Cons:

- Chrome match patterns cannot be safely assumed to express every user-entered LAN host/port case without testing.
- Narrowing too early may break manual host entry, paired-device records, or nonstandard local setups.
- Requires modifying `manifest.json` and likely popup behavior, which is outside CWS.2 scope.
- Needs cross-browser/Chrome behavior testing and a rollback plan.

Implementation risk: medium to high. This is a functional permission redesign, not a document-only decision.

Review risk: low to medium if implemented correctly; high if it breaks the core local-control workflow or creates confusing permission prompts.

### Option C: Prototype Optional Host Permissions Later

Prototype optional host permissions or another user-granted permission model in a later functional design phase.

Pros:

- Could move host access closer to explicit user consent.
- Could reduce install-time permission breadth.
- Could create a better long-term permission posture if the UX remains simple.

Cons:

- Requires new UX, permission request handling, and failure states.
- May not fit naturally with arbitrary LAN service addresses and popup-only control.
- `activeTab`-style models are not a direct fit because Local Find controls an Android service, not the active webpage.
- Adds implementation and testing burden.

Implementation risk: high for pre-submission readiness; acceptable only as a separate prototype after requirements are clear.

Review risk: potentially low after a mature implementation; uncertain during a rushed first-submission path.

## Decision

For the first Chrome Web Store readiness pass, keep `http://*/*` and prepare a precise reviewer justification.

CWS.2 decision:

- Do not change `host_permissions` in CWS.2.
- Do not modify `chrome-extension/manifest.json` in CWS.2.
- Do not modify `chrome-extension/popup.js` in CWS.2.
- Do not attempt optional host permissions until a separate functional design phase.
- Keep the listing and privacy disclosure aligned with the local-network controller model.

Reasoning:

- The current permission is broad but directly tied to user-entered or paired Android LAN service addresses.
- The extension has no content scripts, background worker, or sensitive browser-data permissions.
- The request model is popup-initiated and local-control focused.
- A permission redesign should be handled as a separate CWS phase with implementation and UX testing.

## Reviewer Justification Draft

```text
Local Find uses host access to send HTTP requests from the extension popup to the Local Find Android app running on a user-entered or paired local network address. The Android service address and port vary by device and network, so the extension needs to support user-entered local HTTP hosts.

The extension does not inject content scripts, does not run a background worker, does not read webpage content, and does not access browsing history, cookies, or tabs. It does not use tabs, history, cookies, webRequest, scripting, or nativeMessaging permissions. Requests are initiated from the popup for local device control.
```

## Revisit Triggers

Reconsider host-permission narrowing if any of the following happen:

- Chrome Web Store reviewer rejects or questions the broad host permission.
- Users complain about the permission warning.
- The pairing model changes.
- The Android app supports a more constrained discovery/control path.
- Optional permissions can be implemented without breaking the popup controller UX.
- A future Chrome extension API or Chrome Web Store policy change makes a narrower approach practical.

## Next Step

Recommended next phase:

- CWS.3: prepare package/listing readiness inputs, unless Chrome Web Store review strategy requires a pre-submission permission prototype first.
