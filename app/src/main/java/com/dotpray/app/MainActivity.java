package com.dotpray.app;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.dotpray.app.alerts.NotificationHelper;
import com.dotpray.app.alerts.PrayerAlarmScheduler;
import com.dotpray.app.data.Format;
import com.dotpray.app.data.LocalityResolver;
import com.dotpray.app.data.Place;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.data.ThemeController;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.Hijri;
import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.prayer.PrayerCalculator;
import com.dotpray.app.prayer.PrayerClock;
import com.dotpray.app.prayer.Qibla;
import com.dotpray.app.ui.DotFieldView;
import com.dotpray.app.ui.ProgressLineView;
import com.dotpray.app.ui.SettingsDialog;
import com.dotpray.app.widget.PrayerWidgetProvider;
import com.dotpray.app.widget.SmallPrayerWidgetProvider;
import com.dotpray.app.widget.WidgetUpdater;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The single screen: next prayer, countdown, the day ahead, and the controls. */
public class MainActivity extends Activity implements SettingsDialog.Callback {

    private static final int REQUEST_LOCATION = 41;
    private static final int REQUEST_NOTIFICATIONS = 42;

    /** Current-location re-check interval while the app is open. */
    private static final long LOCATION_REFRESH_MS = 15 * 60_000L;
    /** A cached fix is considered current only for this long. */
    private static final long LOCATION_FRESH_MS = 10 * 60_000L;

    private PrayerPrefs prefs;
    private PrayerClock.Snapshot snapshot;

    private ScrollView scroll;
    private DotFieldView dotField;
    private TextView cityLabel;
    private TextView locationDetail;
    private TextView dateLabel;
    private TextView hijriLabel;
    private TextView clockValue;
    private TextView heroIndex;
    private TextView nextName;
    private TextView nextTime;
    private TextView countdownValue;
    private TextView sinceLabel;
    private TextView progressLabel;
    private ProgressLineView progress;
    private TextView clockTag;
    private TextView locationValue;
    private TextView locationCoords;
    private TextView qiblaValue;
    private TextView locationHint;
    private LinearLayout prayerList;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private SettingsDialog settingsDialog;
    private boolean pendingLocation;
    private boolean pendingAlerts;
    private boolean locationFetching;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Runnable locationTimeout;

