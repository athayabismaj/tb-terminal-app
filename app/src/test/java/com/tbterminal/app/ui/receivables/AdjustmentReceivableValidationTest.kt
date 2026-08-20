package com.tbterminal.app.ui.receivables

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdjustmentReceivableValidationTest {
    private val today = LocalDate.of(2026, 8, 20)
    private val activeCustomers = setOf("active-1")

    @Test
    fun validAdjustmentIsNormalized() {
        val value = validateAdjustmentReceivableInput(
            "active-1", activeCustomers, "1500.25", "2026-08-19", "2026-09-19",
            " NOTA-42 ", " Koreksi migrasi ", today
        ).getOrThrow()
        assertEquals(BigDecimal("1500.25"), value.amount)
        assertEquals("NOTA-42", value.reference)
        assertEquals("Koreksi migrasi", value.reason)
    }

    @Test
    fun inactiveCustomerAndInvalidAmountsAreRejected() {
        assertTrue(validate("inactive", "10", "REF", "Alasan").isFailure)
        listOf("0", "-1", "1.001", "abc").forEach { assertTrue(validate("active-1", it, "REF", "Alasan").isFailure) }
    }

    @Test
    fun datesReferenceAndReasonAreRequired() {
        assertTrue(validate("active-1", "10", "", "Alasan").isFailure)
        assertTrue(validate("active-1", "10", "REF", "").isFailure)
        assertTrue(validate("active-1", "10", "REF", "Alasan", debt = "2026-08-21").isFailure)
        assertTrue(validate("active-1", "10", "REF", "Alasan", due = "2026-08-18").isFailure)
    }

    @Test
    fun adjustmentRolePolicyRejectsCashier() {
        assertTrue(canManageReceivableAdjustment("owner"))
        assertTrue(canManageReceivableAdjustment("ADMIN"))
        assertTrue(!canManageReceivableAdjustment("kasir"))
    }

    private fun validate(
        customer: String,
        amount: String,
        reference: String,
        reason: String,
        debt: String = "2026-08-19",
        due: String = "2026-08-20"
    ) = validateAdjustmentReceivableInput(customer, activeCustomers, amount, debt, due, reference, reason, today)
}
