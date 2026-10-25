package com.dotpray.app.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dotpray.app.Api30;
import com.dotpray.app.R;
import com.dotpray.app.data.Place;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.data.ThemeController;
import com.dotpray.app.data.UiText;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.PrayerClock;

import java.time.Instant;

/**
 * Per-placement widget appearance, launched by the launcher with
 * {@link AppWidgetManager#ACTION_APPWIDGET_CONFIGURE}.
 *
 * <p>Android 12+ also opens this screen from the widget's long-press
 * <em>Edit</em> / <em>Widget settings</em> action because each provider
 * declares {@code android:widgetFeatures="reconfigurable"}. The choice is
 * stored under the placement's {@code appWidgetId}, so two copies of the same
 * provider can have different transparency levels.
 *
 * <p>A concrete subclass exists for each provider. The configuration intent
 * carries only the new id; on the first placement that id may not yet be bound
 * to its provider, so the variant cannot be read reliably from
 * {@link AppWidgetManager#getAppWidgetInfo(int)}.
 */
public abstract class WidgetConfigActivity extends Activity {

    private static final String TAG = "DotprayWidgetConfig";

    protected static final int KIND_WIDE = 0;
    protected static final int KIND_SMALL = 1;
    protected static final int KIND_MINIMAL = 2;

    private static final String STATE_TRANSPARENCY = "widget_transparency";

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private int selectedTransparency = WidgetPrefs.SOLID;

    private ScrollView scroll;
    private View content;
    private View previewStage;
    private ImageView previewCanvas;
    private TextView previewMode;
    private TextView variantTag;
    private SeekBar transparencyBar;
    private TextView transparencyValue;
    private Bitmap previewBitmap;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable previewTick;
    private boolean previewRequested;

    /** Which of the three providers opened this screen. */
    protected abstract int variant();

    @Override
    protected void attachBaseContext(Context newBase) {
        // The configuration screen belongs to the in-app language and theme,
        // not to whatever the launcher is using.
        super.attachBaseContext(ThemeController.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent launch = getIntent();
        appWidgetId = launch == null ? AppWidgetManager.INVALID_APPWIDGET_ID
                : launch.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        String kind = variantName();
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "onCreate: missing or invalid EXTRA_APPWIDGET_ID"
                    + " kind=" + kind + " intent=" + launch);
            // Still return the id extra when one is present, so the launcher
            // can associate the cancellation with the placement it started.
            setResult(RESULT_CANCELED, resultIntent());
            if (isDebuggable()) {
                Toast.makeText(this,
                        "Dotpray: widget configuration failed (appWidgetId="
                                + appWidgetId + ", kind=" + kind + ")",
                        Toast.LENGTH_LONG).show();
            }
            finish();
            return;
        }
        Log.i(TAG, "onCreate: appWidgetId=" + appWidgetId + " kind=" + kind);

        // Cancelling or backing out must leave the widget as it was.
        setResult(RESULT_CANCELED, resultIntent());
        configureWindow();
        setContentView(R.layout.activity_widget_config);
        setTitle(R.string.widget_settings_title);
        bindViews();

        int saved = savedInstanceState == null ? WidgetPrefs.SOLID
                : savedInstanceState.getInt(STATE_TRANSPARENCY, WidgetPrefs.SOLID);
        selectedTransparency = WidgetPrefs.clamp(savedInstanceState == null
                ? WidgetPrefs.getTransparency(this, appWidgetId) : saved);

