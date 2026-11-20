# Spesifikasi Tema Swadaya — Rencana Perubahan Kode

> **Status: spesifikasi saja.** Dokumen ini mencatat perubahan kode yang **akan** dilakukan nanti. Sekarang **jangan edit kode launcher apa pun** — tugas agen ini hanya membuat file JSON tema + dokumen. Gunakan dokumen ini sebagai acuan saat eksekusi.

## 1. `ThemeManager.java` — tambah konstanta tema

File: `launcher/app/src/main/java/com/gathin/tvhome/ThemeManager.java`

Tambah tiga konstanta baru di bawah `THEME_DARK_GLOW`:

```java
public static final String THEME_SWADAYA = "swadaya";
public static final String THEME_SWADAYA_MALAM = "swadaya_malam";
public static final String THEME_SWADAYA_TERANG = "swadaya_terang";
```

Lalu perbarui method yang sudah ada agar mengenali tema baru, tanpa mengubah tema lama:

- `getActiveTheme()` — tetap mengembalikan default `THEME_FROSTED_GLASS`, tidak berubah.
- `isDarkGlow()` / `isNeumorphism()` / `isFrostedGlass()` — tetap seperti sekarang; tema Swadaya akan ditangani cabang baru (bukan masuk ke salah satu dari ketiganya).
- Tambah method helper baru (nama bisa disesuaikan saat eksekusi): `isSwadaya(Context)`, `isSwadayaMalam(Context)`, `isSwadayaTerang(Context)`.
- `getThemeDisplayName(String)` — tambah case:
  - `THEME_SWADAYA` → `"Swadaya"`
  - `THEME_SWADAYA_MALAM` → `"Swadaya Malam"`
  - `THEME_SWADAYA_TERANG` → `"Swadaya Terang"`
- `getThemeSubtitle(String)` — tambah case dengan subtitle Bahasa Indonesia:
  - Swadaya: `"Tema default hijau-abu minimal SwadayaTV tanpa wallpaper"`
  - Swadaya Malam: `"Tema gelap pekat hijau-abu untuk menonton malam, wallpaper aktif"`
  - Swadaya Terang: `"Tema terang krem bersih untuk ruangan yang cukup cahaya"`

## 2. `MainActivity.java` — `showThemeDialog()` tambah 3 option

File: `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java`, method `showThemeDialog()` (baris ~4132).

Saat ini dialog isinya 3 `ThemeOption`: `THEME_FROSTED_GLASS` ("💎 Frosted Glass"), `THEME_NEUMORPHISM` ("⚪ Neumorphism"), `THEME_DARK_GLOW` ("⚡ Dark Glow"). Tambah 3 lagi di bawahnya, supaya urutan tampilnya: Frosted Glass, Neumorphism, Dark Glow, **Swadaya, Swadaya Malam, Swadaya Terang**:

```java
options.add(new ThemeOption(ThemeManager.THEME_SWADAYA,
        "🌿 Swadaya",
        "Tema default hijau-abu minimal SwadayaTV tanpa wallpaper"));
options.add(new ThemeOption(ThemeManager.THEME_SWADAYA_MALAM,
        "🌙 Swadaya Malam",
        "Tema gelap pekat hijau-abu untuk menonton malam"));
options.add(new ThemeOption(ThemeManager.THEME_SWADAYA_TERANG,
        "☀️ Swadaya Terang",
        "Tema terang krem bersih untuk ruangan yang cukup cahaya"));
```

Logika dialog lain (checklist tema aktif, `ThemedDialogItem`, animasi) tetap jalan apa adanya — hanya menambah item ke list `options`.

## 3. Palet warna hex yang akan dipakai

Palet ini yang akan jadi acuan untuk cabang tema baru di `applyTopMenuTheme()` dan `applyHomeScreenTheme()` (di `MainActivity.java`), plus `GlassView` / `Glider` / `AppAdapter` jika perlu disentuh:

| Elemen | Swadaya (`#0E1713`) | Swadaya Malam (`#060A08`) | Swadaya Terang (`#F2F0EB`) |
|---|---|---|---|
| Background utama | `#0E1713` hijau-abu gelap | `#060A08` malam | `#F2F0EB` krem |
| Teks utama | `#FFFFFF` | `#E8F0EB` | `#1A2420` |
| Teks redup | `#9DB3A8` | `#7A8F84` | `#6B7672` |
| Aksen (hijau Swadaya) | `#2FA37C` | `#1F7A5C` | `#2FA37C` |
| Pill glass normal | `#EE101D16` | `#E6080F0C` | `#FFF7F5F0` |
| Pill glass fokus | `#FF1B2E25` | `#FF12201A` | `#FFFFFFFF` |
| Wallpaper filter | gelap lembut | lebih gelap lagi | tanpa filter / terang |

Catatan penting: Swadaya + Swadaya Malam memakai **teks putih** seperti Frosted Glass, sedangkan **Swadaya Terang memakai teks gelap** (`#1A2420`) seperti Neumorphism — jadi cabang `isNeu`-style-nya harus pakai kondisi `isSwadayaTerang`, bukan `isNeumorphism`. Bayangan teks untuk versi terang dimatikan (shadow layer 0) supaya tetap bersih.

## 4. Drawee fallback `pill_bg` + glass

Yang akan diubah nanti di cabang tema Swadaya:

1. **Pill top menu** (saat ini di `applyTopMenuTheme()` pakai `GlassDrawable` untuk Frosted Glass, `NeumorphicDrawable` untuk Neumorphism, gradient neon untuk Dark Glow): untuk tema Swadaya, pakai `GlassDrawable` juga tapi dengan warna tint dari tabel di atas, supaya pill-nya tetap kaca buram hijau-abu. Jangan membuat drawable baru jika `GlassDrawable` cukup dengan param warna.
2. **Poster drawee tile** di `AppAdapter.java` (saat ini branching `isDarkGlow` → poster hitam, `isNeumorphism` → poster terang, default → poster biasa): tambah cabang `isSwadaya()` → poster gelap hijau-abu, `isSwadayaMalam()` → poster hitam kehijauan, `isSwadayaTerang()` → poster krem terang. Fallback-nya tetap sama: kalau drawee tidak termuat, poster warna tema yang dipakai, bukan hitam/warna aneh.
3. **Glass highlight** di `GlassView.java` (saat ini branching `isDarkGlow` / `isNeumorphism` di `Glider.java` juga): jika Swadaya memakai `GlassDrawable`, pastikan tint glass-nya ikut aksen `#2FA37C` untuk Swadaya & Swadaya Terang, dan `#1F7A5C` untuk Swadaya Malam.

Garis merahnya: **tidak ada file yang diubah sekarang**. Ini murni spesifikasi untuk PR berikutnya. File JSON tema (`themes/swadaya/config.json` dkk.) yang sudah ada di repo ini sudah valid dan siap di-push — begitu kode tema Swadaya masuk, ganti tema dari dialog atau `tools/theme.sh` langsung tersambung.
