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

- Owner: seluruh area backoffice, termasuk user management, audit, adjustment piutang, dan backup/restore server.
- Admin: operasional/master data, transaksi, laporan, dan audit; tidak mengelola user, backup/restore server, atau security settings.
- Kasir: dashboard kasir, sesi kas, POS, histori/struk yang diizinkan, pembayaran piutang; menu administratif disembunyikan dan backend tetap menolak pemanggilan langsung.

## Backup dan cetak

`Backup Lokal Perangkat` hanya mencakup database Room perangkat. `Backup Database Server` mengelola job PostgreSQL dan hanya terlihat untuk Owner. Upload/download memakai Storage Access Framework dan streaming. Confirmation token restore hanya hidup di memory; confirm restore tidak di-retry otomatis bila respons ambigu.

Cetak memakai Android Print Framework. Pembatalan dialog print tidak mengubah transaksi atau pembayaran. Auto print, paper size, cash tolerance, dan auto-lock dipersistensikan sebagai pengaturan lokal perangkat.

## Troubleshooting ringkas

- Status offline tetapi `/health` hidup: periksa `/api/readiness` dan koneksi PostgreSQL.
- 401 berulang: login ulang; jangan memaksa memakai refresh token lama.
- Timeout checkout/pembayaran: cari histori dan pertahankan idempotency key.
- Sync gagal permanen: lihat tipe entity; master data memang tidak didukung offline.
- Restore ambigu: jangan tekan confirm ulang; login ulang dan periksa status job.
- Printer tidak muncul: periksa print service Android dan pilih tujuan pada dialog sistem.

Dokumentasi backend, backup/restore, pengujian, dan checklist UAT tersedia di repository `tb-terminal-service/docs`.
## Manager Approval UI

Infrastructure frontend Manager Approval tersedia sebagai satu flow reusable:

```text
ManagerApprovalDialog
        ↓
ManagerApprovalViewModel
        ↓
ManagerApprovalRepository
        ↓
POST /api/system/manager-approvals
```

Action frontend bersifat type-safe dan dibatasi pada `VOID_TRANSACTION`,
`REFUND_TRANSACTION`, serta `DISCOUNT_OVERRIDE`. Pemanggil memberikan
`ManagerApprovalContext(action, resourceId)` dan menerima `ManagerApprovalGrant`
melalui callback `onApproved`. Dialog tidak menjalankan Void, Refund, atau
Discount; fitur pemanggil bertanggung jawab memakai `approvalId` pada request
bisnis berikutnya.

PIN manager hanya berada di memory selama verifikasi, tidak disimpan ke Room,
DataStore, SharedPreferences, log, atau analytics. Input PIN dibersihkan saat
submit, error, berhasil, dan dialog dibatalkan.

## Checkout, diskon, dan preview

POS mendukung diskon item dan transaksi dengan tipe `PERCENTAGE` atau
`FIXED_AMOUNT`. Diskon nominal item berlaku pada total baris. Nilai yang tampil
sebelum preview hanya estimasi UX; harga, gross, nominal diskon, batas kasir,
keharusan approval, dan net final selalu berasal dari
`POST /api/sales/checkout/preview`.

Jika backend menandai `approvalRequired`, aplikasi menggunakan dialog Manager
Approval yang sama dengan action `DISCOUNT_OVERRIDE` dan resource
`checkoutAttemptId`. Perubahan produk, quantity, atau diskon membuang preview
lama. Final checkout mengirim intent diskon, `checkoutAttemptId`, dan
`managerApprovalId` bila dibutuhkan; aplikasi tidak mengirim nominal diskon atau
total lokal sebagai sumber kebenaran.

Diskon tidak didukung pada checkout offline. Timeout/response ambigu diputar
ulang memakai request dan idempotency key yang sama; keranjang tidak dapat
diubah sampai status checkout tersebut diperiksa. Pembayaran dan DP divalidasi
terhadap net total, termasuk transaksi gratis dengan net nol untuk metode
non-piutang.

