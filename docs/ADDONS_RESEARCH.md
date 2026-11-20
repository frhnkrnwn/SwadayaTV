# Riset Addon CleanYourTV untuk SwadayaTV

Dokumen ini merangkum hasil penelusuran saya di folder `/tmp/clean/addons` — daftar addon bawaan CleanYourTV, lalu fokus mana saja yang akan kita ambil untuk SwadayaTV v1. Semua ditulis dalam Bahasa Indonesia.

Semua addon CleanYourTV adalah aplikasi Android TV kecil yang terpisah dari launcher. Cara tersambungnya rapi: masing-masing mendeklarasikan sebuah **module card** di `AndroidManifest.xml` melalui `intent-filter` kategori `com.gathin.tvhome.category.DOCK_CARD` plus `meta-data com.gathin.tvhome.card` yang menunjuk ke `res/xml/module_card.xml`. Launcher menemukan card itu, bertanya sekali ("Show card?"), lalu menampilkannya sebagai kartu di Home dengan **label + caption + accent (warna glow)** dari file itu. Jadi launcher tidak memerlukan perubahan kode untuk menambah addon baru — cukup pasang APK-nya.

## Daftar addon CleanYourTV (yang dibahas di sini)

Berikut 11 addon yang Anda minta, lengkap dengan package, permission, label/caption/accent, dan fitur ringkasnya.

### 1. Weather (simplesource)

- **Package:** `io.github.sumyabazar1.weather`
- **Permission (`app/src/main/AndroidManifest.xml`):** `INTERNET` (hanya itu; untuk mengambil data Open-Meteo)
- **Module card (`app/src/main/res/xml/module_card.xml`):** label `Weather`, caption `Forecast and air quality`, accent `#4A90D9` (biru langit)
- **Fitur ringkas:** cuaca satu kota layar penuh: suhu sekarang, suhu terasa, kondisi, AQI AS berupa pill berwarna, dan 3 hari ke depan. Kota dipilih sekali memakai keyboard TV, lalu disimpan. Datanya dari **Open-Meteo** (ICON untuk cuaca, CAMS untuk kualitas udara, tanpa API key). Diperbarui tiap 30 menit selama layarnya terbuka. Ada pengaturan untuk unit °C/°F, jumlah hari prakiraan (3–7), kelembapan, dan angin.

### 2. Prayer Times (Waktu Salat)

- **Package:** `com.majnioui.tv.prayer`
- **Permission:** `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `SCHEDULE_EXACT_ALARM`, `SYSTEM_ALERT_WINDOW`
- **Module card:** label `Prayer Times`, caption `Next prayer`, accent `#2BB673` (hijau)
- **Fitur ringkas:** 5 waktu salat + matahari terbit untuk satu kota, salat berikutnya disorot plus hitung mundur per menit, tanggal Masehi + Hijriah, nama Arab + Inggris untuk tiap salat. Datanya dari **Aladhan** (tanpa key). Kota, koordinat, metode hitung (`method`, misalnya 21 Maroko, 20 Kemenag Indonesia), dan mazhab Asr (`school`) diatur di `res/values/config.xml` setelah build. Jika switch `reminders` dinyalakan, ada alert card kecil di pojok kanan atas saat waktu salat tiba. Diperbarui saat layar dibuka dan setelah tengah malam; offline tetap menampilkan waktu terakhir.

### 3. World Clock (Jam Dunia)

- **Package:** `io.github.savioronezero.worldclock`
- **Permission:** tidak ada (pure offline)
- **Module card:** label `World Clock`, caption `Local and away`, accent `#83D6DE` (toska muda)
- **Fitur ringkas:** 1–6 jam offline, default Local + London + Sydney. Tiap jam menampilkan waktu, tanggal lokal, offset UTC, dan selisih hari dari jam TV. Mengikuti zona waktu TV, termasuk DST dan zona setengah jam. Bisa menambah kota dari katalog IANA, mengubah format 12/24 jam, dan mengurutkan. Ada mode screensaver bawaannya sendiri (peta dunia, starfield, gradient, hitam).