        variantTag.setText(variantLabel());
        wireInteractions();
        applyInsets();
        updateSelection();
    }

    private void configureWindow() {
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
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Api30.setLightSystemBars(window, !night);
            } catch (RuntimeException ignored) {
                // Icon tint is cosmetic; never take the screen down for it.
            }
        }
    }

    private void bindViews() {
        scroll = findViewById(R.id.configScroll);
        content = findViewById(R.id.configContent);
        previewStage = findViewById(R.id.configPreviewStage);
        previewCanvas = findViewById(R.id.configPreviewCanvas);
        previewMode = findViewById(R.id.configPreviewMode);
        variantTag = findViewById(R.id.configVariantTag);
        transparencyBar = findViewById(R.id.configTransparencyBar);
        transparencyValue = findViewById(R.id.configTransparencyValue);
    }

    private void wireInteractions() {
        findViewById(R.id.btnConfigBack).setOnClickListener(v -> cancel());
        findViewById(R.id.btnConfigCancel).setOnClickListener(v -> cancel());
        findViewById(R.id.btnConfigApply).setOnClickListener(v -> apply());
        transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                setTransparency(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                requestPreview();
            }
        });
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
        renderPreview();
        startPreviewTick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreviewTick();
    }

    @Override
    protected void onDestroy() {
        stopPreviewTick();
        if (previewCanvas != null) previewCanvas.removeCallbacks(previewInvalidate);
        setPreviewBitmap(null);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TRANSPARENCY, selectedTransparency);
    }

    private void setTransparency(int level) {
        int next = WidgetPrefs.clamp(level);
        if (selectedTransparency == next) return;
        selectedTransparency = next;
        updateSelection();
        requestPreview();
    }

    private void updateSelection() {
        if (transparencyBar.getProgress() != selectedTransparency) {
            transparencyBar.setProgress(selectedTransparency);
        }
        transparencyValue.setText(levelLabel());
        transparencyBar.setContentDescription(UiText.format(this,
                R.string.widget_transparency_a11y, selectedTransparency));
        previewMode.setText(modeLabel());
    }

    private String levelLabel() {
        return UiText.format(this, R.string.widget_transparency_value,
                selectedTransparency);
    }

    private String modeLabel() {
        return UiText.format(this, R.string.widget_settings_preview_mode,
                selectedTransparency);
    }

    /** Stable diagnostic name; the UI label is localized separately. */
    private String variantName() {
        switch (variant()) {
            case KIND_SMALL:
                return "small";
            case KIND_MINIMAL:
                return "minimal";
            case KIND_WIDE:
            default:
                return "wide";
        }
    }

    private boolean isDebuggable() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private CharSequence variantLabel() {
        switch (variant()) {
            case KIND_SMALL:
                return getString(R.string.widget_small_button);
            case KIND_MINIMAL:
                return getString(R.string.widget_minimal_button);
            case KIND_WIDE:
            default:
                return getString(R.string.widget_wide_button);
        }
    }

    private void apply() {
        Log.i(TAG, "apply: appWidgetId=" + appWidgetId + " kind=" + variantName()
                + " transparency=" + selectedTransparency);
        WidgetPrefs.setTransparency(this, appWidgetId, selectedTransparency);
        // Commit the placement before the first paint. A renderer or
        // RemoteViews failure must not turn a valid configuration into a
        // cancelled addition; the provider's onUpdate can repaint later.
        setResult(RESULT_OK, resultIntent());
        try {
            WidgetUpdater.updateOne(this, appWidgetId);
        } catch (RuntimeException e) {
            Log.e(TAG, "apply: first paint failed after RESULT_OK,"
                    + " appWidgetId=" + appWidgetId + " kind=" + variantName(), e);
        }
        finish();
    }

    private void cancel() {
        setResult(RESULT_CANCELED, resultIntent());
        finish();
    }

    private Intent resultIntent() {
        return new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    }

    private void renderPreview() {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID
                || previewCanvas == null) {
            return;
        }

        PrayerPrefs prefs = new PrayerPrefs(this);
        Place place = prefs.syncGpsZone();
        Instant now = Instant.now();
        PrayerClock.Snapshot snapshot = PrayerClock.snapshot(
                now, place, CalcMethod.EGYPT, prefs.getAsr());
        boolean use24 = prefs.is24Hour();
        int transparency = selectedTransparency;
        Context themed = ThemeController.wrap(this);

        int[] px = WidgetUpdater.previewPixels(this, appWidgetId,
                fallbackWidthDp(), fallbackHeightDp());
        Bitmap next;
        switch (variant()) {
            case KIND_SMALL:
                next = WidgetRenderer.small(themed, snapshot, use24, now,
                        px[0], px[1], transparency);
                break;
            case KIND_MINIMAL:
                next = WidgetRenderer.minimal(themed, snapshot, use24, now,
                        px[0], px[1], transparency);
                break;
            case KIND_WIDE:
            default:
                next = WidgetRenderer.wide(themed, snapshot, use24, now,
                        px[0], px[1], transparency);
                break;
        }

        setPreviewBitmap(next);
        previewStage.setContentDescription(UiText.format(this,
                R.string.widget_settings_preview_a11y, selectedTransparency));
    }

    /**
     * Coalesces slider steps into one paint per frame. Rendering every
     * intermediate value synchronously would allocate dozens of bitmaps while
     * the thumb is moving.
     */
    private void requestPreview() {
        if (previewCanvas == null) {
            renderPreview();
            return;
        }
        if (previewRequested) return;
        previewRequested = true;
        previewCanvas.post(previewInvalidate);
    }

    private final Runnable previewInvalidate = () -> {
        previewRequested = false;
        renderPreview();
    };

    private void setPreviewBitmap(Bitmap next) {
        if (previewCanvas == null) {
            if (next != null) next.recycle();
            return;
        }
        Bitmap previous = previewBitmap;
        previewBitmap = next;
        previewCanvas.setImageBitmap(next);
        if (previous != null && previous != next) previous.recycle();
    }

    private void startPreviewTick() {
        stopPreviewTick();
        previewTick = () -> {
            renderPreview();
            schedulePreviewTick();
        };
        schedulePreviewTick();
    }

    private void schedulePreviewTick() {
        if (previewTick == null) return;
        long now = System.currentTimeMillis();
        handler.postDelayed(previewTick, 60_000L - (now % 60_000L) + 250L);
    }

    private void stopPreviewTick() {
        if (previewTick != null) handler.removeCallbacks(previewTick);
        previewTick = null;
        if (previewCanvas != null) previewCanvas.removeCallbacks(previewInvalidate);
        previewRequested = false;
    }

    private int fallbackWidthDp() {
        return variant() == KIND_WIDE ? 326 : 150;
    }

    private int fallbackHeightDp() {
        return variant() == KIND_WIDE ? 160 : 150;
    }

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}
