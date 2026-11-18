# Tutorial Debloat SwadayaTV (Untuk Pemula)

## Kenapa perlu debloat?
Banyak TV/STB Android datang dengan aplikasi bawaan (bloatware) yang jarang dipakai. Aplikasi ini memakan RAM dan memperlambat TV. Debloat = menonaktifkan/menghapus aplikasi tersebut agar TV lebih ringan.

> ⚠️ Debloat hanya untuk pengguna yang paham. Jika salah hapus, bisa di-restore.

## Cara 1: Via HP (Paling Gampang) — pakai LADb

1. Download aplikasi **LADb** dari Play Store di HP.
2. Pastikan HP dan TV terhubung ke **Wi-Fi yang sama**.
3. Di TV, aktifkan **ADB over Wi-Fi** (Pengaturan > Developer Options > ADB over Network). Catat IP TV.
4. Buka LADb di HP, masukkan IP TV (contoh: `192.168.1.10:5555`), lalu Connect.
5. Setelah terhubung, jalankan script debloat (copy dari `tools/disable.sh`):

```bash
# Contoh: nonaktifkan aplikasi bawaan
pm disable-user --user 0 com.example.bloatware
```

6. Selesai. Restart TV jika perlu.

## Cara 2: Via PC (ADB)

1. Install ADB di laptop (Windows/Mac/Linux).
2. Hubungkan TV ke laptop via USB atau Wi-Fi.
3. Jalankan:

```bash
adb connect <IP_TV>:5555
adb shell pm list packages  # lihat daftar aplikasi
adb shell pm disable-user --user 0 <nama.package>
```

## Cara Restore (Jika salah hapus)

Jika aplikasi yang dinonaktifkan ternyata penting, balikkan dengan:

```bash
adb shell pm enable <nama.package>
```

Atau reset ke pengaturan pabrik (factory reset) untuk mengembalikan semua.

> 💡 Selalu catat nama package yang kamu disable agar bisa di-restore.