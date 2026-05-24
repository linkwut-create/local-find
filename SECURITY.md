# Security

Local Find is intended for trusted local networks only. It is not designed to be exposed to the public internet.

## Security boundary

- The Android app runs a local HTTP service for LAN use.
- Controllers should be on the same trusted local network as the Android device.
- Do not port-forward the Local Find service.
- Do not expose the Local Find service through public tunnels or reverse proxies.
- Pair only devices and browsers you trust.

## Tokens and pairing

Local Find uses local pairing and token-based control:

- The Android app displays an 8-character fallback/admin token for local control.
- Pairing mode allows a trusted controller to request access during a limited window.
- Accepted controllers receive a per-controller control token.
- Deleting a paired controller can call revoke so the Android device rejects that controller token.
- Stop-all remains available as an emergency local control action.

Tokens are local secrets. Anyone with a valid token and LAN access to the phone may be able to control supported find actions. Reset or revoke tokens if you believe a token or controller has been exposed.

## Non-goals

Local Find does not claim to provide:

- Anti-theft protection
- Remote internet-based phone recovery
- Cloud account recovery
- Background location tracking
- SMS-based control
- Protection against attackers already trusted on your local network

## Reporting security issues

Please report security issues privately when possible. If GitHub private vulnerability reporting is enabled for this repository, use that channel. Otherwise, open a GitHub issue with a minimal description and avoid posting working exploit details, real tokens, IP addresses, or other sensitive local data.

For non-sensitive hardening suggestions, public GitHub issues are appropriate.
