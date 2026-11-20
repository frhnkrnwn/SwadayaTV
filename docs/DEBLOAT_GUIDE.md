# Panduan Debloat + Pemasangan Manual SwadayaTV

Panduan langkah demi langkah untuk Anda yang ingin memasang SwadayaTV sendiri di TV Android / Google TV, tanpa coding assistant. Saya merangkum dengan bahasa saya sendiri dari README CleanYourTV bagian "Doing it by hand" dan skrip di `tools/` (disable.sh, restore.sh, tv.py, theme.sh) — di SwadayaTV skripnya sama persis, jadi perintahnya tinggal copy-paste. Tidak menyalin mentah lebih dari 100 baris, santai saja membacanya.

Prinsipnya mudah: **tidak ada yang di-uninstall selamanya**, hanya **di-disable** per user 0, dan selalu ada jalan kembali. Skripnya juga tidak akan meninggalkan TV tanpa Home screen. Semua membutuhkan komputer yang berada di Wi-Fi yang sama dengan TV.

## 1. Persiapan di komputer

Anda membutuhkan tiga hal:

| Kebutuhan | Untuk apa |
|---|---|
| **Python 3** (versi 3.x apa saja) | Untuk `tools/`-nya. Perintah venv di bawah sekaligus memasang dependensinya. |
| **JDK 17 s/d 22** (full JDK, termasuk `jlink`) | Untuk build launcher & addon. JDK 23+ membuat Gradle 8.9 berhenti. |
| **Android SDK platform 34 + build-tools 34.0.0 atau 34.x terbaru** | Platform 34 yang dibaca build-nya; platform lebih baru tidak menggantikan. |

Di macOS tanpa semua itu: `brew install openjdk@17 android-commandlinetools`, lalu `sdkmanager "platforms;android-34" "build-tools;34.0.0"`. Di Linux, skrip build mencari SDK di `~/Android/Sdk` dan `java` di PATH Anda.

Jika komputer Anda tidak memiliki JDK/SDK sama sekali, ada **Docker**: `tools/docker-build.sh` sudah menyiapkan image JDK 17 + SDK 34, Anda tinggal memasang hasilnya. Detailnya di `tools/docker-build.md`.

## 2. Siapkan venv Python

Dari root folder SwadayaTV:

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
```

## 3. Sambungkan ke TV lewat ADB Wi-Fi

`tools/tv.py` berbicara ke network ADB di port **5555**. Dua cara untuk membuka port-nya:

1. **Cara biasa**: nyalakan "Wireless debugging" di TV (Chromecast with Google TV dan Google TV Streamer memilikinya dengan pairing code). Pair sekali memakai `adb` bawaan, lalu jalankan `adb tcpip 5555` — port-nya terbuka sampai TV reboot.
2. **Port 5555 tetap tertutup**: arahkan `TV_ADB_BINARY` ke `adb` yang sama dan `TV_HOST` ke port yang ditampilkan layar Wireless debugging. Tools tetap bisa tersambung lewat koneksi pairing terenkripsi. Langkah pair lengkap ada di `docs/TROUBLESHOOTING.md`.

## 4. Cari IP TV Anda

Anda membutuhkan IP TV yang berada di jaringan yang sama dengan komputer. Mudahnya:

- Di TV: **Settings → About** (atau Network), lihat IPv4 address di sana.
- Di komputer: daftar perangkat di Wi-Fi yang sama (router, aplikasi Fing, atau `arp -a`).
- Simpan ke variabel agar tidak mengetik ulang:

```bash
export TV_HOST=192.168.1.50   # ganti dengan IP TV Anda
```

## 5. Periksa koneksi + lihat daftar aplikasi

```bash
tools/venv/bin/python -I tools/tv.py --probe                 # model, Android, firmware
tools/venv/bin/python -I tools/tv.py 'pm list packages'      # semua package di TV
```

`tv.py` juga bisa untuk shell command lain, `--push/--pull` file kecil, `--batch`, `--screenshot`, dan `--reboot`. Help-nya: `tools/venv/bin/python -I tools/tv.py --help` (tidak membutuhkan venv/TV untuk itu).

## 6. Debloat (nonaktifkan aplikasi bawaan)

Ambil daftar debloat untuk model TV Anda — contohnya ada di `examples/<model>/debloat.txt`, satu package per baris, `#` untuk komentar:

