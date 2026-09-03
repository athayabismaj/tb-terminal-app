package com.tbterminal.app.data.model

import java.math.BigDecimal

enum class DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT,
}

data class CheckoutDiscount(
    val type: DiscountType,
    val value: BigDecimal,
)

data class CheckoutSubmitCommand(
    val idempotencyKey: String,
    val items: List<CheckoutSubmitItem>,
    val paymentMethod: String,
    val amountPaid: String,
    val customerId: String? = null,
    val notes: String? = null,
    val dueDays: Int = 30,
    val transactionDiscount: CheckoutDiscount? = null,
    val checkoutAttemptId: String? = null,
    val managerApprovalId: String? = null,
)

data class CheckoutSubmitItem(
    val productId: String,
    val quantity: Int,
    val discountRequest: CheckoutDiscount? = null,
)

data class CheckoutPreviewCommand(
    val items: List<CheckoutSubmitItem>,
    val transactionDiscount: CheckoutDiscount? = null,
)

data class CheckoutPreviewItem(
    val productId: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val grossLineTotal: BigDecimal,
    val discountType: DiscountType?,
    val discountValue: BigDecimal,
    val discountAmount: BigDecimal,
    val netLineTotal: BigDecimal,
)

data class CheckoutPreview(
    val checkoutAttemptId: String,
    val discountFingerprint: String,
    val items: List<CheckoutPreviewItem>,
    val grossSubtotal: BigDecimal,
    val itemDiscountTotal: BigDecimal,
    val transactionDiscountAmount: BigDecimal,
    val totalDiscountAmount: BigDecimal,
    val effectiveDiscountPercent: BigDecimal,
    val netTotal: BigDecimal,
    val cashierDiscountLimitPercent: BigDecimal,
    val approvalRequired: Boolean,
    val expiresAt: String,
)

data class CheckoutReceipt(
    val receiptId: String,
    val transactionId: String,
    val amountTendered: String,
    val changeAmount: String,
    val idempotentReplay: Boolean
)
