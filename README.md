# ViTor

ViTor adalah proyek editor video native Android ARM64 yang sedang dikembangkan untuk mengadaptasi alur kerja Gebang Editor. Prioritas proyek adalah fitur yang benar-benar berfungsi, bukan sekadar tampilan.

## Sumber Gebang Editor
- Katalog konten resmi: https://github.com/gebangkiidiw/GebangEditor-Konten
- Katalog JSON disimpan apa adanya di `app/src/main/assets/gebang/katalog.json`.
- Nama item, kategori, status premium/gratis, dan daftar nama file dibaca dari katalog sumber.
- Repositori publik yang diperiksa belum menyediakan source code mesin editor. Karena itu, ViTor membangun mesin native sendiri dan tidak mengklaim identik sebelum diuji terhadap aplikasi sumber.

## Kondisi implementasi saat ini

- [x] Kerangka Android native Kotlin + Jetpack Compose.
- [x] Import beberapa video lokal dengan izin URI persisten.
- [x] Preview klip menggunakan Media3 ExoPlayer.
- [x] Daftar klip, pilih, hapus, dan ubah urutan.
- [x] Trim satu klip berdasarkan rentang waktu dan ekspor MP4.
- [x] Gabungkan klip sesuai urutan daftar menggunakan Media3 Transformer.
- [x] Tambahkan musik latar, atur level volume musik, dan loop track musik saat render.
- [x] Terapkan judul teks statis dan filter asli, grayscale, atau high-contrast saat render.
- [x] Model data subtitle dengan timestamp milidetik dan timing per kata.
- [x] Impor, edit teks, tambah, hapus, dan ekspor subtitle SRT.
- [ ] Timeline multitrack visual yang dapat menggeser dan memangkas setiap track secara independen.
- [ ] Simpan/buka proyek agar susunan klip, audio, teks, filter, dan subtitle dapat dilanjutkan.
- [ ] Subtitle bertiming yang dapat dibakar ke video hasil render.
- [ ] Transisi, keyframe, animasi teks, dan efek per-klip.
- [ ] Mesin speech-to-text offline untuk Auto Caption.
- [ ] Pengenalan lirik dan penyelarasan per kata untuk Auto Lyrics.

## Status implementasi 0.6.0

Versi pengembangan 0.6.0 memiliki impor beberapa klip, preview, pengaturan urutan, trim dan ekspor klip, penggabungan klip, musik latar dengan kontrol volume, judul teks statis, filter video, serta pengelolaan subtitle SRT. Subtitle saat ini dapat diimpor, diedit, ditambah, dihapus, dan diekspor kembali. Subtitle belum dibakar ke hasil video. UI timeline saat ini masih berupa daftar klip berurutan, belum menjadi timeline multitrack penuh.

Ekspor menggunakan Media3 Transformer. Keberhasilan build dan unit test CI membuktikan kompilasi dan pengujian unit yang tersedia, bukan validasi runtime pada seluruh perangkat atau codec. Pengujian perangkat ARM64 tetap diperlukan untuk memastikan hasil render, sinkronisasi audio, kompatibilitas codec, performa, dan penggunaan memori.

## Rencana Auto Caption dan Auto Lyrics
Targetnya adalah pemrosesan audio lokal di perangkat, dengan model yang dapat diunduh/dikelola terpisah dari APK. Speech-to-text untuk vlog dan pengenalan lirik lagu diperlakukan sebagai dua mode berbeda. Hasil pengenalan harus dapat dikoreksi manual. Mode lirik membutuhkan word-level alignment agar sorotan karaoke mengikuti audio.

Referensi teknis yang sedang dievaluasi:
- sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
- whisper.cpp Android: https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android

Belum ada engine ASR yang terintegrasi di APK saat ini. Tidak ada tombol Auto Caption/Auto Lyrics yang boleh ditampilkan sebagai fitur aktif sebelum pipeline audio, model, timestamp, dan pengujian perangkat selesai.

## Pengujian
GitHub Actions menjalankan unit test sebelum membangun APK debug. Uji perangkat ARM64 tetap diperlukan untuk memvalidasi penggabungan file dengan berbagai codec, performa, konsumsi memori, dan hasil rendering. Build CI yang sukses hanya membuktikan kompilasi dan unit test, bukan pengujian runtime di perangkat.