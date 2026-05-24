# Chrome Web Store Permission Strategy

Strategy date: 2026-05-25

Scope: planning document for the Chrome extension host-permission strategy. This document does not modify `chrome-extension/manifest.json` or change extension behavior.

## Current Manifest Permission

Current `chrome-extension/manifest.json` host permission:

```json
"host_permissions": [
  "http://*/*"
]
```

## Why It Exists

The Chrome extension needs to send HTTP requests to user-entered Local Find Android service addresses on the user's LAN.

The Local Find Chrome extension acts as a popup controller for the Local Find Android app. The Android service address can vary by user, device, network, and port, so the current manifest allows HTTP requests to arbitrary HTTP hosts.

## Risk

`http://*/*` is broader than ideal and includes non-LAN HTTP origins.

Chrome Web Store review may ask why a phone-finder controller needs broad HTTP host access. Even though the extension is intended for local network use, the manifest permission itself is not limited to private LAN ranges.

Primary risk:

- Permission overreach relative to the product's local-network purpose.

Secondary risk:

- The listing and privacy disclosure must clearly explain local-network use without implying that the extension inspects arbitrary websites.

## Current Mitigating Facts

CWS.0 found the following mitigating facts:

- No content scripts.
- No background worker.
- No `tabs` permission.
- No `history` permission.
- No `cookies` permission.
- No `webRequest` permission.
- No `scripting` permission.
- No `nativeMessaging` permission.
- Popup-initiated local control model.
- No remote script loading found.
- The user enters the host/port or uses paired local device data.

These facts reduce webpage-content, browsing-history, cookie, and background-activity concerns. They do not remove the need to justify `http://*/*`.

## Chrome Web Store Justification Draft

```text
Local Find uses host access to send HTTP requests from the extension popup to the Local Find Android app running on a user-entered or paired local network address. The address and port vary by device and network.

The extension does not inject content scripts, does not run a background worker, does not read webpage content, does not access browsing history or cookies, and does not use tabs, webRequest, scripting, or nativeMessaging permissions. Requests are initiated from the popup for local device control.
```

## Options

### Option A: Keep `http://*/*` With Precise CWS Justification

Keep the current behavior and prepare a precise Chrome Web Store justification.

Pros:

- Preserves current LAN controller behavior.
- Supports user-entered Android service addresses without additional permission prompts or redesign.
- Lowest implementation risk before a first Chrome Web Store readiness pass.

Cons:

- Still broad.
- May require reviewer explanation.
- The privacy disclosure and listing must be careful and specific.

### Option B: Explore Narrowing Host Permissions Later

Investigate whether Chrome extension constraints allow a narrower permission model while still supporting arbitrary LAN hosts and ports.

Possible directions:

- Limit documented supported hosts to private network addresses if Chrome match patterns and product UX can support that cleanly.
- Review whether a fixed Android service path or constrained setup flow can reduce host access.
- Keep current behavior until the narrower design is proven.

Pros:

- Could reduce review friction and user concern.
- Better aligns manifest permissions with local-network intent.

Cons:

- May not be practical for arbitrary user-entered LAN addresses.
- Could break existing pairing/control workflows if rushed.
- Requires functional code and manifest changes, so it belongs in a later CWS phase.

### Option C: Consider Optional Host Permissions Or ActiveTab-Style Redesign Only If Practical

Consider a deeper redesign using optional permissions or another user-activated model only if it can preserve the popup controller workflow.

Pros:

- Could make the extension's permission surface more user-driven.
- May improve review posture if implemented cleanly.

Cons:

- `activeTab` is likely not a natural fit for local Android service requests because the target is not the active webpage.
- Optional host permissions may add UX complexity and still need careful handling for arbitrary LAN hosts.
- This is a functional redesign, not a CWS.1 planning change.

## Recommendation

For CWS.1, do not change functional behavior.

Prepare the Chrome Web Store justification first, keep the listing and privacy disclosure aligned with the local-network controller model, and defer any manifest change to a separate CWS.2 decision.

Recommended next phase:

- CWS.2: decide whether to keep `http://*/*` with store justification or prototype a narrower host-permission model.

Do not modify `chrome-extension/manifest.json` until that decision is explicitly approved.
