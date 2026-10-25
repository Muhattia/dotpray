package com.dotpray.app.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.dotpray.app.data.Place;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.PrayerClock;

import java.time.Instant;

/**
 * Re-renders the widgets on the next minute boundary and exactly when a grace
 * period ends.
 *
 * <p>The card artwork is a bitmap (RemoteViews has no custom-font support), so
 * it is redrawn when the minute, day, theme, location or grace state changes.
 * Non-wakeup {@code RTC} alarms are used so the refresh never wakes the phone;
 * a delayed alarm is corrected as soon as the screen is turned on, when someone
 * can actually see the card.
 */
public final class WidgetTick {

    static final String ACTION = "com.dotpray.app.action.TICK";
    public static final String ACTION_GRACE = "com.dotpray.app.action.GRACE";
    private static final int REQUEST = 41;
    private static final int REQUEST_GRACE = 42;

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
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC, next,
                        intent(context, REQUEST, ACTION));
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC, next,
                        intent(context, REQUEST, ACTION));
            }
        } catch (SecurityException ignored) {
            // no alarm permission: the other refresh paths still run
        }
        scheduleGrace(context);
    }

    /**
     * Books a single widget-only refresh for the moment the active grace ends.
     * It never touches the prayer alert: the widget roll-over and the alert
     * are deliberately independent.
     */
    public static void scheduleGrace(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null || !hasWidgets(context)) return;

        PrayerPrefs prefs = new PrayerPrefs(context);
        Place place = prefs.syncGpsZone();
        long now = System.currentTimeMillis();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(Instant.ofEpochMilli(now),
                place, CalcMethod.EGYPT, prefs.getAsr());
        long at = Math.max(now + 1_000L, snapshot.nextGraceEndMs(now));
        PendingIntent pending = intent(context, REQUEST_GRACE, ACTION_GRACE);

        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC, at, pending);
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC, at, pending);
            }
        } catch (SecurityException e) {
            try {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC, at, pending);
            } catch (SecurityException ignored) {
                // the minute tick will correct the card
            }
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
            alarms.set(AlarmManager.RTC, System.currentTimeMillis() + 1_500L,
                    intent(context, REQUEST, ACTION));
        } catch (SecurityException ignored) {
            // the minute tick will correct it
        }
    }

    static void cancel(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        alarms.cancel(intent(context, REQUEST, ACTION));
        alarms.cancel(intent(context, REQUEST_GRACE, ACTION_GRACE));
    }

    /** True while at least one Dotpray widget is on a home screen. */
    static boolean hasWidgets(Context context) {
        android.appwidget.AppWidgetManager manager =
                android.appwidget.AppWidgetManager.getInstance(context);
        return manager.getAppWidgetIds(new android.content.ComponentName(
                context, PrayerWidgetProvider.class)).length > 0
                || manager.getAppWidgetIds(new android.content.ComponentName(
                context, SmallPrayerWidgetProvider.class)).length > 0
                || manager.getAppWidgetIds(new android.content.ComponentName(
                context, MinimalPrayerWidgetProvider.class)).length > 0;
    }

    private static PendingIntent intent(Context context, int request, String action) {
        Intent refresh = new Intent(context, PrayerWidgetProvider.class).setAction(action);
        return PendingIntent.getBroadcast(context, request, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
