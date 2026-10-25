package com.dotpray.app.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.dotpray.app.R;
import com.dotpray.app.data.Format;
import com.dotpray.app.data.Language;
import com.dotpray.app.data.LocaleController;
import com.dotpray.app.data.PlaceNames;
import com.dotpray.app.data.UiText;
import com.dotpray.app.prayer.ElapsedText;
import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.prayer.PrayerCalculator;
import com.dotpray.app.prayer.PrayerClock;
import com.dotpray.app.prayer.PrayerNames;
import com.dotpray.app.ui.ProgressDots;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Draws the home-screen widgets pixel by pixel.
 *
 * <p>RemoteViews cannot carry a custom {@code Typeface}: the launcher inflates
 * the layout with its own resources, so {@code android:fontFamily="@font/..."}
 * silently falls back to the system face. Rendering the card artwork into a
 * bitmap keeps the static type in Doto, and Alexandria for Arabic labels. The
 * card shows only minutes, so it is repainted on the minute boundary and needs
 * no live seconds views or per-second wake-ups.
 */
public final class WidgetRenderer {

    private static final int ACCENT = 0xFFD71921;
    private static final int ACCENT_TEXT_NIGHT = 0xFFF4333B;
    /** Quiet ink that still clears 4.5:1 on both card fills. */
    private static final int QUIET_LIGHT = 0xFF6B6B6B;
    private static final int QUIET_NIGHT = 0xFF7E7E7E;

    /** Alexandria runs optically larger than Doto at the same point size. */
    private static final float ARABIC_TEXT_SCALE = 0.8f;

    private WidgetRenderer() {
    }

