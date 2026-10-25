package com.dotpray.app.prayer;

import com.dotpray.app.data.Place;

/**
 * JVM checks for the Qibla compass maths. Run after a normal build:
 * {@code javac --release 8 -encoding UTF-8 -cp build/classes -d build/test tools/TestQibla.java}
 * then {@code java -cp build/classes:build/test com.dotpray.app.prayer.TestQibla}.
 */
public final class TestQibla {

    private static int checks;
    private static int failures;

    private TestQibla() {
    }

    public static void main(String[] args) {
        normalization();
        shortestTurn();
        alignment();
        trueNorth();
        paddedReadings();
        bearing();
        headingFilter();

        System.out.println();
        if (failures == 0) {
            System.out.println("PASS " + checks + " checks");
        } else {
            System.out.println("FAIL " + failures + " of " + checks + " checks");
            System.exit(1);
        }
    }

    private static void normalization() {
        section("normalization");
        near("360 wraps to 0", 0.0, CompassMath.normalize360(360.0));
        near("720 wraps to 0", 0.0, CompassMath.normalize360(720.0));
        near("negative wraps up", 350.0, CompassMath.normalize360(-10.0));
        near("over 360 wraps down", 10.0, CompassMath.normalize360(370.0));
        near("fraction preserved", 359.5, CompassMath.normalize360(-0.5));
    }

    private static void shortestTurn() {
        section("shortest turn");
        near("east is right", 90.0, CompassMath.shortestDelta(0.0, 90.0));
        near("west is left", -90.0, CompassMath.shortestDelta(0.0, 270.0));
        near("across north right", 20.0, CompassMath.shortestDelta(350.0, 10.0));
        near("across north left", -20.0, CompassMath.shortestDelta(10.0, 350.0));
        near("cairo example", -78.0, CompassMath.shortestDelta(214.0, 136.0));
        near("opposite resolves to right", 180.0, CompassMath.shortestDelta(0.0, 180.0));
        near("opposite wraps consistently", 180.0, CompassMath.shortestDelta(180.0, 0.0));
    }

    private static void alignment() {
        section("alignment");
        check("exact is aligned", CompassMath.aligned(0.0, 3.0));
        check("inside is aligned", CompassMath.aligned(-2.9, 3.0));
        check("edge is aligned", CompassMath.aligned(3.0, 3.0));
        check("outside is not aligned", !CompassMath.aligned(3.1, 3.0));
        check("wrapped alignment works",
                CompassMath.aligned(CompassMath.shortestDelta(1.0, 359.0), 3.0));
    }

    private static void trueNorth() {
        section("true north");
        near("east declination adds", 5.0, CompassMath.trueHeading(355.0, 10.0));
        near("west declination subtracts", 355.0, CompassMath.trueHeading(5.0, -10.0));
        near("zero declination keeps heading", 136.0,
                CompassMath.trueHeading(136.0, 0.0));
    }

    private static void paddedReadings() {
        section("readings");
        eq("136 stays 136", 136, CompassMath.paddedDegrees(136.4));
        eq("359.6 becomes 0", 0, CompassMath.paddedDegrees(359.6));
        eq("negative becomes 359", 359, CompassMath.paddedDegrees(-1.0));
        eq("360 becomes 0", 0, CompassMath.paddedDegrees(360.0));
    }

    private static void bearing() {
        section("qibla bearing");
        Place cairo = new Place("Cairo", "EG", 30.0444, 31.2357, "Africa/Cairo");
        near("Cairo faces the Kaaba", 136.14, Qibla.bearing(cairo), 0.15);
        Place london = new Place("London", "UK", 51.5074, -0.1278, "Europe/London");
        near("London faces the Kaaba", 118.99, Qibla.bearing(london), 0.2);
        Place makkah = new Place("Makkah", "SA", 21.4225, 39.8262, "Asia/Riyadh");
        check("Makkah bearing is finite", Qibla.bearing(makkah) >= 0.0
                && Qibla.bearing(makkah) < 360.0);
    }

    private static void headingFilter() {
        section("heading filter");

        CompassMath.HeadingFilter filter = new CompassMath.HeadingFilter();
        filter.reset();
        check("first sample starts immediately", filter.update(350.0, 0L));
        near("first sample is exact", 350.0, filter.value());
        filter.update(10.0, 20_000_000L);
        check("crosses north without spinning",
                filter.value() > 340.0 || filter.value() < 20.0);

        for (int i = 2; i <= 100; i++) {
            filter.update(10.0, i * 20_000_000L);
        }
        check("converges after a second", Math.abs(
                CompassMath.shortestDelta(filter.value(), 10.0)) < 0.2);

        CompassMath.HeadingFilter reset = new CompassMath.HeadingFilter();
        reset.reset();
        reset.update(20.0, 10L);
        check("long sensor gap snaps", reset.update(200.0, 1_000_000_000L));
        near("long gap lands on new heading", 200.0, reset.value());

        CompassMath.HeadingFilter still = new CompassMath.HeadingFilter();
        still.reset();
        still.update(136.0, 0L);
        check("deadband skips invisible movement", !still.update(136.001, 20_000_000L));
    }

    // ── harness ────────────────────────────────────────────────────────

    private static void section(String name) {
        System.out.println();
        System.out.println("─ " + name);
    }

    private static void check(String label, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  ok  " + label);
        } else {
            failures++;
            System.out.println("  FAIL " + label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected.equals(actual)) {
            System.out.println("  ok  " + label);
        } else {
            failures++;
            System.out.println("  FAIL " + label + " (expected " + expected
                    + ", got " + actual + ")");
        }
    }

    private static void near(String label, double expected, double actual) {
        near(label, expected, actual, 1e-6);
    }

    private static void near(String label, double expected, double actual,
                             double tolerance) {
        checks++;
        if (Math.abs(expected - actual) <= tolerance) {
            System.out.println("  ok  " + label);
        } else {
            failures++;
            System.out.println("  FAIL " + label + " (expected " + expected
                    + ", got " + actual + ")");
        }
    }
}
