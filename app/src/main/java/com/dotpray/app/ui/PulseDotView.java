package com.dotpray.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.dotpray.app.R;

/** A single red status dot that breathes. */
public class PulseDotView extends View {

    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;
    private float phase;

    public PulseDotView(Context context) {
        super(context);
        init();
    }

    public PulseDotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        dot.setColor(getContext().getColor(R.color.accent));
        dot.setAntiAlias(true);
        ring.setColor(getContext().getColor(R.color.accent));
        ring.setAntiAlias(true);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(1f * density);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1900);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float base = Math.min(getWidth(), getHeight()) * 0.22f;

        ring.setAlpha((int) (70 * (1f - phase)));
        canvas.drawCircle(cx, cy, base + phase * 2.6f * density, ring);

        dot.setAlpha(255);
        canvas.drawCircle(cx, cy, base, dot);
    }
}
