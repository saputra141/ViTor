# ViTor

ViTor adalah proyek editor video native Android ARM64 yang sedang dikembangkan untuk mengadaptasi alur kerja Gebang Editor. Prioritas proyek adalah fitur yang benar-benar berfungsi, bukan sekadar tampilan.

## Sumber Gebang Editor
- Katalog konten resmi: https://github.com/gebangkiidiw/GebangEditor-Konten
- Katalog JSON disimpan apa adanya di `app/src/main/assets/gebang/katalog.json`.
- Nama item, kategori, status premium/gratis, dan daftar nama file dibaca dari katalog sumber.
- Repositori publik yang diperiksa belum menyediakan source code mesin editor. Karena itu, ViTor membangun mesin native sendiri dan tidak mengklaim identik sebelum diuji terhadap aplikasi sumber.

## Kondisi implementasi saat ini
- [x] Kerangka Android native Kotlin + Jetpack Compose.
- [x] Import video lokal dan preview Media3.
- [x] Membaca katalog konten sumber.
- [x] Model data subtitle dengan timestamp milidetik dan timing per kata.
- [x] Parser dan writer subtitle SRT, termasuk teks multi-baris dan variasi line ending.
- [ ] UI track subtitle dan penyuntingan cue di timeline.
- [ ] Mesin speech-to-text offline untuk vlog.
- [ ] Pengenalan lirik dan penyelarasan per kata untuk video musik.
- [ ] Timeline editing, audio, efek, transisi, dan rendering/export.

## Rencana Auto Caption dan Auto Lyrics
Targetnya adalah pemrosesan audio lokal di perangkat, dengan model yang dapat diunduh/dikelola terpisah dari APK. Speech-to-text untuk vlog dan pengenalan lirik lagu diperlakukan sebagai dua mode berbeda. Hasil pengenalan harus dapat dikoreksi manual. Mode lirik membutuhkan word-level alignment agar sorotan karaoke mengikuti audio.

Referensi teknis yang sedang dievaluasi:
- sherpa-onnx mendukung speech recognition lokal/offline dan menyediakan contoh Android/Kotlin serta forced alignment: https://github.com/k2-fsa/sherpa-onnx
- whisper.cpp memiliki contoh Android untuk transkripsi file audio; model Whisper perlu disediakan terpisah: https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android

Belum ada engine ASR yang terintegrasi di APK saat ini. Tidak ada tombol Auto Caption/Auto Lyrics yang boleh ditampilkan sebagai fitur aktif sebelum pipeline audio, model, timestamp, dan pengujian perangkat selesai.

## Pengujian
GitHub Actions menjalankan unit test sebelum membangun APK debug. Uji perangkat ARM64 tetap diperlukan untuk memvalidasi performa, konsumsi memori, dukungan codec, dan hasil pengenalan.
