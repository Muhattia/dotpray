package com.dotpray.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.dotpray.app.R;

/**
 * Segmented dotted progress rail. Completed dots use the accent colour and
 * remaining dots use the quiet track colour; the same geometry is shared with
 * the home-screen widgets.
 */
public class ProgressLineView extends View {

    private final Paint done = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint remaining = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float radius;
    private float pitch;
    private float progress;

    public ProgressLineView(Context context) {
        super(context);
        init();
    }

    public ProgressLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        radius = ProgressDots.RADIUS_DP * density;
        pitch = ProgressDots.PITCH_DP * density;
        done.setColor(getContext().getColor(R.color.accent));
        remaining.setColor(getContext().getColor(R.color.track));
    }

    public void setProgress(float value) {
        float clamped = Math.max(0f, Math.min(1f, value));
        if (Math.abs(clamped - progress) < 0.001f) return;
        progress = clamped;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= radius * 2f || h <= 0f) return;

        float start = radius;
        float end = w - radius;
        float cy = h / 2f;
        int count = ProgressDots.count(end - start, radius, pitch);
        int completed = ProgressDots.completed(count, progress);
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        for (int i = 0; i < count; i++) {
            float cx = ProgressDots.center(start, end, radius, count, i);
            boolean lit = ProgressDots.isComplete(i, count, completed, rtl);
            canvas.drawCircle(cx, cy, radius, lit ? done : remaining);
        }
    }
}
