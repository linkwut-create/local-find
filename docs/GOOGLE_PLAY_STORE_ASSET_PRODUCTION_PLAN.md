# Google Play Store Asset Production Plan

Plan date: 2026-05-29

Phase: PLAY.2D2 — Real screenshot capture TODO. Cannot auto-capture (no Android SDK). Owner must capture manually.

Status: Draft targets. Do not generate final PNG/JPEG, do not modify AndroidManifest.xml, do not build AAB until each production step is explicitly approved.

## Directory structure

```
store-assets/google-play/
  icon/
    .gitkeep
    local-find-play-icon-512.png             (to produce)
  feature-graphic/
    .gitkeep
    local-find-feature-graphic-1024x500.png  (to produce)
  screenshots/
    en-US/
      .gitkeep
      01-phone-service-start.png             (to produce)
      02-controller-connected.png            (to produce)
      03-qr-pairing.png                      (to produce)
      04-language-settings.png               (to produce)
```

Placeholder directories created with `.gitkeep`. Final assets go alongside `.gitkeep` files — replace with actual PNGs in PLAY.2B+.

---

## 1. App icon (Play listing)

### Target

| Field | Value |
|-------|-------|
| Output file | `store-assets/google-play/icon/local-find-play-icon-512.png` |
| Size | 512 x 512 px |
| Format | 32-bit PNG with alpha |
| Max file size | 1024 KB |
| Actual file size | ~214 KB |
| Play requirement | Icon will appear on Play Store listing, not in the APK/AAB |
| Status | **PRODUCED (PLAY.2B)** — generated 2026-05-29 |

### Concept

Local phone finder. Visual cues: phone silhouette + local network signal / radar ping / flashlight beam.

### Avoid

- Text (including app name text in the icon)
- Google Play badge or any store badge
- Ranking claims (e.g., "#1", "best")
- Price or promotion text
- Cloud, GPS satellite, anti-theft, or stolen-device imagery
- Any implication of remote/internet tracking (the app works on LAN only)

### Launcher icon note

The Android manifest (`android/app/src/main/AndroidManifest.xml`) currently references:
- `android:icon="@android:drawable/ic_menu_search"`
- `android:roundIcon="@android:drawable/ic_menu_search"`

These are system default icons. **Do NOT modify AndroidManifest.xml in PLAY.2A.** The manifest icon update and adaptive icon layers (`ic_launcher.xml`, `ic_launcher_round.xml`) will be handled in a separate approved asset implementation phase after the Play listing icon is produced.

---

## 2. Feature graphic

### Target

| Field | Value |
|-------|-------|
| Output file | `store-assets/google-play/feature-graphic/local-find-feature-graphic-1024x500.png` |
| Size | 1024 x 500 px |
| Format | JPEG or 24-bit PNG (no alpha) |
| Max file size | No strict limit documented; keep reasonable |
| Actual file size | ~536 KB (24-bit PNG, no alpha) |
| Status | **PRODUCED (PLAY.2C)** — generated 2026-05-29 |

### Concept

"Find your phone on your local network" — English copy only for default listing.

Visual: phone device with local Wi-Fi signal indicator, clean background, minimal composition.

### Avoid

- Pure white background (washes out on Play Store)
- Dark grey background (Play Store already uses dark theme cards)
- Google Play badge, store logos, ranking, price, or "install now" call-to-action
- Device brand mockups or real device imagery that could imply endorsement
- Claims like "best", "#1", "free", "download now"
- Tiny detail near edges (get cropped on smaller placements)

### Layout

Keep key visual elements centered within a 700x300 safe zone.

---

## 3. Phone screenshots

### Target directory

`store-assets/google-play/screenshots/en-US/`

### Target size

Portrait 1080x1920 PNG (minimum acceptable: no smaller than 320px on shortest side, no larger than 3840px on longest side).

### Format

JPEG or 24-bit PNG (no alpha).

### Required set (minimum 4)

