package com.swadaya.tv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The Weather card's data, free of Android so it's unit tested:
 *  - BMKG's open forecast (api.bmkg.go.id, no key): three days, every three hours, per
 *    kelurahan (adm4). BMKG is credited on the card, as its terms ask.
 *  - Copernicus CAMS air quality (US AQI, PM2.5 and haze), from Open-Meteo (no key), for the
 *    chosen city.
 *
 * The places list comes from assets/places.json: the Indonesian cities the card can show,
 * each with its BMKG adm4 code and coordinates.
 */
final class Weather {
    private Weather() {}

    /** Today's forecast for a place: temperatures in °C and the outlook for each part of the day. */
    static final class Day {
        final int min, max;
        final String summary, morning, afternoon, night;

        Day(int min, int max, String summary, String morning, String afternoon, String night) {
            this.min = min;
            this.max = max;
            this.summary = summary;
            this.morning = morning;
            this.afternoon = afternoon;
            this.night = night;
        }
    }

    /** An Indonesian city: its BMKG adm4 code, province and coordinates. */
    static final class Place {
        final String id, name, province;
        final double lat, lon;

        Place(String id, String name, String province, double lat, double lon) {
            this.id = id;
            this.name = name;
            this.province = province;
            this.lat = lat;
            this.lon = lon;
        }

        /** The line under the place's name in Settings. */
        String type() {
            return province;
        }
    }

    /**
     * Copernicus's air quality now: US AQI, PM2.5 (µg/m³), haze as aerosol optical depth (how much
     * the air's particles dim the sunlight: about 0.1 clear, 1 thick haze) and the model hour it's
     * for (local time). Missing numbers are NaN.
     */
    static final class Air {
        final int aqi;
        final double pm25, haze;
        final LocalDateTime time;

        Air(int aqi, double pm25, double haze, LocalDateTime time) {
            this.aqi = aqi;
            this.pm25 = pm25;
            this.haze = haze;
            this.time = time;
        }
    }

    /**
     * BMKG's nearest forecast slot: the temperature and how hot it feels (°C), the WMO weather
     * code (-1 if missing) and the slot's local time.
     */
    static final class Now {
        final double temp, feels;
        final int code;
        final LocalDateTime time;

        Now(double temp, double feels, int code, LocalDateTime time) {
            this.temp = temp;
            this.feels = feels;
            this.code = code;
            this.time = time;
        }
    }

    // ---------------------------------------------------------------- parsing

    private static final DateTimeFormatter BMKG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** The row for {@code today} among BMKG's forecast days, or null. */
    static Day parseForecast(String json, String placeId, LocalDate today) {
        try {
            JSONArray data = new JSONObject(json).optJSONArray("data");
            if (data == null || data.length() == 0) return null;
            JSONObject first = data.optJSONObject(0);
            JSONObject loc = first == null ? null : first.optJSONObject("lokasi");
            if (loc == null) return null;
            String adm4 = loc.optString("adm4");
            if (placeId != null && !placeId.isEmpty() && !adm4.isEmpty() && !placeId.equals(adm4)) return null;
            JSONArray days = first.optJSONArray("cuaca");
            if (days == null) return null;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            String morning = "", afternoon = "", night = "", summary = "";
            for (int d = 0; d < days.length(); d++) {
                JSONArray slots = days.optJSONArray(d);
                if (slots == null) continue;
                for (int s = 0; s < slots.length(); s++) {
                    JSONObject sl = slots.optJSONObject(s);
                    if (sl == null) continue;
                    String local = sl.optString("local_datetime");
                    if (!local.startsWith(today.toString())) continue;
                    if (!sl.isNull("t")) {
                        int t = sl.optInt("t");
                        if (t < min) min = t;
                        if (t > max) max = t;
                    }
                    String desc = sl.optString("weather_desc_en", sl.optString("weather_desc"));
                    if (desc.isEmpty()) continue;
                    int h = hour(local);
                    // Each part of the day keeps its most severe slot (a rainy afternoon isn't lost
                    // to an earlier cloudy one), and the day's outlook is the worst of them.
                    if (h >= 5 && h < 12) morning = worse(morning, desc);
                    else if (h >= 12 && h < 18) afternoon = worse(afternoon, desc);
                    else if (h >= 18 || h < 5) night = worse(night, desc);
                    summary = worse(summary, desc);
                }
            }
            if (min == Integer.MAX_VALUE) return null;
            return new Day(min, max, summary, morning, afternoon, night);
        } catch (Exception e) {
            return null;
        }
    }

