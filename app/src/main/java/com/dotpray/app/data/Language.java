package com.dotpray.app.data;

import java.util.Locale;

/**
 * The two languages the app ships with. Kept free of Android types so the
 * language and RTL rules can be exercised by the JVM tests.
 */
public final class Language {

    public static final String ENGLISH = "en";
    public static final String ARABIC = "ar";

    /** English stays the default so an existing installation looks unchanged. */
    public static final String DEFAULT = ENGLISH;

    private Language() {
    }

    /** Unknown or empty stored values fall back to English. */
    public static String normalize(String language) {
        return ARABIC.equals(language) ? ARABIC : ENGLISH;
    }

    public static boolean isArabic(String language) {
        return ARABIC.equals(normalize(language));
    }

    public static boolean isRtl(String language) {
        return isArabic(language);
    }

    public static Locale locale(String language) {
        return isArabic(language) ? new Locale("ar") : Locale.ENGLISH;
    }

    /** Latin UI is upper-cased; Arabic has no case and must be left alone. */
    public static String displayCase(String language, String text) {
        if (text == null) return "";
        return isArabic(language) ? text : text.toUpperCase(Locale.US);
    }
}
