# ViTor

ViTor adalah editor video native Android yang sedang dikembangkan dengan Kotlin, Jetpack Compose, dan AndroidX Media3.

## Fitur yang tersedia pada tahap pengembangan
- Impor beberapa video melalui pemilih dokumen Android.
- Pratinjau video.
- Susun ulang dan hapus klip.
- Potong rentang klip dan ekspor MP4.
- Gabungkan klip sesuai urutan.
- Tambahkan musik latar dan atur volumenya.
- Tambahkan judul teks statis dan filter video.
- Impor, edit teks, tambah, hapus, dan ekspor subtitle SRT.
- Simpan dan buka data proyek lokal.

## Fitur yang masih perlu diselesaikan
- Timeline multitrack visual dengan pengeditan independen untuk setiap track.
- Trim dan split per klip yang tersimpan konsisten di proyek.
- Subtitle bertiming yang dapat dibakar ke video.
- Transisi, keyframe, animasi teks, dan efek per-klip.
- Mesin speech-to-text offline untuk Auto Caption.
- Mesin pengenalan lirik dan penyelarasan kata untuk Auto Lyrics.
- Pengujian kompatibilitas codec, performa, dan stabilitas di perangkat nyata.

## Privasi dan pemrosesan
Aplikasi dirancang untuk memproses file pilihan pengguna secara lokal. Aplikasi tidak memerlukan akun. Email dukungan: koinklub@gmail.com. Lihat draf kebijakan privasi di `docs/PRIVACY_POLICY_ID.md`; dokumen tersebut harus ditinjau dan dipublikasikan pada URL publik sebelum rilis.

## Build
CI menjalankan unit test dan membangun APK debug. Workflow rilis AAB membutuhkan upload keystore dan secret signing yang dikelola pemilik akun developer. Lihat `docs/PLAY_STORE_RELEASE_CHECKLIST_ID.md`.

Build CI yang sukses membuktikan kompilasi dan pengujian unit yang tersedia, bukan pengujian runtime pada seluruh perangkat.