```bash
tools/disable.sh examples/<model>/debloat.txt
```

Yang dilakukannya: catat dulu keadaan sekarang (package yang sudah nonaktif tetap nonaktif), lalu nonaktifkan sisanya per user 0. Ia menolak daftar yang isinya bukan nama package murni, menolak jika daftar kosong/semua komentar, dan menolak package inti Android/middleware TV karena bisa membuat TV tidak boot. Ia juga melewati aplikasi yang sedang memegang Home jika belum ada launcher lain yang menjawab HOME — jadi TV Anda tidak pernah tanpa Home screen. Override daruratnya: `FORCE_HOME=1` atau `FORCE_SETUPWRAITH=1` (misalnya di Ethernet), tetapi gunakan itu dengan sadar.

## 7. Pasang APK SwadayaTV (launcher)

```bash
TV_HOST=192.168.1.50 launcher/build-and-install.sh
```

Skrip ini membangun release shrunk (~572 KB), memasangnya, mengganti Home ke launcher baru, dan mengembalikan jika Home tidak mau pindah. Di PowerShell: `powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.50`. Setelah ini, jalankan ulang daftar debloat Anda sekali lagi agar entri stock launcher yang tadinya dilewati sekarang nonaktif — atau pakai `FORCE_HOME=1`.

## 8. Kembalikan jika gagal (batalkan debloat)

Gunakan daftar yang **sama persis** seperti saat menonaktifkan:

```bash
tools/restore.sh examples/<model>/debloat.txt
```

Di Google TV ia juga mengembalikan stock launcher (`com.google.android.apps.tv.launcherx` secara default; di PowerShell berikan `-Stock com.google.android.apps.tv.launcherx`). Exit code 0 = keadaan kembali, 1 = tidak selesai, 2 = perintahnya salah dibaca (tidak ada yang terkirim ke TV).

## 9. Push tema memakai `tools/theme.sh`

Tema adalah folder berisi `config.json` (+ opsional `wallpaper.jpg` / folder `wallpapers/`). Push tema Swadaya Anda:

```bash
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya          # push + restart Home
TV_HOST=192.168.1.50 tools/theme.sh --reset                 # hapus overlay, kembali ke config.xml bawaan
```

Di PowerShell: `tools\theme.ps1 -TvHost 192.168.1.50 themes\aurora`. Push tema juga menghapus pilihan kolom/motion yang Anda set di TV, agar konsisten dengan tema baru. Folder `themes/swadaya`, `themes/swadaya-malam`, `themes/swadaya-terang` di SwadayaTV sudah siap di repo ini — tinggal push yang Anda suka.

## 10. Build & push addon

Untuk menambah kartu cuaca, salat, koin, jam dunia, statistik, atau screensaver foto (dari folder `addons/<nama>/`):

```bash
cd addons/<nama> && ./gradlew assembleRelease && cd ../..
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py --push addons/<nama>/app/build/outputs/apk/release/app-release.apk /data/local/tmp/addon.apk
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'pm install -r /data/local/tmp/addon.apk; rm -f /data/local/tmp/addon.apk'
```

Karena addon mendeklarasikan module card, launcher akan bertanya sekali ("Show card?") setelah itu muncul sebagai kartu. Di Windows pakai `gradlew.bat assembleRelease` dan `tools\venv\Scripts\python.exe`. Atau build di Docker: `tools/docker-build.sh addons/<nama>`.

## Ringkasan alur lengkap

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=192.168.1.50
tools/venv/bin/python -I tools/tv.py --probe
tools/disable.sh examples/<model>/debloat.txt
TV_HOST=192.168.1.50 launcher/build-and-install.sh
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya
# kembalikan jika perlu: tools/restore.sh examples/<model>/debloat.txt
```

Selamat mencoba TV-nya! Jika buntu, baca `docs/TROUBLESHOOTING.md` di repo aslinya.
