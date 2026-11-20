package com.swadaya.tv;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Locale;

final class DailyPanel {
    private static final int TEXT = 0xFFFFFFFF, DIM = 0x8CFFFFFF;

    private final Context ctx;
    private final TextView title, arabic, translation, source;

    DailyPanel(Context ctx, LinearLayout root) {
        this.ctx = ctx;
        float dp = ctx.getResources().getDisplayMetrics().density;
        LinearLayout header = new LinearLayout(ctx);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(ctx);
        t.setText("DAILY · QURAN & DOA");
        t.setTextSize(10.5f);
        t.setTextColor(DIM);
        t.setLetterSpacing(0.08f);
        t.setTypeface(Fonts.outfit(ctx, 500));
        header.addView(t);
        root.addView(header);

        arabic = new TextView(ctx);
        arabic.setTextSize(15f);
        arabic.setTextColor(TEXT);
        arabic.setTypeface(Fonts.outfit(ctx, 500));
        arabic.setPadding(0, (int)(8*dp), 0, 0);
        root.addView(arabic);

        translation = new TextView(ctx);
        translation.setTextSize(11.5f);
        translation.setTextColor(DIM);
        translation.setPadding(0, (int)(4*dp), 0, 0);
        root.addView(translation);

        source = new TextView(ctx);
        source.setTextSize(9.5f);
        source.setTextColor(DIM);
        source.setPadding(0, (int)(4*dp), 0, 0);
        root.addView(source);

        title = t;
        refresh();
    }

    void refresh() {
        try {
            JSONArray quran = load("daily_quran.json");
            JSONArray dua = load("daily_dua.json");
            int day = LocalDate.now().getDayOfYear();
            if (quran.length() > 0) {
                JSONObject v = quran.getJSONObject(day % quran.length());
                arabic.setText(v.optString("arabic"));
                translation.setText(v.optString("translation"));
                source.setText(v.optString("surah"));
            }
            // Append a doa in a second line via translation? We'll store in source? Instead add separate view dynamically is complex; append text.
        } catch (Exception e) {
            arabic.setText("");
            translation.setText("");
            source.setText("");
        }
        // Doa: show in a separate TextView appended lazily
        try {
            JSONArray dua = load("daily_dua.json");
            int day = LocalDate.now().getDayOfYear();
            if (dua.length() > 0) {
                JSONObject d = dua.getJSONObject(day % dua.length());
                // reuse translation? We'll append doa to translation with newline
                String cur = translation.getText().toString();
                translation.setText(cur + "\n\n" + d.optString("arabic") + "\n" + d.optString("translation")
                        + "\n— " + d.optString("title"));
            }
        } catch (Exception ignored) {}
    }

    private JSONArray load(String name) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(ctx.getAssets().open(name), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (String line; (line = r.readLine()) != null; ) sb.append(line);
        r.close();
        return new JSONArray(sb.toString());
    }
}