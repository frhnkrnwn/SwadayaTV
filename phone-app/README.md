# Swadaya App — bersihkan TV dari HP

Aplikasi Android untuk **menonaktifkan bloatware dan iklan di Android TV / Google TV langsung dari HP**, tanpa PC, tanpa laptop dan tanpa AI. HP menjadi ADB host: ia bicara protokol ADB sendiri ke TV di Wi-Fi yang sama, membaca daftar aplikasi, menunjukkan rencananya, lalu mematikan yang dipilih. **Tidak ada yang di-uninstall** — semua bisa dinyalakan lagi dari app ini.

**Unduh:** [Releases](https://github.com/frhnkrnwn/SwadayaTV/releases) → `swadaya-app-<versi>.apk`, atau jalankan workflow **Release Swadaya App** di tab Actions (APK yang sudah ditandatangani ikut terbit di Releases).

Panduan langkah demi langkah (Bahasa Indonesia, untuk pemula): [docs/TUTORIAL_HP_TANPA_PC.md](../docs/TUTORIAL_HP_TANPA_PC.md).

## Alur

| 1 · Sambung | 2 · Analisa | 3 · Bersihkan | 4 · Pulihkan |
|---|---|---|---|
| Masukkan IP TV (atau tekan **Cari TV**), lalu **Sambungkan**. TV menanyakan “Allow USB debugging?” — pilih **Always allow**. | App membaca `pm list packages`, merek TV, Home yang aktif dan keyboard, lalu menampilkan rencana per kategori: iklan, pelacakan, aplikasi bawaan, tambahan pabrikan. | Pilih kategori, tekan **Bersihkan sekarang**. Setiap paket dimatikan dengan `pm disable-user --user 0` dan dicatat di HP. | Satu baris atau semua sekaligus. Skrip pemulihan juga bisa dibagikan sebagai teks. |

## Yang dijaga

- **Home:** launcher bawaan hanya dimatikan kalau sudah ada launcher lain yang bisa menjawab Home. Karena itu app menawarkan **Unduh & pasang SwadayaTV otomatis**: app mengambil `tvhome-<versi>.apk` dari rilis terbaru repo ini (`api.github.com/repos/frhnkrnwn/SwadayaTV/releases/latest`), memeriksa ukuran dan SHA-256-nya, memasangnya ke TV lewat ADB lalu menjadikannya Home. Rilis dibaca dari `api.github.com/repos/frhnkrnwn/SwadayaTV/releases/latest`; kalau API sedang membatasi permintaan (60/jam per IP tanpa token), app memakai pengalihan `/releases/latest` dan mengunduh `releases/latest/download/tvhome-<versi>.apk`, jadi tombolnya tetap bekerja. Ada juga **Pasang dari APK di HP (manual)** untuk dipakai tanpa internet.
- **Keyboard, Settings, Play Store, Play Services, SystemUI, permission controller, package installer, dan input TV** tidak pernah disentuh.
- **Tidak ada uninstall.** Semua perubahan `disable-user`, bisa dibalik kapan saja.
- **Tidak ada akun, analytics, atau iklan.** App hanya bicara ke TV di jaringan lokal.

## Kebutuhan

- HP Android 8.0+ (minSdk 26).
- TV Android TV / Google TV dengan **USB debugging** dan **Network debugging (ADB over network)** atau **Wireless debugging** yang dinyalakan.
- HP dan TV di Wi-Fi yang sama.

TV yang hanya punya *Wireless debugging* dengan kode pairing membutuhkan satu kali `adb tcpip 5555` dari komputer sebelum app bisa menyambung — sama seperti aplikasi sejenis. Detailnya di tutorial.

## Bangun sendiri

```bash
cd phone-app
./gradlew assembleDebug      # JDK 17 + Android SDK 36
./gradlew assembleRelease    # ditandatangani kunci rilis, di-shrink R8
```

APK hasil build ada di `app/build/outputs/apk/debug/app-debug.apk` dan
`app/build/outputs/apk/release/app-release.apk`. Butuh `local.properties` berisi `sdk.dir=…`
(tidak di-commit).

## Rilis dan tanda tangan

`.github/workflows/release-app.yml` membangun APK rilis lalu menerbitkannya sebagai GitHub Release
dengan tag `swadaya-app-v<versi>`: buka **Actions → Release Swadaya App → Run workflow**, atau push
tag `swadaya-app-v1.0.0`. Naikkan `versionCode` dan `versionName` di `app/build.gradle` sebelum rilis
berikutnya. Release ini **tidak** ditandai “latest”, supaya pembaruan OTA launcher di TV tetap
menemukan `tvhome-*.apk` di release-nya sendiri.

APK rilis ditandatangani kunci tetap yang di-commit: `../launcher/tvhome-release.jks` dengan
konfigurasi di `keystore.properties`. Karena itu APK dari Releases selalu bisa dipasang di atas
versi sebelumnya. Rahasia CI `SIGNING_*` (kalau diisi) menggantikan kunci itu; tanpa kunci sama
sekali, build rilis memakai kunci debug mesin setempat.

## Struktur

| Berkas | Isi |
|---|---|
| `MainActivity.java` | Alur 4 langkah dan seluruh UI. |
| `Tv.java` | Koneksi ADB (dadb), baca paket, disable/enable, install APK. |
| `LauncherApk.java` | Mencari `tvhome-*.apk` di rilis terbaru GitHub dan mengunduhnya (ukuran + SHA-256 diperiksa). |
| `Catalog.java` | Membaca `assets/bloat.json`. |
| `Planner.java` | Menyusun rencana + daftar “dibiarkan” dan pengaman Home/keyboard. |
| `RestoreStore.java` | Catatan perubahan di HP + ekspor skrip pemulihan. |
| `TvFinder.java` | Menemukan TV lewat mDNS (`_adb._tcp`, `_adb-tls-connect._tcp`), tanpa duplikat. |
| `assets/bloat.json` | Daftar paket per kategori dan merek. |

Daftar di `assets/bloat.json` sengaja konservatif: hanya paket yang jelas iklan/pelacakan/aplikasi bawaan. Tambah entri baru dengan `{"p": "nama.paket", "n": "Nama", "g": "ads|tracking|bloat|extra", "b": ["merek"]}`; `keep: true` untuk paket yang harus selalu dibiarkan.
