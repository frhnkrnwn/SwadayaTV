package com.gathin.tvhome;

/**
 * Kalkulator waktu sholat standalone Kemenag/Bimas Islam (offline).
 * Parameter: Fajr 20°, Isya 18°, sunset 0,833°, ihtiyat {2,3,2,3,2} menit.
 */
final class KemenagPrayer {
    private KemenagPrayer() {}

    private static final double FAJR_ANGLE = 20.0;
    private static final double ISHA_ANGLE = 18.0;
    private static final double SUNSET_ANGLE = 0.833;
    private static final double[] IHTIYAT = {2, 3, 2, 3, 2};

    /** Nama sholat sesuai Kemenag: Subuh, Dzuhur, Ashar, Maghrib, Isya. */
    static String[] names() {
        return new String[]{"Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"};
    }

    /**
     * Hitung 5 waktu sholat (epoch ms) untuk koordinat lat/lon.
     * dayOffset 0 = hari ini, 1 = besok.
     */
    static long[] times(double lat, double lon, long now, int dayOffset) {
        // Perkiraan zona waktu dari longitude (WIB=7, WITA=8, WIT=9) agar cocok per daerah.
        double tz = Math.round(lon / 15.0);
        return times(lat, lon, now, dayOffset, tz);
    }

    static long[] times(double lat, double lon, long now, int dayOffset, double tzOffsetHours) {
        java.util.TimeZone zona = java.util.TimeZone.getTimeZone("GMT" + (tzOffsetHours >= 0 ? "+" : "") + (int) tzOffsetHours + ":00");
        java.util.Calendar kal = java.util.Calendar.getInstance(zona);
        kal.setTimeInMillis(now);
        kal.add(java.util.Calendar.DAY_OF_MONTH, dayOffset);
        kal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        kal.set(java.util.Calendar.MINUTE, 0);
        kal.set(java.util.Calendar.SECOND, 0);
        kal.set(java.util.Calendar.MILLISECOND, 0);
        long tengahMalam = kal.getTimeInMillis();
        double tz = zona.getOffset(tengahMalam + 12 * 3_600_000L) / 3_600_000.0;
        double jd = hariJulian(kal.get(java.util.Calendar.YEAR),
                kal.get(java.util.Calendar.MONTH) + 1,
                kal.get(java.util.Calendar.DAY_OF_MONTH)) - lon / 360.0;

        double[][] tebakan = {{5, 12, 13, 18, 18}};
        double[] t = tebakan[0];
        // Dua kali iterasi agar posisi matahari lebih tepat.
        for (int i = 0; i < 2; i++) {
            double[] h = new double[5];
            h[0] = tengahHari(jd, t[0], tz, lon) - busurSudut(jd, t[0], lat, FAJR_ANGLE);
            h[1] = tengahHari(jd, t[1], tz, lon);
            h[2] = tengahHari(jd, t[2], tz, lon) + busurAshar(jd, t[2], lat);
            h[3] = tengahHari(jd, t[3], tz, lon) + busurSudut(jd, t[3], lat, SUNSET_ANGLE);
            h[4] = tengahHari(jd, t[4], tz, lon) + busurSudut(jd, t[4], lat, ISHA_ANGLE);
            t = h;
        }
        long[] keluar = new long[5];
        for (int i = 0; i < 5; i++) {
            // Bulatkan ke atas per menit, seperti jadwal cetak Bimas Islam.
            double menit = Math.ceil(t[i] * 60 + IHTIYAT[i]);
            keluar[i] = tengahMalam + Math.round(menit) * 60_000L;
        }
        return keluar;
    }

