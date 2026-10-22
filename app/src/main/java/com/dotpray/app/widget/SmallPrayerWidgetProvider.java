package com.dotpray.app.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

/** Compact 2x2 variant of the prayer widget. */
public class SmallPrayerWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        WidgetUpdater.updateSmall(context, manager, ids);
        WidgetTick.schedule(context);
        WidgetTick.scheduleSoon(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int id, Bundle options) {
        WidgetUpdater.updateSmall(context, manager, new int[]{id});
    }

    @Override
    public void onDisabled(Context context) {
        if (!WidgetTick.hasWidgets(context)) WidgetTick.cancel(context);
    }
}
