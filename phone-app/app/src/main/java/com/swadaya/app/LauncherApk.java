package com.swadaya.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * The newest SwadayaTV APK, so the phone can fetch it and hand it to the TV without the user copying
 * a file around.
 *
 * It asks the project's site first ({@code release/version.json}): that file is written by the same
 * build that publishes {@code release/tvhome-latest.apk}, so the version, size and SHA-256 always
 * describe the file behind it, and the site has no rate limit. GitHub's Releases stay as the
 * fallback ({@code releases/latest} and its redirect). Either way the APK is signed with the same
 * key every release is, and it installs over the version on the TV with its settings kept.
 */
final class LauncherApk {

    private static final String REPO = "frhnkrnwn/SwadayaTV";
    private static final String LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";
    /** The site's own version file: one build, one APK, no API limit. */
    private static final String SITE = "https://frhnkrnwn.github.io/SwadayaTV/release/version.json";

    /** Download progress, 0–100. Called on the calling thread. */
    interface Progress {
        void onPercent(int percent);
    }

    /** The launcher APK of the latest release. */
    static final class Release {
        final String version;
        final String asset;
        final String url;
        final String sha256; // null when GitHub doesn't state one
        final long size;     // 0 when unknown

        Release(String version, String asset, String url, long size, String sha256) {
            this.version = version;
            this.asset = asset;
            this.url = url;
            this.size = size;
            this.sha256 = sha256;
        }
    }

    private LauncherApk() {
    }

    /** The latest release's launcher APK. Blocks; throws with a readable reason. */
    static Release latest() throws Exception {
        StringBuilder why = new StringBuilder();
        try {
            return fromSite();
        } catch (Exception e) {
            why.append("situs: ").append(reason(e));
        }
        try {
            return fromApi();
        } catch (Exception e) {
            // The API is rate limited per IP (60 requests an hour without a token) and a shared
            // network can use them all up: the release's own download link still works, so ask
            // the releases page where it redirects instead and build the link from that.
            why.append("; GitHub API: ").append(reason(e));
        }
        try {
            return fromRedirect();
        } catch (Exception e) {
            throw new Exception("Server tidak bisa dihubungi (" + why + "; " + reason(e) + ").");
        }
    }