    /**
     * Renders the wide 4x2 card.
     *
     * @param transparency 0 keeps the shipped card, 100 draws only the type,
     *                     times and indicators over the wallpaper
     */
    public static Bitmap wide(Context context, PrayerClock.Snapshot snapshot,
                              boolean use24, Instant now, int widthPx, int heightPx,
                              int transparency) {
        Ink ink = new Ink(context, widthPx, heightPx, transparency);
        if (transparency < WidgetPrefs.CLEAR) {
            ink.card();
            ink.dotField();
        }
        WidgetModel model = WidgetModel.of(snapshot, now.toEpochMilli());

        // The wide widget is composed in a 326 x 150 dp space. Resizing it
        // shorter drops the schedule and date lines instead of shrinking every
        // line into illegibility; the hero and its rail remain.
        float density = context.getResources().getDisplayMetrics().density;
        float widthDp = widthPx / density;
        float heightDp = heightPx / density;
        boolean full = WidgetLayout.wideIsFull(widthDp, heightDp);

        float designW = ink.dp(WidgetLayout.WIDE_WIDTH_DP);
        float designH = ink.dp(full ? WidgetLayout.WIDE_FULL_HEIGHT_DP
                : WidgetLayout.WIDE_SHORT_HEIGHT_DP);
        float scale = Math.min(ink.W / designW, ink.H / designH);
        float offsetX = (ink.W - designW * scale) / 2f;
        float offsetY = (ink.H - designH * scale) / 2f;
        Canvas c = ink.canvas;
        c.save();
        c.translate(offsetX, offsetY);
        c.scale(scale, scale);

        float pad = ink.dp(WidgetLayout.WIDE_PAD_DP);
        float header = ink.dp(full ? WidgetLayout.WIDE_HEADER_BASE_DP
                : WidgetLayout.WIDE_SHORT_HEADER_BASE_DP);
        float timeBase = ink.dp(full ? WidgetLayout.WIDE_TIME_BASE_DP
                : WidgetLayout.WIDE_SHORT_TIME_BASE_DP);
        float countBase = ink.dp(full ? WidgetLayout.WIDE_COUNT_BASE_DP
                : WidgetLayout.WIDE_SHORT_COUNT_BASE_DP);

        // Both languages use the same two-column composition. Arabic mirrors
        // it: the prayer column moves to the right, the location/clock column
        // to the left, and the schedule runs Fajr (right) to Isha (left).
        boolean rtl = ink.rtl;
        float nameBase = ink.dp(full ? WidgetLayout.WIDE_NAME_BASE_DP
                : WidgetLayout.WIDE_SHORT_NAME_BASE_DP);
        if (rtl) nameBase -= ink.dp(WidgetLayout.RTL_NAME_SHIFT_DP);
        float primaryEdge = rtl ? designW - pad : pad;
        float secondaryEdge = rtl ? pad : designW - pad;
        Paint.Align primaryAlign = rtl ? Paint.Align.RIGHT : Paint.Align.LEFT;
        Paint.Align secondaryAlign = rtl ? Paint.Align.LEFT : Paint.Align.RIGHT;

        float columnGap = ink.dp(18);
        float primaryWidth = ink.dp(WidgetLayout.WIDE_LEFT_COLUMN_DP);
        float secondaryWidth = designW - pad * 2f - primaryWidth - columnGap;

        // Centre the status dot on the label's ink, not its baseline.
        float dotX = rtl ? designW - pad - ink.dp(3) : pad + ink.dp(3);
        float headerX = rtl ? designW - pad - ink.dp(13) : pad + ink.dp(13);
        c.drawCircle(dotX, header - ink.dp(3.5f), ink.dp(3), ink.fill(ACCENT));

        String headerText = context.getString(model.grace
                ? R.string.prayer_now : R.string.next_prayer);
        ink.text(headerText, headerX, header, ink.label(), ink.dim,
                ink.track(0.18f), primaryAlign);

        // Place sits on the header, opposite NEXT PRAYER. The clock and
        // dates form one compact block centred in the hero, so the right
        // column has the same top-to-bottom balance as the left instead of
        // hanging a large clock from the top edge.
        String city = Language.displayCase(ink.language,
                PlaceNames.shortName(context, snapshot.place));
        Paint cityPaint = ink.label(9.5f);
        ink.text(ink.fit(city, cityPaint, secondaryWidth, ink.track(0.12f)),
                secondaryEdge, header, cityPaint, ink.faint, ink.track(0.12f),
                secondaryAlign);

        currentClock(context, ink, now, use24, secondaryEdge,
                ink.dp(full ? WidgetLayout.WIDE_CLOCK_BASE_DP
                        : WidgetLayout.WIDE_SHORT_NAME_BASE_DP),
                full ? WidgetLayout.WIDE_CLOCK_SIZE_SP
                        : WidgetLayout.WIDE_SHORT_CLOCK_SIZE_SP,
                snapshot.place.zone(), ink.dim, secondaryAlign);

        if (full) {
            Paint gregPaint = ink.label(8.5f);
            Paint hijriPaint = ink.label(8f);
            String date = UiText.widgetDate(context,
                    snapshot.next.local.toLocalDate());
            String hijri = UiText.hijri(context,
                    snapshot.next.local.toLocalDate(), true);
            ink.text(ink.fit(date, gregPaint, secondaryWidth, ink.track(0.08f)),
                    secondaryEdge, ink.dp(WidgetLayout.WIDE_DATE_BASE_DP),
                    gregPaint, ink.dim, ink.track(0.08f), secondaryAlign);
            ink.text(ink.fit(hijri, hijriPaint, secondaryWidth, ink.track(0.08f)),
                    secondaryEdge, ink.dp(WidgetLayout.WIDE_HIJRI_BASE_DP),
                    hijriPaint, ink.faint, ink.track(0.08f), secondaryAlign);
        }

        String prayerName = PrayerNames.display(context, model.prayer);
        float nameSize = full ? WidgetLayout.WIDE_NAME_SIZE_SP
                : WidgetLayout.WIDE_SHORT_NAME_SIZE_SP;
        Paint namePaint = ink.label(nameSize);
        ink.text(ink.fit(prayerName, namePaint, primaryWidth, ink.track(0.04f)),
                primaryEdge, nameBase, namePaint, ink.fg, ink.track(0.04f),
                primaryAlign);

        float timeSize = full ? WidgetLayout.WIDE_TIME_SIZE_SP
                : WidgetLayout.WIDE_SHORT_TIME_SIZE_SP;
        String period = use24 ? "" : UiText.meridiem(context, model.entry.local);
        String time = use24 ? Format.time24(model.entry.local)
                : Format.time12(model.entry.local);
        Paint clock = ink.display(timeSize);
        float timeWidth = ink.trackedWidth(time, clock, 0f);
        ink.text(time, primaryEdge, timeBase, clock, ink.fg, 0f, primaryAlign);
        if (!period.isEmpty()) {
            Paint periodPaint = ink.label(9f);
            float periodX = rtl
                    ? primaryEdge - timeWidth - ink.dp(5)
                    : primaryEdge + timeWidth + ink.dp(5);
            ink.text(period, periodX, timeBase - ink.dp(8), periodPaint, ink.faint,
                    ink.track(0.14f),
                    rtl ? Paint.Align.RIGHT : Paint.Align.LEFT);
        }

        // The countdown hangs directly below the next-prayer time. Its red
        // "IN" is the only strong accent in the hero. During the grace period
        // the countdown is replaced by the localized elapsed-time line.
        if (model.grace) {
            String message = ElapsedText.full(context, model.elapsedMinutes,
                    model.prayer);
            Paint messagePaint = ink.message(13f);
            float available = designW - pad * 2f;
            ink.text(ink.fit(message, messagePaint, available, 0f),
                    primaryEdge, countBase, messagePaint, ink.fg, 0f, primaryAlign);
        } else {
            countdownLine(context, ink, snapshot, now, primaryEdge, countBase,
                    full ? WidgetLayout.WIDE_COUNT_SIZE_SP
                            : WidgetLayout.WIDE_SHORT_COUNT_SIZE_SP,
                    primaryAlign, rtl);
        }

        if (full) {
            float divider = ink.dp(WidgetLayout.WIDE_RAIL_DP);
            // The divider doubles as the elapsed-time progress indicator: a row
            // of small dots advances with the minute update, so it never needs
            // a second-by-second widget wake-up.
            ink.progress(snapshot, now.toEpochMilli(), pad, divider, designW - pad);

            // The schedule is a five-column grid, not a run of justified
            // blocks. Equal cells give every prayer the same visual weight and
            // keep the strip symmetric in both directions; centring each
            // label and time inside its cell avoids an empty tail after Isha.
            Prayer[] prayers = Prayer.values();
            Paint scheduleName = ink.label(10);
            Paint timePaint = ink.display(15);
            String[] names = new String[prayers.length];
            String[] times = new String[prayers.length];
            boolean[] activeFlags = new boolean[prayers.length];
            PrayerCalculator.Entry activeEntry = model.entry;
            for (int i = 0; i < prayers.length; i++) {
                PrayerCalculator.Entry entry = snapshot.today.get(i);
                // Compare instants rather than names/dates: at high latitudes
                // an Isha from the previous calculation day can fall after
                // midnight and must not light today's Isha slot.
                boolean active = activeEntry.instant.equals(entry.instant)
                        || (model.displayIsTomorrow
                        && prayers[i] == activeEntry.prayer);
                activeFlags[i] = active;
                // After Isha the next prayer is tomorrow's Fajr. Keep the
                // strip useful by lighting its Fajr slot with that time.
                if (active && model.displayIsTomorrow) entry = activeEntry;
                names[i] = PrayerNames.display(context, prayers[i]);
                times[i] = use24 ? Format.time24(entry.local)
                        : Format.time12(entry.local);
            }
            float cellWidth = (designW - pad * 2f) / prayers.length;
            for (int i = 0; i < prayers.length; i++) {
                float cellCenter = rtl
                        ? designW - pad - cellWidth * (i + 0.5f)
                        : pad + cellWidth * (i + 0.5f);
                ink.text(names[i], cellCenter,
                        ink.dp(WidgetLayout.WIDE_SCHEDULE_NAME_BASE_DP),
                        scheduleName, activeFlags[i] ? ink.accentText : ink.faint,
                        ink.track(0.08f), Paint.Align.CENTER);
                ink.text(times[i], cellCenter,
                        ink.dp(WidgetLayout.WIDE_SCHEDULE_TIME_BASE_DP),
                        timePaint, activeFlags[i] ? ink.fg : ink.inactive, 0f,
                        Paint.Align.CENTER);
            }
        } else {
            // Keep the rail in the short resize even though the schedule and
            // date lines have been dropped.
            ink.progress(snapshot, now.toEpochMilli(), pad,
                    ink.dp(WidgetLayout.WIDE_SHORT_RAIL_DP), designW - pad);
        }

        c.restore();
        return ink.bitmap;
    }

