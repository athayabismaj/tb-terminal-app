# UI Design Rules — TB Terminal

Dokumen ini menjadi acuan untuk screen baru dan refactor UI. Perubahan presentasi tidak boleh mengubah API, validasi, autentikasi, role guard, atau aturan bisnis tanpa permintaan khusus.

## Prinsip utama

1. Susun layar berdasarkan pekerjaan pengguna, bukan nama modul backend.
2. Gunakan Material 3 dan komponen standar Android bila sudah tersedia.
3. Tetapkan satu primary action yang paling jelas pada setiap layar.
4. Gunakan typography, spacing, surface, dan tonal elevation untuk hierarchy; hindari outline tebal berulang.
5. Gunakan spacing berbasis 4/8dp, terutama 8, 12, 16, 24, dan 32dp.
6. Semua interaksi utama harus memiliki touch target minimal 48dp.
7. Tampilkan loading, disabled, success, error, offline, dan empty state dengan jelas.
8. Hindari gradient, shadow besar, glassmorphism, serta warna dekoratif yang tidak membantu tugas.

## Adaptive layout

- Phone menggunakan single column, safe spacing, dan komponen dengan lebar nyaman.
- Tablet menggunakan split, master-detail, NavigationRail, atau drawer sesuai konteks.
- Form, dialog, serta detail menggunakan max-width dan tidak di-stretch memenuhi tablet.
- Gunakan satu implementasi Compose adaptif; jangan menduplikasi screen phone dan tablet.

## Bahasa UI

Gunakan istilah sederhana seperti `Piutang`, `Hutang Supplier`, `Sesuaikan Stok`, `Cocokkan Kas`, dan `Riwayat Aktivitas`. Hindari istilah teknis backend pada label yang dilihat pengguna.

## Pola reusable

Jika pola dipakai berulang, ekstrak komponen atau token untuk page title, section title, primary/tonal button, form field, info card, status badge, dan empty state. Konsistensi design language lebih penting daripada variasi visual antar-screen.
