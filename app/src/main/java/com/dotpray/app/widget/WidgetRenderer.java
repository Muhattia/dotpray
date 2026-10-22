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
import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.prayer.PrayerCalculator;
import com.dotpray.app.prayer.PrayerClock;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Draws the home-screen widgets pixel by pixel.
 *
 * <p>RemoteViews cannot carry a custom {@code Typeface}: the launcher inflates
 * the layout with its own resources, so {@code android:fontFamily="@font/..."}
 * silently falls back to the system face. That left the widget in Roboto while
 * the app was in Doto. Rendering the whole card into a bitmap here means the
 * widget is typographically identical to the app, and it also removes the
 * Chronometer, whose base has to be in {@code SystemClock.elapsedRealtime()}
 * rather than wall-clock milliseconds (the 1.1 widget showed "IN 497916:07:08"
 * because of exactly that mismatch).
 */
public final class WidgetRenderer {

    private static final int ACCENT = 0xFFD71921;

    private WidgetRenderer() {
    }

    /** Renders the wide 4x2 card. */
    public static Bitmap wide(Context context, PrayerClock.Snapshot snapshot,
                              boolean use24, int widthPx, int heightPx) {
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
        Canvas c = ink.canvas;
        c.save();
        c.translate((ink.W - designW * scale) / 2f, (ink.H - designH * scale) / 2f);
        c.scale(scale, scale);

        float pad = ink.dp(16);
        float header = ink.dp(23);
        float divider = designH - ink.dp(42);
        float nameBase = divider - ink.dp(52);
        float metaBase = divider - ink.dp(30);
        float countBase = divider - ink.dp(14);
        float clockBase = divider - ink.dp(30);

        c.drawCircle(pad + ink.dp(3), ink.dp(19), ink.dp(3), ink.fill(ACCENT));
        ink.text("NEXT PRAYER", pad + ink.dp(13), header,
                ink.label(), ink.dim, 0.18f, Paint.Align.LEFT);

        String city = snapshot.place.name.toUpperCase(Locale.US);
        float cityRight = designW - pad;
        ink.text(ink.fit(city, ink.label(), cityRight - (pad + ink.dp(102)), 0.14f),
                cityRight, header, ink.label(), ink.faint, 0.14f, Paint.Align.RIGHT);

        ink.text(snapshot.next.prayer.label.toUpperCase(Locale.US), pad, nameBase,
                ink.display(22), ink.fg, 0.04f, Paint.Align.LEFT);
        ink.text(meta(snapshot), pad, metaBase, ink.label(), ink.faint, 0.04f,
                Paint.Align.LEFT);
        ink.text(countdown(snapshot), pad, countBase, ink.value(13), ink.fg, 0.06f,
                Paint.Align.LEFT);

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

        ink.progress(snapshot, pad, divider, designW - pad);

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
        return ink.bitmap;
    }

    /** Renders the compact 2x2 card. */
    public static Bitmap small(Context context, PrayerClock.Snapshot snapshot,
                               boolean use24, int widthPx, int heightPx) {
        Ink ink = new Ink(context, widthPx, heightPx);
        ink.card();
        ink.dotField();

        float designW = ink.dp(150);
        float designH = ink.dp(150);
        float scale = Math.min(ink.W / designW, ink.H / designH);
        Canvas c = ink.canvas;
        c.save();
        c.translate((ink.W - designW * scale) / 2f, (ink.H - designH * scale) / 2f);
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

        String city = snapshot.place.name.toUpperCase(Locale.US);
        ink.text(ink.fit(city, ink.label(), designW - pad * 2 - ink.dp(62), 0.14f),
                designW - pad, header, ink.label(), ink.faint, 0.14f, Paint.Align.RIGHT);

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

        ink.text(countdown(snapshot), pad, countBase, ink.value(11), ink.fg, 0.05f,
                Paint.Align.LEFT);
        ink.text(snapshot.nextIsTomorrow ? "TOMORROW" : "TODAY", pad, metaBase,
                ink.label(), ink.faint, 0.14f, Paint.Align.LEFT);

        c.restore();
        return ink.bitmap;
    }

    /** Short line used as the widget's accessibility description. */
    public static String describe(PrayerClock.Snapshot snapshot, boolean use24) {
        String time = use24 ? Format.time24(snapshot.next.local)
                : Format.time12(snapshot.next.local) + " " + Format.meridiem(snapshot.next.local);
        return snapshot.next.prayer.label + " at " + time + ", " + countdown(snapshot);
    }

    /** "IN 2H 14M" — always minute-accurate because the bitmap is redrawn each minute. */
    static String countdown(PrayerClock.Snapshot snapshot) {
        long ms = Math.max(0L, snapshot.next.instant.toEpochMilli()
                - Instant.now().toEpochMilli());
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
        return date + " \u00b7 " + (snapshot.nextIsTomorrow ? "TOMORROW" : "TODAY");
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

        void progress(PrayerClock.Snapshot snapshot, float left, float y, float right) {
            Paint track = fill(hairline);
            canvas.drawRect(left, y, right, y + Math.max(1f, dp(1)), track);

            long from = snapshot.previous.instant.toEpochMilli();
            long to = snapshot.next.instant.toEpochMilli();
            long now = Instant.now().toEpochMilli();
            float fraction = to <= from ? 0f
                    : Math.max(0f, Math.min(1f, (now - from) / (float) (to - from)));
            Paint done = fill(ACCENT);
            canvas.drawRect(left, y, left + (right - left) * fraction,
                    y + Math.max(2f, dp(2)), done);
        }

    }
}
