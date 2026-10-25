package com.dotpray.app.data;

import android.content.Context;

import com.dotpray.app.R;

/**
 * Localizes the bundled place names while keeping the canonical English names
 * in storage. A GPS place name that is not one of the presets is shown as the
 * platform geocoder returned it (a proper noun, not interface copy).
 */
public final class PlaceNames {

    private PlaceNames() {
    }

    public static String city(Context context, String name) {
        if (name == null) return "";
        if (Place.CURRENT_LOCATION.equalsIgnoreCase(name)
                || Place.CURRENT_SHORT.equalsIgnoreCase(name)) {
            return context.getString(R.string.place_current);
        }
        switch (name) {
            case "Makkah":
                return context.getString(R.string.place_city_makkah);
            case "Madinah":
                return context.getString(R.string.place_city_madinah);
            case "London":
                return context.getString(R.string.place_city_london);
            case "Istanbul":
                return context.getString(R.string.place_city_istanbul);
            case "Dubai":
                return context.getString(R.string.place_city_dubai);
            case "Doha":
                return context.getString(R.string.place_city_doha);
            case "Muscat":
                return context.getString(R.string.place_city_muscat);
            case "Cairo":
                return context.getString(R.string.place_city_cairo);
            case "Lagos":
                return context.getString(R.string.place_city_lagos);
            case "Nairobi":
                return context.getString(R.string.place_city_nairobi);
            case "Cape Town":
                return context.getString(R.string.place_city_cape_town);
            case "Karachi":
                return context.getString(R.string.place_city_karachi);
            case "Lahore":
                return context.getString(R.string.place_city_lahore);
            case "Delhi":
                return context.getString(R.string.place_city_delhi);
            case "Dhaka":
                return context.getString(R.string.place_city_dhaka);
            case "Jakarta":
                return context.getString(R.string.place_city_jakarta);
            case "Kuala Lumpur":
                return context.getString(R.string.place_city_kuala_lumpur);
            case "Singapore":
                return context.getString(R.string.place_city_singapore);
            case "Bangkok":
                return context.getString(R.string.place_city_bangkok);
            case "Tokyo":
                return context.getString(R.string.place_city_tokyo);
            case "Sydney":
                return context.getString(R.string.place_city_sydney);
            case "Melbourne":
                return context.getString(R.string.place_city_melbourne);
            case "New York":
                return context.getString(R.string.place_city_new_york);
            case "Chicago":
                return context.getString(R.string.place_city_chicago);
            case "Toronto":
                return context.getString(R.string.place_city_toronto);
            case "Los Angeles":
                return context.getString(R.string.place_city_los_angeles);
            case "Paris":
                return context.getString(R.string.place_city_paris);
            case "Berlin":
                return context.getString(R.string.place_city_berlin);
            case "Amsterdam":
                return context.getString(R.string.place_city_amsterdam);
            case "Madrid":
                return context.getString(R.string.place_city_madrid);
            case "Moscow":
                return context.getString(R.string.place_city_moscow);
            case "Stockholm":
                return context.getString(R.string.place_city_stockholm);
            default:
                return name;
        }
    }

    public static String country(Context context, String code) {
        if (code == null || code.isEmpty()) return "";
        switch (code) {
            case "SA":
                return context.getString(R.string.place_country_sa);
            case "UK":
                return context.getString(R.string.place_country_uk);
            case "TR":
                return context.getString(R.string.place_country_tr);
            case "AE":
                return context.getString(R.string.place_country_ae);
            case "QA":
                return context.getString(R.string.place_country_qa);
            case "OM":
                return context.getString(R.string.place_country_om);
            case "EG":
                return context.getString(R.string.place_country_eg);
            case "NG":
                return context.getString(R.string.place_country_ng);
            case "KE":
                return context.getString(R.string.place_country_ke);
            case "ZA":
                return context.getString(R.string.place_country_za);
            case "PK":
                return context.getString(R.string.place_country_pk);
            case "IN":
                return context.getString(R.string.place_country_in);
            case "BD":
                return context.getString(R.string.place_country_bd);
            case "ID":
                return context.getString(R.string.place_country_id);
            case "MY":
                return context.getString(R.string.place_country_my);
            case "SG":
                return context.getString(R.string.place_country_sg);
            case "TH":
                return context.getString(R.string.place_country_th);
            case "JP":
                return context.getString(R.string.place_country_jp);
            case "AU":
                return context.getString(R.string.place_country_au);
            case "US":
                return context.getString(R.string.place_country_us);
            case "CA":
                return context.getString(R.string.place_country_ca);
            case "FR":
                return context.getString(R.string.place_country_fr);
            case "DE":
                return context.getString(R.string.place_country_de);
            case "NL":
                return context.getString(R.string.place_country_nl);
            case "ES":
                return context.getString(R.string.place_country_es);
            case "RU":
                return context.getString(R.string.place_country_ru);
            case "SE":
                return context.getString(R.string.place_country_se);
            default:
                return code;
        }
    }

    public static String displayName(Context context, Place place) {
        String city = city(context, place.name);
        String country = country(context, place.country);
        if (country.isEmpty()) return city;
        return context.getString(R.string.location_pair, city, country);
    }

    public static String shortName(Context context, Place place) {
        if (Place.CURRENT_LOCATION.equalsIgnoreCase(place.name)
                || Place.CURRENT_SHORT.equalsIgnoreCase(place.name)) {
            return context.getString(R.string.place_current_short);
        }
        String value = city(context, place.name).trim();
        int comma = value.indexOf(',');
        if (comma > 0) value = value.substring(0, comma).trim();
        if (value.length() > 20) {
            int firstSpace = value.indexOf(' ');
            int secondSpace = firstSpace > 0 ? value.indexOf(' ', firstSpace + 1) : -1;
            if (secondSpace > 0) value = value.substring(0, secondSpace).trim();
            if (value.length() > 20) {
                int cut = value.lastIndexOf(' ', 20);
                if (cut < 10) cut = 20;
                value = value.substring(0, cut).trim();
            }
        }
        return value.isEmpty() ? context.getString(R.string.place_current_short) : value;
    }
}
