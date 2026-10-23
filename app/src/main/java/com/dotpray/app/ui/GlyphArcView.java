package com.dotpray.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.dotpray.app.R;

/** Decorative concentric glyph arcs behind the next-prayer header. */
public class GlyphArcView extends View {

    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;

    public GlyphArcView(Context context) {
        super(context);
        init();
    }

    public GlyphArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(1f * density);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(getContext().getColor(R.color.arc));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() * 0.72f;
        float cy = getHeight() * 0.36f;

        arc.setAlpha(26);
        drawArc(canvas, cx, cy, getWidth() * 0.46f, 200f, 150f);
        arc.setAlpha(16);
        drawArc(canvas, cx, cy, getWidth() * 0.34f, 215f, 120f);
        arc.setAlpha(11);
        drawArc(canvas, cx, cy, getWidth() * 0.22f, 230f, 90f);
    }

    private void drawArc(Canvas canvas, float cx, float cy, float radius, float start, float sweep) {
        RectFHolder rect = new RectFHolder(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(rect.left, rect.top, rect.right, rect.bottom, start, sweep, false, arc);
    }

    private static final class RectFHolder {
        final float left, top, right, bottom;

        RectFHolder(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
