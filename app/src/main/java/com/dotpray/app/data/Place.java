package com.dotpray.app.data;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A location with a fixed IANA time zone so prayer times stay correct across DST. */
public final class Place {

    public final String name;
    public final String country;
    public final double latitude;
    public final double longitude;
    public final String zoneId;
    /** True when the fix came from the device rather than the city list. */
    public final boolean gps;

    public Place(String name, String country, double latitude, double longitude, String zoneId) {
        this(name, country, latitude, longitude, zoneId, false);
    }

    public Place(String name, String country, double latitude, double longitude,
                 String zoneId, boolean gps) {
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoneId = zoneId;
        this.gps = gps;
    }

    public ZoneId zone() {
        try {
            return ZoneId.of(zoneId);
        } catch (RuntimeException e) {
            return ZoneId.systemDefault();
        }
    }

    public boolean isCustom() {
        return gps || CURRENT_LOCATION.equals(name);
    }

    /** Coordinates with caller-supplied hemisphere letters. */
    public String coordinates(String north, String south, String east, String west) {
        return Format.coord(latitude, true, north, south, east, west)
                + " / " + Format.coord(longitude, false, north, south, east, west);
    }

    /**
     * Compact name for the primary header. Geocoder results can be a full
     * neighbourhood or street; the header only needs the locality. The caller
     * still ellipsizes this if the available width is even smaller.
     */
    public String shortName() {
        String value = name == null ? "" : name.trim();
        int comma = value.indexOf(',');
        if (comma > 0) value = value.substring(0, comma).trim();
        if (value.length() > 20) {
            // Prefer the first two words over a mid-word cut: geocoded
            // neighbourhood names are often "Al Manteqah Al Gharbeyah" and
            // "Al Manteqah" reads as a place, while "Al Manteqah Al" does not.
            int firstSpace = value.indexOf(' ');
            int secondSpace = firstSpace > 0 ? value.indexOf(' ', firstSpace + 1) : -1;
            if (secondSpace > 0) value = value.substring(0, secondSpace).trim();
            if (value.length() > 20) {
                int cut = value.lastIndexOf(' ', 20);
                if (cut < 10) cut = 20;
                value = value.substring(0, cut).trim();
            }
        }
        return value.isEmpty() ? CURRENT_SHORT : value;
    }

    /** Stable stored tokens for a GPS fix with no geocoder name. */
    public static final String CURRENT_LOCATION = "CURRENT LOCATION";
    public static final String CURRENT_SHORT = "CURRENT";

    /**
     * Nearest bundled city, or {@code null} when every preset is farther than
     * {@code maxKm}. Used as the offline fallback when the platform geocoder
     * has no answer.
     */
    public static Place nearest(double latitude, double longitude, double maxKm) {
        Place best = null;
        double bestKm = maxKm;
        for (Place place : PRESETS) {
            double km = distanceKm(latitude, longitude, place.latitude, place.longitude);
            if (km < bestKm) {
                bestKm = km;
                best = place;
            }
        }
        return best;
    }

    /** Haversine distance in kilometres. */
    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Last-resort fallback before the first GPS fix arrives. */
    public static final Place CAIRO = new Place("Cairo", "EG", 30.0444, 31.2357, "Africa/Cairo");

    public static final List<Place> PRESETS = Collections.unmodifiableList(Arrays.asList(
            new Place("Makkah", "SA", 21.4225, 39.8262, "Asia/Riyadh"),
            new Place("Madinah", "SA", 24.4686, 39.6142, "Asia/Riyadh"),
            new Place("London", "UK", 51.5074, -0.1278, "Europe/London"),
            new Place("Istanbul", "TR", 41.0082, 28.9784, "Europe/Istanbul"),
            new Place("Dubai", "AE", 25.2048, 55.2708, "Asia/Dubai"),
            new Place("Doha", "QA", 25.2854, 51.5310, "Asia/Qatar"),
            new Place("Muscat", "OM", 23.5880, 58.3829, "Asia/Muscat"),
            CAIRO,
            new Place("Lagos", "NG", 6.5244, 3.3792, "Africa/Lagos"),
            new Place("Nairobi", "KE", -1.2921, 36.8219, "Africa/Nairobi"),
            new Place("Cape Town", "ZA", -33.9249, 18.4241, "Africa/Johannesburg"),
            new Place("Karachi", "PK", 24.8607, 67.0011, "Asia/Karachi"),
            new Place("Lahore", "PK", 31.5204, 74.3587, "Asia/Karachi"),
            new Place("Delhi", "IN", 28.6139, 77.2090, "Asia/Kolkata"),
            new Place("Dhaka", "BD", 23.8103, 90.4125, "Asia/Dhaka"),
            new Place("Jakarta", "ID", -6.2088, 106.8456, "Asia/Jakarta"),
            new Place("Kuala Lumpur", "MY", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
            new Place("Singapore", "SG", 1.3521, 103.8198, "Asia/Singapore"),
            new Place("Bangkok", "TH", 13.7563, 100.5018, "Asia/Bangkok"),
            new Place("Tokyo", "JP", 35.6762, 139.6503, "Asia/Tokyo"),
            new Place("Sydney", "AU", -33.8688, 151.2093, "Australia/Sydney"),
            new Place("Melbourne", "AU", -37.8136, 144.9631, "Australia/Melbourne"),
            new Place("New York", "US", 40.7128, -74.0060, "America/New_York"),
            new Place("Chicago", "US", 41.8781, -87.6298, "America/Chicago"),
            new Place("Toronto", "CA", 43.6532, -79.3832, "America/Toronto"),
            new Place("Los Angeles", "US", 34.0522, -118.2437, "America/Los_Angeles"),
            new Place("Paris", "FR", 48.8566, 2.3522, "Europe/Paris"),
            new Place("Berlin", "DE", 52.5200, 13.4050, "Europe/Berlin"),
            new Place("Amsterdam", "NL", 52.3676, 4.9041, "Europe/Amsterdam"),
            new Place("Madrid", "ES", 40.4168, -3.7038, "Europe/Madrid"),
            new Place("Moscow", "RU", 55.7558, 37.6173, "Europe/Moscow"),
            new Place("Stockholm", "SE", 59.3293, 18.0686, "Europe/Stockholm")
    ));
}
