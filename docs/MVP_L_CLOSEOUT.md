# MVP-L Series Closeout

Status: complete. All L.0–L.4-C tags committed.

## L Series Goal

Add formal pairing and multi-device management across Chrome extension and Android HTTP service. Every paired controller gets a unique control token; deletion on Chrome revokes the Android-side token.

## Completed Items

### L.0 — Plan
- `docs/MVP_L_PAIRING_PLAN.md` — pairing protocol design, endpoint plan, token model.

### L.1 — Android pairing mode and endpoints
- `POST /pairing/request` — create pairing request during pairing mode window (5 min).
- `GET /pairing/status?requestId=` — poll pairing status, return `controlToken` on acceptance.
- `GET /device-info` — public device identity with `pairingMode` flag.
- `PairingRequestStore` — in-memory request lifecycle (pending → accepted/rejected/expired).
- `PairedControllerTokenStore` — issue, validate, and list paired controller tokens.

### L.2 — Chrome pairing flow for one phone
- `GET /device-info` check-phone button.
- `POST /pairing/request` with persistent `controllerId`.
- Poll `/pairing/status` until acceptance, then save device to `devices[]`.
- `controllerId` generated once per extension install (UUID).

### L.3 — Paired phone list and local deletion
- Multi-device `devices[]` storage with `selectedDeviceId`.
- Paired device list UI with select/delete actions.
- Switching current target updates command routing.
- Local-only delete (pre-L.4).

### L.4-A — Android controller listing
- `GET /pairing/controllers` — authenticated list of all paired controllers.
- Uses existing `authenticate()` — no auth changes.

### L.4-B — Android paired controller revoke
- `POST /pairing/revoke` — revoke paired controller token by `controllerId`.
- `PairedControllerTokenStore.revokeByControllerId()`.
- Verified: revoked paired token returns 401 on command endpoints.

### L.4-C — Chrome delete calls revoke
- Delete button calls `POST /pairing/revoke` before removing local device.
- `controllerId` saved in device record during pairing.
- If revoke fails (offline, 401, etc.), user chooses local-only delete.
- Pre-L.4 records without `controllerId` skip revoke and offer local-only delete.

## Tags

| Tag | Commit | Description |
|-----|--------|-------------|
| `mvp-l3-ok` | `9bc6477` | L.3 baseline (paired phone list, local delete, manual fallback) |
| `mvp-l4a-ok` | `a3666b0` | L.4-A Android GET /pairing/controllers |
| `mvp-l4b-ok` | `5e4892c` | L.4-B Android POST /pairing/revoke |
| `mvp-l4c-ok` | `39dcd4a` | L.4-C Chrome delete calls revoke |

## Current User Flow

### First-time phone pairing

1. Phone: open Local Find, start service.
2. Phone: enable "computer plugin pairing mode" (5-minute window).
3. Chrome extension: enter phone IP address and port in the "add phone" section.
4. Click "check phone" — confirms device identity and pairing mode.
5. Click "request pairing" — POSTs `/pairing/request` with controller identity.
6. Phone: accept the incoming pairing request.
7. Chrome extension receives `controlToken` via polling `/pairing/status`.
8. Device saved to `devices[]` with `controllerId`, host, port, and controlToken.
9. Device becomes the selected target (`selectedDeviceId`).
10. Subsequent commands (find phone, flash, stop all) use the current target's token.

### Deleting a paired phone

1. Chrome extension: click "delete" on a paired device.
2. Confirm dialog explains revoke will be attempted.
3. Local PIN/WebAuthn verification (if enabled).
4. Extension calls `POST /pairing/revoke` with the device's `controllerId`.
5. Android removes the paired controller token from its store.
6. Extension removes the device from local `devices[]`.
7. If the deleted device was the current target, fallback to next device or manual mode.
8. If phone is offline or revoke fails, user chooses whether to delete only the local record.

### Manual mode fallback

When no paired device is selected, or the selected device lacks host/port/token, the extension falls back to the manual host/port/token fields. The legacy 8-character global token works for command authorization in this mode.

## Security Model

### Tokens

- **8-character global token**: generated on Android (`PairingTokenManager`), displayed on phone UI. Acts as admin/fallback token for all command endpoints. Used when no paired device is selected.
- **Paired controlToken**: 43-character Base64URL random token issued per controller on pairing acceptance. Stored in `PairedControllerTokenStore` on Android, in `chrome.storage.local.devices[].token` on Chrome.

### Authentication

- Command endpoints (`/command/*`) accept either the global token or a valid paired controlToken via `X-LocalFind-Token` header or `?token=` query parameter.
- `/pairing/controllers` and `/pairing/revoke` use the same `authenticate()` function.
- Public endpoints (`/ping`, `/device-info`) require no authentication.
- `/pairing/request` requires active pairing mode but no token.
- `/pairing/status` is public (accessed by `requestId`).

### Revocation

- Deleting a paired device on Chrome calls `POST /pairing/revoke`.
- Android removes the controller's token from storage.
- The revoked controlToken immediately returns 401 on all authenticated endpoints.
- The global 8-character token is never revoked by paired-device deletion.

### Local protection

- Chrome extension supports local PIN and/or WebAuthn for sensitive actions (delete device, save token, change settings).
- PIN is stored as PBKDF2 hash with salt; WebAuthn uses platform authenticator.
- "Stop all" is always available without verification.

### What we do NOT do

- No cloud accounts or cloud services.
- No location tracking or anti-theft.
- No SMS or iOS.
- No background scanning or automatic discovery.
- Chrome token storage is `chrome.storage.local` (not a hardware key store).

## Known Limitations

- **Manual IP entry**: users must type the phone's LAN IP address at least once.
- **No QR code pairing**: no camera-based setup.
- **No automatic LAN scanning**: no NSD/mDNS discovery from Chrome.
- **No Android controller management UI**: paired controllers are only visible via `/pairing/controllers` API, not in the Android app UI.
- **No "revoke all"**: revocation is one controller at a time.
- **No re-pair button**: if a paired token is lost, the user must delete and re-pair.
- **No release/signed package**: debug APK only.
- **Chrome storage is not a secure key store**: `chrome.storage.local` is plaintext on disk.
- **8-character global token is high-privilege fallback**: it can impersonate or revoke any paired controller.
- **Pairing mode is time-limited (5 minutes)**: no permanent pairing mode.
- **Single extension instance per controllerId**: reinstalling the extension generates a new `controllerId`, leaving orphaned entries on Android.

## Future Backlog

| Series | Topic |
|--------|-------|
| M | QR code / short-code pairing feasibility |
| N | Automatic discovery or LAN-assisted discovery (NSD/mDNS from Chrome) |
| O | Android paired controller management UI (list, revoke from phone) |
| P | Release packaging, signing, and install documentation |
| Q | Security hardening: token hashing, expiry, revoke-all, rate limiting |
| R | Re-pair flow, controllerId recovery, orphan cleanup |
| I.0 | Android internal pairing model refactor (currently in stash) |
