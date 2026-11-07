package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.widget.ImageView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Logo klub: dicari lewat ID klub (bukan nama, supaya klub bernama mirip tidak tertukar).
 * Daftar ID -> URL logo (teams.json) dibuat otomatis oleh GitHub Action, jadi tidak ada URL logo yang
 * ditulis manual di kode. Urutan: logo PNG dari TheSportsDB, lalu crest football-data.org bila PNG.
 * SVG dilewati karena Android tidak bisa menampilkannya langsung. Hasil unduhan disimpan di disk
 * (tidak diunduh ulang) dan di memori. Bila semua gagal, tampil lingkaran berisi inisial klub.
 * Gambar ditampilkan FIT_CENTER (proporsi asli, tidak gepeng).
 */
final class LogoCache {
    private LogoCache() {}

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final LruCache<String, Bitmap> MEM = new LruCache<>(120);
    private static volatile JSONObject teams = new JSONObject();
    private static volatile boolean teamsLoaded = false;

    // ---- daftar klub (teams.json) ----

    private static File teamsFile(Context c) { return new File(c.getCacheDir(), "football_teams.json"); }

    /** Simpan teams.json hasil unduhan, dan pakai segera. */
    static void setTeamsJson(Context c, String json) {
        try {
            JSONObject o = new JSONObject(json);
            teams = o;
            teamsLoaded = true;
            writeFile(teamsFile(c), json.getBytes("UTF-8"));
        } catch (Exception ignored) { }
    }

    private static void ensureTeams(Context c) {
        if (teamsLoaded) return;
        try {
            File f = teamsFile(c);
            if (f.exists()) teams = new JSONObject(new String(readFile(f), "UTF-8"));
        } catch (Exception ignored) { }
        teamsLoaded = true;
    }

    /** Kandidat URL logo untuk klub, urut dari yang terbaik. */
    private static List<String> candidates(int teamId) {
        List<String> out = new ArrayList<>();
        JSONObject e = teams.optJSONObject(String.valueOf(teamId));
        if (e == null) return out;
        String badge = e.optString("badge", "");
        String crest = e.optString("crest", "");
        if (!badge.isEmpty() && !"null".equals(badge)) out.add(badge);
        if (!crest.isEmpty() && !"null".equals(crest) && crest.toLowerCase().endsWith(".png")) out.add(crest);
        return out;
    }

    // ---- tampil ----

    /** Pasang logo klub ke ImageView. Langsung tampil inisial, lalu diganti logo bila sudah ada. */
    static void load(final Context ctx, final int teamId, final String name, final ImageView iv) {
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
        iv.setImageDrawable(initials(ctx, name));
        if (teamId <= 0) return;
        final String tag = "logo:" + teamId;
        iv.setTag(tag);
        Bitmap m = MEM.get(tag);
        if (m != null) { iv.setImageBitmap(m); return; }
        final Context app = ctx.getApplicationContext();
        POOL.execute(() -> {
            final Bitmap b = fetch(app, teamId);
            if (b == null) return;
            MEM.put(tag, b);
            iv.post(() -> { if (tag.equals(iv.getTag())) iv.setImageBitmap(b); });
        });
    }

    private static Bitmap fetch(Context c, int teamId) {
        ensureTeams(c);
        File dir = new File(c.getCacheDir(), "logos");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File f = new File(dir, teamId + ".png");
        try {
            if (f.exists() && f.length() > 0) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b != null) return b;
            }
        } catch (Throwable ignored) { }
        for (String url : candidates(teamId)) {
            byte[] data = download(url);
            if (data == null) continue;
            Bitmap b = decode(data);
            if (b == null) continue;               // mis. SVG: coba kandidat berikutnya
            try { writeFile(f, data); } catch (Exception ignored) { }
            return b;
        }
        return null;
    }

    private static Bitmap decode(byte[] data) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int max = Math.max(o.outWidth, o.outHeight);
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = max > 512 ? max / 256 : 1;   // logo besar dikecilkan, hemat memori
            return BitmapFactory.decodeByteArray(data, 0, data.length, o2);
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] download(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "SwadayaTV");
            if (conn.getResponseCode() != 200) return null;
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > 3_000_000) return null;
            }
            in.close();
            return bo.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ---- berkas ----

    private static void writeFile(File f, byte[] data) throws Exception {
        FileOutputStream o = new FileOutputStream(f);
        o.write(data);
        o.close();
    }

    private static byte[] readFile(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return bo.toByteArray();
    }

    // ---- cadangan: lingkaran + inisial ----

    private static Drawable initials(Context c, String name) {
        int px = 96;
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF24352D);
        cv.drawCircle(px / 2f, px / 2f, px / 2f, p);
        String t = "?";
        if (name != null) {
            String[] w = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String x : w) { if (!x.isEmpty() && sb.length() < 2 && Character.isLetter(x.charAt(0))) sb.append(Character.toUpperCase(x.charAt(0))); }
            if (sb.length() > 0) t = sb.toString();
        }
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(px * 0.42f);
        p.setTextAlign(Paint.Align.CENTER);
        cv.drawText(t, px / 2f, px / 2f - (p.descent() + p.ascent()) / 2f, p);
        return new BitmapDrawable(c.getResources(), b);
    }
}
