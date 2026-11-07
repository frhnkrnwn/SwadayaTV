package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model + parser data bola SwadayaTV. Murni Java (tanpa API Android) supaya mudah diuji.
 * Mendukung dua sumber: ESPN (utama, tanpa API key) dan FotMob (cadangan).
 * Semua parser sengaja "tahan banting": field yang hilang tidak membuat seluruh data gagal.
 */
final class SoccerParse {
    private SoccerParse() {}

    static final int ST_SCHEDULED = 0;
    static final int ST_LIVE = 1;
    static final int ST_FINISHED = 2;
    static final int ST_OFF = 3; // ditunda / dibatalkan / dihentikan

    // ---- model ----

    static final class Row {
        String name = "";
        String group = "";
        int rank = 0;
        int played = 0;
        int won = 0;
        int drawn = 0;
        int lost = 0;
        int goalDiff = 0;
        int points = 0;
        int qualColor = 0; // ARGB, 0 = tanpa warna
        int teamId = 0;    // id klub football-data.org (untuk logo), 0 = tidak ada
    }

    static final class Match {
        String id = "";
        String home = "";
        String away = "";
        long utcTime = 0L;
        int state = ST_SCHEDULED;
        String homeScore = "";
        String awayScore = "";
        String detail = ""; // menit berjalan, "FT", "Ditunda", dst.
        int homeId = 0;     // id klub (untuk logo)
        int awayId = 0;

        boolean hasScore() { return !homeScore.isEmpty() && !awayScore.isEmpty(); }
    }

    static final class Data {
        List<Row> table = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        String season = "";
        String source = "";
        long fetchedAt = 0L;

        boolean isEmpty() { return table.isEmpty() && matches.isEmpty(); }

        boolean hasLive() {
            for (Match m : matches) if (m.state == ST_LIVE) return true;
            return false;
        }
    }

    // ---- waktu ----

    private static final Pattern ISO = Pattern.compile(
            "^(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d+)?\\s*(Z|[+-]\\d{2}:?\\d{2})?$");

