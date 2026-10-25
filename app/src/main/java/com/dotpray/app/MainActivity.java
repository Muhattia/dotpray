package com.dotpray.app;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
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
import com.dotpray.app.data.PlaceNames;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.data.ThemeController;
import com.dotpray.app.data.UiText;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.CompassMath;
import com.dotpray.app.prayer.ElapsedText;
import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.prayer.PrayerCalculator;
import com.dotpray.app.prayer.PrayerClock;
import com.dotpray.app.prayer.PrayerNames;
import com.dotpray.app.prayer.Qibla;
import com.dotpray.app.ui.DotFieldView;
import com.dotpray.app.ui.ProgressLineView;
import com.dotpray.app.ui.SettingsDialog;
import com.dotpray.app.ui.WidgetSizeDialog;
import com.dotpray.app.widget.MinimalPrayerWidgetProvider;
import com.dotpray.app.widget.PrayerWidgetProvider;
import com.dotpray.app.widget.SmallPrayerWidgetProvider;
import com.dotpray.app.widget.WidgetModel;
import com.dotpray.app.widget.WidgetUpdater;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private View cityButton;
    private TextView cityLabel;
    private TextView locationDetail;
    private TextView dateLabel;
    private TextView hijriLabel;
    private TextView clockValue;
    private TextView heroIndex;
    private TextView nextHeader;
    private TextView nextName;
    private TextView nextTime;
    private TextView graceMessage;
    private View countdownRow;
    private TextView countdownValue;
    private TextView sinceLabel;
    private TextView progressLabel;
    private ProgressLineView progress;
    private TextView clockTag;
    private TextView locationValue;
    private TextView locationCoords;
    private View qiblaTile;
    private TextView qiblaTileValue;
    private TextView locationHint;
    private LinearLayout prayerList;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private long lastTickMs;
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
        cityButton = findViewById(R.id.cityButton);
        cityLabel = findViewById(R.id.cityLabel);
        locationDetail = findViewById(R.id.locationDetail);
        dateLabel = findViewById(R.id.dateLabel);
        hijriLabel = findViewById(R.id.hijriLabel);
        clockValue = findViewById(R.id.clockValue);
        heroIndex = findViewById(R.id.heroIndex);
        nextHeader = findViewById(R.id.nextHeader);
        nextName = findViewById(R.id.nextName);
        nextTime = findViewById(R.id.nextTime);
        graceMessage = findViewById(R.id.graceMessage);
        countdownRow = findViewById(R.id.countdownRow);
        countdownValue = findViewById(R.id.countdownValue);
        sinceLabel = findViewById(R.id.sinceLabel);
        progressLabel = findViewById(R.id.progressLabel);
        progress = findViewById(R.id.progress);
        clockTag = findViewById(R.id.clockTag);
        locationValue = findViewById(R.id.locationValue);
        locationCoords = findViewById(R.id.locationCoords);
        qiblaTile = findViewById(R.id.tileQibla);
        qiblaTileValue = findViewById(R.id.qiblaTileValue);
        locationHint = findViewById(R.id.locationHint);
        prayerList = findViewById(R.id.prayerList);
    }

    private void wireInteractions() {
        cityButton.setOnClickListener(v -> requestCurrentLocation());
        cityButton.setFocusable(true);
        expandCityTouchTarget();
        findViewById(R.id.tileLocation).setOnClickListener(v -> requestCurrentLocation());
        qiblaTile.setOnClickListener(v -> openQibla());
        findViewById(R.id.btnSettingsGhost).setOnClickListener(v -> openSettings());
        findViewById(R.id.btnAddWidget).setOnClickListener(v -> showWidgetSizes());

        scroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                dotField.setScrollOffset(scrollY * 0.35f));
    }

    /**
     * The location chip is deliberately compact, so extend its touch target to
     * the 48 dp frame around it without changing how it looks.
     */
    private void expandCityTouchTarget() {
        View touch = findViewById(R.id.cityTouch);
        if (touch == null || cityButton == null) return;
        // Recompute on every layout: rotation and long place names change the
        // chip's width even though the activity is not recreated.
        touch.addOnLayoutChangeListener((v, left, top, right, bottom,
                                         oldLeft, oldTop, oldRight, oldBottom) ->
                updateCityTouchDelegate(touch));
        touch.post(() -> updateCityTouchDelegate(touch));
    }

    private void updateCityTouchDelegate(View touch) {
        if (cityButton == null || touch.getHeight() <= 0) return;
        Rect rect = new Rect();
        cityButton.getHitRect(rect);
        rect.top = 0;
        rect.bottom = touch.getHeight();
        touch.setTouchDelegate(new TouchDelegate(rect, cityButton));
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
        Place place = prefs.syncGpsZone();
        snapshot = PrayerClock.snapshot(Instant.now(), place, CalcMethod.EGYPT, prefs.getAsr());
        render();
    }

    private void render() {
        Place place = snapshot.place;
        boolean use24 = prefs.is24Hour();
        WidgetModel model = WidgetModel.of(snapshot, System.currentTimeMillis());
        String city = UiText.upper(this, PlaceNames.shortName(this, place));
        // The location readouts name the place itself, not its country: the
        // area is the useful part, and "Cairo, EG" only adds noise at a glance.
        String placeName = PlaceNames.city(this, place.name);
        String coordinates = UiText.coordinates(this, place);

        cityLabel.setText(city);
        cityButton.setContentDescription(getString(R.string.a11y_location_refresh,
                placeName));
        locationDetail.setText(UiText.upper(this, getString(
                R.string.location_detail, placeName, coordinates)));
        dateLabel.setText(UiText.date(this, snapshot.date));
        hijriLabel.setText(UiText.hijri(this, snapshot.date, false));
        clockTag.setText(getString(use24 ? R.string.clock_24h_tag : R.string.clock_12h_tag));
        updateClock();

        nextHeader.setText(getString(model.grace
                ? R.string.prayer_now : R.string.next_prayer));
        heroIndex.setText(String.format(Locale.US, "%02d / 05",
                model.prayer.ordinal() + 1));
        nextName.setText(PrayerNames.display(this, model.prayer));
        nextTime.setText(UiText.timeText(this, model.entry.local, use24, 0.28f));

        sinceLabel.setText(getString(R.string.since_prayer,
                PrayerNames.display(this, snapshot.previous.prayer),
                UiText.time(this, snapshot.previous.local, use24)));

        locationValue.setText(UiText.upper(this, placeName));
        locationCoords.setText(coordinates);
        updateLocationHint(place.gps);
        int bearingDegrees = CompassMath.paddedDegrees(Qibla.bearing(place));
        qiblaTileValue.setText(String.format(Locale.US, "%d°", bearingDegrees));
        qiblaTile.setContentDescription(getString(R.string.a11y_qibla_open,
                String.valueOf(bearingDegrees)));

        renderPrayerList(model);
        updateCountdown(model);
    }

    private void updateLocationHint(boolean gps) {
        if (locationHint == null) return;
        locationHint.setText(gps
                ? R.string.location_hint_gps : R.string.location_hint_default);
    }

    private void showLocatingHint() {
        if (locationHint == null) return;
        locationHint.setText(R.string.location_hint_locating);
    }

    private void renderPrayerList(WidgetModel model) {
        prayerList.removeAllViews();
        Prayer[] prayers = Prayer.values();

        for (int i = 0; i < prayers.length; i++) {
            PrayerCalculator.Entry entry = snapshot.entry(prayers[i]);
            if (entry == null) continue;

            boolean past = !entry.instant.isAfter(Instant.ofEpochMilli(model.nowMs));
            // Match the instant, not the name or date. An Isha from yesterday
            // can still be inside its grace after midnight; that must not light
            // today's Isha row.
            boolean active = entry.instant.equals(model.entry.instant);
            int bottom = i == prayers.length - 1 && !model.displayIsTomorrow
                    ? 0 : dp(8);
            addPrayerRow(entry, i + 1, active, past, bottom, model.grace && active);
        }

        // After the Isha grace the next prayer belongs to tomorrow. Keep
        // today's five times visible, then show that Fajr explicitly instead
        // of leaving the whole list without a lit row.
        if (model.displayIsTomorrow) {
            TextView tomorrow = new TextView(this);
            tomorrow.setText(R.string.tomorrow_label);
            tomorrow.setTextAppearance(R.style.LabelFaint);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dp(18);
            labelParams.bottomMargin = dp(8);
            labelParams.setMarginStart(dp(2));
            tomorrow.setLayoutParams(labelParams);
            prayerList.addView(tomorrow);

            addPrayerRow(snapshot.next, 1, true, false, 0, false);
        }
    }

    private void addPrayerRow(PrayerCalculator.Entry entry, int indexNumber,
                              boolean active, boolean past, int bottomMargin,
                              boolean now) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_prayer, prayerList, false);
        View marker = row.findViewById(R.id.pMarker);
        TextView index = row.findViewById(R.id.pIndex);
        TextView name = row.findViewById(R.id.pName);
        TextView time = row.findViewById(R.id.pTime);
        TextView status = row.findViewById(R.id.pStatus);

        index.setText(String.format(Locale.US, "%02d", indexNumber));
        name.setText(PrayerNames.display(this, entry.prayer));
        time.setText(UiText.timeText(this, entry.local, prefs.is24Hour(), 0.5f));

        row.setBackgroundResource(active
                ? R.drawable.bg_prayer_active
                : R.drawable.bg_option);

        if (active) {
            // A small dot and the status label carry the state; the row keeps
            // the same surface as the rest of the schedule so the red accent
            // stays an indicator rather than a large block of colour.
            marker.setVisibility(View.VISIBLE);
            name.setTextColor(getColor(R.color.fg));
            time.setTextColor(getColor(R.color.fg));
            index.setTextColor(getColor(R.color.accent_text));
            status.setVisibility(View.VISIBLE);
            status.setText(getString(now ? R.string.status_now : R.string.status_next));
        } else {
            marker.setVisibility(View.INVISIBLE);
            name.setTextColor(getColor(past ? R.color.fg_past : R.color.fg));
            time.setTextColor(getColor(past ? R.color.fg_past : R.color.fg));
            index.setTextColor(getColor(R.color.fg_dim));
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
        clockValue.setText(UiText.timeText(this, now, prefs.is24Hour(), 0.75f));
    }

    private void updateCountdown() {
        updateCountdown(WidgetModel.of(snapshot, System.currentTimeMillis()));
    }

    private void updateCountdown(WidgetModel model) {
        if (model.grace) {
            countdownRow.setVisibility(View.GONE);
            graceMessage.setVisibility(View.VISIBLE);
            graceMessage.setText(ElapsedText.full(this, model.elapsedMinutes,
                    model.prayer));
        } else {
            graceMessage.setVisibility(View.GONE);
            countdownRow.setVisibility(View.VISIBLE);
            long remaining = model.entryEpochMs() - model.nowMs;
            if (remaining < 0) remaining = 0;
            // The red "IN" beside this value already labels the countdown.
            countdownValue.setText(Format.duration(remaining));
        }

        progress.setProgress(model.progress);
        progressLabel.setText(UiText.progress(this, model.progress));
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
        lastTickMs = System.currentTimeMillis();
        ticker = new Runnable() {
            @Override
            public void run() {
                long nowMs = System.currentTimeMillis();
                if (snapshot != null) {
                    LocalDate today = Instant.ofEpochMilli(nowMs)
                            .atZone(snapshot.place.zone()).toLocalDate();
                    long remaining = snapshot.next.instant.toEpochMilli() - nowMs;
                    boolean graceEnded = lastTickMs < snapshot.graceEnd
                            && nowMs >= snapshot.graceEnd;
                    boolean zoneChanged = snapshot.place.gps
                            && !ZoneId.systemDefault().getId().equals(snapshot.place.zoneId);
                    // A manual clock change can move time backwards without
                    // crossing the next Adhan. The larger forward threshold
                    // tolerates a delayed main-thread tick.
                    boolean clockJumped = lastTickMs > 0L
                            && (nowMs < lastTickMs - 2_000L
                            || nowMs > lastTickMs + 120_000L);
                    lastTickMs = nowMs;
                    if (remaining <= 0 || graceEnded || zoneChanged || clockJumped
                            || !today.equals(snapshot.date)) {
                        refresh();
                    } else {
                        updateClock();
                        updateCountdown();
                    }
                } else {
                    lastTickMs = nowMs;
                }
                // The clock and countdown show minutes only, so wake the UI
                // on the next minute boundary instead of once a second.
                handler.postDelayed(this, 60_000L - (nowMs % 60_000L) + 100L);
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

    private void openQibla() {
        startActivity(new Intent(this, QiblaActivity.class));
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
    public void onLanguageChanged() {
        if (settingsDialog != null) {
            settingsDialog.dismiss();
            settingsDialog = null;
        }
        // Repaint all widgets and retitle the notification channel before
        // the activity comes back in the new locale. Preferences are stored
        // application-wide, so recreation cannot lose the selection.
        NotificationHelper.refreshChannel(this);
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
            toast(getString(R.string.toast_allow_notifications));
            openNotificationSettings();
            return;
        }
        prefs.setAlertsEnabled(true);
        PrayerAlarmScheduler.reschedule(this);
        toast(getString(R.string.toast_alerts_on));
    }

    @Override
    public void onAddWidget(WidgetSizeDialog.Kind kind) {
        pinWidget(kind);
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
        if (locationFetching) {
            if (userInitiated) showLocatingHint();
            return;
        }

        final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            if (userInitiated) toast(getString(R.string.toast_location_unavailable));
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
            if (userInitiated) toast(getString(R.string.toast_no_location_fix));
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
                if (userInitiated) toast(getString(R.string.toast_turn_on_location));
                return;
            }

            if (userInitiated) showLocatingHint();
            manager.requestLocationUpdates(provider, 0L, 0f, locationListener,
                    Looper.getMainLooper());
            handler.postDelayed(locationTimeout, 12_000L);
        } catch (SecurityException e) {
            cleanupLocationFetch();
            if (userInitiated) toast(getString(R.string.toast_location_permission_denied));
        } catch (IllegalArgumentException e) {
            cleanupLocationFetch();
            if (userInitiated) toast(getString(R.string.toast_turn_on_location));
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
        if (prefs != null) {
            Place place = prefs.getPlace();
            updateLocationHint(place != null && place.gps);
        }
    }

    private void useLocation(Location location, boolean announce) {
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();
        final String zoneId = ZoneId.systemDefault().getId();

        // Name it immediately from the bundled cities, then let the platform
        // geocoder refine it in the background. The widget shows this name, so
        // both passes go through applyChanges().
        Place near = Place.nearest(latitude, longitude, 150);
        String name = near != null ? near.name : Place.CURRENT_LOCATION;
        String country = near != null ? near.country : "";
        prefs.setPlace(new Place(name, country, latitude, longitude, zoneId, true));
        prefs.setLocationUpdatedAt(System.currentTimeMillis());
        applyChanges();
        if (announce) toast(getString(R.string.toast_location_updated));

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
                toast(getString(R.string.toast_alerts_on));
            } else {
                toast(getString(R.string.toast_notifications_blocked));
            }
            pendingAlerts = false;
            return;
        }

        if (requestCode == REQUEST_LOCATION) {
            pendingLocation = false;
            if (granted) {
                fetchLocation(true);
            } else {
                toast(getString(R.string.toast_location_permission_denied));
            }
        }
    }

    // ── widgets ─────────────────────────────────────────────────────────

    private void showWidgetSizes() {
        WidgetSizeDialog.show(this, this::pinWidget);
    }

    private void pinWidget(WidgetSizeDialog.Kind kind) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName component;
        switch (kind) {
            case MINIMAL:
                component = new ComponentName(this, MinimalPrayerWidgetProvider.class);
                break;
            case SMALL:
                component = new ComponentName(this, SmallPrayerWidgetProvider.class);
                break;
            case WIDE:
            default:
                component = new ComponentName(this, PrayerWidgetProvider.class);
                break;
        }
        if (manager.isRequestPinAppWidgetSupported()) {
            if (!manager.requestPinAppWidget(component, null, null)) {
                toast(getString(R.string.toast_pin_widget));
            }
        } else {
            toast(getString(R.string.toast_pin_widget));
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
            toast(getString(R.string.toast_open_notification_settings));
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
