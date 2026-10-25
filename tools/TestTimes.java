import com.dotpray.app.data.Place;
import com.dotpray.app.prayer.AsrMethod;
import com.dotpray.app.prayer.CalcMethod;
import com.dotpray.app.prayer.PrayerCalculator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/** Prints calculated prayer times so they can be diffed against a reference. */
public class TestTimes {

    public static void main(String[] args) {
        String[][] cities = {
                {"Cairo", "30.0444", "31.2357", "Africa/Cairo"},
                {"Makkah", "21.4225", "39.8262", "Asia/Riyadh"},
                {"London", "51.5074", "-0.1278", "Europe/London"},
                {"New York", "40.7128", "-74.0060", "America/New_York"},
                {"Jakarta", "-6.2088", "106.8456", "Asia/Jakarta"},
                {"Sydney", "-33.8688", "151.2093", "Australia/Sydney"},
                {"Reykjavik", "64.1466", "-21.9426", "Atlantic/Reykjavik"},
        };
        String[] dates = {"2026-01-15", "2026-04-21", "2026-07-04", "2026-10-22", "2026-12-21"};

        for (String[] city : cities) {
            Place place = new Place(city[0], "", Double.parseDouble(city[1]),
                    Double.parseDouble(city[2]), city[3]);
            for (String date : dates) {
                LocalDate d = LocalDate.parse(date);
                List<PrayerCalculator.Entry> entries = PrayerCalculator.calculate(
                        d, place, CalcMethod.EGYPT, AsrMethod.STANDARD);
                StringBuilder sb = new StringBuilder();
                sb.append(city[0]).append('|').append(date).append('|');
                for (PrayerCalculator.Entry e : entries) {
                    ZonedDateTime t = e.instant.atZone(ZoneId.of(city[3]));
                    sb.append(String.format("%s=%02d:%02d;", e.prayer.name(),
                            t.getHour(), t.getMinute()));
                }
                System.out.println(sb);
            }
        }
    }
}
