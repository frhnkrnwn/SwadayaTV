# Riset Struktur Tema CleanYourTV

Halo! Ini hasil aku bedah dua config tema aslinya: `/tmp/clean/themes/default/config.json` dan `/tmp/clean/themes/aurora/config.json`. Di SwadayaTV, struktur ini yang kita pakai buat tiga tema kita sendiri.

## Struktur `config.json`

Tema itu folder berisi **`config.json`** dan opsional **`wallpaper.jpg`** (atau folder `wallpapers/` buat koleksi). Isi `config.json` adalah JSON object dengan key-key opsional. Key yang nggak ada = ngikutin nilai bawaan di `launcher/.../res/values/config.xml`. Key yang nggak dikenal atau salah tipe = diabaikan. File yang nggak bisa di-parse = diabaikan seluruhnya — jadi tema rusak nggak bakal merusak Home.

### Field yang penting (dari `themes/README.md`)

| Key | Tipe | Artinya |
|---|---|---|
| `dock_apps` | list package name | Urutan aplikasi di baris dock paling bawah. Aplikasi yang nggak terinstall di-skip. |
| `hidden_apps` | list package name | Aplikasi yang nggak pernah ditampilin sebagai tile. |
| `columns` | 1–8 | Jumlah tile per baris; di atas 5 tile-nya mengecil biar muat. |
| `rows_below_dock` | 0–3 | Berapa baris grid tetap kelihatan di bawah dock. |
| `background_color` | `#RRGGBB` | Warna di belakang semuanya; default hitam. |
| `wallpaper` | bool | `true` = nampilin gambar tema, `false` = gambar diabaikan, cuma warna. |
| `reduced_motion` | bool | Gerak lebih kalem buat GPU lemah (highlight geser tanpa pantulan, animasi ≤90 ms). |

Key tambahan kayak `dock_title`, `dock_band`, `fill_dock`, `sky_intensity`, `icon_only_buttons`, dll. ada di tabel lengkap `themes/README.md` — buat tema Swadaya kita cukup pakai yang tujuh di atas biar sederhana.

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

Ini tulisan persis nilai bawaan `config.xml` — titik mulai paling gampang buat tema kamu sendiri. Empat dock: Netflix, Prime Video, YouTube TV, Hotstar. Dua hidden: TV Center MediaTek dan Galeri MiTV.

### Contoh nyata: `themes/aurora/config.json`

```json
{
  "background_color": "#070A14",
  "wallpaper": true
}
```

Aurora cuma ngeset dua key: warna navy pekat `#070A14` dan wallpaper `true`, sisanya (dock, kolom, dsb.) ikut bawaan. Folder aurora sendiri ngebawa wallpaper aurora (teal + violet di atas navy, lebih gelap di bawah). Pelajaran dari sini: tema minimal bisa cuma 2 baris kayak gini.

## Ganti tema tanpa rebuild

Kamu **nggak perlu rebuild APK** buat ganti tema. Cukup push folder temanya ke TV:

```bash
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya          # push + restart Home
TV_HOST=192.168.1.50 tools/theme.sh --reset                 # hapus overlay, balik ke config.xml
```

Cara kerjanya: `theme.sh` upload `config.json` (+ wallpaper kalau ada) ke folder data launcher di TV lewat provider, terus restart Home. Launcher baca overlay itu pas start, di atas `config.xml` bawaan. Push tema (atau `--reset`) juga menghapus pilihan kolom/motion yang kamu set manual di TV, biar nggak bentrok sama tema baru. Di SwadayaTV, tiga folder tema kita sudah siap:

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

Buat nonton malam, bener-bener gelap biar nggak silau: background hampir hitam kehijauan, wallpaper dimatiin di JSON default-nya (tambah `wallpaper.jpg` + set `wallpaper: true` kalau kamu mau gambar di belakang). Palet:

| Elemen | Hex |
|---|---|
| Background utama | `#060A08` (hijau-abu malam) |
| Teks utama | `#E8F0EB` |
| Teks redup | `#7A8F84` |
| Aksen | `#1F7A5C` (hijau lebih kalem) |
| Pill glass normal | `#E6080F0C` |
| Pill glass fokus | `#FF12201A` |

### 3. `themes/swadaya-terang` — terang

Versi siang yang bersih buat ruangan terang: background krem terang, wallpaper mati, teks gelap. Palet:

| Elemen | Hex |
|---|---|
| Background utama | `#F2F0EB` (krem) |
| Teks utama | `#1A2420` |
| Teks redup | `#6B7672` |
| Aksen | `#2FA37C` |
| Pill glass normal | `#FFF7F5F0` |
| Pill glass fokus | `#FFFFFFFF` dengan bayangan lembut |

Ketiga JSON-nya valid dan pakai paket dock yang sama (Netflix, YouTube TV, Prime Video, Disney+) biar konsisten. File-nya ada di `themes/swadaya/config.json`, `themes/swadaya-malam/config.json`, `themes/swadaya-terang/config.json` — masing-masing punya `README.md` singkat. Kalau mau ganti warna, edit `background_color`-nya aja, push ulang, beres.
