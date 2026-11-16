package com.tbterminal.app.ui.receivables

import com.tbterminal.app.data.model.Receivable
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `calculateFifoAllocations allocates funds sequentially by due date`() {
        val inv1 = createDummyReceivable(id = "1", remaining = BigDecimal("95000"), dueDate = "2026-08-01")
        val inv2 = createDummyReceivable(id = "2", remaining = BigDecimal("45000"), dueDate = "2026-08-10")

        val allocations = calculateFifoAllocations(
            invoices = listOf(inv2, inv1), // intentionally reverse order
            selectedIds = setOf("1", "2"),
            totalPaymentAmount = BigDecimal("100000")
        )

        assertEquals(2, allocations.size)
        // First sorted by dueDate -> inv1
        val first = allocations[0]
        assertEquals("1", first.receivable.id)
        assertTrue(first.isSelected)
        assertEquals(BigDecimal("95000"), first.allocatedAmount)
        assertEquals(BigDecimal.ZERO, first.remainingAfterPayment)
        assertTrue(first.isFullyPaid)

        // Second -> inv2
        val second = allocations[1]
        assertEquals("2", second.receivable.id)
        assertTrue(second.isSelected)
        assertEquals(BigDecimal("5000"), second.allocatedAmount)
        assertEquals(BigDecimal("40000"), second.remainingAfterPayment)
        assertTrue(second.isPartiallyPaid)
    }

    @Test
    fun `calculateFifoAllocations skips unselected invoices`() {
        val inv1 = createDummyReceivable(id = "1", remaining = BigDecimal("95000"), dueDate = "2026-08-01")
        val inv2 = createDummyReceivable(id = "2", remaining = BigDecimal("45000"), dueDate = "2026-08-10")

        val allocations = calculateFifoAllocations(
            invoices = listOf(inv1, inv2),
            selectedIds = setOf("2"), // only invoice 2 selected
            totalPaymentAmount = BigDecimal("45000")
        )

        val first = allocations[0]
        assertEquals("1", first.receivable.id)
        assertFalse(first.isSelected)
        assertEquals(BigDecimal.ZERO, first.allocatedAmount)
        assertEquals(BigDecimal("95000"), first.remainingAfterPayment)

        val second = allocations[1]
        assertEquals("2", second.receivable.id)
        assertTrue(second.isSelected)
        assertEquals(BigDecimal("45000"), second.allocatedAmount)
        assertEquals(BigDecimal.ZERO, second.remainingAfterPayment)
        assertTrue(second.isFullyPaid)
    }

    private fun createDummyReceivable(id: String, remaining: BigDecimal, dueDate: String): Receivable {
        return Receivable(
            id = id,
            customerId = "cust-1",
            customerName = "Jagad",
            transactionId = "trx-$id",
            source = "SALE",
            amount = remaining,
            paidAmount = BigDecimal.ZERO,
            remainingAmount = remaining,
            debtDate = dueDate,
            dueDate = dueDate,
            status = "UNPAID",
            legacyInvoiceNumber = "INV-00$id",
            notes = null,
            createdBy = "admin",
            isActive = true,
            createdAt = "2026-08-01T00:00:00Z"
        )
    }
}

