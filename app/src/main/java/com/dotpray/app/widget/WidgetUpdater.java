package com.dotpray.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "DotprayWidgetUpdater";

    private WidgetUpdater() {
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateWide(context, manager, manager.getAppWidgetIds(
                new ComponentName(context, PrayerWidgetProvider.class)));
        updateSmall(context, manager, manager.getAppWidgetIds(
                new ComponentName(context, SmallPrayerWidgetProvider.class)));
        updateMinimal(context, manager, manager.getAppWidgetIds(
                new ComponentName(context, MinimalPrayerWidgetProvider.class)));
        WidgetTick.schedule(context);
    }

    /**
     * Repaints a single placement after its appearance is changed.
     *
     * <p>The first configuration can run before the launcher has bound the
     * new id to its provider, so {@link AppWidgetManager#getAppWidgetInfo(int)}
     * may be null there. In that case the provider's own {@code onUpdate} is
     * the first draw, and this method leaves every existing placement alone
     * instead of refreshing unrelated widgets as a side effect.
     */
    public static void updateOne(Context context, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "updateOne: invalid appWidgetId=" + appWidgetId
                    + "; no widget was repainted");
            return;
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(appWidgetId);
        if (info == null) {
            // The first configuration intent can arrive before the launcher
            // binds the new id. The provider's own onUpdate will paint it; do
            // not repaint unrelated placements as a side effect.
            Log.i(TAG, "updateOne: appWidgetId=" + appWidgetId
                    + " is not bound yet; provider onUpdate will paint it");
            return;
        }
        ComponentName provider = info.provider;
        if (provider == null) {
            Log.w(TAG, "updateOne: appWidgetId=" + appWidgetId
                    + " has no provider; no widget was repainted");
            return;
        }
        String className = provider.getClassName();
        if (PrayerWidgetProvider.class.getName().equals(className)) {
            updateWide(context, manager, new int[]{appWidgetId});
            WidgetTick.schedule(context);
            return;
        }
        if (SmallPrayerWidgetProvider.class.getName().equals(className)) {
            updateSmall(context, manager, new int[]{appWidgetId});
            WidgetTick.schedule(context);
            return;
        }
        if (MinimalPrayerWidgetProvider.class.getName().equals(className)) {
            updateMinimal(context, manager, new int[]{appWidgetId});
            WidgetTick.schedule(context);
            return;
        }
        Log.w(TAG, "updateOne: appWidgetId=" + appWidgetId
                + " belongs to a different provider (" + className
                + "); no widget was repainted");
    }

    public static void updateWide(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        PrayerPrefs prefs = new PrayerPrefs(context);
        Place place = prefs.syncGpsZone();
        Instant now = Instant.now();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(
                now, place, CalcMethod.EGYPT, prefs.getAsr());
        boolean use24 = prefs.is24Hour();
        Context themed = ThemeController.wrap(context);

        for (int id : ids) {
            try {
                int[] px = pixels(context, manager, id);
                int transparency = WidgetPrefs.getTransparency(context, id);
                RemoteViews views = new RemoteViews(context.getPackageName(),
                        R.layout.widget_prayer);
                clearSurface(views);
                Bitmap bitmap = WidgetRenderer.wide(themed, snapshot, use24, now,
                        px[0], px[1], transparency);
                if (bitmap == null) {
                    Log.e(TAG, "updateWide: type=wide id=" + id
                            + " renderer returned null");
                    continue;
                }
                views.setImageViewBitmap(R.id.wCanvas, bitmap);
                // One accessible node owns both the description and the click;
                // the bitmap itself stays out of the screen-reader tree.
                views.setContentDescription(R.id.wRoot,
                        WidgetRenderer.describe(themed, snapshot, use24,
                                now.toEpochMilli()));
                views.setOnClickPendingIntent(R.id.wRoot, openApp(context, 10));
                manager.updateAppWidget(id, views);
                Log.d(TAG, "updateWide: type=wide id=" + id + " " + px[0] + "x"
                        + px[1] + " transparency=" + transparency + " ok");
            } catch (RuntimeException e) {
                Log.e(TAG, "updateWide: type=wide id=" + id + " failed", e);
            }
        }
    }

    public static void updateSmall(Context context, AppWidgetManager manager, int[] ids) {
        updateCompact(context, manager, ids, true);
    }

    public static void updateMinimal(Context context, AppWidgetManager manager,
                                     int[] ids) {
        updateCompact(context, manager, ids, false);
    }

    private static void updateCompact(Context context, AppWidgetManager manager,
                                      int[] ids, boolean rail) {
        if (ids == null || ids.length == 0) return;
        PrayerPrefs prefs = new PrayerPrefs(context);
        Place place = prefs.syncGpsZone();
        Instant now = Instant.now();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(
                now, place, CalcMethod.EGYPT, prefs.getAsr());
        boolean use24 = prefs.is24Hour();
        Context themed = ThemeController.wrap(context);

        String type = rail ? "small" : "minimal";
        for (int id : ids) {
            try {
                int[] px = pixels(context, manager, id);
                int transparency = WidgetPrefs.getTransparency(context, id);
                RemoteViews views = new RemoteViews(context.getPackageName(),
                        R.layout.widget_prayer_small);
                clearSurface(views);
                Bitmap bitmap = rail
                        ? WidgetRenderer.small(themed, snapshot, use24, now,
                        px[0], px[1], transparency)
                        : WidgetRenderer.minimal(themed, snapshot, use24, now,
                        px[0], px[1], transparency);
                if (bitmap == null) {
                    Log.e(TAG, "updateCompact: type=" + type + " id=" + id
                            + " renderer returned null");
                    continue;
                }
                views.setImageViewBitmap(R.id.wCanvas, bitmap);
                views.setContentDescription(R.id.wRoot,
                        WidgetRenderer.describe(themed, snapshot, use24,
                                now.toEpochMilli()));
                views.setOnClickPendingIntent(R.id.wRoot,
                        openApp(context, rail ? 12 : 13));
                manager.updateAppWidget(id, views);
                Log.d(TAG, "updateCompact: type=" + type + " id=" + id + " "
                        + px[0] + "x" + px[1] + " transparency=" + transparency
                        + " ok");
            } catch (RuntimeException e) {
                Log.e(TAG, "updateCompact: type=" + type + " id=" + id
                        + " failed", e);
            }
        }
    }

    /**
     * The live preview uses the placement's own cell size when one exists.
     *
     * <p>A brand-new placement may not have options yet. Its provider info
     * reports the minimum supported size, which would preview the short wide
     * fallback instead of the 4x2 default, so that case uses the variant's
     * design size instead.
     */
    public static int[] previewPixels(Context context, int appWidgetId,
                                      int fallbackWidthDp, int fallbackHeightDp) {
        Bundle options = AppWidgetManager.getInstance(context)
                .getAppWidgetOptions(appWidgetId);
        int wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
        if (wDp <= 0) {
            wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        }
        int hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (hDp <= 0) {
            hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        }
        if (wDp <= 0) wDp = fallbackWidthDp;
        if (hDp <= 0) hDp = fallbackHeightDp;
        return pixelsFromDp(context, wDp, hDp);
    }

    /**
     * The widget's current size in pixels. Launchers report the size in dp via
     * the widget options; the provider info is the fallback for the first
     * update, before the options bundle exists.
     */
    private static int[] pixels(Context context, AppWidgetManager manager, int id) {
        return pixels(context, manager, id, 326, 160);
    }

    private static int[] pixels(Context context, AppWidgetManager manager, int id,
                                int fallbackWidthDp, int fallbackHeightDp) {
        Bundle options = manager.getAppWidgetOptions(id);
        // Render against the upper bound so the launcher downscales the bitmap
        // instead of stretching a min-width paint across the cell. The old
        // min-width bitmap was upscaled by ~25% on a 4x2 placement, which made
        // every dot and glyph look soft.
        int wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
        if (wDp <= 0) {
            wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        }
        int hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (hDp <= 0) hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);

        if (wDp <= 0 || hDp <= 0) {
            AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
            if (info != null) {
                if (wDp <= 0) wDp = Math.max(info.minWidth, info.minResizeWidth);
                if (hDp <= 0) hDp = Math.max(info.minHeight, info.minResizeHeight);
            }
        }
        if (wDp <= 0) wDp = fallbackWidthDp;
        if (hDp <= 0) hDp = fallbackHeightDp;
        return pixelsFromDp(context, wDp, hDp);
    }

    private static int[] pixelsFromDp(Context context, int wDp, int hDp) {
        float density = context.getResources().getDisplayMetrics().density;
        // A small oversample covers launchers whose max bounds are a few dp
        // tighter than the laid-out host view. The host downscales, which is
        // sharp; the old min-width path upscaled, which was soft.
        float oversample = 1.12f;
        float w = wDp * density * oversample;
        float h = hDp * density * oversample;

        // One bitmap has to fit through the widget pipe; cap the longest edge
        // while keeping the aspect ratio the launcher gave us.
        float scale = Math.min(1f, 1600f / Math.max(w, h));
        return new int[]{Math.max(1, Math.round(w * scale)),
                Math.max(1, Math.round(h * scale))};
    }

    /**
     * The card is painted into {@code wCanvas}; the RemoteViews containers
     * must stay transparent at every level.
     *
     * <p>This is not redundant with the transparent colour in the layout XML.
     * When the launcher reuses an existing widget view it calls
     * {@code RemoteViews.reapply()}, which runs the update actions but does
     * not re-inflate the layout attributes. A background left on that view by
     * an older build therefore survives every later XML-only change. Resetting
     * both containers through RemoteViews actions runs on both fresh
     * inflation and recycling, so the bitmap alpha is the only thing the user
     * can see.
     */
    private static void clearSurface(RemoteViews views) {
        views.setInt(R.id.wRoot, "setBackgroundColor", Color.TRANSPARENT);
        views.setInt(R.id.wCanvas, "setBackgroundColor", Color.TRANSPARENT);
    }

    private static PendingIntent openApp(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
