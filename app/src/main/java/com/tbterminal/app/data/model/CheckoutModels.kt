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

data class CheckoutPaymentLine(
    val method: String,
    val amount: String,
    val reference: String? = null,
)

data class CheckoutSubmitCommand(
    val idempotencyKey: String,
    val items: List<CheckoutSubmitItem>,
    val paymentMethod: String,
    val amountPaid: String,
    val payments: List<CheckoutPaymentLine>? = null,
    val customerId: String? = null,
    val notes: String? = null,
    val dueDays: Int = 30,
    val transactionDiscount: CheckoutDiscount? = null,
    val checkoutAttemptId: String? = null,
    val managerApprovalId: String? = null,
)

data class CheckoutSubmitItem(
    val productId: String,
    val quantity: BigDecimal,
    val discountRequest: CheckoutDiscount? = null,
    val note: String? = null,
) {
    constructor(
        productId: String,
        quantity: Int,
        discountRequest: CheckoutDiscount? = null,
        note: String? = null,
    ) : this(
        productId = productId,
        quantity = quantity.toBigDecimal(),
        discountRequest = discountRequest,
        note = note,
    )
}

data class CheckoutPreviewCommand(
    val items: List<CheckoutSubmitItem>,
    val transactionDiscount: CheckoutDiscount? = null,
    val customerId: String? = null,
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
    val taxEnabled: Boolean = false,
    val taxMode: String? = null,
    val taxRate: BigDecimal = BigDecimal.ZERO,
    val taxLabel: String? = null,
    val taxableBase: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val totalWithTax: BigDecimal = netTotal,
    val cashRoundingUnit: Int = 0,
    val cashRoundingRule: String = "NEAREST",
    val estimatedCashRoundingAmount: BigDecimal = BigDecimal.ZERO,
    val estimatedTotalWithCashRounding: BigDecimal = totalWithTax,
)

data class CheckoutReceipt(
    val receiptId: String,
    val transactionId: String,
    val amountTendered: String,
    val changeAmount: String,
    val idempotentReplay: Boolean
)
