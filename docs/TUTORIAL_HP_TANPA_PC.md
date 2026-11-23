# Pakai SwadayaTV dan bersihkan TV dari HP (tanpa PC, tanpa AI)

Panduan ini untuk orang yang **hanya punya HP Android dan TV** — tanpa laptop, tanpa kabel, tanpa coding agent. Semua langkah dilakukan dari HP. Hasilnya: SwadayaTV terpasang sebagai Home, bloatware dan baris iklan mati, dan semuanya bisa dipulihkan.

> Butuh: HP Android 8.0 ke atas, TV Android TV / Google TV, dan keduanya di **Wi-Fi yang sama**. Prosesnya sekitar 10 menit.

## Ringkasan

1. Pasang **Swadaya App** di HP (APK dari [Releases](https://github.com/frhnkrnwn/SwadayaTV/releases) repo ini).
2. Nyalakan **USB debugging** + **Network debugging** di TV, catat IP TV.
3. Sambungkan app ke TV, izinkan sekali di TV.
4. **Pasang SwadayaTV dulu**, jadikan Home.
5. Analisa → pilih kategori → Bersihkan.
6. Matikan lagi debugging di TV.

---

## 1. Siapkan HP

1. Unduh `swadaya-app-<versi>.apk` ke HP: tekan tombol **Unduh Swadaya App** di [situs SwadayaTV](https://frhnkrnwn.github.io/SwadayaTV/id.html#app), atau ambil dari halaman **Releases**. Simpan berkasnya di folder **Download** supaya mudah ditemukan.
2. Buka berkasnya. Android akan bertanya apakah boleh memasang dari aplikasi ini (Files/Chrome): pilih **Allow / Izinkan**.
3. Pasang, lalu buka **Swadaya App**.

APK di Releases sudah ditandatangani kunci rilis SwadayaTV, jadi versi baru bisa dipasang langsung di atas versi lama (tidak perlu uninstall).

Tidak perlu root, tidak perlu Play Store, tidak perlu langganan.

## 2. Nyalakan debugging di TV (sekali saja)

Dengan remote TV:

1. **Settings → About** (di beberapa TV: *System → About*). Sorot **Build**, tekan **OK 7 kali** sampai muncul “You are now a developer”.
2. **Settings → System → Developer options** (di Google TV: *Settings → System → Developer options*).
3. Nyalakan **USB debugging**.
4. Di menu yang sama, nyalakan:
   - **Network debugging** / **ADB over network** / **ADB debugging over network**, **atau**
   - **Wireless debugging** (kalau hanya ini yang ada).
5. Catat **IP TV**: **Settings → Network & Internet** (atau *About → Network*) → **IPv4 address**, misalnya `192.168.1.50`.

> **Penting:** “USB debugging” di sini adalah nama menu ADB di TV. Tidak perlu kabel USB dan tidak perlu komputer — HP akan menyambung lewat Wi-Fi.

## 3. Sambungkan app ke TV

1. Di HP, pastikan Wi-Fi sama dengan TV (jangan pakai data seluler).
2. Buka **Swadaya App** → isi **Alamat IP TV** (contoh `192.168.1.50`), port biarkan `5555`.
3. Tekan **Sambungkan**.
4. Di TV akan muncul **“Allow USB debugging?”** dengan sidik jari kunci. Centang **Always allow from this computer**, lalu pilih **OK** dengan remote.
5. Di HP akan tampil nama TV dan versi Android. Kalau berhasil, app otomatis pindah ke langkah **Analisa**.

Bisa juga tekan **Cari TV**: app mencari TV yang mengumumkan ADB di Wi-Fi (`_adb._tcp`, `_adb-tls-connect._tcp`) lalu mengisi IP-nya otomatis.

## 4. Pasang SwadayaTV **sebelum** debloat

Ini bagian yang paling sering terlewat. Kalau launcher bawaan dimatikan sebelum ada pengganti, TV bisa tidak punya Home.

1. Di Swadaya App, langkah **Analisa**, tekan **Unduh & pasang SwadayaTV otomatis**. App mencari rilis terbaru di GitHub, mengunduh `tvhome-<versi>.apk` ke HP, mengirimnya ke TV lewat ADB, memasangnya, lalu menjalankan `cmd package set-home-activity` supaya SwadayaTV langsung menjadi Home. Tidak ada berkas yang perlu diunduh atau dipindahkan sendiri. Setelah selesai, app menampilkan tiga baris ringkas: **SwadayaTV di TV** (nama versi + build), **versi rilis terbaru** yang baru dipasang, lalu kesimpulan **TV sudah memakai versi terbaru** (plus versi asalnya kalau baru diperbarui) atau **TV belum memakai versi terbaru**.
2. Kalau HP sedang tanpa internet (atau GitHub tidak bisa dihubungi), unduh APK-nya dari [Releases](https://github.com/frhnkrnwn/SwadayaTV/releases) (`tvhome-<versi>.apk`), lalu tekan **Pasang dari APK di HP (manual)** dan pilih berkas itu.
3. Kalau TV menolak memasang (mis. “Install blocked”), di TV buka **Settings → Security & restrictions → Install unknown apps**, izinkan untuk aplikasi yang dipakai (mis. Package Installer / Files), lalu ulangi.
4. Tekan **Home** di remote untuk memastikan SwadayaTV muncul. Kalau muncul pilihan Home, pilih **SwadayaTV** → **Always**.

> Swadaya App juga berguna tanpa SwadayaTV: ia tetap bisa menonaktifkan aplikasi, asalkan TV masih punya launcher lain. Tapi urutan yang disarankan tetap: pasang launcher dulu, debloat kemudian.

## 5. Analisa

App membaca isi TV dan menampilkan rencana, dikelompokkan:

| Kategori | Isi |
|---|---|
| Iklan & rekomendasi | baris sponsor, saran tontonan, mode demo toko |
| Pelacakan & telemetri | laporan pemakaian ke pabrikan |
| Aplikasi bawaan | app bawaan yang jarang dipakai |
| Tambahan pabrikan | toko/layanan merek TV |

Di bawahnya ada daftar **Dibiarkan (tidak disentuh)**, lengkap dengan alasannya: `dilindungi`, `keyboard aktif`, `satu-satunya Home; pasang SwadayaTV dulu`, atau `sudah nonaktif`.

Tidak ada yang berubah di langkah ini. Hanya membaca.

## 6. Bersihkan

1. Hapus centang kategori yang tidak ingin dimatikan. Kalau ragu, biarkan semua tercentang — daftarnya sudah konservatif (hanya iklan, pelacakan, dan app bawaan).
2. Tekan **Lanjut: bersihkan TV** → **Bersihkan sekarang** → konfirmasi.
3. App menjalankan `pm disable-user --user 0 <paket>` satu per satu, menampilkan centang per paket, lalu memverifikasi hasilnya.
4. Setiap paket yang berhasil dicatat di HP (langkah **Pulihkan**), jadi tidak ada perubahan yang hilang jejaknya.

**Yang tidak pernah dimatikan:** Wi-Fi, Settings, keyboard, Play Store, Play Services, SystemUI, permission controller, package installer, input HDMI/tuner, dan Home satu-satunya.

## 7. Matikan lagi debugging (keamanan)

Setelah selesai, di TV:

1. **Developer options → matikan USB debugging, Network debugging, dan Wireless debugging.**
2. Kalau ada, hapus juga pasangan HP di **Developer options → Revoke USB debugging authorizations**.
3. Restart TV sekali supaya port 5555 benar-benar tertutup.

ADB terbuka itu seperti pintu belakang: siapa pun di Wi-Fi yang sama bisa memerintah TV. Nyalakan hanya saat dipakai.

## 8. Kalau ada yang salah

- **Satu aplikasi** tidak jalan setelah dimatikan → buka Swadaya App → **Pulihkan** → tekan **Pulihkan** pada baris itu, atau nyalakan dari **Settings → Apps → lihat semua app → pilih app → Enable**.
- **Semua** → **Pulihkan semua**, atau bagikan **skrip pemulihan** lalu jalankan di mana pun ada adb.
- TV jadi aneh → restart TV. Semua perubahan tetap bisa dibalik karena tidak ada yang di-uninstall.

## Troubleshooting

| Gejala | Arti & solusi |
|---|---|
| “Port 5555 tertutup” | Network debugging belum nyala, atau TV reboot dan mematikannya. Nyalakan lagi di Developer options. |
| “TV tidak menjawab” | IP salah, HP di jaringan lain (mis. Wi-Fi tamu atau 5 GHz terpisah), atau AP isolation di router. Coba **Cari TV**. |
| TV menampilkan “Allow USB debugging?” terus | Pilih **Always allow** + **OK**. Kalau tetap muncul: cabut izin lama di **Revoke USB debugging authorizations**, sambungkan lagi. |
| TV hanya punya **Wireless debugging** (kode pairing) | TV tipe ini tidak membuka port 5555 sendiri. Sekali saja, dari komputer di Wi-Fi yang sama: `adb pair <IP>:<port-pairing> <kode>` lalu `adb connect <IP>:<port>` dan `adb tcpip 5555`. Setelah itu app bisa menyambung tanpa PC. |
| “Gagal memasang: INSTALL_FAILED_…” | Izinkan **Install unknown apps** di TV, atau pastikan APK-nya untuk Android TV (bukan APK HP). |
| App berhenti saat menyambung | Tutup app lain yang memakai ADB, lalu ulangi. Satu TV hanya melayani beberapa koneksi ADB sekaligus. |

## Setelah debloat

- Pasang **Private DNS AdGuard** di TV (*Settings → Network → Private DNS* → `dns.adguard-dns.com`) untuk memblokir domain iklan.
- Matikan auto-update Play Store kalau TV terasa berat.
- Restart TV seminggu sekali.
- Untuk tweak lain (background restriction, animasi 0.5×), lihat [xiaomi-debloat.md](xiaomi-debloat.md).