    /** Renders the compact card with the dotted progress rail. */
    public static Bitmap small(Context context, PrayerClock.Snapshot snapshot,
                               boolean use24, Instant now, int widthPx, int heightPx,
                               int transparency) {
        return compact(context, snapshot, use24, now, widthPx, heightPx, true,
                transparency);
    }

    /** Renders the compact card without the progress rail. */
    public static Bitmap minimal(Context context, PrayerClock.Snapshot snapshot,
                                 boolean use24, Instant now, int widthPx,
                                 int heightPx, int transparency) {
        return compact(context, snapshot, use24, now, widthPx, heightPx, false,
                transparency);
    }

    private static Bitmap compact(Context context, PrayerClock.Snapshot snapshot,
                                  boolean use24, Instant now, int widthPx,
                                  int heightPx, boolean rail, int transparency) {
        Ink ink = new Ink(context, widthPx, heightPx, transparency);
        if (transparency < WidgetPrefs.CLEAR) {
            ink.card();
            ink.dotField();
        }
        WidgetModel model = WidgetModel.of(snapshot, now.toEpochMilli());

        float designW = ink.dp(WidgetLayout.SMALL_SIZE_DP);
        float designH = ink.dp(WidgetLayout.SMALL_SIZE_DP);
        float scale = Math.min(ink.W / designW, ink.H / designH);
        float offsetX = (ink.W - designW * scale) / 2f;
        float offsetY = (ink.H - designH * scale) / 2f;
        Canvas c = ink.canvas;
        c.save();
        c.translate(offsetX, offsetY);
        c.scale(scale, scale);

        // The compact card is one centred stack. During a grace the stack moves up
        // a little to make room for the two elapsed lines above the rail.
        float centerX = designW / 2f;
        float nameBase = ink.dp(WidgetLayout.smallNameBaseDp(model.grace));
        if (ink.rtl) nameBase -= ink.dp(WidgetLayout.RTL_NAME_SHIFT_DP);
        float timeBase = ink.dp(WidgetLayout.smallTimeBaseDp(model.grace));
        float countBase = ink.dp(WidgetLayout.SMALL_COUNT_BASE_DP);
        float progressY = ink.dp(WidgetLayout.smallRailDp(model.grace));

        String prayerName = PrayerNames.display(context, model.prayer);
        Paint namePaint = ink.label(WidgetLayout.SMALL_NAME_SIZE_SP);
        ink.text(ink.fit(prayerName, namePaint, designW - ink.dp(32), ink.track(0.04f)),
                centerX, nameBase, namePaint, ink.fg, ink.track(0.04f),
                Paint.Align.CENTER);

        String period = use24 ? "" : UiText.meridiem(context, model.entry.local);
        String time = use24 ? Format.time24(model.entry.local)
                : Format.time12(model.entry.local);
        Paint clock = ink.display(WidgetLayout.SMALL_TIME_SIZE_SP);
        float timeWidth = ink.trackedWidth(time, clock, 0f);
        Paint periodPaint = ink.label(9f);
        float periodWidth = period.isEmpty() ? 0f
                : ink.trackedWidth(period, periodPaint, ink.track(0.14f));
        float periodGap = period.isEmpty() ? 0f : ink.dp(4);
        float totalWidth = timeWidth + periodGap + periodWidth;
        float timeLeft = centerX - totalWidth / 2f;
        if (ink.rtl) {
            // The Arabic meridiem precedes the digits in reading order, so it
            // sits to their left in the centred block.
            if (!period.isEmpty()) {
                ink.text(period, timeLeft, timeBase - ink.dp(8), periodPaint,
                        ink.faint, ink.track(0.14f), Paint.Align.LEFT);
            }
            ink.text(time, timeLeft + periodWidth + periodGap, timeBase, clock,
                    ink.fg, 0f, Paint.Align.LEFT);
        } else {
            ink.text(time, timeLeft, timeBase, clock, ink.fg, 0f,
                    Paint.Align.LEFT);
            if (!period.isEmpty()) {
                ink.text(period, timeLeft + timeWidth + periodGap,
                        timeBase - ink.dp(8), periodPaint, ink.faint,
                        ink.track(0.14f), Paint.Align.LEFT);
            }
        }

        if (model.grace) {
            Paint first = ink.message(WidgetLayout.SMALL_GRACE_LINE1_SIZE_SP);
            Paint second = ink.message(WidgetLayout.SMALL_GRACE_LINE2_SIZE_SP);
            float available = designW - ink.dp(32);
            String line1 = ElapsedText.compact(context, model.elapsedMinutes);
            String line2 = ElapsedText.on(context, model.prayer);
            ink.text(ink.fit(line1, first, available, 0f), centerX,
                    ink.dp(WidgetLayout.SMALL_GRACE_LINE1_DP), first, ink.fg, 0f,
                    Paint.Align.CENTER);
            ink.text(ink.fit(line2, second, available, 0f), centerX,
                    ink.dp(WidgetLayout.SMALL_GRACE_LINE2_DP), second, ink.dim, 0f,
                    Paint.Align.CENTER);
        } else {
            countdownLine(context, ink, snapshot, now, centerX, countBase,
                    WidgetLayout.SMALL_COUNT_SIZE_SP, Paint.Align.CENTER,
                    ink.rtl);
        }

        // The small card uses the same elapsed rail as the wide one: 100%
        // during a grace, then the wait from the grace boundary to the next
        // Adhan. The minimal card deliberately omits it.
        if (rail) {
            ink.progress(snapshot, now.toEpochMilli(), ink.dp(16), progressY,
                    designW - ink.dp(16));
        }

        c.restore();
        return ink.bitmap;
    }

