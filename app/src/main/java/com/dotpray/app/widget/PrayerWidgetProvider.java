package com.dotpray.app.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.dotpray.app.alerts.PrayerAlarmScheduler;

/**
 * Home screen widget showing the next prayer, a live countdown and the whole
 * day at a glance.
 */
public class PrayerWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.dotpray.app.action.REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        WidgetUpdater.updateWide(context, manager, ids);
        PrayerAlarmScheduler.reschedule(context);
        WidgetTick.schedule(context);
        WidgetTick.scheduleSoon(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int id, Bundle options) {
        WidgetUpdater.updateWide(context, manager, new int[]{id});
    }

    @Override
    public void onDisabled(Context context) {
        if (!WidgetTick.hasWidgets(context)) WidgetTick.cancel(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) return;

        boolean needsRefresh = ACTION_REFRESH.equals(action)
                || WidgetTick.ACTION.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action);

        if (needsRefresh) {
            WidgetUpdater.updateAll(context);
            PrayerAlarmScheduler.reschedule(context);
        }
    }
}
