package com.dotpray.app.data;

import java.util.Locale;

/** Small formatting helpers shared by the UI and the widgets. */
public final class Format {

    private Format() {
    }

    public static String coord(double value, boolean isLatitude) {
        double abs = Math.abs(value);
        String hemi = isLatitude ? (value >= 0 ? "N" : "S") : (value >= 0 ? "E" : "W");
        return String.format(Locale.US, "%.3f°%s", abs, hemi);
    }

    /** 24 hour clock, e.g. 05:12 */
    public static String time24(java.time.ZonedDateTime t) {
        return String.format(Locale.US, "%02d:%02d", t.getHour(), t.getMinute());
    }

    /** 12 hour clock without meridiem, e.g. 5:12 */
    public static String time12(java.time.ZonedDateTime t) {
        int h = t.getHour() % 12;
        if (h == 0) h = 12;
        return String.format(Locale.US, "%d:%02d", h, t.getMinute());
    }

    public static String meridiem(java.time.ZonedDateTime t) {
        return t.getHour() < 12 ? "AM" : "PM";
    }

    /** Countdown as HH:MM:SS */
    public static String duration(long millis) {
        long total = Math.max(0, millis / 1000L);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    public static String compactDuration(long millis) {
        long total = Math.max(0, millis / 1000L);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        return String.format(Locale.US, "%dh %02dm", h, m);
    }

    public static String cardinal(double bearing) {
        String[] points = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(bearing / 22.5) % 16;
        return points[index];
    }
}
