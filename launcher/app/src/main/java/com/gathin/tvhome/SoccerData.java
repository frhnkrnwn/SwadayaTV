package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Data bola SwadayaTV: klasemen, hasil, dan jadwal (termasuk skor langsung).
 * Sumber: football-data.org, diambil berkala oleh GitHub Action lalu disimpan sebagai JSON di repo
 * (kunci API tidak ada di aplikasi). Hasil terakhir yang berhasil disimpan sebagai cache supaya tetap tampil saat luring.
 */
final class SoccerData {
    private static final String TAG = "SoccerData";
    private static final String PREFS = "soccer_cache";
    /** Snapshot publik (tanpa kunci API) yang diperbarui otomatis oleh .github/workflows/sync-football.yml */
    private static final String DATA_BASE =
            "https://raw.githubusercontent.com/frhnkrnwn/SwadayaTV/main/data/football/";
    private static final String UA = "Mozilla/5.0 (Linux; Android TV) SwadayaTV/3.7";

    private SoccerData() {}

    static final class League {
        final int id;          // id liga (kunci pilihan tersimpan; nama berkas data = code)
        final String name;
        final String code;

        League(int id, String name, String code) {
            this.id = id;
            this.name = name;
            this.code = code;
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
        new League(47, "Premier League", "EPL"),
        new League(87, "LaLiga", "LL"),
        new League(55, "Serie A", "SA"),
        new League(54, "Bundesliga", "BL"),
        new League(53, "Ligue 1", "L1"),
        new League(8983, "Liga 1 Indonesia", "IDN"),
        new League(42, "Champions League", "UCL")
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

    /** Ambil data (blok; panggil dari thread latar). Dibaca dari snapshot yang dibuat GitHub Action. */
    static Result fetch(Context ctx, League l) {
        Result r = new Result();
        StringBuilder err = new StringBuilder();
        String json = get(DATA_BASE + l.code + ".json", err, l.name);
        SoccerParse.Data d = SoccerParse.fromJson(json);
        if (d == null || d.isEmpty()) {
            boolean tidakAda = err.indexOf("HTTP 404") >= 0;
            r.error = tidakAda ? "Data " + l.name + " belum tersedia dari server."
                    : (err.length() > 0 ? err.toString() : "data kosong");
            return r;
        }
        if (ctx != null) {
            String tj = get(DATA_BASE + "teams.json", new StringBuilder(), "teams");
            if (tj != null) LogoCache.setTeamsJson(ctx, tj);
        }
        r.data = d;
        return r;
    }

    /** Ambil di thread latar, hasil dikirim ke UI thread Activity. */
    static void fetchAsync(final Activity act, final League l, final Callback cb) {
        new Thread(() -> {
            final Result r = fetch(act.getApplicationContext(), l);
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
