package com.tbterminal.app.ui.payables

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningSupplierDebtValidationTest {
    private val today = LocalDate.of(2026, 8, 1)

    @Test
    fun validOpeningBalanceIsParsed() {
        val result = validateOpeningSupplierDebtInput(
            supplierId = "supplier-1",
            amountInput = "1250000.50",
            debtDateInput = "2026-07-01",
            dueDateInput = "2026-08-15",
            today = today
        ).getOrThrow()

        assertEquals(BigDecimal("1250000.50"), result.amount)
        assertEquals(LocalDate.of(2026, 7, 1), result.debtDate)
        assertEquals(LocalDate.of(2026, 8, 15), result.dueDate)
    }

    @Test
    fun invalidAmountAndMissingSupplierAreRejected() {
        assertTrue(validateOpeningSupplierDebtInput("", "100000", "2026-08-01", "2026-08-02", today).isFailure)
        listOf("0", "-1", "1.001", "abc").forEach { amount ->
            assertTrue(
                validateOpeningSupplierDebtInput("supplier-1", amount, "2026-08-01", "2026-08-02", today).isFailure
            )
        }
    }

    @Test
    fun futureDebtAndInvalidDueDateAreRejected() {
        assertTrue(
            validateOpeningSupplierDebtInput("supplier-1", "100000", "2026-08-02", "2026-08-03", today).isFailure
        )
        assertTrue(
            validateOpeningSupplierDebtInput("supplier-1", "100000", "2026-08-01", "2026-07-31", today).isFailure
        )
    }
}
