package com.tbterminal.app.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CashSessionActiveBlockingMatcherTest {
    @Test
    fun matchesExplicitBackendErrorCodesOn4xx() {
        CashSessionActiveBlockingMatcher.BACKEND_ERROR_CODES.forEach { code ->
            assertTrue(
                CashSessionActiveBlockingMatcher.isActiveSessionBlockingOpen(
                    httpCode = 400,
                    errorCode = code,
                    errorMessage = "validation error",
                )
            )
            assertTrue(
                CashSessionActiveBlockingMatcher.isActiveSessionBlockingOpen(
                    httpCode = 409,
                    errorCode = code.lowercase(),
                    errorMessage = null,
                )
            )
        }
    }

    @Test
    fun matchesCurrentBackendIndonesianValidationMessageFallbackOn400And409() {
        assertTrue(
            CashSessionActiveBlockingMatcher.isActiveSessionBlockingOpen(
                httpCode = 400,
                errorCode = "VALIDATION_ERROR",
                errorMessage = "Anda masih memiliki sesi kasir yang belum ditutup (ID: sess-99). Tutup sesi tersebut sebelum sinkronisasi sesi lokal.",
            )
        )
        assertTrue(
            CashSessionActiveBlockingMatcher.isActiveSessionBlockingOpen(
                httpCode = 409,
                errorCode = null,
                errorMessage = "Masih ada sesi kas yang belum ditutup untuk akun ini",
            )
        )
    }

    @Test
    fun ignores5xxOrUnrelatedValidationErrors() {
        assertFalse(
            CashSessionActiveBlockingMatcher.isActiveSessionBlockingOpen(
                httpCode = 500,
                errorCode = "ACTIVE_CASH_SESSION_EXISTS",
                errorMessage = "Anda masih memiliki sesi kasir yang belum ditutup",
            )
        )
        assertFalse(
            CashSessionActiveBlockingMatcher.isActiveSessionBlockingOpen(
                httpCode = 400,
                errorCode = "VALIDATION_ERROR",
                errorMessage = "Nominal kas awal tidak boleh negatif",
            )
        )
    }
}
