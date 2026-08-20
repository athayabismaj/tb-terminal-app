# Build dan Instalasi Android Production

## Prasyarat signing

Gunakan keystore milik klien yang disimpan di password manager/secure build storage, bukan repository. Siapkan environment berikut pada mesin release:

```powershell
$env:PROD_BASE_URL = "https://pos.example.com/"
$env:RELEASE_STORE_FILE = "D:\secure\tb-terminal-release.jks"
$env:RELEASE_STORE_PASSWORD = "<dari-secret-manager>"
$env:RELEASE_KEY_ALIAS = "<alias-klien>"
$env:RELEASE_KEY_PASSWORD = "<dari-secret-manager>"
.\gradlew.bat clean :app:testDebugUnitTest :app:assembleRelease
```

Build release akan gagal cepat bila URL bukan HTTPS, masih localhost/emulator, nilai signing kosong, atau keystore tidak ditemukan. Release memakai R8/resource shrinking, `debuggable=false`, backup aplikasi nonaktif, dan token sesi disimpan dengan Android Keystore AES/GCM.

APK berada di `app/build/outputs/apk/release/`. Verifikasi sebelum distribusi:

```powershell
apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

Catat SHA-256 APK dan fingerprint sertifikat pada berita acara. Distribusikan lewat kanal privat/MDM. Pada perangkat, aktifkan tanggal otomatis, pasang APK, login dengan akun non-default, uji `/ready`, cetak, logout, lalu pastikan data sesi tidak muncul setelah uninstall/clear data.

Keystore dan password wajib diserahkan kepada pemilik aplikasi melalui kanal terpisah. Kehilangan signing key menghalangi update APK dengan package yang sama.
