# Google Play Data safety — draft answers

**App:** Dotpray 1.3 (`com.dotpray.app`)  
**Status:** Draft for Play Console. Re-check every answer against the build you
upload; the form is a legal declaration, not marketing copy.

## Short version

Dotpray has **no server, no account, no ads, no analytics, and no `INTERNET`
permission**. Prayer times are calculated on the device. Location is read
on-device to calculate those times and is stored in the app's local
preferences; it is not sent to Dotpray.

## Data safety form

| Question | Recommended answer | Notes |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | No data is transmitted off the device by Dotpray. |
| Does your app use advertising or marketing? | **No** | No ad SDKs. |
| Does your app use analytics? | **No** | No analytics or crash-reporting SDKs. |
| Does your app create an account? | **No** | No login, profile, or cloud sync. |
| Does your app allow users to request data deletion? | **No account/data on a server** | Uninstalling the app or clearing app data removes local settings. |
| Is all user data encrypted in transit? | **Not applicable** | Dotpray does not transmit user data. |
| Does your app use location? | **Yes — approximate and precise, for app functionality** | Used on-device to calculate prayer times and to ask the platform geocoder for a place name. Not collected or shared by Dotpray. |
| Does your app access photos, contacts, microphone, camera, or files? | **No** | None of these permissions are declared. |
| Does your app use notifications? | **Yes — prayer alerts** | `POST_NOTIFICATIONS`; local notifications only. |
| Does your app use exact alarms? | **Yes — prayer alerts** | `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`; local scheduling only. |
| Does your app receive boot-completed broadcasts? | **Yes — to re-arm local alarms/widgets** | `RECEIVE_BOOT_COMPLETED`; no data collection. |

### Location wording for the privacy policy / console notes

> With your permission, Dotpray reads your approximate or precise location on
> the device to calculate prayer times and to ask Android's platform geocoder
> for a place name. The coordinates and place name are stored in the app's
> local preferences. Dotpray has no server and declares no `INTERNET`
> permission, so it does not upload them. The Android platform geocoder is an
> operating-system service and may process the request under the device
> manufacturer's or Google's terms. If no location is available, Cairo is used
> as a neutral starting point.

If Play Console's Data safety form treats the platform geocoder as a transfer
to a third party, disclose location as **shared with the operating system's
geocoding service** and link to this policy. Do not claim “no location data
leaves the device” without that qualification.

## Android backup

The manifest currently sets `android:allowBackup="true"`. If the user has
Android backup enabled, the operating system may back up app preferences
(including the saved location) to the user's Google account. This is an
OS-level backup controlled by the user, not a Dotpray server. Mention it in the
privacy policy and disable backup if you do not want that behaviour.

## Permissions declared by the app

| Permission | Why |
|---|---|
| `ACCESS_COARSE_LOCATION` | Calculate prayer times from the current location. |
| `ACCESS_FINE_LOCATION` | Same, with a precise fix. |
| `POST_NOTIFICATIONS` | Show the optional prayer alert. |
| `RECEIVE_BOOT_COMPLETED` | Re-arm local prayer alarms and widget refresh after reboot. |
| `SCHEDULE_EXACT_ALARM` | Deliver prayer alerts at the intended time when Android allows. |
| `USE_EXACT_ALARM` | Exact-alarm capability on supported Android versions. |

**Not declared:** `INTERNET`, `ACCESS_NETWORK_STATE`, camera, microphone,
contacts, storage, advertising ID.

## Content rating questionnaire

Expected result: **Everyone**. The app contains:
- no violence, sexual content, profanity, drugs, gambling, or horror;
- no user-generated content or social features;
- no ads or purchases;
- religious reference material (prayer times, Qibla, Hijri date) but no
  controversial or graphic content.

Confirm the IARC questionnaire in Play Console. Religious content alone does
not require a mature rating.

## Target audience

- **Recommended:** 13+.
- Not designed for children; do not opt into the Families programme.
- No personal data collection, but the app requests location permission, so a
  general-audience (13+) declaration is the cleanest fit.
