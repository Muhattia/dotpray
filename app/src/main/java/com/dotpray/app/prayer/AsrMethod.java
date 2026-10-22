package com.dotpray.app.prayer;

/** Asr shadow factor: 1 (Shafi/Maliki/Hanbali) or 2 (Hanafi). */
public enum AsrMethod {
    STANDARD("STANDARD", "SHAFI / MALIKI / HANBALI"),
    HANAFI("HANAFI", "HANAFI");

    public final String id;
    public final String longName;

    /** Stable storage key; never shown to the user. */
    public final String key;

    AsrMethod(String id, String longName) {
        this.id = id;
        this.longName = longName;
        this.key = name();
    }

    public int shadowFactor() {
        return this == HANAFI ? 2 : 1;
    }

    public static AsrMethod fromId(String id) {
        for (AsrMethod m : values()) {
            if (m.id.equals(id)) return m;
        }
        return STANDARD;
    }

    /** Looks an Asr rule up by its stable storage key. */
    public static AsrMethod fromKey(String key) {
        for (AsrMethod m : values()) {
            if (m.key.equals(key)) return m;
        }
        // older builds stored the display label
        return fromId(key);
    }
}
