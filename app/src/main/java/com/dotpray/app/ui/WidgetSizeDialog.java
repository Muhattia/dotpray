package com.dotpray.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dotpray.app.R;

/**
 * The three widget layouts. Each row pins its own AppWidgetProvider; all three
 * keep resize handles on the home screen.
 */
public final class WidgetSizeDialog {

    public enum Kind {
        WIDE, SMALL, MINIMAL
    }

    public interface OnSize {
        void onSize(Kind kind);
    }

    private WidgetSizeDialog() {
    }

    public static Dialog show(Activity activity, OnSize callback) {
        final Dialog dialog = new Dialog(activity, R.style.Theme_Dotpray_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_widget_picker, null, false);
        dialog.setContentView(content);

        ((TextView) content.findViewById(R.id.widgetPickerTitle))
                .setText(R.string.widget_chooser_title);
        ((TextView) content.findViewById(R.id.widgetPickerSubtitle))
                .setText(R.string.widget_chooser_subtitle);

        LinearLayout container = content.findViewById(R.id.widgetPickerOptions);
        addOption(activity, container, R.string.widget_size_small,
                R.string.widget_size_small_desc, dialog, callback, Kind.SMALL);
        addOption(activity, container, R.string.widget_size_minimal,
                R.string.widget_size_minimal_desc, dialog, callback, Kind.MINIMAL);
        addOption(activity, container, R.string.widget_size_wide,
                R.string.widget_size_wide_desc, dialog, callback, Kind.WIDE);

        // Configure the window before it is attached, exactly like the
        // settings sheet, so the dialog cannot appear at its default size for
        // one frame and then jump.
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = Math.min(ListPickerDialog.screenWidth(activity)
                    - ListPickerDialog.dp(activity, 28), ListPickerDialog.dp(activity, 560));
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.75f;
            window.setAttributes(params);
        }
        dialog.show();
        return dialog;
    }

    private static void addOption(Context context, LinearLayout container, int title,
                                  int subtitle, Dialog dialog, OnSize callback,
                                  Kind kind) {
        View row = LayoutInflater.from(context)
                .inflate(R.layout.item_option, container, false);
        ((TextView) row.findViewById(R.id.optTitle)).setText(title);
        ((TextView) row.findViewById(R.id.optSub)).setText(subtitle);
        row.findViewById(R.id.optMark).setVisibility(View.GONE);
        // The row is the button, so give screen readers the two lines as one
        // label instead of two unrelated text nodes.
        row.setContentDescription(context.getString(title) + ". "
                + context.getString(subtitle));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = ListPickerDialog.dp(context, 8);
        row.setLayoutParams(lp);

        row.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onSize(kind);
        });
        container.addView(row);
    }
}
