package com.dotpray.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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

        for (int id : ids) {
            int[] px = pixels(context, manager, id);
            Bitmap card = WidgetRenderer.wide(themed, snapshot, use24, px[0], px[1]);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_prayer);
            views.setImageViewBitmap(R.id.wCanvas, card);
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

        for (int id : ids) {
            int[] px = pixels(context, manager, id);
            Bitmap card = WidgetRenderer.small(themed, snapshot, use24, px[0], px[1]);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_prayer_small);
            views.setImageViewBitmap(R.id.wCanvas, card);
            views.setContentDescription(R.id.wCanvas,
                    WidgetRenderer.describe(snapshot, use24));
            views.setOnClickPendingIntent(R.id.wRoot, openApp(context, 12));
            manager.updateAppWidget(id, views);
        }
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
