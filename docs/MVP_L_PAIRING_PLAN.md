# Local Find MVP-L Pairing Plan

Status: design audit only. Do not implement from this document without a new implementation task.

## Scope

MVP-L starts formal pairing and multi-device management for the Chrome extension and Android HTTP service.

Non-goals remain: cloud service, account system, location tracking, background scanning, anti-theft suite, Native Messaging, local PC service, SMS, iOS, QR-code work in this slice.

## Current Android Audit

### Current HTTP service

Android runs an embedded Ktor/Netty server on `0.0.0.0:8888`.

Current unauthenticated endpoints:

- `GET /ping`
  - Returns `{ "ok": true }`.
- `GET /`
  - Browser diagnostic/control page.
  - Displays `Build.MODEL` as the current device name.
- `GET /status`
  - Returns a small in-memory status object:
    - `service: "running"`
    - `ring_active: Boolean`
    - `flash_mode: String`

Current command endpoints:

- `POST /command/ring/start`
- `POST /command/ring/stop`
- `POST /command/flash/steady/start`
- `POST /command/flash/strobe/start`
- `POST /command/flash/stop`
- `POST /command/stop-all`

All current command endpoints use the same auth helper. A request is accepted when either:

- Header `X-LocalFind-Token` matches the current token.
- Query parameter `token` matches the current token.

Recommendation for L: keep `X-LocalFind-Token` as the control auth header and deprecate query-token auth for new clients. Do not put tokens in URLs.

### Current token behavior

`PairingTokenManager` stores a single global token in Android `SharedPreferences` file `pairing_prefs`, key `pairing_token`.

Behavior:

- On init, if no token exists, it generates one.
- Token is generated from a UUID, stripped of dashes, truncated to 8 chars, uppercased.
- UI can display/copy/regenerate the token.

This token currently acts as the control secret, not a formal per-device pairing credential.

### Current device identity and discovery

Current stable identity is incomplete:

- Device name is derived from `android.os.Build.MODEL` in the diagnostic page and UI.
- NSD advertiser uses service name `LocalFind-${Build.MODEL}` and service type `_localfind._tcp.`.
- No stable public `device_id` is present on master.

Current Android code already contains useful discovery-related components:

- `NsdAdvertiser`
- `NsdDiscoveryManager`
- `DiscoveredDevice`
- `RemoteDeviceTokenStore`

These are mainly Android-to-Android/controller features. A Chrome extension cannot directly consume Android NSD/mDNS without adding a PC-side helper or broader browser capabilities, which are out of scope for L.0/L.1.

### Pairing residue on master

Master already has pairing-adjacent pieces:

- `PairingTokenManager`
- pairing token UI
- QR utility and QR scanner UI
- NSD advertiser/discovery classes
- `RemoteDeviceTokenStore`

These are useful references, but they do not yet provide formal Chrome-to-Android pairing or multi-device storage for the extension.

## Chrome Extension Audit

### Current storage

Current Chrome extension storage is scalar, not multi-device:

- `host`
- `port`
- `rememberToken`
- `savedToken`
- `lastSuccessAt`
- `protectionEnabled`
- `localPinSalt`
- `localPinHash`
- `protectionMethod`
- `webauthnEnabled`
- `webauthnCredentialId`

Current behavior:

- `host` and `port` are saved automatically.
- `savedToken` is only saved when the user opts into remembering the token.
- `lastSuccessAt` is one global timestamp for the current target.
- Local PIN stores only PBKDF2 salt/hash, not plaintext PIN.
- WebAuthn stores only `webauthnEnabled`, `webauthnCredentialId`, and selected `protectionMethod`.

### Multi-device migration risk

Migration is straightforward but requires one explicit storage migration:

- Existing scalar `host/port/savedToken/lastSuccessAt` can be migrated into a single `devices[0]` entry.
- Existing protection settings should remain global extension settings.
- New command code should read the selected device from `devices[]` instead of scalar fields.

Risk areas:

- Existing UI assumes one current endpoint preview.
- `tokenInput` is a single input, while paired devices should have stored per-device control tokens.
- Current `lastSuccessAt` is global; it should become per-device.

## Android I.0 Stash Audit

Commands run read-only:

- `git stash list`
- `git stash show --stat "stash@{0}"`
- `git stash show -p "stash@{0}"`

Stash found:

- `stash@{0}: On master: wip android i0 pairing model before pc endpoint`

