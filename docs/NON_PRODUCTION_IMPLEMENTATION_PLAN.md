# Rencana Implementasi Non-Production Batch 8A/8B

> Status 20 Agustus 2026: Tahap A dan B telah diimplementasikan dan masuk regression Batch 8C. Dokumen ini dipertahankan sebagai catatan keputusan awal. Tahap C (multi-satuan dan perluasan offline master data) tetap tidak diimplementasikan. Status operasional terkini ada di `README.md` dan laporan Batch 8C backend.

Lanjutkan perbaikan non-production pada `tb-terminal-app` dan backend hanya jika diperlukan.

Fokus:

1. Ubah status koneksi mobile menggunakan `GET /api/readiness`, bukan health process.
2. Hubungkan profil ke `GET /api/auth/me`.
3. Aktifkan perubahan password dan PIN sendiri sesuai endpoint backend.
4. Hapus data profil bentukan/placeholder.
5. Persistensikan pengaturan perangkat seperti auto print, paper size, cash tolerance, dan auto-lock.
6. Hapus printer dummy dan gunakan Android Print Framework.
7. Tegaskan offline sync hanya mendukung `TRANSACTION`, `CASH_SESSION`, dan `CASH_EXPENSE`.
8. Cegah entity unsupported masuk antrean sync.
9. Tambahkan unit test untuk readiness, profil, settings, dan offline sync.
10. Jangan mengubah aturan bisnis POS, stok, atau piutang.

Pastikan build mobile/backend berhasil dan laporkan bug, file yang diubah, test, serta langkah pengujian manual.
Non-Production

Tanggal audit: 20 Agustus 2026  
Repository mobile: `E:\tbterminalapp`  
Repository backend: `E:\tbterminal_backend`

## 1. Batas pekerjaan

Dokumen ini merinci pekerjaan yang masih perlu dilakukan setelah audit Batch 1-8. Pekerjaan production berikut **ditunda dan tidak boleh dieksekusi** sampai ada instruksi terpisah:

- deployment backend ke server production;
- pengisian secret dan environment variable production;
- pembuatan atau pemasangan sertifikat HTTPS;
- pembuatan/penyimpanan release keystore dan signed APK;
- restore terhadap database production;
- aktivasi scheduler backup production, monitoring eksternal, atau alerting;
- distribusi artifact ke pengguna.

Perubahan non-production harus dikerjakan dan diverifikasi di local/test atau staging yang terisolasi. Tidak boleh menggunakan salinan database production yang mengandung data sensitif.

## 2. Ringkasan status koneksi fitur

Fitur utama Batch 1-7 sudah mempunyai alur backend dan mobile, tetapi belum semuanya tersambung penuh. Gap yang terkonfirmasi adalah:

| Area | Status saat audit | Keputusan |
|---|---|---|
| POS, checkout, idempotency, stok, void, laporan | Terhubung | Pertahankan; hanya regression test |
| Piutang, pembayaran, reversal, histori | Terhubung | Pertahankan; tambahkan koneksi adjustment |
| Saldo awal piutang | Terhubung | Pertahankan |
| Adjustment piutang | Endpoint backend ada, mobile belum memanggil | Implementasikan |
| Backup/restore PostgreSQL | Backend ada, layar mobile hanya backup Room lokal | Implementasikan sebagai menu server terpisah |
| Profil pengguna | Identitas di layar kasir masih data bentukan; simpan profil belum aktif | Hubungkan data nyata dan perubahan password/PIN |
| Pengaturan perangkat | Beberapa nilai hanya hidup selama layar aktif; printer masih dummy | Persistensikan; gunakan Android Print Framework |
| Health/readiness | Mobile memeriksa health proses, belum readiness database | Ubah indikator operasional ke readiness |
| Offline sync | Nyata hanya untuk transaksi, sesi kas, dan pengeluaran kas | Tegaskan batas; jangan menjanjikan sinkronisasi master data |
| Konversi satuan | UI placeholder dan backend belum mempunyai model/endpoint | Tunda sampai aturan bisnis disetujui |

## 3. Urutan implementasi yang disarankan

### Tahap A - Stabilitas tanpa perubahan aturan bisnis

1. Gunakan readiness backend untuk status koneksi operasional.
2. Hubungkan profil ke identitas sesi/API nyata serta aktifkan perubahan password dan PIN.
3. Persistensikan pengaturan perangkat dan hapus pilihan printer dummy.
4. Batasi dan jelaskan cakupan offline sync yang benar.

