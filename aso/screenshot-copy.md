# Screenshot copy and order

All screenshots are **1080 × 1920** (9:16), RGB PNG, ready for the phone
screenshot slot. The five app screens are composed from the layout-accurate
previews in `preview/`; the actual app uses the same layouts and colours.

| # | File | Headline | Supporting line | Job |
|---|---|---|---|---|
| 1 | `01-prayer-times.png` | PRAYER TIMES, WITHOUT THE NOISE. | The next prayer, a countdown, and the full day — on one quiet screen. | Show the core product immediately. |
| 2 | `02-live-countdown.png` | EVERY MINUTE COUNTS. | A calm countdown, with a rail from the last prayer. | Make the countdown the hook. |
| 3 | `03-widgets.png` | YOUR NEXT PRAYER, AT A GLANCE. | Three widget layouts: a wide 4×2 schedule, a small 3×3 with the elapsed rail, or a minimal 2×2 without it. | Show the home-screen habit. |
| 4 | `04-themes.png` | DARK. LIGHT. YOUR CHOICE. | Follow the system, or keep it fixed. All widgets follow. | Show personalization and polish. |
| 5 | `05-private.png` | PRIVATE BY DEFAULT. | A prayer app with no server. | Close with no ads, no tracking, and no INTERNET permission. |

## Why this order

- **Screenshot 1** answers “what is this?” without requiring the user to read
  the description.
- **Screenshot 2** shows the one thing a static prayer-times app cannot do:
  a countdown that stays current.
- **Screenshot 3** moves the value onto the home screen, where the app is meant
  to live.
- **Screenshot 4** proves the design is intentional in both themes.
- **Screenshot 5** closes with the sharpest differentiator: no ads, no
  tracking, and no `INTERNET` permission.

## Text rules used

- One idea per screenshot.
- Five words or fewer in the headline where possible.
- Supporting line under 90 characters.
- No “Download now”, “Install”, star ratings, awards, or competitor logos.
- No fake UI or features the app does not have.
- Red is reserved for the next-prayer accent and the privacy points.

## Before publishing

The five app screens are marketing renders, not literal device captures. They
mirror the real layouts, but a final QA pass on a physical device is still
recommended.

1. Install `dist/dotpray-1.3.apk` on a phone.
2. Set the location to Cairo and the theme to Dark, then check screenshot 1.
3. Add all three widgets and confirm the wide/small/minimal layouts, the elapsed rail, resize handles, and the grace state a minute after a prayer.
4. Switch App Language to العربية and confirm the RTL layout, Alexandria labels, localized widgets and notifications.
5. If any system font fallback or launcher difference changes the layout,
   replace the affected render with a real device capture at 1080 × 1920.
