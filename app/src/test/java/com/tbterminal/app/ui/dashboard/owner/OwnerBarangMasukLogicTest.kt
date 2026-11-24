package com.tbterminal.app.ui.dashboard.owner

import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.incominggoods.IncomingPaymentMethod
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerBarangMasukLogicTest {

    private fun product(stock: String, cogs: String) = ProductStock(
        productId = "p-1",
        sku = "SEM-655815",
        productName = "Semen Merdeka",
        categoryName = "Semen",
        unitName = "Pieces",
        quantity = BigDecimal(stock),
        minStock = BigDecimal("10"),
        priceBuy = BigDecimal(cogs),
        priceRetail = BigDecimal("48000"),
        priceContractor = BigDecimal("45000"),
        discount = BigDecimal.ZERO,
        isActive = true
    )

    private fun supplier(termDays: Int) = Supplier(
        id = "sup-1",
        name = "Supri",
        phone = "08123",
        address = "Jl. Raya",
        paymentTermDays = termDays,
        isActive = true,
        createdAt = "",
        updatedAt = ""
    )

    @Test
    fun zeroOldStockSetsNewCogsEqualToIncomingPrice() {
        val calc = calculateOwnerBarangMasuk(
            selectedProduct = product(stock = "0", cogs = "38000"),
            quantityDigits = "200",
            unitPriceDigits = "41000",
            amountPaidDigits = "8200000"
        )

        assertTrue(calc.hasImpactPreview)
        assertEquals(BigDecimal("0"), calc.oldStock)
        assertEquals(BigDecimal("200"), calc.newStock)
        assertEquals(BigDecimal("41000"), calc.newCogs)
        assertEquals(BigDecimal("8200000"), calc.total)
        assertEquals(BigDecimal.ZERO, calc.remainingDebt)
        assertFalse(calc.amountPaidExceedsTotal)
    }

    @Test
    fun positiveOldStockCalculatesWeightedMovingAverageRoundedToRupiah() {
        // (100 * 30.000 + 200 * 39.000) / 300 = 10.800.000 / 300 = 36.000
        val calc = calculateOwnerBarangMasuk(
            selectedProduct = product(stock = "100", cogs = "30000"),
            quantityDigits = "200",
            unitPriceDigits = "39000",
            amountPaidDigits = "5000000"
        )

        assertEquals(BigDecimal("300"), calc.newStock)
        assertEquals(BigDecimal("36000"), calc.newCogs)
        assertEquals(BigDecimal("7800000"), calc.total)
        assertEquals(BigDecimal("2800000"), calc.remainingDebt)
    }

    @Test
    fun amountPaidZeroEqualsTotalDebtPaidEqualsTotalAndExceedingTotal() {
        val p = product("0", "38000")
        val sup = supplier(30)
        val due = LocalDate.of(2026, 12, 23)

        val zeroPaid = calculateOwnerBarangMasuk(p, "200", "38000", "0")
        assertEquals(BigDecimal("7600000"), zeroPaid.remainingDebt)
        assertFalse(zeroPaid.amountPaidExceedsTotal)
        assertTrue(isOwnerBarangMasukFormValid(p, sup, zeroPaid, due))

        val fullPaid = calculateOwnerBarangMasuk(p, "200", "38000", "7600000")
        assertEquals(BigDecimal.ZERO, fullPaid.remainingDebt)
        assertFalse(fullPaid.amountPaidExceedsTotal)
        assertTrue(isOwnerBarangMasukFormValid(p, sup, fullPaid, dueDate = null))

        val overPaid = calculateOwnerBarangMasuk(p, "200", "38000", "8000000")
        assertTrue(overPaid.amountPaidExceedsTotal)
        assertEquals(BigDecimal.ZERO, overPaid.remainingDebt)
        assertFalse(isOwnerBarangMasukFormValid(p, sup, overPaid, due))
    }

    @Test
    fun zeroTermRequiresManualDueDateWhenDebtRemainsButNotWhenPaidInFull() {
        val p = product("10", "38000")
        val sup = supplier(termDays = 0)
        val today = LocalDate.of(2026, 11, 23)

        assertNull(initialDueDateForSupplier(sup.paymentTermDays, today))
        assertEquals(
            LocalDate.of(2026, 12, 23),
            initialDueDateForSupplier(30, today)
        )

        val partial = calculateOwnerBarangMasuk(p, "200", "38000", "5000000")
        assertFalse(isOwnerBarangMasukFormValid(p, sup, partial, dueDate = null))
        assertTrue(isOwnerBarangMasukFormValid(p, sup, partial, dueDate = today.plusDays(14)))
        assertEquals(14, resolveDueDaysForSubmit(partial.remainingDebt, today.plusDays(14), today))

        val paidFull = calculateOwnerBarangMasuk(p, "200", "38000", "7600000")
        assertTrue(isOwnerBarangMasukFormValid(p, sup, paidFull, dueDate = null))
        assertEquals(0, resolveDueDaysForSubmit(paidFull.remainingDebt, today.plusDays(14), today))
    }

    @Test
    fun paymentMethodInitialAmountAndWholeRupiahFormatters() {
        val total = BigDecimal("7600000")
        assertEquals("7600000", initialAmountPaidDigitsForMethod(IncomingPaymentMethod.CASH, total))
        assertEquals("7600000", initialAmountPaidDigitsForMethod(IncomingPaymentMethod.TRANSFER, total))
        assertEquals("7600000", initialAmountPaidDigitsForMethod(IncomingPaymentMethod.QRIS, total))
        assertEquals("0", initialAmountPaidDigitsForMethod(IncomingPaymentMethod.DEBT, total))

        assertEquals("Rp 38.000", formatRupiahWhole(BigDecimal("38000.00")))
        assertEquals("Rp 7.600.000", formatRupiahWhole(total))
        assertEquals("23 Des 2026", formatIndonesianDateShort(LocalDate.of(2026, 12, 23)))
    }
}
