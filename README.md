<div align="center">

# 📺 SwadayaTV

**Launcher Android TV / STB yang Cepat, Minimalis, dan Mudah Dipakai**  
*Tema hijau-abu Swadaya sendiri — tenang, tanpa neon, dan tetap smooth di STB kentang.*

[![Build & Release APK](https://github.com/frhnkrnwn/SwadayaTV/actions/workflows/build-apk.yml/badge.svg)](https://github.com/frhnkrnwn/SwadayaTV/actions/workflows/build-apk.yml)
[![Latest Release](https://img.shields.io/github/v/release/frhnkrnwn/SwadayaTV?color=0284C7&label=Rilis%20Terbaru)](https://github.com/frhnkrnwn/SwadayaTV/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Google%20TV%20%7C%20STB-10B981)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[**📥 Unduh APK (SwadayaTV.apk)**](https://github.com/frhnkrnwn/SwadayaTV/releases/latest/download/SwadayaTV.apk)

---

</div>

## Daftar Isi

1. [Apa Itu SwadayaTV?](#apa-itu-swadayatv)
2. [Fitur Lengkap](#fitur-lengkap)
3. [Bahan-Bahan yang Dibutuhkan](#bahan-bahan-yang-dibutuhkan)
4. [Cara Install — Paling Gampang (Flashdisk)](#cara-install--paling-gampang--flashdisk)
5. [Cara Install — Lewat ADB (Wi-Fi)](#cara-install--lewat-adb-wi-fi)
6. [Cara Bersihkan (Debloat) Launcher Bawaan STB](#cara-bersihkan-debloat-launcher-bawaan-stb)
7. [Cara Ganti Tema](#cara-ganti-tema)
8. [Cara Ganti Wallpaper HD Gratis](#cara-ganti-wallpaper-hd-gratis)
9. [Fitur Bola: Klasemen & Jadwal](#fitur-bola-klasemen--jadwal)
10. [Waktu Sholat & Ayat Harian](#waktu-sholat--ayat-harian)
11. [Cuaca & Kualitas Udara (AQI)](#cuaca--kualitas-udara-aqi)
12. [Now Playing Musik](#now-playing-musik)
13. [Update Otomatis (OTA)](#update-otomatis-ota)
14. [Addon (Jam Dunia, Kurs Koin, dll)](#addon-jam-dunia-kurs-koin-dll)
15. [Struktur Repo](#struktur-repo)
16. [Build Sendiri dari Source](#build-sendiri-dari-source)
17. [Bahasa](#bahasa)
18. [Kredit & Sumber](#kredit--sumber)

---

## Apa Itu SwadayaTV?

SwadayaTV adalah **launcher (layar utama) pengganti** untuk Android TV / Google TV / STB Indonesia (IndiHome, Biznet, ZTE, FiberHome). Bedanya dari launcher bawaan pabrik:

- Lebih bersih dan minimal — cuma aplikasi yang kamu pakai yang nongol
- Bisa ganti tema, wallpaper HD gratis, dan lihat cuaca/bola/sholat/ayat langsung di layar utama
- Ringan (~700 KB) dan smooth — dirancang jalan lancar di chip STB kelas menengah bawah (Amlogic, Rockchip, Allwinner)

---

## Fitur Lengkap

| No | Fitur | Keterangan |
|---|---|---|
| 1 | **Ganti tema** | 3 tema minimal hijau-abu buatan Swadaya (Swadaya, Swadaya Malam, Swadaya Terang) |
| 2 | **Ganti wallpaper HD** | 60+ foto 4K gratis dari Unsplash & Pexels, otomatis ganti tiap 6 jam |
| 3 | **Latar hidup (live wallpaper)** | 🌊 Samudra & Pantai beranimasi dan 🏟️ Stadion Malam — aktif dari menu Wallpaper |
| 4 | **Cuaca + AQI** | Suhu, ramalan 3/6/9 jam, kualitas udara, kelembapan, angin — dari Open-Meteo (tanpa API key) |
| 5 | **Bola: klasemen & jadwal** | Liga Inggris, Spanyol, Italia, Jerman, Prancis, Liga 1 Indonesia, UCL — dari FotMob |
| 6 | **Waktu sholat (Kemenag)** | Hitung offline pakai rumus Kemenag (Subuh 20°, Isya 18°) — jalan tanpa internet |
| 7 | **Ayat/Doa harian** | Rotasi ayat dan doa harian, tersimpan offline |
| 8 | **Now Playing musik** | Judul + cover album dari Spotify / YouTube / VLC / Apple Music, muncul otomatis pas musik jalan |
| 9 | **Addon kartu modul** | Install APK addon kecil (Jam Dunia, Kurs Koin, Capacity Stats, Photo Screensaver) |
| 10 | **OTA update** | Tombol update di TV, langsung download & install dari GitHub |

---

## Bahan-Bahan yang Dibutuhkan

| Bahan | Untuk apa | Link |
|---|---|---|
| **STB / Android TV / Google TV** | Perangkat tujuan | — |
| **HP Android / Laptop / PC** | Buat install via USB atau ADB | — |
| **SwadayaTV.apk** | Launcher-nya | [Unduh terbaru](https://github.com/frhnkrnwn/SwadayaTV/releases/latest/download/SwadayaTV.apk) |
| *(Opsional)* **Flashdisk kosong** | Kalau install cara USB | — |
| *(Opsional)* **JDK 17 + Android SDK** | Kalau mau build sendiri dari source | [Panduan build](#build-sendiri-dari-source) |

---

## Cara Install — Paling Gampang (Flashdisk)

1. Di HP/PC, unduh `SwadayaTV.apk` dari tombol biru di atas
2. Salin file `SwadayaTV.apk` ke Flashdisk
3. Colok flashdisk ke TV/STB, buka **File Manager** di STB
4. Kalau diminta izin, nyalakan **"Install unknown apps"** untuk File Manager
5. Klik file `SwadayaTV.apk` → **Install**
6. Tekan tombol **Home** di remote → pilih **SwadayaTV** → pilih **"Selalu" (Always)**
7. Selesai! Sekarang layar utama TV udah SwadayaTV 🎉

---

## Cara Install — Lewat ADB (Wi-Fi)

Butuh PC/Laptop/HP yang satu jaringan Wi-Fi dengan TV.

1. Install **platform-tools** (isi file `adb`):  
   - Windows/Linux/Mac: [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)  
   - HP Android: install aplikasi **LADb** atau **Wireless ADB Debugger** dari Play Store
2. Di TV: **Pengaturan → Tentang → ketuk "Build" beberapa kali** sampai muncul menu **Opsi Pengembang (Developer Options)** → aktifkan **USB debugging** + **Wireless debugging**
3. Di TV: **Pengaturan → Tentang → Status → IP address** — catat IP-nya (misal `192.168.1.8`)
4. Di PC/Laptop, sambungkan ke TV:
   ```bash
   adb connect 192.168.1.8:5555
   adb install -r SwadayaTV.apk

   # (Opsional) perlu izin pemutar musik:
   adb shell cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.MediaNotificationListener

   # Jadikan launcher default:
   adb shell cmd package set-home-activity com.gathin.tvhome/.MainActivity
   ```
5. Tekan **Home** di remote → pilih **SwadayaTV** → **Selalu**
6. Selesai!

> 💡 Ini perintah **cuma sekali** aja. Kalau nanti ganti TV baru, ulangi langkah 2–5.

---

## Cara Bersihkan (Debloat) Launcher Bawaan STB

Kalau tombol Home masih balik ke launcher pabrik (IndiHome, Android TV Core, Google TV bawaan), matikan launcher bawaannya. **Aman** — bisa dinyalakan lagi kapan aja.

**Baris perintahnya ada di tool bawaan repo ini:**

```bash
bash tools/disable.sh          # matikan aplikasi pabrik yang mengganggu
bash tools/restore.sh          # nyalakan lagi kalau mau balik normal
bash tools/theme.sh themes/swadaya      # ganti tema tanpa build ulang
bash tools/theme.sh --reset              # kembalikan tema default
```

Atau manual satu-satu:

```bash
adb shell pm disable-user --user 0 com.google.android.tvlauncher          # Android TV
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx  # Google TV UI baru
adb shell pm disable-user --user 0 com.zte.iptvclient.android.launcher    # STB IndiHome ZTE
adb shell pm disable-user --user 0 com.fiberhome.myindihome              # FiberHome / Biznet
```

Balikin lagi: ganti `disable-user` jadi `enable`.

---

## Cara Ganti Tema

Buka **Pengaturan → Tema** di launcher, pilih salah satu:

1. **🌿 Swadaya** (default) — kaca jernih hijau-abu, kartu mengambang tenang tanpa neon
2. **🌙 Swadaya Malam** — hitam zamrud OLED, hemat mata untuk malam
3. **☀️ Swadaya Terang** — porselen terang hijau-abu, nyaman untuk siang hari

Tema langsung berubah tanpa restart. Nama tema lama (`Frosted Glass`, `Neumorphism`, `Dark Glow`) otomatis dipetakan jadi tema Swadaya versi baru.

---

## Cara Ganti Wallpaper HD Gratis

SwadayaTV sudah bawa koleksi **60+ foto wallpaper 4K (3840×2160) gratis** dari:

- **Unsplash** (lisensi Unsplash — gratis komersial)
- **Pexels** (lisensi Pexels — gratis komersial)

Kategorinya: Indonesia (Bromo, Bali, Lombok, dll), alam, kota, pantai, pegunungan. Diunduh sekali lalu **cache lokal**, diganti otomatis tiap 6 jam. Ada juga foto tempat wisata dengan nama tempat + cerita singkatnya (dari Bing).

Di **Pengaturan → Wallpaper** kamu bisa: ganti manual, upload gambar sendiri dari HP (scan QR), atau matikan wallpaper online (`"places": false` di `config.json`).

### Wallpaper Hidup (Beranimasi)

Selain foto biasa, SwadayaTV punya dua latar hidup yang bergerak halus 60 FPS:

| Latar | Suasananya | Cocok untuk |
|---|---|---|
| **🌊 Samudra & Pantai** | Langit berubah mengikuti jam (pagi, siang, senja, malam), matahari/bulan terbit dan terbenam, ombak tiga lapis, bukit pasir, pohon kelapa, dan kawanan ikan | Santai, suka pemandangan laut |
| **🏟️ Stadion Malam** | Lapangan rumput hijau, lampu stadion berdenyut, sorotan lampu menyapu | Penggemar bola, dipadukan dengan klasemen |

Cara mengaktifkan:

1. Tekan tombol **Pengaturan** di launcher
2. Pilih **🌊 Latar Samudra Hidup** atau **🏟️ Latar Stadion Hidup**
3. Latar langsung berganti tanpa restart
4. Untuk mematikan, pilih menu yang sama sekali lagi — wallpaper foto akan kembali

Kalau keduanya aktif, Stadion yang dipakai. Animasi dibuat ringan supaya remote dan perpindahan menu tetap lancar di STB.

---

## Fitur Bola: Klasemen & Jadwal

Buka **Pengaturan → Klasemen & Jadwal Bola** dari launcher.

- Liga: **Premier League (EPL), LaLiga, Serie A, Bundesliga, Ligue 1, Liga 1 Indonesia, Champions League (UCL)**
- Tab **Klasemen** (posisi, tim, main, menang-seri-kalah, poin) dan **Jadwal** (hasil & jadwal ke depan)
- Pilih **liga favorit** dan **tim favorit** (misal: AS Roma di Serie A) — tim favorit akan disorot hijau
- Data dari FotMob, tanpa API key, dengan cache offline — terakhir diupdate musim berjalan

---

## Waktu Sholat & Ayat Harian

- **Waktu sholat Kemenag:** Dihitung langsung di TV pakai rumus Bimas Islam Kemenag (Subuh 20°, Isya 18°, Asr Syafi'i) — bukan dari web. Jalan offline, tampil langsung di layar utama.
- **Zona waktu otomatis:** WIB, WITA, atau WIT menyesuaikan kota yang dipilih (dari garis bujur), jadi jamnya tidak meleset antar-daerah.
- **Syuruk (terbit matahari):** Ikut dihitung, bukan sekadar tanda "-".
- **Sinkron online:** Saat internet tersedia, jadwal disegarkan dari server Aladhan (metode Kemenag) dan ditandai **SERVER**; kalau offline tetap pakai hitungan lokal dan ditandai **OFFLINE**.
- **Ayat/Doa harian:** Rak kanan layar utama bergantian menampilkan ayat Al-Qur'an dan doa harian (Arab + terjemahan), tersimpan offline.
- **Kumpulan Doa:** Buka **Pengaturan → 🤲 Doa Harian** untuk membaca semua doa (71 doa online, 15 doa cadangan saat offline).
- Kotanya pilih di **Pengaturan → Lokasi**.

---

## Cuaca & Kualitas Udara (AQI)

- Menampilkan lokasi, suhu besar, ikon vektor cuaca, dan kondisi cuaca saat ini
- **Slot Ramalan 3 Jam ke Depan:** Memantau ramalan cuaca pada rentang waktu `+3 jam`, `+6 jam`, dan `+9 jam` secara akurat dari Open-Meteo
- Dua pil informasi di bagian bawah: **AQI (Indeks Kualitas Udara)** dan **Peluang Hujan (%)**
- Posisi widget terkunci statis tepat di tengah antara Jam dan Nama Aplikasi Baris 1 tanpa bergeser

---

## Now Playing Musik

- Kapsul status dinamis setinggi 42dp yang sejajar dengan bilah menu atas (hanya muncul saat musik diputar)
- Menampilkan cover album asli, judul lagu berjalan (*marquee*), nama artis, serta tombol kontrol instan `[ ⏯ Play/Pause ]` dan `[ ⏭ Next ]`
- Mendukung berbagai pemutar musik & media (Spotify TV, YouTube TV, SmartTube, Apple Music, VLC, pemutar lagu lokal USB, dll)
- Tampilan beradaptasi 100% mengikuti tema aktif

> 💡 Jalankan perintah ADB setelah install agar fitur Now Playing langsung aktif:
> ```bash
> adb shell cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.MediaNotificationListener
> ```

---

## Update Otomatis (OTA)

SwadayaTV punya tombol **update langsung di TV**:

1. Kalau ada versi baru, launcher akan cek GitHub otomatis saat dibuka
2. Akan muncul notifikasi **"Update tersedia: SwadayaTV vX.Y.Z"**
3. Tekan **Update** → download → install → restart sendiri

Metadata versi tersimpan di:

```
https://raw.githubusercontent.com/frhnkrnwn/SwadayaTV/main/release/version.json
```

---

## Addon (Jam Dunia, Kurs Koin, dll)

Addon = APK kecil terpisah yang nempel sebagai kartu di layar utama. Masing-masing addon jalan mandiri (auto on/off) jadi nggak bentrok sama launcher.

1. Unduh addon dari folder [`addons/`](addons/) di repo ini atau dari [CleanYourTV addons](https://cleanyour.tv/addons)
2. Install APK addon ke TV seperti Cara 1/2
3. Setujui **"Show card" / "Tampilkan kartu"** sekali aja pas pertama buka
4. Kartu bakal muncul di layar utama

> File lengkap addon ada di `addons/` dan `docs/ADDONS_RESEARCH.md`. Yang siap dipakai: Cuaca (Open-Meteo), Sholat (Kemenag offline), Koin (Frankfurter, tanpa API key), Jam Dunia, Capacity Stats, Photo Screensaver.

---

## Struktur Repo

```
SwadayaTV/
├─ launcher/            App launcher utama (Android TV)
│  └─ app/src/main/java/com/gathin/tvhome/
│     ├─ MainActivity.java       Layar utama + semua pengaturan
│     ├─ ThemeManager.java       3 tema Swadaya (default, malam, terang)
│     ├─ SoccerData/FotMob       Klasemen & Jadwal Liga 1/EPL/dll
│     ├─ KemenagPrayer.java      Kalkulator sholat offline Kemenag
│     ├─ DailyAyat.java          Ayat/Doa harian offline
│     ├─ OnlineWallpapers.java   Wallpaper HD Unsplash/Pexels
│     └─ ... (cuaca, Now Playing, dll)
├─ addons/              Addon kartu modul (cuaca, sholat, koin, jam dunia, ...)
├─ themes/              Config tema (swadaya, swadaya-malam, swadaya-terang)
├─ tools/               Script debloat/install/theme di terminal
├─ docs/                Riset addon & panduan (Bahasa Indonesia)
├─ .github/workflows/   GitHub Actions build APK + OTA release
└─ release/version.json Metadata OTA
```

---

## Build Sendiri dari Source

Kalau kamu pengen compile ulang sendiri:

1. Install **JDK 17** dan **Android SDK 34 + build-tools 34**
2. Klon repo ini, buka folder `launcher/`
3. Build:
   ```bash
   cd launcher
   ./gradlew --no-daemon :app:assembleRelease
   # APK ada di app/build/outputs/apk/release/app-release.apk
   ```
4. Install via `adb install -r app-release.apk` atau upload ke Release GitHub

---

## Bahasa

Utama: **Bahasa Indonesia** (santai, gampang dimengerti orang awam). Tambahan: **US English** (`values-en-rUS`) — banyak menu udah ada terjemahan Inggrisnya. Sistem mengikuti bahasa HP/TV otomatis.

---

## Kredit & Sumber

SwadayaTV dibangun di atas arsitektur launcher Android TV dari [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (Gathin23). Terima kasih kepada pengembang aslinya atas fondasi yang solid.

Pustaka dan layanan pihak ketiga yang dipakai:

- Data bola: FotMob
- Cuaca dan indeks kualitas udara: Open-Meteo
- Kurs koin: Frankfurter
- Doa harian: GitHub quran-json (iherbs)
- Wallpaper HD: Unsplash dan Pexels
- Font: Outfit — SIL Open Font License 1.1

Pertanyaan atau laporan bug silakan buka di halaman Issues.