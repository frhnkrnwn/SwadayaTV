package com.swadaya.tv;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Is there a newer SwadayaTV? Asks the project's own site first (release/version.json), which is
 * written by the same build that publishes release/tvhome-latest.apk: one file and one APK from
 * one commit, and no API limit. GitHub's latest release stays as the fallback for when the site
 * can't be read (the API allows 60 requests an hour per IP, and a shared network uses them up).
 * Releases are signed with the same key as the installed app, so UpdateActivity can install one
 * over it, settings kept. Debug builds don't look: they're signed with another key.
 */
final class UpdateCheck {
    static final String REPO = "frhnkrnwn/SwadayaTV";
    private static final String LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String RELEASES = "https://api.github.com/repos/" + REPO + "/releases?per_page=30";
    /** The site's version file: the same APK its download button hands out. Regenerated each build. */
    private static final String SITE = "https://frhnkrnwn.github.io/SwadayaTV/release/version.json";

    /** A release newer than the installed app. */
    static final class Release {
        final String version, url, sha256; // sha256: null when GitHub doesn't give one
        final long size;

        Release(String version, String url, long size, String sha256) {
            this.version = version;
            this.url = url;
            this.size = size;
            this.sha256 = sha256;
        }
    }

    private UpdateCheck() {}

    // The last check's answer, whichever ran it (the bell's daily one or Settings → About), so
    // About shows it and the daily check knows when it's next due. Kept in prefs: a launcher
    // restart doesn't check GitHub again before the day is up.
    private static final String PREFS = "update_check";
    private static volatile Release lastFound;
    private static volatile long lastAt;
    private static volatile boolean lastFailed, loaded;

    /** Reads the saved answer once. A saved release that's no longer newer (just installed) is dropped. */
    static synchronized void load(Context ctx) {
        if (loaded) return;
        loaded = true;
        android.content.SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        lastAt = p.getLong("at", 0);
        lastFailed = p.getBoolean("failed", false);
        String v = p.getString("version", null), current = installedVersion(ctx);
        if (v != null && current != null && compare(v, current) > 0) {
            lastFound = new Release(v, p.getString("url", ""), p.getLong("size", 0), p.getString("sha256", null));
        }
    }

    private static void save(Context ctx) {
        Release r = lastFound;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("at", lastAt).putBoolean("failed", lastFailed)
                .putString("version", r != null ? r.version : null).putString("url", r != null ? r.url : null)
                .putLong("size", r != null ? r.size : 0).putString("sha256", r != null ? r.sha256 : null)
                .apply();
    }

    /** The newer release the last check found, or null. */
    static Release lastFound() {
        return lastFound;
    }

    /** When the last check finished (epoch ms), or 0 if there hasn't been one. */
    static long lastCheckedAt() {
        return lastAt;
    }

    /** The last check couldn't reach GitHub. */
    static boolean lastFailed() {
        return lastFailed;
    }

