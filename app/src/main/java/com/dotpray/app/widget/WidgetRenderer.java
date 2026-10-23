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
import com.dotpray.app.prayer.Hijri;
import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.prayer.PrayerCalculator;
import com.dotpray.app.prayer.PrayerClock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Draws the home-screen widgets pixel by pixel.
 *
 * <p>RemoteViews cannot carry a custom {@code Typeface}: the launcher inflates
 * the layout with its own resources, so {@code android:fontFamily="@font/..."}
 * silently falls back to the system face. Rendering the card artwork into a
 * bitmap keeps the static type in Doto. For the moving seconds, the layout uses
 * the same Doto glyphs as white vector drawables inside a {@code ViewFlipper}.
 * The launcher flips those tiny views itself, so the seconds stay live without
 * a service or a per-second AlarmManager wake-up.
 */
public final class WidgetRenderer {

    private static final int ACCENT = 0xFFD71921;

    /** Doto's digits all use the same 600/1000 em advance. */
    private static final float CELL_ADVANCE = 0.600f;
    private static final float BOLD_GLYPH_TOP = 0.691f;
    private static final float BOLD_GLYPH_HEIGHT = 0.682f;
    private static final float REGULAR_GLYPH_TOP = 0.680f;
    private static final float REGULAR_GLYPH_HEIGHT = 0.660f;

    private WidgetRenderer() {
    }

    /** Pixel placement of one live two-digit seconds field. */
    public static final class Live {
        public final int left;
        public final int top;
        public final int width;
        public final int height;

