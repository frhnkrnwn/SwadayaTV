package com.swadaya.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** What this app disabled, kept on the phone so every change can be undone. */
final class RestoreStore {

    static final class Entry {
        final String pkg;
        final String name;
        final String tv;
        final long at;

        Entry(String pkg, String name, String tv, long at) {
            this.pkg = pkg;
            this.name = name;
            this.tv = tv;
            this.at = at;
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("restore", Context.MODE_PRIVATE);
    }

    static void add(Context ctx, String pkg, String name, String tv) {
        List<Entry> all = all(ctx);
        for (Entry e : all) {
            if (e.pkg.equals(pkg) && e.tv.equals(tv)) {
                return;
            }
        }
        all.add(new Entry(pkg, name, tv, System.currentTimeMillis()));
        save(ctx, all);
    }

    static void remove(Context ctx, String pkg, String tv) {
        List<Entry> all = all(ctx);
        List<Entry> keep = new ArrayList<>();
        for (Entry e : all) {
            if (!(e.pkg.equals(pkg) && e.tv.equals(tv))) {
                keep.add(e);
            }
        }
        save(ctx, keep);
    }

    static List<Entry> forTv(Context ctx, String tv) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all(ctx)) {
            if (tv == null || tv.isEmpty() || e.tv.equals(tv)) {
                out.add(e);
            }
        }
        return out;
    }

    static List<Entry> all(Context ctx) {
        List<Entry> list = new ArrayList<>();
        String raw = prefs(ctx).getString("entries", "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Entry(o.getString("pkg"), o.optString("name", ""),
                        o.optString("tv", ""), o.optLong("at", 0)));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static void save(Context ctx, List<Entry> entries) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : entries) {
                JSONObject o = new JSONObject();
                o.put("pkg", e.pkg);
                o.put("name", e.name);
                o.put("tv", e.tv);
                o.put("at", e.at);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        prefs(ctx).edit().putString("entries", arr.toString()).apply();
    }

    /** A plain script that restores everything without this app, only needing adb somewhere. */
    static String script(Context ctx, String tvHost, String tvModel) {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/usr/bin/env sh\n");
        sb.append("# Pemulihan Swadaya App");
        if (!tvModel.isEmpty()) {
            sb.append(" — ").append(tvModel);
        }
        sb.append("\n");
        if (!tvHost.isEmpty()) {
            sb.append("adb connect ").append(tvHost).append(":5555\n");
        }
        for (Entry e : forTv(ctx, tvModel)) {
            sb.append("adb shell pm enable --user 0 ").append(e.pkg).append("\n");
        }
        return sb.toString();
    }
}
