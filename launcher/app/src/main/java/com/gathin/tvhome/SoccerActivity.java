package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Layar Klasemen &amp; Jadwal bola (FotMob) SwadayaTV.
 * Tampilan programatik, gaya hijau-abu gelap, bisa pakai D-pad remote.
 */
public class SoccerActivity extends Activity {

    // warna Swadaya
    private static final int BG = 0xFF0E1713;
    private static final int CARD = 0xFF15211B;
    private static final int CARD_FOKUS = 0xFF1E2E24;
    private static final int STROKE = 0x3314E8A0;
    private static final int AKSE = 0xFF14E8A0;
    private static final int TEKS = 0xFFFFFFFF;
    private static final int TEKS_REDUP = 0xB3FFFFFF;

    private static final String PREFS = "soccer_prefs";

    private int ligaIndex = 0;
    private int modeTab = 0; // 0 = Klasemen, 1 = Jadwal

    private TextView judulLiga;
    private TextView subJudul;
    private TextView status;
    private TextView[] tabViews;
    private ListView daftar;
    private BarisAdapter adapter;

    private SoccerData.Data data;
    private boolean memuat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bacaPililihan();
        buatTampilan();
        muatLiga(ligaIndex, false);
    }

    private void buatTampilan() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(40), dp(18), dp(40), dp(14));
        root.setFocusableInTouchMode(true);

        // header: judul + season, tombol Segarkan / Pilih Tim / Kembali
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout judulKolom = new LinearLayout(this);
        judulKolom.setOrientation(LinearLayout.VERTICAL);
        judulLiga = buatTeks("⚽ Klasemen & Jadwal", 26, TEKS, true);
        subJudul = buatTeks("FotMob • SwadayaTV", 12, TEKS_REDUP, false);
        judulKolom.addView(judulLiga);
        judulKolom.addView(subJudul);
        header.addView(judulKolom, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView segar = buatTombol("Segarkan");
        segar.setOnClickListener(v -> muatLiga(ligaIndex, true));
        TextView tim = buatTombol("Pilih Tim Favorit");
        tim.setOnClickListener(v -> tampilDialogTim());
        TextView kembali = buatTombol("Kembali");
        kembali.setOnClickListener(v -> finish());
        header.addView(segar);
        header.addView(tim);
        header.addView(kembali);
        root.addView(header);

        // tab liga (scroll horizontal)
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFocusable(false);
        LinearLayout barisTab = new LinearLayout(this);
        barisTab.setOrientation(LinearLayout.HORIZONTAL);
        SoccerData.League[] ligas = SoccerData.getLeagues();
        tabViews = new TextView[ligas.length];
        for (int i = 0; i < ligas.length; i++) {
            final int idx = i;
            TextView tab = buatTeks(ligas[i].name, 13, TEKS_REDUP, false);
            tab.setFocusable(true);
            tab.setPadding(dp(14), dp(8), dp(14), dp(8));
            tab.setOnClickListener(v -> muatLiga(idx, true));
            tabViews[i] = tab;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            barisTab.addView(tab, lp);
        }
        scroll.addView(barisTab);
        root.addView(scroll);

        // tab mode: Klasemen | Jadwal
        LinearLayout barisMode = new LinearLayout(this);
        barisMode.setOrientation(LinearLayout.HORIZONTAL);
        barisMode.setGravity(Gravity.CENTER_VERTICAL);
        TextView m1 = buatTeks("Klasemen", 14, TEKS, true);
        m1.setFocusable(true);
        m1.setPadding(dp(16), dp(8), dp(16), dp(8));
        m1.setOnClickListener(v -> { modeTab = 0; refreshAdapter(); });
        TextView m2 = buatTeks("Jadwal", 14, TEKS_REDUP, false);
        m2.setFocusable(true);
        m2.setPadding(dp(16), dp(8), dp(16), dp(8));
        m2.setOnClickListener(v -> { modeTab = 1; refreshAdapter(); });
        barisMode.addView(m1);
        barisMode.addView(m2);
        LinearLayout.LayoutParams modeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        modeLp.topMargin = dp(8);
        root.addView(barisMode, modeLp);

        status = buatTeks("", 12, TEKS_REDUP, false);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(6);
        root.addView(status, stLp);

        // daftar isi
        daftar = new ListView(this);
        daftar.setBackgroundColor(Color.TRANSPARENT);
        daftar.setDivider(null);
        daftar.setFocusable(true);
        adapter = new BarisAdapter(this);
        daftar.setAdapter(adapter);
        LinearLayout.LayoutParams daftarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        daftarLp.topMargin = dp(6);
        root.addView(daftar, daftarLp);

        TextView kaki = buatTeks("Sumber: FotMob • cache tersimpan otomatis agar terbaca saat luring",
                11, TEKS_REDUP, false);
        LinearLayout.LayoutParams kakiLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kakiLp.topMargin = dp(8);
        root.addView(kaki, kakiLp);

        setContentView(root);
        // fokus awal ke tab liga aktif
        root.post(() -> {
            if (tabViews != null && tabViews[ligaIndex] != null) tabViews[ligaIndex].requestFocus();
        });
    }

    // ---- data ----

    private void muatLiga(int index, boolean paksaSegar) {
        ligaIndex = index;
        tandaiTab();
        SoccerData.League liga = SoccerData.getLeagues()[index];
        judulLiga.setText("⚽ " + liga.name);
        subJudul.setText("FotMob • Musim: - • " + liga.code);
        if (paksaSegar || data == null) {
            tampilStatus("Memuat " + liga.name + " ...");
            if (memuat) return;
            memuat = true;
            SoccerData.downloadAsync(this, liga, (l, d, raw) -> {
                memuat = false;
                if (l.id != SoccerData.getLeagues()[ligaIndex].id) return;
                if (d == null || (d.table.isEmpty() && d.matches.isEmpty())) {
                    // coba cache lama
                    String mentah = SoccerData.loadCached(this, l.id);
                    if (mentah != null) {
                        data = SoccerData.parse(mentah);
                        tampilStatus("Luring, tampil cache " + l.name);
                    } else {
                        data = new SoccerData.Data();
                        tampilStatus("Gagal memuat, periksa internet lalu Segarkan");
                    }
                } else {
                    data = d;
                    tampilStatus(l.name + " • " + data.table.size() + " tim • " + data.matches.size() + " laga");
                }
                if (data != null && !data.season.isEmpty()) {
                    subJudul.setText("FotMob • Musim: " + data.season + " • " + liga.code);
                }
                refreshAdapter();
            });
        } else {
            refreshAdapter();
        }
    }

    private void refreshAdapter() {
        if (adapter != null) adapter.notifyDataSetChanged();
        tandaiMode();
    }

    private void tampilStatus(String s) {
        if (status != null) status.setText(s);
    }

    // ---- tab ----

    private void tandaiTab() {
        for (int i = 0; i < tabViews.length; i++) {
            TextView t = tabViews[i];
            if (t == null) continue;
            boolean aktif = (i == ligaIndex);
            t.setTextColor(aktif ? AKSE : TEKS_REDUP);
            t.setTypeface(null, aktif ? Typeface.BOLD : Typeface.NORMAL);
            t.setBackground(kotak(aktif ? CARD_FOKUS : CARD, STROKE));
        }
    }

    private void tandaiMode() {
        // sederhana: warna teks saja
        tampilStatus(modeTab == 0 ? "Mode: Klasemen" : "Mode: Jadwal");
        // status sudah diisi di muatLiga; jangan timpa kalau baru selesai muat
    }

    // ---- dialog tim favorit ----

    private void tampilDialogTim() {
        final SoccerData.League[] ligas = SoccerData.getLeagues();
        String[] namaLiga = new String[ligas.length];
        for (int i = 0; i < ligas.length; i++) namaLiga[i] = ligas[i].name;
        AlertDialog.Builder b1 = new AlertDialog.Builder(this);
        b1.setTitle("Pilih liga dulu");
        b1.setItems(namaLiga, (dialog, which) -> {
            muatLiga(which, false);
            // ambil daftar tim dari cache/parse atau daftar cadangan 20 tim EPL
            List<String> timList = ambilNamaTim(which);
            String[] arr = timList.toArray(new String[0]);
            AlertDialog.Builder b2 = new AlertDialog.Builder(this);
            b2.setTitle("Tim favorit — " + ligas[which].name);
            b2.setItems(arr, (d2, pilihan) -> {
                SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                sp.edit()
                        .putInt("league_id", ligas[which].id)
                        .putString("fav_team_name", arr[pilihan])
                        .putInt("fav_team_index", pilihan)
                        .apply();
                Toast.makeText(this, "Favorit: " + arr[pilihan], Toast.LENGTH_SHORT).show();
                refreshAdapter();
            });
            b2.setNegativeButton("Batal", null);
            b2.show();
        });
        b1.setNegativeButton("Batal", null);
        b1.show();
    }

    private List<String> ambilNamaTim(int ligaIndex) {
        List<String> keluar = new ArrayList<>();
        SoccerData.League liga = SoccerData.getLeagues()[ligaIndex];
        String mentah = SoccerData.loadCached(this, liga.id);
        if (mentah != null) {
            SoccerData.Data d = SoccerData.parse(mentah);
            for (SoccerData.Row r : d.table) keluar.add(r.name);
        }
        if (keluar.isEmpty() && data != null && ligaIndex == this.ligaIndex) {
            for (SoccerData.Row r : data.table) keluar.add(r.name);
        }
        if (keluar.isEmpty()) {
            // cadangan statis 20 tim EPL
            String[] epl = {"Arsenal", "Manchester City", "Liverpool", "Chelsea", "Tottenham",
                    "Newcastle", "Manchester United", "West Ham", "Aston Villa", "Brighton",
                    "Brentford", "Crystal Palace", "Everton", "Fulham", "Wolves",
                    "Bournemouth", "Nottingham Forest", "Luton", "Burnley", "Sheffield Utd"};
            for (String s : epl) keluar.add(s);
        }
        return keluar;
    }

    private void bacaPililihan() {
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int leagueId = sp.getInt("league_id", 47);
        for (int i = 0; i < SoccerData.getLeagues().length; i++) {
            if (SoccerData.getLeagues()[i].id == leagueId) { ligaIndex = i; break; }
        }
    }

    private String timFavorit() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("fav_team_name", null);
    }

    // ---- adapter ----

    private final class BarisAdapter extends BaseAdapter {
        private final Context ctx;

        BarisAdapter(Context ctx) { this.ctx = ctx; }

        private int jumlah() {
            if (data == null) return 0;
            if (modeTab == 0) return data.table.size();
            return data.matches.size();
        }

        @Override public int getCount() { return jumlah(); }
        @Override public Object getItem(int pos) { return null; }
        @Override public long getItemId(int pos) { return pos; }

        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            LinearLayout baris = new LinearLayout(ctx);
            baris.setOrientation(LinearLayout.HORIZONTAL);
            baris.setGravity(Gravity.CENTER_VERTICAL);
            baris.setPadding(dp(12), dp(8), dp(12), dp(8));
            baris.setBackground(kotak(CARD, STROKE));

            String fav = timFavorit();
            if (modeTab == 0) {
                SoccerData.Row r = data.table.get(pos);
                TextView no = buatTeks(String.valueOf(r.rank), 14, TEKS, true);
                no.setGravity(Gravity.CENTER);
                no.setMinWidth(dp(36));
                TextView nama = buatTeks(r.name, 14, TEKS, false);
                nama.setEllipsize(TextUtils.TruncateAt.END);
                nama.setSingleLine(true);
                TextView info = buatTeks(r.played + " • " + r.points + " poin", 12, TEKS_REDUP, false);
                info.setGravity(Gravity.RIGHT);
                if (fav != null && fav.equalsIgnoreCase(r.name)) {
                    nama.setTextColor(AKSE);
                    nama.setTypeface(null, Typeface.BOLD);
                }
                baris.addView(no);
                baris.addView(nama, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                baris.addView(info);
            } else {
                SoccerData.Match m = data.matches.get(pos);
                String skor = m.scoreStr != null && !m.scoreStr.isEmpty() ? m.scoreStr : "-";
                TextView kiri = buatTeks(m.home, 13, TEKS, false);
                kiri.setEllipsize(TextUtils.TruncateAt.END);
                kiri.setSingleLine(true);
                TextView tengah = buatTeks("  " + skor + "  ", 13, AKSE, true);
                tengah.setGravity(Gravity.CENTER);
                TextView kanan = buatTeks(m.away, 13, TEKS, false);
                kanan.setEllipsize(TextUtils.TruncateAt.END);
                kanan.setSingleLine(true);
                kanan.setGravity(Gravity.RIGHT);
                TextView waktu = buatTeks(SoccerData.formatWaktu(m.utcTime), 11, TEKS_REDUP, false);
                waktu.setGravity(Gravity.CENTER);
                if (fav != null && (fav.equalsIgnoreCase(m.home) || fav.equalsIgnoreCase(m.away))) {
                    kiri.setTextColor(AKSE);
                    kanan.setTextColor(AKSE);
                }
                baris.addView(kiri, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                baris.addView(tengah);
                baris.addView(kanan, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                baris.addView(waktu);
            }
            // margin antar baris
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(4);
            baris.setLayoutParams(lp);
            return baris;
        }
    }

    // ---- pembantu tampilan ----

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
        t.setBackground(kotak(CARD_FOKUS, STROKE));
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
