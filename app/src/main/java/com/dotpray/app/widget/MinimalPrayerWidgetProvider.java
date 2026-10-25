package com.dotpray.app.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

/**
 * Compact 2x2 variant showing the next prayer and countdown without the
 * dotted elapsed rail.
 */
public class MinimalPrayerWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        WidgetUpdater.updateMinimal(context, manager, ids);
        WidgetTick.schedule(context);
        WidgetTick.scheduleSoon(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int id, Bundle options) {
        WidgetUpdater.updateMinimal(context, manager, new int[]{id});
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        WidgetPrefs.delete(context, appWidgetIds);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds,
                           int[] newWidgetIds) {
        WidgetPrefs.restore(context, oldWidgetIds, newWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        if (!WidgetTick.hasWidgets(context)) WidgetTick.cancel(context);
    }
}