    /** Draws the minute-resolution wall clock in the secondary column. */
    private static void currentClock(Context context, Ink ink, Instant now,
                                     boolean use24, float edge, float baseline,
                                     float sizeSp, ZoneId zone, int color,
                                     Paint.Align align) {
        ZonedDateTime local = now.atZone(zone);
        Paint paint = ink.display(sizeSp);
        Paint periodPaint = ink.label(Math.max(8f, sizeSp * 0.38f));
        String clock = use24 ? Format.time24(local) : Format.time12(local);
        String period = use24 ? "" : UiText.meridiem(context, local);

        float clockWidth = ink.measure(clock, paint);
        float periodGap = period.isEmpty() ? 0f : ink.dp(4);
        float periodWidth = period.isEmpty() ? 0f
                : ink.trackedWidth(period, periodPaint, ink.track(0.14f));
        float total = clockWidth + periodGap + periodWidth;
        float left = align == Paint.Align.RIGHT ? edge - total
                : align == Paint.Align.CENTER ? edge - total / 2f : edge;

        if (ink.rtl) {
            // Arabic places the meridiem before the digits in reading order,
            // so it is drawn to their left. The digits stay LTR.
            if (!period.isEmpty()) {
                ink.text(period, left, baseline - ink.dp(sizeSp * 0.12f),
                        periodPaint, color, ink.track(0.14f), Paint.Align.LEFT);
            }
            ink.text(clock, left + periodWidth + periodGap, baseline, paint, color,
                    0f, Paint.Align.LEFT);
            return;
        }

        ink.text(clock, left, baseline, paint, color, 0f, Paint.Align.LEFT);
        if (!period.isEmpty()) {
            ink.text(period, left + clockWidth + periodGap,
                    baseline - ink.dp(sizeSp * 0.12f), periodPaint, color,
                    ink.track(0.14f), Paint.Align.LEFT);
        }
    }

