package com.tbterminal.app.ui.receivables

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningReceivableValidationTest {
    private val today = LocalDate.of(2026, 8, 1)

    @Test
    fun validOpeningBalanceIsParsed() {
        val result = validateOpeningReceivableInput(
            customerId = "customer-1",
            amountInput = "1250.50",
            debtDateInput = "2026-07-01",
            dueDateInput = "2026-08-15",
            today = today
        ).getOrThrow()

        assertEquals(BigDecimal("1250.50"), result.amount)
        assertEquals(LocalDate.of(2026, 7, 1), result.debtDate)
    }

    @Test
    fun invalidAmountAndMissingCustomerAreRejected() {
        assertTrue(validateOpeningReceivableInput("", "10", "2026-08-01", "2026-08-02", today).isFailure)
        listOf("0", "-1", "1.001").forEach { amount ->
            assertTrue(
                validateOpeningReceivableInput("customer-1", amount, "2026-08-01", "2026-08-02", today).isFailure
            )
        }
    }

    @Test
    fun futureDebtAndInvalidDueDateAreRejected() {
        assertTrue(
            validateOpeningReceivableInput("customer-1", "10", "2026-08-02", "2026-08-03", today).isFailure
        )
        assertTrue(
            validateOpeningReceivableInput("customer-1", "10", "2026-08-01", "2026-07-31", today).isFailure
        )
    }
}
