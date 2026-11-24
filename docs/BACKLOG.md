# Backlog Temuan di Luar Scope (Fase B1)

Temuan berikut diidentifikasi saat penyesuaian Fase B1 terhadap security hardening backend, namun tidak diubah di fase ini agar ruang lingkup tetap terisolasi:

- [SEDANG] Layar profil kasir dan profil bersama belum menyambungkan seluruh callback ganti/reset PIN dan force-logout ke CredentialChangeDialog — app/src/main/java/com/tbterminal/app/ui/settings/CashierProfileScreen.kt, app/src/main/java/com/tbterminal/app/ui/settings/SharedProfileScreen.kt — Alur ganti/reset kredensial dan handling pencabutan sesi 401 dari layar profil belum seragam penuh dengan layar pengaturan akun utama — Fase B2
- [RENDAH] Refactor file berukuran besar (>2.000 baris) ke komponen UI dan kelas logika terpisah — app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutViewModel.kt, app/src/main/java/com/tbterminal/app/ui/checkout/CashierPaymentScreen.kt, app/src/main/java/com/tbterminal/app/ui/settings/SharedSettingsScreen.kt, app/src/main/java/com/tbterminal/app/ui/salestransactions/AdminReceiptDetailScreen.kt — Kompleksitas pemeliharaan tinggi dan risiko regresi saat menambah atau menguji fitur baru — Fase B2/B3

## Selesai di Fase B1b

- [SELESAI] Satuan sekunder di sync checkout offline — kini mengirim pasangan `unitId` + `baseQuantity` eksak dengan persistensi Room v5 → v6 dan klasifikasi 409 `OFFLINE_UNIT_CONFLICT` (`OfflineCheckoutSyncService.kt`, `TbTerminalDatabase.kt`, `SyncErrorClassifier.kt`)
- [SELESAI] Peringatan proaktif umur sesi kas offline — banner peringatan pada >= 48 jam dan peringatan keras pada >= 66 jam (batas default 72 jam) di `CashierCashSessionScreen.kt`, `CashReconciliationScreen.kt`, dan `SyncCenterScreen.kt`

## Temuan di Luar Scope (Fase B1b)

Temuan berikut diidentifikasi saat pengerjaan Fase B1b namun tidak diubah pada fase ini agar ruang lingkup tetap terisolasi:

- [TINGGI] Endpoint GET /api/inventory/stock (StockDetailResponse) belum mengirim baseUnitId, secondaryUnitId, secondaryUnitName, dan secondaryUnitFactor — app/src/main/java/com/tbterminal/app/data/remote/InventoryApi.kt, app/src/main/java/com/tbterminal/app/data/repository/InventoryRepository.kt — POS dan cache offline harus melakukan hidrasi tambahan ke GET /api/inventory/products dan GET /api/inventory/units yang berisiko gagal oleh RBAC role KASIR atau perbedaan paginasi/urutan halaman — Backend Fase 5b
- [SEDANG] POS membatasi satuan sekunder hanya untuk faktor > 1 sehingga produk dengan faktor konversi pecahan (< 1) tidak dapat dijual dalam satuan sekunder — app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutUiState.kt, app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutViewModel.kt — Produk dengan faktor konversi antara 0 dan 1 (valid di ProductInputValidation) tidak menampilkan opsi toggle satuan sekunder di layar kasir — Fase B2
- [SEDANG] Checkout online (previewCheckout dan submitCheckout) masih memotong effectiveQty dengan .toInt() — app/src/main/java/com/tbterminal/app/ui/checkout/CheckoutViewModel.kt — Kuantitas desimal atau hasil perkalian faktor pecahan pada transaksi online kehilangan bagian desimal sebelum dikirim ke server — Fase B2
- [RENDAH] Serialisasi payload lokal di LocalCheckoutService.toSyncPayloadJson membangun string JSON manual tanpa escaping backslash atau newline — app/src/main/java/com/tbterminal/app/data/local/checkout/LocalCheckoutService.kt — Catatan atau nama item yang mengandung karakter khusus berisiko menghasilkan JSON lokal tidak valid untuk diagnostik — Fase B2
