package com.dotpray.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.dotpray.app.R;

/** Hairline progress rail showing how much of the wait has elapsed. */
public class ProgressLineView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;
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
        density = getResources().getDisplayMetrics().density;
        track.setColor(getContext().getColor(R.color.track));
        track.setStrokeWidth(1.5f * density);
        fill.setColor(getContext().getColor(R.color.accent));
        fill.setStrokeWidth(2.5f * density);
        fill.setStrokeCap(Paint.Cap.ROUND);
        cap.setColor(getContext().getColor(R.color.accent));
    }

    public void setProgress(float value) {
        float clamped = Math.max(0f, Math.min(1f, value));
        if (Math.abs(clamped - progress) < 0.002f) return;
        progress = clamped;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cy = h / 2f;
        float left = 1.5f * density;
        float right = w - 1.5f * density;

        canvas.drawLine(left, cy, right, cy, track);

        float end = left + (right - left) * progress;
        if (end > left + 0.5f) {
            canvas.drawLine(left, cy, end, cy, fill);
            canvas.drawCircle(end, cy, 2.6f * density, cap);
        }
    }
}