    /** Draws the minute-resolution countdown line. */
    private static void countdownLine(Context context, Ink ink,
                                      PrayerClock.Snapshot snapshot, Instant now,
                                      float edge, float baseline, float sizeSp,
                                      Paint.Align align, boolean rtl) {
        long remaining = Math.max(0L, snapshot.next.instant.toEpochMilli()
                - now.toEpochMilli());
        long total = remaining / 60_000L;
        long hours = total / 60L;
        long minutes = total % 60L;

        Paint value = ink.value(sizeSp);
        Paint label = ink.label(Math.max(9f, sizeSp * 0.7f));
        String labelText = context.getString(R.string.in_label);
        String clock = String.format(Locale.US, "%02d:%02d", hours, minutes);
        float labelWidth = ink.trackedWidth(labelText, label, ink.track(0.12f));
        float gap = ink.dp(5f);
        float valueWidth = ink.measure(clock, value);
        float totalWidth = labelWidth + gap + valueWidth;
        float left;
        if (align == Paint.Align.RIGHT) left = edge - totalWidth;
        else if (align == Paint.Align.CENTER) left = edge - totalWidth / 2f;
        else left = edge;

        if (rtl) {
            // Reading right-to-left, "بعد" comes first and the digits sit to
            // its left. The digits themselves keep their LTR order.
            ink.text(clock, left, baseline, value, ink.fg, 0f,
                    Paint.Align.LEFT);
            ink.text(labelText, left + totalWidth, baseline, label, ink.accentText,
                    ink.track(0.12f), Paint.Align.RIGHT);
            return;
        }

        ink.text(labelText, left, baseline, label, ink.accentText, ink.track(0.12f),
                Paint.Align.LEFT);
        ink.text(clock, left + labelWidth + gap, baseline, value, ink.fg, 0f,
                Paint.Align.LEFT);
    }

