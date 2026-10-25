package com.dotpray.app.prayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dotpray.app.data.Place;

/** Resolves "what is the next prayer right now?" for any place, method and asr rule. */
public final class PrayerClock {

    private PrayerClock() {
    }

    /**
     * The rolling state of the day: today's times plus the next/previous prayer.
     *
     * <p>A prayer remains the displayed prayer for its grace period. The next
     * prayer is still tracked normally during that time, so alerts and the
     * countdown after the grace ends need no extra state. Progress is measured
     * from the end of the grace period to the next Adhan, which is why it is
     * 100% while a prayer is active and 0% the moment the next prayer becomes
     * the display.
     */
    public static final class Snapshot {
        public final LocalDate date;
        public final Place place;
        public final CalcMethod method;
        public final AsrMethod asr;
        public final List<PrayerCalculator.Entry> today;
        public final PrayerCalculator.Entry next;
        public final PrayerCalculator.Entry previous;
        public final boolean nextIsTomorrow;

        /** The effective end of the previous prayer's grace period. */
        public final long graceEnd;

        Snapshot(LocalDate date, Place place, CalcMethod method, AsrMethod asr,
                 List<PrayerCalculator.Entry> today,
                 PrayerCalculator.Entry next,
                 PrayerCalculator.Entry previous,
                 boolean nextIsTomorrow) {
            this.date = date;
            this.place = place;
            this.method = method;
            this.asr = asr;
            this.today = today;
            this.next = next;
            this.previous = previous;
            this.nextIsTomorrow = nextIsTomorrow;

            long previousAt = previous.instant.toEpochMilli();
            long nextAt = next.instant.toEpochMilli();
            // A grace period can never overlap the following Adhan. At extreme
            // latitudes the two can be minutes apart; cutting the grace there
            // keeps the display from skipping a prayer entirely.
            long graceEnd = Math.min(previousAt + previous.prayer.graceMillis(), nextAt);
            this.graceEnd = Math.max(previousAt, graceEnd);
        }

        public PrayerCalculator.Entry entry(Prayer prayer) {
            for (PrayerCalculator.Entry e : today) {
                if (e.prayer == prayer) return e;
            }
            return null;
        }

        /** True when {@code nowMs} is still inside the previous prayer's grace. */
        public boolean graceActive(long nowMs) {
            long from = previous.instant.toEpochMilli();
            return nowMs >= from && nowMs < graceEnd;
        }

        /** Whole minutes since the previous Adhan, from the real timestamp. */
        public int elapsedMinutes(long nowMs) {
            long elapsed = nowMs - previous.instant.toEpochMilli();
            if (elapsed <= 0L) return 0;
            return (int) Math.min(Integer.MAX_VALUE, elapsed / 60_000L);
        }

        /** The prayer to show at {@code nowMs}. */
        public PrayerCalculator.Entry displayAt(long nowMs) {
            return graceActive(nowMs) ? previous : next;
        }

        /** True only after the grace ends and the display moves to tomorrow. */
        public boolean displayIsTomorrowAt(long nowMs) {
            return !graceActive(nowMs) && nextIsTomorrow;
        }

        /**
         * How much of the wait between the end of the grace period and the
         * next Adhan has elapsed, clamped to 0..1. Used only for presentation;
         * it does not change any calculated prayer instant.
         */
        public float progress(long nowMs) {
            if (graceActive(nowMs)) return 1f;
            long from = graceEnd;
            long to = next.instant.toEpochMilli();
            if (to <= from) return 0f;
            return Math.max(0f, Math.min(1f, (nowMs - from) / (float) (to - from)));
        }

        /**
         * When the widget should next refresh because a grace period ends.
         * During a grace that is the current boundary; otherwise it is the
         * boundary after the upcoming prayer, capped at the prayer after that
         * so a very short interval cannot skip a prayer.
         */
        public long nextGraceEndMs(long nowMs) {
            if (graceActive(nowMs)) return graceEnd;
            long nextAt = next.instant.toEpochMilli();
            long end = nextAt + next.prayer.graceMillis();
            PrayerCalculator.Entry following = following(next);
            if (following != null) {
                end = Math.min(end, following.instant.toEpochMilli());
            }
            return Math.max(nextAt, end);
        }

        private PrayerCalculator.Entry following(PrayerCalculator.Entry after) {
            LocalDate day = after.local.toLocalDate();
            List<PrayerCalculator.Entry> list = day.equals(date) ? today
                    : PrayerCalculator.calculate(day, place, method, asr);
            for (PrayerCalculator.Entry e : list) {
                if (e.instant.isAfter(after.instant)) return e;
            }
            return null;
        }
    }

    public static Snapshot snapshot(Instant now, Place place, CalcMethod method, AsrMethod asr) {
        ZoneId zone = place.zone();
        LocalDate today = now.atZone(zone).toLocalDate();

        List<PrayerCalculator.Entry> entries = PrayerCalculator.calculate(today, place, method, asr);

        List<PrayerCalculator.Entry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, (a, b) -> a.instant.compareTo(b.instant));

        PrayerCalculator.Entry next = null;
        PrayerCalculator.Entry previous = null;
        for (PrayerCalculator.Entry e : sorted) {
            if (e.instant.isAfter(now)) {
                next = e;
                break;
            }
            previous = e;
        }

        boolean tomorrow = false;
        if (next == null) {
            List<PrayerCalculator.Entry> tomorrowEntries =
                    PrayerCalculator.calculate(today.plusDays(1), place, method, asr);
            next = tomorrowEntries.get(0);
            tomorrow = true;
        }

        if (previous == null) {
            List<PrayerCalculator.Entry> yesterdayEntries =
                    PrayerCalculator.calculate(today.minusDays(1), place, method, asr);
            previous = yesterdayEntries.get(yesterdayEntries.size() - 1);
        }

        return new Snapshot(today, place, method, asr, entries, next, previous, tomorrow);
    }
}