    /** BMKG's forecast slot nearest {@code now}: temperature, feels-like (heat index) and code. */
    static Now parseNow(String json, LocalDateTime now) {
        try {
            JSONArray data = new JSONObject(json).optJSONArray("data");
            if (data == null || data.length() == 0) return null;
            JSONArray days = data.getJSONObject(0).optJSONArray("cuaca");
            if (days == null) return null;
            JSONObject best = null;
            long bestGap = Long.MAX_VALUE;
            for (int d = 0; d < days.length(); d++) {
                JSONArray slots = days.optJSONArray(d);
                if (slots == null) continue;
                for (int s = 0; s < slots.length(); s++) {
                    JSONObject sl = slots.optJSONObject(s);
                    if (sl == null || sl.isNull("t")) continue;
                    LocalDateTime at;
                    try {
                        at = LocalDateTime.parse(sl.optString("local_datetime"), BMKG_TIME);
                    } catch (RuntimeException ignored) {
                        continue;
                    }
                    long gap = Math.abs(java.time.Duration.between(now, at).toMinutes());
                    if (gap < bestGap) {
                        bestGap = gap;
                        best = sl;
                    }
                }
            }
            if (best == null || bestGap > 6 * 60) return null;
            double temp = best.optDouble("t", Double.NaN);
            if (Double.isNaN(temp)) return null;
            double humidity = best.optDouble("hu", Double.NaN);
            LocalDateTime at = LocalDateTime.parse(best.optString("local_datetime"), BMKG_TIME);
            int code = best.isNull("weather") ? -1 : best.optInt("weather", -1);
            return new Now(temp, feels(temp, humidity), code, at);
        } catch (Exception e) {
            return null;
        }
    }

    /** Heat index (Rothfusz), in °C; the plain temperature when it's cool or the humidity is missing. */
    static double feels(double temp, double humidity) {
        if (Double.isNaN(humidity) || temp < 27) return temp;
        double t = temp, r = humidity;
        double hi = -8.78469475556 + 1.61139411 * t + 2.33854883889 * r - 0.14611605 * t * r
                - 0.012308094 * t * t - 0.0164248277778 * r * r + 0.002211732 * t * t * r
                + 0.00072546 * t * r * r - 0.000003582 * t * t * r * r;
        return hi;
    }

