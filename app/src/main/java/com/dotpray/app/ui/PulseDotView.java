package com.dotpray.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.dotpray.app.R;

/**
 * A single red status dot that breathes.
 *
 * <p>The pulse is a nicety, not information: when the system asks for reduced
 * motion the dot stays still, and the animator only runs while the view is
 * actually on screen.
 */
public class PulseDotView extends View {

    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;
    private float phase;
    private ValueAnimator animator;

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
        ring.setColor(getContext().getColor(R.color.accent));
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(1f * density);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startPulse();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPulse();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            startPulse();
        } else {
            stopPulse();
        }
    }

    private void startPulse() {
        if (animator != null || !isAttachedToWindow() || !ValueAnimator.areAnimatorsEnabled()) {
            return;
        }
        ValueAnimator pulse = ValueAnimator.ofFloat(0f, 1f);
        pulse.setDuration(1900);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new LinearInterpolator());
        pulse.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        pulse.start();
        animator = pulse;
    }

    private void stopPulse() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        // Keep a quiet static ring instead of freezing at a random phase.
        if (phase != 0f) {
            phase = 0f;
            invalidate();
        }
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
