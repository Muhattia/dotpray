package com.dotpray.app.prayer;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;

/**
 * Gregorian to Hijri conversion using the Umm al-Qura calendar.
 *
 * <p>The platform {@link HijrahDate} is the authoritative source: its
 * {@code islamic-umalqura} variant carries the Saudi month-length table and
 * handles the one-day month boundaries that a simple arithmetic calendar
 * cannot reproduce. The arithmetic fallback is kept only for the unlikely
 * case where the platform chronology rejects a date; it is not used on current
 * Android releases.
 *
 * <p>Month names are supplied by the caller from localized resources; this
 * class stays free of Android types and of any hardcoded language.
 */
public final class Hijri {

    private final int year;
    private final int month;
    private final int day;

    private Hijri(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static Hijri from(LocalDate date) {
        try {
            HijrahDate hijrah = HijrahDate.from(date);
            return new Hijri(
                    (int) hijrah.get(ChronoField.YEAR_OF_ERA),
                    (int) hijrah.get(ChronoField.MONTH_OF_YEAR),
                    (int) hijrah.get(ChronoField.DAY_OF_MONTH));
        } catch (RuntimeException e) {
            return tabular(date);
        }
    }

    /** Arithmetic fallback, used only if the platform Umm al-Qura table is absent. */
    private static Hijri tabular(LocalDate date) {
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

    /** e.g. "10 JUMADA AL-AWWAL 1448" or "10 جمادى الأولى 1448". */
    public String label(String[] months) {
        return day + " " + monthName(months) + " " + year;
    }

    /** e.g. "10 JUM I 1448" for the widget's narrow date line. */
    public String shortLabel(String[] months) {
        return day + " " + monthName(months) + " " + year;
    }

    private String monthName(String[] months) {
        if (months == null || months.length < 12) {
            return String.valueOf(month);
        }
        return months[Math.max(0, Math.min(11, month - 1))];
    }

    private static int julianDay(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }
}
