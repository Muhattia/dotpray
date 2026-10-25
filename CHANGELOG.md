# Changelog

## 1.3

### Added

- **Arabic interface.** Full Arabic resources and RTL layouts across the app,
  settings, dialogs, notifications, accessibility labels and all three widgets,
  with Alexandria for Arabic and Doto for numerals and English. Choose
  العربية / English in Settings, independently of the system language.
- **Three resizable widgets.** Small 3×3 with a dotted progress rail, Minimal
  2×2 without the rail, and Wide 4×2 with a live clock, Gregorian and Hijri
  dates, countdown and the five prayer times. All resize in both directions;
  the wide card drops its date and schedule rows when the cell is too short.
  The in-app **Add Widget** sheet pins the chosen size.
- **Qibla compass.** A live true-north compass with a 5° dial, cardinal marks,
  a red Qibla needle and a 3° alignment state, plus a calibration prompt and a
  bearing-only fallback when the sensor is unavailable. It follows the app
  language and System / Light / Dark theme and needs no new permission.
- **Per-widget Widget Settings.** Each placement opens its own settings screen,
  with a live preview and a 0–100% transparency slider. The card fades while
  the type, times and indicators stay readable, and every placement remembers
  its own level independently.

### Changed

- Prayer times, countdowns and widgets update on the minute (`HH:MM`).
- Arabic typography is optically scaled and mirrored, including prayer names,
  dates, grace messages and widget terminology.
- The wide widget aligns its name, time, clock, countdown, Gregorian and Hijri
  lines on a shared four-row grid.
- The home screen uses a single 52 dp action row and a full-width Current
  Location card.

### Fixed

- Prayer state and progress stay correct for the full grace period and reset
  exactly at the next Adhan, including across midnight and clock changes.
- Extreme-latitude grace is capped at the following Adhan.
- Android 12+ widgets keep the same resize behaviour as older releases.

## 1.2

- Centred widgets, a live next-prayer rail, refreshed icons, and a smoother
  settings sheet.
