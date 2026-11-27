# Backlog Temuan di Luar Scope (Fase B1)

Temuan berikut diidentifikasi saat penyesuaian Fase B1 terhadap security hardening backend, namun tidak diubah di fase ini agar ruang lingkup tetap terisolasi:

- [RENDAH] Refactor file berukuran besar (>2.000 baris) ke komponen UI dan kelas logika terpisah — app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutViewModel.kt, app/src/main/java/com/tbterminal/app/ui/checkout/CashierPaymentScreen.kt, app/src/main/java/com/tbterminal/app/ui/settings/SharedSettingsScreen.kt, app/src/main/java/com/tbterminal/app/ui/salestransactions/AdminReceiptDetailScreen.kt — Kompleksitas pemeliharaan tinggi dan risiko regresi saat menambah atau menguji fitur baru — Fase B2/B3

## Selesai di Fase B1b

- [SELESAI] Satuan sekunder di sync checkout offline — kini mengirim pasangan `unitId` + `baseQuantity` eksak dengan persistensi Room v5 → v6 dan klasifikasi 409 `OFFLINE_UNIT_CONFLICT` (`OfflineCheckoutSyncService.kt`, `TbTerminalDatabase.kt`, `SyncErrorClassifier.kt`)
- [SELESAI] Peringatan proaktif umur sesi kas offline — banner peringatan pada >= 48 jam dan peringatan keras pada >= 66 jam (batas default 72 jam) di `CashierCashSessionScreen.kt`, `CashReconciliationScreen.kt`, dan `SyncCenterScreen.kt`

## Temuan di Luar Scope (Fase B1b)

Temuan berikut diidentifikasi saat pengerjaan Fase B1b namun tidak diubah pada fase ini agar ruang lingkup tetap terisolasi:

- [SEDANG] POS membatasi satuan sekunder hanya untuk faktor > 1 sehingga produk dengan faktor konversi pecahan (< 1) tidak dapat dijual dalam satuan sekunder — app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutUiState.kt, app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutViewModel.kt — Produk dengan faktor konversi antara 0 dan 1 (valid di ProductInputValidation) tidak menampilkan opsi toggle satuan sekunder di layar kasir — Fase B2

## Selesai di Fase B1c

- [SELESAI] Hidrasi satuan stok dari kontrak backend 5b (`GET /api/inventory/stock`) dengan fallback otomatis v1.0.7 (`/products` + `/units`) serta pembersihan cache satuan sekunder Room saat `secondaryUnitId = null` (`InventoryApi.kt`, `InventoryMappers.kt`, `ProductLocalDataSource.kt`, `InventoryRepository.kt`)
- [SELESAI] Perbaikan pemotongan `.toInt()` pada `previewCheckout` dan `submitCheckout` online menggunakan `BigDecimal` eksak (`<= 2` angka desimal, memblokir `> 2` angka desimal), memasukkan faktor konversi ke fingerprint idempotency, serta memblokir produk duplikat (`productId` sama) sebelum preview/submit online (`CheckoutModels.kt`, `CheckoutRepository.kt`, `OnlineCheckoutQuantityResolver.kt`, `CheckoutViewModel.kt`)
- [SELESAI] Penampilan `unitName` dari respons item transaksi server di struk/riwayat/WhatsApp dengan fallback teks lama hanya saat `unitName` null/kosong, serta pembungkusan baris `"qty unitName"` agar sesuai lebar 32/48 kolom printer ESC/POS (`SalesApi.kt`, `CashReconciliationRepository.kt`, `TransactionItemUnitFormatter.kt`, `AdminReceiptDetailScreen.kt`, `WhatsAppReceiptComponents.kt`, `ReceiptEscPosFormatter.kt`, `DeliveryOrderFormatter.kt`)
- [SELESAI] Penyambungan callback verifikasi/ganti/reset PIN dan force-logout di `CashierProfileScreen.kt` dan `SharedProfileScreen.kt` ke `ProfileViewModel`, validasi 4–6 digit, pembersihan PIN dari memori setelah submit/error/cancel, dan penanganan HTTP 429/423 (`ProfileViewModel.kt`, `CredentialChangeModal.kt`)
- [SELESAI] Migrasi serialisasi payload sync lokal dari string concatenation ke `kotlinx.serialization` pada `LocalCheckoutService.kt`, `LocalCashExpenseService.kt`, dan `LocalCashSessionService.kt`

