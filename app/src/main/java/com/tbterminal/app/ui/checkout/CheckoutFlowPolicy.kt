package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CheckoutPreview

internal enum class CheckoutPreviewDecision {
    FINAL_CHECKOUT,
    MANAGER_APPROVAL,
}

internal fun CheckoutPreview.nextStep(): CheckoutPreviewDecision =
    if (approvalRequired) CheckoutPreviewDecision.MANAGER_APPROVAL
    else CheckoutPreviewDecision.FINAL_CHECKOUT

internal fun CheckoutUiState.canStartCheckout(): Boolean = !isLoading && !isPreviewLoading

internal fun requiresFreshDiscountPreview(errorCode: String): Boolean = errorCode in setOf(
    "DISCOUNT_APPROVAL_SCOPE_MISMATCH",
    "MANAGER_APPROVAL_SCOPE_MISMATCH",
    "MANAGER_APPROVAL_EXPIRED",
    "MANAGER_APPROVAL_ALREADY_USED",
    "INVALID_DISCOUNT",
    "DISCOUNT_EXCEEDS_AMOUNT",
)

internal fun CheckoutUiState.withAuthoritativePreview(preview: CheckoutPreview): CheckoutUiState = copy(
    subtotal = preview.grossSubtotal,
    itemDiscountTotal = preview.itemDiscountTotal,
    transactionDiscountAmount = preview.transactionDiscountAmount,
    totalDiscount = preview.totalDiscountAmount,
    finalTotal = preview.netTotal,
    checkoutPreview = preview,
    approvedManagerApprovalId = null,
    hasAmbiguousCheckout = false,
    amountPaidInput = when (selectedPaymentMethod) {
        PaymentMethod.HUTANG -> "0"
        PaymentMethod.DP -> amountPaidInput
        else -> preview.netTotal.toPlainString()
    },
    isPreviewLoading = false,
    errorEvent = null,
)
