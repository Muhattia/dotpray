package com.dotpray.app.alerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.dotpray.app.data.Place;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.PrayerClock;
import com.dotpray.app.widget.PrayerWidgetProvider;

import java.time.Instant;

/**
 * Keeps two alarms alive: one that fires the prayer notification and one that
 * asks the widgets to roll over to the next prayer.
 */
public final class PrayerAlarmScheduler {

    public static final String ACTION_ALERT = "com.dotpray.app.action.PRAYER_ALERT";

    private static final int REQUEST_ALERT = 8101;
    private static final int REQUEST_REFRESH = 8102;

    private PrayerAlarmScheduler() {
    }

    public static void reschedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;

        PrayerPrefs prefs = new PrayerPrefs(app);
        Place place = prefs.getPlace();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(Instant.now(), place,
                CalcMethod.EGYPT, prefs.getAsr());

        PendingIntent alertIntent = PendingIntent.getBroadcast(app, REQUEST_ALERT,
                new Intent(app, PrayerAlarmReceiver.class).setAction(ACTION_ALERT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent refreshIntent = PendingIntent.getBroadcast(app, REQUEST_REFRESH,
                new Intent(app, PrayerWidgetProvider.class).setAction(PrayerWidgetProvider.ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        manager.cancel(alertIntent);
        manager.cancel(refreshIntent);

        long triggerAt = snapshot.next.instant.toEpochMilli() + 1500L;
        if (triggerAt < System.currentTimeMillis()) triggerAt = System.currentTimeMillis() + 60_000L;

        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, refreshIntent);

        if (prefs.isAlertsEnabled()) {
            Intent alert = new Intent(app, PrayerAlarmReceiver.class)
                    .setAction(ACTION_ALERT)
                    .putExtra("prayer", snapshot.next.prayer.label)
                    .putExtra("time", snapshot.next.local.toString())
                    .putExtra("place", place.name);
            PendingIntent pending = PendingIntent.getBroadcast(app, REQUEST_ALERT, alert,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
        }
    }

    public static void cancelAll(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent alertIntent = PendingIntent.getBroadcast(app, REQUEST_ALERT,
                new Intent(app, PrayerAlarmReceiver.class).setAction(ACTION_ALERT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent refreshIntent = PendingIntent.getBroadcast(app, REQUEST_REFRESH,
                new Intent(app, PrayerWidgetProvider.class).setAction(PrayerWidgetProvider.ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(alertIntent);
        manager.cancel(refreshIntent);
    }
}