    private final Runnable locationRefresh = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (hasLocationPermission()) fetchLocation(false);
            handler.postDelayed(this, LOCATION_REFRESH_MS);
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply the saved System / Light / Dark choice before the activity
        // resolves its theme and resources. values-night/ then follows the
        // chosen mode, not necessarily the phone's mode.
        super.attachBaseContext(ThemeController.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrayerPrefs(this);

        Window window = getWindow();
        boolean night = isNight();
        window.setStatusBarColor(getColor(R.color.bg));
        window.setNavigationBarColor(getColor(R.color.bg));
        if (Build.VERSION.SDK_INT >= 30) {
            Api30.setDecorFitsSystemWindows(window);
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (!night) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }

        setContentView(R.layout.activity_main);
        // The insets controller needs the decor view, which only exists after
        // setContentView (calling it earlier threw a NullPointerException on
        // Android 11+ and killed the launch).
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Api30.setLightSystemBars(window, !night);
            } catch (RuntimeException ignored) {
                // Icon tint is cosmetic; never let it take the launch down.
            }
        }
        bindViews();
        wireInteractions();
        applyInsets();

        refresh();
        handleIntent(getIntent());
    }

    private void bindViews() {
        scroll = findViewById(R.id.scroll);
        dotField = findViewById(R.id.dotField);
        cityLabel = findViewById(R.id.cityLabel);
        locationDetail = findViewById(R.id.locationDetail);
        dateLabel = findViewById(R.id.dateLabel);
        hijriLabel = findViewById(R.id.hijriLabel);
        clockValue = findViewById(R.id.clockValue);
        heroIndex = findViewById(R.id.heroIndex);
        nextName = findViewById(R.id.nextName);
        nextTime = findViewById(R.id.nextTime);
        countdownValue = findViewById(R.id.countdownValue);
        sinceLabel = findViewById(R.id.sinceLabel);
        progressLabel = findViewById(R.id.progressLabel);
        progress = findViewById(R.id.progress);
        clockTag = findViewById(R.id.clockTag);
        locationValue = findViewById(R.id.locationValue);
        locationCoords = findViewById(R.id.locationCoords);
        qiblaValue = findViewById(R.id.qiblaValue);
        locationHint = findViewById(R.id.locationHint);
        prayerList = findViewById(R.id.prayerList);
    }

    private void wireInteractions() {
        findViewById(R.id.cityButton).setOnClickListener(v -> requestCurrentLocation());
        findViewById(R.id.tileLocation).setOnClickListener(v -> requestCurrentLocation());
        findViewById(R.id.btnSettingsGhost).setOnClickListener(v -> openSettings());
        findViewById(R.id.btnAddWidget).setOnClickListener(v -> pinWidget(true));

        scroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                dotField.setScrollOffset(scrollY * 0.35f));
    }

    private void applyInsets() {
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                int[] bars = new int[2];
                Api30.systemBarInsets(insets, bars);
                top = bars[0];
                bottom = bars[1];
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
    }

    // ── data ────────────────────────────────────────────────────────────

    private void refresh() {
        Place place = prefs.getPlace();
        snapshot = PrayerClock.snapshot(Instant.now(), place, CalcMethod.EGYPT, prefs.getAsr());
        render();
    }

    private void render() {
        Place place = snapshot.place;
        boolean use24 = prefs.is24Hour();

        cityLabel.setText(place.shortName().toUpperCase(Locale.US));
        locationDetail.setText((place.label() + " \u00b7 " + place.coordinates())
                .toUpperCase(Locale.US));
        dateLabel.setText(DateTimeFormatter.ofPattern("EEE / dd MMM yyyy", Locale.US)
                .format(snapshot.date).toUpperCase(Locale.US));
        hijriLabel.setText(hijri(snapshot.date));
        clockTag.setText(use24 ? "24H" : "12H");
        updateClock();

        heroIndex.setText(String.format(Locale.US, "%02d / 05",
                snapshot.next.prayer.ordinal() + 1));
        nextName.setText(snapshot.next.prayer.label.toUpperCase(Locale.US));
        nextTime.setText(timeText(snapshot.next.local, use24, 0.28f));

        String sinceTime = use24 ? Format.time24(snapshot.previous.local)
                : Format.time12(snapshot.previous.local) + " "
                + Format.meridiem(snapshot.previous.local);
        sinceLabel.setText("SINCE "
                + snapshot.previous.prayer.label.toUpperCase(Locale.US) + " " + sinceTime);

        locationValue.setText(place.label().toUpperCase(Locale.US));
        locationCoords.setText(place.coordinates());
        locationHint.setText(place.gps
                ? "GPS FIX · TAP TO REFRESH"
                : "DEFAULT · TAP TO USE GPS");
        double bearing = Qibla.bearing(place);
        qiblaValue.setText(String.format(Locale.US, "QIBLA %.0f° %s", bearing, Format.cardinal(bearing)));

        renderPrayerList();
        updateCountdown();
    }

    private void renderPrayerList() {
        prayerList.removeAllViews();
        Instant now = Instant.now();
        Prayer[] prayers = Prayer.values();

        for (int i = 0; i < prayers.length; i++) {
            PrayerCalculator.Entry entry = snapshot.entry(prayers[i]);
            if (entry == null) continue;

            boolean past = !entry.instant.isAfter(now);
            boolean active = prayers[i] == snapshot.next.prayer
                    && !snapshot.nextIsTomorrow;
            int bottom = i == prayers.length - 1 && !snapshot.nextIsTomorrow
                    ? 0 : dp(8);
            addPrayerRow(entry, i + 1, active, past, bottom);
        }

        // After Isha the next prayer belongs to tomorrow. Keep today's five
        // times visible, then show that Fajr explicitly instead of leaving the
        // whole list without a lit "NEXT" row.
        if (snapshot.nextIsTomorrow) {
            TextView tomorrow = new TextView(this);
            tomorrow.setText("TOMORROW");
            tomorrow.setTextAppearance(R.style.LabelFaint);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dp(18);
            labelParams.bottomMargin = dp(8);
            labelParams.leftMargin = dp(2);
            tomorrow.setLayoutParams(labelParams);
            prayerList.addView(tomorrow);

            addPrayerRow(snapshot.next, 1, true, false, 0);
        }
    }

    private void addPrayerRow(PrayerCalculator.Entry entry, int indexNumber,
                              boolean active, boolean past, int bottomMargin) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_prayer, prayerList, false);
        View marker = row.findViewById(R.id.pMarker);
        TextView index = row.findViewById(R.id.pIndex);
        TextView name = row.findViewById(R.id.pName);
        TextView time = row.findViewById(R.id.pTime);
        TextView status = row.findViewById(R.id.pStatus);

        index.setText(String.format(Locale.US, "%02d", indexNumber));
        name.setText(entry.prayer.label.toUpperCase(Locale.US));
        time.setText(timeText(entry.local, prefs.is24Hour(), 0.5f));

        row.setBackgroundResource(active
                ? R.drawable.bg_prayer_active
                : R.drawable.bg_option);

        if (active) {
            // A small dot and the NEXT label carry the state; the row keeps the
            // same surface as the rest of the schedule so the red accent stays
            // an indicator rather than a large block of colour.
            marker.setVisibility(View.VISIBLE);
            name.setTextColor(getColor(R.color.fg));
            time.setTextColor(getColor(R.color.fg));
            index.setTextColor(getColor(R.color.accent));
            status.setVisibility(View.VISIBLE);
            status.setText("NEXT");
        } else {
            marker.setVisibility(View.INVISIBLE);
            name.setTextColor(getColor(past ? R.color.fg_past : R.color.fg));
            time.setTextColor(getColor(past ? R.color.fg_past : R.color.fg));
            index.setTextColor(getColor(R.color.fg_faint));
            // Keep the status line's height reserved so every schedule row is
            // the same height; only its ink changes with the active state.
            status.setVisibility(View.INVISIBLE);
        }

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        lp.bottomMargin = bottomMargin;
        row.setLayoutParams(lp);
        prayerList.addView(row);
    }

    private void updateClock() {
        ZonedDateTime now = Instant.now().atZone(snapshot.place.zone());
        clockValue.setText(Format.clockWithSeconds(now, prefs.is24Hour()));
    }

    private void updateCountdown() {
        long nowMs = System.currentTimeMillis();
        long remaining = snapshot.next.instant.toEpochMilli() - nowMs;
        if (remaining < 0) remaining = 0;
        // The red "IN" beside this value already labels the countdown.
        countdownValue.setText(Format.duration(remaining));

        float fraction = snapshot.progress(nowMs);
        progress.setProgress(fraction);
        progressLabel.setText(String.format(Locale.US, "%d%% ELAPSED",
                Math.round(fraction * 100f)));
    }

    private CharSequence timeText(java.time.ZonedDateTime time, boolean use24, float periodScale) {
        if (use24) return Format.time24(time);
        String base = Format.time12(time);
        String period = Format.meridiem(time);
        SpannableString text = new SpannableString(base + " " + period);
        text.setSpan(new RelativeSizeSpan(periodScale), base.length() + 1, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    private String hijri(java.time.LocalDate date) {
        try {
            return Hijri.from(date).label();
        } catch (RuntimeException e) {
            return "";
        }
    }

    // ── ticking ─────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        // Never leave the switch claiming alerts are on while the system is
        // silently dropping them (permission revoked or channel blocked).
        if (prefs.isAlertsEnabled() && !NotificationHelper.notificationsEnabled(this)) {
            prefs.setAlertsEnabled(false);
            if (settingsDialog != null) settingsDialog.setAlertsChecked(false);
        }
        refresh();
        WidgetUpdater.updateAll(this);
        // A force-stop, battery manager or app update can clear the alarm
        // while the preference stays on. Re-arm whenever the app is opened.
        if (prefs.isAlertsEnabled()) PrayerAlarmScheduler.reschedule(this);
        startTicker();
        startLocationRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTicker();
        stopLocationRefresh();
    }

    private void startTicker() {
        stopTicker();
        ticker = new Runnable() {
            @Override
            public void run() {
                if (snapshot != null) {
                    LocalDate today = Instant.now().atZone(snapshot.place.zone()).toLocalDate();
                    long remaining = snapshot.next.instant.toEpochMilli()
                            - System.currentTimeMillis();
                    if (remaining <= 0 || !today.equals(snapshot.date)) {
                        refresh();
                    } else {
                        updateClock();
                        updateCountdown();
                    }
                }
                handler.postDelayed(this, 1000L);
            }
        };
        handler.post(ticker);
    }

    private void stopTicker() {
        if (ticker != null) handler.removeCallbacks(ticker);
        ticker = null;
    }

    private void openSettings() {
        if (settingsDialog != null) settingsDialog.dismiss();
        settingsDialog = new SettingsDialog(this, this);
        settingsDialog.show();
    }

    // ── settings callbacks ──────────────────────────────────────────────

    @Override
    public void onAsrChanged() {
        applyChanges();
    }

    @Override
    public void onThemeChanged() {
        if (settingsDialog != null) {
            settingsDialog.dismiss();
            settingsDialog = null;
        }
        // The bitmap widgets use the same preference, so repaint them before
        // the activity is recreated with the new configuration.
        WidgetUpdater.updateAll(this);
        recreate();
    }

    @Override
    public void onClockChanged(boolean use24Hour) {
        applyChanges();
    }

    @Override
    public void onAlertsChanged(boolean enabled) {
        if (!enabled) {
            prefs.setAlertsEnabled(false);
            PrayerAlarmScheduler.reschedule(this);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Perm.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingAlerts = true;
            requestPermissions(new String[]{Perm.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
            return;
        }
        if (!NotificationHelper.notificationsEnabled(this)) {
            prefs.setAlertsEnabled(false);
            if (settingsDialog != null) settingsDialog.setAlertsChecked(false);
            toast("Allow notifications for Dotpray");
            openNotificationSettings();
            return;
        }
        prefs.setAlertsEnabled(true);
        PrayerAlarmScheduler.reschedule(this);
        toast("Prayer alerts on");
    }

    @Override
    public void onAddWidget(boolean wide) {
        pinWidget(wide);
    }

    // ── location ────────────────────────────────────────────────────────

    /**
     * Current-location mode is deliberately simple: there is no city picker.
     * The saved GPS fix remains in use, and it is re-checked every 15 minutes
     * while this screen is open (plus once on resume when it is stale). The
     * per-minute widget refresh is separate in WidgetTick.
     */
    private void startLocationRefresh() {
        handler.removeCallbacks(locationRefresh);

        if (hasLocationPermission()) {
            if (isLocationStale()) fetchLocation(false);
        } else if (!prefs.hasAskedForLocation()) {
            prefs.setAskedForLocation();
            handler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed() && !hasLocationPermission()) {
                    requestCurrentLocation(true);
                }
            }, 550L);
        }

        handler.postDelayed(locationRefresh, LOCATION_REFRESH_MS);
    }

    private void stopLocationRefresh() {
        handler.removeCallbacks(locationRefresh);
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationStale() {
        long updated = prefs.getLocationUpdatedAt();
        return updated <= 0L
                || System.currentTimeMillis() - updated >= LOCATION_REFRESH_MS;
    }

    private void requestCurrentLocation() {
        requestCurrentLocation(true);
    }

    private void requestCurrentLocation(boolean userInitiated) {
        if (hasLocationPermission()) {
            fetchLocation(userInitiated);
            return;
        }
        if (!userInitiated || pendingLocation) return;

        pendingLocation = true;
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_LOCATION);
    }

    private void fetchLocation(boolean userInitiated) {
        if (locationFetching) return;

        final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            if (userInitiated) toast("Location unavailable");
            return;
        }

        locationFetching = true;
        locationManager = manager;

        Location best = null;
        try {
            for (String provider : manager.getProviders(true)) {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
            }
        } catch (SecurityException ignored) {
            // fall through to a live request
        }

        // A cached fix is only "current" for ten minutes. Older fixes are
        // ignored so the periodic check asks for a live position instead of
        // silently reusing a location from another city.
        if (best != null
                && System.currentTimeMillis() - best.getTime() < LOCATION_FRESH_MS) {
            cleanupLocationFetch();
            useLocation(best, userInitiated);
            return;
        }

        locationTimeout = () -> {
            cleanupLocationFetch();
            if (userInitiated) toast("No location fix yet");
        };

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (!locationFetching) return;
                cleanupLocationFetch();
                useLocation(location, userInitiated);
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
            }
        };

        try {
            String provider = null;
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            } else if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            }

            if (provider == null) {
                cleanupLocationFetch();
                if (userInitiated) toast("Turn on location services to use GPS");
                return;
            }

            manager.requestLocationUpdates(provider, 0L, 0f, locationListener,
                    Looper.getMainLooper());
            handler.postDelayed(locationTimeout, 12_000L);
        } catch (SecurityException e) {
            cleanupLocationFetch();
            if (userInitiated) toast("Location permission denied");
        } catch (IllegalArgumentException e) {
            cleanupLocationFetch();
            if (userInitiated) toast("Turn on location services to use GPS");
        }
    }

    /**
     * Removes the live request and its timeout. A fix can arrive while the
     * activity is paused; that is fine, because the place and widgets are
     * application state, not activity state.
     */
    private void cleanupLocationFetch() {
        if (locationTimeout != null) {
            handler.removeCallbacks(locationTimeout);
            locationTimeout = null;
        }
        if (locationListener != null && locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException | IllegalArgumentException ignored) {
                // already removed or permission revoked
            }
        }
        locationListener = null;
        locationManager = null;
        locationFetching = false;
    }

    private void useLocation(Location location, boolean announce) {
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();
        final String zoneId = ZoneId.systemDefault().getId();

        // Name it immediately from the bundled cities, then let the platform
        // geocoder refine it in the background. The widget shows this name, so
        // both passes go through applyChanges().
        Place near = Place.nearest(latitude, longitude, 150);
        String name = near != null ? near.name : "CURRENT LOCATION";
        String country = near != null ? near.country : "";
        prefs.setPlace(new Place(name, country, latitude, longitude, zoneId, true));
        prefs.setLocationUpdatedAt(System.currentTimeMillis());
        applyChanges();
        if (announce) toast("Location updated");

        LocalityResolver.resolve(this, latitude, longitude, (resolved, code) -> {
            if (isFinishing() || isDestroyed()) return;
            Place current = prefs.getPlace();
            if (current == null || !current.gps) return;
            if (Math.abs(current.latitude - latitude) > 1e-5
                    || Math.abs(current.longitude - longitude) > 1e-5) return;
            if (resolved == null || resolved.equalsIgnoreCase(current.name)) return;

            prefs.setPlace(new Place(resolved,
                    code == null ? "" : code, latitude, longitude, zoneId, true));
            applyChanges();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == REQUEST_NOTIFICATIONS) {
            boolean alertsOn = granted && NotificationHelper.notificationsEnabled(this);
            prefs.setAlertsEnabled(alertsOn);
            if (settingsDialog != null) settingsDialog.setAlertsChecked(alertsOn);
            if (alertsOn) {
                PrayerAlarmScheduler.reschedule(this);
                toast("Prayer alerts on");
            } else {
                toast("Notifications blocked");
            }
            pendingAlerts = false;
            return;
        }

        if (requestCode == REQUEST_LOCATION) {
            pendingLocation = false;
            if (granted) {
                fetchLocation(true);
            } else {
                toast("Location permission denied");
            }
        }
    }

    // ── widgets ─────────────────────────────────────────────────────────

    private void pinWidget(boolean wide) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName component = new ComponentName(this,
                wide ? PrayerWidgetProvider.class : SmallPrayerWidgetProvider.class);
        if (manager.isRequestPinAppWidgetSupported()) {
            manager.requestPinAppWidget(component, null, null);
        } else {
            toast("Long-press the home screen, open Widgets, choose Dotpray");
        }
    }

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyChanges() {
        refresh();
        WidgetUpdater.updateAll(this);
        PrayerAlarmScheduler.reschedule(this);
    }

    // ── misc ────────────────────────────────────────────────────────────

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("open_settings", false)) {
            intent.removeExtra("open_settings");
            handler.postDelayed(this::openSettings, 220L);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (RuntimeException e) {
            toast("Open system settings to allow notifications");
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        stopTicker();
        stopLocationRefresh();
        cleanupLocationFetch();
        super.onDestroy();
    }
}
