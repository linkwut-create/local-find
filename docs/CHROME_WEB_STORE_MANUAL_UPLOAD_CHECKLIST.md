# Chrome Web Store Manual Upload Checklist

Checklist date: 2026-05-26

## Purpose

This is the final checklist before a manual Chrome Web Store upload.

CWS.19 does not upload to Chrome Web Store, does not submit the extension for review, and does not modify the zip package.

## Upload Package

Package path:

```text
dist/chrome-web-store/local-find-chrome-extension.zip
```

Package validation result: PASS FOR MANUAL UPLOAD CANDIDATE.

Manifest summary:

| Field | Value |
| --- | --- |
| `manifest_version` | `3` |
| `name` | `Local Find` |
| `version` | `0.1.0` |
| `permissions` | `storage` |
| `host_permissions` | `http://*/*` |

## Store Listing Fields

| Field | Value |
| --- | --- |
| Extension title | Local Find |
| Short description | Control your Local Find Android phone finder from Chrome on your local network. |
| Support email | `linkwut@gmail.com` |
| Privacy policy URL | `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md` |
| Category recommendation | Productivity first; Utilities as the documented alternative from the existing listing draft |
| Language | English first |

## Screenshots

Screenshot directory:

```text
store-assets/chrome-web-store/screenshots/en-US/
```

Screenshots:

- `01-popup-paired-device.png`
- `02-controller-actions.png`
- `03-pairing-or-manual-host.png`
- `04-language-switching.png`

All four screenshots are 1280x800 PNG files.

## Privacy / Permissions Notes

- `storage` permission is used for local extension settings.
- `host_permissions` `http://*/*` is used for user-entered or paired local Android HTTP service addresses.
- No `tabs`, `cookies`, `history`, `webRequest`, `scripting`, or `nativeMessaging` permissions are used.
- No content scripts are declared.
- No background service worker is declared.
- No remote script loading is used.
- No Local Find cloud upload is performed by the extension.

## Chrome Web Store Dashboard Manual Steps

1. Open Chrome Web Store Developer Dashboard.
2. Create a new item or upload a new package.
3. Upload:

```text
dist/chrome-web-store/local-find-chrome-extension.zip
```

4. Fill Store Listing fields.
5. Upload 4 screenshots.
6. Fill privacy fields.
7. Fill distribution fields.
8. Do not submit for review until owner confirms final review.
9. Stop before final Submit for review if any warning appears.

## Stop Conditions

- unexpected permission warning
- CWS rejects zip
- CWS asks for broader privacy disclosure
- CWS rejects privacy URL
- screenshots rejected by size/content
- dashboard asks for extra trader/developer verification
- any field differs from documented listing fields

## Decision

CWS.19 does not grant upload approval.

Upload requires explicit owner command.

Review submission requires separate explicit owner command.
