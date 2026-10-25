package com.dotpray.app.data;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Applies the in-app language to a context, independently of the phone's
 * system language.
 *
 * <p>The app uses ordinary {@code values/} and {@code values-ar/} resources.
 * Wrapping the activity (and the widget/notification contexts) with the saved
 * locale means Android resolves the correct strings, plurals, fonts and
 * layout direction without restarting the whole process.
 */
public final class LocaleController {

    private LocaleController() {
    }

    public static String getLanguage(Context context) {
        return new PrayerPrefs(context).getLanguage();
    }

    public static boolean isArabic(Context context) {
        return Language.isArabic(getLanguage(context));
    }

    public static boolean isRtl(Context context) {
        return Language.isRtl(getLanguage(context));
    }

    /** Returns a context whose resources use the saved app language. */
    public static Context wrap(Context base) {
        Context app = base.getApplicationContext();
        if (app == null) app = base;
        return wrap(base, new PrayerPrefs(app).getLanguage());
    }

    public static Context wrap(Context base, String language) {
        Locale locale = Language.locale(language);
        Configuration config = new Configuration(
                base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }
}
