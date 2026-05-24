# Google Play Asset Production Notes

Status: source notes only. This phase does not produce final upload-ready image files.

## Scope

- Do not generate final PNG or JPEG upload assets in this phase.
- Do not generate a new AAB.
- Do not upload to Google Play.
- Do not modify Android or Chrome extension feature code.
- Do not modify the frozen GitHub Release or the `mvp-u5-ok` tag.

## Production timing

Final image files should be generated or captured only after Play Console account verification resumes.

Before final upload, verify:

- Image dimensions.
- PNG/JPEG format.
- Alpha channel rules.
- File size.
- Screenshot language consistency.
- No private IP addresses, SSIDs, tokens, phone numbers, or notifications.
- No Play Store badge, ranking claim, price, or install/download call to action.

Final upload should wait until:

- Support email is ready.
- Privacy policy public URL is ready.
- Play Console account verification is complete.
- Store listing text, Data Safety, and Foreground Service declaration have been reviewed together.

## Frozen release boundary

The MVP-U.5 GitHub Release and `mvp-u5-ok` tag remain frozen. Google Play assets and future Play release AABs should not modify or replace the published GitHub MVP-U.5 release package.
