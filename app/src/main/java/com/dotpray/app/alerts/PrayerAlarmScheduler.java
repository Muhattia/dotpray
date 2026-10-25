package com.dotpray.app.alerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.dotpray.app.data.Place;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.PrayerClock;
import com.dotpray.app.widget.PrayerWidgetProvider;
import com.dotpray.app.widget.WidgetTick;

import java.time.Instant;

/**
 * Keeps the prayer alert and the widget roll-over alarm alive.
 *
 * <p>The two alarms are deliberately independent. The widget roll-over fires
 * shortly after a prayer and must not reschedule (and therefore cancel) an
 * alert that is still waiting to be delivered. Alerts are re-booked only when
 * the alert itself fires, or when the app, location, time zone or alert setting
 * changes.
 */
public final class PrayerAlarmScheduler {

    public static final String ACTION_ALERT = "com.dotpray.app.action.PRAYER_ALERT";

    private static final int REQUEST_ALERT = 8101;
    private static final int REQUEST_REFRESH = 8102;

    /** The widget rolls over after the alert has had time to fire. */
    private static final long REFRESH_DELAY_MS = 1_500L;

    private PrayerAlarmScheduler() {
    }

    public static void reschedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;

        PrayerPrefs prefs = new PrayerPrefs(app);
        Place place = prefs.syncGpsZone();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(Instant.now(), place,
                CalcMethod.EGYPT, prefs.getAsr());

        long now = System.currentTimeMillis();
        long prayerAt = snapshot.next.instant.toEpochMilli();
        // Fire at the prayer instant itself. The small floor only protects
        // against a reschedule that lands inside the last 100 ms before it.
        long alertAt = Math.max(now + 100L, prayerAt);
        long refreshAt = Math.max(now + 1_000L, prayerAt + REFRESH_DELAY_MS);

        // Cancel by identity. Re-creating the PendingIntent with an empty
        // Intent here would wipe the extras of a pending alert.
        cancelIfPresent(manager, app, REQUEST_REFRESH,
                PrayerWidgetProvider.class, PrayerWidgetProvider.ACTION_REFRESH);

        PendingIntent refresh = PendingIntent.getBroadcast(app, REQUEST_REFRESH,
                new Intent(app, PrayerWidgetProvider.class)
                        .setAction(PrayerWidgetProvider.ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        setExactOrAllowWhileIdle(manager, refreshAt, refresh);

        // The card shows "PRAYER NOW" between the Adhan and the end of the
        // grace period, then switches to the next countdown. This widget-only
        // alarm is separate from the alert above so neither can cancel the
        // other.
        WidgetTick.scheduleGrace(app);

        if (!prefs.isAlertsEnabled()) {
            cancelIfPresent(manager, app, REQUEST_ALERT,
                    PrayerAlarmReceiver.class, ACTION_ALERT);
            return;
        }

        Intent alert = new Intent(app, PrayerAlarmReceiver.class)
                .setAction(ACTION_ALERT)
                .putExtra("prayer", snapshot.next.prayer.name())
                .putExtra("time", snapshot.next.local.toString())
                .putExtra("place", place.name);
        PendingIntent pending = PendingIntent.getBroadcast(app, REQUEST_ALERT, alert,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        setExactOrAllowWhileIdle(manager, alertAt, pending);
    }

    public static void cancelAll(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        cancelIfPresent(manager, app, REQUEST_ALERT,
                PrayerAlarmReceiver.class, ACTION_ALERT);
        cancelIfPresent(manager, app, REQUEST_REFRESH,
                PrayerWidgetProvider.class, PrayerWidgetProvider.ACTION_REFRESH);
    }

    /**
     * Exact alarms are the difference between a notification at the prayer
     * minute and one that Doze may defer by several minutes. Android 13+
     * grants USE_EXACT_ALARM to clock/reminder apps; when it is unavailable we
     * still schedule an allow-while-idle alarm rather than dropping the alert.
     */
    private static void setExactOrAllowWhileIdle(AlarmManager manager, long triggerAt,
                                                 PendingIntent intent) {
        try {
            if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent);
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent);
            }
        } catch (SecurityException e) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent);
        }
    }

    private static void cancelIfPresent(AlarmManager manager, Context app, int requestCode,
                                        Class<?> receiver, String action) {
        PendingIntent existing = PendingIntent.getBroadcast(app, requestCode,
                new Intent(app, receiver).setAction(action),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (existing != null) manager.cancel(existing);
    }
}