    private static String reason(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    /** The site's version.json, which names the very APK its download button serves. */
    private static Release fromSite() throws Exception {
        JSONObject r = new JSONObject(get(SITE, null));
        String version = r.optString("versionName", "").trim();
        String url = r.optString("apkUrl", "").trim();
        if (version.isEmpty() || !url.startsWith("https://") || !url.endsWith(".apk")) {
            throw new Exception("version.json situs tidak lengkap");
        }
        String name = r.optString("apkName", "").trim();
        String sha = r.optString("apkSha256", "").trim().toLowerCase(Locale.ROOT);
        return new Release(version, name.isEmpty() ? assetName(url) : name, url,
                r.optLong("apkSize", 0), sha.matches("[0-9a-f]{64}") ? sha : null);
    }

    /** The latest release and its tvhome asset, from the GitHub API (it states the SHA-256). */
    private static Release fromApi() throws Exception {
        String body;
        try {
            body = get(LATEST, "application/vnd.github+json");
        } catch (Exception e) {
            String why = e.getMessage() == null ? e.toString() : e.getMessage();
            if (why.contains("404")) {
                throw new Exception("belum ada rilis di GitHub");
            }
            throw new Exception(why.toLowerCase(Locale.ROOT));
        }

        JSONObject release = new JSONObject(body);
        String version = versionFromTag(release.optString("tag_name"));
        JSONArray assets = release.optJSONArray("assets");
        for (int i = 0; assets != null && i < assets.length(); i++) {
            JSONObject a = assets.optJSONObject(i);
            if (a == null) {
                continue;
            }
            String name = a.optString("name");
            if (!name.startsWith("tvhome-") || !name.endsWith(".apk")) {
                continue;
            }
            String digest = a.optString("digest", "");
            String sha = digest.startsWith("sha256:")
                    ? digest.substring("sha256:".length()).toLowerCase(Locale.ROOT)
                    : null;
            return new Release(version, name, a.optString("browser_download_url"),
                    a.optLong("size"), sha);
        }
        throw new Exception("rilis terbaru belum membawa APK tvhome");
    }

    /**
     * The same release without the API: {@code /releases/latest} redirects to the newest tag, and
     * {@code /releases/latest/download/tvhome-<version>.apk} is that tag's launcher APK.
     */
    private static Release fromRedirect() throws Exception {
        String page = "https://github.com/" + REPO + "/releases/latest";
        HttpURLConnection c = (HttpURLConnection) new URL(page).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(20_000);
        c.setReadTimeout(20_000);
        c.setRequestProperty("User-Agent", "swadaya-app");
        String location;
        try {
            int code = c.getResponseCode();
            location = c.getHeaderField("Location");
            if (code < 300 || code >= 400 || location == null || !location.contains("/releases/tag/")) {
                throw new Exception("halaman Releases tidak menunjuk ke satu rilis (HTTP " + code + ")");
            }
        } finally {
            c.disconnect();
        }

        String tag = location.substring(location.lastIndexOf('/') + 1);
        String version = versionFromTag(tag);
        if (version.isEmpty()) {
            throw new Exception("tag rilis terbaru tidak bisa dibaca");
        }
        String asset = "tvhome-" + version + ".apk";
        String url = page + "/download/" + asset;
        return new Release(version, asset, url, sizeOf(url), null);
    }

    /** Content length of the asset, or 0 when GitHub doesn't say; throws when it isn't there. */
    private static long sizeOf(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(20_000);
        c.setRequestProperty("User-Agent", "swadaya-app");
        c.setRequestProperty("Range", "bytes=0-0"); // ask for the size, not the whole APK
        try {
            int code = c.getResponseCode();
            if (code != 200 && code != 206) {
                throw new Exception(assetName(url) + " tidak ada di rilis terbaru (HTTP " + code + ")");
            }
            String range = c.getHeaderField("Content-Range"); // "bytes 0-0/595613"
            if (range != null) {
                int slash = range.lastIndexOf('/');
                try {
                    return Long.parseLong(range.substring(slash + 1).trim());
                } catch (Exception ignored) {
                    return 0;
                }
            }
            return Math.max(0, c.getContentLength());
        } finally {
            c.disconnect();
        }
    }

    private static String assetName(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    /**
     * Downloads the release APK to {@code dest}, replacing whatever was there. Blocks; throws when
     * the download is cut short, doesn't match the announced size, or doesn't match its SHA-256.
     */
    static void download(Release r, File dest, Progress progress) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(r.url).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(30_000);
        c.setRequestProperty("User-Agent", "swadaya-app");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = r.size;
        long read = 0;
        try {
            int code = c.getResponseCode();
            if (code != 200) {
                throw new Exception("Unduhan ditolak (HTTP " + code + ").");
            }
            if (total <= 0) {
                total = c.getContentLength();
            }
            try (InputStream in = c.getInputStream();
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                int last = -1;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    digest.update(buf, 0, n);
                    read += n;
                    if (total > 0) {
                        int pct = (int) Math.min(100, read * 100 / total);
                        if (pct != last) {
                            last = pct;
                            progress.onPercent(pct);
                        }
                    }
                }
            }
        } finally {
            c.disconnect();
        }

        if (total > 0 && read != total) {
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
            throw new Exception("Unduhan terputus (" + read + " dari " + total + " byte).");
        }
        if (r.sha256 != null) {
            String got = hex(digest.digest());
            if (!got.equalsIgnoreCase(r.sha256)) {
                //noinspection ResultOfMethodCallIgnored
                dest.delete();
                throw new Exception("Berkas yang diunduh rusak (SHA-256 tidak cocok).");
            }
        }
        progress.onPercent(100);
    }

    /** "1.2.0" from a release tag such as "v1.2.0-build35". */
    static String versionFromTag(String tag) {
        return tag.replaceFirst("^[vV]", "").replaceFirst("-build\\d+$", "");
    }

    /** "12,4 MB", for the download line. */
    static String sizeText(long bytes) {
        if (bytes <= 0) {
            return "ukuran tidak diketahui";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

    /** One GET as a string. {@code accept} is null for the site, GitHub's media type for the API. */
    private static String get(String url, String accept) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(20_000);
        if (accept != null) {
            c.setRequestProperty("Accept", accept);
        }
        c.setRequestProperty("User-Agent", "swadaya-app");
        try {
            int code = c.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException("HTTP " + code);
            }
            try (InputStream in = c.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > 2_000_000) {
                        throw new IllegalStateException("jawaban GitHub terlalu besar");
                    }
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            c.disconnect();
        }
    }
}
