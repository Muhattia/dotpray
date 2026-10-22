package com.dotpray.app.data;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Turns a GPS fix into a human place name.
 *
 * <p>The platform geocoder runs in the system's location service, so the app
 * still needs no {@code INTERNET} permission. When no backend is available
 * (or the device is offline) the nearest bundled city within 150 km is used,
 * and otherwise the fix stays "CURRENT LOCATION".
 *
 * <p>Both the lookup and the fallback run off the main thread; the callback is
 * always delivered on the main thread.
 */
public final class LocalityResolver {

    public interface Callback {
        void onResolved(String name, String country);
    }

    private LocalityResolver() {
    }

    public static void resolve(Context context, double latitude, double longitude,
                               Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            String name = null;
            String country = null;

            try {
                // Keep the place name in Latin script: the UI has no Arabic
                // strings and Doto has no Arabic glyphs.
                Geocoder geocoder = new Geocoder(app, Locale.US);
                if (Geocoder.isPresent()) {
                    List<Address> found = geocoder.getFromLocation(latitude, longitude, 1);
                    if (found != null && !found.isEmpty()) {
                        Address address = found.get(0);
                        name = first(address.getLocality(), address.getSubAdminArea(),
                                address.getAdminArea());
                        country = address.getCountryCode();
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                // offline, no geocoder backend, or a bad fix: use the presets
            }

            // The typeface and the UI are Latin-only. If a geocoder ignores
            // the locale hint and returns Arabic script, use the English
            // nearest-city name instead of rendering missing glyphs.
            if (containsArabic(name)) name = null;

            if (name == null) {
                Place near = Place.nearest(latitude, longitude, 150);
                if (near != null) {
                    name = near.name;
                    country = near.country;
                }
            }

            final String resolvedName = name;
            final String resolvedCountry = country;
            main.post(() -> callback.onResolved(resolvedName, resolvedCountry));
        }, "dotpray-locality").start();
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private static boolean containsArabic(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            if ((cp >= 0x0600 && cp <= 0x06FF)
                    || (cp >= 0x0750 && cp <= 0x077F)
                    || (cp >= 0x08A0 && cp <= 0x08FF)
                    || (cp >= 0xFB50 && cp <= 0xFDFF)
                    || (cp >= 0xFE70 && cp <= 0xFEFF)) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }
}
