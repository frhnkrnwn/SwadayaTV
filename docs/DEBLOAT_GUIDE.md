# Panduan Debloat + Install Manual SwadayaTV

Halo! Ini panduan langkah-demi-langkah buat kamu yang mau masang SwadayaTV sendiri di TV Android / Google TV kamu, tanpa coding assistant. Aku rangkum dengan bahasaku sendiri dari README CleanYourTV bagian "Doing it by hand" dan script-script di `tools/` (disable.sh, restore.sh, tv.py, theme.sh) — di SwadayaTV script-nya sama persis, jadi perintahnya tinggal copy-paste aja. Nggak nempel mentah lebih dari 100 baris, santai aja dibacanya.

Prinsipnya gampang: **nggak ada yang di-uninstall selamanya**, cuma **di-disable** per user 0, dan selalu ada jalan baliknya. Script-nya juga nggak akan ninggalin TV tanpa Home screen. Semua butuh komputer yang satu Wi-Fi sama TV-nya.

## 1. Siapan di komputer

Kamu butuh tiga hal:

| Kebutuhan | Buat apa |
|---|---|
| **Python 3** (versi 3.x apa aja) | Buat `tools/`-nya. Perintah venv di bawah sekalian pasang dependensinya. |
| **JDK 17 s/d 22** (full JDK, termasuk `jlink`) | Buat build launcher & addon. JDK 23+ bikin Gradle 8.9 berhenti. |
| **Android SDK platform 34 + build-tools 34.0.0 atau 34.x terbaru** | Platform 34 yang dibaca build-nya; platform lebih baru tidak menggantikan. |

Di macOS tanpa itu semua: `brew install openjdk@17 android-commandlinetools`, terus `sdkmanager "platforms;android-34" "build-tools;34.0.0"`. Di Linux, script build-nya cari SDK di `~/Android/Sdk` dan `java` di PATH kamu.

Kalau komputer kamu nggak ada JDK/SDK sama sekali, ada **Docker**: `tools/docker-build.sh` sudah siapin image JDK 17 + SDK 34, kamu tinggal install hasilnya. Detailnya di `tools/docker-build.md`.

## 2. Siapkan venv Python

Dari root folder SwadayaTV:

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
```

## 3. Sambungin ke TV lewat ADB Wi-Fi

`tools/tv.py` ngomong ke network ADB di port **5555**. Dua cara buat buka port-nya:

1. **Cara biasa**: nyalain "Wireless debugging" di TV (Chromecast with Google TV dan Google TV Streamer punya ini dengan pairing code). Pair sekali pakai `adb` bawaan, terus jalanin `adb tcpip 5555` — port-nya kebuka sampai TV reboot.
2. **Port 5555 tetap ketutup**: arahkan `TV_ADB_BINARY` ke `adb` yang sama dan `TV_HOST` ke port yang ditampilin layar Wireless debugging. Tools-nya tetap bisa nyambung lewat koneksi pairing terenkripsi. Langkah pair lengkapnya ada di `docs/TROUBLESHOOTING.md`.

## 4. Cari IP TV kamu

Kamu butuh IP TV yang satu jaringan sama komputer. Gampangnya:

- Di TV: **Settings → About** (atau Network), lihat IPv4 address di sana.
- Di komputer: daftar perangkat di Wi-Fi yang sama (router, aplikasi Fing, atau `arp -a`).
- Simpan ke variabel biar nggak ngetik ulang:

```bash
export TV_HOST=192.168.1.50   # ganti sama IP TV kamu
```

## 5. Cek koneksi + lihat daftar aplikasi

```bash
tools/venv/bin/python -I tools/tv.py --probe                 # model, Android, firmware
tools/venv/bin/python -I tools/tv.py 'pm list packages'      # semua package di TV
```

`tv.py` juga bisa buat shell command lain, `--push/--pull` file kecil, `--batch`, `--screenshot`, dan `--reboot`. Help-nya: `tools/venv/bin/python -I tools/tv.py --help` (nggak butuh venv/TV buat itu).

## 6. Debloat (disable aplikasi bawaan)

Ambil list debloat buat model TV kamu — contohnya ada di `examples/<model>/debloat.txt`, satu package per baris, `#` buat komentar:

```bash
tools/disable.sh examples/<model>/debloat.txt
```

Yang dia lakukan: catat dulu state sekarang (package yang udah ke-disable tetap ke-disable), terus disable sisanya per user 0. Dia nolak list yang isinya bukan nama package murni, nolak kalau list kosong/komentar semua, dan nolak package inti Android/middleware TV karena bisa bikin TV nggak boot. Dia juga skip aplikasi yang lagi pegang Home kalau belum ada launcher lain yang menjawab HOME — jadi TV kamu nggak pernah tanpa Home screen. Override daruratnya: `FORCE_HOME=1` atau `FORCE_SETUPWRAITH=1` (misal di Ethernet), tapi pakai itu dengan sadar ya.

## 7. Install APK SwadayaTV (launcher)

```bash
TV_HOST=192.168.1.50 launcher/build-and-install.sh
```

Script ini build release shrunk (~572 KB), install, ganti Home ke launcher baru, dan rollback kalau Home nggak mau pindah. Di PowerShell: `powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.50`. Setelah ini, jalanin ulang list debloat kamu sekali lagi biar entri stock launcher yang tadinya di-skip sekarang ke-disable — atau pakai `FORCE_HOME=1`.

## 8. Restore kalau gagal (batalkan debloat)

Pakai list yang **sama persis** kayak pas disable:

```bash
tools/restore.sh examples/<model>/debloat.txt
```

Di Google TV dia juga balikin stock launcher (`com.google.android.apps.tv.launcherx` secara default; di PowerShell kasih `-Stock com.google.android.apps.tv.launcherx`). Exit code 0 = state balik, 1 = nggak selesai, 2 = perintahnya salah baca (nggak ada yang terkirim ke TV).

## 9. Push tema pakai `tools/theme.sh`

Tema itu folder berisi `config.json` (+ opsional `wallpaper.jpg` / folder `wallpapers/`). Push tema Swadaya kamu:

```bash
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya          # push + restart Home
TV_HOST=192.168.1.50 tools/theme.sh --reset                 # hapus overlay, balik ke config.xml bawaan
```

Di PowerShell: `tools\theme.ps1 -TvHost 192.168.1.50 themes\aurora`. Push tema juga ngapus pilihan kolom/motion yang kamu set di TV, biar konsisten sama tema baru. Folder `themes/swadaya`, `themes/swadaya-malam`, `themes/swadaya-terang` di SwadayaTV sudah siap di repo ini — tinggal push yang kamu suka.

## 10. Build & push addon

Buat nambah tile cuaca, sholat, koin, jam dunia, statistik, atau screensaver foto (dari folder `addons/<nama>/`):

```bash
cd addons/<nama> && ./gradlew assembleRelease && cd ../..
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py --push addons/<nama>/app/build/outputs/apk/release/app-release.apk /data/local/tmp/addon.apk
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'pm install -r /data/local/tmp/addon.apk; rm -f /data/local/tmp/addon.apk'
```

Karena addon-nya nge-declare module card, launcher bakal nanya sekali ("Show card?") habis itu muncul sebagai tile. Di Windows pakai `gradlew.bat assembleRelease` dan `tools\venv\Scripts\python.exe`. Atau build di Docker: `tools/docker-build.sh addons/<nama>`.

## Ringkasan alur lengkap

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=192.168.1.50
tools/venv/bin/python -I tools/tv.py --probe
tools/disable.sh examples/<model>/debloat.txt
TV_HOST=192.168.1.50 launcher/build-and-install.sh
TV_HOST=192.168.1.50 tools/theme.sh themes/swadaya
# restore kalau perlu: tools/restore.sh examples/<model>/debloat.txt
```

Selamat utak-atik TV-nya! Kalau buntu, baca `docs/TROUBLESHOOTING.md` di repo aslinya.