Tahap A dapat dimulai tanpa keputusan bisnis baru.

### Tahap B - Menyambungkan endpoint yang sudah tersedia

1. Tambahkan adjustment piutang pada mobile.
2. Tambahkan pengelolaan backup/restore PostgreSQL dari mobile dengan pengamanan berlapis.

Tahap B tidak mengubah perhitungan POS/stok/piutang, tetapi restore wajib diuji hanya pada database test terisolasi.

### Tahap C - Memerlukan persetujuan bisnis

1. Konversi satuan produk.
2. Perluasan offline sync untuk produk, kategori, satuan, pelanggan, pembayaran, piutang, stok, dan audit log.

Tahap C tidak boleh digabung sebagai bugfix karena membutuhkan skema data, aturan konflik, dan perubahan perhitungan stok.

## 4. Rincian implementasi

### 4.1 Readiness sebagai sumber status backend

#### Masalah

Mobile memanggil `GET /api/health` dan fallback `GET /`. Kedua endpoint dapat berhasil saat proses backend hidup tetapi PostgreSQL belum siap. Akibatnya aplikasi dapat menampilkan status terhubung lalu request bisnis gagal.

#### Perubahan mobile

- Ubah `HealthApi.kt`:
  - tambahkan `GET /api/readiness`;
  - pertahankan health hanya sebagai diagnostik proses, bukan keputusan untuk sinkronisasi/checkout.
- Ubah `BackendHealthMonitor.kt`:
  - `CONNECTED` hanya jika readiness menghasilkan HTTP 2xx;
  - HTTP 503, timeout, DNS error, atau response tidak valid menghasilkan `UNREACHABLE`;
  - jangan fallback dari readiness gagal ke health sukses;
  - simpan alasan ringkas untuk UI/diagnostik tanpa mengekspos response sensitif.
- Sesuaikan `OfflineStatusRepository.kt` dan `SyncManager.kt` bila diperlukan agar antrean hanya diproses setelah readiness berhasil.

#### Test

- readiness 200 -> `CONNECTED`;
- health 200 tetapi readiness 503 -> `UNREACHABLE`;
- timeout/read error -> `UNREACHABLE` dan tidak crash;
- perubahan jaringan memicu pemeriksaan ulang;
- antrean sync tidak dikirim ketika database belum ready.

#### Kriteria penerimaan

- Indikator online berarti backend **dan database** siap menerima request bisnis.
- Kegagalan readiness tidak menghapus antrean lokal.

### 4.2 Profil nyata dan perubahan credential

#### Masalah

- Layar profil admin masih placeholder.
- Layar profil kasir membentuk username/email dari nama lokal dan tombol simpan belum memanggil API.
- Backend sudah menyediakan identitas pengguna serta endpoint perubahan password/PIN.

#### Kontrak API yang digunakan

- `GET /api/auth/me` untuk identitas pengguna aktif.
- `PUT /api/system/users/me/password` untuk mengganti password sendiri.
- `PUT /api/system/users/me/pin` untuk mengganti PIN sendiri.

Semua role yang sudah login boleh mengganti credential miliknya sendiri. Pengelolaan user lain tetap hanya owner sesuai RBAC backend.

#### Perubahan mobile

- Tambahkan method/DTO profil pada `AuthApi.kt` atau API khusus `ProfileApi.kt`.
- Tambahkan `ProfileRepository.kt` untuk:
  - membaca profil nyata;
  - mengganti password dengan password lama dan password baru sesuai kontrak backend;
  - mengganti PIN sesuai kontrak backend;
  - memetakan 400/401/403 dan validation error menjadi pesan aman.
- Tambahkan `ProfileViewModel.kt` dengan state loading, success, validation error, dan session expired.
- Ganti `AdminPlaceholderRoute` pada `DashboardNavGraph.kt` dengan layar profil yang sama/komponen bersama.
- Ubah `SharedProfileScreen.kt`:
  - jangan membentuk email palsu;
  - tampilkan hanya data yang benar-benar dikirim server;
  - nama, username, role, dan email bersifat read-only untuk tahap ini;
  - sediakan dialog perubahan password/PIN;
  - bersihkan input credential setelah request selesai.

Perubahan nama/email sendiri tidak dimasukkan karena backend belum menyediakan aturan otorisasi/audit untuk itu. Jika dibutuhkan, harus menjadi keputusan terpisah.

#### Validasi mobile

