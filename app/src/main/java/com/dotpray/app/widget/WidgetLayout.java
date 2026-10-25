package com.dotpray.app.widget;

/**
 * Design-space geometry for the widget variants.
 *
 * <p>The renderers scale these compositions uniformly into whatever pixel size
 * the launcher reports. All three providers are resizable, so the wide card
 * drops its schedule and date lines when it is given a short cell rectangle,
 * while the compact cards keep their centred stack.
 *
 * <p>Baselines below are chosen from the two faces' actual ink, not from an
 * even baseline grid. Doto's digits end almost exactly on the baseline, while
 * Alexandria's Arabic descenders hang below it. The wide hero keeps the
 * 44 / 84.5 / 103.5 dp name / time / countdown baselines so the time stays on
 * the same row as the Gregorian date, while the 35 sp time leaves clean air
 * above the smaller digits and the countdown stays tight below them. Shifting
 * the Arabic name up 1.5 dp is an optical alignment, not a baseline change: it
 * centres the taller Arabic body on the same row as the wall clock and keeps
 * descenders clear of the time.
 */
public final class WidgetLayout {

    public static final float WIDE_WIDTH_DP = 326f;
    public static final float WIDE_FULL_HEIGHT_DP = 160f;
    public static final float WIDE_SHORT_HEIGHT_DP = 104f;
    public static final float WIDE_FULL_MIN_WIDTH_DP = 280f;
    // The full composition was 150 dp tall at a 128 dp threshold. Scaling
    // that minimum density to the new 160 dp composition gives 136 dp, so a
    // short cell drops the strip instead of squeezing the larger layout.
    public static final float WIDE_FULL_MIN_HEIGHT_DP = 136f;
    public static final float SMALL_SIZE_DP = 150f;

    public static final float WIDE_PAD_DP = 16f;
    public static final float WIDE_HEADER_BASE_DP = 17f;
    public static final float WIDE_LEFT_COLUMN_DP = 150f;

    // Hero rows. The 10.1 dp above the header ink and below the schedule
    // times is deliberate: it makes the full card's top and bottom margins
    // match instead of leaving the schedule pressed against the bottom edge.
    public static final float WIDE_NAME_BASE_DP = 44f;
    public static final float WIDE_TIME_BASE_DP = 84.5f;
    public static final float WIDE_COUNT_BASE_DP = 103.5f;

    // The right column keeps the same four rows as the hero: the wall clock
    // shares the name row, the Gregorian date shares the time row, and the
    // Hijri date shares the countdown row.
    public static final float WIDE_CLOCK_BASE_DP = 44f;
    public static final float WIDE_DATE_BASE_DP = 84.5f;
    public static final float WIDE_HIJRI_BASE_DP = 103.5f;

    // Top edge of the 2 dp dots. The rail sits 8-11 dp below the countdown ink
    // and 7 dp above the schedule label ink, so it separates the hero from the
    // strip without becoming a third line of type.
    public static final float WIDE_RAIL_DP = 114f;
    public static final float WIDE_SCHEDULE_NAME_BASE_DP = 132f;
    public static final float WIDE_SCHEDULE_TIME_BASE_DP = 150f;

    public static final float WIDE_NAME_SIZE_SP = 22f;
    // 35 sp is 16.7% below the previous 42 sp hero time. The shared baselines
    // and the 22 sp wall clock stay put, so only the wide card's main clock
    // becomes lighter; the short and compact cards use their own sizes.
    public static final float WIDE_TIME_SIZE_SP = 35f;
    public static final float WIDE_COUNT_SIZE_SP = 14f;
    public static final float WIDE_CLOCK_SIZE_SP = 22f;

    // A short card keeps the same hierarchy at a smaller optical scale: 20 /
    // 34 / 11.5 sp, with the rail still fully inside the card. At 104 dp the
    // previous 24 / 42 sp hero had only 4-6 dp of air between ink.
    public static final float WIDE_SHORT_HEADER_BASE_DP = 14.5f;
    public static final float WIDE_SHORT_NAME_BASE_DP = 36f;
    public static final float WIDE_SHORT_TIME_BASE_DP = 71f;
    public static final float WIDE_SHORT_COUNT_BASE_DP = 87f;
    public static final float WIDE_SHORT_RAIL_DP = 96f;
    public static final float WIDE_SHORT_NAME_SIZE_SP = 20f;
    public static final float WIDE_SHORT_TIME_SIZE_SP = 34f;
    public static final float WIDE_SHORT_COUNT_SIZE_SP = 11.5f;
    public static final float WIDE_SHORT_CLOCK_SIZE_SP = 20f;

    /** Optical lift for Alexandria's taller Arabic body on the shared row. */
    public static final float RTL_NAME_SHIFT_DP = 1.5f;

    // Compact stack: the name, time and countdown are centred as one block.
    // The normal stack leaves 42 dp above and 40 dp below the ink, so it stays
    // optically centred even when the minimal variant has no rail.
    public static final float SMALL_RAIL_DP = 128f;
    public static final float SMALL_GRACE_RAIL_DP = 134f;
    public static final float SMALL_NAME_SIZE_SP = 20f;
    public static final float SMALL_TIME_SIZE_SP = 35f;
    public static final float SMALL_COUNT_SIZE_SP = 12f;
    public static final float SMALL_COUNT_BASE_DP = 110f;
    public static final float SMALL_GRACE_LINE1_DP = 101f;
    public static final float SMALL_GRACE_LINE2_DP = 120f;
    public static final float SMALL_GRACE_LINE1_SIZE_SP = 14f;
    public static final float SMALL_GRACE_LINE2_SIZE_SP = 12f;

    private WidgetLayout() {
    }

    /** True when a resize still has room for the dates and schedule strip. */
    public static boolean wideIsFull(float widthDp, float heightDp) {
        return widthDp >= WIDE_FULL_MIN_WIDTH_DP
                && heightDp >= WIDE_FULL_MIN_HEIGHT_DP;
    }

    public static float smallNameBaseDp(boolean grace) {
        return grace ? 48f : 56f;
    }

    public static float smallTimeBaseDp(boolean grace) {
        return grace ? 84f : 92f;
    }

    public static float smallRailDp(boolean grace) {
        return grace ? SMALL_GRACE_RAIL_DP : SMALL_RAIL_DP;
    }
}
