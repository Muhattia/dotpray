package com.dotpray.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.dotpray.app.R;
import com.dotpray.app.alerts.NotificationHelper;
import com.dotpray.app.data.Language;
import com.dotpray.app.data.PrayerPrefs;
import com.dotpray.app.prayer.AsrMethod;

/** The preferences sheet. */
public class SettingsDialog {

    public interface Callback {
        void onThemeChanged();

        void onLanguageChanged();

        void onAsrChanged();

        void onClockChanged(boolean use24Hour);

        void onAlertsChanged(boolean enabled);

        void onAddWidget(WidgetSizeDialog.Kind kind);
    }

    private final Activity activity;
    private final Callback callback;
    private final PrayerPrefs prefs;

    private Dialog dialog;
    private TextView languageArabic;
    private TextView languageEnglish;
    private TextView themeSystem;
    private TextView themeLight;
    private TextView themeDark;
    private TextView asrStandard;
    private TextView asrHanafi;
    private Switch clockSwitch;
    private Switch alertsSwitch;
    private TextView alertsHint;
    private ScrollView settingsScroll;
    private View rowAbout;
    private View aboutContent;
    private ImageView aboutArrow;

    public SettingsDialog(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.prefs = new PrayerPrefs(activity);
    }

    public void show() {
        dialog = new Dialog(activity, R.style.Theme_Dotpray_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_settings, null, false);
        content.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setContentView(content);
        settingsScroll = (ScrollView) content;

        themeSystem = content.findViewById(R.id.btnThemeSystem);
        themeLight = content.findViewById(R.id.btnThemeLight);
        themeDark = content.findViewById(R.id.btnThemeDark);
        languageArabic = content.findViewById(R.id.btnLanguageArabic);
        languageEnglish = content.findViewById(R.id.btnLanguageEnglish);
        asrStandard = content.findViewById(R.id.btnAsrStandard);
        asrHanafi = content.findViewById(R.id.btnAsrHanafi);
        clockSwitch = content.findViewById(R.id.switchClock);
        alertsSwitch = content.findViewById(R.id.switchAlerts);
        alertsHint = content.findViewById(R.id.alertsHint);
        aboutContent = content.findViewById(R.id.aboutContent);
        aboutArrow = content.findViewById(R.id.aboutArrow);
        rowAbout = content.findViewById(R.id.rowAbout);

        content.findViewById(R.id.btnCloseSettings).setOnClickListener(v -> dialog.dismiss());
        alertsHint.setOnClickListener(v -> openNotificationSettings());
        rowAbout.setOnClickListener(v -> toggleAbout());

        // Material rows are one target: tapping the label should work, not
        // just the small switch at the end.
        View rowClock = content.findViewById(R.id.rowClock);
        rowClock.setOnClickListener(v -> clockSwitch.setChecked(!clockSwitch.isChecked()));
        View rowAlerts = content.findViewById(R.id.rowAlerts);
        rowAlerts.setOnClickListener(v -> alertsSwitch.setChecked(!alertsSwitch.isChecked()));
        content.findViewById(R.id.btnWidgetWide).setOnClickListener(v ->
                callback.onAddWidget(WidgetSizeDialog.Kind.WIDE));
        content.findViewById(R.id.btnWidgetSmall).setOnClickListener(v ->
                callback.onAddWidget(WidgetSizeDialog.Kind.SMALL));
        content.findViewById(R.id.btnWidgetMinimal).setOnClickListener(v ->
                callback.onAddWidget(WidgetSizeDialog.Kind.MINIMAL));

        themeSystem.setOnClickListener(v -> selectTheme(PrayerPrefs.THEME_SYSTEM));
        themeLight.setOnClickListener(v -> selectTheme(PrayerPrefs.THEME_LIGHT));
        themeDark.setOnClickListener(v -> selectTheme(PrayerPrefs.THEME_DARK));

        languageArabic.setOnClickListener(v -> selectLanguage(Language.ARABIC));
        languageEnglish.setOnClickListener(v -> selectLanguage(Language.ENGLISH));

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

        // Configure the window before it is attached. Doing this from
        // onShow() made the sheet appear at the default dialog size for one
        // frame, then jump to its final width/height and dim.
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            int width = Math.min(ListPickerDialog.screenWidth(activity)
                    - ListPickerDialog.dp(activity, 28), ListPickerDialog.dp(activity, 560));
            int height = Math.min((int) (ListPickerDialog.screenHeight(activity) * 0.88f),
                    ListPickerDialog.dp(activity, 820));
            params.width = width;
            params.height = height;
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.75f;
            window.setAttributes(params);
        }
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null) dialog.dismiss();
    }

    /** Reflects an alert switch change that happened outside the dialog. */
    public void setAlertsChecked(boolean checked) {
        if (alertsSwitch != null) alertsSwitch.setChecked(checked);
        updateAlertsHint();
    }

    private void selectTheme(int mode) {
        prefs.setThemeMode(mode);
        updateThemeStyles();
        callback.onThemeChanged();
    }

    private void selectLanguage(String language) {
        boolean changed = !Language.normalize(language).equals(prefs.getLanguage());
        if (changed) prefs.setLanguage(language);
        updateLanguageStyles();
        if (changed) callback.onLanguageChanged();
    }

    private void refreshValues() {
        updateLanguageStyles();
        updateThemeStyles();
        updateAsrStyles();
        updateAlertsHint();
    }

    private void updateLanguageStyles() {
        boolean arabic = Language.isArabic(prefs.getLanguage());
        stylePill(languageArabic, arabic);
        stylePill(languageEnglish, !arabic);
    }

    private void updateAlertsHint() {
        if (alertsHint == null) return;
        alertsHint.setVisibility(NotificationHelper.notificationsEnabled(activity)
                ? View.GONE : View.VISIBLE);
    }

    private void toggleAbout() {
        if (aboutContent == null) return;
        boolean open = aboutContent.getVisibility() != View.VISIBLE;
        aboutContent.setVisibility(open ? View.VISIBLE : View.GONE);
        aboutArrow.animate().rotation(open ? 180f : 0f).setDuration(160L).start();
        // The row sits at the end of the sheet; bring the opened copy into
        // view instead of leaving the reader at the fold.
        if (open && settingsScroll != null && rowAbout != null) {
            int offset = Math.max(0, rowAbout.getTop()
                    - ListPickerDialog.dp(activity, 12));
            settingsScroll.post(() -> settingsScroll.smoothScrollTo(0, offset));
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            activity.startActivity(intent);
        } catch (RuntimeException ignored) {
            // The system settings screen is unavailable; the hint stays visible.
        }
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
