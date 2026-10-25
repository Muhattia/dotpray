package com.dotpray.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.dotpray.app.data.Place;
import com.dotpray.app.data.PlaceNames;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.data.ThemeController;
import com.dotpray.app.data.UiText;
import com.dotpray.app.prayer.CompassMath;
import com.dotpray.app.prayer.Qibla;
import com.dotpray.app.ui.QiblaCompassView;

import java.util.Locale;

/**
 * The live Qibla compass.
 *
 * <p>The dial is driven by the rotation-vector sensor (or the accelerometer +
 * magnetometer pair on devices without one). The heading is corrected from
 * magnetic north to true north with the local declination, then low-pass
 * filtered for a calm dial. The Qibla bearing itself stays computed from the
 * saved place, so this screen needs no location permission of its own.
 *
 * <p>The screen follows the app's Light / Dark choice; the primary dark theme
 * is the near-black instrument, and the light theme keeps the same geometry
 * and single red mark on paper.
 */
public class QiblaActivity extends Activity implements SensorEventListener {

    /** Entering alignment needs this close a heading; leaving it allows a little slack. */
    private static final double ALIGN_TOLERANCE = 3.0;
    private static final double ALIGN_RELEASE = 4.5;
    private static final long SENSOR_START_TIMEOUT_MS = 2500L;

    private static final int FOOTER_ACQUIRING = 0;
    private static final int FOOTER_NO_SENSOR = 1;
    private static final int FOOTER_NO_HEADING = 2;
    private static final int FOOTER_CALIBRATE = 3;
    private static final int FOOTER_HOLD = 4;

    private PrayerPrefs prefs;
    private Place place;
    private double qiblaBearing;
    private double declination;

    private ScrollView scroll;
    private View content;
    private QiblaCompassView compass;
    private TextView toleranceLabel;
    private TextView placeLine;
    private TextView placeTag;
    private TextView bearingValue;
    private TextView headingValue;
    private TextView offsetValue;
    private TextView stateLabel;
    private View stateDot;
    private TextView footerHint;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private boolean sensorRegistered;
    private boolean noSensor;
    private boolean sensorTimedOut;
    private int sensorAccuracy = -2;

    private boolean hasHeading;
    private boolean aligned;
    private int lastHeadingDegrees = -1;
    private int lastOffsetDegrees = -1;
    private int lastStateRes = -1;
    private boolean lastStateAligned;
    private int footerMode = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompassMath.HeadingFilter headingFilter = new CompassMath.HeadingFilter();

    private final float[] rotationMatrix = new float[9];
    private final float[] remappedMatrix = new float[9];
    private final float[] orientation = new float[3];
    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private boolean hasGravity;
    private boolean hasMagnetic;

    private final Runnable sensorTimeout = () -> {
        if (!hasHeading) {
            sensorTimedOut = true;
            updateReadout(0f, false);
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
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

        setContentView(R.layout.activity_qibla);
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Api30.setLightSystemBars(window, !night);
            } catch (RuntimeException ignored) {
                // Icon tint is cosmetic; never take the screen down for it.
            }
        }

        bindViews();
        applyInsets();
        findViewById(R.id.btnQiblaBack).setOnClickListener(v -> finish());
        toleranceLabel.setText(getString(R.string.qibla_tolerance,
                String.valueOf(Math.round(ALIGN_TOLERANCE))));

