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
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kurs koin (Frankfurter) — USD/SGD/EUR/JPY ke IDR, tanpa API key.
 */
public class KoinActivity extends Activity {

    private static final int BG = 0xFF0E1713;
    private static final int CARD = 0xFF15211B;
    private static final int CARD_FOKUS = 0xFF1E2E24;
    private static final int STROKE = 0x3314E8A0;
    private static final int AKSE = 0xFF14E8A0;
    private static final int TEKS = 0xFFFFFFFF;
    private static final int TEKS_REDUP = 0xB3FFFFFF;

    private TextView status;
    private ListView daftar;
    private KoinAdapter adapter;
    private final List<Koin> data = new ArrayList<>();

    static final class Koin {
        final String kode;
        final String nama;
        double nilai;
        Koin(String kode, String nama) { this.kode = kode; this.nama = nama; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buatTampilan();
        muat();
    }

    private void buatTampilan() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(40), dp(18), dp(40), dp(14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView judul = buatTeks("💱 Kurs Koin → IDR", 26, TEKS, true);
        header.addView(judul, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView segar = buatTombol("Segarkan");
        segar.setOnClickListener(v -> muat());
        TextView kembali = buatTombol("Kembali");
        kembali.setOnClickListener(v -> finish());
        header.addView(segar);
        header.addView(kembali);
        root.addView(header);

        status = buatTeks("Memuat kurs...", 12, TEKS_REDUP, false);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(8);
        root.addView(status, stLp);

        daftar = new ListView(this);
        daftar.setBackgroundColor(Color.TRANSPARENT);
        daftar.setDivider(null);
        adapter = new KoinAdapter();
        daftar.setAdapter(adapter);
        LinearLayout.LayoutParams daftarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        daftarLp.topMargin = dp(8);
        root.addView(daftar, daftarLp);

        TextView kaki = buatTeks("Sumber: Frankfurter • tanpa API key, cache otomatis",
                11, TEKS_REDUP, false);
        LinearLayout.LayoutParams kakiLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kakiLp.topMargin = dp(8);
        root.addView(kaki, kakiLp);

        setContentView(root);
        daftar.post(this::fokusAwal);
    }

    /** Fokus awal ke kartu pertama supaya remote langsung bisa dipakai. */
    private void fokusAwal() {
        if (daftar == null) return;
        if (daftar.getChildCount() > 0) daftar.getChildAt(0).requestFocus();
        else daftar.requestFocus();
    }

    private void muat() {
        status.setText("Memuat kurs dari Frankfurter...");
        new Thread(() -> {
            boolean ok = fetch();
            runOnUiThread(() -> {
                if (ok) {
                    status.setText("Kurs terbaru • " + data.size() + " mata uang");
                    adapter.notifyDataSetChanged();
                    daftar.post(this::fokusAwal);
                } else {
                    status.setText("Gagal memuat, cek internet lalu Segarkan");
                }
            });
        }).start();
    }

    private boolean fetch() {
        data.clear();
        String[] kode = {"USD", "SGD", "EUR", "JPY", "GBP", "AUD", "CNY", "KRW"};
        String[] nama = {"Dolar AS", "Dolar Singapura", "Euro", "Yen Jepang", "Pound Inggris", "Dolar Australia", "Yuan Tiongkok", "Won Korea"};
        try {
            URL url = new URL("https://api.frankfurter.app/latest?from=IDR&to=" + String.join(",", kode));
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONObject rates = root.getJSONObject("rates");
            for (int i = 0; i < kode.length; i++) {
                Koin k = new Koin(kode[i], nama[i]);
                // rates are X per 1 IDR, so invert to IDR per 1 X
                double r = rates.optDouble(kode[i], 0);
                k.nilai = r > 0 ? 1.0 / r : 0;
                data.add(k);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private final class KoinAdapter extends BaseAdapter {
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int pos) { return data.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            Koin k = data.get(pos);
            LinearLayout baris = new LinearLayout(KoinActivity.this);
            baris.setOrientation(LinearLayout.HORIZONTAL);
            baris.setGravity(Gravity.CENTER_VERTICAL);
            baris.setPadding(dp(12), dp(10), dp(12), dp(10));
            baris.setBackground(kotak(CARD, STROKE));
            TextView kode = buatTeks(k.kode, 16, AKSE, true);
            kode.setMinWidth(dp(70));
            TextView nama = buatTeks(k.nama, 13, TEKS_REDUP, false);
            nama.setSingleLine(true);
            TextView nilai = buatTeks(format(k.nilai), 16, TEKS, true);
            nilai.setGravity(Gravity.RIGHT);
            baris.addView(kode);
            baris.addView(nama, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            baris.addView(nilai);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(4);
            baris.setLayoutParams(lp);
            return baris;
        }
    }

    private String format(double v) {
        if (v == 0) return "-";
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        nf.setMaximumFractionDigits(2);
        return "Rp " + nf.format(v);
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

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
