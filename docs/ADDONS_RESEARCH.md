# Riset Addon CleanYourTV untuk SwadayaTV

Halo! Dokumen ini hasil utak-atik aku di folder `/tmp/clean/addons` — daftar addon bawaan CleanYourTV, terus fokus mana aja yang mau kita ambil buat SwadayaTV v1. Semua Bahasa Indonesia santai ya, pakai "kamu".

Semua addon di CleanYourTV itu aplikasi Android TV kecil yang terpisah dari launcher. Cara nyambungnya rapi: masing-masing declare sebuah **module card** di `AndroidManifest.xml` lewat `intent-filter` kategori `com.gathin.tvhome.category.DOCK_CARD` plus `meta-data com.gathin.tvhome.card` yang nunjuk ke `res/xml/module_card.xml`. Launcher-nya nemu card itu, nanya sekali ("Show card?"), terus nongol sebagai tile di Home pakai **label + caption + accent (warna glow)** dari file itu. Jadi launcher nggak butuh ubahan kode buat nambah addon baru — cukup pasang APK-nya.

## Daftar addon CleanYourTV (yang dibahas di sini)

Berikut 11 addon yang kamu minta, lengkap dengan package, permission, label/caption/accent, dan fitur ringkasnya.

### 1. Weather (simplesource)

- **Package:** `io.github.sumyabazar1.weather`
- **Permission (`app/src/main/AndroidManifest.xml`):** `INTERNET` (cuma itu; buat ngambil data Open-Meteo)
- **Module card (`app/src/main/res/xml/module_card.xml`):** label `Weather`, caption `Forecast and air quality`, accent `#4A90D9` (biru langit)
- **Fitur ringkas:** cuaca satu kota full-screen: suhu sekarang, feels-like, kondisi, AQI AS berupa pill berwarna, dan 3 hari ke depan. Kota dipilih sekali pakai keyboard TV, terus disimpan. Datanya dari **Open-Meteo** (ICON buat cuaca, CAMS buat kualitas udara, tanpa API key). Update tiap 30 menit selama layarnya kebuka. Ada setting buat unit °C/°F, jumlah hari forecast (3–7), humidity, wind.

### 2. Prayer Times (Waktu Sholat)

- **Package:** `com.majnioui.tv.prayer`
- **Permission:** `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `SCHEDULE_EXACT_ALARM`, `SYSTEM_ALERT_WINDOW`
- **Module card:** label `Prayer Times`, caption `Next prayer`, accent `#2BB673` (hijau)
- **Fitur ringkas:** 5 waktu sholat + sunrise buat satu kota, sholat berikutnya di-highlight plus countdown per menit, tanggal Masehi + Hijriah, nama Arab + Inggris buat tiap sholat. Datanya dari **Aladhan** (tanpa key). Kota, koordinat, metode hitung (`method`, misal 21 Maroko, 20 Kemenag Indonesia), dan mazhab Asr (`school`) disetting di `res/values/config.xml` pasca build. Kalau switch `reminders` dinyalain, ada alert card kecil di pojok kanan atas pas waktu sholat datang. Update pas layar dibuka dan setelah tengah malam; offline tetap nampilin waktu terakhir.

### 3. World Clock (Jam Dunia)

- **Package:** `io.github.savioronezero.worldclock`
- **Permission:** tidak ada (pure offline)
- **Module card:** label `World Clock`, caption `Local and away`, accent `#83D6DE` (toska muda)
- **Fitur ringkas:** 1–6 jam offline, default Local + London + Sydney. Tiap jam nampilin waktu, tanggal lokal, offset UTC, dan selisih hari dari jam TV. Ikutin timezone TV, termasuk DST dan zona setengah jam. Bisa tambah kota dari katalog IANA, ubah format 12/24 jam, urut-urutin. Ada mode screensaver bawaannya sendiri (peta dunia, starfield, gradient, hitam).

### 4. Capacity Stats (Statistik TV)

- **Package:** `com.lionlev.tv.capacity`
- **Permission:** tidak ada
- **Module card:** label `Capacity Stats`, caption `RAM and storage`, accent `#74A987` (hijau sage)
- **Fitur ringkas:** bacaan live RAM (available/total dan used/total) sama storage (available/total dan used/total) langsung dari `/proc/meminfo` dan kapasitas filesystem data. Refresh tiap 10 detik cuma selama layarnya kebuka, nggak ada background service. UI Inggris + Ibrani (RTL buat bahasa lain ikut standar). Diuji di Sony KD-50X85J Android TV 12.

### 5. Photo Screensaver (Screensaver Foto)

- **Package:** `com.gathin.photodream`
- **Permission:** `ACCESS_COARSE_LOCATION`, `INTERNET` (koordinat kasar + internet cuma kepake kalau opsi cuaca dinyalain)
- **Module card:** label `Photos`, caption `Slideshow`, accent `#5B8DEF` (biru)
- **Fitur ringkas:** screensaver sistem Android (`DreamService`) buat foto-foto dan klip MP4 kamu, urut nama file, zoom + drift pelan, crossfade tiap 20 detik (bisa diatur). Di Home CleanYourTV, OK di card-nya langsung mulai slideshow, tahan OK / Menu buka settings. Opsi: jam + tanggal, tanggal foto dari nama file/EXIF, 3 print miring di langit-langit, cuaca mini dari Open-Meteo.

### 6. Game Day