### 4. Capacity Stats (Statistik TV)

- **Package:** `com.lionlev.tv.capacity`
- **Permission:** tidak ada
- **Module card:** label `Capacity Stats`, caption `RAM and storage`, accent `#74A987` (hijau sage)
- **Fitur ringkas:** bacaan langsung RAM (available/total dan used/total) dan penyimpanan (available/total dan used/total) dari `/proc/meminfo` serta kapasitas filesystem data. Diperbarui tiap 10 detik hanya selama layarnya terbuka, tidak ada layanan latar belakang. UI Inggris + Ibrani (RTL untuk bahasa lain mengikuti standar). Diuji di Sony KD-50X85J Android TV 12.

### 5. Photo Screensaver (Screensaver Foto)

- **Package:** `com.gathin.photodream`
- **Permission:** `ACCESS_COARSE_LOCATION`, `INTERNET` (koordinat kasar + internet hanya terpakai jika opsi cuaca dinyalakan)
- **Module card:** label `Photos`, caption `Slideshow`, accent `#5B8DEF` (biru)
- **Fitur ringkas:** screensaver sistem Android (`DreamService`) untuk foto dan klip MP4 Anda, urut nama file, zoom + drift pelan, crossfade tiap 20 detik (bisa diatur). Di Home CleanYourTV, OK pada card langsung memulai slideshow, tahan OK / Menu membuka pengaturan. Opsi: jam + tanggal, tanggal foto dari nama file/EXIF, 3 print miring di langit-langit, cuaca mini dari Open-Meteo.

### 6. Game Day

- **Package:** `io.github.justinnewman.gameday`
- **Permission:** `INTERNET` (hanya ke tautan kalender yang Anda tambahkan)
- **Module card:** label `Game Day`, caption `Your teams' next games`, accent `#3CBE78` (hijau)
- **Fitur ringkas:** jadwal pertandingan berikutnya untuk tim Anda, dihitung mundur ("Puck drop in 2h 18m", lalu "Live now"), diambil dari kalender (iCal link) yang Anda daftarkan — tim apa pun, olahraga apa pun, tanpa skor. Di Home, launcher juga bisa menampilkan jadwal bergantian di panel F1 lewat key `game_day`. Jika tidak dipakai, abaikan saja.

### 7. Sleep Timer

- **Package:** `io.github.luyzfernando08.sleeptimer`
- **Permission:** `SCHEDULE_EXACT_ALARM`, `SYSTEM_ALERT_WINDOW` (di `AndroidManifest.xml`; di README disebut juga accessibility untuk aksi lock-screen saja)
- **Module card:** label `Sleep Timer`, caption *(tidak ada, hanya label)*, accent `#7A6FF0` (ungu)
- **Fitur ringkas:** timer tidur 15/30/60/90/120 menit; saat tersisa 1 menit muncul hitung mundur kecil di atas apa pun yang sedang diputar dengan tombol "15 more minutes" dan "Turn off now". Jika tidak ada yang menjawab, TV standby. Back menutup peringatan tetapi timer tetap berjalan.

### 8. Brightness

- **Package:** `io.github.maxhayim.brightness`
- **Permission:** `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`
- **Module card:** label `Brightness`, caption `Night mode`, accent `#F5A524` (amber)
- **Fitur ringkas:** meredupkan gambar di bawah batas minimum pengaturan TV, di semua aplikasi, Live TV, dan Home. Ada night mode hangat terjadwal, slider brightness 1–100%, quick-bar di bawah untuk mengubah dengan Left/Right, dan toggle malam satu tombol. Di TV berbasis Realtek ia juga menurunkan backlight/kontras TV aslinya. Tetap hidup kembali setelah reboot.

### 9. Sources

