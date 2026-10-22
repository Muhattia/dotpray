package com.dotpray.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.dotpray.app.prayer.AsrMethod;

/** Everything the app remembers. All of it stays on the device. */
public class PrayerPrefs {

    private static final String FILE = "dotpray_prefs";

    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_NAME = "name";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_ZONE = "zone";
    private static final String KEY_GPS = "gps";
    private static final String KEY_ASR = "asr";
    private static final String KEY_ALERTS = "alerts";
    private static final String KEY_24H = "clock24";
    private static final String KEY_THEME = "theme";
    private static final String KEY_LOCATION_ASKED = "location_asked";
    private static final String KEY_LOCATION_AT = "location_at";

    /** Theme choices. The values are stable and stored in SharedPreferences. */
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private final SharedPreferences sp;

    public PrayerPrefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public Place getPlace() {
        String name = sp.getString(KEY_NAME, Place.CAIRO.name);
        String country = sp.getString(KEY_COUNTRY, Place.CAIRO.country);
        double lat = sp.getFloat(KEY_LAT, (float) Place.CAIRO.latitude);
        double lon = sp.getFloat(KEY_LON, (float) Place.CAIRO.longitude);
        String zone = sp.getString(KEY_ZONE, Place.CAIRO.zoneId);
        boolean gps = sp.getBoolean(KEY_GPS, false);
        return new Place(name, country, lat, lon, zone, gps);
    }

    public void setPlace(Place place) {
        sp.edit()
                .putString(KEY_NAME, place.name)
                .putString(KEY_COUNTRY, place.country)
                .putFloat(KEY_LAT, (float) place.latitude)
                .putFloat(KEY_LON, (float) place.longitude)
                .putString(KEY_ZONE, place.zoneId)
                .putBoolean(KEY_GPS, place.gps)
                .apply();
    }

    /** True once the first-launch location prompt has been shown. */
    public boolean hasAskedForLocation() {
        return sp.getBoolean(KEY_LOCATION_ASKED, false);
    }

    public void setAskedForLocation() {
        sp.edit().putBoolean(KEY_LOCATION_ASKED, true).apply();
    }

    /** Wall-clock time of the last accepted GPS fix, or 0 before the first fix. */
    public long getLocationUpdatedAt() {
        return sp.getLong(KEY_LOCATION_AT, 0L);
    }

    public void setLocationUpdatedAt(long timestamp) {
        sp.edit().putLong(KEY_LOCATION_AT, timestamp).apply();
    }

    public int getThemeMode() {
        int mode = sp.getInt(KEY_THEME, THEME_SYSTEM);
        if (mode < THEME_SYSTEM || mode > THEME_DARK) return THEME_SYSTEM;
        return mode;
    }

    public void setThemeMode(int mode) {
        if (mode < THEME_SYSTEM || mode > THEME_DARK) mode = THEME_SYSTEM;
        sp.edit().putInt(KEY_THEME, mode).apply();
    }

    public AsrMethod getAsr() {
        return AsrMethod.fromKey(sp.getString(KEY_ASR, AsrMethod.STANDARD.key));
    }

    public void setAsr(AsrMethod asr) {
        sp.edit().putString(KEY_ASR, asr.key).apply();
    }

    public boolean isAlertsEnabled() {
        return sp.getBoolean(KEY_ALERTS, false);
    }

    public void setAlertsEnabled(boolean enabled) {
        sp.edit().putBoolean(KEY_ALERTS, enabled).apply();
    }

    public boolean is24Hour() {
        return sp.getBoolean(KEY_24H, true);
    }

    public void set24Hour(boolean value) {
        sp.edit().putBoolean(KEY_24H, value).apply();
    }
}
