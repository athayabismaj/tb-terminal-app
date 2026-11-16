package com.tbterminal.app.ui.checkout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class CheckoutTaxRoundingAndSplitValidationTest {

    private fun assertBigDecimalEquals(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }

    @Test
    fun `estimateTaxCalculation when disabled keeps net total and zeroes tax fields`() {
        val result = estimateTaxCalculation(
            netGoodsTotal = BigDecimal("150000"),
            taxEnabled = false,
            taxRate = BigDecimal("11"),
            taxMode = "EXCLUDE",
            taxLabel = "PPN",
        )

        assertEquals(false, result.taxEnabled)
        assertNull(result.taxMode)
        assertBigDecimalEquals("0", result.taxableBase)
        assertBigDecimalEquals("0", result.taxAmount)
        assertBigDecimalEquals("150000.00", result.totalWithTax)
    }

    @Test
    fun `estimateTaxCalculation exclude mode adds tax on top of net goods`() {
        val result = estimateTaxCalculation(
            netGoodsTotal = BigDecimal("100000"),
            taxEnabled = true,
            taxRate = BigDecimal("11"),
            taxMode = "EXCLUDE",
            taxLabel = " ",
        )

        assertEquals(true, result.taxEnabled)
        assertEquals("EXCLUDE", result.taxMode)
        assertEquals("PPN", result.taxLabel)
        assertBigDecimalEquals("100000.00", result.taxableBase)
        assertBigDecimalEquals("11000.00", result.taxAmount)
        assertBigDecimalEquals("111000.00", result.totalWithTax)
    }

    @Test
    fun `estimateTaxCalculation include mode extracts dpp and tax preserving net total`() {
        val result = estimateTaxCalculation(
            netGoodsTotal = BigDecimal("111000"),
            taxEnabled = true,
            taxRate = BigDecimal("11"),
            taxMode = "INCLUDE",
            taxLabel = "PPN",
        )

        assertEquals("INCLUDE", result.taxMode)
        assertBigDecimalEquals("100000.00", result.taxableBase)
        assertBigDecimalEquals("11000.00", result.taxAmount)
        assertBigDecimalEquals("111000.00", result.totalWithTax)
        assertBigDecimalEquals(
            "111000.00",
            result.taxableBase.add(result.taxAmount)
        )
    }

    @Test
    fun `estimateCashRoundingCalculation skips rounding for non cash or disabled unit`() {
        val nonCash = estimateCashRoundingCalculation(
            totalBeforeRounding = BigDecimal("12340"),
            hasCashPayment = false,
            roundingUnit = 500,
            roundingRule = "NEAREST",
        )
        assertBigDecimalEquals("0.00", nonCash.roundingAmount)
        assertBigDecimalEquals("12340.00", nonCash.finalTotal)

        val unitZero = estimateCashRoundingCalculation(
            totalBeforeRounding = BigDecimal("12340"),
            hasCashPayment = true,
            roundingUnit = 0,
            roundingRule = "UP",
        )
        assertBigDecimalEquals("0.00", unitZero.roundingAmount)
        assertBigDecimalEquals("12340.00", unitZero.finalTotal)
    }

    @Test
    fun `estimateCashRoundingCalculation supports nearest down and up rules`() {
        val nearest = estimateCashRoundingCalculation(
            totalBeforeRounding = BigDecimal("12250"),
            hasCashPayment = true,
            roundingUnit = 500,
            roundingRule = "NEAREST",
        )
        assertBigDecimalEquals("250.00", nearest.roundingAmount)
        assertBigDecimalEquals("12500.00", nearest.finalTotal)

        val down = estimateCashRoundingCalculation(
            totalBeforeRounding = BigDecimal("12490"),
            hasCashPayment = true,
            roundingUnit = 500,
            roundingRule = "DOWN",
        )
        assertBigDecimalEquals("-490.00", down.roundingAmount)
        assertBigDecimalEquals("12000.00", down.finalTotal)

        val up = estimateCashRoundingCalculation(
            totalBeforeRounding = BigDecimal("12010"),
            hasCashPayment = true,
            roundingUnit = 1000,
            roundingRule = "UP",
        )
        assertBigDecimalEquals("990.00", up.roundingAmount)
        assertBigDecimalEquals("13000.00", up.finalTotal)
    }

    @Test
    fun `validateSplitCheckoutPayment computes change when cash plus qris exceeds total`() {
        val result = validateSplitCheckoutPayment(
            lines = listOf(
                SplitPaymentLineUi(method = PaymentMethod.QRIS, amountInput = "60000"),
                SplitPaymentLineUi(method = PaymentMethod.TUNAI, amountInput = "50000"),
            ),
            total = BigDecimal("100000"),
            isDpMode = false,
            customerSelected = false,
        )

        assertNull(result.error)
        assertBigDecimalEquals("110000", result.totalTendered)
        assertBigDecimalEquals("50000", result.cashTendered)
        assertBigDecimalEquals("60000", result.nonCashPaid)
        assertBigDecimalEquals("10000", result.changeAmount)
    }

    @Test
    fun `validateSplitCheckoutPayment validates dp split requires customer and calculates receivable`() {
        val missingCustomer = validateSplitCheckoutPayment(
            lines = listOf(
                SplitPaymentLineUi(method = PaymentMethod.TRANSFER, amountInput = "30000"),
                SplitPaymentLineUi(method = PaymentMethod.TUNAI, amountInput = "20000"),
            ),
            total = BigDecimal("100000"),
            isDpMode = true,
            customerSelected = false,
        )
        assertNotNull(missingCustomer.error)

        val validDp = validateSplitCheckoutPayment(
            lines = listOf(
                SplitPaymentLineUi(method = PaymentMethod.TRANSFER, amountInput = "30000"),
                SplitPaymentLineUi(method = PaymentMethod.TUNAI, amountInput = "20000"),
            ),
            total = BigDecimal("100000"),
            isDpMode = true,
            customerSelected = true,
        )
        assertNull(validDp.error)
        assertBigDecimalEquals("50000", validDp.totalTendered)
        assertBigDecimalEquals("50000", validDp.remainingReceivable)
    }

    @Test
    fun `validateSplitCheckoutPayment rejects duplicate cash redundant cash and overpaid non cash`() {
        val duplicateCash = validateSplitCheckoutPayment(
            lines = listOf(
                SplitPaymentLineUi(method = PaymentMethod.TUNAI, amountInput = "50000"),
                SplitPaymentLineUi(method = PaymentMethod.TUNAI, amountInput = "50000"),
            ),
            total = BigDecimal("100000"),
            isDpMode = false,
            customerSelected = false,
        )
        assertNotNull(duplicateCash.error)

        val overNonCash = validateSplitCheckoutPayment(
            lines = listOf(
                SplitPaymentLineUi(method = PaymentMethod.QRIS, amountInput = "120000"),
            ),
            total = BigDecimal("100000"),
            isDpMode = false,
            customerSelected = false,
        )
        assertNotNull(overNonCash.error)

        val redundantCash = validateSplitCheckoutPayment(
            lines = listOf(
                SplitPaymentLineUi(method = PaymentMethod.QRIS, amountInput = "100000"),
                SplitPaymentLineUi(method = PaymentMethod.TUNAI, amountInput = "10000"),
            ),
            total = BigDecimal("100000"),
            isDpMode = false,
            customerSelected = false,
        )
        assertNotNull(redundantCash.error)
    }
}
