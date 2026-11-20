# PLAN: SwadayaTV All-in-One Widgets

Branch: `feature/all-in-one-widgets`
Repo referensi: ~/ref/ (lihat CREDITS.md untuk atribusi)

---

## 1. Audit Hasil

### 1.1 SwadayaTV (repo utama)
- Bahasa: **Java**
- UI: XML + custom View (tvOS-style), bukan Compose
- minSdk 26, targetSdk 34, compileSdk 34
- Dependency: `rec_numerous`, `dynamicanimation`, `junit`
- Arsitektur: `MainActivity.java` (layar utama), berbagai widget class

### 1.2 Repo Referensi
| Repo | Lisensi | Fitur khas |
|---|---|---|
| aura-tv-home | MIT (Gathin) | Sudah sama basisnya |
| arsyan-tv-launcher | MIT | Azan overlay + service + audio focus, screensaver DreamView dengan jam salat, PrayerTimes Kemenag |
| cleanyourtv | MIT | Addon ecosystem, cuaca, koin, kapasitas |
| weatherglass-tv | MIT | Cuaca glass UI, wallpaper hidup |
| SwadayaTV-TV-Launcher | MIT | DrmInfo, app grid reorder |

### 1.3 Kesimpulan
- Semua referensi **MIT license** → bisa disalin dengan atribusi.
- **arsyan-tv-launcher** punya implementasi azan yang lebih baik (overlay + foreground service + audio focus) → akan diintegrasikan.
- **arsyan-tv-launcher** screensaver `DreamView` punya jam salat di layar → akan diintegrasikan.
- **KemenagPrayer** di SwadayaTV sudah ada, tinggal tambah Subuh +8 menit (Muhammadiyah).
- **Kalender Hijriyah KHGT** → SwadayaTV sudah punya `getHijriDate()` dengan `java.time.chrono.HijrahDate`, tinggal diadaptasi.

---

## 2. Urutan Pengerjaan

1. Jam/tanggal/salam (sudah ada, tinggal dipoles)
2. Info sistem (sudah ada di CapacityStatsActivity, tinggal polish)
3. Cuaca (sudah ada, tinggal dipoles)
4. Jadwal salat + hitung mundur (tambah Subuh +8 menit Muhammadiyah)
5. Kalender Hijriyah KHGT + hitung mundur Hari Raya Islam
6. Adzan + screensaver (integrasikan dari arsyan-tv-launcher)
7. Klasemen liga (sudah ada, tinggal dipoles)
8. Baris aplikasi & pengaturan (sudah ada, tinggal dipoles)
9. Polesan tampilan

---

## 3. Risiko & Catatan
- Semua fitur inti sudah ada di SwadayaTV, fokusnya pada integrasi dan polish.
- Tambah Subuh +8 menit di KemenagPrayer.
- Integrasikan azan overlay + service dari arsyan-tv-launcher.
- Pastikan tidak ada crash saat offline.