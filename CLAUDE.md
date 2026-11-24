# CLAUDE.md — tb-terminal-app

Aplikasi Android POS toko bangunan, klien untuk backend `tb-terminal-service`.
Bahasa UI: Indonesia. Identifier kode: Inggris.

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · Retrofit + OkHttp · kotlinx.serialization ·
WorkManager · DI manual lewat `data/di/AppContainer.kt` · minSdk 28 · Roborazzi untuk screenshot test.

## Perintah

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug   # wajib lulus sebelum selesai
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. Prosedur release ada di `docs/RELEASE.md`.

## Struktur

```
app/src/main/java/com/tbterminal/app/
  data/remote/      Retrofit API + DTO, NetworkInterceptors (Auth, Unauthorized, RefreshTokenAuthenticator)
  data/local/       Room (entity, dao, mapper) untuk offline
  data/repository/  jembatan remote/local → UI
  data/sync/        offline queue & sync (checkout, sesi kas, pengeluaran), SyncErrorClassifier
  data/session/     SessionManager, TokenStore (terenkripsi)
  data/di/          AppContainer (DI manual)
  ui/<fitur>/       Route + Screen + ViewModel per fitur
  navigation/
docs/UI_DESIGN_RULES.md   acuan wajib untuk layar baru dan refactor UI
```

## Aturan wajib

### Backend adalah sumber kebenaran
- Harga, diskon, batas diskon kasir, kebutuhan approval, pajak, dan total final berasal dari backend
  (`/checkout/preview`, `/checkout`). Perhitungan di aplikasi hanya estimasi UX.
- Sync offline mengirim harga lokal, dan backend memvalidasinya (HTTP 409 berkode `OFFLINE_*`:
  `PRICE`/`ROUNDING`/`TAX_CONFLICT`, `OCCURRED_AT`/`OPENED_AT`/`CLOSED_AT_INVALID`). Pesannya selalu memuat
  frasa "business rule", sehingga `SyncErrorClassifier` menandainya CONFLICT. Jangan "memperbaiki" angka
  di klien agar lolos validasi.
- Sesi offline yang tersambung lebih lama dari `OFFLINE_MAX_SESSION_AGE_HOURS` (default 72 jam) ditolak server.
- Hanya role KASIR yang boleh membuka sesi kas (online maupun sync). OWNER/ADMIN tidak.
- Kontrak API mengikuti backend. Lihat `tb-terminal-service/docs/SECURITY_HARDENING.md` untuk
  perubahan terbaru.

### Sesi & kredensial
- Refresh token SEKALI PAKAI. Hanya `RefreshTokenAuthenticator` (yang di-synchronize) yang boleh
  memanggil `/api/auth/refresh`. Worker/WorkManager tidak boleh me-refresh sendiri.
  Pemakaian ganda dianggap pencurian dan mencabut sesi di SEMUA perangkat user tersebut.
- HTTP 401 yang tidak bisa dipulihkan → bersihkan sesi lalu kembali ke login. Pengecualian:
  `/api/auth/unlock` dengan kode `INVALID_PIN`/`PIN_LOCKED`.
- PIN, password, dan PIN manager hanya hidup di memory. Jangan disimpan ke Room, DataStore,
  SharedPreferences, log, atau analytics. Bersihkan input setelah submit, error, atau batal.
- Pengecualian resmi untuk unlock offline: verifier PIN terenkripsi (`PBKDF2WithHmacSHA256` + salt acak 16 byte, dienkripsi AES-GCM lewat `AndroidKeyStore`, terikat ke `userId`, kedaluwarsa 72 jam sejak verifikasi online terakhir) serta state lockout lokal terenkripsi diizinkan khusus di `PinUnlockService`/`OfflinePinVerifierStore` untuk unlock offline (`PinScreen` dan kunci layar POS); PIN mentah tetap dilarang disimpan di mana pun.
- Setelah ganti password sukses, sesi di server sudah dicabut: arahkan user ke login.
- HTTP 429 (`RATE_LIMITED`, `TOO_MANY_ATTEMPTS`, `MANAGER_APPROVER_LOCKED`) dan 423 (`PIN_LOCKED`)
  → tampilkan pesan tunggu dan hormati header `Retry-After`.
- Validasi lokal harus sama dengan backend: password 8–72 karakter, PIN 4–6 digit angka.
- Jangan commit URL production, token, password, atau signing key.

### Offline & idempotensi
- Offline queue hanya untuk `TRANSACTION`, `CASH_SESSION`, dan `CASH_EXPENSE`. Data lain wajib online.
- Request yang bisa di-retry (checkout, pembayaran, void, refund, sync) memakai idempotency key yang
  dibuat sekali dan DIPAKAI ULANG saat retry. Jangan membuat key baru untuk percobaan ulang.
- Timeout pada checkout/pembayaran: cek histori dulu sebelum mengirim ulang.
- `deviceId` dari `DeviceIdProvider` (`android-<ANDROID_ID>`); jangan diganti formatnya.

### UI
- Ikuti `docs/UI_DESIGN_RULES.md`: Material 3, spacing 4/8dp, touch target ≥ 48dp, state loading/
  error/empty/offline yang jelas, satu implementasi adaptif phone/tablet.
- Refactor UI tidak boleh mengubah API, validasi, role guard, atau aturan bisnis.
- Pola layar: `<Fitur>Route` (ambil state dari ViewModel) → `<Fitur>Screen` (stateless) →
  `components/`. Composable stateless publik diberi `@Preview`.

## Cara bekerja

1. Baca file yang akan diubah, termasuk test terkait di `app/src/test`.
2. Beberapa file sudah sangat besar (> 2.000 baris), misalnya `CheckoutViewModel`,
   `CashierPaymentScreen`, `SharedSettingsScreen`, `AdminReceiptDetailScreen`. Jangan ditambah;
   ekstrak logika ke kelas yang bisa diuji dan pecah UI ke komponen.
3. Perubahan skema Room → naikkan versi database dan tulis migrasinya. Jangan
   `fallbackToDestructiveMigration`, karena data offline belum tersinkron bisa hilang.
4. Tambah unit test untuk setiap perubahan perilaku; jalankan perintah build di atas sebelum selesai
   dan laporkan hasilnya apa adanya.
