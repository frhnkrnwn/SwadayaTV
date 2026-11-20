package com.swadaya.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** The Weather card's parsing and choices, on a real BMKG response captured on 20 November 2026. */
public class WeatherTest {
    private static final String FORECAST = "31.71.03.1001"; // Kemayoran, from the captured response
    private static final String JAKARTA = "31.71.01.1001"; // Gambir, the city's entry in places.json

    @Test
    public void picksTodaysForecastFromBmkg() throws Exception {
        String json = resource("weather-bmkg.json");
        Weather.Day d = Weather.parseForecast(json, FORECAST, LocalDate.of(2026, 11, 20));
        assertNotNull(d);
        // The day's four slots (13:00, 16:00, 19:00, 22:00 WIB): 25–30°C, rain in the afternoon.
        assertEquals(25, d.min);
        assertEquals(30, d.max);
        assertEquals("Light Rain", Weather.outlook(d));
        assertEquals("Light Rain", d.afternoon);
        assertEquals("Mostly Cloudy", d.night);
        assertEquals(Weather.RAIN, Weather.kind(d.summary));
        assertNull(Weather.parseForecast(json, "31.71.01.1001", LocalDate.of(2026, 11, 20))); // another place
        assertNull(Weather.parseForecast(json, FORECAST, LocalDate.of(2026, 12, 1))); // out of range
    }

    @Test
    public void readsTheIndonesianCityList() throws Exception {
        List<Weather.Place> all = Weather.parsePlaces(resource("weather-places.json"));
        assertEquals(58, all.size());
        Weather.Place jakarta = Weather.place(all, JAKARTA);
        assertNotNull(jakarta);
        assertEquals("Jakarta", jakarta.name);
        assertEquals("DKI Jakarta", jakarta.type());
        assertEquals(-6.17638, jakarta.lat, 1e-4);
        assertEquals(106.82671, jakarta.lon, 1e-4);
        assertNull(Weather.place(all, "nope"));
        // Sorted by name, so Settings lists them alphabetically.
        assertEquals("Ambon", all.get(0).name);
    }

    @Test
    public void nowIsTheNearestBmkgSlot() throws Exception {
        Weather.Now n = Weather.parseNow(resource("weather-bmkg.json"), LocalDateTime.of(2026, 11, 20, 13, 10));
        assertNotNull(n);
        assertEquals(30, n.temp, 1e-9);
        assertEquals(2, n.code); // partly cloudy
        assertEquals(LocalDateTime.of(2026, 11, 20, 13, 0), n.time);
        assertTrue("feels warmer than it is", n.feels > n.temp);
        assertNull(Weather.parseNow(resource("weather-bmkg.json"), LocalDateTime.of(2026, 11, 25, 12, 0)));
    }

    @Test
    public void feelsLikeIsTheHeatIndex() {
        assertEquals(30, Weather.feels(30, Double.NaN), 1e-9); // no humidity: the temperature itself
        assertEquals(20, Weather.feels(20, 90), 1e-9);         // cool: no heat index
        assertTrue(Weather.feels(32, 80) > 32);
        assertTrue(Weather.feels(35, 90) > Weather.feels(35, 60));
    }

    @Test
    public void readsCopernicusAirQuality() throws Exception {
        Weather.Air a = Weather.parseAir(resource("weather-air.json"));
        assertNotNull(a);
        assertEquals(153, a.aqi);
        assertEquals(36.6, a.pm25, 1e-9);
        assertEquals(1.03, a.haze, 1e-9);
        assertEquals(LocalDateTime.of(2026, 11, 20, 13, 0), a.time);
        assertTrue(Weather.usAqiBand(a.aqi).contains("Unhealthy"));
        assertEquals(2, Weather.pm25Band(a.pm25)); // "Sensitive": 35.4 < 36.6 ≤ 55.4
    }

    @Test
    public void hourlyReadingsAreDueOncePerHour() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 20, 13, 5);
        assertTrue(Weather.hourlyDue(null, now, 1, 600_000, 300_000));
        assertTrue(Weather.hourlyDue(LocalDateTime.of(2026, 11, 20, 11, 0), now, 1, 600_000, 300_000));
        assertTrue(Weather.hourlyDue(LocalDateTime.of(2026, 11, 20, 12, 0), now, 1, 600_000, 300_000)); // 13:00 is out
        assertFalse(Weather.hourlyDue(LocalDateTime.of(2026, 11, 20, 13, 0), now, 1, 600_000, 300_000));
        assertFalse(Weather.hourlyDue(null, now, 1, 60_000, 300_000)); // tried a minute ago: wait
    }

    @Test
    public void rainAndLightningFollowTheWeatherCode() {
        assertEquals(Weather.FX_MIST, Weather.rainEffect(45)); // fog
        assertEquals(Weather.FX_LIGHT, Weather.rainEffect(61)); // light rain
        assertEquals(Weather.FX_RAIN, Weather.rainEffect(63));
        assertEquals(Weather.FX_HEAVY, Weather.rainEffect(65));
        assertEquals(Weather.FX_NONE, Weather.rainEffect(2)); // partly cloudy
        assertTrue(Weather.thunder(95));
        assertFalse(Weather.thunder(61));
    }

    @Test
    public void wordsCoverIndonesianAndEnglishDescriptions() {
        assertEquals(Weather.SUN, Weather.kind("Cerah"));
        assertEquals(Weather.SUN, Weather.kind("Clear"));
        assertEquals(Weather.CLOUD, Weather.kind("Berawan"));
        assertEquals(Weather.RAIN, Weather.kind("Hujan Lebat"));
        assertEquals(Weather.RAIN, Weather.kind("Heavy Rain"));
        assertEquals(Weather.STORM, Weather.kind("Hujan Petir"));
        assertEquals(Weather.HAZE, Weather.kind("Asap"));
        assertEquals(Weather.CLOUD, Weather.kind("Kabut"));
        assertEquals("Storms", Weather.word("Hujan Petir"));
        assertEquals("Fair", Weather.shortOutlook(new Weather.Day(20, 30, "Cerah", "", "", "")));
        assertEquals("Hujan Lebat", Weather.worse("Berawan", "Hujan Lebat"));
    }

    @Test
    public void bandsFollowTheUsScale() {
        assertEquals("Good", Weather.US_SHORT[Weather.band(40, Weather.US_TOP)]);
        assertEquals("Moderate", Weather.US_SHORT[Weather.band(80, Weather.US_TOP)]);
        assertEquals("Unhealthy", Weather.US_SHORT[Weather.band(180, Weather.US_TOP)]);
        assertEquals("Hazardous", Weather.US_SHORT[Weather.band(900, Weather.US_TOP)]);
        assertEquals("Good", Weather.US_SHORT[Weather.pm25Band(8)]);
        assertEquals("Hazardous", Weather.US_SHORT[Weather.pm25Band(300)]);
    }

    private static String resource(String name) throws Exception {
        try (InputStream in = WeatherTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("missing test resource: " + name);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
