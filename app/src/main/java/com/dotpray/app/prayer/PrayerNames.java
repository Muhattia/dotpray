package com.dotpray.app.prayer;

import android.content.Context;

import com.dotpray.app.R;
import com.dotpray.app.data.Language;
import com.dotpray.app.data.LocaleController;

/** Resolves a prayer's name from the selected language's resources. */
public final class PrayerNames {

    private PrayerNames() {
    }

    public static int resource(Prayer prayer) {
        switch (prayer) {
            case FAJR:
                return R.string.prayer_fajr;
            case DHUHR:
                return R.string.prayer_dhuhr;
            case ASR:
                return R.string.prayer_asr;
            case MAGHRIB:
                return R.string.prayer_maghrib;
            case ISHA:
            default:
                return R.string.prayer_isha;
        }
    }

    public static String get(Context context, Prayer prayer) {
        return context.getString(resource(prayer));
    }

    /** English is upper-cased for the display face; Arabic is left untouched. */
    public static String display(Context context, Prayer prayer) {
        return Language.displayCase(LocaleController.getLanguage(context),
                get(context, prayer));
    }
}
