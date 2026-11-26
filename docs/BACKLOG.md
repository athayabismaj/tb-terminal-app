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

- [TINGGI] Pembayaran utang supplier (`SupplierDebtViewModel.kt`, `SupplierDebtDetailViewModel.kt`) dan restok barang masuk (`IncomingGoodsViewModel.kt`) belum mengirimkan/mengelola idempotency key — membutuhkan dukungan idempotency di endpoint backend terlebih dahulu (Fase B2/backend)
- [SEDANG] Audit & pembersihan `runCatching` / `catch Throwable` di luar jalur sync dan pembayaran agar tidak menelan `CancellationException` pada ViewModel UI maupun background job lain (Fase B2/B3)
- [SEDANG] Kebijakan retensi / TTL antrean sync offline (`> 72 jam`) dan alur pembersihan/eskalasi permanen jika terminal offline sangat lama atau gagal non-transien berulang kali (Fase B2/B3)
