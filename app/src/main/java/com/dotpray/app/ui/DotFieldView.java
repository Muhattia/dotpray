package com.dotpray.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.dotpray.app.R;

/**
 * The faint dot matrix that sits behind every screen, in the spirit of a
 * monochrome dot grid.
 */
public class DotFieldView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;
    private float spacing;
    private float radius;
    private float scrollOffset;

    public DotFieldView(Context context) {
        super(context);
        init();
    }

    public DotFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        spacing = 22 * density;
        radius = 1.05f * density;
        paint.setColor(getContext().getColor(R.color.dot_field));
        paint.setAlpha(18);
        setWillNotDraw(false);
    }

    public void setScrollOffset(float offset) {
        float clamped = offset % spacing;
        if (Math.abs(clamped - scrollOffset) < 0.5f) return;
        scrollOffset = clamped;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float y = spacing - scrollOffset;
        while (y < h + spacing) {
            float x = spacing;
            while (x < w + spacing) {
                canvas.drawCircle(x, y, radius, paint);
                x += spacing;
            }
            y += spacing;
        }
    }
}
