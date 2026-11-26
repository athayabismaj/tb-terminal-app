package com.tbterminal.app.ui.incominggoods

import com.tbterminal.app.ui.common.sanitizeIdrDecimalInput
import com.tbterminal.app.ui.common.toIdrDecimalInputText
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingGoodsStateMoneyInputTest {
    @Test
    fun `buyPrice uses Indonesian thousands and comma decimal rules`() {
        assertEquals("1.250,50", sanitizeIdrDecimalInput("1.250,50"))
        assertEquals("1333,33", BigDecimal("1333.33").toIdrDecimalInputText())
        assertEquals("15000", BigDecimal("15000.00").toIdrDecimalInputText())

        val groupedWhole = IncomingGoodsUiState(quantityInput = "2", buyPriceInput = "15.000")
        assertEquals(0, BigDecimal("15000").compareTo(groupedWhole.buyPrice))
        assertEquals(0, BigDecimal("30000").compareTo(groupedWhole.total))

        val decimalComma = IncomingGoodsUiState(quantityInput = "3", buyPriceInput = "1.333,33")
        assertEquals(0, BigDecimal("1333.33").compareTo(decimalComma.buyPrice))
        assertEquals(0, BigDecimal("3999.99").compareTo(decimalComma.total))

        val rejectedDotDecimal = IncomingGoodsUiState(quantityInput = "3", buyPriceInput = "1333.33")
        assertNull(rejectedDotDecimal.buyPrice)
        assertNull(rejectedDotDecimal.total)
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
