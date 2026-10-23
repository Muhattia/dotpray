package com.dotpray.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.dotpray.app.MainActivity;
import com.dotpray.app.R;
import com.dotpray.app.data.Place;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.data.ThemeController;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.PrayerClock;

import java.time.Instant;

/** Pushes the current prayer state into every placed widget. */
public final class WidgetUpdater {

    // One 60-child ViewFlipper per seconds field: 00..59. Keeping both digits
    // in the same flipper avoids a separate tens timer drifting out of phase
    // with the wall clock.
    private static final int[] NOW_SECONDS_IDS = {
            R.id.wNowSec0,
            R.id.wNowSec1,
            R.id.wNowSec2,
            R.id.wNowSec3,
            R.id.wNowSec4,
            R.id.wNowSec5,
            R.id.wNowSec6,
            R.id.wNowSec7,
            R.id.wNowSec8,
            R.id.wNowSec9,
            R.id.wNowSec10,
            R.id.wNowSec11,
            R.id.wNowSec12,
            R.id.wNowSec13,
            R.id.wNowSec14,
            R.id.wNowSec15,
            R.id.wNowSec16,
            R.id.wNowSec17,
            R.id.wNowSec18,
            R.id.wNowSec19,
            R.id.wNowSec20,
            R.id.wNowSec21,
            R.id.wNowSec22,
            R.id.wNowSec23,
            R.id.wNowSec24,
            R.id.wNowSec25,
            R.id.wNowSec26,
            R.id.wNowSec27,
            R.id.wNowSec28,
            R.id.wNowSec29,
            R.id.wNowSec30,
            R.id.wNowSec31,
            R.id.wNowSec32,
            R.id.wNowSec33,
            R.id.wNowSec34,
            R.id.wNowSec35,
            R.id.wNowSec36,
            R.id.wNowSec37,
            R.id.wNowSec38,
            R.id.wNowSec39,
            R.id.wNowSec40,
            R.id.wNowSec41,
            R.id.wNowSec42,
            R.id.wNowSec43,
            R.id.wNowSec44,
            R.id.wNowSec45,
            R.id.wNowSec46,
            R.id.wNowSec47,
            R.id.wNowSec48,
            R.id.wNowSec49,
            R.id.wNowSec50,
            R.id.wNowSec51,
            R.id.wNowSec52,
            R.id.wNowSec53,
            R.id.wNowSec54,
            R.id.wNowSec55,
            R.id.wNowSec56,
            R.id.wNowSec57,
            R.id.wNowSec58,
            R.id.wNowSec59
    };
    private static final int[] COUNTDOWN_SECONDS_IDS = {
            R.id.wCountdownSec0,
            R.id.wCountdownSec1,
            R.id.wCountdownSec2,
            R.id.wCountdownSec3,
            R.id.wCountdownSec4,
            R.id.wCountdownSec5,
            R.id.wCountdownSec6,
            R.id.wCountdownSec7,
            R.id.wCountdownSec8,
            R.id.wCountdownSec9,
            R.id.wCountdownSec10,
            R.id.wCountdownSec11,
            R.id.wCountdownSec12,
            R.id.wCountdownSec13,
            R.id.wCountdownSec14,
            R.id.wCountdownSec15,
            R.id.wCountdownSec16,
            R.id.wCountdownSec17,
            R.id.wCountdownSec18,
            R.id.wCountdownSec19,
            R.id.wCountdownSec20,
            R.id.wCountdownSec21,
            R.id.wCountdownSec22,
            R.id.wCountdownSec23,
            R.id.wCountdownSec24,
            R.id.wCountdownSec25,
            R.id.wCountdownSec26,
            R.id.wCountdownSec27,
            R.id.wCountdownSec28,
            R.id.wCountdownSec29,
            R.id.wCountdownSec30,
            R.id.wCountdownSec31,
            R.id.wCountdownSec32,
            R.id.wCountdownSec33,
            R.id.wCountdownSec34,
            R.id.wCountdownSec35,
            R.id.wCountdownSec36,
            R.id.wCountdownSec37,
            R.id.wCountdownSec38,
            R.id.wCountdownSec39,
            R.id.wCountdownSec40,
            R.id.wCountdownSec41,
            R.id.wCountdownSec42,
            R.id.wCountdownSec43,
            R.id.wCountdownSec44,
            R.id.wCountdownSec45,
            R.id.wCountdownSec46,
            R.id.wCountdownSec47,
            R.id.wCountdownSec48,
            R.id.wCountdownSec49,
            R.id.wCountdownSec50,
            R.id.wCountdownSec51,
            R.id.wCountdownSec52,
            R.id.wCountdownSec53,
            R.id.wCountdownSec54,
            R.id.wCountdownSec55,
            R.id.wCountdownSec56,
            R.id.wCountdownSec57,
            R.id.wCountdownSec58,
            R.id.wCountdownSec59
    };
    private static final int[] LIVE_FLIPPERS = {
            R.id.wNowSeconds, R.id.wCountdownSeconds
    };

