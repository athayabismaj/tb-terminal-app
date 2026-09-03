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
}
