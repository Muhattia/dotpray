package com.dotpray.app.prayer;

import com.dotpray.app.data.Language;
import com.dotpray.app.data.Place;
import com.dotpray.app.ui.ProgressDots;
import com.dotpray.app.widget.WidgetLayout;
import com.dotpray.app.widget.WidgetModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM tests for v1.3 grace periods, stable prayer keys and the widget layouts.
 *
 * <p>Run after a normal build:
 * {@code javac --release 8 -encoding UTF-8 -cp build/classes -d build/test tools/TestGrace.java}
 * then {@code java -Dfile.encoding=UTF-8 -cp build/classes:build/test
 * com.dotpray.app.prayer.TestGrace}. The test shares the package of
 * {@link PrayerClock} so it can build exact synthetic boundaries without an
 * Android device. Localized strings and plural grammar are checked separately
 * by {@code tools/TestLocale.java}, which reads the resource XML directly.
 */
public final class TestGrace {

    private static final Place CAIRO = new Place("Cairo", "EG", 30.0444, 31.2357,
            "Africa/Cairo");
    private static final Place LONDON = new Place("London", "GB", 51.5074, -0.1278,
            "Europe/London");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);

    private static int checks;
    private static int failures;

    private TestGrace() {
    }

    public static void main(String[] args) {
        languageRules();
        graceDurations();
        stableKeys();
        realGraceBoundaries();
        maghribGraceBoundary();
        ishaGraceAcrossMidnight();
        graceCappedAtNextAdhan();
        widgetModel();
        widgetLayouts();
        restartAndJumpSafety();

        System.out.println();
        if (failures == 0) {
            System.out.println("PASS " + checks + " checks");
        } else {
            System.out.println("FAIL " + failures + " of " + checks + " checks");
            System.exit(1);
        }
    }

    // ------------------------------------------------------------- language

    private static void languageRules() {
        section("language rules");
        eq("default is English", Language.ENGLISH, Language.DEFAULT);
        eq("null falls back to English", Language.ENGLISH,
                Language.normalize(null));
        eq("empty falls back to English", Language.ENGLISH,
                Language.normalize(""));
        eq("unknown falls back to English", Language.ENGLISH,
                Language.normalize("fr"));
        eq("case is not a second dialect", Language.ENGLISH,
                Language.normalize("AR"));
        eq("Arabic is preserved", Language.ARABIC, Language.normalize("ar"));

        check("Arabic is Arabic", Language.isArabic("ar"));
        check("English is not Arabic", !Language.isArabic("en"));
        check("Arabic is RTL", Language.isRtl("ar"));
        check("English is LTR", !Language.isRtl("en"));
        eq("Arabic interface locale", "ar", Language.locale("ar").getLanguage());
        eq("English interface locale", "en", Language.locale("en").getLanguage());
        eq("Arabic display case is untouched", "الفجر",
                Language.displayCase("ar", "الفجر"));
        eq("English display case is upper", "FAJR",
                Language.displayCase("en", "Fajr"));
        eq("null display text stays empty", "", Language.displayCase("en", null));
    }

    // ---------------------------------------------------------------- grace

    private static void graceDurations() {
        section("grace durations");
        eq("Fajr", 15, Prayer.FAJR.graceMinutes);
        eq("Dhuhr", 15, Prayer.DHUHR.graceMinutes);
        eq("Asr", 15, Prayer.ASR.graceMinutes);
        eq("Maghrib", 10, Prayer.MAGHRIB.graceMinutes);
        eq("Isha", 15, Prayer.ISHA.graceMinutes);
        eq("Maghrib millis", 600_000L, Prayer.MAGHRIB.graceMillis());
        eq("Fajr millis", 900_000L, Prayer.FAJR.graceMillis());
    }

    private static void stableKeys() {
        section("stable prayer keys");
        for (Prayer prayer : Prayer.values()) {
            eq(prayer.name() + " enum key", prayer,
                    Prayer.fromKey(prayer.name()));
            eq(prayer.name() + " lower-case key", prayer,
                    Prayer.fromKey(prayer.name().toLowerCase(java.util.Locale.US)));
        }
        // Alarms booked by Dotpray 1.2 stored the English display label. They
        // must survive the update instead of being dropped as an unknown key.
        eq("legacy Fajr", Prayer.FAJR, Prayer.fromKey("Fajr"));
        eq("legacy Dhuhr", Prayer.DHUHR, Prayer.fromKey("Dhuhr"));
        eq("legacy Asr", Prayer.ASR, Prayer.fromKey("Asr"));
        eq("legacy Maghrib", Prayer.MAGHRIB, Prayer.fromKey("Maghrib"));
        eq("legacy Isha", Prayer.ISHA, Prayer.fromKey("Isha"));
        eq("unknown key", null, Prayer.fromKey("Tahajjud"));
        eq("null key", null, Prayer.fromKey(null));
    }

    private static void realGraceBoundaries() {
        section("real Fajr grace boundaries (Cairo 2026-10-22)");
        // Use the calculator for the real instants, then probe exactly around
        // the Fajr Adhan and its 15-minute grace.
        List<PrayerCalculator.Entry> day = PrayerCalculator.calculate(DAY, CAIRO,
                CalcMethod.EGYPT, AsrMethod.STANDARD);
        PrayerCalculator.Entry fajr = day.get(0);
        PrayerCalculator.Entry dhuhr = day.get(1);

        long adhan = fajr.instant.toEpochMilli();
        long graceEnd = adhan + fajr.prayer.graceMillis();
        check("grace end is adhan + 15m",
                graceEnd == adhan + 900_000L);

        PrayerClock.Snapshot before = snapshotAt(adhan - 1L);
        check("1ms before: not grace", !before.graceActive(adhan - 1L));
        eq("1ms before: next is Fajr", Prayer.FAJR, before.next.prayer);
        check("1ms before: progress almost full", before.progress(adhan - 1L) > 0.99f);

        PrayerClock.Snapshot atAdhan = snapshotAt(adhan);
        check("at adhan: grace active", atAdhan.graceActive(adhan));
        eq("at adhan: display is Fajr", Prayer.FAJR,
                atAdhan.displayAt(adhan).prayer);
        eq("at adhan: elapsed 0", 0, atAdhan.elapsedMinutes(adhan));
        eq("at adhan: progress 100%", 1f, atAdhan.progress(adhan));
        check("at adhan: not tomorrow",
                !atAdhan.displayIsTomorrowAt(adhan));

        long minute14 = adhan + 14L * 60_000L + 59_000L;
        check("14m59s: still grace", atAdhan.graceActive(minute14));
        eq("14m59s: elapsed 14", 14, atAdhan.elapsedMinutes(minute14));
        eq("14m59s: progress 100%", 1f, atAdhan.progress(minute14));

        PrayerClock.Snapshot lastMs = snapshotAt(graceEnd - 1L);
        check("1ms before grace end: still grace",
                lastMs.graceActive(graceEnd - 1L));
        eq("last grace ms: elapsed 14", 14,
                lastMs.elapsedMinutes(graceEnd - 1L));
        eq("last grace ms: progress 100%", 1f,
                lastMs.progress(graceEnd - 1L));

        PrayerClock.Snapshot atEnd = snapshotAt(graceEnd);
        check("grace end: not grace", !atEnd.graceActive(graceEnd));
        eq("grace end: display Dhuhr", Prayer.DHUHR,
                atEnd.displayAt(graceEnd).prayer);
        eq("grace end: progress 0%", 0f, atEnd.progress(graceEnd));
        eq("grace end: elapsed resets for display", 0,
                WidgetModel.of(atEnd, graceEnd).elapsedMinutes);

        long after = graceEnd + 60_000L;
        PrayerClock.Snapshot afterEnd = snapshotAt(after);
        check("one minute later: progress > 0",
                afterEnd.progress(after) > 0f);
        check("one minute later: progress is small",
                afterEnd.progress(after) < 0.01f);
        check("one minute later: next is Dhuhr",
                afterEnd.next.prayer == Prayer.DHUHR);
        check("one minute later: previous is Fajr",
                afterEnd.previous.prayer == Prayer.FAJR);

        eq("next grace end is displayed prayer + grace",
                afterEnd.next.instant.toEpochMilli()
                        + afterEnd.next.prayer.graceMillis(),
                afterEnd.nextGraceEndMs(after));
        eq("during grace, next grace end is current boundary", graceEnd,
                atAdhan.nextGraceEndMs(atAdhan.graceEnd - 1L));
        check("Dhuhr instant differs from Fajr", !fajr.instant.equals(dhuhr.instant));
    }

    private static void maghribGraceBoundary() {
        section("Maghrib 10-minute grace");
        List<PrayerCalculator.Entry> day = PrayerCalculator.calculate(DAY, CAIRO,
                CalcMethod.EGYPT, AsrMethod.STANDARD);
        PrayerCalculator.Entry maghrib = day.get(3);
        long adhan = maghrib.instant.toEpochMilli();
        PrayerClock.Snapshot snapshot = snapshotAt(adhan + 1_000L);
        check("inside Maghrib grace", snapshot.graceActive(adhan + 1_000L));
        eq("Maghrib progress 100%", 1f,
                snapshot.progress(adhan + 1_000L));

        long end = adhan + 600_000L;
        PrayerClock.Snapshot after = snapshotAt(end);
        check("after 10m: grace over", !after.graceActive(end));
        eq("after 10m: next is Isha", Prayer.ISHA, after.next.prayer);
        eq("after 10m: progress 0", 0f, after.progress(end));
    }

    private static void ishaGraceAcrossMidnight() {
        section("Isha grace across midnight");
        LocalDate day = LocalDate.of(2026, 6, 21);
        ZoneId zone = LONDON.zone();
        List<PrayerCalculator.Entry> today = syntheticDay(day, zone,
                3, 0, 13, 0, 17, 0, 21, 0, 23, 55);
        List<PrayerCalculator.Entry> tomorrow = syntheticDay(day.plusDays(1), zone,
                3, 5, 13, 5, 17, 5, 21, 5, 23, 58);
        PrayerCalculator.Entry isha = today.get(4);
        PrayerCalculator.Entry fajr = tomorrow.get(0);

        long ishaAt = isha.instant.toEpochMilli();
        long graceEnd = ishaAt + Prayer.ISHA.graceMillis();
        check("constructed grace crosses midnight",
                graceEnd > day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());

        PrayerClock.Snapshot nearMidnight = new PrayerClock.Snapshot(day, LONDON,
                CalcMethod.EGYPT, AsrMethod.STANDARD, today, fajr, isha, true);
        long beforeMidnight = day.plusDays(1).atStartOfDay(zone)
                .toInstant().toEpochMilli() - 5_000L;
        check("23:59:55: still Isha grace",
                nearMidnight.graceActive(beforeMidnight));
        eq("23:59:55: display Isha", Prayer.ISHA,
                nearMidnight.displayAt(beforeMidnight).prayer);
        eq("23:59:55: elapsed 4", 4,
                nearMidnight.elapsedMinutes(beforeMidnight));
        eq("23:59:55: progress 100%", 1f,
                nearMidnight.progress(beforeMidnight));
        check("23:59:55: displayed Isha is today's, not tomorrow's",
                nearMidnight.displayAt(beforeMidnight).instant.equals(isha.instant));

        // At 00:05 the app has rolled to the new date. The previous prayer is
        // yesterday's Isha, which is still inside its grace.
        long afterMidnight = day.plusDays(1).atTime(0, 5).atZone(zone)
                .toInstant().toEpochMilli();
        PrayerClock.Snapshot rolled = new PrayerClock.Snapshot(day.plusDays(1),
                LONDON, CalcMethod.EGYPT, AsrMethod.STANDARD, tomorrow, fajr, isha,
                false);
        WidgetModel model = WidgetModel.of(rolled, afterMidnight);
        check("00:05: grace active", model.grace);
        eq("00:05: display Isha", Prayer.ISHA, model.prayer);
        eq("00:05: elapsed 10", 10, model.elapsedMinutes);
        check("00:05: not marked tomorrow", !model.displayIsTomorrow);
        eq("00:05: display time is yesterday's Isha",
                isha.instant.toEpochMilli(), model.entryEpochMs());
        check("00:05: today's Isha row is not the active instant",
                !tomorrow.get(4).instant.equals(model.entry.instant));

        PrayerClock.Snapshot ended = new PrayerClock.Snapshot(day.plusDays(1),
                LONDON, CalcMethod.EGYPT, AsrMethod.STANDARD, tomorrow, fajr, isha,
                false);
        WidgetModel endedModel = WidgetModel.of(ended, graceEnd);
        check("00:10: grace over after midnight", !endedModel.grace);
        eq("00:10: display today's Fajr", Prayer.FAJR, endedModel.prayer);
        eq("00:10: progress 0", 0f, endedModel.progress);
        check("00:10: Fajr is not tomorrow", !endedModel.displayIsTomorrow);
        check("00:10: no grace", !endedModel.grace);
    }

    private static void graceCappedAtNextAdhan() {
        section("grace never overlaps the following Adhan");
        LocalDate day = LocalDate.of(2026, 6, 21);
        ZoneId zone = LONDON.zone();
        List<PrayerCalculator.Entry> today = syntheticDay(day, zone,
                3, 0, 12, 0, 12, 5, 21, 0, 23, 0);
        List<PrayerCalculator.Entry> tomorrow = syntheticDay(day.plusDays(1), zone,
                3, 5, 13, 5, 17, 5, 21, 5, 23, 5);
        PrayerCalculator.Entry dhuhr = today.get(1);
        PrayerCalculator.Entry asr = today.get(2);
        long adhan = dhuhr.instant.toEpochMilli();
        long nextAdhan = asr.instant.toEpochMilli();
        PrayerClock.Snapshot snapshot = new PrayerClock.Snapshot(day, LONDON,
                CalcMethod.EGYPT, AsrMethod.STANDARD, today, asr, dhuhr, false);
        eq("grace end is capped at Asr", nextAdhan, snapshot.graceEnd);
        check("one ms before Asr still Dhuhr grace",
                snapshot.graceActive(nextAdhan - 1L));
        check("at Asr the grace is over", !snapshot.graceActive(nextAdhan));
        eq("at Asr display is Asr", Prayer.ASR,
                snapshot.displayAt(nextAdhan).prayer);
        eq("at Asr progress resets", 0f, snapshot.progress(nextAdhan));

        // The widget alarm for Dhuhr's grace must also stop at Asr rather
        // than booking a refresh 10 minutes after Asr has already arrived.
        PrayerClock.Snapshot beforeDhuhr = new PrayerClock.Snapshot(day, LONDON,
                CalcMethod.EGYPT, AsrMethod.STANDARD, today, dhuhr, today.get(0),
                false);
        eq("scheduled grace end is capped at Asr", nextAdhan,
                beforeDhuhr.nextGraceEndMs(adhan - 3_600_000L));
    }

    // ---------------------------------------------------------- widget model

    private static void widgetModel() {
        section("widget model, all variants");
        List<PrayerCalculator.Entry> day = PrayerCalculator.calculate(DAY, CAIRO,
                CalcMethod.EGYPT, AsrMethod.STANDARD);
        PrayerCalculator.Entry fajr = day.get(0);
        long adhan = fajr.instant.toEpochMilli();

        WidgetModel grace = WidgetModel.of(snapshotAt(adhan + 2L * 60_000L),
                adhan + 2L * 60_000L);
        check("grace flag", grace.grace);
        eq("grace prayer", Prayer.FAJR, grace.prayer);
        eq("grace elapsed", 2, grace.elapsedMinutes);
        eq("grace progress", 1f, grace.progress);
        eq("grace display time is the Adhan", adhan, grace.entryEpochMs());

        long end = adhan + Prayer.FAJR.graceMillis();
        WidgetModel next = WidgetModel.of(snapshotAt(end), end);
        check("after grace: not grace", !next.grace);
        eq("after grace prayer", Prayer.DHUHR, next.prayer);
        eq("after grace elapsed", 0, next.elapsedMinutes);
        eq("after grace progress", 0f, next.progress);
        eq("after grace time", day.get(1).instant.toEpochMilli(),
                next.entryEpochMs());

        long lastGrace = end - 1L;
        WidgetModel last = WidgetModel.of(snapshotAt(lastGrace), lastGrace);
        eq("last grace minute still full", 1f, last.progress);
        eq("last grace minute is 14", 14, last.elapsedMinutes);

        // After Isha the model points at tomorrow's Fajr and still resets the
        // rail to zero.
        long ishaAt = day.get(4).instant.toEpochMilli();
        long ishaEnd = ishaAt + Prayer.ISHA.graceMillis();
        WidgetModel tomorrow = WidgetModel.of(snapshotAt(ishaEnd), ishaEnd);
        check("after Isha grace: not grace", !tomorrow.grace);
        eq("after Isha grace: tomorrow flag", true, tomorrow.displayIsTomorrow);
        eq("after Isha grace: Fajr", Prayer.FAJR, tomorrow.prayer);
        eq("after Isha grace: progress", 0f, tomorrow.progress);
        eq("after Isha grace: Fajr time",
                snapshotAt(ishaEnd).next.instant.toEpochMilli(),
                tomorrow.entryEpochMs());
    }

    private static void widgetLayouts() {
        section("resizable widget geometry");
        eq("wide design width", 326f, WidgetLayout.WIDE_WIDTH_DP);
        eq("wide 4x2 design height", 160f, WidgetLayout.WIDE_FULL_HEIGHT_DP);
        eq("wide short design height", 104f, WidgetLayout.WIDE_SHORT_HEIGHT_DP);
        eq("wide full threshold width", 280f, WidgetLayout.WIDE_FULL_MIN_WIDTH_DP);
        eq("wide full threshold height", 136f, WidgetLayout.WIDE_FULL_MIN_HEIGHT_DP);
        eq("small compact design size", 150f, WidgetLayout.SMALL_SIZE_DP);
        eq("wide rail", 114f, WidgetLayout.WIDE_RAIL_DP);
        eq("wide schedule name base", 132f, WidgetLayout.WIDE_SCHEDULE_NAME_BASE_DP);
        eq("wide schedule time base", 150f, WidgetLayout.WIDE_SCHEDULE_TIME_BASE_DP);
        eq("wide name base", 44f, WidgetLayout.WIDE_NAME_BASE_DP);
        eq("wide time base", 84.5f, WidgetLayout.WIDE_TIME_BASE_DP);
        eq("wide count base", 103.5f, WidgetLayout.WIDE_COUNT_BASE_DP);
        eq("wide clock base", 44f, WidgetLayout.WIDE_CLOCK_BASE_DP);
        eq("wide date base", 84.5f, WidgetLayout.WIDE_DATE_BASE_DP);
        eq("wide hijri base", 103.5f, WidgetLayout.WIDE_HIJRI_BASE_DP);
        eq("wide name size", 22f, WidgetLayout.WIDE_NAME_SIZE_SP);
        eq("wide time size", 35f, WidgetLayout.WIDE_TIME_SIZE_SP);
        eq("wide count size", 14f, WidgetLayout.WIDE_COUNT_SIZE_SP);
        eq("wide clock size", 22f, WidgetLayout.WIDE_CLOCK_SIZE_SP);
        eq("wide short rail", 96f, WidgetLayout.WIDE_SHORT_RAIL_DP);
        eq("small rail", 128f, WidgetLayout.smallRailDp(false));
        eq("small grace rail", 134f, WidgetLayout.smallRailDp(true));

        float r = ProgressDots.RADIUS_DP;
        check("wide rail fits with dots", WidgetLayout.WIDE_RAIL_DP + r
                < WidgetLayout.WIDE_FULL_HEIGHT_DP);
        check("wide short rail fits with dots", WidgetLayout.WIDE_SHORT_RAIL_DP + r
                < WidgetLayout.WIDE_SHORT_HEIGHT_DP);
        check("default wide cells keep the full composition",
                WidgetLayout.wideIsFull(326f, 160f)
                        && WidgetLayout.wideIsFull(280f, 136f));
        check("shrunk wide cells switch to the short composition",
                !WidgetLayout.wideIsFull(326f, 135f)
                        && !WidgetLayout.wideIsFull(280f, 128f)
                        && !WidgetLayout.wideIsFull(250f, 160f)
                        && !WidgetLayout.wideIsFull(220f, 90f));
        check("small rail fits with dots", WidgetLayout.smallRailDp(false) + r
                < WidgetLayout.SMALL_SIZE_DP);
        check("small grace rail fits with dots", WidgetLayout.smallRailDp(true) + r
                < WidgetLayout.SMALL_SIZE_DP);
        check("small grace line 2 sits above rail",
                WidgetLayout.SMALL_GRACE_LINE2_DP
                        < WidgetLayout.smallRailDp(true));
        check("small grace line 1 sits above line 2",
                WidgetLayout.SMALL_GRACE_LINE1_DP
                        < WidgetLayout.SMALL_GRACE_LINE2_DP);

        // The wide card always keeps the full schedule: name, time, countdown,
        // rail, then the five-column strip.
        check("wide full stack ordered",
                WidgetLayout.WIDE_NAME_BASE_DP < WidgetLayout.WIDE_TIME_BASE_DP
                        && WidgetLayout.WIDE_TIME_BASE_DP
                        < WidgetLayout.WIDE_COUNT_BASE_DP
                        && WidgetLayout.WIDE_COUNT_BASE_DP
                        < WidgetLayout.WIDE_RAIL_DP);
        check("wide right column ordered",
                WidgetLayout.WIDE_HEADER_BASE_DP
                        < WidgetLayout.WIDE_CLOCK_BASE_DP
                        && WidgetLayout.WIDE_CLOCK_BASE_DP
                        < WidgetLayout.WIDE_DATE_BASE_DP
                        && WidgetLayout.WIDE_DATE_BASE_DP
                        < WidgetLayout.WIDE_HIJRI_BASE_DP);
        check("wide rows share baselines",
                WidgetLayout.WIDE_CLOCK_BASE_DP
                        == WidgetLayout.WIDE_NAME_BASE_DP
                        && WidgetLayout.WIDE_DATE_BASE_DP
                        == WidgetLayout.WIDE_TIME_BASE_DP
                        && WidgetLayout.WIDE_HIJRI_BASE_DP
                        == WidgetLayout.WIDE_COUNT_BASE_DP);
        check("wide right column is centred against the hero",
                Math.abs((WidgetLayout.WIDE_CLOCK_BASE_DP
                        + WidgetLayout.WIDE_HIJRI_BASE_DP) / 2f
                        - (WidgetLayout.WIDE_NAME_BASE_DP
                        + WidgetLayout.WIDE_COUNT_BASE_DP) / 2f) <= 4f);
        check("wide right column ends above the rail",
                WidgetLayout.WIDE_HIJRI_BASE_DP < WidgetLayout.WIDE_RAIL_DP);
        check("wide rail has breathing room above",
                WidgetLayout.WIDE_RAIL_DP - WidgetLayout.WIDE_COUNT_BASE_DP >= 8f);
        check("wide schedule clears the rail",
                WidgetLayout.WIDE_SCHEDULE_NAME_BASE_DP
                        > WidgetLayout.WIDE_RAIL_DP + 4f + 8f);
        check("wide schedule ordered",
                WidgetLayout.WIDE_SCHEDULE_NAME_BASE_DP
                        < WidgetLayout.WIDE_SCHEDULE_TIME_BASE_DP
                        && WidgetLayout.WIDE_SCHEDULE_TIME_BASE_DP
                        < WidgetLayout.WIDE_FULL_HEIGHT_DP);
        check("wide schedule cells have room",
                (WidgetLayout.WIDE_WIDTH_DP - WidgetLayout.WIDE_PAD_DP * 2f) / 5f
                        >= 56f);
        check("wide clock does not outrank the name and time",
                WidgetLayout.WIDE_CLOCK_SIZE_SP
                        <= WidgetLayout.WIDE_NAME_SIZE_SP
                        && WidgetLayout.WIDE_NAME_SIZE_SP
                        < WidgetLayout.WIDE_TIME_SIZE_SP);
        check("wide rail clears the schedule", WidgetLayout.WIDE_RAIL_DP + 6f
                < WidgetLayout.WIDE_FULL_HEIGHT_DP);
        check("wide short stack ordered",
                WidgetLayout.WIDE_SHORT_NAME_BASE_DP
                        < WidgetLayout.WIDE_SHORT_TIME_BASE_DP
                        && WidgetLayout.WIDE_SHORT_TIME_BASE_DP
                        < WidgetLayout.WIDE_SHORT_COUNT_BASE_DP
                        && WidgetLayout.WIDE_SHORT_COUNT_BASE_DP
                        < WidgetLayout.WIDE_SHORT_RAIL_DP);
        check("small stack ordered",
                WidgetLayout.smallNameBaseDp(false) < WidgetLayout.smallTimeBaseDp(false)
                        && WidgetLayout.smallTimeBaseDp(false) < 106f
                        && 106f < WidgetLayout.smallRailDp(false));
        check("small grace stack ordered",
                WidgetLayout.smallNameBaseDp(true) < WidgetLayout.smallTimeBaseDp(true)
                        && WidgetLayout.smallTimeBaseDp(true)
                        < WidgetLayout.SMALL_GRACE_LINE1_DP
                        && WidgetLayout.SMALL_GRACE_LINE1_DP
                        < WidgetLayout.SMALL_GRACE_LINE2_DP
                        && WidgetLayout.SMALL_GRACE_LINE2_DP
                        < WidgetLayout.smallRailDp(true));

        float wideMargin = 16f;
        check("wide rail has margins",
                wideMargin < WidgetLayout.WIDE_WIDTH_DP - wideMargin);
        check("small rail has margins",
                wideMargin < WidgetLayout.SMALL_SIZE_DP - wideMargin);
        check("wide columns have room",
                WidgetLayout.WIDE_LEFT_COLUMN_DP + wideMargin * 2f + 18f
                        < WidgetLayout.WIDE_WIDTH_DP);

        // Dotted progress: even spacing, no clipped end dots, and a mirror
        // direction that matches the layout.
        float wideSpan = WidgetLayout.WIDE_WIDTH_DP - wideMargin * 2f;
        int wideDots = ProgressDots.count(wideSpan, r, ProgressDots.PITCH_DP);
        check("wide dots fit the whole rail", wideDots >= 20);
        eq("wide first dot inset by one radius", wideMargin + r,
                ProgressDots.center(wideMargin, wideMargin + wideSpan, r, wideDots, 0));
        eq("wide last dot inset by one radius",
                wideMargin + wideSpan - r,
                ProgressDots.center(wideMargin, wideMargin + wideSpan, r,
                        wideDots, wideDots - 1));
        float step = (wideSpan - r * 2f) / (wideDots - 1);
        check("wide dots do not overlap", step >= r * 2f);
        check("wide dots are evenly spaced", Math.abs(
                (ProgressDots.center(wideMargin, wideMargin + wideSpan, r, wideDots, 3)
                        - ProgressDots.center(wideMargin, wideMargin + wideSpan, r,
                        wideDots, 2)) - step) < 0.001f);

        float smallSpan = WidgetLayout.SMALL_SIZE_DP - wideMargin * 2f;
        int smallDots = ProgressDots.count(smallSpan, r, ProgressDots.PITCH_DP);
        check("small dots fit the whole rail", smallDots >= 8);
        check("small dots do not overlap",
                (smallSpan - r * 2f) / (smallDots - 1) >= r * 2f);

        eq("no completed dots at 0", 0, ProgressDots.completed(wideDots, 0f));
        eq("all dots completed at 1", wideDots,
                ProgressDots.completed(wideDots, 1f));
        eq("half the dots completed",
                Math.round(wideDots * 0.5f),
                ProgressDots.completed(wideDots, 0.5f));
        check("LTR starts at the left dot",
                ProgressDots.isComplete(0, wideDots, 4, false));
        check("LTR leaves the right dots quiet",
                !ProgressDots.isComplete(wideDots - 1, wideDots, 4, false));
        check("RTL starts at the right dot",
                ProgressDots.isComplete(wideDots - 1, wideDots, 4, true));
        check("RTL leaves the left dots quiet",
                !ProgressDots.isComplete(0, wideDots, 4, true));
        check("negative progress is zero",
                ProgressDots.completed(wideDots, -1f) == 0);
        check("progress above one is full",
                ProgressDots.completed(wideDots, 2f) == wideDots);
    }

    private static void restartAndJumpSafety() {
        section("restart and clock-jump safety");
        List<PrayerCalculator.Entry> day = PrayerCalculator.calculate(DAY, CAIRO,
                CalcMethod.EGYPT, AsrMethod.STANDARD);
        long adhan = day.get(0).instant.toEpochMilli();
        long probe = adhan + 7L * 60_000L + 31_000L;

        // A process restart or a clock change simply asks a new snapshot for
        // the same instant; no accumulated counter or stale state is involved.
        PrayerClock.Snapshot first = snapshotAt(probe);
        PrayerClock.Snapshot second = snapshotAt(probe);
        eq("restart: same display", first.displayAt(probe).prayer,
                second.displayAt(probe).prayer);
        eq("restart: same elapsed", first.elapsedMinutes(probe),
                second.elapsedMinutes(probe));
        eq("restart: same progress", first.progress(probe),
                second.progress(probe));
        eq("restart: elapsed 7", 7, second.elapsedMinutes(probe));

        // Jumping backwards before the Adhan must not keep showing "PRAYER
        // NOW"; it must ask for the upcoming Fajr and normal progress again.
        PrayerClock.Snapshot backwards = snapshotAt(adhan - 60_000L);
        eq("backwards jump: Fajr next", Prayer.FAJR, backwards.next.prayer);
        check("backwards jump: no grace", !backwards.graceActive(adhan - 60_000L));
        WidgetModel backwardsModel = WidgetModel.of(backwards, adhan - 60_000L);
        check("backwards jump: model not grace", !backwardsModel.grace);
        eq("backwards jump: model prayer", Prayer.FAJR, backwardsModel.prayer);

        // Jumping forward past the grace must switch to the next prayer
        // immediately.
        PrayerClock.Snapshot forwards = snapshotAt(
                adhan + Prayer.FAJR.graceMillis() + 1L);
        eq("forwards jump: Dhuhr next", Prayer.DHUHR, forwards.next.prayer);
        eq("forwards jump: model prayer", Prayer.DHUHR,
                WidgetModel.of(forwards, adhan + Prayer.FAJR.graceMillis() + 1L).prayer);

        // A time-zone change recalculates from the new zone when the app asks
        // for a fresh snapshot; the previously stored snapshot is immutable.
        Instant same = day.get(0).instant;
        PrayerClock.Snapshot cairo = PrayerClock.snapshot(same, CAIRO,
                CalcMethod.EGYPT, AsrMethod.STANDARD);
        PrayerClock.Snapshot london = PrayerClock.snapshot(same, LONDON,
                CalcMethod.EGYPT, AsrMethod.STANDARD);
        check("zone change uses another calculation day",
                !cairo.date.equals(london.date)
                        || !cairo.next.instant.equals(london.next.instant));
        check("zone snapshot is internally consistent",
                !london.next.instant.isBefore(same));
    }

    // --------------------------------------------------------------- helpers

    private static PrayerClock.Snapshot snapshotAt(long epochMs) {
        return PrayerClock.snapshot(Instant.ofEpochMilli(epochMs), CAIRO,
                CalcMethod.EGYPT, AsrMethod.STANDARD);
    }

    private static List<PrayerCalculator.Entry> syntheticDay(LocalDate date,
                                                             ZoneId zone,
                                                             int fajrHour,
                                                             int fajrMinute,
                                                             int dhuhrHour,
                                                             int dhuhrMinute,
                                                             int asrHour,
                                                             int asrMinute,
                                                             int maghribHour,
                                                             int maghribMinute,
                                                             int ishaHour,
                                                             int ishaMinute) {
        List<PrayerCalculator.Entry> entries = new ArrayList<>();
        entries.add(at(Prayer.FAJR, date, zone, fajrHour, fajrMinute));
        entries.add(at(Prayer.DHUHR, date, zone, dhuhrHour, dhuhrMinute));
        entries.add(at(Prayer.ASR, date, zone, asrHour, asrMinute));
        entries.add(at(Prayer.MAGHRIB, date, zone, maghribHour, maghribMinute));
        entries.add(at(Prayer.ISHA, date, zone, ishaHour, ishaMinute));
        return entries;
    }

    private static PrayerCalculator.Entry at(Prayer prayer, LocalDate date,
                                             ZoneId zone, int hour, int minute) {
        ZonedDateTime local = date.atTime(hour, minute).atZone(zone);
        return new PrayerCalculator.Entry(prayer, local.toInstant(), zone);
    }

    private static void section(String name) {
        System.out.println();
        System.out.println("-- " + name);
    }

    private static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        checks++;
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what + " expected <" + expected
                    + "> but was <" + actual + ">");
        }
    }
}