    private WidgetUpdater() {
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateWide(context, manager, manager.getAppWidgetIds(
                new ComponentName(context, PrayerWidgetProvider.class)));
        updateSmall(context, manager, manager.getAppWidgetIds(
                new ComponentName(context, SmallPrayerWidgetProvider.class)));
        WidgetTick.schedule(context);
    }

    public static void updateWide(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        PrayerPrefs prefs = new PrayerPrefs(context);
        Place place = prefs.getPlace();
        Instant now = Instant.now();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(
                now, place, CalcMethod.EGYPT, prefs.getAsr());
        boolean use24 = prefs.is24Hour();
        Context themed = ThemeController.wrap(context);
        boolean liveSeconds = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

        for (int id : ids) {
            int[] px = pixels(context, manager, id);
            WidgetRenderer.Card card = WidgetRenderer.wide(themed, snapshot, use24, now,
                    px[0], px[1], liveSeconds);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_prayer);
            views.setImageViewBitmap(R.id.wCanvas, card.bitmap);
            bindLiveDigits(views, card.nowSeconds,
                    card.countdownSeconds, snapshot, now, liveSeconds,
                    px[2] / (float) px[0], px[3] / (float) px[1]);
            views.setContentDescription(R.id.wCanvas,
                    WidgetRenderer.describe(snapshot, use24));
            views.setOnClickPendingIntent(R.id.wRoot, openApp(context, 10));
            manager.updateAppWidget(id, views);
        }
    }

    public static void updateSmall(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        PrayerPrefs prefs = new PrayerPrefs(context);
        Place place = prefs.getPlace();
        Instant now = Instant.now();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(
                now, place, CalcMethod.EGYPT, prefs.getAsr());
        boolean use24 = prefs.is24Hour();
        Context themed = ThemeController.wrap(context);
        boolean liveSeconds = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

        for (int id : ids) {
            int[] px = pixels(context, manager, id);
            WidgetRenderer.Card card = WidgetRenderer.small(themed, snapshot, use24, now,
                    px[0], px[1], liveSeconds);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_prayer_small);
            views.setImageViewBitmap(R.id.wCanvas, card.bitmap);
            bindLiveDigits(views, card.nowSeconds,
                    card.countdownSeconds, snapshot, now, liveSeconds,
                    px[2] / (float) px[0], px[3] / (float) px[1]);
            views.setContentDescription(R.id.wCanvas,
                    WidgetRenderer.describe(snapshot, use24));
            views.setOnClickPendingIntent(R.id.wRoot, openApp(context, 12));
            manager.updateAppWidget(id, views);
        }
    }

    /**
     * Seeds and places the two live seconds flippers. The flippers advance in
     * launcher time, so this only sets the current child and positions the
     * fields over the blank pixels the renderer reserved in the bitmap.
     *
     * <p>The countdown layout is in reverse value order (59, 58, ... 00), so a
     * normal forward flip is a decrement and wraps from 00 back to 59.
     */
    private static void bindLiveDigits(RemoteViews views,
                                       WidgetRenderer.Live nowField,
                                       WidgetRenderer.Live countdownField,
                                       PrayerClock.Snapshot snapshot, Instant now,
                                       boolean live, float scaleX, float scaleY) {
        if (!live || (nowField == null && countdownField == null)) {
            for (int id : LIVE_FLIPPERS) {
                views.setViewVisibility(id, View.GONE);
            }
            return;
        }

        // The wall clock is deliberately quieter than the countdown, so each
        // flipper keeps the colour chosen by the renderer. The 2x2 card has no
        // wall clock, so either field may be absent.
        if (nowField != null) {
            tint(views, NOW_SECONDS_IDS, nowField.color);
            // Round to the nearest second rather than truncating. The flipper's
            // timer starts when the host attaches it, so seeding the nearest value
            // keeps the first second within half a tick of wall time.
            int millis = (int) (now.toEpochMilli() % 1000L);
            int second = now.atZone(snapshot.place.zone()).getSecond();
            if (millis >= 500) second = (second + 1) % 60;
            views.setDisplayedChild(R.id.wNowSeconds, second);
            place(views, R.id.wNowSeconds, nowField, scaleX, scaleY);
            views.setViewVisibility(R.id.wNowSeconds, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.wNowSeconds, View.GONE);
        }

        if (countdownField != null) {
            tint(views, COUNTDOWN_SECONDS_IDS, countdownField.color);
            long remainingMillis = Math.max(0L, snapshot.next.instant.toEpochMilli()
                    - now.toEpochMilli());
            int countdownSecond = (int) (((remainingMillis + 500L) / 1000L) % 60L);
            views.setDisplayedChild(R.id.wCountdownSeconds, 59 - countdownSecond);
            place(views, R.id.wCountdownSeconds, countdownField, scaleX, scaleY);
            views.setViewVisibility(R.id.wCountdownSeconds, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.wCountdownSeconds, View.GONE);
        }
    }

    private static void tint(RemoteViews views, int[] ids, int color) {
        for (int id : ids) {
            // White vector glyphs become the current foreground colour.
            views.setInt(id, "setColorFilter", color);
        }
    }

    /**
     * Places a live field. The renderer works in bitmap pixels; when the
     * widget bitmap was capped for memory, the host stretches it back to the
     * real widget size, so both the position and the cell size must scale too.
     */
    private static void place(RemoteViews views, int id, WidgetRenderer.Live field,
                              float scaleX, float scaleY) {
        views.setViewLayoutMargin(id, RemoteViews.MARGIN_LEFT,
                Math.round(field.left * scaleX), TypedValue.COMPLEX_UNIT_PX);
        views.setViewLayoutMargin(id, RemoteViews.MARGIN_TOP,
                Math.round(field.top * scaleY), TypedValue.COMPLEX_UNIT_PX);
        views.setViewLayoutWidth(id, Math.round(field.width * scaleX),
                TypedValue.COMPLEX_UNIT_PX);
        views.setViewLayoutHeight(id, Math.round(field.height * scaleY),
                TypedValue.COMPLEX_UNIT_PX);
    }

    /**
     * The widget's current size in pixels. Launchers report the size in dp via
     * the widget options; the provider info is the fallback for the first
     * update, before the options bundle exists.
     */
    private static int[] pixels(Context context, AppWidgetManager manager, int id) {
        Bundle options = manager.getAppWidgetOptions(id);
        int wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (hDp <= 0) hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);

        if (wDp <= 0 || hDp <= 0) {
            android.appwidget.AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
            if (info != null) {
                if (wDp <= 0) wDp = info.minWidth;
                if (hDp <= 0) hDp = info.minHeight;
            }
        }
        if (wDp <= 0) wDp = 280;
        if (hDp <= 0) hDp = 150;

        float density = context.getResources().getDisplayMetrics().density;
        float w = wDp * density;
        float h = hDp * density;

        // One bitmap has to fit through the widget pipe; cap the longest edge
        // while keeping the aspect ratio the launcher gave us.
        float scale = Math.min(1f, 1600f / Math.max(w, h));
        int actualW = Math.max(1, Math.round(w));
        int actualH = Math.max(1, Math.round(h));
        return new int[]{Math.max(1, Math.round(w * scale)),
                Math.max(1, Math.round(h * scale)), actualW, actualH};
    }

    private static PendingIntent openApp(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