    /** Short line used as the widget's accessibility description. */
    public static String describe(Context context, PrayerClock.Snapshot snapshot,
                                  boolean use24, long nowMs) {
        WidgetModel model = WidgetModel.of(snapshot, nowMs);
        String name = PrayerNames.display(context, model.prayer);
        if (model.grace) {
            if (model.elapsedMinutes <= 0) {
                return context.getString(R.string.a11y_grace_just_now, name);
            }
            return context.getString(R.string.a11y_grace, name,
                    ElapsedText.compact(context, model.elapsedMinutes));
        }
        String time = UiText.time(context, model.entry.local, use24);
        return context.getString(R.string.a11y_next, name, time,
                countdown(context, snapshot, nowMs));
    }

    /** Minute-accurate fallback used for accessibility and pre-Android 12. */
    private static String countdown(Context context, PrayerClock.Snapshot snapshot,
                                    long nowMs) {
        long ms = Math.max(0L, snapshot.next.instant.toEpochMilli() - nowMs);
        long minutes = ms / 60000L;
        if (minutes >= 60) {
            return UiText.format(context, R.string.duration_hours_minutes,
                    (int) (minutes / 60), (int) (minutes % 60));
        }
        if (minutes >= 1) {
            return UiText.format(context, R.string.duration_minutes,
                    (int) minutes);
        }
        return context.getString(R.string.duration_less_minute);
    }

    /** Paint, canvas and unit helpers for one render pass. */
    private static final class Ink {
        final Canvas canvas;
        final Bitmap bitmap;
        final float W;
        final float H;
        final String language;
        final boolean rtl;
        private final float density;
        private final Resources res;
        private final Typeface dotoBold;
        private final Typeface dotoRegular;
        private final Typeface labelFace;
        private final Typeface messageFace;
        final int fg;
        final int dim;
        final int faint;
        final int inactive;
        final int accentText;
        final int hairline;
        final int dotColor;
        final int cardColor;
        final int railQuiet;
        final int textOutline;
        final int transparency;