- **Package:** `io.github.maxhayim.sources`
- **Permission:** tidak ada
- **Module card:** label `Devices` (sengaja bukan "Sources" agar tidak bertabrakan dengan label Inputs bawaan launcher di bahasa Prancis), caption `Sources by name`, accent `#3DDC84` (hijau)
- **Fitur ringkas:** daftar input TV memakai nama dari perangkatnya sendiri lewat HDMI-CEC — "PlayStation 5" bukan "HDMI 2". Yang tersambung ditampilkan lebih dulu secara penuh, OK untuk mengganti sumber, plus Live TV. Namanya bisa diganti dari pengaturan TV.

### 10. Weather (pendamping) / Photo Prints

Dua addon teman yang sering muncul bersama Weather & Photo Screensaver — saya bahas sekalian di sini agar tidak bingung:

- **Package:** `io.github.shivamsingh262.photoprints`
- **Permission:** tidak ada
- **Module card:** label `Photo Prints`, caption *(tidak ada)*, accent `#E8A33D` (kuning mustard)
- **Fitur ringkas:** 3 foto Anda sekaligus sebagai print miring di atas langit sesuai waktu hari; tiap 15 detik bergeser ke kiri dan 3 foto baru masuk dari kanan. Ada baris tanggal di bawah. Tetap utuh, tidak pernah dipotong. Komponen visualnya (print miring, langit, tanggal foto) dipakai juga oleh Photo Screensaver.

### 11. Startup & Branding

- **Package:** `io.github.maxhayim.startupbranding`
- **Permission:** tidak ada
- **Module card:** label `Startup & Branding`, caption `Welcome screen`, accent `#F2B84B` (kuning)
- **Fitur ringkas:** layar sambutan Anda sendiri: gambar (logo, foto keluarga, nama ruangan), warna background, dan satu baris teks seperti "Welcome to the Den", layar penuh beberapa detik atau sampai Back/OK. Ada live preview 16:9, skala Fit/Fill/Stretch/Original, pilihan posisi gambar, dan pemeriksaan kompatibilitas tanpa root.

## Fokus SwadayaTV v1 (yang akan kita ambil)

Enam ini saja yang disepakati untuk v1 pada Oktober 2026. Sisanya (Game Day, Sleep Timer, Brightness, Sources, Photo Prints, Startup & Branding) kita tunda dulu — dokumennya tetap di sini untuk referensi jika nanti ingin menambah.

| Prioritas v1 | Acuan CleanYourTV | Adaptasi Swadaya |
|---|---|---|
| **Cuaca** | Weather (`io.github.sumyabazar1.weather`) | Layar cuaca 1 kota, sumber **Open-Meteo**, tanpa API key. Bahasa + unit mengikuti TV. |
| **Waktu salat** | Prayer Times (`com.majnioui.tv.prayer`) | Hitung **Kemenag offline** (metode 20) memakai koordinat kota, bukan menarik data Aladhan terus-menerus. Tetap menampilkan Masehi + Hijriah. |
| **Kurs koin** | *(tidak ada padanannya di CleanYourTV)* | Fitur baru Swadaya: kurs dari **Frankfurter** (tanpa key), kartu kecil di Home. Card label `Koin`, caption `Kurs tukar`. |
| **Jam dunia** | World Clock (`io.github.savioronezero.worldclock`) | Jam offline 1–6 zona waktu, memakai katalog IANA di TV. |
| **Statistik TV** | Capacity Stats (`com.lionlev.tv.capacity`) | RAM + penyimpanan langsung dari `/proc/meminfo`, tanpa permission. |
| **Screensaver foto** | Photo Screensaver (`com.gathin.photodream`) | Slideshow foto + MP4 Anda, zoom/drift pelan, crossfade 20 detik. Cuaca mini opsional. |

Semua data dibaca langsung dari `/tmp/clean/addons/<nama>/app/src/main/AndroidManifest.xml` (permission), `res/xml/module_card.xml` (label/caption/accent), `README.md` (fitur), dan `app/build.gradle` (applicationId) — bukan dari ingatan.
