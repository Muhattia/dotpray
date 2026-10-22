package com.dotpray.app.prayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.dotpray.app.data.Place;

/**
 * Solar prayer-time calculator.
 *
 * <p>Uses the NOAA solar position algorithm: equation of time + declination for the
 * reference instant, then hour-angle offsets from true solar noon. Everything is
 * computed on device, in UTC, and only converted to local time at the very end so
 * that DST boundaries can never shift a prayer by an hour.
 */
public final class PrayerCalculator {

    private static final double SUN_ALTITUDE = -0.833;
    private static final double MINUTE = 60_000d;

    private PrayerCalculator() {
    }

    /** One prayer entry with its absolute instant. */
    public static final class Entry {
        public final Prayer prayer;
        public final Instant instant;
        public final ZonedDateTime local;

        Entry(Prayer prayer, Instant instant, java.time.ZoneId zone) {
            this.prayer = prayer;
            this.instant = instant;
            this.local = instant.atZone(zone);
        }
    }

    public static List<Entry> calculate(LocalDate date, Place place, CalcMethod method, AsrMethod asr) {
        java.time.ZoneId zone = place.zone();

        // Reference: local solar midday, used for equation of time / declination.
        // NOTE: ZonedDateTime.toInstant()/toEpochSecond() do not exist on API 26
        // (Android 8 shipped an incomplete java.time), so the conversion is done
        // through LocalDateTime#toEpochSecond(ZoneOffset), which is present on
        // every supported API level.
        ZonedDateTime localNoon = date.atTime(12, 0).atZone(zone);
        Instant reference = Instant.ofEpochSecond(
                localNoon.toLocalDateTime().toEpochSecond(localNoon.getOffset()));
        double[] solar = equationOfTimeAndDeclination(reference);
        double eqt = solar[0];
        double declination = solar[1];

        // True solar noon expressed in minutes after 00:00 UTC of the reference UTC day.
        LocalDate utcDay = reference.atZone(ZoneOffset.UTC).toLocalDate();
        double noonMinutes = 720.0 - 4.0 * place.longitude - eqt;
        long midnightMillis = utcDay.toEpochDay() * 86_400_000L;
        Instant solarNoon = Instant.ofEpochMilli(midnightMillis + Math.round(noonMinutes * MINUTE));

        // Keep solar noon on the requested local date.
        LocalDate noonDate = solarNoon.atZone(zone).toLocalDate();
        if (noonDate.isBefore(date)) {
            solarNoon = solarNoon.plusSeconds(86_400);
        } else if (noonDate.isAfter(date)) {
            solarNoon = solarNoon.minusSeconds(86_400);
        }

        double lat = place.latitude;

        double sunAngle = hourAngle(SUN_ALTITUDE, lat, declination);
        double fajrAngle = hourAngle(-method.fajrAngle, lat, declination);
        double ishaAngle = hourAngle(-method.ishaAngle, lat, declination);
        double asrAngle = hourAngle(asrAltitude(lat, declination, asr.shadowFactor()), lat, declination);

        // Fall back to sensible values in polar regions where the sun never crosses.
        double dayLength = Double.isNaN(sunAngle) ? 720.0 : 8.0 * sunAngle;
        double nightLength = 1440.0 - dayLength;

        double sunriseOffset = Double.isNaN(sunAngle) ? -360.0 : -4.0 * sunAngle;
        double sunsetOffset = Double.isNaN(sunAngle) ? 360.0 : 4.0 * sunAngle;

        double fajrOffset;
        if (Double.isNaN(fajrAngle)) {
            fajrOffset = sunriseOffset - nightLength / 7.0;
        } else {
            fajrOffset = -4.0 * fajrAngle;
        }

        double ishaOffset;
        if (Double.isNaN(ishaAngle)) {
            ishaOffset = sunsetOffset + nightLength / 7.0;
        } else {
            ishaOffset = 4.0 * ishaAngle;
        }

        double asrOffset = Double.isNaN(asrAngle) ? 4.0 * 60.0 : 4.0 * asrAngle;
        double dhuhrOffset = 1.0; // a one minute safety margin past true noon

        List<Entry> entries = new ArrayList<>(5);
        entries.add(new Entry(Prayer.FAJR, solarNoon.plusMillis(Math.round(fajrOffset * MINUTE)), zone));
        entries.add(new Entry(Prayer.DHUHR, solarNoon.plusMillis(Math.round(dhuhrOffset * MINUTE)), zone));
        entries.add(new Entry(Prayer.ASR, solarNoon.plusMillis(Math.round(asrOffset * MINUTE)), zone));
        entries.add(new Entry(Prayer.MAGHRIB, solarNoon.plusMillis(Math.round(sunsetOffset * MINUTE)), zone));
        entries.add(new Entry(Prayer.ISHA, solarNoon.plusMillis(Math.round(ishaOffset * MINUTE)), zone));
        return entries;
    }

    /** Altitude of the sun when an object's shadow reaches the given factor. */
    private static double asrAltitude(double latitude, double declination, int shadowFactor) {
        double lat = Math.toRadians(Math.abs(latitude - Math.toDegrees(declination)));
        double shadow = Math.tan(lat) + shadowFactor;
        return Math.toDegrees(Math.atan(1.0 / shadow));
    }

    /** Hour angle in degrees for a given solar altitude, NaN when the sun never reaches it. */
    private static double hourAngle(double altitude, double latitude, double declination) {
        double h = Math.toRadians(altitude);
        double lat = Math.toRadians(latitude);
        double cosH = (Math.sin(h) - Math.sin(lat) * Math.sin(declination))
                / (Math.cos(lat) * Math.cos(declination));
        if (cosH > 1.0 || cosH < -1.0) return Double.NaN;
        return Math.toDegrees(Math.acos(cosH));
    }

    /** @return {equation of time in minutes, declination in radians} */
    private static double[] equationOfTimeAndDeclination(Instant instant) {
        double jd = instant.getEpochSecond() / 86400.0 + 2440587.5;
        double t = (jd - 2451545.0) / 36525.0;

        double l0 = mod360(280.46646 + t * (36000.76983 + t * 0.0003032));
        double m = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);

        double mRad = Math.toRadians(m);
        double l0Rad = Math.toRadians(l0);

        double center = Math.sin(mRad) * (1.914602 - t * (0.004817 + 0.000014 * t))
                + Math.sin(2 * mRad) * (0.019993 - 0.000101 * t)
                + Math.sin(3 * mRad) * 0.000289;
        double trueLong = l0 + center;
        double omega = 125.04 - 1934.136 * t;
        double appLong = trueLong - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        double meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0;
        double obliquity = meanObliquity + 0.00256 * Math.cos(Math.toRadians(omega));

        double declination = Math.asin(Math.sin(Math.toRadians(obliquity)) * Math.sin(Math.toRadians(appLong)));

        double y = Math.tan(Math.toRadians(obliquity) / 2.0);
        y *= y;
        double eqt = 4.0 * Math.toDegrees(y * Math.sin(2 * l0Rad)
                - 2.0 * e * Math.sin(mRad)
                + 4.0 * e * y * Math.sin(mRad) * Math.cos(2 * l0Rad)
                - 0.5 * y * y * Math.sin(4 * l0Rad)
                - 1.25 * e * e * Math.sin(2 * mRad));

        return new double[]{eqt, declination};
    }

    private static double mod360(double value) {
        double v = value % 360.0;
        return v < 0 ? v + 360.0 : v;
    }
}
