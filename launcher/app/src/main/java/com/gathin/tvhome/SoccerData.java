package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Data bola FotMob versi sederhana SwadayaTV.
 * Tanpa API key. Cache JSON mentah di SharedPreferences "soccer_cache".
 */
final class SoccerData {
    private static final String TAG = "SoccerData";
    private static final String API = "https://www.fotmob.com/api/data/leagues?id=";
    private static final String PREFS = "soccer_cache";

    private SoccerData() {}

    // ---- model ----

    static final class League {
        final int id;
        final String name;
        final String code;

        League(int id, String name) { this(id, name, ""); }

        League(int id, String name, String code) {
            this.id = id;
            this.name = name;
            this.code = code;
        }
    }

    static final class Row {
        String name = "";
        int rank = 0;
        int played = 0;
        int won = 0;
        int drawn = 0;
        int lost = 0;
        int goalDiff = 0;
        int points = 0;
        String scoresStr = "";
        int qualColor = 0;
    }

    static final class Match {
        String home = "";
        String away = "";
        long utcTime = 0L;
        String scoreStr = "";
        boolean finished = false;
    }

    static final class Data {
        List<Row> table = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        String season = "";
    }

    /** Daftar liga utama, id dari FotMob. */
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

    static String loadCached(Context ctx, int leagueId) {
        if (ctx == null) return null;
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("league_" + leagueId, null);
    }

    static void saveCache(Context ctx, int leagueId, String json) {
        if (ctx == null || json == null) return;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("league_" + leagueId, json).apply();
    }

    // ---- jaringan ----

    static String download(League league) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(API + league.id).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "SwadayaTV/1.0");
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String baris;
            while ((baris = br.readLine()) != null) sb.append(baris);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "download liga " + league.id + " gagal: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Unduh di background lalu kirim balik ke UI thread lewat handler. */
    static void downloadAsync(Context ctx, final League league, final Callback cb) {
        new Thread(() -> {
            String json = download(league);
            Data data = json != null ? parse(json) : null;
            if (json != null) saveCache(ctx, league.id, json);
            final Data kirim = data;
            if (ctx instanceof android.app.Activity) {
                ((android.app.Activity) ctx).runOnUiThread(() -> cb.onDone(league, kirim, json));
            } else if (cb != null) {
                cb.onDone(league, kirim, json);
            }
        }).start();
    }

    // ---- parse ----

    static Data parse(String json) {
        Data d = new Data();
        if (json == null) return d;
        try {
            JSONObject root = new JSONObject(json);
            // musim aktif dari details.selectedSeason bila ada
            JSONObject details = root.optJSONObject("details");
            if (details != null) {
                d.season = details.optString("selectedSeason", "");
            }
            // klasemen
            JSONArray tableArr = root.optJSONArray("table");
            if (tableArr != null && tableArr.length() > 0) {
                JSONObject tabel0 = tableArr.optJSONObject(0);
                if (tabel0 != null) {
                    JSONObject data = tabel0.optJSONObject("data");
                    if (data != null) {
                        // FotMob mengirim "table" sebagai objek {"all":[...], "home":[...], ...}
                        // pada API terbaru, dan sebagai array langsung pada versi lama.
                        JSONArray all = null;
                        Object tabel = data.opt("table");
                        if (tabel instanceof JSONObject) {
                            all = ((JSONObject) tabel).optJSONArray("all");
                            if (all == null) all = ((JSONObject) tabel).optJSONArray("table");
                        } else if (tabel instanceof JSONArray) {
                            all = (JSONArray) tabel;
                        }
                        if (all != null) {
                            for (int i = 0; i < all.length(); i++) {
                                JSONObject o = all.optJSONObject(i);
                                if (o == null) continue;
                                Row r = new Row();
                                r.rank = o.optInt("idx", i + 1);
                                r.name = o.optString("name", "");
                                r.played = o.optInt("played", 0);
                                r.won = o.optInt("wins", 0);
                                r.drawn = o.optInt("draws", 0);
                                r.lost = o.optInt("losses", 0);
                                r.scoresStr = o.optString("scoresStr", "");
                                r.goalDiff = o.optInt("goalConDiff", o.optInt("goalDiff", 0));
                                r.points = o.optInt("pts", o.optInt("points", 0));
                                Object warna = o.opt("qualColor");
                                if (warna instanceof String) {
                                    try {
                                        r.qualColor = android.graphics.Color.parseColor((String) warna);
                                    } catch (Exception ignore) {
                                        r.qualColor = 0;
                                    }
                                } else {
                                    r.qualColor = o.optInt("qualColor", 0);
                                }
                                d.table.add(r);
                            }
                        }
                    }
                }
            }
            // jadwal
            JSONObject fixtures = root.optJSONObject("fixtures");
            if (fixtures != null) {
                JSONArray all = fixtures.optJSONArray("allMatches");
                if (all != null) {
                    for (int i = 0; i < all.length(); i++) {
                        JSONObject o = all.optJSONObject(i);
                        if (o == null) continue;
                        Match m = new Match();
                        JSONObject home = o.optJSONObject("home");
                        JSONObject away = o.optJSONObject("away");
                        m.home = home != null ? home.optString("name", "") : o.optString("home", "");
                        m.away = away != null ? away.optString("name", "") : o.optString("away", "");
                        JSONObject st = o.optJSONObject("status");
                        if (st != null) {
                            String utc = st.optString("utcTime", "");
                            // parse ISO8601 UTC
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                m.utcTime = sdf.parse(utc).getTime();
                            } catch (Exception ignore) { m.utcTime = 0L; }
                            m.finished = st.optBoolean("finished", false);
                            m.scoreStr = st.optString("scoreStr", "");
                        } else {
                            m.utcTime = o.optLong("utcTime", 0L);
                            m.scoreStr = o.optString("scoreStr", o.optString("score", ""));
                        }
                        d.matches.add(m);
                    }
                }
            }
            // Jadwal terdekat dulu: laga yang belum dimainkan (terdekat), lalu hasil terbaru.
            final long sekarang = System.currentTimeMillis();
            java.util.Collections.sort(d.matches, (a, b) -> {
                boolean aDepan = a.utcTime >= sekarang;
                boolean bDepan = b.utcTime >= sekarang;
                if (aDepan != bDepan) return aDepan ? -1 : 1;
                if (aDepan) return Long.compare(a.utcTime, b.utcTime);
                return Long.compare(b.utcTime, a.utcTime);
            });
        } catch (Exception e) {
            Log.w(TAG, "parse gagal: " + e.getMessage());
        }
        return d;
    }

    interface Callback {
        void onDone(League league, Data data, String rawJson);
    }

    /** Format waktu laga singkat, mis. "Sab 14/02 21:00". */
    static String formatWaktu(long utcMs) {
        if (utcMs <= 0) return "-";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE dd/MM HH:mm", Locale.getDefault());
            return sdf.format(new Date(utcMs));
        } catch (Exception e) {
            return "-";
        }
    }
}
