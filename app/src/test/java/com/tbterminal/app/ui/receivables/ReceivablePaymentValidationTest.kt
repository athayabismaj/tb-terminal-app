package com.tbterminal.app.ui.receivables

import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.ui.common.toWholeRupiahInputText
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceivablePaymentValidationTest {
    @Test
    fun `partial payment is accepted`() {
        val grouped = validateReceivablePaymentInput("40.000", BigDecimal("100.00"), "payment-key-001")
        assertTrue(validateReceivablePaymentInput("40.000", BigDecimal("100000"), "payment-key-001").isSuccess)
        assertEquals(BigDecimal("40000"), validateReceivablePaymentInput("40000,00", BigDecimal("100000"), "payment-key-001").getOrThrow())
        assertTrue(grouped.isFailure)
    }

    @Test
    fun `invalid amount and overpayment are rejected`() {
        listOf("0", "-1", "10.000,50", "100.001").forEach { input ->
            assertTrue(
                input,
                validateReceivablePaymentInput(input, BigDecimal("100000"), "payment-key-001").isFailure
            )
        }
    }

    @Test
    fun `retry key is required`() {
        assertTrue(validateReceivablePaymentInput("10", BigDecimal("100"), "short").isFailure)
    }

    @Test
    fun `cent remaining balance allows exact lunas and whole partial while rejecting arbitrary cents`() {
        val remaining = BigDecimal("150000.50")
        val lunasText = remaining.toWholeRupiahInputText()
        assertEquals("150000,5", lunasText)

        val lunasResult = validateReceivablePaymentInput(lunasText, remaining, "payment-key-001")
        assertEquals(0, BigDecimal("150000.50").compareTo(lunasResult.getOrThrow()))

        val partialResult = validateReceivablePaymentInput("150000", remaining, "payment-key-001")
        val partialPaid = partialResult.getOrThrow()
        assertEquals(0, BigDecimal("150000").compareTo(partialPaid))

        val allocation = calculateFifoAllocations(
            invoices = listOf(createDummyReceivable(id = "cent-1", remaining = remaining, dueDate = "2026-08-01")),
            selectedIds = setOf("cent-1"),
            totalPaymentAmount = partialPaid
        ).single()
        assertEquals(0, BigDecimal("0.50").compareTo(allocation.remainingAfterPayment))
        assertFalse(allocation.isFullyPaid)

        assertTrue(validateReceivablePaymentInput("150000,25", remaining, "payment-key-001").isFailure)
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

