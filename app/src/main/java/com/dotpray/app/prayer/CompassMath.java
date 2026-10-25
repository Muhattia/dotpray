package com.dotpray.app.prayer;

/**
 * Angle rules and sensor smoothing for the Qibla compass.
 *
 * <p>Bearings use the navigation convention: 0° is north, the value grows
 * clockwise, and east is 90°. Keeping this maths free of Android types lets
 * the JVM test harness exercise wrap-around, turn direction and filtering.
 */
public final class CompassMath {

    private CompassMath() {
    }

    /** Folds any angle into [0, 360). */
    public static double normalize360(double degrees) {
        double value = degrees % 360.0;
        if (value < 0) value += 360.0;
        return value;
    }

    /**
     * Shortest signed rotation from one bearing to another, in (-180, 180].
     * A positive value means clockwise, so the Qibla is to the user's right.
     */
    public static double shortestDelta(double from, double to) {
        double delta = normalize360(to) - normalize360(from);
        if (delta > 180.0) delta -= 360.0;
        if (delta <= -180.0) delta += 360.0;
        return delta;
    }

    public static boolean aligned(double delta, double tolerance) {
        return Math.abs(delta) <= tolerance + 1e-9;
    }

    /** Converts a magnetic heading to true north using east-positive declination. */
    public static double trueHeading(double magnetic, double declination) {
        return normalize360(magnetic + declination);
    }

    /** Whole-number bearing where 360 collapses to 0, ready for %03d. */
    public static int paddedDegrees(double degrees) {
        return ((int) Math.round(normalize360(degrees))) % 360;
    }

    /**
     * Time-based exponential smoothing. A fixed alpha would behave differently
     * at 15 Hz and 50 Hz sensor rates; a 60 ms time constant stays responsive
     * while taking the jitter out of a magnetometer.
     *
     * <p>Updates travel through the shortest arc, so 350° → 10° never spins
     * the dial the long way around.
     */
    public static final class HeadingFilter {

        private static final double TIME_CONSTANT_SECONDS = 0.06;
        private static final double SNAP_GAP_SECONDS = 0.5;
        private static final double DEADBAND_DEGREES = 0.02;

        private double value;
        private long lastTimestampNanos;
        private boolean started;

        public void reset() {
            started = false;
            lastTimestampNanos = 0L;
        }

        /** @return true when the visible heading changed enough to redraw. */
        public boolean update(double heading, long timestampNanos) {
            double next = normalize360(heading);
            if (!started) {
                started = true;
                value = next;
                lastTimestampNanos = timestampNanos;
                return true;
            }

            double seconds = (timestampNanos - lastTimestampNanos) / 1_000_000_000.0;
            lastTimestampNanos = timestampNanos;
            if (seconds <= 0.0 || seconds > SNAP_GAP_SECONDS) {
                value = next;
                return true;
            }

            double delta = shortestDelta(value, next);
            if (Math.abs(delta) < DEADBAND_DEGREES) return false;

            double alpha = 1.0 - Math.exp(-seconds / TIME_CONSTANT_SECONDS);
            value = normalize360(value + delta * alpha);
            return true;
        }

        public double value() {
            return value;
        }

        public boolean started() {
            return started;
        }
    }
}
