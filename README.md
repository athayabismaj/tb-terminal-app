# TB Terminal Android

Client Android non-production untuk `tb-terminal-service`. Batch 1-8C mencakup autentikasi/RBAC, sesi kas, POS, inventory dan kartu stok, piutang dan pembayaran, void, laporan/CSV, profil dan pengaturan perangkat, offline queue terbatas, serta backup lokal/server.

## Menjalankan local/test

Prasyarat: JDK 21, Android SDK, dan backend lokal yang readiness-nya sehat. Debug memakai URL emulator dari konfigurasi source set debug; ubah hanya untuk jaringan development yang memang digunakan. Jangan memasukkan URL production, token, password, atau signing key ke repository.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

APK debug dihasilkan di `app/build/outputs/apk/debug/app-debug.apk`. Release signed tidak dibuat pada audit Batch 8C; prosedur release terpisah ada di `docs/RELEASE.md`.

## Status koneksi dan sesi

- Indikator online menggunakan `GET /api/readiness`, sehingga proses backend dan PostgreSQL harus sama-sama siap.
- `GET /api/auth/me` adalah sumber profil; layar tidak membentuk username/email placeholder.
- HTTP 401 yang tidak dapat dipulihkan akan membersihkan sesi dan mengarahkan pengguna ke login.
- Password dan PIN dapat diubah sendiri; nilainya tidak disimpan di Room atau log.

## Batas offline

Offline queue hanya menerima `TRANSACTION`, `CASH_SESSION`, dan `CASH_EXPENSE`. Produk, kategori, satuan, pelanggan, stok, piutang, pembayaran, laporan, audit, dan backup server tetap membutuhkan koneksi. Entity lain ditolak saat enqueue; item lama yang unsupported dikarantina agar tidak retry tanpa akhir.

Jika checkout/pembayaran mengalami timeout, periksa histori lebih dahulu. Gunakan retry dari alur yang mempertahankan idempotency key; jangan membuat request bisnis baru secara manual.

## Role

- Owner: seluruh area, termasuk user management, audit, adjustment piutang, backup/restore server.
- Admin: operasional/master data, laporan, adjustment, void, dan backup/restore server; tidak mengelola user/audit khusus owner.
- Kasir: dashboard kasir, sesi kas, POS, histori/struk yang diizinkan, pembayaran piutang; menu administratif disembunyikan dan backend tetap menolak pemanggilan langsung.

## Backup dan cetak

`Backup Lokal Perangkat` hanya mencakup database Room perangkat. `Backup Database Server` mengelola job PostgreSQL dan hanya terlihat untuk owner/admin. Upload/download memakai Storage Access Framework dan streaming. Confirmation token restore hanya hidup di memory; confirm restore tidak di-retry otomatis bila respons ambigu.

Cetak memakai Android Print Framework. Pembatalan dialog print tidak mengubah transaksi atau pembayaran. Auto print, paper size, cash tolerance, dan auto-lock dipersistensikan sebagai pengaturan lokal perangkat.

## Troubleshooting ringkas

- Status offline tetapi `/health` hidup: periksa `/api/readiness` dan koneksi PostgreSQL.
- 401 berulang: login ulang; jangan memaksa memakai refresh token lama.
- Timeout checkout/pembayaran: cari histori dan pertahankan idempotency key.
- Sync gagal permanen: lihat tipe entity; master data memang tidak didukung offline.
- Restore ambigu: jangan tekan confirm ulang; login ulang dan periksa status job.
- Printer tidak muncul: periksa print service Android dan pilih tujuan pada dialog sistem.

Dokumentasi backend, backup/restore, pengujian, dan checklist UAT tersedia di repository `tb-terminal-service/docs`.
