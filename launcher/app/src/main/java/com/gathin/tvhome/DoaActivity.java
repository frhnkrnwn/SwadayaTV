package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Kumpulan doa harian: daftar doa dengan teks Arab dan terjemahan Indonesia.
 * Sumber quran-json (online), dengan 15 doa cadangan yang selalu tersedia offline.
 */
public class DoaActivity extends Activity {

    private static final int BG = 0xFF0E1713;
    private static final int CARD = 0xFF15211B;
    private static final int CARD_FOKUS = 0xFF1E2E24;
    private static final int STROKE = 0x3314E8A0;
    private static final int AKSE = 0xFF14E8A0;
    private static final int TEKS = 0xFFFFFFFF;
    private static final int TEKS_REDUP = 0xB3FFFFFF;

    private final List<DailyDoa.Entry> data = new ArrayList<>();
    private DoaAdapter adapter;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buatTampilan();
        isiData();
        // Ambil versi terbaru dari server di latar belakang, lalu muat ulang bila berhasil.
        new Thread(() -> {
            DailyDoa.Segarkan(this);
            runOnUiThread(this::isiData);
        }).start();
    }

    private void buatTampilan() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(40), dp(18), dp(40), dp(14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView judul = buatTeks("🤲 Doa Harian", 26, TEKS, true);
        header.addView(judul, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView kembali = buatTombol("Kembali");
        kembali.setOnClickListener(v -> finish());
        header.addView(kembali);
        root.addView(header);

        status = buatTeks("Arab + terjemahan Indonesia • bisa dibaca offline", 12, TEKS_REDUP, false);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(8);
        root.addView(status, stLp);

        ListView daftar = new ListView(this);
        daftar.setBackgroundColor(Color.TRANSPARENT);
        daftar.setDivider(null);
        daftar.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        adapter = new DoaAdapter();
        daftar.setAdapter(adapter);
        LinearLayout.LayoutParams daftarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        daftarLp.topMargin = dp(8);
        root.addView(daftar, daftarLp);

        setContentView(root);
    }

    private void isiData() {
        data.clear();
        data.addAll(DailyDoa.ambilDaftar(this));
        if (adapter != null) adapter.notifyDataSetChanged();
        if (status != null) {
            status.setText("Arab + terjemahan Indonesia • " + data.size() + " doa • bisa dibaca offline");
        }
    }

    private final class DoaAdapter extends BaseAdapter {
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int pos) { return data.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            DailyDoa.Entry e = data.get(pos);

            LinearLayout kartu = new LinearLayout(DoaActivity.this);
            kartu.setOrientation(LinearLayout.VERTICAL);
            kartu.setFocusable(true);
            kartu.setFocusableInTouchMode(false);
            kartu.setPadding(dp(18), dp(12), dp(18), dp(14));
            kartu.setBackground(kotak());

            TextView judul = buatTeks(e.title, 13, AKSE, true);
            judul.setSingleLine(true);
            kartu.addView(judul);

            TextView arab = buatTeks(e.arabic, 21, 0xFFFFC56B, false);
            arab.setGravity(Gravity.RIGHT);
            arab.setTextDirection(View.TEXT_DIRECTION_RTL);
            arab.setLineSpacing(dp(2), 1f);
            try {
                arab.setTypeface(getResources().getFont(R.font.amiri_quran));
            } catch (Exception ignore) {
                // pakai font bawaan bila font Arab tidak tersedia
            }
            LinearLayout.LayoutParams arabLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            arabLp.topMargin = dp(6);
            kartu.addView(arab, arabLp);

            TextView arti = buatTeks(e.translation, 12, TEKS_REDUP, false);
            arti.setLineSpacing(dp(1), 1f);
            LinearLayout.LayoutParams artiLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            artiLp.topMargin = dp(4);
            kartu.addView(arti, artiLp);

            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            kartu.setLayoutParams(lp);
            return kartu;
        }
    }

    private TextView buatTeks(String s, int sp, int warna, boolean tebal) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(warna);
        t.setTypeface(null, tebal ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private TextView buatTombol(String s) {
        TextView t = buatTeks(s, 12, TEKS, true);
        t.setFocusable(true);
        t.setPadding(dp(14), dp(8), dp(14), dp(8));
        t.setGravity(Gravity.CENTER);
        t.setBackground(kotak());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    private StateListDrawable kotak() {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(CARD);
        normal.setCornerRadius(dp(12));
        normal.setStroke(dp(1), STROKE);
        GradientDrawable fokus = new GradientDrawable();
        fokus.setColor(CARD_FOKUS);
        fokus.setCornerRadius(dp(12));
        fokus.setStroke(dp(2), AKSE);
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused}, fokus);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