- field wajib tidak boleh kosong;
- password baru mengikuti minimum backend dan tidak sama dengan password lama;
- konfirmasi password harus sama;
- PIN hanya digit dan panjangnya mengikuti aturan backend;
- tombol submit nonaktif selama request;
- credential tidak ditulis ke log atau saved state.

#### Test

- profil menampilkan response server, bukan data bentukan;
- role owner/admin/kasir dapat mengubah credential sendiri;
- user tidak dapat mengubah credential user lain dari layar ini;
- validation error tidak mengirim request;
- 401 menghapus sesi dan kembali ke login;
- sukses mengganti password/PIN menghasilkan pesan berhasil dan audit backend.

#### Kriteria penerimaan

- Tidak ada username/email palsu di layar profil.
- Tidak ada password/PIN dalam log, database Room, analytics, atau crash message.

### 4.3 Persistensi pengaturan perangkat dan printer

#### Masalah

`SettingsViewModel.saveLocalPreferences()` belum menyimpan seluruh preferensi secara permanen dan pemilihan printer mengisi nama dummy `Bluetooth Printer 58mm`. Struk sebenarnya sudah memakai Android `PrintManager`.

#### Perubahan mobile

- Tambahkan key pada `AppSettingKeys.kt`, misalnya:
  - `AUTO_PRINT_RECEIPT`;
  - `PAPER_SIZE`;
  - `CASH_TOLERANCE`;
  - `AUTO_LOCK_MINUTES`;
  - `LAST_PRINT_SERVICE` hanya bila Android memberikan identifier aman/stabil.
- Tambahkan getter, observer, dan setter bertipe pada `LocalAppSettingsDataSource.kt`.
- Ubah `SettingsViewModel.kt` agar nilai dimuat dari Room, divalidasi, lalu disimpan atomik dari sudut pandang UI.
- Hapus `selectDummyPrinter()` dan teks yang memberi kesan printer Bluetooth sudah dipasangkan.
- Gunakan Android Print Framework yang sudah dipakai oleh:
  - `AdminReceiptDetailScreen.kt`;
  - `ReceivablePaymentReceiptDialog.kt`.
- Tombol uji cetak menghasilkan halaman uji melalui `PrintManager`; pemilihan perangkat tetap melalui dialog sistem Android.
- Jangan menambahkan driver ESC/POS langsung sebelum merek/model printer dan encoding kertas disepakati.
- Fitur barcode tetap disembunyikan/dinonaktifkan karena Batch 8 secara eksplisit mengecualikan fitur barcode lanjutan.

#### Validasi

- ukuran kertas hanya nilai yang didukung;
- toleransi kas angka non-negatif dan maksimal dua desimal;
- auto-lock berada dalam rentang aman;
- kegagalan print tidak mengubah status transaksi atau pembayaran.

#### Test

- nilai bertahan setelah ViewModel dibuat ulang dan aplikasi dibuka ulang;
- input invalid tidak tersimpan;
- print dibatalkan pengguna tidak dianggap error transaksi;
- tidak ada nama printer dummy pada state awal.

#### Kriteria penerimaan

- Pengaturan benar-benar persisten per perangkat.
- Cetak struk tetap dapat diulang tanpa membuat transaksi/pembayaran baru.

### 4.4 Batas offline sync yang eksplisit

#### Masalah

Enum lokal menyebut banyak entity, tetapi implementasi retry nyata hanya tersedia untuk `TRANSACTION`, `CASH_SESSION`, dan `CASH_EXPENSE`. Entity lain berakhir sebagai `Unsupported` jika masuk antrean.

#### Perubahan mobile yang aman

- Definisikan satu konstanta/registry `SUPPORTED_OFFLINE_SYNC_TYPES` yang hanya berisi tiga tipe tersebut.
- Validasi saat enqueue sehingga entity unsupported tidak pernah masuk antrean.
- Ubah `SyncMonitoringRepository.kt` dan layar Sync Center untuk menampilkan cakupan yang benar.
- Bila database lama sudah mengandung item unsupported:
  - jangan hapus diam-diam;
  - tandai sebagai `FAILED_PERMANENT`/status yang setara dengan alasan migrasi yang jelas;
  - sediakan audit/log lokal tanpa payload sensitif.
- Dokumentasikan bahwa master data memerlukan koneksi server pada versi ini.

#### Test