Stash stat summary:

- Adds model classes:
  - `LocalDeviceIdentity`
  - `PairedDevice`
  - `PairingRequest`
- Adds stores:
  - `LocalDeviceIdentityStore`
  - `PairedDeviceStore`
  - `PairingRequestStore`
- Adds pairing changes in:
  - `HttpServerManager`
  - `RemoteControlClient`
  - `FindPhoneForegroundService`
  - `MainActivity`
  - `MainScreen`

Useful design ideas from stash:

- Stable local device identity with `id`, `name`, `type`, `createdAt`.
- Paired device model with `id`, `name`, `type`, `host`, `port`, `pairedAt`, `lastSeen`.
- Pairing request model with `requestId`, requester/target metadata, status, nonce, expiry.
- Android-side persistent stores for identity, paired devices, and pairing requests.
- Pairing endpoints resembling:
  - `POST /pair/request`
  - `POST /pair/complete`
  - `GET /pair/status`

Do not restore stash directly:

- It is Android-to-Android oriented and large.
- It mixes model/storage/server/UI changes.
- Endpoint names and handshake semantics should be tightened for Chrome pairing.
- It does not cleanly solve Chrome extension discovery constraints.

## MVP-L Pairing Model

### Principle

Pairing should be user-confirmed on the Android phone and should produce a per-controller control token for the Chrome extension. The existing command endpoints should continue to use `X-LocalFind-Token`, but Android should validate against paired-device tokens instead of one long-lived global manual token.

### Device identity

Android should create and persist a stable identity:

```json
{
  "id": "uuid",
  "name": "Pixel 8",
  "type": "android_phone",
  "createdAt": 1710000000000
}
```

Chrome extension should create or persist a controller identity:

```json
{
  "id": "uuid",
  "name": "Chrome on Windows",
  "type": "chrome_extension"
}
```

### Pairing mode on Android

Android should expose a visible pairing mode:

- User opens Local Find on the phone.
- User taps "Allow pairing" or "Pair new computer".
- Pairing mode becomes active for a short TTL, for example 5 minutes.
- Android UI displays pairing status and incoming requests.
- Android can disable pairing mode manually.

Pairing mode should not start background LAN scanning.

### How the plugin finds the phone

Given the current constraints, Chrome extension MVP-L should use "manual seed discovery":

1. User enters host/port once, or starts from the existing saved host/port.
2. Plugin calls `GET /device-info`.
3. If the phone is reachable and in pairing mode, plugin can show the device card and start pairing.

This does not satisfy fully automatic LAN discovery, but it avoids Native Messaging, PC services, cloud, accounts, background scan, and new broad permissions. Automatic mDNS/NSD discovery from a Chrome extension should remain out of scope unless a later architecture explicitly allows a local helper.

### Pairing request flow

Suggested minimal flow:

1. Android enters pairing mode.
2. Chrome calls `GET /device-info` on `http://HOST:PORT`.
3. Chrome calls `POST /pairing/request` with controller metadata:

```json
{
  "controllerId": "uuid",
  "controllerName": "Chrome on Windows",
  "controllerType": "chrome_extension",
  "nonce": "random"
}
```

4. Android creates a pending request and shows it in local UI.
5. User confirms or rejects on Android.
6. Chrome polls `GET /pairing/status?requestId=...`.
7. On acceptance, Android returns pairing result exactly once:

```json
{
  "status": "accepted",
  "device": {
    "id": "android-device-id",
    "name": "Pixel 8",
    "type": "android_phone",
    "host": "192.168.1.108",
    "port": 8888
  },
  "controlToken": "random-long-token"
}
```

8. Chrome stores the device in `devices[]` and sets `selectedDeviceId`.

### Phone confirmation

For MVP-L, phone-side local confirmation is simpler and safer than code entry:

- Incoming request appears on Android with controller name/type.
- User taps Accept or Reject.
- Acceptance issues a token scoped to that controller.

Do not use QR code in MVP-L.0-L.3.

### Token policy

`X-LocalFind-Token` should remain the control auth mechanism for existing command endpoints.

Recommended change:

- Move from one global 8-char token to per-controller random control tokens.
- Store only token hashes on Android if feasible.
- Store plaintext control token in Chrome because the extension must send it, but protect access through the existing local PIN/WebAuthn controls.
- Deleting a paired device on Android should revoke that controller token.
- Deleting a device in Chrome should remove local token/device data only; it should not assume Android revocation unless an authenticated delete endpoint exists.