        Ink(Context context, int widthPx, int heightPx, int transparency) {
            bitmap = Bitmap.createBitmap(Math.max(1, widthPx), Math.max(1, heightPx),
                    Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            W = bitmap.getWidth();
            H = bitmap.getHeight();
            this.transparency = WidgetPrefs.clamp(transparency);
            res = context.getResources();
            density = res.getDisplayMetrics().density;
            language = LocaleController.getLanguage(context);
            rtl = Language.isRtl(language);
            dotoBold = face(R.font.doto_bold, Typeface.DEFAULT_BOLD);
            dotoRegular = face(R.font.doto_regular, Typeface.DEFAULT);
            Typeface arabicRegular = face(R.font.alexandria_regular, Typeface.DEFAULT);
            Typeface arabicSemibold = face(R.font.alexandria_semibold,
                    Typeface.DEFAULT_BOLD);
            labelFace = rtl ? arabicSemibold : dotoBold;
            messageFace = rtl ? arabicRegular : dotoRegular;
            boolean night = (res.getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            // The last stretch of the slider leaves the card behind. Dark ink
            // then disappears into a dark wallpaper, and a white outline alone
            // looks faded. Use the same wallpaper-safe ink as the night theme
            // once the card is mostly gone, while the card layers keep the
            // selected theme's fill for the levels that still draw them.
            boolean wallpaperInk = night || transparency >= 80;
            if (wallpaperInk) {
                fg = 0xFFF4F4F2;
                dim = 0xFF8E8E8E;
                faint = QUIET_NIGHT;
                inactive = QUIET_NIGHT;
                accentText = ACCENT_TEXT_NIGHT;
                textOutline = 0xFF000000;
            } else {
                fg = 0xFF0A0A0A;
                dim = 0xFF5E5E5E;
                faint = QUIET_LIGHT;
                inactive = QUIET_LIGHT;
                accentText = ACCENT;
                textOutline = 0xFFFFFFFF;
            }
            if (night) {
                hairline = 0x26FFFFFF;
                dotColor = 0x14FFFFFF;
                cardColor = 0xF2000000;
                // The card's quiet rail is calibrated against its own dark
                // fill. On a wallpaper it needs a little more presence.
                railQuiet = 0x66FFFFFF;
            } else {
                hairline = 0x33000000;
                dotColor = 0x12000000;
                cardColor = 0xF2FFFFFF;
                // Match the wallpaper ink once the light card has mostly
                // cleared; black quiet dots would vanish on a dark wallpaper.
                railQuiet = wallpaperInk ? 0x66FFFFFF : 0x66000000;
            }
        }

        private Typeface face(int id, Typeface fallback) {
            try {
                return res.getFont(id);
            } catch (RuntimeException e) {
                return fallback;
            }
        }

        float dp(float v) {
            return v * density;
        }

        /** Latin labels keep their tracking; Arabic must never be spaced out. */
        float track(float latin) {
            return rtl ? 0f : latin;
        }

        Paint fill(int color) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            p.setStyle(Paint.Style.FILL);
            return p;
        }

        Paint display(float sp) {
            return text(dotoBold, sp);
        }

        Paint value(float sp) {
            return text(dotoRegular, sp);
        }

        Paint label() {
            return label(10f);
        }

        Paint label(float sp) {
            return text(labelFace, rtl ? sp * ARABIC_TEXT_SCALE : sp);
        }

        Paint message(float sp) {
            return text(messageFace, rtl ? sp * ARABIC_TEXT_SCALE : sp);
        }

        private Paint text(Typeface face, float sp) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            p.setTypeface(face);
            // density, not scaledDensity: the card is a fixed composition, so a
            // large system font scale must not push the lines into each other.
            p.setTextSize(sp * density);
            p.setColor(fg);
            return p;
        }

