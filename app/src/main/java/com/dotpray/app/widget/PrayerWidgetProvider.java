package com.dotpray.app.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.dotpray.app.alerts.PrayerAlarmScheduler;

/**
 * Home screen widget showing the next prayer, a countdown and the whole day at
 * a glance.
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

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) return;

        boolean needsRefresh = ACTION_REFRESH.equals(action)
                || WidgetTick.ACTION.equals(action)
                || WidgetTick.ACTION_GRACE.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action);

        if (!needsRefresh) return;

        WidgetUpdater.updateAll(context);
        WidgetTick.schedule(context);

        // A minute tick or the post-prayer roll-over must never re-book the
        // prayer alert. At the boundary the snapshot has already moved to the
        // following prayer, so rescheduling there would cancel the alert that
        // is about to fire. Re-book only when the clock/zone/package changed
        // or the device rebooted.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)) {
            PrayerAlarmScheduler.reschedule(context);
        }
    }
}
