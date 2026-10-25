package com.dotpray.app.widget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Per-instance widget appearance.
 *
 * <p>Widgets share the app's language and theme, but the card transparency is
 * chosen for each placement. Android gives every placement a stable
 * {@code appWidgetId}; storing the level under that id keeps two copies of the
 * same provider from changing each other. The file is separate from
 * {@code PrayerPrefs} so removing a widget only touches its own keys.
 */
public final class WidgetPrefs {

    private static final String FILE = "dotpray_widget_prefs";
    private static final String KEY_TRANSPARENCY = "transparency_";
    /** The binary Default/Transparent key used by the unreleased preview. */
    private static final String LEGACY_BACKGROUND = "background_";

    /** The card exactly as shipped: fill, border and dot field. */
    public static final int SOLID = 0;
    /** Only the type, times and indicators; the wallpaper shows through. */
    public static final int CLEAR = 100;

    private WidgetPrefs() {
    }

    /**
     * The saved transparency for one placement, from 0 (solid card) to 100
     * (clear). A missing value falls back to the sold card.
     */
    public static int getTransparency(Context context, int appWidgetId) {
        SharedPreferences prefs = prefs(context);
        String key = KEY_TRANSPARENCY + appWidgetId;
        if (prefs.contains(key)) {
            return clamp(prefs.getInt(key, SOLID));
        }
        // Keep the appearance of a widget configured by the previous review
        // build, whose choice was stored as a boolean-like flag.
        return prefs.getInt(LEGACY_BACKGROUND + appWidgetId, 0) == 1
                ? CLEAR : SOLID;
    }

    /** Stores the transparency for one placement without touching the others. */
    public static void setTransparency(Context context, int appWidgetId, int value) {
        if (appWidgetId < 0) return;
        SharedPreferences.Editor editor = prefs(context).edit()
                .putInt(KEY_TRANSPARENCY + appWidgetId, clamp(value));
        // Do not leave a stale binary choice that could win the fallback again.
        editor.remove(LEGACY_BACKGROUND + appWidgetId).apply();
    }

    /** Drops every key that belonged to a removed widget. */
    public static void delete(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return;
        SharedPreferences.Editor editor = prefs(context).edit();
        for (int id : appWidgetIds) {
            if (id < 0) continue;
            editor.remove(KEY_TRANSPARENCY + id);
            editor.remove(LEGACY_BACKGROUND + id);
        }
        editor.apply();
    }

    /**
     * Moves a placement's appearance to the id the launcher assigned after a
     * backup restore. Android allocates new widget ids on restore; without
     * this the restored widget would silently fall back to the solid card.
     *
     * <p>The two arrays are paired by index by the platform. A widget that
     * never had a stored level is left absent, so it keeps the same default
     * it had before.
     */
    public static void restore(Context context, int[] oldWidgetIds,
                               int[] newWidgetIds) {
        if (oldWidgetIds == null || newWidgetIds == null) return;
        int count = Math.min(oldWidgetIds.length, newWidgetIds.length);
        if (count == 0) return;

        SharedPreferences prefs = prefs(context);
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < count; i++) {
            int from = oldWidgetIds[i];
            int to = newWidgetIds[i];
            if (from < 0 || to < 0 || from == to) continue;

            String oldKey = KEY_TRANSPARENCY + from;
            if (prefs.contains(oldKey)) {
                editor.putInt(KEY_TRANSPARENCY + to,
                        clamp(prefs.getInt(oldKey, SOLID)));
            } else if (prefs.getInt(LEGACY_BACKGROUND + from, 0) == 1) {
                editor.putInt(KEY_TRANSPARENCY + to, CLEAR);
            }
            editor.remove(oldKey);
            editor.remove(LEGACY_BACKGROUND + from);
        }
        editor.apply();
    }

    static int clamp(int value) {
        return Math.max(SOLID, Math.min(CLEAR, value));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
