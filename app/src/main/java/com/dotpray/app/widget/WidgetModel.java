package com.dotpray.app.widget;

import com.dotpray.app.prayer.Prayer;
import com.dotpray.app.prayer.PrayerCalculator;
import com.dotpray.app.prayer.PrayerClock;

/**
 * A single render-time view of what the widgets should show.
 *
 * <p>Every widget variant and the app hero consume this model, so "which prayer",
 * "is it now", "how many minutes" and "how full is the rail" can never drift
 * between surfaces. It contains no Android types and no language-specific
 * text; the renderers resolve the selected language from resources.
 */
public final class WidgetModel {

    public final PrayerClock.Snapshot snapshot;
    public final long nowMs;
    public final PrayerCalculator.Entry entry;
    public final Prayer prayer;
    public final boolean grace;
    public final boolean displayIsTomorrow;
    public final int elapsedMinutes;
    public final float progress;

    private WidgetModel(PrayerClock.Snapshot snapshot, long nowMs) {
        this.snapshot = snapshot;
        this.nowMs = nowMs;
        this.grace = snapshot.graceActive(nowMs);
        this.entry = this.grace ? snapshot.previous : snapshot.next;
        this.prayer = entry.prayer;
        this.displayIsTomorrow = !this.grace && snapshot.nextIsTomorrow;
        this.elapsedMinutes = this.grace ? snapshot.elapsedMinutes(nowMs) : 0;
        this.progress = snapshot.progress(nowMs);
    }

    public static WidgetModel of(PrayerClock.Snapshot snapshot, long nowMs) {
        return new WidgetModel(snapshot, nowMs);
    }

    /** The scheduled time of the prayer currently on display. */
    public long entryEpochMs() {
        return entry.instant.toEpochMilli();
    }
}
