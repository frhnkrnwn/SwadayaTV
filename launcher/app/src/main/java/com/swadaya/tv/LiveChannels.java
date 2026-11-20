package com.swadaya.tv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Live TV screen's Indonesian channels: TVRI's national, world and regional streams (its own
 * public HLS servers) and Metro TV. Each channel is a plain HLS playlist played by Android's media
 * framework, so there is no web player, no sign-in and no DRM to work around.
 *
 * <p>There is no free programme guide for these streams, so {@link #load()} returns an empty map
 * and the screen shows the channel and "Live now" instead of now/next programme titles.
 */
final class LiveChannels {
    /** The guide keeps programmes from this long ago... */
    static final long KEEP_BEFORE_MS = 2 * 3600_000L;
    /** ...to this far ahead. */
    static final long KEEP_AHEAD_MS = 36 * 3600_000L;
    private static final String TVRI_LOGO =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/TVRILogo2019.svg/240px-TVRILogo2019.svg.png";
    private static final String METRO_LOGO = "https://i.imgur.com/QnU70NI.png";
    private static final String TVRI = "https://ott-balancer.tvri.go.id/live/eds/";

    /** Where a channel comes from, shown in Settings → Live TV. */
    enum Site {
        TVRI("TVRI"),
        METRO("Metro TV");

        final String label;

        Site(String label) {
            this.label = label;
        }
    }

    static final class Channel {
        final String slug, name, url, logoUrl;
        final Site site;

        Channel(String slug, String name, Site site, String url, String logoUrl) {
            this.slug = slug;
            this.name = name;
            this.site = site;
            this.url = url;
            this.logoUrl = logoUrl;
        }

        String page() {
            return url;
        }

        String logoUrl() {
            return logoUrl;
        }
    }

    private static Channel tvri(String slug, String name, String path) {
        return new Channel(slug, name, Site.TVRI, TVRI + path + "/hls/" + path + ".m3u8", TVRI_LOGO);
    }

    /** In the order the screen lists them: national first, then Metro, then TVRI's regions. */
    static final List<Channel> ALL = Collections.unmodifiableList(java.util.Arrays.asList(
            tvri("tvri-nasional", "TVRI Nasional", "Nasional"),
            new Channel("metro-tv", "Metro TV", Site.METRO,
                    "https://edge.medcom.id/live-edge/smil:metro.smil/playlist.m3u8", METRO_LOGO),
            tvri("tvri-world", "TVRI World", "TVRIWorld"),
            tvri("tvri-aceh", "TVRI Aceh", "Aceh"),
            tvri("tvri-bali", "TVRI Bali", "Bali"),
            tvri("tvri-babel", "TVRI Bangka Belitung", "Babel"),
            tvri("tvri-bengkulu", "TVRI Bengkulu", "Bengkulu"),
            tvri("tvri-jateng", "TVRI Jawa Tengah", "Jateng"),
            tvri("tvri-kalteng", "TVRI Kalimantan Tengah", "Kalteng"),
            tvri("tvri-sulteng", "TVRI Sulawesi Tengah", "Sulteng"),
            tvri("tvri-jatim", "TVRI Jawa Timur", "Jatim"),
            tvri("tvri-kaltim", "TVRI Kalimantan Timur", "Kaltim"),
            tvri("tvri-ntt", "TVRI Nusa Tenggara Timur", "NTT"),
            tvri("tvri-gorontalo", "TVRI Gorontalo", "Gorontalo"),
            tvri("tvri-jakarta", "TVRI Jakarta", "DKI"),
            tvri("tvri-jambi", "TVRI Jambi", "Jambi"),
            tvri("tvri-lampung", "TVRI Lampung", "Lampung"),
            tvri("tvri-maluku", "TVRI Maluku", "Ambon"),
            tvri("tvri-sulut", "TVRI Sulawesi Utara", "Sulut"),
            tvri("tvri-sumut", "TVRI Sumatra Utara", "Sumut"),
            tvri("tvri-papua", "TVRI Papua", "Papua"),
            tvri("tvri-riau", "TVRI Riau", "Riau"),
            tvri("tvri-kalsel", "TVRI Kalimantan Selatan", "Kalsel"),
            tvri("tvri-sulsel", "TVRI Sulawesi Selatan", "Sulsel"),
            tvri("tvri-sumsel", "TVRI Sumatra Selatan", "Sumsel"),
            tvri("tvri-sultra", "TVRI Sulawesi Tenggara", "Sultra"),
            tvri("tvri-jabar", "TVRI Jawa Barat", "Jabar"),
            tvri("tvri-kalbar", "TVRI Kalimantan Barat", "Kalbar"),
            tvri("tvri-ntb", "TVRI Nusa Tenggara Barat", "NTB"),
            tvri("tvri-pabar", "TVRI Papua Barat", "Pabar"),
            tvri("tvri-sulbar", "TVRI Sulawesi Barat", "Sulbar"),
            tvri("tvri-sumbar", "TVRI Sumatra Barat", "Sumbar"),
            tvri("tvri-yogyakarta", "TVRI Yogyakarta", "Jogjakarta")));

    /** The stations switched on in Settings → Live TV, in list order. */
    static List<Channel> enabled(android.content.Context ctx) {
        List<Channel> out = new ArrayList<>();
        for (Channel c : ALL) if (LauncherSettings.flag(ctx, LauncherSettings.stationKey(c.slug))) out.add(c);
        return out;
    }

    static Channel byPage(String url) {
        for (Channel c : ALL) if (c.page().equals(url)) return c;
        return null;
    }

    static boolean isChannelPage(String url) {
        return byPage(url) != null;
    }

    static final class Programme {
        final long start, end; // epoch ms
        final String title, description;

        Programme(long start, long end, String title, String description) {
            this.start = start;
            this.end = end;
            this.title = title;
            this.description = description;
        }

        boolean airingAt(long now) {
            return start <= now && now < end;
        }

        float progress(long now) {
            return end <= start ? 0f : Math.max(0f, Math.min(1f, (now - start) / (float) (end - start)));
        }
    }

    /** No free guide exists for these streams: the screen says "Live now" instead. */
    static Map<String, List<Programme>> load() {
        return Collections.emptyMap();
    }

    /** The programme on now, or null. */
    static Programme now(List<Programme> list, long now) {
        if (list != null) for (Programme p : list) if (p.airingAt(now)) return p;
        return null;
    }

    /** Up to {@code max} programmes starting after {@code now}. */
    static List<Programme> upcoming(List<Programme> list, long now, int max) {
        List<Programme> out = new ArrayList<>();
        if (list != null) for (Programme p : list) {
            if (out.size() == max) break;
            if (p.start > now) out.add(p);
        }
        return out;
    }

    /** A channel logo, capped at {@code limit} bytes. */
    static byte[] download(String url, int limit) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
                byte[] buf = new byte[16384];
                for (int n; (n = in.read(buf)) != -1; ) {
                    if (out.size() + n > limit) throw new IOException("Too large");
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }
}
