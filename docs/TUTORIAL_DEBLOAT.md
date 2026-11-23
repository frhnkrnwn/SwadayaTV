# Tutorial Debloat SwadayaTV (Untuk Pemula)

> Kalau Anda cuma punya HP (tanpa PC dan tanpa AI), pakai [Swadaya App](TUTORIAL_HP_TANPA_PC.md): app Android yang menyambung ke TV lewat Wi-Fi, memasang SwadayaTV, dan menonaktifkan bloatware. Tutorial di bawah ini untuk jalur PC/LADb.

## Kenapa perlu debloat?
Banyak TV/STB Android datang dengan aplikasi bawaan (bloatware) yang jarang dipakai. Aplikasi ini memakan RAM dan memperlambat TV. Debloat = menonaktifkan/menghapus aplikasi tersebut agar TV lebih ringan.

> ⚠️ Debloat hanya untuk pengguna yang paham. Jika salah hapus, bisa di-restore.

## Cara 1: Lewat HP (Paling Mudah) — memakai LADb

1. Unduh aplikasi **LADb** dari Play Store di HP.
2. Pastikan HP dan TV terhubung ke **Wi-Fi yang sama**.
3. Di TV, aktifkan **ADB over Wi-Fi** (Pengaturan > Developer Options > ADB over Network). Catat IP TV.
4. Buka LADb di HP, masukkan IP TV (contoh: `192.168.1.10:5555`), lalu hubungkan.
5. Setelah terhubung, jalankan skrip debloat (salin dari `tools/disable.sh`):

```bash
# Contoh: nonaktifkan aplikasi bawaan
pm disable-user --user 0 com.example.bloatware
```

6. Selesai. Restart TV jika perlu.

## Cara 2: Via PC (ADB)

1. Pasang ADB di laptop (Windows/Mac/Linux).
2. Hubungkan TV ke laptop via USB atau Wi-Fi.
3. Jalankan:

```bash
adb connect <IP_TV>:5555
adb shell pm list packages  # lihat daftar aplikasi
adb shell pm disable-user --user 0 <nama.package>
```

## Cara Restore (Jika salah hapus)

Jika aplikasi yang dinonaktifkan ternyata penting, kembalikan dengan:

```bash
adb shell pm enable <nama.package>
```

Atau reset ke pengaturan pabrik (factory reset) untuk mengembalikan semua.

> 💡 Selalu catat nama package yang Anda nonaktifkan agar bisa dikembalikan.