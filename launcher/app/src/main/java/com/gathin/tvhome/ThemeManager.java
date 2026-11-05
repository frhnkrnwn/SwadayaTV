package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tema visual SwadayaTV.
 *
 * Ada tiga tema resmi — semuanya minimalis, tanpa meniru tema launcher lain:
 * 1. Swadaya       — kaca jernih hijau-abu, kartu mengambang tenang (default)
 * 2. Swadaya Malam — OLED hijau zamrud pekat, hemat mata untuk malam
 * 3. Swadaya Terang— porselen terang hijau-abu, nyaman untuk siang
 *
 * Kunci tema lama (frosted_glass / dark_glow / neumorphism) tetap dipakai
 * sebagai alias supaya seluruh kode tampilan yang sudah ada tidak perlu diubah.
 */
public final class ThemeManager {
    public static final String THEME_SWADAYA = "swadaya";
    public static final String THEME_SWADAYA_MALAM = "swadaya_malam";
    public static final String THEME_SWADAYA_TERANG = "swadaya_terang";

    /** Alias kompatibilitas untuk kode lama. */
    public static final String THEME_FROSTED_GLASS = THEME_SWADAYA;
    public static final String THEME_DARK_GLOW = THEME_SWADAYA_MALAM;
    public static final String THEME_NEUMORPHISM = THEME_SWADAYA_TERANG;

    private static final String PREFS_NAME = "tvhome_theme_prefs";
    private static final String KEY_ACTIVE_THEME = "active_theme";

    private ThemeManager() {}

    /** Tema yang sedang aktif. Default: Swadaya. */
    public static String getActiveTheme(Context context) {
        if (context == null) return THEME_SWADAYA;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String theme = sp.getString(KEY_ACTIVE_THEME, THEME_SWADAYA);
        return migrate(theme);
    }

    /** Menyimpan tema aktif. */
    public static void setActiveTheme(Context context, String theme) {
        if (context == null || theme == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ACTIVE_THEME, migrate(theme)).apply();
    }

    /** Memetakan nama tema lama (dari versi sebelumnya) ke tema Swadaya baru. */
    private static String migrate(String theme) {
        if (theme == null) return THEME_SWADAYA;
        switch (theme) {
            case "frosted_glass":
                return THEME_SWADAYA;
            case "dark_glow":
                return THEME_SWADAYA_MALAM;
            case "neumorphism":
                return THEME_SWADAYA_TERANG;
            default:
                return theme;
        }
    }

    public static boolean isSwadaya(Context context) {
        return THEME_SWADAYA.equalsIgnoreCase(getActiveTheme(context));
    }

    public static boolean isSwadayaMalam(Context context) {
        return THEME_SWADAYA_MALAM.equalsIgnoreCase(getActiveTheme(context));
    }

    public static boolean isSwadayaTerang(Context context) {
        return THEME_SWADAYA_TERANG.equalsIgnoreCase(getActiveTheme(context));
    }

    /** True untuk tema gelap (Swadaya Malam). */
    public static boolean isDarkGlow(Context context) {
        return isSwadayaMalam(context);
    }

    /** True untuk tema terang (Swadaya Terang). */
    public static boolean isNeumorphism(Context context) {
        return isSwadayaTerang(context);
    }

    /** True untuk tema kaca jernih (Swadaya). */
    public static boolean isFrostedGlass(Context context) {
        return isSwadaya(context);
    }

    /** Nama tema untuk ditampilkan di UI. */
    public static String getThemeDisplayName(String theme) {
        String key = migrate(theme);
        switch (key) {
            case THEME_SWADAYA_MALAM:
                return "Swadaya Malam";
            case THEME_SWADAYA_TERANG:
                return "Swadaya Terang";
            case THEME_SWADAYA:
            default:
                return "Swadaya";
        }
    }

    /** Deskripsi singkat tiap tema. */
    public static String getThemeSubtitle(String theme) {
        String key = migrate(theme);
        switch (key) {
            case THEME_SWADAYA_MALAM:
                return "Hitam zamrud OLED, aksen hijau lembut, paling hemat mata untuk malam";
            case THEME_SWADAYA_TERANG:
                return "Porselen terang hijau-abu, bersih dan nyaman untuk siang hari";
            case THEME_SWADAYA:
            default:
                return "Kaca jernih hijau-abu, kartu mengambang tenang tanpa neon";
        }
    }
}
