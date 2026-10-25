package com.dotpray.app.data;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Applies the in-app language and System / Light / Dark choice to a context.
 *
 * <p>The app uses the normal {@code values/}, {@code values-ar/} and
 * {@code values-night/} resource qualifiers. Both overrides are derived from
 * the application's clean configuration, not from a context that may already
 * carry a previous override, so switching back to System English cannot leave
 * an old locale or palette behind.
 */
public final class ThemeController {

    private ThemeController() {
    }

    /** Returns a context whose resources use the saved language and theme. */
    public static Context wrap(Context base) {
        Context app = base.getApplicationContext();
        if (app == null) app = base;

        PrayerPrefs prefs = new PrayerPrefs(app);
        Locale locale = Language.locale(prefs.getLanguage());

        Configuration config = new Configuration(
                app.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        int mode = prefs.getThemeMode();
        if (mode != PrayerPrefs.THEME_SYSTEM) {
            int night = mode == PrayerPrefs.THEME_DARK
                    ? Configuration.UI_MODE_NIGHT_YES
                    : Configuration.UI_MODE_NIGHT_NO;
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        }
        return base.createConfigurationContext(config);
    }

    /** True when the saved choice resolves to the dark palette. */
    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}