    /** Parse waktu ISO-8601 dengan atau tanpa detik/milidetik. 0 bila gagal. */
    static long parseIso(String s) {
        if (s == null) return 0L;
        Matcher m = ISO.matcher(s.trim());
        if (!m.matches()) return 0L;
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
            c.clear();
            c.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1,
                    Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(5)), m.group(6) != null ? Integer.parseInt(m.group(6)) : 0);
            long t = c.getTimeInMillis();
            String zona = m.group(7);
            if (zona != null && !zona.equals("Z")) {
                int tanda = zona.charAt(0) == '-' ? -1 : 1;
                String d = zona.substring(1).replace(":", "");
                int offsetMenit = Integer.parseInt(d.substring(0, 2)) * 60 + Integer.parseInt(d.substring(2, 4));
                t -= (long) tanda * offsetMenit * 60_000L;
            }
            return t;
        } catch (Exception e) {
            return 0L;
        }
    }

    // ---- pembantu JSON ----

    /** Teks field JSON; "" bila hilang atau null (org.json Android mengembalikan "null" untuk nilai null). */
    private static String s(JSONObject o, String k) {
        return o == null || o.isNull(k) ? "" : o.optString(k, "");
    }

    private static String teks(Object o) {
        if (o == null || o == JSONObject.NULL) return "";
        if (o instanceof JSONObject) {
            JSONObject j = (JSONObject) o;
            String v = s(j, "displayValue");
            if (v.isEmpty() && j.has("value")) v = teks(j.opt("value"));
            return v;
        }
        if (o instanceof Double) {
            double d = (Double) o;
            if (d == Math.rint(d)) return String.valueOf((long) d);
        }
        return String.valueOf(o).trim();
    }

    private static int angka(Object o, int def) {
        String s = teks(o);
        if (s.isEmpty()) return def;
        try {
            if (s.startsWith("+")) s = s.substring(1);
            return (int) Math.round(Double.parseDouble(s));
        } catch (Exception e) {
            return def;
        }
    }

    /** "#RRGGBB" / "#AARRGGBB" -> ARGB. 0 bila tidak valid. */
    static int warna(String hex) {
        if (hex == null) return 0;
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        try {
            if (h.length() == 6) return 0xFF000000 | (int) Long.parseLong(h, 16);
            if (h.length() == 8) return (int) Long.parseLong(h, 16);
        } catch (Exception ignore) { /* abaikan */ }
        return 0;
    }

    // ---- ESPN: klasemen ----

    private static void kumpulkanGrup(JSONObject node, List<JSONObject> keluar, int kedalaman) {
        if (node == null || kedalaman > 4) return;
        JSONObject st = node.optJSONObject("standings");
        if (st != null && st.optJSONArray("entries") != null) keluar.add(node);
        JSONArray anak = node.optJSONArray("children");
        if (anak != null) {
            for (int i = 0; i < anak.length(); i++) kumpulkanGrup(anak.optJSONObject(i), keluar, kedalaman + 1);
        }
    }

    private static double stat(JSONArray stats, double def, String... nama) {
        if (stats == null) return def;
        for (String n : nama) {
            for (int i = 0; i < stats.length(); i++) {
                JSONObject s = stats.optJSONObject(i);
                if (s == null) continue;
                if (n.equalsIgnoreCase(s.optString("name")) || n.equalsIgnoreCase(s.optString("abbreviation"))
                        || n.equalsIgnoreCase(s.optString("type"))) {
                    if (s.has("value") && !s.isNull("value")) {
                        double v = s.optDouble("value", Double.NaN);
                        if (!Double.isNaN(v)) return v;
                    }
                    String dv = s(s, "displayValue");
                    if (!dv.isEmpty()) {
                        try { return Double.parseDouble(dv.startsWith("+") ? dv.substring(1) : dv); }
                        catch (Exception ignore) { /* coba nama lain */ }
                    }
                }
            }
        }
        return def;
    }

    /** Isi d.table (dan d.season) dari JSON klasemen ESPN. Melempar exception bila JSON rusak. */
    static void parseEspnStandings(String json, Data d) throws Exception {
        JSONObject root = new JSONObject(json);
        List<JSONObject> grup = new ArrayList<>();
        kumpulkanGrup(root, grup, 0);
        List<Row> hasil = new ArrayList<>();
        for (JSONObject g : grup) {
            JSONObject st = g.optJSONObject("standings");
            JSONArray entries = st.optJSONArray("entries");
            if (d.season.isEmpty()) {
                String musim = s(st, "seasonDisplayName");
                if (musim.isEmpty()) musim = s(st, "season");
                d.season = musim;
            }
            List<Row> bagian = new ArrayList<>();
            for (int i = 0; i < entries.length(); i++) {
                JSONObject e = entries.optJSONObject(i);
                if (e == null) continue;
                JSONObject tim = e.optJSONObject("team");
                String nama = "";
                if (tim != null) {
                    nama = s(tim, "displayName");
                    if (nama.isEmpty()) nama = s(tim, "name");
                    if (nama.isEmpty()) nama = s(tim, "shortDisplayName");
                }
                if (nama.isEmpty()) continue;
                JSONArray stats = e.optJSONArray("stats");
                Row r = new Row();
                r.name = nama;
                r.group = grup.size() > 1 ? s(g, "name") : "";
                r.rank = (int) stat(stats, 0, "rank");
                r.played = (int) stat(stats, 0, "gamesPlayed", "GP");
                r.won = (int) stat(stats, 0, "wins", "W");
                r.drawn = (int) stat(stats, 0, "ties", "draws", "D");
                r.lost = (int) stat(stats, 0, "losses", "L");
                r.points = (int) stat(stats, 0, "points", "PTS", "P");
                double sg = stat(stats, Double.NaN, "pointDifferential", "goalDifference", "GD");
                if (Double.isNaN(sg)) {
                    sg = stat(stats, 0, "pointsFor", "goalsFor", "F") - stat(stats, 0, "pointsAgainst", "goalsAgainst", "A");
                }
                r.goalDiff = (int) Math.round(sg);
                JSONObject catatan = e.optJSONObject("note");
                if (catatan != null) r.qualColor = warna(s(catatan, "color"));
                bagian.add(r);
            }
            boolean adaRank = false;
            for (Row r : bagian) if (r.rank > 0) { adaRank = true; break; }
            if (adaRank) {
                Collections.sort(bagian, (a, b) -> Integer.compare(a.rank == 0 ? 999 : a.rank, b.rank == 0 ? 999 : b.rank));
            } else {
                for (int i = 0; i < bagian.size(); i++) bagian.get(i).rank = i + 1;
            }
            hasil.addAll(bagian);
        }
        d.table = hasil;
    }

    // ---- ESPN: jadwal & skor ----

    private static String namaTim(JSONObject tim) {
        if (tim == null) return "";
        String n = s(tim, "displayName");
        if (n.isEmpty()) n = s(tim, "shortDisplayName");
        if (n.isEmpty()) n = s(tim, "name");
        return n;
    }

    /** Tambahkan laga dari JSON scoreboard ESPN ke d.matches (tanpa duplikat id). */
    static void parseEspnScoreboard(String json, Data d) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray events = root.optJSONArray("events");
        if (events == null) return;
        Set<String> sudah = new HashSet<>();
        for (Match m : d.matches) if (!m.id.isEmpty()) sudah.add(m.id);
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e == null) continue;
            JSONArray comps = e.optJSONArray("competitions");
            JSONObject comp = comps != null ? comps.optJSONObject(0) : null;
            if (comp == null) continue;
            Match m = new Match();
            m.id = s(e, "id");
            if (!m.id.isEmpty() && !sudah.add(m.id)) continue;
            m.utcTime = parseIso(s(e, "date"));
            if (m.utcTime == 0L) m.utcTime = parseIso(s(comp, "date"));
            JSONArray lawan = comp.optJSONArray("competitors");
            if (lawan == null) continue;
            for (int k = 0; k < lawan.length(); k++) {
                JSONObject c = lawan.optJSONObject(k);
                if (c == null) continue;
                String sisi = s(c, "homeAway");
                boolean tuanRumah = sisi.equals("home") || (sisi.isEmpty() && k == 0);
                String nama = namaTim(c.optJSONObject("team"));
                String skor = teks(c.opt("score"));
                if (tuanRumah) { m.home = nama; m.homeScore = skor; }
                else { m.away = nama; m.awayScore = skor; }
            }
            if (m.home.isEmpty() || m.away.isEmpty()) continue;

            JSONObject status = e.optJSONObject("status");
            if (status == null) status = comp.optJSONObject("status");
            JSONObject tipe = status != null ? status.optJSONObject("type") : null;
            String state = tipe != null ? s(tipe, "state") : "";
            String nm = tipe != null ? s(tipe, "name").toUpperCase(Locale.US) : "";
            boolean selesai = tipe != null && tipe.optBoolean("completed", false);
            String pendek = tipe != null ? s(tipe, "shortDetail") : "";
            String jam = status != null ? s(status, "displayClock") : "";

            if (nm.contains("POSTPONED") || nm.contains("CANCEL") || nm.contains("SUSPENDED")
                    || nm.contains("ABANDON") || nm.contains("FORFEIT")) {
                m.state = ST_OFF;
                String desk = s(tipe, "description");
                m.detail = desk.isEmpty() ? "Ditunda" : desk;
            } else if ("in".equals(state)) {
                m.state = ST_LIVE;
                if (nm.contains("HALFTIME")) m.detail = "HT";
                else if (!jam.isEmpty() && !jam.equals("0'")) m.detail = jam;
                else m.detail = pendek.isEmpty() ? "LIVE" : pendek;
            } else if ("post".equals(state) || selesai) {
                m.state = ST_FINISHED;
                m.detail = pendek.isEmpty() ? "FT" : pendek;
            } else {
                m.state = ST_SCHEDULED;
            }
            if (m.state == ST_SCHEDULED || m.state == ST_OFF) {
                // laga yang belum jalan tidak punya skor yang berarti
                m.homeScore = "";
                m.awayScore = "";
            }
            d.matches.add(m);
        }
    }

    // ---- FotMob (cadangan) ----

    private static final Pattern SKOR = Pattern.compile("(\\d+)\\s*[-–:]\\s*(\\d+)");

    private static void parseFotmobBaris(JSONArray arr, String grup, List<Row> keluar) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Row r = new Row();
            r.group = grup;
            r.rank = o.optInt("idx", i + 1);
            r.name = s(o, "name");
            if (r.name.isEmpty()) continue;
            r.played = o.optInt("played", 0);
            r.won = o.optInt("wins", 0);
            r.drawn = o.optInt("draws", 0);
            r.lost = o.optInt("losses", 0);
            r.goalDiff = o.optInt("goalConDiff", o.optInt("goalDiff", 0));
            r.points = o.optInt("pts", o.optInt("points", 0));
            Object w = o.opt("qualColor");
            r.qualColor = w instanceof String ? warna((String) w) : 0;
            keluar.add(r);
        }
    }

    private static JSONArray fotmobSemua(Object tabel) {
        if (tabel instanceof JSONArray) return (JSONArray) tabel;
        if (tabel instanceof JSONObject) {
            JSONObject t = (JSONObject) tabel;
            JSONArray a = t.optJSONArray("all");
            if (a == null) a = t.optJSONArray("table");
            return a;
        }
        return null;
    }

    static Data parseFotmob(String json) throws Exception {
        Data d = new Data();
        JSONObject root = new JSONObject(json);
        JSONObject details = root.optJSONObject("details");
        if (details != null) d.season = s(details, "selectedSeason");

        JSONArray tableArr = root.optJSONArray("table");
        if (tableArr != null && tableArr.length() > 0) {
            JSONObject data = tableArr.optJSONObject(0) != null ? tableArr.optJSONObject(0).optJSONObject("data") : null;
            if (data != null) {
                JSONArray banyak = data.optJSONArray("tables");
                if (banyak != null && banyak.length() > 0) {
                    for (int i = 0; i < banyak.length(); i++) {
                        JSONObject t = banyak.optJSONObject(i);
                        if (t == null) continue;
                        String nama = s(t, "leagueName");
                        parseFotmobBaris(fotmobSemua(t.opt("table")), banyak.length() > 1 ? nama : "", d.table);
                    }
                } else {
                    parseFotmobBaris(fotmobSemua(data.opt("table")), "", d.table);
                }
            }
        }

        JSONObject fixtures = root.optJSONObject("fixtures");
        JSONArray all = fixtures != null ? fixtures.optJSONArray("allMatches") : null;
        if (all != null) {
            for (int i = 0; i < all.length(); i++) {
                JSONObject o = all.optJSONObject(i);
                if (o == null) continue;
                Match m = new Match();
                m.id = s(o, "id");
                JSONObject home = o.optJSONObject("home");
                JSONObject away = o.optJSONObject("away");
                m.home = home != null ? s(home, "name") : s(o, "home");
                m.away = away != null ? s(away, "name") : s(o, "away");
                if (m.home.isEmpty() || m.away.isEmpty()) continue;
                JSONObject st = o.optJSONObject("status");
                if (st != null) {
                    m.utcTime = parseIso(s(st, "utcTime"));
                    boolean selesai = st.optBoolean("finished", false);
                    boolean mulai = st.optBoolean("started", false);
                    boolean batal = st.optBoolean("cancelled", false);
                    String skorStr = s(st, "scoreStr");
                    Matcher sm = SKOR.matcher(skorStr);
                    if (sm.find()) { m.homeScore = sm.group(1); m.awayScore = sm.group(2); }
                    if (batal) { m.state = ST_OFF; m.detail = "Ditunda"; }
                    else if (selesai) { m.state = ST_FINISHED; m.detail = "FT"; }
                    else if (mulai) {
                        m.state = ST_LIVE;
                        JSONObject lt = st.optJSONObject("liveTime");
                        m.detail = s(lt, "short");
                        if (m.detail.isEmpty()) m.detail = "LIVE";
                    }
                } else {
                    m.utcTime = parseIso(s(o, "utcTime"));
                }
                if (m.state == ST_SCHEDULED || m.state == ST_OFF) { m.homeScore = ""; m.awayScore = ""; }
                d.matches.add(m);
            }
        }
        return d;
    }


    // ---- pemilihan sumber ----

    /** Ada laga dalam rentang +-45 hari dari sekarang? Dipakai untuk mendeteksi data musim lalu yang basi. */
    static boolean adaMatchTerkini(Data d, long now) {
        if (d == null) return false;
        final long jendela = 45L * 24L * 3600L * 1000L;
        for (Match m : d.matches) {
            if (m.utcTime > 0 && Math.abs(m.utcTime - now) <= jendela) return true;
        }
        return false;
    }

    /**
     * Gabungkan data ESPN dan FotMob menjadi satu. Sumber yang punya laga terkini dipakai sebagai utama
     * (Liga 1 Indonesia di ESPN sering tertinggal di musim lalu); bila keduanya setara, ikuti
     * fotmobDulu. Bagian yang kosong di sumber utama diisi dari sumber lainnya.
     * Mengembalikan null bila keduanya kosong.
     */
    static Data pilih(Data espn, Data fm, boolean fotmobDulu, long now) {
        boolean e = espn != null && !espn.isEmpty();
        boolean f = fm != null && !fm.isEmpty();
        if (!e && !f) return null;
        if (e && !f) { espn.source = "ESPN"; return espn; }
        if (!e) { fm.source = "FotMob"; return fm; }

        boolean eSegar = adaMatchTerkini(espn, now);
        boolean fSegar = adaMatchTerkini(fm, now);
        boolean pakaiFm = (eSegar != fSegar) ? fSegar : fotmobDulu;
        Data utama = pakaiFm ? fm : espn;
        Data cadang = pakaiFm ? espn : fm;
        String namaU = pakaiFm ? "FotMob" : "ESPN";
        String namaC = pakaiFm ? "ESPN" : "FotMob";
        boolean isiDariCadang = false;
        if (utama.table.isEmpty() && !cadang.table.isEmpty()) { utama.table = cadang.table; isiDariCadang = true; }
        if (utama.matches.isEmpty() && !cadang.matches.isEmpty()) { utama.matches = cadang.matches; isiDariCadang = true; }
        if (utama.season.isEmpty()) utama.season = cadang.season;
        utama.source = isiDariCadang ? namaU + "+" + namaC : namaU;
        return utama;
    }

    // ---- serialisasi (cache) ----

    static String toJson(Data d) {
        try {
            JSONObject o = new JSONObject();
            o.put("v", 2);
            o.put("season", d.season);
            o.put("source", d.source);
            o.put("fetchedAt", d.fetchedAt);
            JSONArray t = new JSONArray();
            for (Row r : d.table) {
                JSONObject x = new JSONObject();
                x.put("n", r.name); x.put("g", r.group); x.put("r", r.rank); x.put("p", r.played);
                x.put("w", r.won); x.put("d", r.drawn); x.put("l", r.lost); x.put("gd", r.goalDiff);
                x.put("pt", r.points); x.put("q", r.qualColor); x.put("tid", r.teamId);
                t.put(x);
            }
            o.put("table", t);
            JSONArray ms = new JSONArray();
            for (Match m : d.matches) {
                JSONObject x = new JSONObject();
                x.put("id", m.id); x.put("h", m.home); x.put("a", m.away); x.put("t", m.utcTime);
                x.put("s", m.state); x.put("hs", m.homeScore); x.put("as", m.awayScore); x.put("dt", m.detail);
                x.put("hid", m.homeId); x.put("aid", m.awayId);
                ms.put(x);
            }
            o.put("matches", ms);
            return o.toString();
        } catch (Exception e) {
            return null;
        }
    }

    static Data fromJson(String json) {
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            if (o.optInt("v", 0) != 2) return null;
            Data d = new Data();
            d.season = s(o, "season");
            d.source = s(o, "source");
            d.fetchedAt = o.optLong("fetchedAt", 0L);
            JSONArray t = o.optJSONArray("table");
            if (t != null) for (int i = 0; i < t.length(); i++) {
                JSONObject x = t.optJSONObject(i);
                if (x == null) continue;
                Row r = new Row();
                r.name = s(x, "n"); r.group = s(x, "g"); r.rank = x.optInt("r", 0);
                r.played = x.optInt("p", 0); r.won = x.optInt("w", 0); r.drawn = x.optInt("d", 0);
                r.lost = x.optInt("l", 0); r.goalDiff = x.optInt("gd", 0); r.points = x.optInt("pt", 0);
                r.qualColor = x.optInt("q", 0); r.teamId = x.optInt("tid", 0);
                d.table.add(r);
            }
            JSONArray ms = o.optJSONArray("matches");
            if (ms != null) for (int i = 0; i < ms.length(); i++) {
                JSONObject x = ms.optJSONObject(i);
                if (x == null) continue;
                Match m = new Match();
                m.id = s(x, "id"); m.home = s(x, "h"); m.away = s(x, "a");
                m.utcTime = x.optLong("t", 0L); m.state = x.optInt("s", ST_SCHEDULED);
                m.homeScore = s(x, "hs"); m.awayScore = s(x, "as");
                m.detail = s(x, "dt");
                m.homeId = x.optInt("hid", 0); m.awayId = x.optInt("aid", 0);
                d.matches.add(m);
            }
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- daftar turunan untuk tampilan ----

    /** Hasil pertandingan: terbaru di atas. */
    static List<Match> hasil(Data d) {
        List<Match> out = new ArrayList<>();
        for (Match m : d.matches) if (m.state == ST_FINISHED) out.add(m);
        Collections.sort(out, (a, b) -> Long.compare(b.utcTime, a.utcTime));
        return out;
    }

    /** Sedang main dulu, lalu yang akan datang (terdekat di atas), lalu yang ditunda. */
    static List<Match> mendatang(Data d) {
        List<Match> out = new ArrayList<>();
        for (Match m : d.matches) if (m.state != ST_FINISHED) out.add(m);
        Collections.sort(out, new Comparator<Match>() {
            @Override public int compare(Match a, Match b) {
                int ra = peringkat(a), rb = peringkat(b);
                if (ra != rb) return Integer.compare(ra, rb);
                return Long.compare(a.utcTime, b.utcTime);
            }
            private int peringkat(Match m) { return m.state == ST_LIVE ? 0 : (m.state == ST_SCHEDULED ? 1 : 2); }
        });
        return out;
    }

    private static String normal(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Nama tim cocok (abaikan huruf besar/spasi/tanda baca; boleh singkatan yang terkandung). */
    static boolean samaTim(String a, String b) {
        String x = normal(a), y = normal(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        if (x.length() >= 5 && y.contains(x)) return true;
        return y.length() >= 5 && x.contains(y);
    }

    /** Semua laga tim favorit: yang sedang/akan main dulu, lalu hasil terbaru. */
    static List<Match> laga(Data d, String fav) {
        List<Match> out = new ArrayList<>();
        if (fav == null || fav.isEmpty()) return out;
        for (Match m : d.matches) if (samaTim(fav, m.home) || samaTim(fav, m.away)) out.add(m);
        Collections.sort(out, (a, b) -> {
            boolean aSel = a.state == ST_FINISHED, bSel = b.state == ST_FINISHED;
            if (aSel != bSel) return aSel ? 1 : -1;
            return aSel ? Long.compare(b.utcTime, a.utcTime) : Long.compare(a.utcTime, b.utcTime);
        });
        return out;
    }
}
