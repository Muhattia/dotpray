package com.dotpray.app.ui;

/**
 * Geometry for a segmented dotted progress rail.
 *
 * <p>The app view and every widget renderer use the same dot spacing rules so
 * a percentage looks identical on every surface. Callers pass one consistent
 * unit (dp or px) for the span, radius and pitch; the returned centres use the
 * same unit. Dots are inset by one radius at each end, which keeps every circle
 * fully inside the rail instead of clipping the first or last one.
 */
public final class ProgressDots {

    /** Radius of one progress dot, in dp. */
    public static final float RADIUS_DP = 2.0f;
    /** Preferred centre-to-centre distance, in dp. */
    public static final float PITCH_DP = 9.0f;
    /** Upper bound for unusually wide layouts. */
    public static final int MAX_COUNT = 48;

    private ProgressDots() {
    }

    /** Number of dots that fit in {@code span} without touching each other. */
    public static int count(float span, float radius, float pitch) {
        float usable = span - radius * 2f;
        if (usable <= 0f) return 1;
        float step = Math.max(pitch, radius * 2f + 1f);
        int fit = (int) Math.floor(usable / step) + 1;
        return Math.max(1, Math.min(MAX_COUNT, fit));
    }

    /** Centre of dot {@code index}; index 0 is always at the start edge. */
    public static float center(float start, float end, float radius, int count, int index) {
        if (count <= 1) return (start + end) / 2f;
        float first = start + radius;
        float last = end - radius;
        float clamped = Math.max(0f, Math.min(count - 1, index));
        return first + (last - first) * clamped / (count - 1f);
    }

    /** Completed dots at this fraction, rounded to the nearest whole dot. */
    public static int completed(int count, float fraction) {
        if (count <= 0 || !(fraction > 0f)) return 0;
        if (fraction >= 1f) return count;
        return Math.max(0, Math.min(count, Math.round(count * fraction)));
    }

    /**
     * Whether dot {@code index} is complete. Index 0 is the left edge of the
     * rail; RTL therefore fills from the highest index down.
     */
    public static boolean isComplete(int index, int count, int completed, boolean rtl) {
        if (completed <= 0) return false;
        if (completed >= count) return true;
        return rtl ? index >= count - completed : index < completed;
    }
}
