package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Data bola SwadayaTV: klasemen, hasil, dan jadwal (termasuk skor langsung).
 * Sumber utama ESPN (tanpa API key), cadangan FotMob. Tiap sumber dicoba otomatis,
 * dan hasil terakhir yang berhasil disimpan sebagai cache supaya tetap tampil saat luring.
 */
final class SoccerData {
    private static final String TAG = "SoccerData";
    private static final String PREFS = "soccer_cache";
    private static final String ESPN_BASE = "https://site.api.espn.com/apis";
    private static final String FOTMOB_API = "https://www.fotmob.com/api/data/leagues?id=";
    private static final String UA = "Mozilla/5.0 (Linux; Android TV) SwadayaTV/3.7";

    /** Jendela jadwal yang diambil dari ESPN: hari ke belakang dan ke depan. */
    private static final int HARI_LALU = 21;
    private static final int HARI_DEPAN = 21;

    private SoccerData() {}

    static final class League {
        final int id;          // id FotMob (juga dipakai sebagai kunci pilihan tersimpan)
        final String name;
        final String code;
        final String espn;     // slug liga di ESPN, mis. "eng.1"
        final boolean fotmobDulu; // true: FotMob lebih lengkap/terbaru dari ESPN untuk liga ini

        League(int id, String name, String code, String espn, boolean fotmobDulu) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.espn = espn;
            this.fotmobDulu = fotmobDulu;
        }
    }

    static final class Result {
        SoccerParse.Data data;   // null bila semua sumber gagal
        String error = "";       // ringkasan galat per sumber, untuk ditampilkan
    }

    interface Callback {
        void onDone(League league, Result result);
    }

    static final League[] LEAGUES = {
        new League(47, "Premier League", "EPL", "eng.1", false),
        new League(87, "LaLiga", "LL", "esp.1", false),
        new League(55, "Serie A", "SA", "ita.1", false),
        new League(54, "Bundesliga", "BL", "ger.1", false),
        new League(53, "Ligue 1", "L1", "fra.1", false),
        new League(8983, "Liga 1 Indonesia", "IDN", "idn.1", true),
        new League(42, "Champions League", "UCL", "uefa.champions", false)
    };

    static League[] getLeagues() { return LEAGUES; }

    // ---- cache ----

    private static String kunci(League l) { return "v2_league_" + l.id; }

    static SoccerParse.Data loadCached(Context ctx, League l) {
        if (ctx == null) return null;
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(kunci(l), null);
        return SoccerParse.fromJson(json);
    }

    static void saveCache(Context ctx, League l, SoccerParse.Data d) {
        if (ctx == null || d == null) return;
        String json = SoccerParse.toJson(d);
        if (json == null) return;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(kunci(l), json).apply();
    }

    // ---- jaringan ----

    /** GET sederhana. Mengembalikan isi, atau null dan mengisi err bila gagal. */
    private static String get(String url, StringBuilder err, String label) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "application/json");
            int kode = conn.getResponseCode();
            if (kode != 200) {
                tambahGalat(err, label + " HTTP " + kode);
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = br.read(buf)) > 0) {
                sb.append(buf, 0, n);
                if (sb.length() > 8_000_000) break; // pengaman memori
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, label + " gagal: " + e);
            tambahGalat(err, label + " " + e.getClass().getSimpleName());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void tambahGalat(StringBuilder err, String s) {
        if (err.length() > 0) err.append("; ");
        err.append(s);
    }

    private static String tanggalUtc(int offsetHari) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        c.add(Calendar.DAY_OF_YEAR, offsetHari);
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(c.getTime());
    }

    private static void ambilEspn(League l, SoccerParse.Data d, StringBuilder err) {
        String dasar = ESPN_BASE + "/v2/sports/soccer/" + l.espn + "/standings";
        String klasemen = get(dasar, err, "ESPN klasemen");
        if (klasemen != null) {
            try {
                SoccerParse.parseEspnStandings(klasemen, d);
            } catch (Exception e) {
                tambahGalat(err, "ESPN klasemen format");
                Log.w(TAG, "parse klasemen ESPN: " + e);
            }
        }
        String papan = ESPN_BASE + "/site/v2/sports/soccer/" + l.espn + "/scoreboard";
        String jadwal = get(papan + "?limit=300&dates=" + tanggalUtc(-HARI_LALU) + "-" + tanggalUtc(HARI_DEPAN),
                err, "ESPN jadwal");
        if (jadwal != null) {
            try {
                SoccerParse.parseEspnScoreboard(jadwal, d);
            } catch (Exception e) {
                tambahGalat(err, "ESPN jadwal format");
                Log.w(TAG, "parse jadwal ESPN: " + e);
            }
        }
        if (d.matches.isEmpty()) {
            // rentang tanggal tidak didukung / kosong: minta pertandingan terkini saja
            String hariIni = get(papan + "?limit=100", err, "ESPN jadwal-hari-ini");
            if (hariIni != null) {
                try {
                    SoccerParse.parseEspnScoreboard(hariIni, d);
                } catch (Exception e) {
                    Log.w(TAG, "parse jadwal hari ini ESPN: " + e);
                }
            }
        }
    }

    private static SoccerParse.Data ambilFotmob(League l, StringBuilder err) {
        String json = get(FOTMOB_API + l.id, err, "FotMob");
        if (json == null) return null;
        try {
            return SoccerParse.parseFotmob(json);
        } catch (Exception e) {
            tambahGalat(err, "FotMob format");
            Log.w(TAG, "parse FotMob: " + e);
            return null;
        }
    }

    /** Ambil data (blok; panggil dari thread latar). */
    static Result fetch(League l) {
        Result r = new Result();
        StringBuilder err = new StringBuilder();
        long now = System.currentTimeMillis();

        SoccerParse.Data espn = new SoccerParse.Data();
        ambilEspn(l, espn, err);

        // FotMob dipanggil bila ESPN belum lengkap, kelihatan basi (musim lalu), atau liga ini
        // memang lebih baik dari FotMob (Liga 1 Indonesia).
        boolean perluFotmob = l.fotmobDulu || espn.table.isEmpty() || espn.matches.isEmpty()
                || !SoccerParse.adaMatchTerkini(espn, now);
        SoccerParse.Data fm = perluFotmob ? ambilFotmob(l, err) : null;

        SoccerParse.Data d = SoccerParse.pilih(espn, fm, l.fotmobDulu, now);
        if (d == null) {
            r.error = err.length() > 0 ? err.toString() : "data kosong";
            return r;
        }
        d.fetchedAt = now;
        r.data = d;
        r.error = err.toString();
        return r;
    }

    /** Ambil di thread latar, hasil dikirim ke UI thread Activity. */
    static void fetchAsync(final Activity act, final League l, final Callback cb) {
        new Thread(() -> {
            final Result r = fetch(l);
            if (act == null || act.isFinishing()) return;
            act.runOnUiThread(() -> {
                if (!act.isFinishing()) cb.onDone(l, r);
            });
        }, "soccer-fetch").start();
    }

    // ---- format tampilan ----

    private static final Locale ID = new Locale("id", "ID");

    static String jam(long utcMs) {
        if (utcMs <= 0) return "--:--";
        return new SimpleDateFormat("HH:mm", ID).format(new Date(utcMs));
    }

    static String tanggalSingkat(long utcMs) {
        if (utcMs <= 0) return "Tanggal belum pasti";
        return new SimpleDateFormat("EEEE, d MMMM", ID).format(new Date(utcMs));
    }

    /** Kunci hari lokal untuk mengelompokkan laga per tanggal. */
    static String kunciHari(long utcMs) {
        if (utcMs <= 0) return "?";
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(utcMs));
    }

    static String jamPerbarui(long ms) {
        if (ms <= 0) return "-";
        return new SimpleDateFormat("HH:mm", ID).format(new Date(ms));
    }
}
