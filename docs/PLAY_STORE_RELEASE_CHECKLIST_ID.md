# Checklist Rilis Google Play — ViTor

Dokumen ini adalah daftar persiapan. Jangan menyatakan aplikasi siap rilis sebelum semua pemeriksaan yang relevan selesai.

## Status teknis saat ini
- Package ID: `com.saputra.vitor`
- Minimum Android: API 26
- Target Android: API 36
- Format distribusi: Android App Bundle (.aab)
- Penandatanganan: upload key milik developer melalui secret CI; jangan commit file keystore atau password.
- Pemrosesan media: lokal di perangkat; file dipilih melalui Android document picker.
- Izin sensitif: tidak meminta akses luas ke seluruh penyimpanan media.

## Wajib sebelum upload
- [ ] Jalankan unit test dan build debug tanpa error.
- [ ] Uji instalasi dan fungsi impor, preview, trim, gabung, audio, subtitle, simpan/buka proyek, dan ekspor pada beberapa perangkat ARM64.
- [ ] Uji file besar, codec umum, orientasi video, audio tanpa video, penyimpanan penuh, dan proses ekspor yang dibatalkan/gagal.
- [ ] Uji ulang setelah target API 36.
- [ ] Lengkapi ikon aplikasi final dan ikon listing Google Play 512 x 512 PNG.
- [ ] Siapkan feature graphic 1024 x 500 dan screenshot aktual dari aplikasi.
- [ ] Tulis deskripsi singkat dan lengkap yang hanya menyebut fitur yang sudah benar-benar berfungsi.
- [ ] Isi email kontak developer yang aktif.
- [ ] Terbitkan kebijakan privasi pada URL publik dan masukkan URL tersebut di Play Console.
- [ ] Lengkapi Data safety berdasarkan perilaku APK final, bukan perkiraan.
- [ ] Lengkapi content rating, target audience, app access, ads declaration, dan deklarasi izin.
- [ ] Pastikan nama, ikon, aset, font, audio, dan konten pihak ketiga memiliki hak penggunaan yang sesuai.
- [ ] Naikkan versionCode setiap rilis baru.
- [ ] Buat dan simpan cadangan upload keystore secara aman. Jangan unggah ke repository.
- [ ] Bangun AAB release yang ditandatangani upload key dan verifikasi signature-nya.
- [ ] Jalankan internal testing, lalu closed testing bila diwajibkan akun Play Console.
- [ ] Perbaiki temuan pre-launch report dan crash sebelum produksi.

## Konfigurasi secret untuk workflow rilis
Tambahkan secret berikut pada pengaturan repository sebelum menjalankan workflow **ViTor Play Store Bundle**:
- `VITOR_UPLOAD_KEYSTORE_BASE64`
- `VITOR_UPLOAD_STORE_PASSWORD`
- `VITOR_UPLOAD_KEY_ALIAS`
- `VITOR_UPLOAD_KEY_PASSWORD`

Keystore harus dibuat dan disimpan oleh pemilik akun developer. Jangan membagikan password atau file keystore melalui chat atau memasukkannya ke source control.

## Catatan akun developer
Untuk akun developer personal yang dibuat setelah 13 November 2023, Google Play mensyaratkan closed test dengan setidaknya 12 tester yang ikut serta secara terus-menerus selama 14 hari sebelum mengajukan akses produksi. Periksa persyaratan yang tampil pada akun Play Console karena ketentuan dapat berubah.