## Recommended Minimal Endpoint Design

No endpoint should be implemented in L.0. Proposed endpoint names:

### `GET /device-info`

Unauthenticated, read-only.

Returns:

```json
{
  "id": "android-device-id",
  "name": "Pixel 8",
  "type": "android_phone",
  "port": 8888,
  "pairingMode": true,
  "service": "running"
}
```

Keep returned data minimal. Do not return control token.

### `GET /pairing/status`

Two modes:

- Without `requestId`: returns whether pairing mode is active.
- With `requestId`: returns request status for polling.

Example:

```json
{
  "pairingMode": true,
  "requestId": "abc123",
  "status": "pending"
}
```

### `POST /pairing/request`

Only works while pairing mode is active.

Request:

```json
{
  "controllerId": "uuid",
  "controllerName": "Chrome on Windows",
  "controllerType": "chrome_extension",
  "nonce": "random"
}
```

Response:

```json
{
  "ok": true,
  "requestId": "abc123",
  "status": "pending"
}
```

### `POST /pairing/confirm`

Preferred confirmation is phone-local UI. A separate HTTP confirm endpoint is optional and should not be callable by the untrusted controller unless it is tied to phone-local state.

Recommended for MVP:

- Android UI accepts/rejects pending request locally.
- Chrome observes via `GET /pairing/status?requestId=...`.

### Existing command endpoints

Continue using:

- `POST /command/ring/start`
- `POST /command/ring/stop`
- `POST /command/flash/strobe/start`
- `POST /command/flash/stop`
- `POST /command/stop-all`

Auth remains:

- `X-LocalFind-Token: <paired device control token>`

New Chrome code should never put token in URL.

## Recommended Chrome Storage

Global extension settings:

```json
{
  "selectedDeviceId": "android-device-id",
  "protectionEnabled": true,
  "localPinSalt": "...",
  "localPinHash": "...",
  "protectionMethod": "pin",
  "webauthnEnabled": true,
  "webauthnCredentialId": "..."
}
```

Devices:

```json
{
  "devices": [
    {
      "id": "android-device-id",
      "name": "Pixel 8",
      "type": "android_phone",
      "host": "192.168.1.108",
      "port": 8888,
      "token": "paired-control-token",
      "pairedAt": "2026-05-24T10:30:00.000Z",
      "lastSuccessAt": "2026-05-24T10:35:00.000Z"
    }
  ],
  "selectedDeviceId": "android-device-id"
}
```

Migration from current K storage:

1. If `devices` is absent and `host` exists, create one legacy device.
2. Use `savedToken` only if `rememberToken=true`.
3. Move scalar `lastSuccessAt` into the legacy device.
4. Keep old scalar keys for one release or remove after migration is verified.

## L Slices

### L.1 Android pairing mode and minimal pairing endpoints

Deliver:

- Stable Android device identity store.
- Pairing mode state with TTL.
- `GET /device-info`.
- `GET /pairing/status`.
- `POST /pairing/request`.
- Android UI for pending request accept/reject.
- Per-controller token issuance.
- Existing command endpoints accept paired control tokens through `X-LocalFind-Token`.

Do not implement Chrome multi-device UI in L.1.

### L.2 Chrome add one phone

Deliver:

- "Add phone" flow using manual host/port seed.
- Calls `GET /device-info`.
- Starts `POST /pairing/request`.
- Polls pairing status.
- Saves one paired device to `devices[]`.
- Sends one-key find to selected paired device.

Do not implement full multi-device switching yet.

### L.3 Chrome multi-device list and switching

Deliver:

- Device list UI.
- `selectedDeviceId`.
- Current device card reads from selected device.
- One-key find targets selected device.
- Per-device `lastSuccessAt`.

### L.4 Delete, re-pair, and documentation closeout

Deliver:

- Delete local paired device in Chrome.
- Android-side revoke/delete paired controller.
- Re-pair flow for changed IP/token.
- README update.
- Migration notes from scalar `host/port/token` storage.

## Final Recommendation

Do not restore Android I.0 stash directly. Reuse its identity/request/store ideas, but implement L.1 as a smaller Android-only slice with stable identity, explicit pairing mode, phone-local confirmation, and per-controller tokens. Then migrate Chrome in L.2/L.3 from scalar connection settings into `devices[]`.
