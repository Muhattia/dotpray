package com.dotpray.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.dotpray.app.R;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.prayer.AsrMethod;

/** The preferences sheet. */
public class SettingsDialog {

    public interface Callback {
        void onThemeChanged();

        void onAsrChanged();

        void onClockChanged(boolean use24Hour);

        void onAlertsChanged(boolean enabled);

        void onAddWidget(boolean wide);
    }

    private final Activity activity;
    private final Callback callback;
    private final PrayerPrefs prefs;

    private Dialog dialog;
    private TextView themeSystem;
    private TextView themeLight;
    private TextView themeDark;
    private TextView asrStandard;
    private TextView asrHanafi;
    private Switch clockSwitch;
    private Switch alertsSwitch;

    public SettingsDialog(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.prefs = new PrayerPrefs(activity);
    }

    public void show() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_settings, null, false);
        content.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setContentView(content);

        themeSystem = content.findViewById(R.id.btnThemeSystem);
        themeLight = content.findViewById(R.id.btnThemeLight);
        themeDark = content.findViewById(R.id.btnThemeDark);
        asrStandard = content.findViewById(R.id.btnAsrStandard);
        asrHanafi = content.findViewById(R.id.btnAsrHanafi);
        clockSwitch = content.findViewById(R.id.switchClock);
        alertsSwitch = content.findViewById(R.id.switchAlerts);

        content.findViewById(R.id.btnCloseSettings).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.btnWidgetWide).setOnClickListener(v -> callback.onAddWidget(true));
        content.findViewById(R.id.btnWidgetSmall).setOnClickListener(v -> callback.onAddWidget(false));

        themeSystem.setOnClickListener(v -> selectTheme(PrayerPrefs.THEME_SYSTEM));
        themeLight.setOnClickListener(v -> selectTheme(PrayerPrefs.THEME_LIGHT));
        themeDark.setOnClickListener(v -> selectTheme(PrayerPrefs.THEME_DARK));

        asrStandard.setOnClickListener(v -> {
            prefs.setAsr(AsrMethod.STANDARD);
            updateAsrStyles();
            callback.onAsrChanged();
        });
        asrHanafi.setOnClickListener(v -> {
            prefs.setAsr(AsrMethod.HANAFI);
            updateAsrStyles();
            callback.onAsrChanged();
        });

        clockSwitch.setChecked(prefs.is24Hour());
        clockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.set24Hour(isChecked);
                callback.onClockChanged(isChecked);
            }
        });

        alertsSwitch.setChecked(prefs.isAlertsEnabled());
        alertsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                callback.onAlertsChanged(isChecked);
            }
        });

        refreshValues();

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.75f;
            window.setAttributes(params);
            window.setGravity(Gravity.CENTER);
            window.setLayout(ListPickerDialog.screenWidth(activity) - ListPickerDialog.dp(activity, 28),
                    (int) (ListPickerDialog.screenHeight(activity) * 0.88f));
        });
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null) dialog.dismiss();
    }

    /** Reflects an alert switch change that happened outside the dialog. */
    public void setAlertsChecked(boolean checked) {
        if (alertsSwitch != null) alertsSwitch.setChecked(checked);
    }

    private void selectTheme(int mode) {
        prefs.setThemeMode(mode);
        updateThemeStyles();
        callback.onThemeChanged();
    }

    private void refreshValues() {
        updateThemeStyles();
        updateAsrStyles();
    }

    private void updateThemeStyles() {
        int mode = prefs.getThemeMode();
        stylePill(themeSystem, mode == PrayerPrefs.THEME_SYSTEM);
        stylePill(themeLight, mode == PrayerPrefs.THEME_LIGHT);
        stylePill(themeDark, mode == PrayerPrefs.THEME_DARK);
    }

    private void updateAsrStyles() {
        boolean standard = prefs.getAsr() == AsrMethod.STANDARD;
        stylePill(asrStandard, standard);
        stylePill(asrHanafi, !standard);
    }

    private void stylePill(TextView view, boolean selected) {
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_pill_white);
            view.setTextColor(activity.getColor(R.color.bg));
        } else {
            view.setBackgroundResource(R.drawable.bg_pill_outline);
            view.setTextColor(activity.getColor(R.color.fg));
        }
    }
}
