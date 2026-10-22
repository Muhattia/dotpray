package com.dotpray.app.prayer;

import java.time.LocalDate;

/**
 * Gregorian to Hijri conversion using the published Umm al-Qura algorithm.
 * Kept dependency free so it behaves identically on every Android release.
 */
public final class Hijri {

    private static final String[] MONTHS = {
            "MUHARRAM", "SAFAR", "RABI AL-AWWAL", "RABI AL-THANI",
            "JUMADA AL-AWWAL", "JUMADA AL-THANI", "RAJAB", "SHABAN",
            "RAMADAN", "SHAWWAL", "DHU AL-QIDAH", "DHU AL-HIJJAH"
    };

    private final int year;
    private final int month;
    private final int day;

    private Hijri(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static Hijri from(LocalDate date) {
        int jd = julianDay(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        long l0 = jd - 1948440L + 10632L;
        long n = (l0 - 1) / 10631L;
        long l = l0 - 10631L * n + 354L;
        long j = ((10985L - l) / 5316L) * ((50L * l) / 17719L)
                + (l / 5670L) * ((43L * l) / 15238L);
        long k = l - ((30L - j) / 15L) * ((17719L * j) / 50L)
                - (j / 16L) * ((15238L * j) / 43L) + 29L;
        int month = (int) ((24L * k) / 709L);
        int day = (int) (k - (709L * month) / 24L);
        int year = (int) (30L * n + j - 30L);
        return new Hijri(year, month, day);
    }

    /** e.g. "10 JUMADA AL-AWWAL 1448" */
    public String label() {
        String name = MONTHS[Math.max(0, Math.min(11, month - 1))];
        return day + " " + name + " " + year;
    }

    private static int julianDay(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }
}
