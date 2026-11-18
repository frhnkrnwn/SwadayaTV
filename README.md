# 📺 SwadayaTV — Launcher All-in-One

> Ubah STB atau Android TV yang lemot jadi Smart TV modern dengan **satu aplikasi saja**.

SwadayaTV adalah launcher pengganti untuk Android TV/STB. Tidak perlu install banyak aplikasi: cuaca, jadwal sholat, bola, wallpaper, sleep timer, sampai update otomatis — semuanya sudah ada di satu tempat, tampilan rapi, dan ringan di RAM.

## ✨ Fitur Unggulan

- ✅ **Widget Bola** — Jadwal pertandingan, hasil, dan klasemen top 5. Pilih liga (EPL, Seri A, dll) dan tim favoritmu (misalnya AS Roma), widget langsung menampilkan data tim tersebut.
- ✅ **Cuaca & Kualitas Udara (AQI)** — Info cuaca real-time langsung di layar utama.
- ✅ **Jadwal Sholat Muhammadiyah** — Lengkap dengan countdown dan kalender Hijriyah (KHGT).
- ✅ **Remote Wallpaper via QR Code** — Ganti wallpaper TV langsung dari HP! Scan QR Code, pilih foto, upload, dan TV otomatis mengganti wallpaper.
- ✅ **Tema Estetik** — Pilih suasana: **Aurora**, **Midnight**, **Minimalist**, dan lainnya. Tampilan bersih, modern, tidak norak.
- ✅ **Sleep Timer & Kontrol Kecerahan** — Atur timer tidur dan kecerahan layar langsung dari remote TV.
- ✅ **Update Otomatis (OTA)** — Aplikasi cek update sendiri. Tidak perlu download manual.

## 📥 Cara Install (Paling Gampang)

1. Download file **SwadayaTV.apk** dari halaman [Releases](https://github.com/frhnkrnwn/SwadayaTV/releases).
2. Copy file APK ke **flashdisk/USB**.
3. Colokkan USB ke TV/STB Android.
4. Buka file manager di TV, cari file APK, lalu klik **Install**.
5. Selesai! SwadayaTV siap dipakai sebagai launcher.

> 💡 Jika TV meminta izin "Install dari sumber tidak dikenal", aktifkan dulu di Pengaturan > Keamanan.

## 🔄 Update Otomatis

Tidak perlu repot download manual. SwadayaTV memiliki fitur **OTA (Over-The-Air)**: aplikasi akan cek versi terbaru secara berkala. Jika ada update, akan muncul notifikasi — cukup klik "Unduh & Pasang" dan selesai.

## 📸 Tangkapan Layar

*(Screenshot antarmuka akan segera diperbarui)*

## ⭐ Versi Terbaru: v3.7.0

Pembaruan besar: UI overhaul, tema baru, widget bola, remote wallpaper, sleep timer, kontrol kecerahan, dan perbaikan OTA.

---

<details>
<summary>🛠️ Panduan untuk User Mahir (Debloat & Install via ADB)</summary>

Untuk pengguna yang ingin membersihkan aplikasi bawaan (debloat) dan install via ADB:

```bash
# Hubungkan ADB ke TV
adb connect <IP_TV>:5555

# Install APK
adb install SwadayaTV.apk

# (Opsional) Debloat aplikasi bawaan
adb shell pm uninstall --user 0 <nama.package>
```

Panduan lengkap debloat tersedia di [docs/TUTORIAL_DEBLOAT.md](docs/TUTORIAL_DEBLOAT.md).

Panduan addon tersedia di [docs/TUTORIAL_ADDONS.md](docs/TUTORIAL_ADDONS.md).
</details>

<details>
<summary>👨‍💻 Untuk Developer (Build dari Source)</summary>

```bash
cd launcher
./gradlew assembleDebug
```

Struktur repo: `launcher/` (app Android), `addons/` (widget opsional), `data/` (data bola), `docs/` (dokumentasi), `release/` (metadata OTA).
</details>

---

Lisensi: MIT. Kontribusi dipersilakan!