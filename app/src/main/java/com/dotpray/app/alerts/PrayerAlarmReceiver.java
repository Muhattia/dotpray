package com.dotpray.app.alerts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.widget.WidgetUpdater;

/** Fires when a prayer enters, then books the next one. */
public class PrayerAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !PrayerAlarmScheduler.ACTION_ALERT.equals(intent.getAction())) return;

        PrayerPrefs prefs = new PrayerPrefs(context);
        if (prefs.isAlertsEnabled()) {
            Prayer prayer = Prayer.fromKey(intent.getStringExtra("prayer"));
            String place = intent.getStringExtra("place");
            if (prayer != null) {
                NotificationHelper.show(context, prayer, place == null ? "" : place);
            }
        }

        WidgetUpdater.updateAll(context);
        PrayerAlarmScheduler.reschedule(context);
    }
}
