package com.swadaya.tv;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Minimal soccer card: shows the current standings for a chosen league.
 * Reuses the backend logic from the old SwadayaTV project (SoccerData / SoccerParse).
 */
final class SoccerPanel {
    private final Context ctx;
    private final TextView body;
    private final SoccerData.League league;

    SoccerPanel(Context ctx, LinearLayout root) {
        this.ctx = ctx;
        this.league = SoccerData.LEAGUES[0]; // Premier League by default

        TextView title = new TextView(ctx);
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
                body.setText(r.error.isEmpty() ? "No data" : r.error);
                return;
            }
            StringBuilder sb = new StringBuilder();
            int n = Math.min(5, r.data.table.size());
            for (int i = 0; i < n; i++) {
                SoccerParse.Row row = r.data.table.get(i);
                sb.append(row.rank).append(". ").append(row.name)
                        .append("  ").append(row.points).append(" pts\n");
            }
            body.setText(sb.toString().trim());
        });
    }
}
