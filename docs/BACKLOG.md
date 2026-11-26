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
