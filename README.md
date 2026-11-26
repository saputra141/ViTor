# ViTor

ViTor adalah proyek porting native Android ARM64 untuk mengadaptasi alur editor video Gebang Editor.

## Sumber asli yang dipakai
- Katalog konten resmi: https://github.com/gebangkiidiw/GebangEditor-Konten
- Katalog JSON disalin apa adanya ke `app/src/main/assets/gebang/katalog.json`.
- Thumbnail diambil dari URL sumber yang tercantum di katalog.
- Nama item, kategori, status premium/gratis, dan daftar nama file tidak dibuat ulang oleh ViTor.

## Batas sumber yang tersedia
Repositori publik Gebang Editor yang diperiksa menyediakan katalog konten dan paket rilis desktop, tetapi tidak menyediakan source code mesin editor Android atau desktop. Paket desktop yang tersedia adalah binary Windows/macOS; binary itu bukan APK dan tidak dapat langsung dijalankan sebagai aplikasi Android. Karena itu, ViTor belum bisa diklaim setara dengan Gebang Editor.

## Status
- [x] Kerangka Android native Kotlin + Jetpack Compose.
- [x] Import video lokal dan preview Media3.
- [x] Membaca katalog asli 78 item dari aset sumber.
- [x] Menampilkan thumbnail asli dari URL yang tercantum di katalog.
- [ ] Mengurai dan menerapkan format preset/efek asli.
- [ ] Timeline editing, trim, audio, teks, dan layer fungsional.
- [ ] Rendering dan ekspor video.
- [ ] Pengujian perangkat ARM64.

## Build
GitHub Actions workflow **Android CI** membangun APK debug.