## Temuan di Luar Scope (Fase B1c)

Temuan berikut diidentifikasi saat pengerjaan Fase B1c namun tidak diubah pada fase ini agar ruang lingkup tetap terisolasi:

- [SEDANG] Menggabungkan baris keranjang dengan `productId` sama (satuan dasar + satuan sekunder) menjadi satu baris satuan dasar pada checkout online — app/src/main/java/com/tbterminal/app/ui/checkout/OnlineCheckoutQuantityResolver.kt, app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutViewModel.kt — Saat ini keranjang berisi produk sama dalam satuan dasar + sekunder diblokir sebelum preview/submit online agar sesuai validasi backend online (jalur sync offline `/api/sales/checkout/sync` sudah mendukung item duplikat `productId`) — Fase B2
- [SEDANG] Kebijakan rupiah bulat end-to-end (harga, pajak, diskon, total) — fase backend tersendiri; putuskan aturan pembulatan memakai rounding settings toko.
- [RENDAH] AdminPriceManagementScreen: input diskon harga katalog kemungkinan membaca '15.000' sebagai 15 — masuk fase rupiah bulat end-to-end.

## Selesai di Fase B2a

- [SELESAI] Ketahanan sync offline untuk sesi kas, pengeluaran kas, dan checkout: error transien (`IOException`/timeout/5xx/429) menjaga status tetap `PENDING` tanpa menghabiskan kuota retry per-item; penghormatan `Retry-After` pada HTTP 429 dengan nama unique work terpisah agar worker tidak membatalkan dirinya sendiri; `recoverStaleSyncing()` + serialisasi `OfflineSyncCoordinator` untuk sesi & pengeluaran; pengurasan antrean > 10 item; propagasi ulang `CancellationException` via `runSuspendCatching` (`OfflineCashSessionSyncService.kt`, `OfflineCashExpenseSyncService.kt`, `OfflineCheckoutSyncService.kt`, `SyncManager.kt`, `SyncWorker.kt`, `OfflineSyncScheduler.kt`, `SyncFailurePolicy.kt`, `SyncErrorClassifier.kt`, `SyncErrorResponseParser.kt`, `SuspendCatching.kt`)
- [SELESAI] Idempotency key pembayaran piutang & pelunasan kasir: satu key per niat bayar di `ReceivableCustomerDetailViewModel.kt` (menghapus string dummy `"idempotency-check"`), serta pelestarian key saat dialog ditutup-buka sebelum jawaban pasti dan rotasi key saat nominal/metode berubah pada `CashierTransactionHistoryViewModel.kt`
- [SELESAI] Standardisasi input uang IDR via `IdrMoneyInputParser.kt`: rupiah bulat pada harga jual/eceran/kontraktor/tier/diskon (`AdminPriceManagementScreen.kt`, `PriceManagementInputParser.kt`), limit kredit pelanggan (`CustomerViewModels.kt`, `AdminCustomerScreens.kt`), ongkos kirim (`CheckoutViewModel.kt`), toleransi kas (`SettingsViewModel.kt`), harga jual produk (`ProductInputValidation.kt`, `ProductFormViewModel.kt`, `ProductFormHelpers.kt`, `ProductFormSections.kt`), serta `parseIdrDecimalInput`/`sanitizeIdrDecimalInput`/`toIdrDecimalInputText` untuk harga beli (`IncomingGoodsState.kt`, `IncomingGoodsViewModel.kt`, form produk, katalog harga), dan penghapusan duplikasi `numericInput()`/`decimalInput()`/`sanitizeMoneyInput()` yang bertentangan bila tidak terpakai
- [SELESAI] Penyelarasan `LocalBackupRepository.kt` menggunakan `TbTerminalDatabase.DATABASE_VERSION` (`6`) serta penolakan versi backup `<= 0` atau lebih baru dari versi database aplikasi

## Temuan di Luar Scope (Fase B2a)

