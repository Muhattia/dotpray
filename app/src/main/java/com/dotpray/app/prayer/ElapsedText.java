package com.dotpray.app.prayer;

import android.content.Context;

import com.dotpray.app.R;
import com.dotpray.app.data.UiText;

/**
 * Builds the post-Adhan elapsed message entirely from localized resources.
 *
 * <p>The widgets and the app call these helpers, so the prayer name and the
 * minute grammar always come from the same {@code values/} or
 * {@code values-ar/} resources. No Arabic or English sentence is assembled in
 * Java.
 */
public final class ElapsedText {

    private ElapsedText() {
    }

    /** Full sentence, e.g. "5 minutes since Asr" or "مرّت 5 دقائق على العصر". */
    public static String full(Context context, int minutes, Prayer prayer) {
        int value = Math.max(0, minutes);
        String name = PrayerNames.get(context, prayer);
        return UiText.quantity(context, R.plurals.elapsed_minutes, value,
                value, name);
    }

    /** First line of the compact card, e.g. "5 minutes" or "مرّت 5 دقائق". */
    public static String compact(Context context, int minutes) {
        int value = Math.max(0, minutes);
        return UiText.quantity(context, R.plurals.elapsed_minutes_compact,
                value, value);
    }

    /** Second line of the compact card, e.g. "since Asr" or "على العصر". */
    public static String on(Context context, Prayer prayer) {
        return context.getString(R.string.elapsed_on,
                PrayerNames.get(context, prayer));
    }
}
