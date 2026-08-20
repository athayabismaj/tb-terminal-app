package com.tbterminal.app.ui.cashier.transactions

import java.math.BigDecimal
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class TransactionVoidValidationTest {
    @Test
    fun `void reason is mandatory and bounded`() {
        assertEquals("Alasan void minimal 5 karakter.", validateTransactionVoidReason("  x "))
        assertNull(validateTransactionVoidReason("Salah input barang"))
        assertEquals("Alasan void maksimal 1000 karakter.", validateTransactionVoidReason("x".repeat(1001)))
    }

    @Test
    fun `stock card reconciliation ignores decimal scale`() {
        assertTrue(stockCardBalancesReconciled(BigDecimal("10.00"), BigDecimal("10.0")))
        assertFalse(stockCardBalancesReconciled(BigDecimal("10.00"), BigDecimal("9.99")))
        assertFalse(stockCardBalancesReconciled(null, BigDecimal.ZERO))
    }
}