- [TINGGI] Pembayaran utang supplier (`SupplierDebtViewModel.kt`, `SupplierDebtDetailViewModel.kt`) dan restok barang masuk (`IncomingGoodsViewModel.kt`) belum mengirimkan/mengelola idempotency key — membutuhkan dukungan idempotency di endpoint backend terlebih dahulu (Fase B2/backend) — **[SELESAI DI FASE B2b]**
- [SEDANG] Audit & pembersihan `runCatching` / `catch Throwable` di luar jalur sync dan pembayaran agar tidak menelan `CancellationException` pada ViewModel UI maupun background job lain (Fase B2/B3)
- [SEDANG] Kebijakan retensi / TTL antrean sync offline (`> 72 jam`) dan alur pembersihan/eskalasi permanen jika terminal offline sangat lama atau gagal non-transien berulang kali (Fase B2/B3)

## Selesai di Fase B2b

- [SELESAI] Sinkronisasi `docs/API_CHANGES.md` terhadap kontrak backend `main @ c503509`
- [SELESAI] Migrasi seluruh kolom uang & kuantitas Room dari `REAL`/`Double` ke integer eksak berskala `×100` (`ScaledDecimal2` dengan `ofExact` yang menolak `> 2` desimal; `conversionFactor`, `secondaryUnitFactor`, dan `baseQuantity` tetap `TEXT`), penghapusan `toSafeDouble()` & aritmetika `Double` di jalur offline, perhitungan pajak/diskon/pembulatan `BigDecimal` paritas backend, migrasi eksplisit Room v6 → v7 (`HALF_UP` ke 2 desimal, transaksi, `PRAGMA foreign_key_check`, indeks lengkap, `exportSchema = true` + `app/schemas`), test migrasi `4→7`, `5→7`, `6→7` termasuk antrean sync `PENDING` end-to-end, serta `LocalBackupRepository` v7 yang dapat me-restore backup v6 (`ScaledDecimal2.kt`, `RoomMigration6To7.kt`, `TbTerminalDatabase.kt`, `LocalBackupRepository.kt`, `OfflineExactMoneyAndParityTest.kt`)
- [SELESAI] Idempotency key pembelian (`POST /api/purchasing/purchases`) & pembayaran utang supplier (`POST /api/purchasing/payables/payments`) di `SupplierDebtViewModel.kt`, `SupplierDebtDetailViewModel.kt`, dan `IncomingGoodsViewModel.kt` via `SubmissionIdempotencyTracker.kt`, termasuk pelestarian key saat retry/timeout/dialog ditutup-buka, rotasi saat niat berubah/sukses, dan perlakuan `idempotentReplay = true` sebagai sukses normal
- [SELESAI] Migrasi field angka DTO dashboard & ringkasan penjualan harian di `NetworkModels.kt` (`StockLowSummaryDto`, `DashboardMetricsDto`, `DailySalesSummaryDto`) dari `Double` ke `BigDecimal` dengan `BigDecimalStringSerializer` tanpa mengubah UI

## Temuan di Luar Scope (Fase B2b)

- [SEDANG] Migrasi state tampilan layar Owner & Checkout (`OwnerDashboardScreen.kt` `DashboardUiState`/`DayPoint`, `OwnerKeuanganScreen.kt`, dan `CheckoutUiState.equivalentBaseQuantity`) dari `Double` ke `BigDecimal` — app/src/main/java/com/tbterminal/app/ui/dashboard/owner/OwnerDashboardScreen.kt, app/src/main/java/com/tbterminal/app/ui/dashboard/owner/OwnerKeuanganScreen.kt, app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutUiState.kt — Saat ini konversi `.toDouble()` dipertahankan murni untuk tampilan UI/grafik saja (tidak dipakai menghitung, menyimpan, atau dikirim ke server) agar tidak menyentuh komponen di tengah redesain UI — Pasca redesain UI
- [SEDANG] Persistensi idempotency key lintas process death (`SavedStateHandle` atau draft Room) — app/src/main/java/com/tbterminal/app/ui/payables/SupplierDebtViewModel.kt, app/src/main/java/com/tbterminal/app/ui/payables/SupplierDebtDetailViewModel.kt, app/src/main/java/com/tbterminal/app/ui/incominggoods/IncomingGoodsViewModel.kt — Key idempotency saat ini disimpan di memori selama lifecycle ViewModel agar konsisten dengan pola B2a — Fase B3 — **[SELESAI DI FASE B2c-min]**
- [RENDAH] Audit format tampilan kuantitas stok rendah pada `LowStockWarningCard.kt` (`item.quantity.toString()` / `item.minStock.toString()`) agar disatukan dengan helper `stripTrailingZeros()` jika diperlukan tanpa mengubah tata letak kartu — Pasca redesain UI

