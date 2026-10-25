package com.dotpray.app.alerts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Build;
import android.provider.Settings;

import com.dotpray.app.MainActivity;
import com.dotpray.app.Perm;
import com.dotpray.app.R;
import com.dotpray.app.data.PlaceNames;
import com.dotpray.app.data.ThemeController;
import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.prayer.PrayerNames;

/** Builds the prayer notification in the language selected in Settings. */
public final class NotificationHelper {

    private static final String CHANNEL_ID = "prayer_alerts";
    private static final int NOTIFICATION_ID = 4711;

    private NotificationHelper() {
    }

    public static void show(Context context, Prayer prayer, String place) {
        if (prayer == null) return;

        Context localized = ThemeController.wrap(context);
        NotificationManager manager = (NotificationManager) localized.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (!notificationsEnabled(localized)) return;

        createChannel(localized, manager);

        Intent open = new Intent(localized, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(localized, 99, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String name = PrayerNames.get(localized, prayer);
        String title = PrayerNames.display(localized, prayer);
        String body = localized.getString(R.string.notification_body, name,
                PlaceNames.city(localized, place == null ? "" : place));

        Notification notification = new Notification.Builder(localized, CHANNEL_ID)
                // Monochrome silhouette in the status bar; the coloured mark
                // is the large icon inside the notification.
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(localized.getResources(),
                        R.drawable.ic_notification_large))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(Color.parseColor("#D71921"))
                .setColorized(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(content)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Rewrites the existing channel's name and description after a language
     * change. The channel is only created when the first alert is shown, so a
     * missing channel is left alone rather than being created early.
     */
    public static void refreshChannel(Context context) {
        Context localized = ThemeController.wrap(context);
        NotificationManager manager = (NotificationManager) localized.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && manager.getNotificationChannel(CHANNEL_ID) != null) {
            createChannel(localized, manager);
        }
    }

    /**
     * True when the runtime permission, the app-level notification switch and
     * the prayer channel all allow an alert. This is also used by the settings
     * sheet so a blocked channel is visible instead of silently dropping the
     * notification.
     */
    public static boolean notificationsEnabled(Context context) {
        Context app = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= 33
                && app.checkSelfPermission(Perm.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager manager = (NotificationManager) app.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
            if (channel != null
                    && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                return false;
            }
        }
        return true;
    }

    private static void createChannel(Context localized, NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                localized.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(localized.getString(R.string.notification_channel_desc));
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audio);
        channel.enableLights(true);
        channel.setLightColor(Color.parseColor("#D71921"));
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }
}
