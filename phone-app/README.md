# Swadaya App — bersihkan TV dari HP

Aplikasi Android untuk **menonaktifkan bloatware dan iklan di Android TV / Google TV langsung dari HP**, tanpa PC, tanpa laptop dan tanpa AI. HP menjadi ADB host: ia bicara protokol ADB sendiri ke TV di Wi-Fi yang sama, membaca daftar aplikasi, menunjukkan rencananya, lalu mematikan yang dipilih. **Tidak ada yang di-uninstall** — semua bisa dinyalakan lagi dari app ini.

Panduan langkah demi langkah (Bahasa Indonesia, untuk pemula): [docs/TUTORIAL_HP_TANPA_PC.md](../docs/TUTORIAL_HP_TANPA_PC.md).

## Alur

| 1 · Sambung | 2 · Analisa | 3 · Bersihkan | 4 · Pulihkan |
|---|---|---|---|
| Masukkan IP TV (atau tekan **Cari TV**), lalu **Sambungkan**. TV menanyakan “Allow USB debugging?” — pilih **Always allow**. | App membaca `pm list packages`, merek TV, Home yang aktif dan keyboard, lalu menampilkan rencana per kategori: iklan, pelacakan, aplikasi bawaan, tambahan pabrikan. | Pilih kategori, tekan **Bersihkan sekarang**. Setiap paket dimatikan dengan `pm disable-user --user 0` dan dicatat di HP. | Satu baris atau semua sekaligus. Skrip pemulihan juga bisa dibagikan sebagai teks. |

## Yang dijaga

- **Home:** launcher bawaan hanya dimatikan kalau sudah ada launcher lain yang bisa menjawab Home. Karena itu app menawarkan **Pasang SwadayaTV dari APK** (pilih APK di HP, app memasangnya ke TV lewat ADB lalu menjadikannya Home).
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
```

APK hasil build ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Struktur

| Berkas | Isi |
|---|---|
| `MainActivity.java` | Alur 4 langkah dan seluruh UI. |
| `Tv.java` | Koneksi ADB (dadb), baca paket, disable/enable, install APK. |
| `Catalog.java` | Membaca `assets/bloat.json`. |
| `Planner.java` | Menyusun rencana + daftar “dibiarkan” dan pengaman Home/keyboard. |
| `RestoreStore.java` | Catatan perubahan di HP + ekspor skrip pemulihan. |
| `TvFinder.java` | Menemukan TV lewat mDNS (`_adb._tcp`, `_adb-tls-connect._tcp`). |
| `assets/bloat.json` | Daftar paket per kategori dan merek. |

Daftar di `assets/bloat.json` sengaja konservatif: hanya paket yang jelas iklan/pelacakan/aplikasi bawaan. Tambah entri baru dengan `{"p": "nama.paket", "n": "Nama", "g": "ads|tracking|bloat|extra", "b": ["merek"]}`; `keep: true` untuk paket yang harus selalu dibiarkan.
