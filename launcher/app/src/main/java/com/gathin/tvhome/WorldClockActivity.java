package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Jam dunia: beberapa zona waktu sekaligus.
 */
public class WorldClockActivity extends Activity {

    private static final int BG = 0xFF0E1713;
    private static final int CARD = 0xFF15211B;
    private static final int CARD_FOKUS = 0xFF1E2E24;
    private static final int STROKE = 0x3314E8A0;
    private static final int AKSE = 0xFF14E8A0;
    private static final int TEKS = 0xFFFFFFFF;
    private static final int TEKS_REDUP = 0xB3FFFFFF;

    private ListView daftar;
    private ClockAdapter adapter;
    private final List<Zona> data = new ArrayList<>();
    private TextView status;

    static final class Zona {
        final String kota;
        final String zona;
        Zona(String kota, String zona) { this.kota = kota; this.zona = zona; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buatTampilan();
        isiData();
        adapter.notifyDataSetChanged();
        daftar.post(this::fokusAwal);
        // refresh tiap 30 detik
        final android.os.Handler h = new android.os.Handler();
        Runnable r = new Runnable() {
            public void run() {
                if (adapter != null) adapter.notifyDataSetChanged();
                h.postDelayed(this, 30000);
            }
        };
        h.postDelayed(r, 30000);
    }

    private void buatTampilan() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(40), dp(18), dp(40), dp(14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView judul = buatTeks("🌍 Jam Dunia", 26, TEKS, true);
        header.addView(judul, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView kembali = buatTombol("Kembali");
        kembali.setOnClickListener(v -> finish());
        header.addView(kembali);
        root.addView(header);

        status = buatTeks("Waktu lokal & beberapa kota besar", 12, TEKS_REDUP, false);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(8);
        root.addView(status, stLp);

        daftar = new ListView(this);
        daftar.setBackgroundColor(Color.TRANSPARENT);
        daftar.setDivider(null);
        adapter = new ClockAdapter();
        daftar.setAdapter(adapter);
        LinearLayout.LayoutParams daftarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        daftarLp.topMargin = dp(8);
        root.addView(daftar, daftarLp);

        setContentView(root);
    }

    private void isiData() {
        data.clear();
        data.add(new Zona("Jakarta", "Asia/Jakarta"));
        data.add(new Zona("Mekkah", "Asia/Riyadh"));
        data.add(new Zona("Tokyo", "Asia/Tokyo"));
        data.add(new Zona("London", "Europe/London"));
        data.add(new Zona("New York", "America/New_York"));
        data.add(new Zona("Sydney", "Australia/Sydney"));
        data.add(new Zona("Dubai", "Asia/Dubai"));
        data.add(new Zona("Istanbul", "Europe/Istanbul"));
    }

    private final class ClockAdapter extends BaseAdapter {
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int pos) { return data.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            Zona z = data.get(pos);
            TimeZone tz = TimeZone.getTimeZone(z.zona);
            SimpleDateFormat waktu = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            waktu.setTimeZone(tz);
            SimpleDateFormat tgl = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
            tgl.setTimeZone(tz);
            String now = waktu.format(new java.util.Date());
            String tanggal = tgl.format(new java.util.Date());

            LinearLayout baris = new LinearLayout(WorldClockActivity.this);
            baris.setOrientation(LinearLayout.HORIZONTAL);
            baris.setGravity(Gravity.CENTER_VERTICAL);
            baris.setPadding(dp(12), dp(12), dp(12), dp(12));
            baris.setBackground(kotak(CARD, STROKE));

            TextView kota = buatTeks(z.kota, 18, TEKS, true);
            kota.setMinWidth(dp(140));
            TextView zona = buatTeks(z.zona, 11, TEKS_REDUP, false);
            zona.setSingleLine(true);
            LinearLayout kiri = new LinearLayout(WorldClockActivity.this);
            kiri.setOrientation(LinearLayout.VERTICAL);
            kiri.addView(kota);
            kiri.addView(zona);

            TextView jam = buatTeks(now, 24, AKSE, true);
            jam.setGravity(Gravity.RIGHT);
            TextView tglv = buatTeks(tanggal, 11, TEKS_REDUP, false);
            tglv.setGravity(Gravity.RIGHT);
            LinearLayout kanan = new LinearLayout(WorldClockActivity.this);
            kanan.setOrientation(LinearLayout.VERTICAL);
            kanan.setGravity(Gravity.RIGHT);
            kanan.addView(jam);
            kanan.addView(tglv);

            baris.addView(kiri, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            baris.addView(kanan);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(4);
            baris.setLayoutParams(lp);
            return baris;
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
        t.setBackground(kotak(CARD, STROKE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    private StateListDrawable kotak(int warna, int border) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(warna);
        normal.setCornerRadius(dp(10));
        normal.setStroke(dp(1), border);
        GradientDrawable fokus = new GradientDrawable();
        fokus.setColor(CARD_FOKUS);
        fokus.setCornerRadius(dp(10));
        fokus.setStroke(dp(1), AKSE);
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused}, fokus);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    /** Fokus awal ke kartu pertama supaya remote langsung bisa dipakai. */
    private void fokusAwal() {
        if (daftar == null) return;
        if (daftar.getChildCount() > 0) daftar.getChildAt(0).requestFocus();
        else daftar.requestFocus();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