- hanya tiga tipe didaftarkan ke antrean;
- item supported tetap idempotent ketika retry;
- item legacy unsupported tidak menyebabkan loop WorkManager;
- UI tidak mengklaim produk/pelanggan/piutang akan tersinkron offline.

#### Kriteria penerimaan

- Tidak ada antrean yang gagal berulang tanpa kemungkinan sukses.
- Pengguna mengetahui operasi mana yang aman dilakukan offline.

### 4.5 Adjustment piutang pada mobile

#### Masalah

Backend mempunyai `POST /api/receivable/receivables/adjustment` untuk owner/admin, tetapi `ReceivableApi.kt` belum mengekspos endpoint tersebut.

#### Perubahan mobile

- Tambahkan `createAdjustment()` pada `ReceivableApi.kt` menggunakan DTO standalone yang sudah ada.
- Tambahkan method repository dan state ViewModel.
- Tambahkan aksi `Adjustment Piutang` hanya untuk owner/admin pada area piutang, bukan pada checkout.
- Form berisi pelanggan aktif, nominal, tanggal piutang, jatuh tempo, nomor referensi lama bila ada, dan catatan/alasan wajib.
- Client tidak perlu mengirim sumber yang dapat dipercaya; backend tetap memaksa `source = ADJUSTMENT`.
- Setelah sukses, refresh detail piutang dan ringkasan pelanggan.

#### Validasi

- nominal positif dan maksimal dua desimal, mengikuti endpoint backend saat ini;
- tanggal piutang tidak di masa depan;
- jatuh tempo tidak sebelum tanggal piutang;
- pelanggan aktif;
- catatan/alasan tidak kosong;
- kasir tidak melihat atau dapat memanggil aksi ini.

Catatan: endpoint saat ini membuat record piutang adjustment baru dengan nominal positif. Adjustment negatif terhadap piutang tertentu bukan perilaku endpoint ini dan tidak boleh diasumsikan tanpa perubahan desain backend.

#### Test

- owner/admin berhasil; kasir mendapat 403 dan tidak melihat tombol;
- field invalid tidak memanggil API;
- response berstatus sumber `ADJUSTMENT`;
- ringkasan/status pelanggan diperbarui;
- audit log backend tercatat;
- error jaringan tidak menggandakan submit dari UI.

#### Kriteria penerimaan

- Tidak ada perubahan pada checkout POS.
- Adjustment dapat dilacak sebagai piutang mandiri dan tidak dapat dihapus permanen.

### 4.6 Backup/restore PostgreSQL dari mobile

#### Masalah

Menu mobile saat ini hanya backup/restore database Room lokal. Backend sudah menyediakan backup PostgreSQL, metadata, checksum, retensi, validasi file, confirmation token/phrase, dan audit, tetapi belum ada client mobile.

#### Pemisahan UI wajib

- `Backup Lokal Perangkat`: mempertahankan fungsi Room yang ada.
- `Backup Database Server`: fitur baru untuk owner/admin.

Label dan peringatan harus tegas agar pengguna tidak mengira backup lokal mencakup seluruh data server.

#### Kontrak backend yang sudah ada

- `GET /api/system/database-backups?limit=...` — daftar job.
- `POST /api/system/database-backups` — membuat backup server.
- `GET /api/system/database-backups/{id}/download` — unduh file.
- `POST /api/system/database-backups/restore/validate` — upload multipart dan menerima job, confirmation token, phrase, serta expiry.
- `POST /api/system/database-backups/restore/{id}/confirm` — mengirim token, phrase, dan acknowledgment overwrite/downtime.

#### Perubahan mobile

- Tambahkan `DatabaseBackupApi.kt` beserta DTO job, validation, dan confirm.
- Tambahkan `ServerBackupRepository.kt`:
  - list/create/download;
  - upload file melalui streaming `RequestBody`, bukan membaca seluruh file ke memori;
  - verifikasi MIME/extension/ukuran awal lalu tetap mempercayakan validasi isi/checksum ke backend;
  - mapping status `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`.
- Tambahkan `ServerBackupViewModel.kt` dan bagian server pada `BackupRestoreScreen.kt`.
- Gunakan Storage Access Framework untuk memilih file restore dan tujuan download.
- Jangan menyimpan confirmation token ke Room/log; simpan hanya di memory sampai kadaluarsa.
- Tampilkan checksum, ukuran, waktu, pembuat, status, dan error terpotong yang aman.
- Setelah restore sukses atau koneksi terputus secara ambigu:
  - hapus cache data remote;
  - paksa logout/re-authentication;
  - jangan otomatis retry confirm karena operasi bersifat destruktif;
  - muat ulang daftar job setelah login ulang.