    /** Terbit matahari (Syuruk) untuk koordinat lat/lon, epoch ms. */
    static long sunrise(double lat, double lon, long now, int dayOffset) {
        double tzPerkiraan = Math.round(lon / 15.0);
        java.util.TimeZone zona = java.util.TimeZone.getTimeZone(
                "GMT" + (tzPerkiraan >= 0 ? "+" : "") + (int) tzPerkiraan + ":00");
        java.util.Calendar kal = java.util.Calendar.getInstance(zona);
        kal.setTimeInMillis(now);
        kal.add(java.util.Calendar.DAY_OF_MONTH, dayOffset);
        kal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        kal.set(java.util.Calendar.MINUTE, 0);
        kal.set(java.util.Calendar.SECOND, 0);
        kal.set(java.util.Calendar.MILLISECOND, 0);
        long tengahMalam = kal.getTimeInMillis();
        double tz = zona.getOffset(tengahMalam + 12 * 3_600_000L) / 3_600_000.0;
        double jd = hariJulian(kal.get(java.util.Calendar.YEAR),
                kal.get(java.util.Calendar.MONTH) + 1, kal.get(java.util.Calendar.DAY_OF_MONTH)) - lon / 360.0;
        double t = 6.0;
        for (int i = 0; i < 2; i++) {
            t = tengahHari(jd, t, tz, lon) - busurSudut(jd, t, lat, SUNSET_ANGLE);
        }
        double menit = Math.ceil(t * 60);
        return tengahMalam + Math.round(menit) * 60_000L;
    }

    /** {index, waktu} sholat berikutnya; setelah Isya = Subuh besok. */
    static long[] next(double lat, double lon) {
        return next(lat, lon, System.currentTimeMillis());
    }

    /** {index, waktu} dengan waktu acuan now. */
    static long[] next(double lat, double lon, long now) {
        long[] hariIni = times(lat, lon, now, 0);
        for (int i = 0; i < 5; i++) {
            if (hariIni[i] > now) return new long[]{i, hariIni[i]};
        }
        return new long[]{0, times(lat, lon, now, 1)[0]};
    }

    // ---- rumus posisi matahari (presisi rendah, cukup <1 menit) ----

    private static double hariJulian(int tahun, int bulan, int tanggal) {
        if (bulan <= 2) {
            tahun -= 1;
            bulan += 12;
        }
        double a = Math.floor(tahun / 100.0);
        double b = 2 - a + Math.floor(a / 4);
        return Math.floor(365.25 * (tahun + 4716))
                + Math.floor(30.6001 * (bulan + 1)) + tanggal + b - 1524.5;
    }

    /** {deklinasi, persamaan waktu dalam jam} pada hari Julian jd. */
    private static double[] matahari(double jd) {
        double d = jd - 2451545.0;
        double g = ambilIrama(357.529 + 0.98560028 * d, 360);
        double q = ambilIrama(280.459 + 0.98564736 * d, 360);
        double l = ambilIrama(q + 1.915 * sinDerajat(g) + 0.020 * sinDerajat(2 * g), 360);
        double e = 23.439 - 0.00000036 * d;
        double ra = ambilIrama(atan2Derajat(cosDerajat(e) * sinDerajat(l), cosDerajat(l)) / 15, 24);
        return new double[]{asinDerajat(sinDerajat(e) * sinDerajat(l)), q / 15 - ra};
    }

    /** Jam tengah hari lokal untuk jam t. */
    private static double tengahHari(double jd, double t, double tz, double lon) {
        double eqt = matahari(jd + t / 24)[1];
        return ambilIrama(12 - eqt, 24) + tz - lon / 15;
    }

    /** Jam dari tengah hari ke matahari pada sudut di bawah ufuk. */
    private static double busurSudut(double jd, double t, double lat, double sudut) {
        double deklinasi = matahari(jd + t / 24)[0];
        double v = (-sinDerajat(sudut) - sinDerajat(deklinasi) * sinDerajat(lat))
                / (cosDerajat(deklinasi) * cosDerajat(lat));
        return acosDerajat(potong(v, -1, 1)) / 15;
    }

    /** Jam dari tengah hari ke Ashar (bayangan = benda + bayangan tengah hari). */
    private static double busurAshar(double jd, double t, double lat) {
        double deklinasi = matahari(jd + t / 24)[0];
        double sudut = -Math.toDegrees(Math.atan(
                1 / (1 + Math.tan(Math.toRadians(Math.abs(lat - deklinasi))))));
        return busurSudut(jd, t, lat, sudut);
    }

    private static double ambilIrama(double v, double jarak) {
        v = v - jarak * Math.floor(v / jarak);
        return v < 0 ? v + jarak : v;
    }

    private static double potong(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double sinDerajat(double d) { return Math.sin(Math.toRadians(d)); }
    private static double cosDerajat(double d) { return Math.cos(Math.toRadians(d)); }
    private static double asinDerajat(double v) { return Math.toDegrees(Math.asin(v)); }
    private static double acosDerajat(double v) { return Math.toDegrees(Math.acos(v)); }
    private static double atan2Derajat(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
}
