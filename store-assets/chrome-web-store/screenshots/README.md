# Chrome Web Store Screenshots

This directory holds the planned Chrome Web Store screenshot sets for the Local Find Chrome extension.

CWS.4 only creates directory placeholders and documentation. Final screenshot PNG files are not committed in CWS.4 unless explicitly approved later.

## Expected Screenshot Sets

English first:

```text
en-US/01-popup-paired-device.png
en-US/02-controller-actions.png
en-US/03-pairing-or-manual-host.png
en-US/04-language-switching.png
```

Optional Simplified Chinese later:

```text
zh-CN/01-popup-paired-device.png
zh-CN/02-controller-actions.png
zh-CN/03-pairing-or-manual-host.png
zh-CN/04-language-switching.png
```

## Language Strategy

- Use `en-US` as the first Chrome Web Store screenshot set.
- Prepare `zh-CN` only when localized listing assets are approved.
- Do not mix English and Chinese within the same screenshot set.
- Do not add marketing overlays unless they are localized consistently.

## Hygiene Checklist Summary

Final screenshots must avoid:

- real tokens;
- real controller ids;
- real personal device owner names;
- private phone numbers;
- real Wi-Fi SSIDs;
- real public IPs;
- browser tabs showing private content;
- unrelated extensions;
- notification popups;
- exaggerated privacy or security claims;
- claims that the Chrome extension performs cloud tracking, SMS recovery, or background location tracking.

Prefer demo LAN-looking values when host, port, device, or pairing details need to be visible.

## CWS.4 Status

Placeholder files keep the `en-US` and `zh-CN` directories present in Git. Replace them with final screenshot files only in a later explicitly approved screenshot capture phase.
