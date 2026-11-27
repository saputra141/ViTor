# ViTor

**ViTor** adalah editor video native Android (Kotlin + Jetpack Compose) untuk ARM64. Aplikasi memakai pipeline decode/render/encode Android, bukan menjalankan aplikasi desktop melalui Wine atau emulator.

## Fitur yang sudah ada di kode

- Impor video, foto, dan audio dari penyimpanan Android; media disalin ke penyimpanan privat aplikasi.
- Preview timeline berbasis OpenGL dan decoder Android.
- Multi-track timeline, pilih, pindah, trim, split, duplikasi, dan hapus klip.
- Undo/redo dan penyimpanan proyek lokal berbasis JSON.
- Layer teks dan stiker, filter/LUT, adjustment warna, transisi, speed, crop/canvas, serta keyframe.
- Ekspor MP4 melalui MediaCodec/MediaMuxer dengan pilihan resolusi dan frame rate.
- Pustaka proyek lokal: buat, buka, duplikasi, dan hapus proyek.
- Katalog konten Gebang Editor disertakan sebagai metadata sumber.

## Sumber dan batas kompatibilitas Gebang Editor

- Katalog konten resmi: https://github.com/gebangkiidiw/GebangEditor-Konten
- Katalog JSON berada di `app/src/main/assets/gebang/katalog.json`.
- Rilis desktop Gebang Editor: https://github.com/gebangkiidiw/GebangEditor-Releases

Repositori publik Gebang yang tersedia saat integrasi diperiksa menyediakan katalog konten dan binary rilis desktop, bukan source code lengkap mesin editor. Karena itu binary Windows/macOS tidak dapat langsung dipaketkan menjadi APK native, dan ViTor **belum boleh diklaim memiliki seluruh fitur atau kompatibilitas proyek Gebang Editor**. Format preset dan efek Gebang masih perlu dianalisis serta diimplementasikan satu per satu pada engine Android dengan tetap menghormati lisensi dan format sumber.

## Validasi

GitHub Actions menjalankan unit test JVM, build APK debug, dan build APK release unsigned. Kelulusan CI membuktikan kode berhasil diuji/build di runner, bukan pengujian pada perangkat Android fisik. Preview real-time, kompatibilitas codec perangkat, sinkronisasi audio, dan hasil render akhir tetap perlu diuji pada beberapa perangkat ARM64.

## Build lokal

Dengan JDK 17 dan Gradle 8.9:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
gradle :app:assembleRelease
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`  
APK release unsigned: `app/build/outputs/apk/release/app-release-unsigned.apk`
