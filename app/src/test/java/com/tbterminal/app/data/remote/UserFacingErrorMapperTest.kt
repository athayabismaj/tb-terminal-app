package com.tbterminal.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class UserFacingErrorMapperTest {
    @Test
    fun mapsHttpErrorsToStableIndonesianMessages() {
        assertEquals("Input tidak valid. Periksa kembali data yang diisi.", UserFacingErrorMapper.message(httpStatus = 400))
        assertEquals("Sesi Anda telah habis. Silakan login kembali.", UserFacingErrorMapper.message(httpStatus = 401))
        assertEquals("Anda tidak memiliki akses ke fitur ini.", UserFacingErrorMapper.message(httpStatus = 403))
        assertEquals("Data yang diminta tidak ditemukan.", UserFacingErrorMapper.message(httpStatus = 404))
        assertEquals("Data berubah atau sedang diproses. Muat ulang lalu coba lagi.", UserFacingErrorMapper.message(httpStatus = 409))
        assertEquals("Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi.", UserFacingErrorMapper.message(httpStatus = 429))
        assertEquals("Server sedang mengalami gangguan. Silakan coba beberapa saat lagi.", UserFacingErrorMapper.message(httpStatus = 503))
    }

    @Test
    fun mapsTransportAndTimeoutSeparately() {
        assertEquals(
            "Koneksi bermasalah. Periksa jaringan dan status server.",
            UserFacingErrorMapper.message(code = "CONNECTION_INTERRUPTED"),
        )
        assertEquals(
            "Request terlalu lama. Periksa koneksi lalu coba lagi.",
            UserFacingErrorMapper.message(code = "NETWORK_TIMEOUT"),
        )
    }

    @Test
    fun maps429And423LockoutCodesWithRetryAfterDuration() {
        assertEquals(
            "Terlalu banyak percobaan gagal. Coba lagi dalam 45 detik.",
            UserFacingErrorMapper.message(httpStatus = 429, code = "TOO_MANY_ATTEMPTS", retryAfterSeconds = 45L),
        )
        assertEquals(
            "Terlalu banyak PIN manager salah. Persetujuan akun ini dikunci sementara. Coba lagi dalam 5 menit.",
            UserFacingErrorMapper.message(httpStatus = 429, code = "MANAGER_APPROVER_LOCKED", retryAfterSeconds = 300L),
        )
        assertEquals(
            "PIN terkunci sementara. Coba lagi dalam 1 menit 15 detik.",
            UserFacingErrorMapper.message(httpStatus = 423, code = "PIN_LOCKED", retryAfterSeconds = 75L),
        )
        assertEquals(
            "Terlalu banyak percobaan. Tunggu 2 menit lalu coba lagi.",
            UserFacingErrorMapper.message(httpStatus = 429, code = "RATE_LIMITED", retryAfterSeconds = 120L),
        )
    }
}
