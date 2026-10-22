package com.dotpray.app.prayer;

import com.dotpray.app.data.Place;

/** Great-circle bearing towards the Kaaba. */
public final class Qibla {

    private static final double KAABA_LAT = 21.4224779;
    private static final double KAABA_LON = 39.8251832;

    private Qibla() {
    }

    public static double bearing(Place place) {
        double lat1 = Math.toRadians(place.latitude);
        double lat2 = Math.toRadians(KAABA_LAT);
        double dLon = Math.toRadians(KAABA_LON - place.longitude);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }
}
