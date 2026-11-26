package com.tbterminal.app.ui.incominggoods

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingGoodsStateMoneyInputTest {
    @Test
    fun `buyPrice accepts decimal unit prices and preserves prefill and total`() {
        assertEquals("1250.50", "1250.50".decimalInput())
        assertEquals("1250.50", "1250,50".decimalInput())
        assertEquals("1250.5", BigDecimal("1250.50").toInputText())

        val state = IncomingGoodsUiState(
            quantityInput = "3",
            buyPriceInput = "1250,50".decimalInput()
        )
        assertEquals(0, BigDecimal("1250.50").compareTo(state.buyPrice))
        assertEquals(0, BigDecimal("3751.50").compareTo(state.total))
    }

    @Test
    fun `down payment amountPaid enforces whole rupiah while decimal buyPrice stays intact`() {
        val validGrouped = IncomingGoodsUiState(
            paymentMethod = IncomingPaymentMethod.DOWN_PAYMENT,
            amountPaidInput = "20.000"
        )
        assertEquals(0, BigDecimal("20000").compareTo(validGrouped.effectiveAmountPaid))

        val validTrailingZeros = IncomingGoodsUiState(
            paymentMethod = IncomingPaymentMethod.DOWN_PAYMENT,
            amountPaidInput = "20000,00"
        )
        assertEquals(0, BigDecimal("20000").compareTo(validTrailingZeros.effectiveAmountPaid))

        val rejectedCents = IncomingGoodsUiState(
            paymentMethod = IncomingPaymentMethod.DOWN_PAYMENT,
            amountPaidInput = "1.500,50"
        )
        assertNull(rejectedCents.effectiveAmountPaid)
    }
}
