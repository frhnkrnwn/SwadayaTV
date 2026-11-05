package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

/**
 * Statistik kapasitas TV: RAM, storage, info perangkat.
 */
public class CapacityStatsActivity extends Activity {

    private static final int BG = 0xFF0E1713;
    private static final int CARD = 0xFF15211B;
    private static final int CARD_FOKUS = 0xFF1E2E24;
    private static final int STROKE = 0x3314E8A0;
    private static final int AKSE = 0xFF14E8A0;
    private static final int TEKS = 0xFFFFFFFF;
    private static final int TEKS_REDUP = 0xB3FFFFFF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buatTampilan();
    }

    private void buatTampilan() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(40), dp(18), dp(40), dp(14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView judul = buatTeks("📊 Kapasitas TV", 26, TEKS, true);
        header.addView(judul, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView kembali = buatTombol("Kembali");
        kembali.setOnClickListener(v -> finish());
        header.addView(kembali);
        root.addView(header);

        // RAM
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long totalRam = mi.totalMem;
        long availRam = mi.availMem;
        long usedRam = totalRam - availRam;
        tambahBaris(root, "RAM Total", formatBytes(totalRam));
        tambahBaris(root, "RAM Terpakai", formatBytes(usedRam) + " (" + persen(usedRam, totalRam) + ")");
        tambahBaris(root, "RAM Tersedia", formatBytes(availRam));

        // Storage internal
        StatFs stat = new StatFs(getFilesDir().getAbsolutePath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availBlocks = stat.getAvailableBlocksLong();
        long totalStorage = totalBlocks * blockSize;
        long availStorage = availBlocks * blockSize;
        long usedStorage = totalStorage - availStorage;
        tambahBaris(root, "Storage Total", formatBytes(totalStorage));
        tambahBaris(root, "Storage Terpakai", formatBytes(usedStorage) + " (" + persen(usedStorage, totalStorage) + ")");
        tambahBaris(root, "Storage Tersedia", formatBytes(availStorage));

        // Info perangkat
        tambahBaris(root, "Model", android.os.Build.MODEL);
        tambahBaris(root, "Android", android.os.Build.VERSION.RELEASE + " (SDK " + android.os.Build.VERSION.SDK_INT + ")");
        tambahBaris(root, "Prosesor", android.os.Build.HARDWARE);
        tambahBaris(root, "Brand", android.os.Build.BRAND);

        TextView kaki = buatTeks("Data dibaca langsung dari sistem TV", 11, TEKS_REDUP, false);
        LinearLayout.LayoutParams kakiLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kakiLp.topMargin = dp(12);
        root.addView(kaki, kakiLp);

        setContentView(root);
    }

    private void tambahBaris(LinearLayout root, String label, String nilai) {
        LinearLayout baris = new LinearLayout(this);
        baris.setOrientation(LinearLayout.HORIZONTAL);
        baris.setGravity(Gravity.CENTER_VERTICAL);
        baris.setPadding(dp(12), dp(10), dp(12), dp(10));
        baris.setBackground(kotak(CARD, STROKE));
        TextView l = buatTeks(label, 14, TEKS_REDUP, false);
        l.setMinWidth(dp(180));
        TextView v = buatTeks(nilai, 14, TEKS, true);
        v.setGravity(Gravity.RIGHT);
        baris.addView(l);
        baris.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(4);
        baris.setLayoutParams(lp);
        root.addView(baris);
    }

    private String formatBytes(long b) {
        return Formatter.formatFileSize(this, b);
    }

    private String persen(long used, long total) {
        if (total == 0) return "0%";
        return String.format(Locale.getDefault(), "%.1f%%", (used * 100.0 / total));
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
