# Bersihkan Android TV Anda — Panduan Bahasa Indonesia

Dokumen ini penjelasan lengkap cara **membersihkan TV Android / Google TV** dari iklan dan aplikasi bawaan yang mengganggu, lalu menggantinya dengan launcher minimal. Dibuat berdasarkan tutorial asli CleanYourTV (bahasa Inggris), diringkas dan ditulis ulang dalam Bahasa Indonesia supaya orang awam mudah mengikuti.

> Sumber asli dan semua filenya sudah disalin ke folder [`cleanyourtv/`](../cleanyourtv/) di repo ini. File tutorial aslinya ada di [`cleanyourtv/README.md`](../cleanyourtv/README.md) (bahasa Inggris).

---

## Ringkasnya dulu

Android TV punya fitur debug yang sama seperti HP Android (namanya ADB / USB debugging). Jika fitur ini dinyalakan, Anda bisa kirim perintah dari komputer lewat Wi-Fi. Dengan perintah itu Anda bisa:

1. **Cadangan** — salin daftar aplikasi dan pengaturan yang ada di TV.
2. **Matikan** — nonaktifkan aplikasi bawaan pabrik yang berisi iklan, pelacak, dan "sampah", tanpa menghapus (jadi bisa dikembalikan kapan saja).
3. **Pasang launcher** — ganti layar utama dengan launcher yang bersih.

**Tidak menggunakan root, tidak menggunakan kabel, tidak menghapus apa pun secara permanen, dan bisa dikembalikan dengan satu perintah.**

---

## Cara 1 — Paling mudah (pakai asisten coding)

Jika Anda punya komputer dan aplikasi asisten coding (misalnya Claude Code, Codex, Gemini CLI, atau sejenisnya), ini langkah tercepat:

1. **Di TV:** Setelan → Preferensi Perangkat → Tentang → tekan OK di **Build** sebanyak 7 kali → kembali → Opsi Pengembang → nyalakan **USB debugging**. Jika ada toggle **ADB debugging** / **Wireless debugging**, nyalakan juga. Biarkan TV tetap menyala.
2. **Di komputer:** klon repo ini, lalu buka folder `cleanyourtv/` di asisten coding Anda. Isi file `PROMPT.md` dengan model TV Anda.
3. **Ikuti instruksinya** — asisten akan membuat cadangan, membersihkan, build, dan memasang launcher. Kurang lebih 15 menit.

---

## Cara 2 — Manual (tanpa asisten coding)

Butuh komputer yang satu jaringan Wi-Fi dengan TV, plus Python 3, JDK 17–22, dan Android SDK platform 34 + build-tools 34.

1. **Sambungkan TV ke komputer lewat Wi-Fi:**
   ```bash
   adb connect <IP_TV>:5555
   ```
   (IP TV bisa dilihat di Setelan → Tentang → Status → Alamat IP.)

2. **Cadangkan:** jalankan `tools/disable.sh` — skrip ini membuat cadangan dulu sebelum melakukan apa pun.

3. **Bersihkan:** skrip `tools/disable.sh` akan menonaktifkan aplikasi pabrik yang aman dimatikan. Semua aplikasi yang masih Anda pakai akan ditanyakan dulu.

4. **Pasang launcher:**
   ```bash
   cd cleanyourtv/launcher
   ./gradlew assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

5. **Jadikan layar utama:** tekan tombol **Home** di remote, pilih launcher baru, lalu pilih **Selalu**.

---

## Kembalikan seperti semula

Jika berubah pikiran, cukup jalankan:

```bash
cd cleanyourtv && bash tools/restore.sh
```

Semua langkah pengembalian tersimpan di file cadangan yang dibuat di awal, jadi TV kembali seperti semula.

---

## Jika ada masalah

- **Tombol Home masih kembali ke launcher pabrik?** Jalankan perintah menonaktifkan launcher pabrik (lihat [panduan ADB di README utama](../README.md)).
- **TV tidak bisa terhubung ke ADB?** Pastikan TV dan komputer satu jaringan Wi-Fi, dan USB debugging sudah menyala.
- **Setelah pembaruan firmware TV, hilang lagi?** Ulangi langkah pembersihan seperti di atas.

---

## Yang tidak bisa dibantu

- **TV non-Android** (Samsung Tizen, LG webOS, Roku, Vizio, Hisense VIDAA) — tidak punya ADB, jadi pembersihan lewat komputer tidak bisa. Untungnya Anda masih bisa **matikan pelacakan/iklan dari remote** lewat situs [cleanyour.tv/not-android](https://cleanyour.tv/not-android).
- **Fire TV** — hanya bagian pembersihannya saja yang jalan; Fire OS tidak mengizinkan launcher pihak ketiga jadi Home.

---

## Daftar isi file yang disalin

Di folder `cleanyourtv/` Anda akan menemukan:

- `addons/` — semua addon (weather, prayer-times, world-clock, dll) lengkap dengan kodenya
- `docs/` — dokumentasi bahasa Inggris (bisa menjadi referensi)
- `examples/` — contoh konfigurasi model TV
- `themes/` — tema-tema siap pakai
- `tools/` — skrip bantu (disable, restore, theme)
- `README.md` — tutorial asli bahasa Inggris

Semua file itu bebas dipakai dan dimodifikasi sesuai lisensi MIT.