        Live(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    /** A rendered card and the live fields whose pixels it left blank. */
    public static final class Card {
        public final Bitmap bitmap;
        public final Live nowSeconds;
        public final Live countdownSeconds;

        Card(Bitmap bitmap, Live nowSeconds, Live countdownSeconds) {
            this.bitmap = bitmap;
            this.nowSeconds = nowSeconds;
            this.countdownSeconds = countdownSeconds;
        }
    }

    /** Renders the wide 4x2 card. */
    public static Card wide(Context context, PrayerClock.Snapshot snapshot,
                            boolean use24, Instant now, int widthPx, int heightPx,
                            boolean liveSeconds) {
        Ink ink = new Ink(context, widthPx, heightPx);
        ink.card();
        ink.dotField();

        // Everything below is laid out once, in a 326 x 150 dp design space,
        // then scaled uniformly to whatever the launcher gave us. That keeps
        // the composition intact at every resize instead of letting lines
        // collide when a widget is squeezed.
        float designW = ink.dp(326);
        float designH = ink.dp(150);
        float scale = Math.min(ink.W / designW, ink.H / designH);
        float offsetX = (ink.W - designW * scale) / 2f;
        float offsetY = (ink.H - designH * scale) / 2f;
        Canvas c = ink.canvas;
        c.save();
        c.translate(offsetX, offsetY);
        c.scale(scale, scale);

        float pad = ink.dp(16);
        float header = ink.dp(23);
        float divider = designH - ink.dp(42);
        // With the Hijri date on the meta line, the meta line needs the full
        // width. Lift the big next-prayer time above it instead of letting the
        // two overlap on the right.
        float nameBase = divider - ink.dp(56);
        float clockBase = divider - ink.dp(34);
        float metaBase = divider - ink.dp(22);
        float countBase = divider - ink.dp(8);

        c.drawCircle(pad + ink.dp(3), ink.dp(19), ink.dp(3), ink.fill(ACCENT));
        ink.text("NEXT PRAYER", pad + ink.dp(13), header,
                ink.label(), ink.dim, 0.18f, Paint.Align.LEFT);

        String city = snapshot.place.name.toUpperCase(Locale.US);
        // Leave the top-right clear for the live wall clock.
        float cityRight = designW - pad - ink.dp(96);
        float cityLeft = pad + ink.dp(100);
        ink.text(ink.fit(city, ink.label(), Math.max(ink.dp(20), cityRight - cityLeft), 0.14f),
                cityRight, header, ink.label(), ink.faint, 0.14f, Paint.Align.RIGHT);

        Live nowSeconds = currentClock(ink, now, use24, designW - pad, header, 11f,
                liveSeconds, scale, offsetX, offsetY);

        ink.text(snapshot.next.prayer.label.toUpperCase(Locale.US), pad, nameBase,
                ink.display(22), ink.fg, 0.04f, Paint.Align.LEFT);
        ink.text(ink.fit(meta(snapshot), ink.label(9.5f), designW - pad * 2, 0.04f),
                pad, metaBase, ink.label(9.5f), ink.faint, 0.04f, Paint.Align.LEFT);
        Live countdownSeconds = countdownLine(ink, snapshot, now, pad, countBase, 13f,
                liveSeconds, scale, offsetX, offsetY);

        float clockSize = 46;
        String period = use24 ? "" : Format.meridiem(snapshot.next.local);
        String time = use24 ? Format.time24(snapshot.next.local)
                : Format.time12(snapshot.next.local);
        float timeRight = designW - pad;
        if (!period.isEmpty()) {
            Paint periodPaint = ink.label();
            float pw = ink.measure(period, periodPaint);
            ink.text(period, designW - pad, clockBase - ink.dp(9), periodPaint, ink.faint,
                    0.14f, Paint.Align.RIGHT);
            timeRight -= pw + ink.dp(6);
        }
        ink.text(time, timeRight, clockBase, ink.display(clockSize), ink.fg, -0.01f,
                Paint.Align.RIGHT);

        ink.progress(snapshot, now.toEpochMilli(), pad, divider, designW - pad);

        Prayer[] prayers = Prayer.values();
        float col = (designW - pad * 2) / 5f;
        for (int i = 0; i < prayers.length; i++) {
            float x = pad + col * i;
            boolean active = prayers[i] == snapshot.next.prayer;
            PrayerCalculator.Entry entry = snapshot.today.get(i);
            // After Isha the next prayer is tomorrow's Fajr. Keep the strip
            // useful by lighting its Fajr slot with that upcoming time; the
            // hero's meta line above says TOMORROW.
            if (active && snapshot.nextIsTomorrow) entry = snapshot.next;
            ink.text(prayers[i].label.toUpperCase(Locale.US), x, divider + ink.dp(16),
                    ink.label(9), active ? ink.fg : ink.faint, 0.08f, Paint.Align.LEFT);
            ink.text(use24 ? Format.time24(entry.local) : Format.time12(entry.local),
                    x, divider + ink.dp(33), ink.display(14), active ? ink.fg : ink.inactive,
                    0f, Paint.Align.LEFT);
        }

        c.restore();
        return new Card(ink.bitmap, nowSeconds, countdownSeconds);
    }

    /** Renders the compact 2x2 card. */
    public static Card small(Context context, PrayerClock.Snapshot snapshot,
                             boolean use24, Instant now, int widthPx, int heightPx,
                             boolean liveSeconds) {
        Ink ink = new Ink(context, widthPx, heightPx);
        ink.card();
        ink.dotField();

        float designW = ink.dp(150);
        float designH = ink.dp(150);
        float scale = Math.min(ink.W / designW, ink.H / designH);
        float offsetX = (ink.W - designW * scale) / 2f;
        float offsetY = (ink.H - designH * scale) / 2f;
        Canvas c = ink.canvas;
        c.save();
        c.translate(offsetX, offsetY);
        c.scale(scale, scale);

        float pad = ink.dp(14);
        float header = ink.dp(23);
        float metaBase = designH - ink.dp(14);
        float countBase = designH - ink.dp(32);
        float clockBase = designH - ink.dp(56);
        float nameBase = designH - ink.dp(100);

        c.drawCircle(pad + ink.dp(3), ink.dp(19), ink.dp(2.5f), ink.fill(ACCENT));
        ink.text("NEXT", pad + ink.dp(10), header, ink.label(), ink.dim, 0.18f,
                Paint.Align.LEFT);

        Live nowSeconds = currentClock(ink, now, use24, designW - ink.dp(11), header, 9f,
                liveSeconds, scale, offsetX, offsetY);

        ink.text(snapshot.next.prayer.label.toUpperCase(Locale.US), pad, nameBase,
                ink.display(19), ink.fg, 0.04f, Paint.Align.LEFT);

        String period = use24 ? "" : Format.meridiem(snapshot.next.local);
        String time = use24 ? Format.time24(snapshot.next.local)
                : Format.time12(snapshot.next.local);
        Paint clock = ink.display(36);
        ink.text(time, pad, clockBase, clock, ink.fg, -0.01f, Paint.Align.LEFT);
        if (!period.isEmpty()) {
            ink.text(period, pad + ink.measure(time, clock) + ink.dp(5),
                    clockBase - ink.dp(6), ink.label(), ink.faint, 0.14f, Paint.Align.LEFT);
        }

        Live countdownSeconds = countdownLine(ink, snapshot, now, pad, countBase, 11f,
                liveSeconds, scale, offsetX, offsetY);
        ink.text(ink.fit(smallMeta(snapshot), ink.label(8.5f), designW - pad * 2, 0.08f),
                pad, metaBase, ink.label(8.5f), ink.faint, 0.08f, Paint.Align.LEFT);

        c.restore();
        return new Card(ink.bitmap, nowSeconds, countdownSeconds);
    }

    /**
     * Draws the static half of the wall clock. With live seconds the bitmap
     * ends at the colon and the glyph-pair flipper covers both digits. Before
     * Android 12 the flippers cannot be positioned reliably, so the fallback
     * is an honest HH:MM clock rather than a stale seconds value.
     */
    private static Live currentClock(Ink ink, Instant now, boolean use24, float right,
                                     float baseline, float sizeSp, boolean live,
                                     float scale, float offsetX, float offsetY) {
        ZonedDateTime local = now.atZone(ZoneId.systemDefault());
        Paint paint = ink.display(sizeSp);
        String clock = use24 ? Format.time24(local) : Format.time12(local);
        String suffix = use24 ? "" : " " + Format.meridiem(local);

        if (!live) {
            ink.text(clock + suffix, right, baseline, paint, ink.fg, 0f,
                    Paint.Align.RIGHT);
            return null;
        }

        String prefix = clock + ":";
        float cell = ink.dp(CELL_ADVANCE * sizeSp);
        float suffixWidth = suffix.isEmpty() ? 0f : paint.measureText(suffix);
        float secondsLeft = right - suffixWidth - cell * 2f;
        ink.text(prefix, secondsLeft, baseline, paint, ink.fg, 0f, Paint.Align.RIGHT);
        ink.text(suffix, right, baseline, paint, ink.fg, 0f, Paint.Align.RIGHT);
        return live(ink, secondsLeft, baseline, sizeSp, true, scale, offsetX, offsetY);
    }

    /**
     * Draws the countdown line up to its final colon, leaving pixels for the
     * live seconds. Before Android 12 the original minute-resolution line is
     * used instead, because the layout cannot be positioned reliably there.
     */
    private static Live countdownLine(Ink ink, PrayerClock.Snapshot snapshot, Instant now,
                                      float left, float baseline, float sizeSp,
                                      boolean live, float scale, float offsetX,
                                      float offsetY) {
        long remaining = Math.max(0L, snapshot.next.instant.toEpochMilli()
                - now.toEpochMilli());
        Paint paint = ink.value(sizeSp);
        if (!live) {
            ink.text(countdown(snapshot, now.toEpochMilli()), left, baseline, paint,
                    ink.fg, 0.06f, Paint.Align.LEFT);
            return null;
        }

        String prefix = countdownPrefix(remaining);
        ink.text(prefix, left, baseline, paint, ink.fg, 0f, Paint.Align.LEFT);
        float secondsLeft = left + paint.measureText(prefix);
        return live(ink, secondsLeft, baseline, sizeSp, false, scale, offsetX, offsetY);
    }

    /**
     * Shared geometry for a two-digit live field. The vector drawables have
     * exactly the same advance and glyph height as the static Doto text, so
     * the overlay stitches onto the bitmap without a visible seam.
     */
    private static Live live(Ink ink, float left, float baseline, float sizeSp,
                             boolean bold, float scale, float offsetX, float offsetY) {
        // A pair vector is two adjacent 600-unit advances, so its logical
        // width is 1.2 em. The path viewports are 1200 x glyph height.
        float pairWidth = ink.dp(CELL_ADVANCE * 2f * sizeSp);
        float glyphTop = (bold ? BOLD_GLYPH_TOP : REGULAR_GLYPH_TOP) * sizeSp;
        float glyphHeight = (bold ? BOLD_GLYPH_HEIGHT : REGULAR_GLYPH_HEIGHT) * sizeSp;
        int leftPx = Math.round(offsetX + left * scale);
        int topPx = Math.round(offsetY + (baseline - ink.dp(glyphTop)) * scale);
        int widthPx = Math.max(1, Math.round(pairWidth * scale));
        int heightPx = Math.max(1, Math.round(ink.dp(glyphHeight) * scale));
        return new Live(leftPx, topPx, widthPx, heightPx);
    }

    private static String countdownPrefix(long remainingMs) {
        long total = Math.max(0L, remainingMs) / 1000L;
        long hours = total / 3600L;
        long minutes = (total % 3600L) / 60L;
        return hours > 0
                ? String.format(Locale.US, "IN %d:%02d:", hours, minutes)
                : String.format(Locale.US, "IN %d:", minutes);
    }

    /** Short line used as the widget's accessibility description. */
    public static String describe(PrayerClock.Snapshot snapshot, boolean use24) {
        String time = use24 ? Format.time24(snapshot.next.local)
                : Format.time12(snapshot.next.local) + " " + Format.meridiem(snapshot.next.local);
        return snapshot.next.prayer.label + " at " + time + ", " + countdown(snapshot);
    }

    /** Minute-accurate fallback used for accessibility and pre-Android 12. */
    static String countdown(PrayerClock.Snapshot snapshot) {
        return countdown(snapshot, Instant.now().toEpochMilli());
    }

    private static String countdown(PrayerClock.Snapshot snapshot, long nowMs) {
        long ms = Math.max(0L, snapshot.next.instant.toEpochMilli() - nowMs);
        long minutes = ms / 60000L;
        if (minutes >= 60) {
            return "IN " + (minutes / 60) + "H " + (minutes % 60) + "M";
        }
        if (minutes >= 1) {
            return "IN " + minutes + "M";
        }
        return "IN <1M";
    }

    private static String meta(PrayerClock.Snapshot snapshot) {
        String date = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)
                .format(snapshot.next.local).toUpperCase(Locale.US);
        String hijri = hijri(snapshot);
        String status = snapshot.nextIsTomorrow ? "TOMORROW" : "TODAY";
        return hijri.isEmpty()
                ? date + " \u00b7 " + status
                : date + " \u00b7 " + hijri + " \u00b7 " + status;
    }

