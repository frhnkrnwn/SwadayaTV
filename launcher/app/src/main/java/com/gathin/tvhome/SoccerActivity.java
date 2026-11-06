package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Layar Klasemen, Hasil, dan Jadwal bola SwadayaTV.
 * Tampilan programatik bergaya hijau-abu gelap, bisa dipakai dengan D-pad remote.
 * Data: ESPN (utama) dengan FotMob sebagai cadangan, lihat {@link SoccerData}.
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
    private static final int LIVE = 0xFFFF5252;
    private static final int TUNDA = 0xFFFFB74D;

    private static final String PREFS = "soccer_prefs";
    private static final String[] NAMA_MODE = {"Klasemen", "Hasil", "Mendatang", "Favorit"};
    private static final long UMUR_CACHE_SEGAR_MS = 2 * 60 * 1000L;
    private static final long SEGARKAN_LIVE_MS = 60 * 1000L;
    private static final long SEGARKAN_BIASA_MS = 5 * 60 * 1000L;

    private static final int MODE_KLASEMEN = 0;
    private static final int MODE_HASIL = 1;
    private static final int MODE_MENDATANG = 2;
    private static final int MODE_FAVORIT = 3;

    /** Baris pesan kosong/memuat di dalam daftar. */
    private static final class Info {
        final String teks;
        Info(String teks) { this.teks = teks; }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable segarkanOtomatis = new Runnable() {
        @Override public void run() {
            ambilBaru(true, true);
        }
    };

    private int ligaIndex = 0;
    private int modeTab = MODE_KLASEMEN;
    private int permintaan = 0; // penanda unik tiap unduhan, supaya hasil basi diabaikan
    private boolean memuat = false;

    private TextView judulLiga;
    private TextView subJudul;
    private TextView status;
    private TextView[] tabViews;
    private TextView[] modeViews;
    private LinearLayout headerTabel;
    private ListView daftar;
    private BarisAdapter adapter;

    private SoccerParse.Data data;
    private final List<Object> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bacaPilihan();
        buatTampilan();
        pilihLiga(ligaIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // kembali dari layar lain: segarkan bila data sudah basi
        if (data != null && !memuat) ambilBaru(false, true);
        jadwalkanSegarkan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(segarkanOtomatis);
    }

    // ---- tampilan ----

    private void buatTampilan() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(40), dp(18), dp(40), dp(14));
        root.setFocusableInTouchMode(true);

        // header: judul + musim, tombol Segarkan / Pilih Tim / Kembali
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout judulKolom = new LinearLayout(this);
        judulKolom.setOrientation(LinearLayout.VERTICAL);
        judulLiga = buatTeks("⚽ Klasemen & Jadwal", 26, TEKS, true);
        subJudul = buatTeks("SwadayaTV", 12, TEKS_REDUP, false);
        judulKolom.addView(judulLiga);
        judulKolom.addView(subJudul);
        header.addView(judulKolom, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView segar = buatTombol("Segarkan");
        segar.setOnClickListener(v -> ambilBaru(true, false));
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
            tab.setOnClickListener(v -> pilihLiga(idx));
            tabViews[i] = tab;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            barisTab.addView(tab, lp);
        }
        scroll.addView(barisTab);
        root.addView(scroll);

        // tab mode: Klasemen | Hasil | Mendatang | Favorit
        LinearLayout barisMode = new LinearLayout(this);
        barisMode.setOrientation(LinearLayout.HORIZONTAL);
        barisMode.setGravity(Gravity.CENTER_VERTICAL);
        modeViews = new TextView[NAMA_MODE.length];
        for (int i = 0; i < NAMA_MODE.length; i++) {
            final int idx = i;
            TextView m = buatTeks(NAMA_MODE[i], 14, TEKS_REDUP, false);
            m.setFocusable(true);
            m.setPadding(dp(16), dp(8), dp(16), dp(8));
            m.setOnClickListener(v -> { modeTab = idx; terapkanMode(); });
            modeViews[i] = m;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            barisMode.addView(m, lp);
        }
        LinearLayout.LayoutParams modeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        modeLp.topMargin = dp(8);
        root.addView(barisMode, modeLp);

        status = buatTeks("", 12, TEKS_REDUP, false);
        status.setMaxLines(2);
        status.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(6);
        root.addView(status, stLp);

        // judul kolom klasemen (hanya tampil di tab Klasemen)
        headerTabel = buatBarisTabel(true, null);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hLp.topMargin = dp(6);
        root.addView(headerTabel, hLp);

        // daftar isi
        daftar = new ListView(this);
        daftar.setBackgroundColor(Color.TRANSPARENT);
        daftar.setDivider(null);
        daftar.setFocusable(true);
        daftar.setSelector(new ColorDrawable(0x2214E8A0));
        daftar.setDrawSelectorOnTop(true);
        adapter = new BarisAdapter(this);
        daftar.setAdapter(adapter);
        LinearLayout.LayoutParams daftarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        daftarLp.topMargin = dp(6);
        root.addView(daftar, daftarLp);

        TextView kaki = buatTeks("Sumber: ESPN, cadangan FotMob • diperbarui otomatis (tiap 1 menit saat ada laga berlangsung) • cache tersimpan untuk mode luring",
                11, TEKS_REDUP, false);
        LinearLayout.LayoutParams kakiLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kakiLp.topMargin = dp(8);
        root.addView(kaki, kakiLp);

        setContentView(root);
        tandaiMode();
        // fokus awal ke tab liga aktif
        root.post(() -> {
            if (tabViews != null && tabViews[ligaIndex] != null) tabViews[ligaIndex].requestFocus();
        });
    }

    // ---- data ----

    private SoccerData.League ligaAktif() {
        return SoccerData.getLeagues()[ligaIndex];
    }

    /** Ganti liga: tampilkan cache dulu (bila ada), lalu unduh data terbaru. */
    private void pilihLiga(int index) {
        ligaIndex = index;
        SoccerData.League liga = ligaAktif();
        tandaiTab();
        judulLiga.setText("⚽ " + liga.name);
        subJudul.setText(liga.code + " • SwadayaTV");
        permintaan++;               // batalkan hasil unduhan liga sebelumnya
        data = SoccerData.loadCached(this, liga);
        memuat = false;
        if (data != null) tampilStatus(ringkasan(liga, true));
        terapkanData();
        ambilBaru(false, false);
    }

    /** Teks status ringkas dari data yang sedang tampil. */
    private String ringkasan(SoccerData.League liga, boolean dariCache) {
        String info = liga.name + " • " + data.source + (dariCache ? " (tersimpan) " : " • ")
                + "diperbarui " + SoccerData.jamPerbarui(data.fetchedAt);
        if (data.hasLive()) info += " • 🔴 ada laga berlangsung";
        if (data.source.contains("FotMob")) info += " • memakai sumber cadangan";
        return info;
    }

    /**
     * Unduh data liga aktif.
     * @param paksa abaikan umur cache dan selalu unduh
     * @param diam  jangan ubah teks status saat mulai (untuk penyegaran otomatis)
     */
    private void ambilBaru(boolean paksa, boolean diam) {
        final SoccerData.League liga = ligaAktif();
        if (!paksa && data != null
                && System.currentTimeMillis() - data.fetchedAt < UMUR_CACHE_SEGAR_MS) {
            jadwalkanSegarkan();
            return;
        }
        final int token = ++permintaan;
        memuat = true;
        if (!diam) {
            tampilStatus("Memuat " + liga.name + " ...");
            susunDaftar();
            adapter.notifyDataSetChanged();
        }
        SoccerData.fetchAsync(this, liga, (l, hasil) -> {
            if (token != permintaan) return; // sudah ada permintaan yang lebih baru
            memuat = false;
            if (hasil.data != null) {
                data = hasil.data;
                SoccerData.saveCache(this, l, hasil.data);
                tampilStatus(ringkasan(l, false));
                String musim = data.season;
                subJudul.setText(l.code + (musim.isEmpty() ? "" : " • Musim " + musim) + " • " + data.source);
            } else if (data != null) {
                tampilStatus("Gagal memperbarui (" + hasil.error + ") • menampilkan data tersimpan "
                        + SoccerData.jamPerbarui(data.fetchedAt));
            } else {
                tampilStatus("Gagal memuat: " + hasil.error + " • periksa internet lalu tekan Segarkan");
            }
            terapkanData();
            jadwalkanSegarkan();
        });
    }

    private void jadwalkanSegarkan() {
        handler.removeCallbacks(segarkanOtomatis);
        long tunda = data != null && data.hasLive() ? SEGARKAN_LIVE_MS : SEGARKAN_BIASA_MS;
        handler.postDelayed(segarkanOtomatis, tunda);
    }

    private void tampilStatus(String s) {
        if (status != null) status.setText(s);
    }

    // ---- daftar ----

    private void terapkanData() {
        susunDaftar();
        adapter.notifyDataSetChanged();
        tandaiMode();
    }

    private void terapkanMode() {
        susunDaftar();
        adapter.notifyDataSetChanged();
        daftar.setSelection(0);
        tandaiMode();
    }

    private String favorit() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("fav_team_name", null);
    }

    private void susunDaftar() {
        items.clear();
        if (data == null) {
            items.add(new Info(memuat ? "Memuat data ..." : "Belum ada data. Tekan Segarkan."));
            return;
        }
        if (modeTab == MODE_KLASEMEN) {
            String grup = null;
            for (SoccerParse.Row r : data.table) {
                if (grup == null || !grup.equals(r.group)) {
                    if (!r.group.isEmpty()) items.add(r.group);
                    grup = r.group;
                }
                items.add(r);
            }
            if (items.isEmpty()) items.add(new Info("Klasemen belum tersedia untuk liga ini."));
            return;
        }
        List<SoccerParse.Match> daftarLaga;
        String kosong;
        if (modeTab == MODE_HASIL) {
            daftarLaga = SoccerParse.hasil(data);
            kosong = "Belum ada hasil pertandingan dalam 3 minggu terakhir.";
        } else if (modeTab == MODE_MENDATANG) {
            daftarLaga = SoccerParse.mendatang(data);
            kosong = "Belum ada jadwal dalam 3 minggu ke depan.";
        } else {
            String fav = favorit();
            daftarLaga = SoccerParse.laga(data, fav);
            kosong = fav == null
                    ? "Pilih tim favorit dulu lewat tombol \"Pilih Tim Favorit\"."
                    : "Tidak ada laga " + fav + " di liga ini dalam rentang data.";
        }
        String hariIni = SoccerData.kunciHari(System.currentTimeMillis());
        String kunciTerakhir = null;
        for (SoccerParse.Match m : daftarLaga) {
            String kunci = m.state == SoccerParse.ST_LIVE ? "LIVE" : SoccerData.kunciHari(m.utcTime);
            if (!kunci.equals(kunciTerakhir)) {
                if (kunci.equals("LIVE")) {
                    items.add("🔴 Sedang berlangsung");
                } else {
                    String judul = SoccerData.tanggalSingkat(m.utcTime);
                    if (kunci.equals(hariIni)) judul += " • Hari ini";
                    items.add(judul);
                }
                kunciTerakhir = kunci;
            }
            items.add(m);
        }
        if (daftarLaga.isEmpty()) items.add(new Info(kosong));
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
        if (modeViews == null) return;
        for (int i = 0; i < modeViews.length; i++) {
            TextView t = modeViews[i];
            boolean aktif = (i == modeTab);
            t.setTextColor(aktif ? AKSE : TEKS_REDUP);
            t.setTypeface(null, aktif ? Typeface.BOLD : Typeface.NORMAL);
            t.setBackground(kotak(aktif ? CARD_FOKUS : CARD, STROKE));
        }
        if (headerTabel != null) {
            headerTabel.setVisibility(modeTab == MODE_KLASEMEN ? View.VISIBLE : View.GONE);
        }
    }

    // ---- dialog tim favorit ----

    private void tampilDialogTim() {
        final SoccerData.League[] ligas = SoccerData.getLeagues();
        String[] namaLiga = new String[ligas.length];
        for (int i = 0; i < ligas.length; i++) namaLiga[i] = ligas[i].name;
        AlertDialog.Builder b1 = new AlertDialog.Builder(this);
        b1.setTitle("Pilih liga dulu");
        b1.setItems(namaLiga, (dialog, which) -> {
            if (which != ligaIndex) pilihLiga(which);
            final List<String> timList = ambilNamaTim();
            if (timList.isEmpty()) {
                Toast.makeText(this, "Data " + ligas[which].name
                        + " belum termuat. Coba lagi sebentar lagi.", Toast.LENGTH_LONG).show();
                return;
            }
            final String[] arr = timList.toArray(new String[0]);
            AlertDialog.Builder b2 = new AlertDialog.Builder(this);
            b2.setTitle("Tim favorit — " + ligas[which].name);
            b2.setItems(arr, (d2, pilihan) -> {
                SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                sp.edit()
                        .putInt("league_id", ligas[which].id)
                        .putString("fav_team_name", arr[pilihan])
                        .apply();
                Toast.makeText(this, "Favorit: " + arr[pilihan], Toast.LENGTH_SHORT).show();
                modeTab = MODE_FAVORIT;
                terapkanMode();
            });
            b2.setNeutralButton("Hapus favorit", (d3, w) -> {
                getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("fav_team_name").apply();
                terapkanMode();
            });
            b2.setNegativeButton("Batal", null);
            b2.show();
        });
        b1.setNegativeButton("Batal", null);
        b1.show();
    }

    /** Nama tim dari data liga yang sedang tampil (klasemen dulu, lalu dari jadwal). */
    private List<String> ambilNamaTim() {
        Set<String> nama = new LinkedHashSet<>();
        if (data != null) {
            for (SoccerParse.Row r : data.table) nama.add(r.name);
            if (nama.isEmpty()) {
                for (SoccerParse.Match m : data.matches) { nama.add(m.home); nama.add(m.away); }
            }
        }
        return new ArrayList<>(nama);
    }

    private void bacaPilihan() {
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int leagueId = sp.getInt("league_id", 47);
        for (int i = 0; i < SoccerData.getLeagues().length; i++) {
            if (SoccerData.getLeagues()[i].id == leagueId) { ligaIndex = i; break; }
        }
    }

    // ---- adapter ----

    private final class BarisAdapter extends BaseAdapter {
        private final Context ctx;

        BarisAdapter(Context ctx) { this.ctx = ctx; }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int pos) { return items.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override public boolean areAllItemsEnabled() { return false; }

        @Override public boolean isEnabled(int pos) {
            Object o = items.get(pos);
            return o instanceof SoccerParse.Row || o instanceof SoccerParse.Match;
        }

        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            Object o = items.get(pos);
            View isi;
            if (o instanceof SoccerParse.Row) {
                isi = buatBarisTabel(false, (SoccerParse.Row) o);
            } else if (o instanceof SoccerParse.Match) {
                isi = buatBarisLaga((SoccerParse.Match) o);
            } else if (o instanceof Info) {
                TextView t = buatTeks(((Info) o).teks, 14, TEKS_REDUP, false);
                t.setPadding(dp(12), dp(20), dp(12), dp(20));
                isi = t;
            } else {
                TextView t = buatTeks(String.valueOf(o), 13, AKSE, true);
                t.setPadding(dp(4), dp(10), dp(4), dp(2));
                isi = t;
            }
            LinearLayout bungkus = new LinearLayout(ctx);
            bungkus.setOrientation(LinearLayout.VERTICAL);
            bungkus.setPadding(0, 0, 0, dp(4));
            bungkus.addView(isi, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return bungkus;
        }
    }

    /** Satu baris klasemen, atau judul kolom bila judul=true. */
    private LinearLayout buatBarisTabel(boolean judul, SoccerParse.Row r) {
        LinearLayout baris = new LinearLayout(this);
        baris.setOrientation(LinearLayout.HORIZONTAL);
        baris.setGravity(Gravity.CENTER_VERTICAL);
        baris.setPadding(dp(12), dp(judul ? 4 : 8), dp(12), dp(judul ? 4 : 8));
        if (!judul) baris.setBackground(kotak(CARD, STROKE));

        int ukuran = judul ? 11 : 13;
        int warnaTeks = judul ? TEKS_REDUP : TEKS;
        String fav = judul ? null : favorit();
        boolean favorit = !judul && fav != null && SoccerParse.samaTim(fav, r.name);

        baris.addView(kolom(judul ? "#" : String.valueOf(r.rank), 32, ukuran + 1, warnaTeks, !judul, Gravity.CENTER));
        View tanda = new View(this);
        if (!judul && r.qualColor != 0) tanda.setBackgroundColor(r.qualColor);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(4), dp(judul ? 1 : 20));
        tLp.rightMargin = dp(8);
        baris.addView(tanda, tLp);
        TextView nama = buatTeks(judul ? "Tim" : r.name, judul ? 11 : 14, favorit ? AKSE : warnaTeks, favorit);
        nama.setSingleLine(true);
        nama.setEllipsize(TextUtils.TruncateAt.END);
        baris.addView(nama, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        baris.addView(kolom(judul ? "M" : String.valueOf(r.played), 36, ukuran, warnaTeks, false, Gravity.CENTER));
        baris.addView(kolom(judul ? "M+" : String.valueOf(r.won), 34, ukuran, warnaTeks, false, Gravity.CENTER));
        baris.addView(kolom(judul ? "S" : String.valueOf(r.drawn), 34, ukuran, warnaTeks, false, Gravity.CENTER));
        baris.addView(kolom(judul ? "K" : String.valueOf(r.lost), 34, ukuran, warnaTeks, false, Gravity.CENTER));
        baris.addView(kolom(judul ? "SG" : (r.goalDiff > 0 ? "+" + r.goalDiff : String.valueOf(r.goalDiff)),
                44, ukuran, warnaTeks, false, Gravity.CENTER));
        baris.addView(kolom(judul ? "Poin" : String.valueOf(r.points), 48, ukuran + 1, judul ? TEKS_REDUP : AKSE,
                !judul, Gravity.CENTER));
        return baris;
    }

    private TextView kolom(String s, int lebarDp, int sp, int warna, boolean tebal, int gravity) {
        TextView t = buatTeks(s, sp, warna, tebal);
        t.setGravity(gravity);
        t.setSingleLine(true);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(lebarDp), LinearLayout.LayoutParams.WRAP_CONTENT));
        return t;
    }

    /** Satu baris laga: tuan rumah | skor/jam | tamu. */
    private LinearLayout buatBarisLaga(SoccerParse.Match m) {
        LinearLayout baris = new LinearLayout(this);
        baris.setOrientation(LinearLayout.HORIZONTAL);
        baris.setGravity(Gravity.CENTER_VERTICAL);
        baris.setPadding(dp(12), dp(8), dp(12), dp(8));
        baris.setBackground(kotak(CARD, STROKE));

        String fav = favorit();
        boolean favKiri = fav != null && SoccerParse.samaTim(fav, m.home);
        boolean favKanan = fav != null && SoccerParse.samaTim(fav, m.away);

        TextView kiri = buatTeks(m.home, 14, favKiri ? AKSE : TEKS, favKiri);
        kiri.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        kiri.setSingleLine(true);
        kiri.setEllipsize(TextUtils.TruncateAt.END);

        TextView kanan = buatTeks(m.away, 14, favKanan ? AKSE : TEKS, favKanan);
        kanan.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        kanan.setSingleLine(true);
        kanan.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout tengah = new LinearLayout(this);
        tengah.setOrientation(LinearLayout.VERTICAL);
        tengah.setGravity(Gravity.CENTER);
        String skor;
        String keterangan;
        int warnaSkor;
        int warnaKet = TEKS_REDUP;
        switch (m.state) {
            case SoccerParse.ST_LIVE:
                skor = m.hasScore() ? m.homeScore + " - " + m.awayScore : "0 - 0";
                keterangan = m.detail.isEmpty() ? "LIVE" : m.detail;
                warnaSkor = LIVE;
                warnaKet = LIVE;
                break;
            case SoccerParse.ST_FINISHED:
                skor = m.hasScore() ? m.homeScore + " - " + m.awayScore : "-";
                keterangan = m.detail.isEmpty() ? "FT" : m.detail;
                warnaSkor = TEKS;
                break;
            case SoccerParse.ST_OFF:
                skor = "vs";
                keterangan = m.detail;
                warnaSkor = TEKS_REDUP;
                warnaKet = TUNDA;
                break;
            default:
                skor = "vs";
                keterangan = SoccerData.jam(m.utcTime);
                warnaSkor = TEKS_REDUP;
                warnaKet = AKSE;
                break;
        }
        TextView tSkor = buatTeks(skor, 17, warnaSkor, true);
        tSkor.setGravity(Gravity.CENTER);
        TextView tKet = buatTeks(keterangan, 11, warnaKet, false);
        tKet.setGravity(Gravity.CENTER);
        tengah.addView(tSkor);
        tengah.addView(tKet);

        baris.addView(kiri, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT);
        baris.addView(tengah, tLp);
        baris.addView(kanan, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return baris;
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