    /** The newer release, or null (up to date, offline, a debug build, or no APK in it). Blocks. */
    static Release newer(Context ctx) {
        try {
            return check(ctx);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Settings → About → Check for updates: the newer release, or null when this is the latest;
     * throws when GitHub can't be reached (or for a debug build, which couldn't take it). Blocks.
     */
    static Release check(Context ctx) throws Exception {
        try {
            Release r = fetch(ctx);
            lastFound = r;
            lastFailed = false;
            return r;
        } catch (Exception e) {
            lastFailed = true; // the last one found (if any) stays: it's still out there
            throw e;
        } finally {
            lastAt = System.currentTimeMillis();
            loaded = true; // this answer is newer than anything saved
            save(ctx);
        }
    }

    private static Release fetch(Context ctx) throws Exception {
        if ((ctx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            throw new IllegalStateException("a debug build can't take updates");
        }
        String current = installedVersion(ctx);
        if (current == null) throw new IllegalStateException("no version");
        try {
            return fromSite(current);
        } catch (Exception site) {
            // The site is one small file and never rate-limits. GitHub is only the fallback: an
            // unreachable or renamed site must not leave a TV unable to update.
            try {
                return fromGitHub(current);
            } catch (Exception github) {
                throw new IllegalStateException("situs: " + message(site) + "; GitHub: " + message(github));
            }
        }
    }

    /**
     * The site's version.json: the exact APK the download button serves, with its size and SHA-256.
     * Returns null when this build already has it, so being up to date never falls back to GitHub
     * and disagrees with the site.
     */
    private static Release fromSite(String current) throws Exception {
        JSONObject r = new JSONObject(get(SITE, null));
        String version = r.optString("versionName", "").trim();
        if (version.isEmpty()) throw new IllegalStateException("no version in version.json");
        if (compare(version, current) <= 0) return null;
        String url = r.optString("apkUrl", "").trim();
        if (!url.startsWith("https://") || !url.endsWith(".apk")) {
            throw new IllegalStateException("no APK in version.json");
        }
        String sha = r.optString("apkSha256", "").trim().toLowerCase(java.util.Locale.ROOT);
        if (!sha.matches("[0-9a-f]{64}")) sha = null; // the installer still verifies the key
        return new Release(version, url, r.optLong("apkSize", 0), sha);
    }

    /** GitHub's latest release, used only when the site can't be read. Null when up to date. */
    private static Release fromGitHub(String current) throws Exception {
        JSONObject r = new JSONObject(get(LATEST, "application/vnd.github+json"));
        String version = versionFromTag(r.optString("tag_name"));
        if (version.isEmpty()) throw new IllegalStateException("no release");
        if (compare(version, current) <= 0) return null;
        JSONArray assets = r.optJSONArray("assets");
        for (int i = 0; assets != null && i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            String name = a.optString("name");
            if (!name.startsWith("tvhome-") || !name.endsWith(".apk")) continue;
            String digest = a.optString("digest", "");
            String sha = digest.startsWith("sha256:") ? digest.substring(7).toLowerCase(java.util.Locale.ROOT) : null;
            return new Release(version, a.optString("browser_download_url"), a.optLong("size"), sha);
        }
        return null; // a newer release without its APK yet (CI still building): nothing to install
    }

    private static String message(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    /** Releases are tagged v<version>-build<run>; the app compares and shows just the version. */
    static String versionFromTag(String tag) {
        return tag.replaceFirst("^[vV]", "").replaceFirst("-build\\d+$", "");
    }

    /**
     * The tag of the newest release a build of {@code version} came from ("v1.1.3-build31"), for
     * the files that release carries beside its APK, or null. Drafts and pre-releases are skipped
     * like GitHub's own "latest"; {@code releases} is the array from the releases API. Blocks only
     * in {@link #releaseTag}.
     */
    static String tagFor(JSONArray releases, String version) {
        for (int i = 0; releases != null && i < releases.length(); i++) {
            JSONObject r = releases.optJSONObject(i);
            if (r == null || r.optBoolean("draft") || r.optBoolean("prerelease")) continue;
            String tag = r.optString("tag_name");
            if (version.equals(versionFromTag(tag))) return tag;
        }
        return null;
    }

    /**
     * The release tag the site names for {@code version}, or null (a TV running an older build than
     * the site describes). Saves a rate-limited API call when the traffic engine needs its files.
     * Blocks.
     */
    static String siteTag(String version) {
        try {
            JSONObject r = new JSONObject(get(SITE, null));
            String tag = r.optString("tag", "").trim();
            return version.equals(r.optString("versionName", "")) && tag.startsWith("v") ? tag : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The release tag for {@code version}: the site's answer first, GitHub's releases when the site
     * can't name it (an older build, or offline). Null when neither knows. Blocks.
     */
    static String releaseTag(String version) {
        String tag = siteTag(version);
        if (tag != null) return tag;
        try {
            return tagFor(new JSONArray(get(RELEASES, "application/vnd.github+json")), version);
        } catch (Exception e) {
            return null;
        }
    }

    static String installedVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Dotted versions compared number by number: 1.10.0 > 1.9.1, 1.10.1 > 1.10. */
    static int compare(String a, String b) {
        String[] x = a.split("[.-]"), y = b.split("[.-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? number(x[i]) : 0, q = i < y.length ? number(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int number(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** One GET as a string. {@code accept} is null for the site, GitHub's media type for the API. */
    private static String get(String url, String accept) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(20_000);
        if (accept != null) c.setRequestProperty("Accept", accept);
        c.setRequestProperty("User-Agent", "tvhome");
        try {
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[16 * 1024];
                for (int r; (r = in.read(buf)) > 0; ) {
                    out.write(buf, 0, r);
                    if (out.size() > 2_000_000) throw new IllegalStateException("response too large");
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            c.disconnect();
        }
    }
}