#### Timeout dan rekomendasi backend

Client HTTP normal memiliki timeout pendek dan tidak cocok untuk `pg_dump`/`pg_restore`. Pilihan minimal adalah client khusus backup dengan upload/download timeout panjang. Pilihan yang direkomendasikan adalah menjadikan create/confirm sebagai job asynchronous:

1. POST menerima request dan segera mengembalikan `202 Accepted` + job id.
2. Tambahkan `GET /api/system/database-backups/{id}`.
3. Mobile melakukan polling dengan backoff sampai status terminal.
4. Retry polling aman; retry confirm tetap dilarang kecuali status job membuktikan belum dimulai.

Perubahan asynchronous tidak mengubah aturan bisnis, tetapi mencegah hasil ambigu ketika koneksi Android putus selama operasi panjang.

#### Pengamanan role dan konfirmasi

- owner/admin saja pada UI dan backend;
- file harus lulus validate sebelum tombol confirm aktif;
- pengguna mengetik phrase persis;
- checkbox acknowledgment downtime/overwrite wajib;
- dialog terakhir menampilkan nama file, checksum, target environment, dan konsekuensi;
- restore ke production tetap dilarang dalam tahap ini.

#### Test

- kasir tidak melihat menu dan mendapat 403 bila memanggil langsung;
- daftar metadata dan status ter-render benar;
- download memakai stream dan checksum metadata terlihat;
- file kosong, terlalu besar, format salah, checksum invalid, token salah/kadaluarsa ditolak;
- confirm ganda tidak menjalankan dua restore;
- timeout/pindah jaringan tidak melakukan retry destruktif;
- restore test selalu membuat safety backup dan audit;
- setelah restore, sesi mobile diakhiri dengan aman.

#### Kriteria penerimaan

- Backup lokal dan server tidak tertukar.
- Tidak ada password database, path internal server, token konfirmasi, atau isi backup sensitif di log/mobile database.
- Semua uji restore menggunakan PostgreSQL test terisolasi.

### 4.7 Konversi satuan - ditunda menunggu persetujuan

#### Kondisi

`ProductFormFields.kt` menyatakan endpoint konversi belum tersedia. Menambah konversi bukan sekadar menyambungkan UI: perubahan ini berdampak pada master produk, harga snapshot checkout, kuantitas desimal, kartu stok, laporan, impor CSV, dan rekonsiliasi saldo.

#### Keputusan yang harus disetujui dahulu

- Apakah satu produk dapat dijual dalam banyak satuan?
- Faktor konversi diarahkan ke base unit atau antar-satuan?
- Apakah harga jual/beli dapat berbeda per satuan?
- Pembulatan kuantitas dan uang menggunakan aturan apa?
- Apakah stok selalu disimpan dalam base unit?
- Bagaimana impor dan perubahan faktor menangani transaksi lama?

#### Rancangan jika disetujui

- tabel `product_unit_conversions` dengan unique `(product_id, unit_id)`, faktor positif, harga opsional, active flag, audit timestamps;
- stok dan ledger selalu dalam base unit;
- item transaksi menyimpan snapshot unit, faktor, kuantitas input, kuantitas base, dan harga;
- endpoint CRUD owner/admin dengan optimistic version atau locking;
- validasi perubahan faktor tidak mengubah snapshot histori;
- test checkout, void, kartu stok, CSV import, rounding, concurrency, dan laporan.

Sebelum disetujui, section UI konversi harus disembunyikan atau diberi status `Belum tersedia`, bukan tombol yang tampak aktif.

### 4.8 Perluasan offline master data - ditunda menunggu persetujuan

Perluasan sync harus dirancang per entity dengan:

- server-generated version/ETag atau `updatedAt` yang konsisten;
- idempotency key untuk mutation;
- kebijakan konflik yang eksplisit (server wins, client wins, atau manual merge);
- tombstone untuk soft delete;
- dependency ordering, misalnya kategori/satuan sebelum produk dan pelanggan sebelum piutang;
- aturan stock ledger yang tidak pernah sekadar menimpa angka stok;
- audit actor/device/idempotency;
- test dua perangkat mengubah record yang sama.

Tanpa keputusan tersebut, implementasi offline master data berisiko menimpa SKU, saldo stok, batas kredit, atau histori. Karena itu pekerjaan ini dipisahkan dari perbaikan stabilitas.

