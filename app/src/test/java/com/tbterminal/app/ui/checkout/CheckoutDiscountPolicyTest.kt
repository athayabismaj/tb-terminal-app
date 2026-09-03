package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.DiscountType
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutDiscountPolicyTest {
    @Test
    fun itemPercentageAndFixedDiscountUseLineTotal() {
        val percentage = estimateCheckoutTotals(
            listOf(item(price = "50000", quantity = 2, discount = percent("10"))),
            null,
        )
        val fixed = estimateCheckoutTotals(
            listOf(item(price = "20000", quantity = 3, discount = fixed("10000"))),
            null,
        )

        assertMoney("100000.00", percentage.grossSubtotal)
        assertMoney("10000.00", percentage.itemDiscountTotal)
        assertMoney("90000.00", percentage.netTotal)
        assertMoney("60000.00", fixed.grossSubtotal)
        assertMoney("10000.00", fixed.itemDiscountTotal)
        assertMoney("50000.00", fixed.netTotal)
    }

    @Test
    fun transactionAndCombinedDiscountFollowBackendOrder() {
        val result = estimateCheckoutTotals(
            listOf(item(price = "100000", quantity = 2, discount = percent("10"))),
            percent("10"),
        )

        assertMoney("200000.00", result.grossSubtotal)
        assertMoney("20000.00", result.itemDiscountTotal)
        assertMoney("18000.00", result.transactionDiscountAmount)
        assertMoney("38000.00", result.totalDiscountAmount)
        assertMoney("162000.00", result.netTotal)
    }

    @Test
    fun transactionFixedDiscountUsesSubtotalAfterItemDiscount() {
        val result = estimateCheckoutTotals(
            listOf(item(price = "100000", quantity = 1, discount = fixed("10000"))),
            fixed("15000"),
        )

        assertMoney("100000.00", result.grossSubtotal)
        assertMoney("10000.00", result.itemDiscountTotal)
        assertMoney("15000.00", result.transactionDiscountAmount)
        assertMoney("75000.00", result.netTotal)
    }

    @Test
    fun validationRejectsInvalidDiscounts() {
        val base = BigDecimal("100.00")
        assertNull(validateCheckoutDiscount(base, percent("100")))
        assertNull(validateCheckoutDiscount(base, fixed("100")))
        assertNotNull(validateCheckoutDiscount(base, percent("100.01")))
        assertNotNull(validateCheckoutDiscount(base, fixed("100.01")))
        assertNotNull(validateCheckoutDiscount(base, percent("-1")))
        assertNotNull(validateCheckoutDiscount(base, fixed("1.001")))
    }

    @Test
    fun cartMutationClearsAuthoritativePreview() {
        val state = CheckoutUiState(
            cartItems = listOf(item(price = "100", quantity = 1)),
            checkoutPreview = preview(),
        )

        val changed = state.recalculateCheckoutState(
            items = listOf(item(price = "100", quantity = 2)),
            discount = null,
        )

        assertNull(changed.checkoutPreview)
        assertMoney("200.00", changed.finalTotal)
    }

    @Test
    fun previewControlsUnderAndOverLimitFlowWithoutLocalLimit() {
        assertEquals(CheckoutPreviewDecision.FINAL_CHECKOUT, preview(approvalRequired = false).nextStep())
        assertEquals(CheckoutPreviewDecision.MANAGER_APPROVAL, preview(approvalRequired = true).nextStep())
    }

    @Test
    fun authoritativePreviewReplacesEstimatedGrossDiscountAndNet() {
        val authoritative = preview(approvalRequired = false)
        val state = CheckoutUiState(
            cartItems = listOf(item(price = "999", quantity = 1)),
            finalTotal = BigDecimal("999"),
        ).withAuthoritativePreview(authoritative)

        assertMoney("100.00", state.subtotal)
        assertMoney("15.00", state.totalDiscount)
        assertMoney("85.00", state.finalTotal)
        assertEquals(authoritative.checkoutAttemptId, state.checkoutPreview?.checkoutAttemptId)
    }

    @Test
    fun loadingGateBlocksDoubleCheckoutAndApprovalErrorsRequireFreshPreview() {
        assertTrue(CheckoutUiState().canStartCheckout())
        assertFalse(CheckoutUiState(isLoading = true).canStartCheckout())
        assertFalse(CheckoutUiState(isPreviewLoading = true).canStartCheckout())
        assertTrue(requiresFreshDiscountPreview("MANAGER_APPROVAL_EXPIRED"))
        assertTrue(requiresFreshDiscountPreview("MANAGER_APPROVAL_SCOPE_MISMATCH"))
        assertFalse(requiresFreshDiscountPreview("NETWORK_TIMEOUT"))
    }

    private fun item(
        price: String,
        quantity: Int,
        discount: CheckoutDiscount? = null,
    ) = CartItem(
        productId = "product-1",
        productName = "Produk",
        unitPrice = BigDecimal(price),
        quantity = quantity,
        discountRequest = discount,
    )

    private fun percent(value: String) = CheckoutDiscount(DiscountType.PERCENTAGE, BigDecimal(value))
    private fun fixed(value: String) = CheckoutDiscount(DiscountType.FIXED_AMOUNT, BigDecimal(value))

    private fun preview(approvalRequired: Boolean = false) = CheckoutPreview(
        checkoutAttemptId = "attempt",
        discountFingerprint = "fingerprint",
        items = emptyList(),
        grossSubtotal = BigDecimal("100.00"),
        itemDiscountTotal = BigDecimal("10.00"),
        transactionDiscountAmount = BigDecimal("5.00"),
        totalDiscountAmount = BigDecimal("15.00"),
        effectiveDiscountPercent = BigDecimal("15.00"),
        netTotal = BigDecimal("85.00"),
        cashierDiscountLimitPercent = BigDecimal.TEN,
        approvalRequired = approvalRequired,
        expiresAt = "2099-01-01T00:00:00Z",
    )

    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
