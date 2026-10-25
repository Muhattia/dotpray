package com.dotpray.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.dotpray.app.R;
import com.dotpray.app.data.UiText;
import com.dotpray.app.prayer.CompassMath;

/**
 * The Qibla dial: a north-up instrument whose dial turns under a fixed top
 * index, with the Qibla shown as the only red mark on the face.
 *
 * <p>The dial is built from four thin rings, a 5-degree dot scale, 10/30-degree
 * ticks, upright cardinal letters and a needle that passes behind those letters
 * the way a printed instrument would. No gradients, shadows or texture.
 */
public class QiblaCompassView extends View {

    private static final float OUTER_RING = 1.00f;
    private static final float TICK_RING = 0.92f;
    private static final float INNER_RING = 0.70f;
    private static final float HUB_RING = 0.46f;
    private static final float CARDINAL_RADIUS = 0.80f;
    private static final float TIP_RADIUS = 0.885f;

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final Path triangle = new Path();
    private final RectF arcRect = new RectF();

    private final float density;

    private int colorFg;
    private int colorFgDim;
    private int colorBg;
    private int colorRed;

    private Typeface numericFace;
    private Typeface arabicFace;
    private Typeface arabicBoldFace;

    private String north;
    private String east;
    private String south;
    private String west;
    private boolean arabic;

    private float heading;
    private boolean hasHeading;
    private float qiblaBearing;
    private boolean aligned;
    private float alignment;
    private ValueAnimator alignmentAnimator;

    public QiblaCompassView(Context context) {
        this(context, null);
    }

    public QiblaCompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        colorFg = getContext().getColor(R.color.fg);
        colorFgDim = getContext().getColor(R.color.fg_dim);
        colorBg = getContext().getColor(R.color.bg);
        colorRed = getContext().getColor(R.color.accent_text);

        numericFace = getResources().getFont(R.font.doto_bold);
        arabicFace = getResources().getFont(R.font.alexandria_regular);
        arabicBoldFace = getResources().getFont(R.font.alexandria_semibold);

