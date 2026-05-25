# Chrome Web Store Screenshot Asset Preflight

## Purpose

This document records the CWS.13 read-only preflight for Chrome Web Store screenshot draft assets.

CWS.13 checks draft screenshot locations, expected filenames, image format, dimensions, file size, visible privacy risk signs, and whether the assets need a polish/canvas pass before final Chrome Web Store upload.

CWS.13 does not commit screenshots.

CWS.13 does not move files into `store-assets`.

CWS.13 only decides whether CWS.14 should:

- A. commit existing screenshots as-is, or
- B. create polished 1280x800 store-ready screenshot canvases first.

## Checked Locations

| Location | Exists | File count | Result |
| --- | --- | ---: | --- |
| `D:\local-find-screenshots-draft\` | yes | 0 | External draft directory exists but is empty at this preflight. |
| `D:\local-find\screenshots-draft\` | yes | 4 | Repository-local untracked draft directory contains the four expected PNG files. |

## Expected Screenshot File Presence

| Expected file | `D:\local-find-screenshots-draft\` | `D:\local-find\screenshots-draft\` | Present for preflight |
| --- | --- | --- | --- |
| `01-popup-paired-device.png` | missing | present | yes |
| `02-controller-actions.png` | missing | present | yes |
| `03-pairing-or-manual-host.png` | missing | present | yes |
| `04-language-switching.png` | missing | present | yes |

## Asset Metadata

Source directory used for metadata:

```text
D:\local-find\screenshots-draft\
```

| File | Format | Dimensions | File size | Filename matches expected target |
| --- | --- | ---: | ---: | --- |
| `01-popup-paired-device.png` | PNG | 540x921 | 44,960 bytes | yes |
| `02-controller-actions.png` | PNG | 540x922 | 47,191 bytes | yes |
| `03-pairing-or-manual-host.png` | PNG | 540x921 | 49,936 bytes | yes |
| `04-language-switching.png` | PNG | 540x921 | 47,556 bytes | yes |

## Extra Screenshot Files

No extra files were found in either checked screenshot directory.

## Privacy Risk Preflight

Visual read-only review of the four available draft screenshots found:

| Risk item | Result |
| --- | --- |
| real IP | no real IP observed; `HOST:8888` and `192.168.1.108` are demo/safe placeholders |
| real device name | no real device name observed; visible state uses `Manual` / `N/A` |
| token | no token value observed |
| controller id | no controller id observed |
| private browser content | no clear private browser content observed, but thin non-popup background/page slivers are visible at some image edges |

Privacy preflight result: acceptable for draft review, but edge slivers should be removed or covered during final asset polish.

## CWS Upload Dimension Strategy

Preferred target:

```text
1280x800 PNG
```

Current source screenshots are smaller portrait popup captures:

- 540x921
- 540x922

They do not match the preferred 1280x800 Chrome Web Store upload strategy.

Because the sources are narrow popup captures and include minor edge/background slivers, they should not be committed as final store screenshots as-is.

## CWS.13 Decision

CWS.14 should choose:

```text
B. create polished 1280x800 store-ready screenshot canvases first
```

The current screenshots remain useful source candidates, but final upload assets should be produced through a polish/letterbox/canvas pass before any screenshot commit.

## Boundary

CWS.13 does not:

- commit screenshots;
- move screenshots into `store-assets`;
- add PNG/JPEG/SVG screenshots to the repository;
- generate an extension zip;
- upload to Chrome Web Store;
- submit for review;
- modify Chrome extension code;
- modify Android code;
- move tags or modify the frozen `mvp-u5-ok` release.

## CWS.14 Polished Canvas Generation Note

CWS.14 generated polished Chrome Web Store screenshot candidates from the draft files in:

```text
D:\local-find\screenshots-draft\
```

The source draft screenshots remain in `screenshots-draft/`.

Final screenshot candidates now exist under:

```text
store-assets/chrome-web-store/screenshots/en-US/
```

Generated files:

| File | Format | Dimensions | Result |
| --- | --- | ---: | --- |
| `01-popup-paired-device.png` | PNG | 1280x800 | generated |
| `02-controller-actions.png` | PNG | 1280x800 | generated |
| `03-pairing-or-manual-host.png` | PNG | 1280x800 | generated |
| `04-language-switching.png` | PNG | 1280x800 | generated |

Generation strategy:

- clean neutral 1280x800 canvas;
- conservative English title text;
- original popup screenshot scaled proportionally;
- no stretching or distortion;
- source edge/background slivers cropped before placement;
- no Chrome logo, Google Play badge, Android robot, or exaggerated marketing copy.

CWS.14 does not upload to Chrome Web Store, generate an extension zip, submit for review, modify Chrome extension code, modify Android code, move tags, or modify the frozen `mvp-u5-ok` release.
