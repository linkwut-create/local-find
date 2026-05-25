# Chrome Web Store Contact And Privacy Readiness

Plan date: 2026-05-25

Scope: CWS.5 document-only readiness plan for Chrome Web Store support contact and public privacy policy URL fields. This plan does not publish a webpage, modify `PRIVACY.md`, modify the GitHub Release, upload to Chrome Web Store, generate an extension zip, or change Android/Chrome extension behavior.

## Purpose

Prepare the owner decisions needed before a Chrome Web Store submission can start:

- support email;
- public privacy policy URL.

These fields are listing and disclosure prerequisites. They should be resolved before packaging/upload begins.

## Current Blockers

| Field | Current value | Status |
| --- | --- | --- |
| Chrome Web Store developer account | registered | Developer Dashboard access available; upload not started. |
| Support email | TODO | Manual owner decision required. |
| Public privacy policy URL | TODO | Manual owner decision required. |

Chrome Web Store upload should not start until support email and public privacy policy URL are settled and final upload is explicitly approved.

## Support Email Options

### Option A: Use An Existing Personal/Developer Email

Use an existing owner/developer email address as the public support contact.

Pros:

- Fastest.
- No domain setup.
- Can unblock first readiness pass quickly.

Cons:

- Less professional.
- Personal inbox exposure.
- Support messages may mix with unrelated mail.

Implementation risk: low.

Listing hygiene: acceptable for a first pass if the owner accepts public exposure.

### Option B: Create A Dedicated Support Email

Create a dedicated email address for Local Find support.

Pros:

- Cleaner support workflow.
- Better public listing hygiene.
- Easier to route, filter, or hand off later.

Cons:

- Requires setup.
- May require domain, mailbox, forwarding, or account management.

Implementation risk: low to medium, depending on provider/domain setup.

Listing hygiene: preferred.

### Option C: Use GitHub Issues As Support Channel Plus Email

Use GitHub Issues for public issue tracking, while still providing a support email where required.

Pros:

- Public issue tracking.
- Open-source friendly.
- Keeps reproducible bugs visible.

Cons:

- Chrome Web Store may still require an email/contact field.
- Not all users want to file public issues.
- Security/privacy reports should not require public issue disclosure.

Implementation risk: low.

Listing hygiene: useful as an additional channel, not a complete replacement for email.

## Support Email Recommendation

Prefer a dedicated support email if available.

If a dedicated address is not ready, use the developer email temporarily only after the owner explicitly approves making it public. Keep the final support email as a manual owner decision.

## Public Privacy Policy URL Options

### Option A: Use GitHub Repository `PRIVACY.md` Public URL

Example target:

```text
https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md
```

Pros:

- Already public if the repository is public.
- No website needed.
- Version controlled.
- Easy to update in the same repository.

Cons:

- GitHub UI URL is acceptable for humans but less polished than a dedicated site.
- Branch-based URL may change if the default branch changes.
- It should not be used until `PRIVACY.md` coverage is confirmed adequate for the Chrome extension.

Implementation risk: low.

Listing hygiene: acceptable for a first readiness pass if coverage is adequate.

### Option B: Use GitHub Pages Privacy Page Later

Publish a dedicated privacy page through GitHub Pages.

Pros:

- Cleaner public URL.
- Better for store listing.
- Can present Android and Chrome extension privacy language more clearly.

Cons:

- Requires Pages setup.
- Requires site publishing and maintenance.

Implementation risk: medium.

Listing hygiene: better long-term option.

### Option C: Use Project Website Later

Publish the privacy policy on a dedicated project website.

Pros:

- Most polished.
- Better future product/support presentation.
- Can support multiple policies, localized pages, and support links.

Cons:

- Extra hosting/work.
- More maintenance.
- Not necessary for first readiness if GitHub `PRIVACY.md` is adequate.

Implementation risk: medium to high.

Listing hygiene: best long-term option.

## Public Privacy Policy URL Recommendation

For the first readiness pass, the GitHub `PRIVACY.md` public URL is acceptable if the content covers both Android app and Chrome extension behavior:

```text
https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md
```

If coverage is not adequate at final review time, update `PRIVACY.md` in a later phase before using it as the public URL.

The final public privacy policy URL remains a manual owner decision.

## `PRIVACY.md` Coverage Check

Read-only check performed in CWS.5. `PRIVACY.md` currently covers:

| Required coverage | Current status | Evidence |
| --- | --- | --- |
| Local-first model | Covered | Describes Local Find as local-first and same-LAN. |
| No cloud account | Covered | States no cloud account is required or used. |
| No Local Find cloud upload | Covered | States app/device/token/pairing/usage data is not uploaded to a cloud service. |
| No SMS | Covered | States Local Find does not use SMS. |
| No background location tracking | Covered | States Local Find does not use background location tracking. |
| Chrome extension local storage | Covered | States local data is stored by Android app and/or Chrome extension. |
| LAN host/port | Covered | Lists LAN IP address and port. |
| Paired device metadata | Covered | Lists device name and saved paired devices. |
| Control tokens / optional legacy token | Mostly covered | Lists pairing tokens and control tokens; optional legacy token behavior is better detailed in CWS privacy disclosure docs. |
| Browser local protection settings | Covered | Lists local browser protection settings where supported. |
| User deletion / revocation path | Covered | Describes removing saved devices and revoking paired controller token when supported/reachable. |

First-pass judgment:

- `PRIVACY.md` appears adequate as the candidate public policy source for Chrome Web Store readiness.
- A later polish pass may still add a Chrome extension-specific subsection before final submission.
- Do not submit the listing until the owner chooses the final URL.

## Recommended Minimal Path

1. Owner chooses the public support email.
2. Owner chooses whether to use the GitHub `PRIVACY.md` URL for first submission readiness.
3. If using GitHub `PRIVACY.md`, confirm the repository is public and the URL is reachable from a signed-out browser.
4. If privacy wording needs more explicit Chrome extension detail, update `PRIVACY.md` in a separate approved phase.
5. Fill the final support email and privacy policy URL into Chrome Web Store listing materials before package/upload.

Recommended first-pass candidate:

| Field | Candidate |
| --- | --- |
| Support email | TODO: owner decision |
| Public privacy policy URL | `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md`, pending owner decision and final reachability check |

## Pre-Submission Checklist

- Support email selected by owner.
- Public privacy policy URL selected by owner.
- Public privacy policy URL reachable without repository write access.
- Privacy policy covers Android app and Chrome extension behavior.
- Listing draft and privacy disclosure draft use the same final URL.
- Chrome Web Store upload does not start while either field remains TODO.
- No GitHub Release changes are required for this step.
- No extension package is generated as part of contact/privacy readiness.

## Fields Still Requiring Manual Owner Decision

- Final support email.
- Final public privacy policy URL.
- Whether to use GitHub Issues as an additional support channel.
- Whether to publish a GitHub Pages or project website privacy page later.

## CWS.12 Developer Account Status Note

CWS.12 records that the Chrome Web Store developer account is registered and Developer Dashboard access is available.

Upload remains blocked by final screenshots not committed, support email TODO, public privacy policy URL TODO / reachability check, final extension zip not generated, final package validation not completed, and final upload approval not granted.

CWS.12 does not upload, package, submit for review, change Chrome extension code, change Android code, move tags, or modify the frozen `mvp-u5-ok` release.