        north = getContext().getString(R.string.north);
        east = getContext().getString(R.string.east);
        south = getContext().getString(R.string.south);
        west = getContext().getString(R.string.west);
        arabic = UiText.isArabic(getContext());

        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.BUTT);
        fill.setStyle(Paint.Style.FILL);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(numericFace);
        setWillNotDraw(false);
    }

    public void setQiblaBearing(float bearing) {
        qiblaBearing = (float) CompassMath.normalize360(bearing);
        invalidate();
    }

    public void setHeading(float value, boolean live) {
        heading = (float) CompassMath.normalize360(value);
        hasHeading = live;
        invalidate();
    }

    /**
     * The red rim grows into place over 180 ms. The compass itself is driven by
     * the sensor and never animated on top of the reading, so this is the one
     * authored state change on the surface.
     */
    public void setAligned(boolean value) {
        if (aligned == value) return;
        aligned = value;
        float target = value ? 1f : 0f;

        if (!ValueAnimator.areAnimatorsEnabled()) {
            if (alignmentAnimator != null) {
                alignmentAnimator.cancel();
                alignmentAnimator = null;
            }
            alignment = target;
            invalidate();
            return;
        }

        if (alignmentAnimator != null) alignmentAnimator.cancel();
        alignmentAnimator = ValueAnimator.ofFloat(alignment, target);
        alignmentAnimator.setDuration(180L);
        alignmentAnimator.addUpdateListener(animation -> {
            alignment = (float) animation.getAnimatedValue();
            invalidate();
        });
        alignmentAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (alignmentAnimator != null) {
            alignmentAnimator.cancel();
            alignmentAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) return;

        float cx = width / 2f;
        float cy = height / 2f;
        // The top index reaches 9.5 dp above the rim; keep it inside the
        // view bounds on square and landscape dials.
        float radius = Math.min(width, height) / 2f - dp(11f);
        if (radius <= dp(40f)) return;

        float dialRotation = hasHeading ? -heading : 0f;

        canvas.save();
        canvas.rotate(dialRotation, cx, cy);
        drawRings(canvas, cx, cy, radius);
        drawTickScale(canvas, cx, cy, radius);
        drawQiblaNeedle(canvas, cx, cy, radius);
        canvas.restore();

        drawCardinals(canvas, cx, cy, radius);
        drawPivot(canvas, cx, cy);
        if (hasHeading) {
            drawTopIndex(canvas, cx, cy, radius);
            if (alignment > 0f) drawAlignmentArc(canvas, cx, cy, radius);
        }
    }

    private void drawRings(Canvas canvas, float cx, float cy, float radius) {
        drawRing(canvas, cx, cy, radius * OUTER_RING, 1.2f, 190);
        drawRing(canvas, cx, cy, radius * TICK_RING, 1.0f, 55);
        drawRing(canvas, cx, cy, radius * INNER_RING, 1.0f, 30);
        drawRing(canvas, cx, cy, radius * HUB_RING, 1.0f, 60);
    }

    private void drawRing(Canvas canvas, float cx, float cy, float radius,
                          float widthDp, int alpha) {
        line.setColor(colorFg);
        line.setAlpha(alpha);
        line.setStrokeWidth(dp(widthDp));
        canvas.drawCircle(cx, cy, radius, line);
    }

    private void drawTickScale(Canvas canvas, float cx, float cy, float radius) {
        fill.setColor(colorFg);
        fill.setAlpha(55);
        float dotRadius = dp(0.55f);
        for (int degrees = 5; degrees < 360; degrees += 10) {
            canvas.drawCircle(px(cx, radius * 0.958f, degrees),
                    py(cy, radius * 0.958f, degrees), dotRadius, fill);
        }

        line.setColor(colorFg);
        line.setStrokeWidth(dp(0.8f));
        line.setAlpha(70);
        for (int degrees = 10; degrees < 360; degrees += 10) {
            if (degrees % 30 == 0) continue;
            drawRadial(canvas, cx, cy, radius * 0.935f, radius * 0.985f, degrees);
        }

        line.setStrokeWidth(dp(1.25f));
        line.setAlpha(175);
        for (int degrees = 0; degrees < 360; degrees += 30) {
            drawRadial(canvas, cx, cy, radius * 0.900f, radius * 0.985f, degrees);
        }

        // North gets the longest mark so it remains the reference even when
        // the dial has rotated most of the way around.
        line.setStrokeWidth(dp(1.45f));
        line.setAlpha(235);
        drawRadial(canvas, cx, cy, radius * 0.885f, radius * 1.0f, 0);
    }

    private void drawQiblaNeedle(Canvas canvas, float cx, float cy, float radius) {
        float tip = radius * TIP_RADIUS;
        float stem = radius * 0.84f;

        line.setColor(colorRed);
        line.setAlpha(235);
        line.setStrokeWidth(dp(1.4f + 0.4f * alignment));
        line.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(px(cx, radius * 0.025f, qiblaBearing),
                py(cy, radius * 0.025f, qiblaBearing),
                px(cx, stem, qiblaBearing),
                py(cy, stem, qiblaBearing), line);

        line.setStrokeWidth(dp(1.2f));
        line.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(px(cx, tip, qiblaBearing),
                py(cy, tip, qiblaBearing),
                dp(6.2f + 1.2f * alignment), line);

        fill.setColor(colorRed);
        fill.setAlpha(245);
        canvas.drawCircle(px(cx, tip, qiblaBearing),
                py(cy, tip, qiblaBearing),
                dp(2.5f + 0.5f * alignment), fill);

        line.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawCardinals(Canvas canvas, float cx, float cy, float radius) {
        float cardinalRadius = radius * CARDINAL_RADIUS;

        drawCardinal(canvas, cx, cy, cardinalRadius, north, 0f, true);
        drawCardinal(canvas, cx, cy, cardinalRadius, east, 90f, false);
        drawCardinal(canvas, cx, cy, cardinalRadius, south, 180f, false);
        drawCardinal(canvas, cx, cy, cardinalRadius, west, 270f, false);
    }

    private void drawCardinal(Canvas canvas, float cx, float cy, float radius,
                              String label, float bearing, boolean primary) {
        float screenAngle = hasHeading ? bearing - heading : bearing;
        float x = px(cx, radius, screenAngle);
        float y = py(cy, radius, screenAngle);

        float size = primary ? 12f : (arabic ? 10.5f : 10f);
        text.setTypeface(primary && arabic ? arabicBoldFace
                : arabic ? arabicFace : numericFace);
        text.setTextSize(sp(size));
        text.setColor(primary ? colorFg : colorFgDim);
        text.setAlpha(primary ? 255 : 225);
        text.getFontMetrics(fontMetrics);

        float maskRadius = Math.max(primary ? dp(7.5f) : dp(6.5f),
                Math.max(text.measureText(label) / 2f,
                        (fontMetrics.descent - fontMetrics.ascent) / 2f) + dp(1.4f));
        fill.setColor(colorBg);
        fill.setAlpha(255);
        canvas.drawCircle(x, y, maskRadius, fill);

        float baseline = y - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(label, x, baseline, text);
    }

    private void drawPivot(Canvas canvas, float cx, float cy) {
        fill.setColor(colorBg);
        fill.setAlpha(255);
        canvas.drawCircle(cx, cy, dp(3.4f), fill);

        line.setColor(colorFgDim);
        line.setAlpha(150);
        line.setStrokeWidth(dp(1f));
        canvas.drawCircle(cx, cy, dp(3.4f), line);

        fill.setColor(colorFg);
        fill.setAlpha(220);
        canvas.drawCircle(cx, cy, dp(1.1f), fill);
    }

    private void drawTopIndex(Canvas canvas, float cx, float cy, float radius) {
        triangle.reset();
        triangle.moveTo(cx - dp(4.2f), cy - radius - dp(9.5f));
        triangle.lineTo(cx + dp(4.2f), cy - radius - dp(9.5f));
        triangle.lineTo(cx, cy - radius - dp(1.2f));
        triangle.close();

        fill.setColor(colorFg);
        fill.setAlpha(235);
        canvas.drawPath(triangle, fill);

        if (alignment > 0f) {
            fill.setColor(colorRed);
            fill.setAlpha((int) (255f * alignment));
            canvas.drawPath(triangle, fill);
        }
    }

    private void drawAlignmentArc(Canvas canvas, float cx, float cy, float radius) {
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        line.setColor(colorRed);
        line.setAlpha((int) (255f * alignment));
        line.setStrokeWidth(dp(2.2f));
        line.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(arcRect, 264f, 12f, false, line);
        line.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawRadial(Canvas canvas, float cx, float cy,
                            float inner, float outer, float degrees) {
        canvas.drawLine(px(cx, inner, degrees), py(cy, inner, degrees),
                px(cx, outer, degrees), py(cy, outer, degrees), line);
    }

    private static float px(float cx, float radius, double degrees) {
        return cx + (float) (Math.sin(Math.toRadians(degrees)) * radius);
    }

    private static float py(float cy, float radius, double degrees) {
        return cy - (float) (Math.cos(Math.toRadians(degrees)) * radius);
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
