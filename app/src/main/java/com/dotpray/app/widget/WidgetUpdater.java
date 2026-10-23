package com.dotpray.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
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

/** Pushes the current prayer state into every placed widget. */
public final class WidgetUpdater {

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
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(
                java.time.Instant.now(), place, CalcMethod.EGYPT, prefs.getAsr());
        boolean use24 = prefs.is24Hour();
        Context themed = ThemeController.wrap(context);
        boolean liveCountdown = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

        for (int id : ids) {
            int[] px = pixels(context, manager, id);
            Bitmap card = WidgetRenderer.wide(themed, snapshot, use24, px[0], px[1],
                    liveCountdown);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_prayer);
            views.setImageViewBitmap(R.id.wCanvas, card);
            bindCurrentClock(context, themed, views, use24, px,
                    326f, 150f, 11f, 16f, 23f);
            bindCountdown(context, themed, views, snapshot, px, liveCountdown,
                    R.id.wCountdown, 326f, 150f, 16f, 100f, 13f);
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
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(
                java.time.Instant.now(), place, CalcMethod.EGYPT, prefs.getAsr());
        boolean use24 = prefs.is24Hour();
        Context themed = ThemeController.wrap(context);
        boolean liveCountdown = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

        for (int id : ids) {
            int[] px = pixels(context, manager, id);
            Bitmap card = WidgetRenderer.small(themed, snapshot, use24, px[0], px[1],
                    liveCountdown);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_prayer_small);
            views.setImageViewBitmap(R.id.wCanvas, card);
            bindCurrentClock(context, themed, views, use24, px,
                    150f, 150f, 9f, 11f, 23f);
            bindCountdown(context, themed, views, snapshot, px, liveCountdown,
                    R.id.wCountdown, 150f, 150f, 14f, 118f, 11f);
            views.setContentDescription(R.id.wCanvas,
                    WidgetRenderer.describe(snapshot, use24));
            views.setOnClickPendingIntent(R.id.wRoot, openApp(context, 12));
            manager.updateAppWidget(id, views);
        }
    }

    /**
     * Shows exactly one of the two TextClock views. Keeping one view per format
     * means the widget follows the app's own 12/24-hour setting instead of the
     * phone's system setting, and the clock keeps ticking in the launcher even
     * after the app process is gone.
     */
    private static void bindCurrentClock(Context context, Context themed, RemoteViews views,
                                         boolean use24, int[] px, float designWDp,
                                         float designHDp, float textSizeSp, float rightPadDp,
                                         float headerBaseDp) {
        views.setViewVisibility(R.id.wNow24, use24 ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.wNow12, use24 ? View.GONE : View.VISIBLE);
        int fg = themed.getColor(R.color.fg);
        views.setTextColor(R.id.wNow24, fg);
        views.setTextColor(R.id.wNow12, fg);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;

        float density = context.getResources().getDisplayMetrics().density;
        float designW = designWDp * density;
        float designH = designHDp * density;
        float scale = Math.min(px[0] / designW, px[1] / designH);
        float sizePx = Math.max(1f, textSizeSp * density * scale);
        views.setTextViewTextSize(R.id.wNow24, TypedValue.COMPLEX_UNIT_PX, sizePx);
        views.setTextViewTextSize(R.id.wNow12, TypedValue.COMPLEX_UNIT_PX, sizePx);

        float offsetX = (px[0] - designW * scale) / 2f;
        float offsetY = (px[1] - designH * scale) / 2f;
        int top = Math.round(offsetY + headerBaseDp * density * scale - sizePx * 0.82f);
        int right = Math.round(px[0] - offsetX
                - (designW - rightPadDp * density) * scale);
        for (int id : new int[]{R.id.wNow24, R.id.wNow12}) {
            views.setViewLayoutMargin(id, RemoteViews.MARGIN_TOP, top,
                    TypedValue.COMPLEX_UNIT_PX);
            views.setViewLayoutMargin(id, RemoteViews.MARGIN_RIGHT, right,
                    TypedValue.COMPLEX_UNIT_PX);
        }
    }

    /**
     * Drives the countdown with a Chronometer. Unlike the bitmap countdown this
     * is a real RemoteViews view, so the launcher updates the seconds itself.
     * On API < 31 there is no reliable way to position a child inside the
     * bitmap at arbitrary widget sizes; the renderer then falls back to the
     * minute-accurate static line.
     */
    private static void bindCountdown(Context context, Context themed, RemoteViews views,
                                      PrayerClock.Snapshot snapshot, int[] px,
                                      boolean live, int viewId, float designWDp,
                                      float designHDp, float padDp, float baselineDp,
                                      float textSizeSp) {
        if (!live) {
            views.setViewVisibility(viewId, View.GONE);
            return;
        }

        views.setViewVisibility(viewId, View.VISIBLE);
        long remainingMs = Math.max(0L, snapshot.next.instant.toEpochMilli()
                - System.currentTimeMillis());
        long base = SystemClock.elapsedRealtime() + remainingMs;
        views.setChronometerCountDown(viewId, true);
        views.setChronometer(viewId, base, "IN %s", true);
        views.setTextColor(viewId, themed.getColor(R.color.fg));

        float density = context.getResources().getDisplayMetrics().density;
        float designW = designWDp * density;
        float designH = designHDp * density;
        float scale = Math.min(px[0] / designW, px[1] / designH);
        float sizePx = Math.max(1f, textSizeSp * density * scale);
        views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_PX, sizePx);

        int left = Math.round((px[0] - designW * scale) / 2f
                + padDp * density * scale);
        int top = Math.round((px[1] - designH * scale) / 2f
                + baselineDp * density * scale - sizePx);
        views.setViewLayoutMargin(viewId, RemoteViews.MARGIN_LEFT, left,
                TypedValue.COMPLEX_UNIT_PX);
        views.setViewLayoutMargin(viewId, RemoteViews.MARGIN_TOP, top,
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
        return new int[]{Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale))};
    }

    private static PendingIntent openApp(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