    private static String smallMeta(PrayerClock.Snapshot snapshot) {
        String hijri = hijri(snapshot);
        String city = snapshot.place.name.toUpperCase(Locale.US);
        if (hijri.isEmpty()) return city;
        return city + " \u00b7 " + hijri;
    }

    private static String hijri(PrayerClock.Snapshot snapshot) {
        try {
            return Hijri.from(snapshot.next.local.toLocalDate()).shortLabel();
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Paint, canvas and unit helpers for one render pass. */
    private static final class Ink {
        final Canvas canvas;
        final Bitmap bitmap;
        final float W;
        final float H;
        private final float density;
        private final Resources res;
        private final Typeface dotoBold;
        private final Typeface dotoRegular;
        final int fg;
        final int dim;
        final int faint;
        final int inactive;
        final int hairline;
        final int dotColor;
        final int cardColor;

        Ink(Context context, int widthPx, int heightPx) {
            bitmap = Bitmap.createBitmap(Math.max(1, widthPx), Math.max(1, heightPx),
                    Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            W = bitmap.getWidth();
            H = bitmap.getHeight();
            res = context.getResources();
            density = res.getDisplayMetrics().density;
            dotoBold = face(R.font.doto_bold, Typeface.DEFAULT_BOLD);
            dotoRegular = face(R.font.doto_regular, Typeface.DEFAULT);
            boolean night = (res.getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (night) {
                fg = 0xFFF4F4F2;
                dim = 0xFF8E8E8E;
                faint = 0xFF565656;
                inactive = 0xFF565656;
                hairline = 0x26FFFFFF;
                dotColor = 0x14FFFFFF;
                cardColor = 0xF2000000;
            } else {
                fg = 0xFF0A0A0A;
                dim = 0xFF5E5E5E;
                faint = 0xFF8A8A8A;
                inactive = 0xFF9A9A9A;
                hairline = 0x33000000;
                dotColor = 0x12000000;
                cardColor = 0xF2FFFFFF;
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
            return text(dotoBold, 10);
        }

        Paint label(float sp) {
            return text(dotoBold, sp);
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
            Paint bg = fill(cardColor);
            canvas.drawRoundRect(new RectF(0, 0, W, H), dp(28), dp(28), bg);
            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1f, dp(1)));
            stroke.setColor(hairline);
            float inset = stroke.getStrokeWidth() / 2f;
            canvas.drawRoundRect(new RectF(inset, inset, W - inset, H - inset),
                    dp(28), dp(28), stroke);
        }

        void dotField() {
            Path clip = new Path();
            clip.addRoundRect(new RectF(0, 0, W, H), dp(28), dp(28), Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            Paint p = fill(dotColor);
            float step = dp(22);
            float r = dp(1);
            for (float y = step / 2f; y < H; y += step) {
                for (float x = step / 2f; x < W; x += step) {
                    canvas.drawCircle(x, y, r, p);
                }
            }
            canvas.restore();
        }

        void text(String s, float x, float baseline, Paint paint, int color,
                  float tracking, Paint.Align align) {
            if (s == null || s.isEmpty()) return;
            paint.setColor(color);
            paint.setTextAlign(align);
            paint.setLetterSpacing(tracking);
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

        void progress(PrayerClock.Snapshot snapshot, long nowMs, float left, float y,
                      float right) {
            Paint track = fill(hairline);
            canvas.drawRect(left, y, right, y + Math.max(1f, dp(1)), track);

            long from = snapshot.previous.instant.toEpochMilli();
            long to = snapshot.next.instant.toEpochMilli();
            float fraction = to <= from ? 0f
                    : Math.max(0f, Math.min(1f, (nowMs - from) / (float) (to - from)));
            Paint done = fill(ACCENT);
            canvas.drawRect(left, y, left + (right - left) * fraction,
                    y + Math.max(2f, dp(2)), done);
        }

    }
}
