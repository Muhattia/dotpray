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

    /** The rolling state of the day: today's times plus the next/previous prayer. */
    public static final class Snapshot {
        public final LocalDate date;
        public final Place place;
        public final CalcMethod method;
        public final AsrMethod asr;
        public final List<PrayerCalculator.Entry> today;
        public final PrayerCalculator.Entry next;
        public final PrayerCalculator.Entry previous;
        public final boolean nextIsTomorrow;

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
        }

        public PrayerCalculator.Entry entry(Prayer prayer) {
            for (PrayerCalculator.Entry e : today) {
                if (e.prayer == prayer) return e;
            }
            return null;
        }

        public ZonedDateTimeHolder nextLocal() {
            return new ZonedDateTimeHolder(next.local);
        }
    }

    /** Tiny wrapper so callers do not need to import ZonedDateTime explicitly. */
    public static final class ZonedDateTimeHolder {
        public final java.time.ZonedDateTime value;

        ZonedDateTimeHolder(java.time.ZonedDateTime value) {
            this.value = value;
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
