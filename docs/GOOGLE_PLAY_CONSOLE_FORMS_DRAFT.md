# Google Play Console Forms Draft

Draft date: 2026-05-29
Completed date: 2026-05-29

Phase: PLAY.3B — Play Console forms completed by owner. Forms saved as drafts in Play Console; not yet submitted for review.

> **Status**: Owner has manually entered and saved all form answers in Google Play Console. Play Console Dashboard shows no remaining incomplete items. Forms are saved as DRAFT — not submitted for review.

---

## 1. Data Safety

### Data collection

| Question | Answer | Notes |
|----------|--------|-------|
| Does your app collect or share any of the required user data types? | **No** | App stores data locally only (device names, LAN IPs, tokens). No data is transmitted to developer or third-party servers. |
| Does your app use any data types that are processed ephemerally? | N/A | Not applicable — no data transmitted. |

### Data types — all answered "No" (not collected)

| Data type | Collected? | Reason |
|-----------|-----------|--------|
| Location (approximate) | No | No location APIs used |
| Location (precise) | No | No GPS used |
| Personal info — Name | No | No account system |
| Personal info — Email address | No | No account system |
| Personal info — User IDs | No | No account system |
| Personal info — Address | No | Not requested |
| Personal info — Phone number | No | Not requested |
| Financial info | No | No payments, no financial features |
| Health and fitness | No | No health features |
| Messages | No | No messaging features |
| Photos and videos | No | Camera used only for QR scanning; images processed on-device, not stored or uploaded |
| Audio files | No | No audio features |
| Files and docs | No | No file access |
| Calendar | No | Not accessed |
| Contacts | No | Not accessed |
| App activity | No | No analytics, no logging to server |
| App info and performance | No | No crash reporting, no diagnostics uploaded |
| Device or other identifiers | No | No advertising ID, no device ID collected |

### Data sharing

| Question | Answer |
|----------|--------|
| Does your app share user data with third parties? | **No** |

### Data security

| Question | Answer | Notes |
|----------|--------|-------|
| Is data encrypted in transit? | N/A — no data transmitted to servers | LAN HTTP communication is cleartext but stays on local network. Do NOT claim internet transport encryption. |
| Can users request data deletion? | **Yes** | Users can delete saved devices locally and revoke paired controllers when reachable. Uninstalling the app removes all local data. |

### Owner confirmation required

- [ ] Review all "No" answers above in Play Console Data Safety form
- [ ] Confirm "No data collected" matches actual app behavior
- [ ] Policy note: even local-only data must be described in PRIVACY.md, which it is

---

## 2. App Content — Content Rating

### Content rating questionnaire

| Question | Answer |
|----------|--------|
| Violence | **None** — no violent content |
| Sexual content | **None** — no sexual content |
| Offensive language | **None** — no user-generated content, no profanity |
| Controlled substances | **None** — no references to alcohol, tobacco, drugs |
| Hate speech | **None** |
| Gambling | **None** — no gambling, no simulated gambling |
| User-generated content | **No** — app does not allow users to create or share content |
| In-app purchases | **No** — free app, no paid features |
| Unrestricted web browsing | **No** — app does not include a web browser |

### Content rating category

| Question | Answer |
|----------|--------|
| Target audience | **General audience** — no age restrictions |
| Designed for children under 13? | **No** |
| Does the app appeal to children? | **No** — it's a utility/tool app |
| Include marketing that targets children? | **No** |

### Owner confirmation required

- [ ] Complete content rating questionnaire in Play Console
- [ ] Confirm questionnaire answers match current app behavior

---

## 3. Ads Declaration

| Question | Answer |
|----------|--------|
| Does your app contain ads? | **No** |
| Does your app use any ad SDKs? | **No** |
| Does your app display ads from any ad network? | **No** |

---

## 4. Target Audience

| Question | Answer |
|----------|--------|
| Target age group | **General audience (18+)** — no age gates, no child-directed content |
| Designed for families? | No — it's a utility tool for adults |
| Teacher-approved program? | No |

---

## 5. Privacy Policy

| Field | Value |
|-------|-------|
| Privacy policy URL | `https://github.com/linkwut-create/local-find/blob/master/PRIVACY.md` |
| URL reachable? | Yes — confirmed for Chrome Web Store submission; same URL applies to Play |
| Support email | `linkwut@gmail.com` |

---

## 6. App Access / Login Requirement

| Question | Answer |
|----------|--------|
| Does your app require login credentials? | **No** |
| Do all features require login? | No — all features accessible without login |
| Is there a demo account? | Not applicable |
| Provide instructions for app review access? | Not applicable — no login required |

