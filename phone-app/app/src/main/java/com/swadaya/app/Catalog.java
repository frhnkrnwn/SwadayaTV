package com.swadaya.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The debloat lists: which packages are safe to switch off, per category and brand. */
final class Catalog {

    static final class App {
        final String pkg;
        final String name;
        final String group;
        final String[] brands;
        final boolean keep;

        App(String pkg, String name, String group, String[] brands, boolean keep) {
            this.pkg = pkg;
            this.name = name;
            this.group = group;
            this.brands = brands;
            this.keep = keep;
        }
    }

    static final class Group {
        final String id;
        final String name;
        final String desc;

        Group(String id, String name, String desc) {
            this.id = id;
            this.name = name;
            this.desc = desc;
        }
    }

    final List<Group> groups = new ArrayList<>();
    final List<App> apps = new ArrayList<>();

    Set<String> groupIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Group g : groups) {
            ids.add(g.id);
        }
        return ids;
    }

    Group group(String id) {
        for (Group g : groups) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }

    static Catalog load(Context ctx) throws Exception {
        String json;
        try (InputStream in = ctx.getAssets().open("bloat.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            json = new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        JSONObject root = new JSONObject(json);
        Catalog c = new Catalog();

        JSONArray gs = root.optJSONArray("groups");
        if (gs != null) {
            for (int i = 0; i < gs.length(); i++) {
                JSONObject g = gs.getJSONObject(i);
                c.groups.add(new Group(g.getString("id"), g.getString("name"), g.optString("desc", "")));
            }
        }

        JSONArray apps = root.optJSONArray("apps");
        if (apps != null) {
            for (int i = 0; i < apps.length(); i++) {
                JSONObject a = apps.getJSONObject(i);
                String pkg = a.getString("p");
                if (!pkg.matches("[A-Za-z0-9_.]+")) {
                    continue;
                }
                JSONArray bs = a.optJSONArray("b");
                String[] brands = null;
                if (bs != null && bs.length() > 0) {
                    brands = new String[bs.length()];
                    for (int j = 0; j < bs.length(); j++) {
                        brands[j] = bs.getString(j).toLowerCase();
                    }
                }
                c.apps.add(new App(pkg, a.optString("n", pkg), a.optString("g", "bloat"),
                        brands, a.optBoolean("keep", false)));
            }
        }
        return c;
    }
}
