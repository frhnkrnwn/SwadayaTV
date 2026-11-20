# Riset Struktur Tema CleanYourTV

Halo! Ini hasil saya membedah dua config tema aslinya: `/tmp/clean/themes/default/config.json` dan `/tmp/clean/themes/aurora/config.json`. Di SwadayaTV, struktur ini yang kita pakai untuk tiga tema kita sendiri.

## Struktur `config.json`

Tema itu folder berisi **`config.json`** dan opsional **`wallpaper.jpg`** (atau folder `wallpapers/` untuk koleksi). Isi `config.json` adalah JSON object dengan key-key opsional. Key yang tidak ada = mengikuti nilai bawaan di `launcher/.../res/values/config.xml`. Key yang tidak dikenal atau salah tipe = diabaikan. File yang tidak bisa di-parse = diabaikan seluruhnya — jadi tema rusak tidak akan merusak Home.

### Field yang penting (dari `themes/README.md`)

| Key | Tipe | Artinya |
|---|---|---|
| `dock_apps` | list package name | Urutan aplikasi di baris dock paling bawah. Aplikasi yang tidak terpasang dilewati. |
| `hidden_apps` | list package name | Aplikasi yang tidak pernah ditampilkan sebagai tile. |
| `columns` | 1–8 | Jumlah tile per baris; di atas 5 tile-nya mengecil agar muat. |
| `rows_below_dock` | 0–3 | Berapa baris grid tetap terlihat di bawah dock. |
| `background_color` | `#RRGGBB` | Warna di belakang semuanya; default hitam. |
| `wallpaper` | bool | `true` = menampilkan gambar tema, `false` = gambar diabaikan, hanya warna. |
| `reduced_motion` | bool | Gerak lebih tenang untuk GPU lemah (highlight geser tanpa pantulan, animasi ≤90 ms). |

Key tambahan seperti `dock_title`, `dock_band`, `fill_dock`, `sky_intensity`, `icon_only_buttons`, dll. ada di tabel lengkap `themes/README.md` — untuk tema Swadaya kita cukup pakai yang tujuh di atas agar sederhana.

### Contoh nyata: `themes/default/config.json`

```json
{
  "dock_apps": ["com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "com.google.android.youtube.tv", "in.startv.hotstar"],
  "hidden_apps": ["com.mediatek.wwtv.tvcenter", "com.mitv.gallery"],
  "columns": 4,
  "rows_below_dock": 0,
  "reduced_motion": false,
  "background_color": "#000000",
  "wallpaper": false
}
```

Ini tulisan persis nilai bawaan `config.xml` — titik awal paling mudah untuk tema Anda sendiri. Empat dock: Netflix, Prime Video, YouTube TV, Hotstar. Dua hidden: TV Center MediaTek dan Galeri MiTV.

### Contoh nyata: `themes/aurora/config.json`

```json
{
  "background_color": "#070A14",
  "wallpaper": true
}
```

Aurora hanya mengatur dua key: warna navy pekat `#070A14` dan wallpaper `true`, sisanya (dock, kolom, dsb.) mengikuti bawaan. Folder aurora sendiri membawa wallpaper aurora (teal + violet di atas navy, lebih gelap di bawah). Pelajaran dari sini: tema minimal bisa hanya 2 baris seperti ini.

## Ganti tema tanpa rebuild

Anda **tidak perlu rebuild APK** untuk ganti tema. Cukup push folder temanya ke TV:

```bash
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya          # push + restart Home
TV_HOST=192.168.1.50 tools/theme.sh --reset                 # hapus overlay, kembali ke config.xml
```

Cara kerjanya: `theme.sh` upload `config.json` (+ wallpaper jika ada) ke folder data launcher di TV lewat provider, lalu restart Home. Launcher membaca overlay itu saat start, di atas `config.xml` bawaan. Push tema (atau `--reset`) juga menghapus pilihan kolom/motion yang Anda set manual di TV, agar tidak bentrok dengan tema baru. Di SwadayaTV, tiga folder tema kita sudah siap:

```bash
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya          # hijau-abu minimal (default)
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya-malam    # gelap pekat
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya-terang   # terang
```

Di Windows: `powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost 192.168.1.50 themes\swadaya`.

## Tiga saran tema SwadayaTV

### 1. `themes/swadaya` — putih minimal (default)

Versi terang-hijau Swadaya: background hijau-abu gelap tegas, tanpa wallpaper, grid 5 kolom, 1 baris di bawah dock. Palet:

| Elemen | Hex |
|---|---|
| Background utama | `#0E1713` (hijau-abu gelap) |
| Teks utama | `#FFFFFF` |
| Teks redup | `#9DB3A8` |
| Aksen (hijau Swadaya) | `#2FA37C` |
| Pill glass normal | `#EE101D16` (hijau-abu 92%) |
| Pill glass fokus | `#FF1B2E25` |

### 2. `themes/swadaya-malam` — hijau-abu malam

Untuk menonton malam, benar-benar gelap agar tidak silau: background hampir hitam kehijauan, wallpaper dimatikan di JSON default-nya (tambah `wallpaper.jpg` + set `wallpaper: true` jika Anda ingin gambar di belakang). Palet:

| Elemen | Hex |
|---|---|
| Background utama | `#060A08` (hijau-abu malam) |
| Teks utama | `#E8F0EB` |
| Teks redup | `#7A8F84` |
| Aksen | `#1F7A5C` (hijau lebih tenang) |
| Pill glass normal | `#E6080F0C` |
| Pill glass fokus | `#FF12201A` |

### 3. `themes/swadaya-terang` — terang

Versi siang yang bersih untuk ruangan terang: background krem terang, wallpaper mati, teks gelap. Palet:

| Elemen | Hex |
|---|---|
| Background utama | `#F2F0EB` (krem) |
| Teks utama | `#1A2420` |
| Teks redup | `#6B7672` |
| Aksen | `#2FA37C` |
| Pill glass normal | `#FFF7F5F0` |
| Pill glass fokus | `#FFFFFFFF` dengan bayangan lembut |

Ketiga JSON-nya valid dan pakai paket dock yang sama (Netflix, YouTube TV, Prime Video, Disney+) agar konsisten. File-nya ada di `themes/swadaya/config.json`, `themes/swadaya-malam/config.json`, `themes/swadaya-terang/config.json` — masing-masing punya `README.md` singkat. Jika ingin mengganti warna, edit `background_color`-nya saja, push ulang, selesai.