---

## 7. App Category and Tags

| Field | Recommended value | Rationale |
|-------|------------------|-----------|
| Category | **Productivity** or **Tools** | Utility app — phone finder / device management. Productivity is a common category for utility/tool apps. |
| Tags (max 5) | `utilities`, `networking`, `device-finder` | Tags must be selected from Play Console's available tag list; verify availability |

### Owner confirmation required

- [ ] Select category in Play Console (Productivity recommended, Tools as alternative)
- [ ] Select applicable tags from Play Console's available tag list

---

## 8. Foreground Service Declaration (specialUse)

### FGS type

| Field | Value |
|-------|-------|
| Foreground service type | `specialUse` |
| Subtype description | "Find lost phone controlled via local Wi-Fi HTTP API" |
| Service class | `.service.FindPhoneForegroundService` |

### Why specialUse

The app uses a foreground service because:
1. The user explicitly starts the service from the app UI.
2. The service keeps a local LAN HTTP listener active so paired controllers can find the phone.
3. A persistent notification informs the user the service is running.
4. The user can stop the service at any time.

This does NOT fit standard FGS types:
- Not `dataSync` — no cloud sync
- Not `location` — no location tracking
- Not `mediaPlayback` / `mediaProjection` — no media
- Not `connectedDevice` — the phone IS the device being found, not connecting to external peripherals

### Declaration text (English)

```
Local Find needs to run a foreground service so that paired
controllers on the same local network can reach the phone
after the user starts the service.

The user explicitly starts Local Find on the phone, and a
persistent notification appears while the service is active.
A paired controller can trigger local find actions such as
ring, flashlight strobe, and stop all.

This service does not perform background location tracking,
does not upload data to the internet, and does not use SMS
or remote relay. The user can stop the service at any time
from the app or the notification.
```

### Demo video (optional but recommended)

A short video showing:
1. User opens Local Find → starts service → persistent notification appears
2. Controller pairs on LAN
3. Controller triggers ring / flashlight / stop
4. User stops service

Decision on video deferred to owner. May be required for specialUse FGS review.

### Owner confirmation required

- [ ] Enter FGS declaration text in Play Console
- [ ] Decide whether to upload demo video as FGS evidence
- [ ] Verify specialUse subtype description matches manifest

---

## 9. News, Health, Financial, Government, Gambling Declarations

All answered **No** — not applicable.

| Declaration | Answer |
|-------------|--------|
| News app | No |
| Health app (medical/therapeutic) | No |
| Financial services (banking, lending, trading, insurance, etc.) | No |
| Government apps | No |
| Gambling / real-money games | No |
| COVID-19 contact tracing / status | No |
| Alcohol, tobacco, controlled substance sales | No |
| Dating / social networking | No |
| VPN service | No |

---

## 10. Developer Contact Info

| Field | Value |
|-------|-------|
| Developer name | (Owner to fill in Play Console) |
| Developer email | `linkwut@gmail.com` |
| Developer website | `https://github.com/linkwut-create/local-find` |
| Developer phone | Optional — not required |

---

## 11. Copyright / IP

| Item | Note |
|------|------|
| App icon | Custom — produced for Local Find (PLAY.2B/2E) |
| Feature graphic | Custom — produced for Local Find (PLAY.2C) |
| Screenshots | Real app captures (PLAY.2D3) |
| Code | Original — `io.github.linkwutcreate.localfind` |
| Third-party content | No third-party copyrighted content included |
| Trademarks | No trademark infringement known |

---

## 12. Export Compliance

| Question | Answer |
|----------|--------|
| Does your app use encryption? | LAN communication over HTTP is cleartext. No encryption of data in transit over the internet (no internet communication). |
| Does your app fall under US export controls? | Not expected — standard utility app, no cryptographic export concerns |

---

## Summary of all owner confirmations

1. [ ] Data Safety form — review all "No" answers, confirm no data collected
2. [ ] Content rating questionnaire — complete in Play Console
3. [ ] Ads declaration — confirm no ads
4. [ ] Target audience — general, not for children
5. [ ] Privacy policy URL — confirm reachable
6. [ ] App category — select Productivity (or Tools)
7. [ ] Tags — select from available Play Console tags
8. [ ] FGS declaration — enter text, decide on demo video
9. [ ] Developer contact info — fill in name/email/website
10. [ ] Store listing — short description, full description, screenshots, feature graphic, icon ready

---

## Constraints (PLAY.3A)

- Did not modify Android code.
- Did not modify Chrome extension code.
- Did not build APK/AAB.
- Did not upload to Google Play.
- Did not submit for review.
