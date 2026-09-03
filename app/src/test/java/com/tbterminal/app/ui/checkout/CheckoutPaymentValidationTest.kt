package com.tbterminal.app.ui.checkout

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CheckoutPaymentValidationTest {
    private val total = BigDecimal("100.00")

    @Test
    fun cashRequiresFullPaymentAndCalculatesChange() {
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.TUNAI, "99.99", total).error)

        val valid = validateCheckoutPaymentInput(PaymentMethod.TUNAI, "125.00", total)
        assertNull(valid.error)
        assertEquals(BigDecimal("125.00"), valid.amountPaid)
        assertEquals(BigDecimal("25.00"), checkoutChange("125.00", total))
    }

    @Test
    fun paymentRejectsMoreThanTwoDecimals() {
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.TUNAI, "100.001", total).error)
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.DP, "10.001", total).error)
    }

    @Test
    fun dpMustBeStrictlyBetweenZeroAndTotal() {
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.DP, "0", total).error)
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.DP, "100", total).error)
        assertNull(validateCheckoutPaymentInput(PaymentMethod.DP, "25.00", total).error)
    }

    @Test
    fun electronicAndDebtAmountsAreDerivedSafely() {
        assertEquals(total, validateCheckoutPaymentInput(PaymentMethod.TRANSFER, "", total).amountPaid)
        assertEquals(total, validateCheckoutPaymentInput(PaymentMethod.QRIS, "invalid", total).amountPaid)
        assertEquals(BigDecimal.ZERO, validateCheckoutPaymentInput(PaymentMethod.HUTANG, "999", total).amountPaid)
    }

    @Test
    fun zeroTotalAllowsOnlyNonCreditPaymentWithZeroTendered() {
        val zero = BigDecimal("0.00")

        assertEquals(0, zero.compareTo(validateCheckoutPaymentInput(PaymentMethod.TUNAI, "0.00", zero).amountPaid!!))
        assertEquals(0, zero.compareTo(validateCheckoutPaymentInput(PaymentMethod.TRANSFER, "", zero).amountPaid!!))
        assertEquals(0, zero.compareTo(validateCheckoutPaymentInput(PaymentMethod.QRIS, "", zero).amountPaid!!))
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.TUNAI, "1", zero).error)
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.DP, "0", zero).error)
        assertNotNull(validateCheckoutPaymentInput(PaymentMethod.HUTANG, "0", zero).error)
    }

    @Test
    fun dpValidationUsesDiscountedNetTotal() {
        val netTotal = BigDecimal("900000.00")
        val dp = validateCheckoutPaymentInput(PaymentMethod.DP, "300000.00", netTotal)

        assertNull(dp.error)
        assertEquals(0, BigDecimal("600000.00").compareTo(netTotal.subtract(dp.amountPaid!!)))
    }
}