## Selesai di Fase B2c-min

- [SELESAI] Skema Room v7 → v8 dengan tabel `pending_submissions` (PK `(ownerUserId, scope)`, TTL lokal 72 jam) dan `checkout_drafts` (PK `ownerUserId`), migrasi eksplisit `MIGRATION_7_8` beserta ekspor `8.json` dan unit test migrasi `6→8` serta `7→8` (`TbTerminalDatabase.kt`, `PendingSubmissionEntity.kt`, `CheckoutDraftEntity.kt`, `PendingSubmissionDao.kt`, `CheckoutDraftDao.kt`, `TbTerminalDatabaseMigrationTest.kt`)
- [SELESAI] Persistensi key idempotensi checkout ambigu & pembayaran lintas process death maupun restart aplikasi: penulisan `IN_FLIGHT` sebelum request dan pembaruan ke `AMBIGUOUS` pada error transien/timeout, pemulihan key yang sama untuk retry tanpa mengubah UI, serta pembersihan saat sukses/non-ambigu/berubah/kedaluwarsa 72 jam/data rusak pada `CheckoutViewModel`, `SupplierDebtViewModel`, `SupplierDebtDetailViewModel`, `IncomingGoodsViewModel`, `ReceivableCustomerDetailViewModel`, serta pelunasan kasir, void, dan refund di `CashierTransactionHistoryViewModel` (`PendingSubmissionStore.kt`, `CheckoutPendingSubmissionPersistence.kt`, `SubmissionIdempotencyTracker.kt`)
- [SELESAI] Persistensi keranjang aktif (item, pelanggan terpilih, diskon item & transaksi, info pengiriman) dan held carts di Room (`checkout_drafts`) secara ringkas dengan snapshot cadangan saat produk/pelanggan tidak tersedia di cache/server, penyimpanan debounce (400ms), serta pembersihan draft keranjang aktif setelah checkout online maupun offline lokal sukses (`CheckoutDraftStore.kt`, `CheckoutCartDraftCodec.kt`, `CheckoutCartDraftRestorer.kt`, `CheckoutCartDraftCoordinator.kt`, `CheckoutViewModel.kt`)
- [SELESAI] Injeksi `SavedStateHandle` via `CreationExtras` tidak diperlukan karena seluruh state ketahanan B2c-min sudah tersimpan persisten di Room

## Temuan di Luar Scope (Fase B2c-min)

Temuan berikut diidentifikasi saat pengerjaan Fase B2c-min namun tidak diubah pada fase ini agar ruang lingkup tetap terisolasi:

- [SEDANG] Idempotency key untuk aksi `void` dan `refund` di `CashierTransactionHistoryViewModel` (`voidIdempotencyKey`, `refundIdempotencyKey`) masih disimpan di memori dan belum dipersistenkan ke `pending_submissions` — app/src/main/java/com/tbterminal/app/ui/cashier/transactions/CashierTransactionHistoryViewModel.kt — Jika aplikasi ditutup/mati saat void/refund masih ambigu, retry setelah dibuka ulang akan membuat key baru — Fase B3 — **[SELESAI DI FASE B2c-min]**
- [RENDAH] `LocalBackupRepository` mencadangkan/memulihkan seluruh berkas SQLite sehingga baris sementara `pending_submissions` dan `checkout_drafts` ikut terbawa dalam backup lokal — app/src/main/java/com/tbterminal/app/data/repository/LocalBackupRepository.kt — Aman berkat TTL 72 jam dan replay idempoten server, namun pertimbangkan pembersihan atau pengecualian tabel draft/pending saat restore lintas perangkat — Fase B3
- [RENDAH] Ekstraksi logika B2c-min sudah dipisahkan ke kelas helper teruji, namun `CheckoutViewModel` masih berukuran > 2.900 baris — app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutViewModel.kt — Dekomposisi penuh tetap ditunda mengikuti item backlog B1/redesain UI

