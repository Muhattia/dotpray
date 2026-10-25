package com.dotpray.app.data;

import java.time.ZonedDateTime;
import java.util.Locale;

/** Small numeric formatting helpers shared by the UI and the widgets. */
public final class Format {

    private Format() {
    }

    /**
     * Coordinate with caller-supplied hemisphere letters, so the same numeric
     * layout can be rendered in either language without hardcoded copy.
     */
    public static String coord(double value, boolean isLatitude,
                               String north, String south, String east, String west) {
        double abs = Math.abs(value);
        String hemi = isLatitude
                ? (value >= 0 ? north : south)
                : (value >= 0 ? east : west);
        return String.format(Locale.US, "%.3f°%s", abs, hemi);
    }

    /** 24 hour clock, e.g. 05:12 */
    public static String time24(ZonedDateTime t) {
        return String.format(Locale.US, "%02d:%02d", t.getHour(), t.getMinute());
    }

    /** 12 hour clock without meridiem, e.g. 5:12 */
    public static String time12(ZonedDateTime t) {
        int h = t.getHour() % 12;
        if (h == 0) h = 12;
        return String.format(Locale.US, "%d:%02d", h, t.getMinute());
    }

    /** Countdown as HH:MM. */
    public static String duration(long millis) {
        long total = Math.max(0, millis / 60_000L);
        long h = total / 60;
        long m = total % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    public static String compactDuration(long millis) {
        long total = Math.max(0, millis / 60_000L);
        long h = total / 60;
        long m = total % 60;
        return String.format(Locale.US, "%dh %02dm", h, m);
    }
}