## Void dan full refund transaksi

Detail transaksi menyediakan Void dan Full Refund untuk transaksi penjualan
yang belum `VOIDED` atau `REFUNDED`. Owner/Admin mengeksekusi langsung. Kasir
menggunakan Manager Approval reusable dengan action `VOID_TRANSACTION` atau
`REFUND_TRANSACTION`, serta resource ID transaksi yang sedang dibuka.

Full Refund mendukung kondisi barang `RETURN_TO_STOCK`, `NOT_RETURNED`, dan
`DAMAGED`. Nominal refund tidak dihitung di Android; nilai transaksi dan nominal
refund berasal dari respons backend. Partial Refund belum didukung.

Void dan Refund memakai idempotency key terpisah. Pada timeout atau response
ambigu, form dikunci dan retry memakai alasan, disposition, approval ID, serta
idempotency key yang sama. Setelah berhasil, aplikasi memuat ulang detail dari
server dan menampilkan status terbaru beserta informasi refund yang tersedia.

## Role page alignment

Seluruh visibilitas route, menu, dan action sensitif mengikuti satu sumber
capability di `AppAccessPolicy`. Owner memperoleh kontrol backoffice penuh.
Admin memperoleh fungsi operasional, tetapi tidak melihat manajemen pengguna,
backup/restore database server, atau pengaturan keamanan. Kasir hanya melihat
POS, sesi kas, transaksi miliknya, pelanggan read-only, piutang/pembayaran,
pengaturan perangkat, dan akun.

Dashboard Owner menampilkan penjualan, pendapatan bersih, refund, diskon,
piutang, hutang, dan stok. Ringkasan pendapatan bersih/refund/diskon dibaca dari
laporan backend untuk tanggal hari ini dan tidak dihitung di perangkat.
Dashboard Admin hanya menampilkan ringkasan operasional. Dashboard Kasir tidak
menampilkan analytics manajemen, target contoh, status printer palsu, atau menu
stok backoffice.

Deep link yang tidak sesuai role ditolak oleh `AppRouteAccessPolicy`. Respons
HTTP 403 ditampilkan sebagai `Anda tidak memiliki akses ke fitur ini.`;
penyembunyian UI tetap bukan pengganti validasi izin dari backend.

## UX dan penanganan error

Respons transport dipetakan melalui satu `UserFacingErrorMapper`: input tidak
valid (400), sesi habis (401), akses ditolak (403), data tidak ditemukan (404),
konflik (409), terlalu banyak percobaan (429), gangguan server (5xx), jaringan,
timeout, dan respons tidak valid. Kode error bisnis tetap diteruskan ke
ViewModel agar flow approval, idempotency, Void, Refund, dan checkout tidak
kehilangan konteks. Final 401 pada request terautentikasi membersihkan sesi dan
mengembalikan pengguna ke login.

Pola feedback umum tersedia sebagai `AppSnackbar`, `AppConfirmDialog`,
`AppErrorState`, `AppEmptyState`, dan `AppStatusChip`. Snackbar menutup pesan
aktif sebelum menampilkan pesan berikutnya. Dialog tindakan berisiko menyebut
target dan konsekuensi; tombol submit dinonaktifkan serta menampilkan progress
kecil selama request. Initial load tetap memakai skeleton, sedangkan refresh
mempertahankan konten lama.

Form pelanggan menjadi contoh pola field-level validation: error wajib, format
telepon, nominal non-negatif maksimal dua desimal, panjang teks, dan termin
ditampilkan di dekat input. Keyboard Phone/Decimal/Number, navigasi Next/Done,
fokus awal, serta `imePadding` digunakan agar tombol utama tidak tertutup.

Nilai uang dan tanggal baru harus memakai formatter UI bersama agar berbentuk
`Rp 100.000`, `01 Sep 2026`, dan `14:30`. Operasi dengan respons ambigu tetap
mempertahankan payload serta idempotency key; pengguna tidak boleh membuat
request bisnis baru sebelum status transaksi diperiksa.
