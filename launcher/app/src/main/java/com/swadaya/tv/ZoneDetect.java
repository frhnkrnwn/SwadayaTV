package com.swadaya.tv;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * The prayer city's "Auto": the Indonesian city the TV is in, from its location. The TV's
 * network location (Google's, from nearby Wi-Fi networks; SwadayaTV already has the location
 * permission) is matched offline to the nearest city in places.json: no online service is asked.
 * Near a city border it can pick the neighbour, where the times differ by a minute or two.
 */
final class ZoneDetect {
    // Zone, then latitude/longitude pairs of its main towns (approximate, to about a kilometre).
    private static final Object[][] TOWNS = {
            {"Banda Aceh", 5.55237, 95.31703},
            {"Medan", 3.58399, 98.68474},
            {"Padang", -0.95565, 100.36246},
            {"Pekanbaru", 0.514, 101.44499},
            {"Jambi", -1.61557, 103.57677},
            {"Palembang", -3.00802, 104.7436},
            {"Bengkulu", -3.84067, 102.32073},
            {"Bandar Lampung", -5.38335, 105.26349},
            {"Pangkal Pinang", -2.13281, 106.12902},
            {"Batam", 1.14145, 104.01986},
            {"Tanjung Pinang", 0.91587, 104.44221},
            {"Jakarta", -6.17638, 106.82671},
            {"Bogor", -6.62161, 106.80873},
            {"Sukabumi", -6.91431, 106.91672},
            {"Bandung", -6.87423, 107.5854},
            {"Cirebon", -6.71251, 108.5582},
            {"Bekasi", -6.23336, 107.01356},
            {"Depok", -6.39895, 106.82347},
            {"Magelang", -7.49209, 110.22448},
            {"Surakarta", -7.56714, 110.78626},
            {"Semarang", -6.98371, 110.41869},
            {"Yogyakarta", -7.7736, 110.35719},
            {"Kediri", -7.81866, 112.0034},
            {"Malang", -7.92252, 112.65017},
            {"Surabaya", -7.34058, 112.68994},
            {"Tangerang", -6.17288, 106.63185},
            {"Cilegon", -6.03311, 106.06966},
            {"Serang", -6.12797, 106.14685},
            {"Tangerang Selatan", -6.32231, 106.69429},
            {"Denpasar", -8.73626, 115.23211},
            {"Mataram", -8.58085, 116.07551},
            {"Kupang", -10.17566, 123.55335},
            {"Pontianak", -0.03565, 109.34885},
            {"Singkawang", 0.90897, 109.01413},
            {"Palangka Raya", -2.19975, 113.95033},
            {"Banjarmasin", -3.35646, 114.54687},
            {"Banjarbaru", -3.47016, 114.74147},
            {"Balikpapan", -1.21442, 116.93774},
            {"Samarinda", -0.57219, 117.1775},
            {"Bontang", 0.13358, 117.50849},
            {"Tarakan", 3.32551, 117.58456},
            {"Tanjung Selor", 2.82501, 117.36969},
            {"Manado", 1.54438, 124.84174},
            {"Bitung", 1.40216, 125.18515},
            {"Tomohon", 1.29108, 124.7909},
            {"Palu", -0.88924, 119.86583},
            {"Makassar", -5.16218, 119.40425},
            {"Palopo", -3.00891, 120.19936},
            {"Kendari", -3.9673, 122.51365},
            {"Baubau", -5.48177, 122.57375},
            {"Gorontalo", 0.54691, 123.00509},
            {"Mamuju", -2.74919, 118.96735},
            {"Ambon", -3.7793, 128.11045},
            {"Tual", -5.59097, 132.76836},
            {"Ternate", 0.76141, 127.31969},
            {"Jayapura", -2.52644, 140.65689},
            {"Manokwari", -0.8535, 134.06195},
            {"Sorong", -0.87645, 131.28333},
    };
    /** Farther than this (about 110 km) from every city: not in Indonesia, no guess. */
    private static final double MAX_DEGREES = 1.0;
    private static final long WAIT_MS = 30_000, FRESH_MS = 7L * 24 * 60 * 60_000;

    private ZoneDetect() {}

    /** The city nearest to this point, or null when it's not near any (outside Indonesia). */
    static String nearest(double lat, double lon) {
        String best = null;
        double bestD = MAX_DEGREES * MAX_DEGREES, k = Math.cos(Math.toRadians(lat));
        for (Object[] z : TOWNS) {
            for (int i = 1; i + 1 < z.length; i += 2) {
                double dLat = lat - (Double) z[i], dLon = (lon - (Double) z[i + 1]) * k;
                double d = dLat * dLat + dLon * dLon;
                if (d < bestD) {
                    bestD = d;
                    best = (String) z[0];
                }
            }
        }
        return best;
    }

    /**
     * Finds where the TV is and hands {latitude, longitude} to done (main thread), or null when it
     * can't: no location permission or provider, location off, or no answer within 30 s.
     */
    @SuppressWarnings({"MissingPermission", "deprecation"}) // checked first; requestSingleUpdate: getCurrentLocation is 30+
    static void locate(Context ctx, java.util.function.Consumer<double[]> done) {
        Handler main = new Handler(Looper.getMainLooper());
        LocationManager lm = ctx.getSystemService(LocationManager.class);
        boolean allowed = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (lm == null || !allowed || !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            main.post(() -> done.accept(null));
            return;
        }
        try {
            Location last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last != null && System.currentTimeMillis() - last.getTime() < FRESH_MS) {
                double[] here = {last.getLatitude(), last.getLongitude()};
                main.post(() -> done.accept(here));
                return;
            }
            boolean[] answered = {false};
            LocationListener once = new LocationListener() {
                @Override public void onLocationChanged(Location l) {
                    if (answered[0]) return;
                    answered[0] = true;
                    lm.removeUpdates(this);
                    done.accept(new double[] {l.getLatitude(), l.getLongitude()});
                }

                @Override public void onStatusChanged(String p, int s, Bundle b) {}

                @Override public void onProviderEnabled(String p) {}

                @Override public void onProviderDisabled(String p) {}
            };
            lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, once, Looper.getMainLooper());
            main.postDelayed(() -> {
                if (answered[0]) return;
                answered[0] = true;
                lm.removeUpdates(once);
                done.accept(null);
            }, WAIT_MS);
        } catch (RuntimeException e) { // SecurityException, a provider gone
            main.post(() -> done.accept(null));
        }
    }
}