## Selesai di Fase B2e1 (Isolasi Sesi Kas, Urutan Sync Kronologis & Multi-User)

- [SELESAI] Proteksi baris `local_cash_sessions` yang memiliki perubahan lokal belum tersinkron agar tidak tertimpa mirror server (`hasUnsyncedLocalChanges()` di `LocalCashSessionMappers.kt`, `CashSessionLocalDataSource.kt`), serta rekonsiliasi mirror `OPEN/SYNCED` menjadi `CLOSED/SYNCED` ketika `closeSession()` online sukses atau `getActiveSession()` mengembalikan `null` (`CashSessionDao.markMirroredOpenSessionsClosed`, `CashReconciliationViewModel`, `CheckoutViewModel`).
- [SELESAI] Refresh status sesi kas saat kembali aktif (`ON_RESUME`) di POS/Cart/Payment (`CheckoutViewModel.refreshCashSessionOnResume`, `CheckoutRoute.kt`) dan penolakan transaksi lokal/pengeluaran kas ke sesi lokal yang bukan `OPEN` atau sesi offline (`serverId == null`) berumur > 72 jam (`LocalCheckoutService`, `LocalCashExpenseService`, `OfflineSessionAgePolicy`).
- [SELESAI] Urutan sinkronisasi kronologis per sesi per kasir (`buka S1 -> transaksi & pengeluaran S1 -> tutup S1 -> baru buka S2`) di `SyncManager` dan `SyncMonitoringRepository.orderedForSync`, plus auto-recovery sesi baru yang tertahan karena sesi lama belum tutup di server tanpa terkunci ke `FAILED`/`CONFLICT` (`CashSessionActiveBlockingMatcher`, `OfflineCashSessionSyncService.requeueBlockedOpenSessionsForCashier`).
- [SELESAI] Isolasi multi-user pada satu perangkat bersama: hapus `allowAnyCashier = true` di `CashReconciliationViewModel`, batasi `validateSingleOpenSession` per kasir, filter eksekusi sync & visibilitas queue/indikator ke user aktif tanpa mengubah status data user lain, flush draft keranjang & pending submission sebelum sesi dihapus pada logout dan reset `PosReorderBridge` (`SessionManager`, `CheckoutDraftStore`, `PendingSubmissionStore`, `CheckoutCartDraftCoordinator`, `CheckoutPendingSubmissionPersistence`), serta tampilan ringkas jumlah antrean kasir lain pada teks indikator sync yang sudah ada (Opsi 2).

## Spesifikasi Backend Go (Wajib untuk Migrasi Backend)

- **[WAJIB / KONTRAK API]** Endpoint `POST /api/sales/sessions/sync/open` pada backend Go baru **wajib** mengembalikan **kode error terstruktur khusus** (mis. `code: "ACTIVE_CASH_SESSION_EXISTS"`; lihat daftar kode pada `CashSessionActiveBlockingMatcher.BACKEND_ERROR_CODES` di Android) bersama status HTTP 4xx (disarankan 409 atau 400) ketika pembukaan sesi kasir baru terblokir oleh sesi kasir lain yang masih `OPEN` di server untuk akun tersebut. Android saat ini memiliki fallback pencocokan teks pesan berbahasa Indonesia di satu konstanta terpusat (`CashSessionActiveBlockingMatcher`), namun kontrak backend final tidak boleh bergantung pada redaksi kalimat error.

## Temuan di Luar Scope (Fase B2e1)

- [RENDAH] Penghapusan/pemangkasan retensi baris `sync_queue` berstatus `SYNCED` dan arsip transaksi lama (`SYNC_RETENTION`) tetap ditangani terpisah sesuai backlog reliabilitas disk/performa.
- [RENDAH] Pembersihan atau pengecualian tabel `pending_submissions` dan `checkout_drafts` saat restore backup lokal lintas perangkat tetap mengikuti item backlog B2c-min.

