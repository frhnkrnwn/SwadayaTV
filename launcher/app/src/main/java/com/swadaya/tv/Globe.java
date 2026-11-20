package com.swadaya.tv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Globe screen saver's arithmetic and feed parsing, kept free of Android so it can be unit
 * tested: where the sun is overhead right now, how high it stands anywhere, the prayer cities'
 * map positions, the two live feeds (OpenStreetMap edits, USGS earthquakes), and the aurora,
 * snow and sea-ice maps. Satellites: Orbit.
 *
 * Earth-fixed coordinates (also in GlobeView's shaders): y to the north pole, z to latitude 0
 * longitude 0, x to longitude 90° E.
 */
final class Globe {
    static final double MAKKAH_LAT = 21.4225, MAKKAH_LON = 39.8262;
    /**
     * Sun altitudes (degrees) of the two prayer lines: sunset (Maghrib), and dawn at the angle
     * Kemenag's Subuh times use (they put the sun 20° below the horizon).
     */
    static final double MAGHRIB_ALT = -0.833, FAJR_ALT = -20;

    private Globe() {}

    private static double days(long epochMs) {
        return epochMs / 86_400_000.0 + 2440587.5 - 2451545.0; // days since J2000.0
    }

    /** How far the Earth has turned (Greenwich mean sidereal time), in degrees. */
    static double siderealDegrees(long epochMs) {
        return wrap((18.697374558 + 24.06570982441908 * days(epochMs)) * 15);
    }

    /** The tilt of the Earth's axis against its orbit, in degrees (23.44° now). */
    static double obliquity(long epochMs) {
        return 23.439 - 0.00000036 * days(epochMs);
    }

    /**
     * An Earth-fixed vector in the ecliptic frame at this moment (the plane of the Earth's orbit):
     * y to the ecliptic's north pole, z to the March equinox, x to where the sun stands at the
     * June solstice. Turns the Earth by sidereal time, then tips its axis by the obliquity, so
     * the north pole comes out as (sin 23.44°, cos 23.44°, 0): leaning towards the June sun.
     */
    static double[] toEcliptic(double[] v, long epochMs) {
        double t = Math.toRadians(siderealDegrees(epochMs)), e = Math.toRadians(obliquity(epochMs));
        // Longitude → right ascension (lon + sidereal time), keeping the vector() layout.
        double x = v[0] * Math.cos(t) + v[2] * Math.sin(t), y = v[1], z = v[2] * Math.cos(t) - v[0] * Math.sin(t);
        return new double[]{x * Math.cos(e) + y * Math.sin(e), -x * Math.sin(e) + y * Math.cos(e), z};
    }

    /** Where the sun stands overhead at this moment: {latitude, longitude} in degrees. */
    static double[] subsolar(long epochMs) {
        // The US Naval Observatory's low-precision formulas: within about a minute of arc this century.
        double n = days(epochMs);
        double g = Math.toRadians(357.529 + 0.98560028 * n);         // mean anomaly
        double q = 280.459 + 0.98564736 * n;                         // mean longitude
        double l = Math.toRadians(q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g)); // ecliptic longitude
        double e = Math.toRadians(obliquity(epochMs));
        double ra = Math.toDegrees(Math.atan2(Math.cos(e) * Math.sin(l), Math.cos(l)));
        double dec = Math.toDegrees(Math.asin(Math.sin(e) * Math.sin(l)));
        return new double[]{dec, wrap(ra - siderealDegrees(epochMs))};
    }

    /** The sun's altitude above the horizon (degrees) at a place, ignoring refraction. */
    static double sunAltitude(double lat, double lon, long epochMs) {
        double[] s = subsolar(epochMs);
        double[] a = vector(lat, lon), b = vector(s[0], s[1]);
        return Math.toDegrees(Math.asin(clamp(a[0] * b[0] + a[1] * b[1] + a[2] * b[2])));
    }

    /**
     * Whether the sun is rising at a place (morning: the Fajr side) rather than setting (the
     * Maghrib side). The sun turns westward round the Earth's axis, so the rate its height
     * changes at a place is e · (−y × s): the same sum GlobeView's shader uses to keep each
     * prayer line on its own half (the −18° circle is Fajr at dawn but Isya at dusk).
     */
    static boolean sunRising(double lat, double lon, long epochMs) {
        double[] s = subsolar(epochMs), sun = vector(s[0], s[1]), e = vector(lat, lon);
        return sun[0] * e[2] - sun[2] * e[0] > 0;
    }

    /** A place as a unit vector in Earth-fixed coordinates (see the class comment). */
    static double[] vector(double lat, double lon) {
        double la = Math.toRadians(lat), lo = Math.toRadians(lon);
        return new double[]{Math.cos(la) * Math.sin(lo), Math.sin(la), Math.cos(la) * Math.cos(lo)};
    }

    /** {@code steps + 1} points along the shorter great circle from a to b (unit vectors). */
    static List<double[]> greatCircle(double latA, double lonA, double latB, double lonB, int steps) {
        double[] a = vector(latA, lonA), b = vector(latB, lonB);
        double w = Math.acos(clamp(a[0] * b[0] + a[1] * b[1] + a[2] * b[2]));
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            if (w < 1e-9) {
                out.add(a.clone());
                continue;
            }
            double ka = Math.sin((1 - t) * w) / Math.sin(w), kb = Math.sin(t * w) / Math.sin(w);
            out.add(new double[]{ka * a[0] + kb * b[0], ka * a[1] + kb * b[1], ka * a[2] + kb * b[2]});
        }
        return out;
    }

    static double wrap(double lon) {
        lon = ((lon + 180) % 360 + 360) % 360 - 180;
        return lon == -180 ? 180 : lon;
    }

    private static double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }

    // Each Indonesian city: where the globe marks "you" and starts the qibla arc.
    private static final Map<String, double[]> ZONES = new HashMap<>();

    private static void zone(String code, double lat, double lon) {
        ZONES.put(code, new double[]{lat, lon});
    }

    static {
        zone("Banda Aceh", 5.55237, 95.31703);
        zone("Medan", 3.58399, 98.68474);
        zone("Padang", -0.95565, 100.36246);
        zone("Pekanbaru", 0.514, 101.44499);
        zone("Jambi", -1.61557, 103.57677);
        zone("Palembang", -3.00802, 104.7436);
        zone("Bengkulu", -3.84067, 102.32073);
        zone("Bandar Lampung", -5.38335, 105.26349);
        zone("Pangkal Pinang", -2.13281, 106.12902);
        zone("Batam", 1.14145, 104.01986);
        zone("Tanjung Pinang", 0.91587, 104.44221);
        zone("Jakarta", -6.17638, 106.82671);
        zone("Bogor", -6.62161, 106.80873);
        zone("Sukabumi", -6.91431, 106.91672);
        zone("Bandung", -6.87423, 107.5854);
        zone("Cirebon", -6.71251, 108.5582);
        zone("Bekasi", -6.23336, 107.01356);
        zone("Depok", -6.39895, 106.82347);
        zone("Magelang", -7.49209, 110.22448);
        zone("Surakarta", -7.56714, 110.78626);
        zone("Semarang", -6.98371, 110.41869);
        zone("Yogyakarta", -7.7736, 110.35719);
        zone("Kediri", -7.81866, 112.0034);
        zone("Malang", -7.92252, 112.65017);
        zone("Surabaya", -7.34058, 112.68994);
        zone("Tangerang", -6.17288, 106.63185);
        zone("Cilegon", -6.03311, 106.06966);
        zone("Serang", -6.12797, 106.14685);
        zone("Tangerang Selatan", -6.32231, 106.69429);
        zone("Denpasar", -8.73626, 115.23211);
        zone("Mataram", -8.58085, 116.07551);
        zone("Kupang", -10.17566, 123.55335);
        zone("Pontianak", -0.03565, 109.34885);
        zone("Singkawang", 0.90897, 109.01413);
        zone("Palangka Raya", -2.19975, 113.95033);
        zone("Banjarmasin", -3.35646, 114.54687);
        zone("Banjarbaru", -3.47016, 114.74147);
        zone("Balikpapan", -1.21442, 116.93774);
        zone("Samarinda", -0.57219, 117.1775);
        zone("Bontang", 0.13358, 117.50849);
        zone("Tarakan", 3.32551, 117.58456);
        zone("Tanjung Selor", 2.82501, 117.36969);
        zone("Manado", 1.54438, 124.84174);
        zone("Bitung", 1.40216, 125.18515);
        zone("Tomohon", 1.29108, 124.7909);
        zone("Palu", -0.88924, 119.86583);
        zone("Makassar", -5.16218, 119.40425);
        zone("Palopo", -3.00891, 120.19936);
        zone("Kendari", -3.9673, 122.51365);
        zone("Baubau", -5.48177, 122.57375);
        zone("Gorontalo", 0.54691, 123.00509);
        zone("Mamuju", -2.74919, 118.96735);
        zone("Ambon", -3.7793, 128.11045);
        zone("Tual", -5.59097, 132.76836);
        zone("Ternate", 0.76141, 127.31969);
        zone("Jayapura", -2.52644, 140.65689);
        zone("Manokwari", -0.8535, 134.06195);
        zone("Sorong", -0.87645, 131.28333);
    }

    /** A city's {latitude, longitude}; Jakarta for one this list doesn't know. */
    static double[] zonePlace(String code) {
        double[] p = code == null ? null : ZONES.get(code);
        return (p != null ? p : ZONES.get("Jakarta")).clone();
    }

    static boolean knowsZone(String code) {
        return ZONES.containsKey(code);
    }

    /** A dot from a feed: where, when (ms), how big (magnitude; 0 when not a quake), and a line of text. */
    static final class Spot {
        final double lat, lon;
        final long time;
        final float mag;
        final String text;

        Spot(double lat, double lon, long time, float mag, String text) {
            this.lat = lat;
            this.lon = lon;
            this.time = time;
            this.mag = mag;
            this.text = text;
        }
    }

    private static final Pattern CHANGESET = Pattern.compile(
            "<changeset\\b([^>]*?)(?:/>|>(.*?)</changeset>)", Pattern.DOTALL);
    private static final Pattern COMMENT = Pattern.compile("<tag k=\"comment\" v=\"([^\"]*)\"");

    /**
     * OpenStreetMap's minute file of changesets (replication/changesets/NNN/NNN/NNN.osm): one spot per
     * changeset with an area, in the middle of it, with its comment. Who made it is left out.
     */
    static List<Spot> parseOsm(String xml) {
        List<Spot> out = new ArrayList<>();
        if (xml == null) return out;
        Matcher m = CHANGESET.matcher(xml);
        while (m.find()) {
            String attrs = m.group(1);
            Double minLat = num(attr(attrs, "min_lat")), maxLat = num(attr(attrs, "max_lat")),
                    minLon = num(attr(attrs, "min_lon")), maxLon = num(attr(attrs, "max_lon"));
            if (minLat == null || maxLat == null || minLon == null || maxLon == null) continue; // nothing mapped
            String comment = "";
            if (m.group(2) != null) {
                Matcher c = COMMENT.matcher(m.group(2));
                if (c.find()) comment = unescapeXml(c.group(1)).trim();
            }
            out.add(new Spot((minLat + maxLat) / 2, (minLon + maxLon) / 2,
                    isoMs(attr(attrs, "closed_at")), 0, comment));
        }
        return out;
    }

    /** The sequence number in OpenStreetMap's replication state.yaml, or -1. */
    static long parseOsmSequence(String yaml) {
        Matcher m = Pattern.compile("(?m)^sequence:\\s*(\\d+)").matcher(yaml == null ? "" : yaml);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    /** The minute file's path for a sequence number: 7247203 → "007/247/203.osm.gz". */
    static String osmPath(long sequence) {
        String s = String.format(java.util.Locale.ROOT, "%09d", sequence);
        return s.substring(0, 3) + "/" + s.substring(3, 6) + "/" + s.substring(6) + ".osm.gz";
    }

    private static final Pattern FEATURE = Pattern.compile("\\{\"type\":\"Feature\"");
    private static final Pattern MAG = Pattern.compile("\"mag\":(-?[\\d.]+)");
    private static final Pattern PLACE = Pattern.compile("\"place\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern TIME = Pattern.compile("\"time\":(\\d+)");
    private static final Pattern COORDS = Pattern.compile("\"coordinates\":\\[\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)");

    /** USGS earthquake GeoJSON: one spot per quake with a magnitude, newest first as the feed has them. */
    static List<Spot> parseQuakes(String json) {
        List<Spot> out = new ArrayList<>();
        if (json == null) return out;
        String[] parts = FEATURE.split(json);
        for (int i = 1; i < parts.length; i++) {
            String f = parts[i];
            Matcher mag = MAG.matcher(f), time = TIME.matcher(f), xy = COORDS.matcher(f), place = PLACE.matcher(f);
            if (!mag.find() || !time.find() || !xy.find()) continue;
            Double lon = num(xy.group(1)), lat = num(xy.group(2)), m = num(mag.group(1));
            if (lat == null || lon == null || m == null) continue;
            out.add(new Spot(lat, lon, Long.parseLong(time.group(1)), m.floatValue(),
                    place.find() ? unescapeJson(place.group(1)) : ""));
        }
        return out;
    }

    /**
     * NOAA's GFS forecast for this moment, {@code back} runs before the newest that's likely out:
     * the URL of its 10 m wind (both components, 1°, from NOMADS' grib filter). GFS runs every
     * 6 hours and takes about 4 to appear; its 1° forecasts come every 3 hours, so this picks the
     * step nearest to now.
     */
    static String gfsWindUrl(long nowMs, int back) {
        long six = 6 * 3_600_000L, run = (nowMs - 4 * 3_600_000L) / six * six - back * six;
        int hour = (int) Math.min(120, Math.round((nowMs - run) / (3 * 3_600_000.0)) * 3);
        java.time.ZonedDateTime t = java.time.Instant.ofEpochMilli(run).atZone(java.time.ZoneOffset.UTC);
        String day = String.format(java.util.Locale.ROOT, "%04d%02d%02d", t.getYear(), t.getMonthValue(), t.getDayOfMonth());
        String hh = String.format(java.util.Locale.ROOT, "%02d", t.getHour());
        return "https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_1p00.pl?dir=%2Fgfs." + day + "%2F" + hh + "%2Fatmos"
                + "&file=gfs.t" + hh + "z.pgrb2.1p00.f" + String.format(java.util.Locale.ROOT, "%03d", hour)
                + "&var_UGRD=on&var_VGRD=on&lev_10_m_above_ground=on";
    }

    /** The aurora grid: one byte per degree, 360 wide (−180° first) × 181 tall (90° N first). */
    static final int AURORA_W = 360, AURORA_H = 181;

    /**
     * NOAA SWPC's OVATION aurora forecast (ovation_aurora_latest.json: "coordinates": [[lon 0–359,
     * lat −90–90, chance 0–100], …]) as a grid laid out like the globe's maps, chance → 0–255.
     * Null when there's nothing to read. Scanned by hand: it's 65,000 triples.
     */
    static byte[] parseAurora(String json) {
        if (json == null) return null;
        int i = json.indexOf("\"coordinates\"");
        if (i < 0) return null;
        byte[] grid = new byte[AURORA_W * AURORA_H];
        int[] v = new int[3];
        int n = 0, len = json.length();
        while ((i = json.indexOf('[', i + 1)) >= 0) {
            int k = 0, j = i + 1;
            while (k < 3 && j < len) { // three integers, maybe negative, separated by commas
                while (j < len && (json.charAt(j) == ' ' || json.charAt(j) == ',')) j++;
                boolean neg = j < len && json.charAt(j) == '-';
                if (neg) j++;
                int start = j, num = 0;
                while (j < len && Character.isDigit(json.charAt(j))) num = num * 10 + json.charAt(j++) - '0';
                if (j == start) break;
                v[k++] = neg ? -num : num;
            }
            if (k < 3) continue; // the outer "[[" or something else
            int lon = ((v[0] % 360) + 360) % 360, lat = v[1];
            if (lat < -90 || lat > 90) continue;
            int col = (lon + 180) % 360, row = 90 - lat;
            grid[row * AURORA_W + col] = (byte) Math.round(Math.max(0, Math.min(100, v[2])) * 2.55f);
            n++;
        }
        return n > 0 ? grid : null;
    }

    /**
     * How snowy a pixel of NASA's SMAP snow mass map is, 0–255: the map paints land without snow
     * one grey (82, 98, 106) and snow in colours away from it, so it's the distance from that grey.
     */
    static int snowLevel(int argb) {
        if ((argb >>> 24) < 128) return 0;
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        double d = Math.sqrt((r - 82) * (r - 82) + (g - 98) * (g - 98) + (b - 106) * (b - 106));
        return (int) Math.round(255 * Math.max(0, Math.min(1, (d - 8) / 60)));
    }

    /** Sea ice in a pixel of NASA's GHRSST ice map, 0–255: drawn wherever there's ice. */
    static int iceLevel(int argb) {
        return (argb >>> 24) < 128 ? 0 : 230;
    }

    private static String attr(String attrs, String name) {
        Matcher m = Pattern.compile("\\b" + name + "=\"([^\"]*)\"").matcher(attrs);
        return m.find() ? m.group(1) : null;
    }

    private static Double num(String s) {
        if (s == null) return null;
        try {
            double v = Double.parseDouble(s);
            return Double.isNaN(v) || Double.isInfinite(v) ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long isoMs(String s) {
        try {
            return s == null ? 0 : java.time.Instant.parse(s).toEpochMilli();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    static String unescapeXml(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int end = ch == '&' ? s.indexOf(';', i) : -1;
            if (end < 0 || end - i > 10) {
                out.append(ch);
                continue;
            }
            String ent = s.substring(i + 1, end);
            String rep = switch (ent) {
                case "amp" -> "&";
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "apos" -> "'";
                default -> codePoint(ent);
            };
            if (rep == null) {
                out.append(ch);
                continue;
            }
            out.append(rep);
            i = end;
        }
        return out.toString();
    }

    private static String codePoint(String ent) {
        try {
            if (ent.startsWith("#x")) return new String(Character.toChars(Integer.parseInt(ent.substring(2), 16)));
            if (ent.startsWith("#")) return new String(Character.toChars(Integer.parseInt(ent.substring(1))));
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    static String unescapeJson(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\' || i + 1 >= s.length()) {
                out.append(ch);
                continue;
            }
            char n = s.charAt(++i);
            if (n == 'u' && i + 4 < s.length()) {
                try {
                    out.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            out.append(n == 'n' || n == 't' || n == 'r' ? ' ' : n);
        }
        return out.toString();
    }
}
