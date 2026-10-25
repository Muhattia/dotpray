package com.dotpray.app.prayer;

/** The five daily prayers, in order. */
public enum Prayer {
    FAJR("Fajr", 15),
    DHUHR("Dhuhr", 15),
    ASR("Asr", 15),
    MAGHRIB("Maghrib", 10),
    ISHA("Isha", 15);

    /**
     * Stable identifier used by alarms and stored extras. The enum name is
     * independent of the selected language; {@code legacyLabel} keeps an alarm
     * booked by an older build working across the update.
     */
    public final int graceMinutes;

    private final String legacyLabel;

    Prayer(String legacyLabel, int graceMinutes) {
        this.legacyLabel = legacyLabel;
        this.graceMinutes = graceMinutes;
    }

    public long graceMillis() {
        return graceMinutes * 60_000L;
    }

    /** Parses the current enum name or the label stored by Dotpray 1.2. */
    public static Prayer fromKey(String value) {
        if (value == null) return null;
        for (Prayer prayer : values()) {
            if (prayer.name().equalsIgnoreCase(value)
                    || prayer.legacyLabel.equalsIgnoreCase(value)) {
                return prayer;
            }
        }
        return null;
    }
}
