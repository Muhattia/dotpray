package com.dotpray.app.data;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Applies the in-app System / Light / Dark choice to a context.
 *
 * <p>The app uses the normal {@code values/} and {@code values-night/} resource
 * qualifiers. Wrapping a context with an overridden {@code uiMode} lets the
 * same resources render a light app while the phone itself is in dark mode (or
 * the other way round), without AndroidX or a second set of colours.
 */
public final class ThemeController {

    private ThemeController() {
    }

    /** Returns a context whose resources use the saved theme choice. */
    public static Context wrap(Context base) {
        // Always start from the application context. An Activity can already
        // carry a manual configuration override, and wrapping that again when
        // switching back to System would keep the old forced palette.
        Context app = base.getApplicationContext();
        if (app == null) app = base;

        int mode = new PrayerPrefs(app).getThemeMode();
        if (mode == PrayerPrefs.THEME_SYSTEM) return app;

        Configuration config = new Configuration(app.getResources().getConfiguration());
        int night = mode == PrayerPrefs.THEME_DARK
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        return app.createConfigurationContext(config);
    }

    /** True when the saved choice resolves to the dark palette. */
    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}
