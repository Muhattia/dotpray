package com.dotpray.app.prayer;

/**
 * The single calculation method used by the app: the Egyptian General
 * Authority of Survey (Fajr 19.5°, Isha 17.5°).
 *
 * <p>Keeping this as an enum rather than scattering two numbers through the
 * calculator makes the values visible in one place, and leaves the engine's
 * signature unchanged.
 */
public enum CalcMethod {
    EGYPT("EGYPT", "Egyptian General Authority of Survey", 19.5, 17.5);

    public final String id;
    public final String longName;
    public final double fajrAngle;
    public final double ishaAngle;

    CalcMethod(String id, String longName, double fajrAngle, double ishaAngle) {
        this.id = id;
        this.longName = longName;
        this.fajrAngle = fajrAngle;
        this.ishaAngle = ishaAngle;
    }

    public String summary() {
        return "FAJR " + trim(fajrAngle) + "° / ISHA " + trim(ishaAngle) + "°";
    }

    private static String trim(double v) {
        if (v == Math.rint(v)) return String.valueOf((int) v);
        return String.valueOf(v);
    }
}
