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

import java.util.ArrayList;
import java.util.List;

import com.dotpray.app.R;

/** A monochrome list picker used for cities and calculation methods. */
public final class ListPickerDialog {

    public interface OnPick {
        void onPick(int index);
    }

    private ListPickerDialog() {
    }

    public static Dialog show(Context context, String title, String subtitle,
                              List<String> titles, List<String> subs,
                              int selected, OnPick callback) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(context).inflate(R.layout.dialog_list, null, false);
        content.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setContentView(content);

        ((TextView) content.findViewById(R.id.listTitle)).setText(title);
        ((TextView) content.findViewById(R.id.listSubtitle)).setText(subtitle);

        LinearLayout container = content.findViewById(R.id.listContainer);
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < titles.size(); i++) {
            final int index = i;
            View row = inflater.inflate(R.layout.item_option, container, false);
            ((TextView) row.findViewById(R.id.optTitle)).setText(titles.get(i));
            ((TextView) row.findViewById(R.id.optSub)).setText(subs.get(i));
            View mark = row.findViewById(R.id.optMark);
            mark.setVisibility(i == selected ? View.VISIBLE : View.GONE);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(context, 8);
            row.setLayoutParams(lp);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onPick(index);
            });
            container.addView(row);
        }

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.75f;
            window.setAttributes(params);
            window.setGravity(Gravity.CENTER);
            window.setLayout(screenWidth(context) - dp(context, 28),
                    (int) (screenHeight(context) * 0.80f));
        });
        dialog.show();
        return dialog;
    }

    public static List<String> toList(String... values) {
        List<String> list = new ArrayList<>(values.length);
        for (String value : values) list.add(value);
        return list;
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static int screenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    static int screenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