    /** Every city in assets/places.json, sorted by name. */
    static List<Place> parsePlaces(String json) {
        List<Place> out = new ArrayList<>();
        try {
            JSONArray rows = new JSONArray(json);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.optJSONObject(i);
                if (o == null) continue;
                String id = o.optString("id"), name = o.optString("name");
                if (id.isEmpty() || name.isEmpty()) continue;
                out.add(new Place(id, name, o.optString("province"),
                        o.optDouble("lat", Double.NaN), o.optDouble("lon", Double.NaN)));
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        out.sort((a, b) -> {
            int c = a.name.compareToIgnoreCase(b.name);
            return c != 0 ? c : a.id.compareTo(b.id);
        });
        return out;
    }

    static Place place(List<Place> places, String id) {
        for (Place p : places) if (p.id.equals(id)) return p;
        return null;
    }

    /** Open-Meteo's current air quality, or null. */
    static Air parseAir(String json) {
        try {
            JSONObject cur = new JSONObject(json).getJSONObject("current");
            if (cur.isNull("us_aqi")) return null;
            LocalDateTime time = null;
            try {
                time = LocalDateTime.parse(cur.optString("time")); // "2026-11-20T13:00", asked for in Jakarta time
            } catch (RuntimeException ignored) {
                // no time: the reading still shows
            }
            return new Air((int) Math.round(cur.getDouble("us_aqi")), cur.optDouble("pm2_5", Double.NaN),
                    cur.optDouble("aerosol_optical_depth", Double.NaN), time);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Whether an hourly reading is worth fetching: the source puts out each hour's reading
     * {@code publishMinute} minutes past the hour (Copernicus on the hour). Due when the one we
     * have ({@code have}; null: none) is older than the newest that should be out by {@code now},
     * and the last try was at least {@code retryMs} ago (it can be late).
     */
    static boolean hourlyDue(LocalDateTime have, LocalDateTime now, int publishMinute, long sinceFetchMs,
                             long retryMs) {
        if (sinceFetchMs < retryMs) return false;
        LocalDateTime hour = now.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        LocalDateTime newest = now.getMinute() >= publishMinute ? hour : hour.minusHours(1);
        return have == null || have.isBefore(newest);
    }

    // ---------------------------------------------------------------- home screen effects

    /** The home screen's rain on the glass: none, mist (drizzle, fog), light rain, rain, heavy rain. */
    static final int FX_NONE = 0, FX_MIST = 1, FX_LIGHT = 2, FX_RAIN = 3, FX_HEAVY = 4;

    /** The rain effect for a WMO weather code (BMKG's weather code now). */
    static int rainEffect(int wmo) {
        switch (wmo) {
            case 45: case 48: // fog
            case 51: case 53: case 55: case 56: case 57: // drizzle
                return FX_MIST;
            case 61: case 66: case 80: // slight rain, showers
                return FX_LIGHT;
            case 63: case 81: case 95: // moderate rain, showers, a thunderstorm
                return FX_RAIN;
            case 65: case 67: case 82: case 96: case 99: // heavy rain, violent showers, storms with hail
                return FX_HEAVY;
            default:
                return FX_NONE;
        }
    }

    /** Lightning: a thunderstorm now (WMO 95–99). */
    static boolean thunder(int wmo) {
        return wmo == 95 || wmo == 96 || wmo == 99;
    }

    // ---------------------------------------------------------------- words

    /** Sun (no rain), cloud, rain, storm or haze: the card's icon. */
    static final int SUN = 0, CLOUD = 1, RAIN = 2, STORM = 3, HAZE = 4;

    /** The more severe of two descriptions: storms over rain, rain over haze, haze over cloud, cloud over sun. */
    static String worse(String a, String b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        int ra = severity(a), rb = severity(b);
        return rb > ra ? b : a;
    }

    private static int severity(String desc) {
        switch (kind(desc)) {
            case STORM: return 4;
            case RAIN: return 3;
            case HAZE: return 2;
            case SUN: return 0;
            default: return 1;
        }
    }

    /** BMKG's description, in English (weather_desc_en) or Indonesian, as a card icon. */
    static int kind(String summary) {
        String s = summary.toLowerCase(Locale.ROOT);
        if (s.contains("thunder") || s.contains("ribut") || s.contains("petir") || s.contains("badai")) return STORM;
        if (s.contains("haze") || s.contains("jerebu") || s.contains("asap") || s.contains("smoke")) return HAZE;
        if (s.contains("rain") || s.contains("hujan")) return RAIN;
        if (s.contains("clear") || s.contains("sunny") || s.contains("cerah")) return SUN;
        return CLOUD; // cloudy, berawan, fog, anything new
    }

    /** The outlook in a line: BMKG already gives it in English ("Partly Cloudy", "Light Rain"). */
    static String outlook(Day d) {
        return d.summary.trim();
    }

    /** One word or two for a part of the day: "Storms", "Rain", "Cloudy", "Fair". */
    static String word(String phrase) {
        switch (kind(phrase)) {
            case SUN: return "Fair";
            case STORM: return "Storms";
            case RAIN: return "Rain";
            case HAZE: return "Hazy";
            default: return phrase.trim();
        }
    }

    /** For the card's small label: "Storms", "Rain", "Cloudy", "Fair". */
    static String shortOutlook(Day d) {
        return word(d.summary);
    }

    private static int hour(String local) {
        try {
            return Integer.parseInt(local.substring(11, 13));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    // ---------------------------------------------------------------- bands

    private static final int GREEN = 0xFF63D489, YELLOW = 0xFFF2DC6B, ORANGE = 0xFFFFB340,
            RED = 0xFFFF6B5E, PURPLE = 0xFFB38AE3, MAROON = 0xFFC9607A;

    /** The US EPA's AQI bands, names (short for the card) and colours. */
    static final int[] US_TOP = {50, 100, 150, 200, 300, 500};
    static final String[] US_BANDS = {"Good", "Moderate", "Unhealthy for sensitive groups", "Unhealthy",
            "Very unhealthy", "Hazardous"};
    static final String[] US_SHORT = {"Good", "Moderate", "Sensitive", "Unhealthy", "V. unhealthy", "Hazardous"};
    static final int[] US_COLORS = {GREEN, YELLOW, ORANGE, RED, PURPLE, MAROON};

    /** PM2.5 (µg/m³) bands, the same breakpoints the US AQI uses for PM2.5. */
    static final double[] PM25_TOP = {12.0, 35.4, 55.4, 150.4, 250.4, 500.0};

    /** Which band a value is in (the last one past the scale's end). */
    static int band(int value, int[] tops) {
        for (int i = 0; i < tops.length; i++) if (value <= tops[i]) return i;
        return tops.length - 1;
    }

    static int pm25Band(double pm25) {
        for (int i = 0; i < PM25_TOP.length; i++) if (pm25 <= PM25_TOP[i]) return i;
        return PM25_TOP.length - 1;
    }

    static String usAqiBand(int aqi) {
        return US_BANDS[band(aqi, US_TOP)];
    }

    static int usAqiColor(int aqi) {
        return US_COLORS[band(aqi, US_TOP)];
    }
}