        loadPlace();
        renderPlace();
        renderBearing();
        updateReadout(0f, false);
    }

    private void bindViews() {
        scroll = findViewById(R.id.qiblaScroll);
        content = findViewById(R.id.qiblaContent);
        compass = findViewById(R.id.qiblaCompass);
        toleranceLabel = findViewById(R.id.qiblaTolerance);
        placeLine = findViewById(R.id.qiblaPlace);
        placeTag = findViewById(R.id.qiblaPlaceTag);
        bearingValue = findViewById(R.id.qiblaBearingValue);
        headingValue = findViewById(R.id.qiblaHeadingValue);
        offsetValue = findViewById(R.id.qiblaOffsetValue);
        stateLabel = findViewById(R.id.qiblaState);
        stateDot = findViewById(R.id.qiblaStateDot);
        footerHint = findViewById(R.id.qiblaFooter);

        compass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void applyInsets() {
        final int baseLeft = content.getPaddingLeft();
        final int baseTop = content.getPaddingTop();
        final int baseRight = content.getPaddingRight();
        final int baseBottom = content.getPaddingBottom();

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                int[] bars = new int[4];
                Api30.systemBarInsets4(insets, bars);
                left = bars[0];
                top = bars[1];
                right = bars[2];
                bottom = bars[3];
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, 0, 0, 0);
            content.setPadding(baseLeft + left, baseTop + top,
                    baseRight + right, baseBottom + bottom);
            return insets;
        });
        scroll.requestApplyInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = new PrayerPrefs(this);
        loadPlace();
        renderPlace();
        renderBearing();
        startSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopSensors();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopSensors();
        super.onDestroy();
    }

    // ── place + bearing ────────────────────────────────────────────────

    private void loadPlace() {
        place = prefs.getPlace();
        qiblaBearing = Qibla.bearing(place);
        try {
            declination = new GeomagneticField((float) place.latitude,
                    (float) place.longitude, 0f,
                    System.currentTimeMillis()).getDeclination();
        } catch (RuntimeException e) {
            declination = 0.0;
        }
    }

    private void renderPlace() {
        String city = UiText.upper(this, PlaceNames.shortName(this, place));
        String coordinates = UiText.coordinates(this, place);
        placeLine.setText(getString(R.string.location_detail, city, coordinates));
        placeTag.setVisibility(place.gps ? View.GONE : View.VISIBLE);
    }

    private void renderBearing() {
        bearingValue.setText(String.format(Locale.US, "%03d°",
                CompassMath.paddedDegrees(qiblaBearing)));
        compass.setQiblaBearing((float) CompassMath.normalize360(qiblaBearing));
    }

    // ── sensors ────────────────────────────────────────────────────────

    private void startSensors() {
        stopSensors();
        headingFilter.reset();
        hasHeading = false;
        aligned = false;
        sensorTimedOut = false;
        noSensor = false;
        sensorAccuracy = -2;
        hasGravity = false;
        hasMagnetic = false;
        lastHeadingDegrees = -1;
        lastOffsetDegrees = -1;
        lastStateRes = -1;
        footerMode = -1;

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            noSensor = true;
            updateReadout(0f, false);
            return;
        }

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor != null) {
            sensorRegistered = sensorManager.registerListener(this, rotationSensor,
                    SensorManager.SENSOR_DELAY_GAME);
        }

        if (!sensorRegistered) {
            rotationSensor = null;
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (accelerometer != null && magnetometer != null) {
                boolean accelOk = sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_DELAY_GAME);
                boolean magOk = accelOk && sensorManager.registerListener(this,
                        magnetometer, SensorManager.SENSOR_DELAY_GAME);
                if (!magOk) {
                    sensorManager.unregisterListener(this);
                    accelerometer = null;
                    magnetometer = null;
                } else {
                    sensorRegistered = true;
                }
            }
        }

        noSensor = !sensorRegistered;
        updateReadout(0f, false);
        if (!noSensor) {
            handler.postDelayed(sensorTimeout, SENSOR_START_TIMEOUT_MS);
        }
    }

    private void stopSensors() {
        handler.removeCallbacks(sensorTimeout);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        sensorManager = null;
        rotationSensor = null;
        accelerometer = null;
        magnetometer = null;
        sensorRegistered = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == rotationSensor) {
            setAccuracy(event.accuracy);
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            applyRotation(event.timestamp);
        } else if (event.sensor == accelerometer) {
            filterVector(gravity, event.values, hasGravity);
            hasGravity = true;
            if (hasMagnetic) computeFallback(event.timestamp);
        } else if (event.sensor == magnetometer) {
            filterVector(geomagnetic, event.values, hasMagnetic);
            hasMagnetic = true;
            setAccuracy(event.accuracy);
            if (hasGravity) computeFallback(event.timestamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor == rotationSensor || sensor == magnetometer) {
            setAccuracy(accuracy);
        }
    }

    private void filterVector(float[] out, float[] input, boolean initialized) {
        if (!initialized) {
            System.arraycopy(input, 0, out, 0, 3);
            return;
        }
        for (int i = 0; i < 3; i++) {
            out[i] += 0.16f * (input[i] - out[i]);
        }
    }

    private void computeFallback(long timestampNanos) {
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            return;
        }
        applyRotation(timestampNanos);
    }

    private void applyRotation(long timestampNanos) {
        boolean remapped;
        switch (displayRotation()) {
            case Surface.ROTATION_90:
                remapped = SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remappedMatrix);
                break;
            case Surface.ROTATION_180:
                remapped = SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                        remappedMatrix);
                break;
            case Surface.ROTATION_270:
                remapped = SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remappedMatrix);
                break;
            default:
                System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9);
                remapped = true;
                break;
        }
        if (!remapped) {
            System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9);
        }

        SensorManager.getOrientation(remappedMatrix, orientation);
        double magnetic = Math.toDegrees(orientation[0]);
        double trueNorth = CompassMath.trueHeading(magnetic, declination);

        boolean first = !hasHeading;
        boolean changed = headingFilter.update(trueNorth, timestampNanos);
        if (!hasHeading) {
            hasHeading = true;
            sensorTimedOut = false;
        }
        if (first || changed) {
            updateReadout((float) headingFilter.value(), true);
        }
    }

    private void setAccuracy(int accuracy) {
        if (accuracy == sensorAccuracy) return;
        sensorAccuracy = accuracy;
        updateFooter();
    }

    @SuppressWarnings("deprecation")
    private int displayRotation() {
        // The activity is recreated on an orientation change, so the current
        // display rotation is the one the screen top is using.
        return getWindowManager().getDefaultDisplay().getRotation();
    }

    // ── readout ────────────────────────────────────────────────────────

    private void updateReadout(float heading, boolean live) {
        boolean firstHeading = live && lastHeadingDegrees < 0;
        int headingDegrees = live ? CompassMath.paddedDegrees(heading) : -1;
        double delta = live ? CompassMath.shortestDelta(heading, qiblaBearing) : 0.0;
        int offsetDegrees = live
                ? ((int) Math.round(Math.abs(delta))) % 360
                : -1;

        boolean nextAligned = false;
        if (live) {
            double magnitude = Math.abs(delta);
            nextAligned = aligned
                    ? magnitude <= ALIGN_RELEASE
                    : magnitude <= ALIGN_TOLERANCE;
        }

        if (live) {
            if (headingDegrees != lastHeadingDegrees) {
                headingValue.setText(String.format(Locale.US, "%03d°", headingDegrees));
            }
            if (offsetDegrees != lastOffsetDegrees) {
                offsetValue.setText(String.format(Locale.US, "%03d°", offsetDegrees));
            }
        } else {
            headingValue.setText(R.string.qibla_no_value);
            offsetValue.setText(R.string.qibla_no_value);
        }

        int stateRes;
        if (live) {
            stateRes = nextAligned ? R.string.qibla_aligned
                    : delta >= 0 ? R.string.qibla_turn_right
                    : R.string.qibla_turn_left;
        } else {
            stateRes = noSensor || sensorTimedOut
                    ? R.string.qibla_bearing_only
                    : R.string.qibla_acquiring_short;
        }
        if (stateRes != lastStateRes) {
            stateLabel.setText(stateRes);
            lastStateRes = stateRes;
        }
        if (lastStateAligned != nextAligned) {
            stateLabel.setTextColor(getColor(nextAligned
                    ? R.color.accent_text : R.color.fg));
            stateDot.setBackgroundResource(nextAligned
                    ? R.drawable.bg_dot : R.drawable.bg_dot_dim);
            lastStateAligned = nextAligned;
        }

        if (live && !firstHeading && nextAligned && !aligned) {
            stateLabel.announceForAccessibility(getString(R.string.qibla_a11y_aligned));
            compass.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }

        lastHeadingDegrees = headingDegrees;
        lastOffsetDegrees = offsetDegrees;
        aligned = nextAligned;
        compass.setHeading(live ? heading : 0f, live);
        compass.setAligned(nextAligned);
        updateFooter();
    }

    private void updateFooter() {
        int mode;
        if (noSensor) {
            mode = FOOTER_NO_SENSOR;
        } else if (sensorTimedOut && !hasHeading) {
            mode = FOOTER_NO_HEADING;
        } else if (!hasHeading) {
            mode = FOOTER_ACQUIRING;
        } else if (sensorAccuracy != -2
                && sensorAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
            mode = FOOTER_CALIBRATE;
        } else {
            mode = FOOTER_HOLD;
        }

        if (mode == footerMode) return;
        footerMode = mode;

        // Bearing-only means there is no live heading to align, so the
        // tolerance badge would advertise a state the screen cannot reach.
        toleranceLabel.setVisibility(mode == FOOTER_NO_SENSOR
                || mode == FOOTER_NO_HEADING ? View.INVISIBLE : View.VISIBLE);

        switch (mode) {
            case FOOTER_NO_SENSOR:
                footerHint.setText(R.string.qibla_no_sensor);
                break;
            case FOOTER_NO_HEADING:
                footerHint.setText(R.string.qibla_no_heading);
                break;
            case FOOTER_CALIBRATE:
                footerHint.setText(R.string.qibla_calibrate);
                break;
            case FOOTER_HOLD:
                footerHint.setText(R.string.qibla_hold_flat);
                break;
            case FOOTER_ACQUIRING:
            default:
                footerHint.setText(R.string.qibla_acquiring);
                break;
        }
        footerHint.setTextColor(getColor(mode == FOOTER_CALIBRATE
                ? R.color.fg_dim : R.color.fg_faint));
    }

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }
}
