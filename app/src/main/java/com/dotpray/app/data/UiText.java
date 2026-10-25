package com.dotpray.app.data;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;

import com.dotpray.app.R;
import com.dotpray.app.prayer.Hijri;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Locale;

/** Resource-aware display text. Kept out of the pure formatting helpers. */
public final class UiText {

    private UiText() {
    }

    public static String language(Context context) {
        return LocaleController.getLanguage(context);
    }

    public static boolean isArabic(Context context) {
        return Language.isArabic(language(context));
    }

    public static String upper(Context context, String text) {
        return Language.displayCase(language(context), text);
    }

    public static String date(Context context, LocalDate date) {
        Locale locale = Language.locale(language(context));
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(context.getString(R.string.date_pattern), locale)
                .withDecimalStyle(DecimalStyle.STANDARD);
        return Language.displayCase(language(context), formatter.format(date));
    }

    public static String hijri(Context context, LocalDate date, boolean shortLabel) {
        try {
            Hijri hijri = Hijri.from(date);
            String[] months = context.getResources().getStringArray(shortLabel
                    ? R.array.hijri_months_short : R.array.hijri_months);
            return shortLabel ? hijri.shortLabel(months) : hijri.label(months);
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Compact date line used by the widgets, e.g. THU 22 OCT. */
    public static String widgetDate(Context context, LocalDate date) {
        Locale locale = Language.locale(language(context));
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(context.getString(R.string.widget_date_pattern), locale)
                .withDecimalStyle(DecimalStyle.STANDARD);
        return Language.displayCase(language(context), formatter.format(date));
    }

    public static String meridiem(Context context, ZonedDateTime time) {
        return context.getString(time.getHour() < 12 ? R.string.am : R.string.pm);
    }

    /** Plain time used where a TextView supplies its own typeface. */
    public static String time(Context context, ZonedDateTime time, boolean use24) {
        if (use24) return Format.time24(time);
        return Format.time12(time) + " " + meridiem(context, time);
    }

    /**
     * Hero time: the digits stay in Doto while an Arabic meridiem is switched
     * to the Arabic face, so "ص" never falls back to tofu or a system font.
     */
    public static CharSequence timeText(Context context, ZonedDateTime time,
                                        boolean use24, float periodScale) {
        if (use24) return Format.time24(time);
        String base = Format.time12(time);
        String period = meridiem(context, time);
        return withPeriod(context, base, period, periodScale);
    }

    private static CharSequence withPeriod(Context context, String base, String period,
                                           float periodScale) {
        SpannableString text = new SpannableString(base + " " + period);
        int start = base.length() + 1;
        if (periodScale != 1f) {
            text.setSpan(new RelativeSizeSpan(periodScale), start, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (isArabic(context)) {
            Typeface face = context.getResources().getFont(R.font.alexandria_regular);
            text.setSpan(new FaceSpan(face), start, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return text;
    }

    public static String coordinate(Context context, double value, boolean latitude) {
        return Format.coord(value, latitude,
                context.getString(R.string.north),
                context.getString(R.string.south),
                context.getString(R.string.east),
                context.getString(R.string.west));
    }

    public static String coordinates(Context context, Place place) {
        return coordinate(context, place.latitude, true)
                + " / " + coordinate(context, place.longitude, false);
    }

    public static String progress(Context context, float fraction) {
        return format(context, R.string.progress_elapsed,
                Math.round(fraction * 100f));
    }

    /**
     * Formats resource copy with Latin digits. The app's number design is
     * Doto's Latin figures, so an Arabic system locale must not swap in
     * Arabic-Indic digits for a percentage or a duration.
     */
    public static String format(Context context, int id, Object... args) {
        return String.format(Locale.US, context.getString(id), args);
    }

    /** Plural lookup with the same Latin-digit rule as {@link #format}. */
    public static String quantity(Context context, int id, int quantity,
                                  Object... args) {
        String raw = context.getResources().getQuantityText(id, quantity).toString();
        return String.format(Locale.US, raw, args);
    }

    /** A Typeface span that works on API 26 without the API 28 constructor. */
    private static final class FaceSpan extends MetricAffectingSpan {
        private final Typeface face;

        FaceSpan(Typeface face) {
            this.face = face;
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            paint.setTypeface(face);
        }

        @Override
        public void updateDrawState(TextPaint paint) {
            paint.setTypeface(face);
        }
    }
}