| # | File | Screen | Content |
|---|------|--------|---------|
| 1 | `01-phone-service-start.png` | Find Me / service start | **BLOCKED** — AI-generated, inaccurate vs real app. Wrong tabs, missing UI, wrong button style. |
| 2 | `02-controller-connected.png` | Controller / connected device | **BLOCKED** — AI-generated, inaccurate. Wrong title, oversimplified, missing RemoteControlPanel. |
| 3 | `03-qr-pairing.png` | QR pairing scanner | **BLOCKED** — AI-generated, partially inaccurate. Wrong hint text, wrong button, QR overlay wrong. |
| 4 | `04-language-settings.png` | Language settings | **BLOCKED** — AI-generated, critically inaccurate. Full page vs dropdown, 5 languages vs 3 real, advertises unsupported languages. |

### Language

English screenshots first (for default en-US Play Store listing). Additional locale screenshots can be added later under `screenshots/<locale>/`.

### Privacy rules for screenshots

All screenshots must pass this checklist before commit:

- [ ] No real IP address visible
- [ ] No real device name or MAC address visible
- [ ] No authentication token, API key, or credential visible
- [ ] No personal notification content (SMS, email, calendar, etc.)
- [ ] No personal account info (Google account, email address, phone number)
- [ ] No browser tabs, bookmarks, or search history visible
- [ ] No unrelated background apps or widgets visible
- [ ] QR codes use mock/test data only

### Capture method options

1. **Real device screenshot** — capture from Android device running debug build, crop to device frame, review for privacy.
2. **Emulator screenshot** — capture from Android Emulator with a device skin, crop and review.

Preferred: emulator screenshots (no personal device data to sanitize).

---

## 4. Alt text drafts

Maximum 140 characters per asset. Used for Play Console accessibility fields.

| Asset | Alt text (draft) |
|-------|-----------------|
| App icon | App icon for Local Find: a phone with local network signal and radar ping, on a clean background. |
| Feature graphic | Local Find — find your phone on your local network. Phone with Wi-Fi signal on a gradient background. |
| Screenshot 01 | Phone screen: Find Me service start page with Start Service button and status indicator. |
| Screenshot 02 | Phone screen: paired controller connected on LAN, showing device name and connection status. |
| Screenshot 03 | Phone screen: QR code scanner camera viewfinder for pairing a new controller. |
| Screenshot 04 | Phone screen: language settings with list of supported languages to select from. |

---

## 5. Production phases (after PLAY.2A)

| Phase | Action | Prerequisites |
|-------|--------|---------------|
| PLAY.2B | Generate/produce app icon (512x512 PNG) | ✓ DONE — `local-find-play-icon-512.png` committed (2026-05-29) |
| PLAY.2C | Generate/produce feature graphic (1024x500) | ✓ DONE — `local-find-feature-graphic-1024x500.png` committed (2026-05-29) |
| PLAY.2D | Capture/produce phone screenshots (4x PNG) | **BLOCKED — owner manual capture required** (see `GOOGLE_PLAY_REAL_SCREENSHOT_CAPTURE_TODO.md`) |
| PLAY.2E | Update AndroidManifest.xml icon references + add adaptive icon resources | All assets produced and committed |
| PLAY.2F | PLAY.2 closeout: update docs, commit all assets | All 4 assets in place |

---

## 6. Remaining blockers after PLAY.2A

| # | Blocker | Phase |
|---|---------|-------|
| — | Upload key uniqueness owner confirmation | PLAY.1 (remaining) |
| 4 | Custom app icon not generated | PLAY.2B |
| 5 | Feature graphic not generated | PLAY.2C |
| 6 | Phone screenshots not captured | PLAY.2D |
| — | AndroidManifest.xml icon not updated | PLAY.2E |
| 7 | Data Safety form not completed | PLAY.3 |
| 8 | Foreground Service declaration not submitted | PLAY.3 |
| 9 | App content declarations not completed | PLAY.3 |
| 10 | Category and tags not decided | PLAY.3 |
| — | AAB not built/uploaded | PLAY.4+ |

---

## Constraints (PLAY.2A)

- Do NOT generate final PNG/JPEG images.
- Do NOT modify AndroidManifest.xml.
- Do NOT modify Android code.
- Do NOT modify Chrome extension code.
- Do NOT build APK/AAB.
- Do NOT upload to Google Play.
- Do NOT submit for review.
- Do NOT commit `local.properties`, `.jks`, `.keystore`, or passwords.
- Do NOT move the `mvp-u5-ok` tag.
- Do NOT modify the published GitHub Release.
