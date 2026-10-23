# Dotpray privacy policy

**Effective date:** 23 October 2026  
**App:** Dotpray (`com.dotpray.app`)  
**Developer:** [DEVELOPER NAME]  
**Contact:** [SUPPORT EMAIL]

> **Before publishing:** replace `[DEVELOPER NAME]` and `[SUPPORT EMAIL]`, host
> this page at a public HTTPS URL, and put that URL in Play Console. The
> suggested GitHub Pages URL is
> `https://muhattia.github.io/dotpray/privacy.html` (requires enabling Pages for
> the repository's `docs/` folder).

## The short version

Dotpray is a prayer clock that runs on your phone. It has no account, no ads,
no analytics, no tracking SDKs, and no `INTERNET` permission. Prayer times are
calculated on the device. Your location is used on the device to calculate
those times and is stored locally; Dotpray does not send it to a Dotpray
server.

## What Dotpray stores

Dotpray stores the following in the app's private storage on your device:

- your saved location (place name, country, latitude, longitude, and time
  zone);
- your settings (12/24-hour clock, theme choice, Asr school, and whether
  prayer alerts are enabled);
- timestamps needed to refresh the widget and re-arm local alarms.

You can delete this data by clearing the app's storage in Android settings or
by uninstalling Dotpray. There is no Dotpray account or server-side profile to
delete.

## Location

With your permission, Dotpray reads your approximate or precise location from
Android to calculate prayer times and to ask the platform's geocoder for a
place name.

- The coordinates and place name are stored in the app's private preferences.
- Dotpray does not upload them: the app declares no `INTERNET` permission and
  has no server.
- If you do not grant location permission, or no fix is available, Dotpray uses
  Cairo as a neutral starting point.
- The app re-checks the current location periodically while it is open, and
  when it resumes after the saved fix is more than 15 minutes old.

**Platform geocoder.** To display a place name, Dotpray calls Android's
`Geocoder`. That is an operating-system service, not a Dotpray server. The
device manufacturer or Google may process the request under its own privacy
terms, depending on the device. Dotpray does not control that service.

## Notifications and alarms

If you enable prayer alerts, Dotpray schedules local notifications for prayer
times. This uses Android's notification and alarm APIs, including exact alarms
when the operating system allows them. The alert content is generated locally.
No notification data is sent to Dotpray.

## Android backup

Android may back up app data if backup is enabled on your device. If so, your
Dotpray preferences (including the saved location) may be included in your
Google account's device backup. This is controlled by Android and your Google
account settings, not by a Dotpray server. You can disable backup in Android
settings.

## What Dotpray does not do

- No accounts, sign-in, or cloud sync.
- No advertising or ad SDKs.
- No analytics, crash-reporting, or attribution SDKs.
- No tracking across apps or websites.
- No sale or rental of personal information.
- No `INTERNET` permission.
- No access to contacts, camera, microphone, photos, or files.

## Sharing

Dotpray does not share personal information with third parties. The only
non-Dotpray processing described above is performed by Android system services
(the platform geocoder, notifications, alarms, and optional device backup)
under the operating system's terms.

## Security

Dotpray keeps its data in Android's app-private storage. Because the app has no
`INTERNET` permission, it cannot transmit that data to a Dotpray server. No
method of storage is perfectly secure, but the app is designed to minimise the
data it handles.

## Children

Dotpray is not directed to children under 13. The app does not knowingly
collect personal information from children. It has no account or server-side
profile.

## Changes

If this policy changes, the effective date above will be updated and the new
version will be published at the same URL. Material changes will also be
reflected in the app or its store listing where appropriate.

## Contact

Questions about this policy or the app:

**[DEVELOPER NAME]**  
**[SUPPORT EMAIL]**  
Project page: https://github.com/Muhattia/dotpray
