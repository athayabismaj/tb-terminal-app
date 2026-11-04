package com.tbterminal.app.ui.payables

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdjustmentSupplierDebtValidationTest {
    private val today = LocalDate.of(2026, 8, 20)
    private val activeSuppliers = setOf("active-supplier-1")

    @Test
    fun validAdjustmentIsNormalized() {
        val value = validateAdjustmentSupplierDebtInput(
            "active-supplier-1", activeSuppliers, "1500000.25", "2026-08-19", "2026-09-19",
            " FAKTUR-ADJ-01 ", " Koreksi retur semen ", today
        ).getOrThrow()
        assertEquals(BigDecimal("1500000.25"), value.amount)
        assertEquals("FAKTUR-ADJ-01", value.reference)
        assertEquals("Koreksi retur semen", value.reason)
    }

    @Test
    fun inactiveSupplierAndInvalidAmountsAreRejected() {
        assertTrue(validate("inactive", "100000", "REF", "Alasan").isFailure)
        listOf("0", "-1", "1.001", "abc").forEach { assertTrue(validate("active-supplier-1", it, "REF", "Alasan").isFailure) }
    }

    @Test
    fun datesReferenceAndReasonAreRequired() {
        assertTrue(validate("active-supplier-1", "100000", "", "Alasan").isFailure)
        assertTrue(validate("active-supplier-1", "100000", "REF", "").isFailure)
        assertTrue(validate("active-supplier-1", "100000", "REF", "Alasan", debt = "2026-08-21").isFailure)
        assertTrue(validate("active-supplier-1", "100000", "REF", "Alasan", due = "2026-08-18").isFailure)
    }

    private fun validate(
        supplier: String,
        amount: String,
        reference: String,
        reason: String,
        debt: String = "2026-08-19",
        due: String = "2026-08-20"
    ) = validateAdjustmentSupplierDebtInput(supplier, activeSuppliers, amount, debt, due, reference, reason, today)
}
