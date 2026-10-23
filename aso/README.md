# Dotpray — Google Play ASO package

Prepared for **Dotpray 1.2** (`com.dotpray.app`, version code 17).  
This folder contains the store listing copy, screenshots, feature graphic, icon,
privacy policy, and the Data safety draft.

## What is in here

| File | Use |
|---|---|
| `play-listing.md` | App name, short description, full description, release notes, category, keywords, and ASO rationale. |
| `screenshots/01-prayer-times.png` | Phone screenshot 1 — 1080 × 1920. |
| `screenshots/02-live-countdown.png` | Phone screenshot 2 — 1080 × 1920. |
| `screenshots/03-widgets.png` | Phone screenshot 3 — 1080 × 1920. |
| `screenshots/04-themes.png` | Phone screenshot 4 — 1080 × 1920. |
| `screenshots/05-private.png` | Phone screenshot 5 — 1080 × 1920. |
| `graphics/feature-graphic-1024x500.png` | Feature graphic, 1024 × 500, RGB (no alpha). |
| `graphics/app-icon-512.png` | Play Store icon, 512 × 512, 32-bit PNG. |
| `screenshot-copy.md` | The wording and order of the screenshots. |
| `data-safety.md` | Draft Data safety and content-rating answers. |
| `privacy-policy.md` | Privacy policy source. |
| `privacy-policy.html` | Ready-to-host privacy policy page. |
| `../docs/privacy.html` | Copy for GitHub Pages (`main` / `docs`). |
| `../tools/make_aso.py` | Reproducible generator for all images. |

## Quick start in Play Console

1. **Create the app**
   - Name: `Dotpray: Offline Prayer Times`
   - Default language: English (United States)
   - App or game: App
   - Free or paid: Free
   - Package: `com.dotpray.app`

2. **Upload a release**
   - Google Play requires an **Android App Bundle (AAB)** for a new app.
   - The current `build.sh` produces a signed **APK** for direct installation;
     do not upload the debug-signed APK to production.
   - Use Play App Signing and a release/upload keystore. If you want, the next
     engineering step is to add an AAB pipeline to this repo.

3. **Store listing**
   - Copy from `play-listing.md`.
   - Upload the five screenshots in numerical order.
   - Upload `feature-graphic-1024x500.png` and `app-icon-512.png`.
   - Replace `[PLACEHOLDER: support email]` with a monitored address.
   - Replace the privacy-policy placeholder with the hosted URL.

4. **Policy declarations**
   - Content rating: complete the IARC questionnaire; expected **Everyone**.
   - Target audience: **13+**, not designed for children.
   - Ads: **No**.
   - App access: no login; all screens accessible. Location is optional;
     Cairo is the neutral default.
   - Data safety: follow `data-safety.md`; verify the final form against the
     build you upload.
   - News / government / COVID / financial / health: **No**.

5. **Privacy policy**
   - Replace `[DEVELOPER NAME]` and `[SUPPORT EMAIL]` in
     `privacy-policy.html` (and the Markdown source).
   - Host it at a public HTTPS URL. The included `docs/privacy.html` is ready
     for GitHub Pages:
     **Settings → Pages → Deploy from a branch → `main` / `docs`**.
   - The intended URL is
     `https://muhattia.github.io/dotpray/privacy.html`.
   - Do **not** submit the policy with the placeholders still in it.

6. **Test before production**
   - Upload to internal testing first.
   - Install on a clean device and check the location prompt, prayer alerts,
     both widget sizes, both themes, and the settings sheet.
   - Run the Play pre-launch report.
   - Replace any screenshot that does not match the device rendering.

## Still needed before production

- A **release-signed Android App Bundle** and a Play Console developer account.
- A monitored **support email** and a hosted **privacy-policy URL**.
- Final QA of the app on a clean device; replace marketing renders if the
  device layout differs.
- Play Console declarations: content rating, Data safety, target audience,
  ads, app access, and (if applicable) government/news/financial/health.

## Image specs

| Asset | Size | Format | Status |
|---|---|---|---|
| Phone screenshots | 1080 × 1920 (9:16) | RGB PNG | 5 of 5 ready |
| Feature graphic | 1024 × 500 | RGB PNG, no alpha | Ready |
| Store icon | 512 × 512 | 32-bit PNG with alpha | Ready |
| App icon source | 1024 × 1024 | RGBA PNG | In `preview/` |
| Privacy policy | HTML | Self-contained | Ready to host |

Google Play accepts 2–8 phone screenshots; five are supplied. Tablet and
Chromebook screenshots are **not** included because the app has not been
visually QA'd on those form factors.

## ASO notes

- **Title:** brand + `Offline` + `Prayer Times` fits in 29/30 characters.
- **Short description:** six benefit keywords in 75/80 characters.
- **First screenshot:** shows the whole product and the red live accent.
- **Second screenshot:** the countdown is the habit hook.
- **Third screenshot:** the widget is the retention hook.
- **Fifth screenshot:** privacy is the trust hook.
- **Avoid:** “adhan” (no audio call to prayer), “Nothing OS” (no affiliation),
  “all methods” (only one calculation method), and “Arabic” (not supported).

Run store-listing experiments on:
1. title (offline vs. widget emphasis),
2. short description (privacy vs. countdown emphasis),
3. feature graphic (wordmark vs. widget hero).

Measure **store listing conversion** (installs / store listing visitors), not
just installs. Review the first 30 days before changing everything at once.

## Rebuilding the images

From the repository root:

```bash
python3 tools/make_aso.py
```

The generator reads `preview/app-screen.png`, `preview/app-screen-light.png`,
`preview/widget-wide.png`, `preview/widget-small.png`, and the Doto fonts. It
writes the five screenshots and two graphics into `aso/`.
