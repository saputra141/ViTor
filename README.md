# ViTor

ViTor adalah proyek Android ARM64 untuk membangun ulang pengalaman editor video Gebang Editor dalam aplikasi native Android.

## Status
- [x] Repository Android diinisialisasi.
- [x] Kerangka aplikasi native Kotlin + Jetpack Compose.
- [ ] Import media dan pemilih file.
- [ ] Timeline video/audio.
- [ ] Teks, layer, dan efek.
- [ ] Rendering dan ekspor video.
- [ ] Pengujian pada perangkat ARM64.

## Catatan porting
Repository publik Gebang Editor yang telah diperiksa menyediakan rilis aplikasi desktop dan katalog konten, tetapi belum menyediakan source code mesin editornya. Karena itu ViTor dimulai sebagai pembangunan ulang native Android; kerangka ini belum mengandung seluruh mesin atau fitur Gebang Editor. Jangan menganggap APK sudah setara dengan aplikasi desktop sampai setiap fitur diimplementasikan dan diuji.

## Build
Build debug dilakukan melalui GitHub Actions pada workflow **Android CI**. APK hasil build tersedia sebagai artifact jika workflow berhasil.

## Target awal
- Android ARM64
- Kotlin
- Jetpack Compose
- Minimum Android 8.0 (API 26)