        void card() {
            Paint bg = fill(fade(cardColor));
            canvas.drawRoundRect(new RectF(0, 0, W, H), dp(28), dp(28), bg);
            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1f, dp(1)));
            stroke.setColor(fade(hairline));
            float inset = stroke.getStrokeWidth() / 2f;
            canvas.drawRoundRect(new RectF(inset, inset, W - inset, H - inset),
                    dp(28), dp(28), stroke);
        }

        void dotField() {
            Path clip = new Path();
            clip.addRoundRect(new RectF(0, 0, W, H), dp(28), dp(28), Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            Paint p = fill(fade(dotColor));
            float step = dp(22);
            float r = dp(1);
            for (float y = step / 2f; y < H; y += step) {
                for (float x = step / 2f; x < W; x += step) {
                    canvas.drawCircle(x, y, r, p);
                }
            }
            canvas.restore();
        }

        /** Scales a layer's own alpha by the saved card opacity. */
        private int fade(int color) {
            int base = alpha(color);
            return withAlpha(color, Math.round(base
                    * (1f - transparency / 100f)));
        }

        /**
         * The rail's unfinished dots sit between the card's hairline and the
         * stronger wallpaper-safe quiet ink as transparency rises.
         */
        private int railColor() {
            float level = transparency / 100f;
            int base = alpha(hairline);
            int clear = alpha(railQuiet);
            return withAlpha(hairline, Math.round(base + (clear - base) * level));
        }

        private static int alpha(int color) {
            return (color >>> 24) & 0xFF;
        }

        private static int withAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
        }

        void text(String s, float x, float baseline, Paint paint, int color,
                  float tracking, Paint.Align align) {
            if (s == null || s.isEmpty()) return;
            paint.setTextAlign(align);
            paint.setLetterSpacing(tracking);
            if (transparency > 0) {
                // A crisp outline replaces the old blurred halo. It keeps the
                // type readable over a wallpaper without softening the glyphs.
                int outline = withAlpha(textOutline,
                        Math.round(0xFF * (transparency / 100f)));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1f, dp(0.55f)));
                paint.setColor(outline);
                canvas.drawText(s, x, baseline, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawText(s, x, baseline, paint);
        }

        float measure(String s, Paint paint) {
            return paint.measureText(s);
        }

        String fit(String s, Paint paint, float maxWidth, float tracking) {
            if (trackedWidth(s, paint, tracking) <= maxWidth) return s;
            String ellipsis = "\u2026";
            for (int end = s.length() - 1; end > 0; end--) {
                String candidate = s.substring(0, end).trim() + ellipsis;
                if (trackedWidth(candidate, paint, tracking) <= maxWidth) return candidate;
            }
            return ellipsis;
        }

        /**
         * Android applies letter spacing on top of the glyph advances, so a
         * plain measureText() under-reports. Without this, a long city name
         * "fits" while being measured and then overlaps its neighbour when it
         * is drawn.
         */
        float trackedWidth(String s, Paint paint, float tracking) {
            return paint.measureText(s)
                    + tracking * paint.getTextSize() * Math.max(0, s.length() - 1);
        }

        void progress(PrayerClock.Snapshot snapshot, long nowMs,
                      float left, float y, float right) {
            float radius = dp(ProgressDots.RADIUS_DP);
            float pitch = dp(ProgressDots.PITCH_DP);
            float start = left + radius;
            float end = right - radius;
            if (end <= start) return;
            float cy = y + radius;
            int count = ProgressDots.count(end - start, radius, pitch);
            int completed = ProgressDots.completed(count, snapshot.progress(nowMs));
            Paint quiet = fill(railColor());
            Paint lit = fill(ACCENT);
            for (int i = 0; i < count; i++) {
                float cx = ProgressDots.center(start, end, radius, count, i);
                canvas.drawCircle(cx, cy, radius,
                        ProgressDots.isComplete(i, count, completed, rtl) ? lit : quiet);
            }
        }
    }
}