- **Package:** `io.github.justinnewman.gameday`
- **Permission:** `INTERNET` (cuma ke link kalender yang kamu tambahin)
- **Module card:** label `Game Day`, caption `Your teams' next games`, accent `#3CBE78` (hijau)
- **Fitur ringkas:** jadwal game berikutnya buat tim-tim kamu, di-countdown ("Puck drop in 2h 18m", terus "Live now"), diambil dari kalender (iCal link) yang kamu daftarin — tim apapun, olahraga apapun, tanpa skor. Di Home, launcher-nya juga bisa nampilin jadwal gantian di panel F1 lewat key `game_day`. Kalau kamu nggak pakai, abaikan aja.

### 7. Sleep Timer

- **Package:** `io.github.luyzfernando08.sleeptimer`
- **Permission:** `SCHEDULE_EXACT_ALARM`, `SYSTEM_ALERT_WINDOW` (di `AndroidManifest.xml`; di README disebut juga accessibility buat aksi lock-screen saja)
- **Module card:** label `Sleep Timer`, caption *(tidak ada, cuma label)*, accent `#7A6FF0` (ungu)
- **Fitur ringkas:** timer tidur 15/30/60/90/120 menit, pas udah 1 menit lagi muncul countdown kecil di atas apa pun yang lagi diputar dengan tombol "15 more minutes" dan "Turn off now". Kalau nggak ada yang jawab, TV standby. Back nutup warning tapi timer tetap jalan.

### 8. Brightness

- **Package:** `io.github.maxhayim.brightness`
- **Permission:** `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`
- **Module card:** label `Brightness`, caption `Night mode`, accent `#F5A524` (amber)
- **Fitur ringkas:** ngeredupin gambar di bawah batas minimum setting TV, di semua aplikasi, Live TV, dan Home. Ada night mode hangat terjadwal, slider brightness 1–100%, quick-bar di bawah buat ubah pakai Left/Right, dan toggle malam satu tombol. Di TV berbasis Realtek dia juga nurunin backlight/kontras TV aslinya. Tetap hidup balik setelah reboot.

### 9. Sources

- **Package:** `io.github.maxhayim.sources`
- **Permission:** tidak ada
- **Module card:** label `Devices` (sengaja bukan "Sources" biar nggak nabrak label Inputs bawaan launcher di bahasa Prancis), caption `Sources by name`, accent `#3DDC84` (hijau)
- **Fitur ringkas:** daftar input TV pakai nama dari perangkatnya sendiri lewat HDMI-CEC — "PlayStation 5" bukan "HDMI 2". Yang nyambung ditampilin dulu secara penuh, OK buat ganti sumber, plus Live TV. Namanya bisa diganti dari setting TV.

### 10. Weather (pendamping) / Photo Prints

Dua addon temen yang sering muncul bareng Weather & Photo Screensaver — kubahas sekalian di sini biar nggak bingung:

- **Package:** `io.github.shivamsingh262.photoprints`
- **Permission:** tidak ada
- **Module card:** label `Photo Prints`, caption *(tidak ada)*, accent `#E8A33D` (kuning mustard)
- **Fitur ringkas:** 3 foto kamu sekaligus sebagai print miring di atas langit sesuai waktu hari, tiap 15 detik geser ke kiri dan 3 foto baru masuk dari kanan. Ada baris tanggal di bawah. Tetap utuh, nggak pernah di-crop. Komponen visualnya (print miring, langit, tanggal foto) dipakai juga sama Photo Screensaver.

### 11. Startup & Branding

- **Package:** `io.github.maxhayim.startupbranding`
- **Permission:** tidak ada
- **Module card:** label `Startup & Branding`, caption `Welcome screen`, accent `#F2B84B` (kuning)
- **Fitur ringkas:** layar sambutan kamu sendiri: gambar (logo, foto keluarga, nama ruangan), warna background, dan satu baris teks kayak "Welcome to the Den", full-screen beberapa detik atau sampai Back/OK. Ada live preview 16:9, skala Fit/Fill/Stretch/Original, pilihan posisi gambar, dan cek kompatibilitas tanpa root.

## Fokus SwadayaTV v1 (yang mau kita ambil)

Enam ini aja yang Oktober 2026 kemarin disepakati buat v1. Sisanya (Game Day, Sleep Timer, Brightness, Sources, Photo Prints, Startup & Branding) kita parkir dulu — dokumennya tetap di sini buat referensi kalau nanti mau nambah.

| Prioritas v1 | Acuan CleanYourTV | Adaptasi Swadaya |
|---|---|---|
| **Cuaca** | Weather (`io.github.sumyabazar1.weather`) | Layar cuaca 1 kota, sumber **Open-Meteo**, tanpa API key. Bahasa + unit ikut TV. |
| **Waktu sholat** | Prayer Times (`com.majnioui.tv.prayer`) | Hitung **Kemenag offline** (metode 20) pakai koordinat kota, bukan nge-pull Aladhan terus. Tetap tampilkan Masehi + Hijriah. |
| **Kurs koin** | *(tidak ada padanannya di CleanYourTV)* | Fitur baru Swadaya: kurs dari **Frankfurter** (tanpa key), tile kecil di Home. Card label `Koin`, caption `Kurs tukar`. |
| **Jam dunia** | World Clock (`io.github.savioronezero.worldclock`) | Jam offline 1–6 zona waktu, pakai katalog IANA di TV. |
| **Statistik TV** | Capacity Stats (`com.lionlev.tv.capacity`) | RAM + storage live dari `/proc/meminfo`, tanpa permission. |
| **Screensaver foto** | Photo Screensaver (`com.gathin.photodream`) | Slideshow foto + MP4 kamu, zoom/drift pelan, crossfade 20 detik. Cuaca mini opsional. |

Semua data dibaca langsung dari `/tmp/clean/addons/<nama>/app/src/main/AndroidManifest.xml` (permission), `res/xml/module_card.xml` (label/caption/accent), `README.md` (fitur), dan `app/build.gradle` (applicationId) ya — bukan dari ingatan.
