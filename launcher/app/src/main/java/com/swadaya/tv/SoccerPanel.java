package com.swadaya.tv;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Minimal soccer card: shows the current standings for a chosen league, with the favorite
 * team (from Settings) highlighted and its next match shown.
 */
final class SoccerPanel {
    private final Context ctx;
    private final LinearLayout root;
    private TextView body;
    private SoccerData.League league;

    SoccerPanel(Context ctx, LinearLayout root) {
        this.ctx = ctx;
        this.root = root;
        load();
    }

    void load() {
        root.removeAllViews();

        TextView title = new TextView(ctx);
        int id = LauncherSettings.choice(ctx, LauncherSettings.SOCCER_LEAGUE);
        league = SoccerData.LEAGUES[0];
        for (SoccerData.League l : SoccerData.LEAGUES) if (l.id == id) { league = l; break; }
        title.setText("Soccer \u00b7 " + league.name);
        title.setTextSize(12);
        title.setTextColor(0x8CFFFFFF);
        title.setPadding(0, 0, 0, 6);
        root.addView(title);

        body = new TextView(ctx);
        body.setTextSize(13);
        body.setTextColor(0xFFFFFFFF);
        body.setGravity(Gravity.START);
        body.setText("Loading\u2026");
        root.addView(body);

        SoccerData.fetchAsync((android.app.Activity) ctx, league, (l, r) -> {
            if (r.data == null || r.data.table.isEmpty()) {
                body.setText(r.error == null || r.error.isEmpty() ? "No data" : r.error);
                return;
            }
            String fav = LauncherSettings.soccerTeam(ctx);
            boolean hasFav = !fav.isEmpty();
            StringBuilder sb = new StringBuilder();
            if (hasFav) sb.append("\u2605 ").append(fav).append("\n");
            int n = Math.min(5, r.data.table.size());
            for (int i = 0; i < n; i++) {
                SoccerParse.Row row = r.data.table.get(i);
                boolean isFav = hasFav && row.name.toLowerCase(Locale.ROOT)
                        .contains(fav.toLowerCase(Locale.ROOT));
                sb.append(isFav ? "\u2192 " : "   ").append(row.rank).append(". ").append(row.name)
                        .append("  ").append(row.points).append(" pts").append(isFav ? "  \u2605" : "").append("\n");
            }
            if (hasFav) {
                List<SoccerParse.Match> upcoming = SoccerParse.mendatang(r.data);
                for (SoccerParse.Match m : upcoming) {
                    if (m.home.toLowerCase(Locale.ROOT).contains(fav.toLowerCase(Locale.ROOT))
                            || m.away.toLowerCase(Locale.ROOT).contains(fav.toLowerCase(Locale.ROOT))) {
                        sb.append("\nNext: ").append(m.home).append(" vs ").append(m.away);
                        if (m.utcTime > 0) sb.append(" (").append(SoccerData.jam(m.utcTime)).append(")");
                        break;
                    }
                }
            }
            if (r.error != null && !r.error.isEmpty()) {
                sb.append("\n").append(r.error);
                if (r.data.fetchedAt > 0) {
                    sb.append(" \u00b7 ").append(SoccerData.jamPerbarui(r.data.fetchedAt));
                }
            }
            body.setText(sb.toString().trim());
        });
    }
}
