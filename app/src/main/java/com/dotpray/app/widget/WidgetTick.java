package com.dotpray.app.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Re-renders the widgets on the next minute boundary.
 *
 * <p>The card artwork is a bitmap (RemoteViews has no custom-font support), so
 * the static parts still need a redraw when the minute, day, theme or location
 * changes. The live wall clock and countdown are native remote views and tick
 * independently. A non-wakeup {@code RTC} alarm is used rather than
 * {@code RTC_WAKEUP}: it fires while the device is awake, and is deferred when
 * it is not, which is exactly when nobody can see the widget. Nothing wakes the
 * phone up every minute. The launcher refreshes again immediately when the
 * device is unlocked, so a delayed idle alarm cannot leave stale static data
 * on screen.
 */
final class WidgetTick {

    static final String ACTION = "com.dotpray.app.action.TICK";
    private static final int REQUEST = 41;

    private WidgetTick() {
    }

    static void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null || !hasWidgets(context)) return;
        long now = System.currentTimeMillis();
        long next = now - (now % 60_000L) + 60_000L + 150L;
        boolean exact = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms();
        try {
            // Exact and non-wakeup: the widget is current while the screen is
            // on, but the phone is never woken by the minute tick. If exact
            // alarms have not been made available, fall back to an inexact
            // allow-while-idle alarm; unlocking also refreshes immediately.
            if (exact) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC, next, intent(context));
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC, next, intent(context));
            }
        } catch (SecurityException ignored) {
            // no alarm permission: the other refresh paths still run
        }
    }

    /**
     * Re-renders shortly after a widget is added: the first onUpdate often runs
     * before the launcher has filled in the size options, so the card would be
     * drawn at the fallback size until the next minute tick.
     */
    static void scheduleSoon(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null || !hasWidgets(context)) return;
        try {
            alarms.set(AlarmManager.RTC, System.currentTimeMillis() + 1_500L, intent(context));
        } catch (SecurityException ignored) {
            // the minute tick will correct it
        }
    }

    static void cancel(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        alarms.cancel(intent(context));
    }

    /** True while at least one Dotpray widget is on a home screen. */
    static boolean hasWidgets(Context context) {
        android.appwidget.AppWidgetManager manager =
                android.appwidget.AppWidgetManager.getInstance(context);
        return manager.getAppWidgetIds(new android.content.ComponentName(
                context, PrayerWidgetProvider.class)).length > 0
                || manager.getAppWidgetIds(new android.content.ComponentName(
                context, SmallPrayerWidgetProvider.class)).length > 0;
    }

    private static PendingIntent intent(Context context) {
        Intent refresh = new Intent(context, PrayerWidgetProvider.class).setAction(ACTION);
        return PendingIntent.getBroadcast(context, REQUEST, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