## 5. Target file

### Mobile - file baru yang diperkirakan

- `app/src/main/java/com/tbterminal/app/data/remote/DatabaseBackupApi.kt`
- `app/src/main/java/com/tbterminal/app/data/repository/ServerBackupRepository.kt`
- `app/src/main/java/com/tbterminal/app/ui/backup/ServerBackupViewModel.kt`
- `app/src/main/java/com/tbterminal/app/data/repository/ProfileRepository.kt`
- `app/src/main/java/com/tbterminal/app/ui/profile/ProfileViewModel.kt`

### Mobile - file yang diperkirakan berubah

- `data/remote/HealthApi.kt`
- `data/remote/AuthApi.kt` atau API profil baru
- `data/remote/ReceivableApi.kt`
- `data/remote/NetworkModule.kt`
- `data/di/AppContainer.kt`
- `data/local/model/AppSettingKeys.kt`
- `data/local/database/LocalAppSettingsDataSource.kt`
- `data/sync/BackendHealthMonitor.kt`
- `data/sync/SyncManager.kt`
- `data/sync/SyncMonitoringRepository.kt`
- `ui/backup/BackupRestoreScreen.kt`
- `ui/dashboard/DashboardNavGraph.kt`
- `ui/profile/SharedProfileScreen.kt`
- `ui/settings/SettingsViewModel.kt`
- layar/list/detail piutang yang menjadi entry adjustment

### Backend - perubahan minimal

- Untuk Tahap A dan adjustment mobile: tidak wajib mengubah backend selain test kontrak/regresi.
- Untuk backup asynchronous yang direkomendasikan:
  - `backup/BackupRoutes.kt`;
  - `backup/BackupService.kt`;
  - `backup/BackupRepository.kt`;
  - `backup/BackupModels.kt`;
  - test route/service/repository terkait.

## 6. Matriks test non-production

| Lapisan | Pengujian wajib |
|---|---|
| Mobile unit | validasi profil, settings, adjustment, mapping backup status, readiness |
| Mobile API | MockWebServer untuk 2xx/400/401/403/409/422/500, malformed JSON, timeout |
| Backend unit | validasi restore/job transition dan kontrak adjustment yang sudah ada |
| Backend integration | PostgreSQL test aktif untuk RBAC, audit, checksum, confirm expiry, concurrent confirm |
| E2E local/staging | login tiap role, adjustment, backup create/download, invalid restore, valid restore database test, re-login |
| Regression | seluruh test Batch 1-7, debug build mobile, backend test dan artifact lokal |

Build/test yang dijalankan pada implementasi nanti tidak boleh menyertakan signing release atau koneksi production.

## 7. Definition of Done

Pekerjaan non-production dianggap selesai jika:

1. Semua gap Tahap A dan Tahap B mempunyai implementasi, test, dan dokumentasi manual.
2. Owner/admin/kasir melihat aksi sesuai role; backend tetap menjadi otoritas terakhir.
3. Readiness database mengendalikan status operasional mobile.
4. Profil tidak menampilkan data palsu dan credential tidak tersimpan/log.
5. Pengaturan perangkat persisten dan printer dummy dihapus.
6. Cakupan offline sync sesuai kemampuan nyata.
7. Adjustment piutang mobile terhubung tanpa mengubah checkout.
8. Backup lokal dan backup server terpisah jelas; restore diuji hanya di database test.
9. Seluruh unit/integration test lulus dengan PostgreSQL test aktif.
10. Mobile debug build dan backend artifact lokal berhasil tanpa secret production.
11. Tahap C tetap tidak diimplementasikan sampai keputusan bisnis ditandatangani.

## 8. Checklist persetujuan sebelum implementasi

- [ ] Setuju Tahap A dikerjakan tanpa perubahan bisnis.
- [ ] Setuju adjustment backend saat ini hanya menambah piutang positif mandiri.
- [ ] Pilih backup synchronous dengan timeout khusus atau job asynchronous (direkomendasikan).
- [ ] Setuju restore hanya diuji pada PostgreSQL local/test terisolasi.
- [ ] Konfirmasi hardware printer yang akan dipakai untuk UAT fisik.
- [ ] Putuskan apakah konversi satuan masuk batch baru atau tetap ditunda.
- [ ] Putuskan apakah master-data offline sync memang dibutuhkan.
- [ ] Berikan instruksi terpisah sebelum pekerjaan production apa pun dilakukan.
