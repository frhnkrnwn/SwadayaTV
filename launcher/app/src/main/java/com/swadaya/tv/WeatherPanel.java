package com.swadaya.tv;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Weather card, under the prayer card: today's BMKG forecast for the city set in
 * Settings → Weather, a feels-like reading from BMKG's nearest forecast slot, then air quality
 * for the same city: Copernicus's US AQI and PM2.5 (Open-Meteo, no key). Weather (Weather.java)
 * does the parsing; BMKG is credited on the card, as its terms ask.
 *
 * Everything is kept in files/weather/ and shown from there straight away (offline too), and
 * fetched again as its source changes: BMKG's forecast hourly, Copernicus's values each new
 * hour. It's checked on the clock's minute tick, so there's no polling; the fetches run on their
 * own thread so a slow server never holds up the app list.
 */
final class WeatherPanel {
    private static final String BMKG = "https://api.bmkg.go.id/publik/prakiraan-cuaca";
    private static final String AIR = "https://air-quality-api.open-meteo.com/v1/air-quality"
            + "?current=us_aqi,pm2_5,aerosol_optical_depth&timezone=Asia%2FJakarta";
    private static final long FORECAST_EVERY_MS = 3_600_000L, HOURLY_RETRY_MS = 10 * 60_000L,
            RETRY_MS = 5 * 60_000L, OLD_MS = 3 * 3_600_000L;
    /** Minutes past the hour Copernicus has its new reading out. */
    private static final int AIR_MINUTE = 1;
    private static final int MAX_BYTES = 2 << 20;
    private static final int TEXT = 0xFFFFFFFF, DIM = 0x8CFFFFFF, ICON = 0xA6FFFFFF;
    private static final String NONE = "–";
    /** One thread for all weather fetches (the card's and Settings'), apart from the app's worker. */
    private static final ExecutorService NET = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "weather");
        t.setDaemon(true);
        return t;
    });

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final float dp;
    private final ImageView titleIcon, skyIcon;
    private final TextView title, outlook, temp, feelsLabel, feelsTemp, skyTime;
    private final ImageView[] partIcons = new ImageView[3]; // morning, afternoon, night
    private final Gauge pm25, aqi;
    private final WeatherEffects effects; // the home screen's rain, lightning and haze, or null
    private boolean fetching;
    private long lastAttempt;
    private final WeatherFileCache<Weather.Day> forecastCache = new WeatherFileCache<>();
    private final WeatherFileCache<Weather.Now> nowCache = new WeatherFileCache<>();
    private final WeatherFileCache<Weather.Air> airCache = new WeatherFileCache<>();

    WeatherPanel(Context ctx, LinearLayout root, WeatherEffects effects) {
        this.ctx = ctx;
        this.effects = effects;
        dp = ctx.getResources().getDisplayMetrics().density;
        // Title: WEATHER · BANDUNG, and today's outlook in words on the right.
        LinearLayout header = new LinearLayout(ctx);
        header.setGravity(Gravity.CENTER_VERTICAL);
        titleIcon = icon(R.drawable.ic_sun_ring, DIM); // a ring sun: not the prayer card's solid ones
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(px(12), px(12));
        tlp.setMarginEnd(px(5));
        header.addView(titleIcon, tlp);
        title = small("WEATHER", DIM, 10.5f, 0.08f);
        header.addView(title);
        outlook = small("", DIM, 10.5f, 0.02f);
        outlook.setGravity(Gravity.END);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        olp.setMarginStart(px(10));
        header.addView(outlook, olp);
        header.setPadding(0, 0, px(8), 0); // the card's right edge, as the columns below have
        root.addView(header);

        LinearLayout row = new LinearLayout(ctx);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = px(9);
        root.addView(row, rlp);

        // Weather: the day's range, then morning / afternoon / night, then when BMKG's forecast came in.
        LinearLayout sky = column(row, 1.65f);
        LinearLayout top = new LinearLayout(ctx);
        top.setGravity(Gravity.CENTER_VERTICAL);
        skyIcon = icon(R.drawable.ic_cloud, ICON);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(px(16), px(16));
        slp.setMarginEnd(px(6));
        top.addView(skyIcon, slp);
        // The range, then how hot it feels now (BMKG), with a small word before it: "FEELS 35°C".
        LinearLayout temps = new LinearLayout(ctx); // baseline-aligned: the word sits on the numbers' line
        temp = value();
        temps.addView(temp);
        feelsLabel = small("FEELS", DIM, 8.5f, 0.06f);
        temps.addView(feelsLabel, start(9));
        feelsTemp = value();
        temps.addView(feelsTemp, start(4));
        top.addView(temps);
        sky.addView(top);
        LinearLayout parts = new LinearLayout(ctx);
        parts.setGravity(Gravity.CENTER_VERTICAL);
        String[] names = {"MORNING", "AFTERNOON", "NIGHT"};
        for (int i = 0; i < 3; i++) {
            partIcons[i] = icon(R.drawable.ic_cloud, ICON);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(px(12), px(12));
            ilp.setMarginEnd(px(2));
            if (i > 0) ilp.setMarginStart(px(5));
            parts.addView(partIcons[i], ilp);
            parts.addView(small(names[i], DIM, 7.5f, 0.03f));
        }
        sky.addView(parts, top(5));
        skyTime = small("BMKG", DIM, 8.5f, 0.06f);
        sky.addView(skyTime, top(5));

        pm25 = new Gauge(column(row, 1f));
        aqi = new Gauge(column(row, 1f));
        render();
    }

    /** Called every minute (clock tick) and on resume: shows what's kept, fetches what's old. */
    void refresh() {
        // Fetched for the card or the effects (the effects work with the card hidden); both off: nothing.
        if (!LauncherSettings.flag(ctx, LauncherSettings.SHOW_WEATHER)
                && !LauncherSettings.flag(ctx, LauncherSettings.WEATHER_FX)) {
            if (effects != null) effects.setWeather(Weather.FX_NONE, false, false);
            return;
        }
        render();
        if (fetching || System.currentTimeMillis() - lastAttempt < RETRY_MS) return;
        if (stale(ctx)) fetch();
    }

    /** City changed: show and fetch for the new one now. */
    void settingsChanged() {
        lastAttempt = 0;
        refresh();
    }

    /** Network came back: retry soon after a failure. */
    void onNetworkAvailable() {
        if (!fetching && System.currentTimeMillis() - lastAttempt > 60_000) {
            lastAttempt = 0;
            refresh();
        }
    }

    private void fetch() {
        fetching = true;
        lastAttempt = System.currentTimeMillis();
        Context app = ctx.getApplicationContext();
        NET.execute(() -> {
            update(app);
            main.post(() -> {
                fetching = false;
                render();
            });
        });
    }

    // ---------------------------------------------------------------- display

    private void render() {
        List<Weather.Place> places = places(ctx);
        Weather.Place place = place(ctx, places);
        File ff = place == null ? null : file(ctx, forecastName(place.id));
        LocalDate today = LocalDate.now();
        Weather.Day day = ff == null ? null : forecastCache.get(ff, today.toString(),
                json -> Weather.parseForecast(json, place.id, today));
        setText(title, place != null ? "WEATHER · " + place.name.toUpperCase(Locale.ROOT) : "WEATHER");
        setText(outlook, day != null ? Weather.outlook(day) : "");
        if (day != null) {
            int kind = Weather.kind(day.summary);
            setIcon(skyIcon, kindIcon(kind));
            setText(temp, day.min + "–" + day.max + "°C");
            temp.setTextColor(TEXT);
            String[] p = {day.morning, day.afternoon, day.night};
            for (int i = 0; i < 3; i++) setIcon(partIcons[i], kindIcon(Weather.kind(p[i])));
            setText(skyTime, "BMKG · " + clock(ff.lastModified()));
        } else {
            setIcon(skyIcon, R.drawable.ic_cloud);
            setText(temp, NONE);
            temp.setTextColor(DIM);
            for (ImageView v : partIcons) setIcon(v, R.drawable.ic_cloud);
            setText(skyTime, "BMKG");
        }

        LocalDateTime now = LocalDateTime.now();
        Weather.Now cur = ff == null ? null : nowCache.get(ff, today + " " + now.truncatedTo(java.time.temporal.ChronoUnit.HOURS),
                json -> Weather.parseNow(json, LocalDateTime.now()));
        boolean nowOld = cur != null && now.isAfter(cur.time.plusHours(6));
        degrees(feelsTemp, feelsLabel, cur == null ? Double.NaN : cur.feels, nowOld);

        File af = place == null ? null : file(ctx, airName(place.id));
        Weather.Air air = af == null ? null : airCache.get(af, "", Weather::parseAir);
        if (air != null) {
            boolean old = System.currentTimeMillis() - af.lastModified() > OLD_MS;
            boolean pmKnown = !Double.isNaN(air.pm25);
            int pb = pmKnown ? Weather.pm25Band(air.pm25) : 0;
            String haze = Double.isNaN(air.haze) ? "" : String.format(Locale.ROOT, "Haze %.2f", air.haze);
            pm25.set(pmKnown ? "PM2.5 " + Math.round(air.pm25) : "PM2.5 –",
                    pmKnown ? Weather.US_SHORT[pb] : "", Weather.US_COLORS[pb], haze, DIM,
                    "COPERNICUS" + (air.time != null ? " · " + clock(air.time) : ""), old);
            int ab = Weather.band(air.aqi, Weather.US_TOP);
            aqi.set("AQI " + air.aqi, Weather.US_SHORT[ab], Weather.US_COLORS[ab],
                    pmKnown ? "PM2.5 " + Math.round(air.pm25) : "", DIM,
                    "COPERNICUS" + (air.time != null ? " · " + clock(air.time) : ""), old);
        } else {
            pm25.none("PM2.5 –", "COPERNICUS");
            aqi.none("AQI –", "COPERNICUS");
        }

        if (effects != null) {
            // The weather now (an old reading counts as none): rain, lightning, and haze when the air is unhealthy.
            boolean on = LauncherSettings.flag(ctx, LauncherSettings.WEATHER_FX);
            int code = cur == null || nowOld ? -1 : cur.code;
            effects.setWeather(on ? Weather.rainEffect(code) : Weather.FX_NONE, on && Weather.thunder(code),
                    on && air != null && air.aqi > 100);
        }
    }

    /** "35°C" and its word, greyed when old; hidden when there's no value. */
    private static void degrees(TextView number, TextView word, double value, boolean old) {
        int show = Double.isNaN(value) ? android.view.View.GONE : android.view.View.VISIBLE;
        number.setVisibility(show);
        word.setVisibility(show);
        if (Double.isNaN(value)) return;
        setText(number, Math.round(value) + "°C");
        number.setTextColor(old ? DIM : TEXT);
    }

    /** "15:40" or "3:40 PM", as the home clock is set. */
    private String clock(LocalDateTime t) {
        return t.format(java.time.format.DateTimeFormatter.ofPattern(
                LauncherSettings.use24h(ctx) ? "HH:mm" : "h:mm a", Locale.ENGLISH));
    }

    private String clock(long epochMs) {
        return clock(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMs), java.time.ZoneId.systemDefault()));
    }

    static int kindIcon(int kind) {
        switch (kind) {
            case Weather.SUN: return R.drawable.ic_sun_ring; // the title's ring sun: not the prayer icons' solid ones
            case Weather.RAIN: return R.drawable.ic_rain;
            case Weather.STORM: return R.drawable.ic_storm;
            default: return R.drawable.ic_cloud; // cloudy, hazy
        }
    }

    // ---------------------------------------------------------------- data (also for Settings)

    private static List<Weather.Place> placesKept;

    /** The Indonesian cities in assets/places.json (BMKG adm4 codes), or none yet. */
    static synchronized List<Weather.Place> places(Context ctx) {
        if (placesKept == null) {
            placesKept = Weather.parsePlaces(readAsset(ctx, "places.json"));
        }
        return placesKept;
    }

    private static String readAsset(Context ctx, String name) {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open(name), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) sb.append(line).append('\n');
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    /** The city set in Settings, or for Auto the nearest one. */
    static Weather.Place place(Context ctx, List<Weather.Place> places) {
        String id = LauncherSettings.weatherPlace(ctx);
        if (!id.isEmpty()) {
            Weather.Place p = Weather.place(places, id);
            if (p != null) return p;
        }
        Weather.Place a = autoPlace(ctx, places);
        if (a != null) return a;
        return places.isEmpty() ? null : places.get(0);
    }

    /** Auto: the city nearest the TV (where the prayer zone's Auto found it), else the prayer city. */
    static Weather.Place autoPlace(Context ctx, List<Weather.Place> places) {
        double[] here = LauncherSettings.where(ctx);
        if (here != null) {
            Weather.Place near = nearestPlace(places, here[0], here[1]);
            if (near != null) return near;
        }
        String zone = LauncherSettings.zone(ctx);
        for (Weather.Place p : places) if (p.name.equalsIgnoreCase(zone)) return p;
        return null;
    }

    /** The city nearest a spot, or null when every city is far away. */
    static Weather.Place nearestPlace(List<Weather.Place> places, double lat, double lon) {
        Weather.Place best = null;
        double bestD = Double.MAX_VALUE, k = Math.cos(Math.toRadians(lat));
        for (Weather.Place p : places) {
            if (Double.isNaN(p.lat) || Double.isNaN(p.lon)) continue;
            double dLat = lat - p.lat, dLon = (lon - p.lon) * k;
            double d = dLat * dLat + dLon * dLon;
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private boolean stale(Context ctx) {
        long now = System.currentTimeMillis();
        Weather.Place p = place(ctx, places(ctx));
        if (p == null) return false;
        File f = file(ctx, forecastName(p.id));
        LocalDate today = LocalDate.now();
        if (now - f.lastModified() > FORECAST_EVERY_MS
                || forecastCache.get(f, today.toString(),
                json -> Weather.parseForecast(json, p.id, today)) == null) return true;
        return airDue(ctx, p, now);
    }

    /** Copernicus's values for the city: a new hour has begun (or there's none yet). */
    private static boolean airDue(Context ctx, Weather.Place p, long now) {
        File f = file(ctx, airName(p.id));
        Weather.Air a = Weather.parseAir(read(f));
        LocalDateTime have = a == null || Double.isNaN(a.haze) ? null : a.time;
        return Weather.hourlyDue(have, LocalDateTime.now(), AIR_MINUTE, now - f.lastModified(), HOURLY_RETRY_MS);
    }

    /** Fetches whatever is old, in order (the city decides the rest). Blocking. */
    private static void update(Context ctx) {
        long now = System.currentTimeMillis();
        Weather.Place p = place(ctx, places(ctx));
        if (p == null) return;
        File f = file(ctx, forecastName(p.id));
        if (now - f.lastModified() > FORECAST_EVERY_MS
                || Weather.parseForecast(read(f), p.id, LocalDate.now()) == null) {
            deleteOthers(ctx, "forecast-", f.getName());
            fetchTo(ctx, BMKG + "?adm4=" + p.id, f.getName());
        }
        if (airDue(ctx, p, now)) {
            File a = file(ctx, airName(p.id));
            deleteOthers(ctx, "air-", a.getName());
            fetchTo(ctx, AIR + String.format(Locale.ROOT, "&latitude=%.4f&longitude=%.4f", p.lat, p.lon),
                    a.getName());
        }
    }

    private static String forecastName(String placeId) {
        return "forecast-" + placeId + ".json";
    }

    private static String airName(String placeId) {
        return "air-" + placeId + ".json";
    }

    private static File file(Context ctx, String name) {
        return new File(new File(ctx.getFilesDir(), "weather"), name);
    }

    private static String read(File f) {
        try {
            return f.isFile() ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static void deleteOthers(Context ctx, String prefix, String keep) {
        File[] old = new File(ctx.getFilesDir(), "weather").listFiles((d, n) -> n.startsWith(prefix) && !n.equals(keep));
        if (old != null) for (File f : old) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    /** Downloads to files/weather/{@code name}; a failure keeps the old file. */
    private static void fetchTo(Context ctx, String url, String name) {
        File dir = new File(ctx.getFilesDir(), "weather");
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        File tmp = new File(dir, name + ".tmp");
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15_000);
            c.setReadTimeout(15_000);
            c.setRequestProperty("User-Agent", Wallpapers.UA);
            c.setRequestProperty("Accept", "application/json");
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            int total = 0;
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] b = new byte[16 * 1024];
                for (int n; (n = in.read(b)) > 0; ) {
                    if (total == 0 && b[0] != '[' && b[0] != '{') throw new IOException("not JSON");
                    total += n;
                    if (total > MAX_BYTES) throw new IOException("too big");
                    out.write(b, 0, n);
                }
            }
            if (total == 0) throw new IOException("empty reply");
            if (!tmp.renameTo(new File(dir, name))) throw new IOException("couldn't replace cache");
        } catch (Exception e) {
            android.util.Log.w("TvHome", "weather: " + name + " failed: " + e);
        } finally {
            if (c != null) c.disconnect();
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    // ---------------------------------------------------------------- views

    private static void setText(TextView view, String text) {
        if (!text.contentEquals(view.getText())) view.setText(text);
    }

    private static void setIcon(ImageView view, int resource) {
        if (Integer.valueOf(resource).equals(view.getTag())) return;
        view.setImageResource(resource);
        view.setTag(resource);
    }

    private int px(float dps) {
        return Math.round(dps * dp);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(margin);
        return lp;
    }

    private LinearLayout.LayoutParams start(int margin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(px(margin));
        return lp;
    }

    private LinearLayout column(LinearLayout row, float weight) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        lp.setMarginEnd(px(8));
        row.addView(col, lp);
        return col;
    }

    private ImageView icon(int res, int tint) {
        ImageView v = new ImageView(ctx);
        v.setImageResource(res);
        v.setImageTintList(ColorStateList.valueOf(tint));
        return v;
    }

    private TextView value() {
        TextView t = new TextView(ctx);
        t.setTextSize(14f);
        t.setTypeface(Fonts.outfit(ctx, 500));
        t.setTextColor(TEXT);
        t.setSingleLine(true);
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView small(String text, int color, float size, float spacing) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLetterSpacing(spacing);
        t.setTypeface(Fonts.outfit(ctx, 500));
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    /**
     * An air reading: "AQI 74" with its band beside it in the band's colour, a detail line, then
     * source · time.
     */
    private final class Gauge {
        final TextView value, band, detail, label;

        Gauge(LinearLayout col) {
            LinearLayout top = new LinearLayout(ctx); // baseline-aligned: the band sits on the number's line
            value = value();
            top.addView(value);
            // Beside the number, as the other small labels are; a long one ("VERY UNHEALTHY") ends in "…".
            band = small("", DIM, 8.5f, 0.06f);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            blp.setMarginStart(px(5));
            top.addView(band, blp);
            col.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            detail = small("", DIM, 8.5f, 0.02f);
            col.addView(detail, top(5));
            label = small("", DIM, 8.5f, 0.06f);
            col.addView(label, top(5));
        }

        void set(String v, String bandName, int bandColor, String d, int detailColor, String l, boolean old) {
            setText(value, v);
            value.setTextColor(old ? DIM : TEXT);
            setText(band, bandName.toUpperCase(Locale.ROOT));
            band.setTextColor(old ? DIM : bandColor);
            setText(detail, d);
            detail.setTextColor(old ? DIM : detailColor);
            setText(label, l);
        }

        void none(String v, String l) {
            setText(value, v);
            value.setTextColor(DIM);
            setText(band, "");
            setText(detail, "");
            setText(label, l);
        }
    }
}
