package com.tbterminal.app.ui.receivables

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceivablePaymentValidationTest {
    @Test
    fun `partial payment is accepted`() {
        val result = validateReceivablePaymentInput("40.25", BigDecimal("100.00"), "payment-key-001")
        assertEquals(BigDecimal("40.25"), result.getOrThrow())
    }

    @Test
    fun `invalid amount and overpayment are rejected`() {
        listOf("0", "-1", "10.001", "100.01").forEach { input ->
            assertTrue(
                input,
                validateReceivablePaymentInput(input, BigDecimal("100.00"), "payment-key-001").isFailure
            )
        }
    }

    @Test
    fun `retry key is required`() {
        assertTrue(validateReceivablePaymentInput("10", BigDecimal("100"), "short").isFailure)
    }
}
