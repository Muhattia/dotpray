package com.dotpray.app.prayer;

/** The five daily prayers, in order. */
public enum Prayer {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha");

    public final String label;

    Prayer(String label) {
        this.label = label;
    }
}
