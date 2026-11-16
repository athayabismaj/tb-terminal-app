package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutPaymentLineRequestDto(
    val method: String,
    val amount: String,
    val reference: String? = null,
)

@Serializable
data class CheckoutRequest(
    val idempotencyKey: String,
    val customerId: String? = null,
    val items: List<CheckoutLineItemRequest>,
    val paymentMethod: String,
    val amountPaid: String,
    val payments: List<CheckoutPaymentLineRequestDto>? = null,
    val notes: String? = null,
    val dueDays: Int = 30,
    val transactionDiscount: DiscountRequestDto? = null,
    val checkoutAttemptId: String? = null,
    val managerApprovalId: String? = null,
)

@Serializable
data class CheckoutLineItemRequest(
    val productId: String,
    val qty: String,
    val discountRequest: DiscountRequestDto? = null,
    val note: String? = null,
)

@Serializable
data class DiscountRequestDto(
    val type: String,
    val value: String,
)

@Serializable
data class CheckoutPreviewRequest(
    val items: List<CheckoutLineItemRequest>,
    val transactionDiscount: DiscountRequestDto? = null,
    val customerId: String? = null,
)

@Serializable
data class CheckoutPreviewItemResponse(
    val productId: String,
    val quantity: String,
    val unitPrice: String,
    val grossLineTotal: String,
    val discountType: String? = null,
    val discountValue: String,
    val discountAmount: String,
    val netLineTotal: String,
)

@Serializable
data class CheckoutPreviewResponse(
    val checkoutAttemptId: String,
    val discountFingerprint: String,
    val items: List<CheckoutPreviewItemResponse>,
    val grossSubtotal: String,
    val itemDiscountTotal: String,
    val transactionDiscountAmount: String,
    val totalDiscountAmount: String,
    val effectiveDiscountPercent: String,
    val netTotal: String,
    val cashierDiscountLimitPercent: String,
    val approvalRequired: Boolean,
    val expiresAt: String,
    val taxEnabled: Boolean = false,
    val taxMode: String? = null,
    val taxRate: String = "0",
    val taxLabel: String? = null,
    val taxableBase: String = "0",
    val taxAmount: String = "0",
    val totalWithTax: String = netTotal,
    val cashRoundingUnit: Int = 0,
    val cashRoundingRule: String = "NEAREST",
    val estimatedCashRoundingAmount: String = "0",
    val estimatedTotalWithCashRounding: String = totalWithTax,
)

@Serializable
data class CheckoutResponse(
    val id: String,
    val receiptId: String? = null,
    val amountTendered: String = "0",
    val changeAmount: String = "0",
    val idempotentReplay: Boolean = false,
    val grossSubtotal: String = "0",
    val itemDiscountTotal: String = "0",
    val transactionDiscountAmount: String = "0",
    val totalDiscountAmount: String = "0",
    val taxableBase: String = "0",
    val taxAmount: String = "0",
    val roundingAmount: String = "0",
    val total: String = "0",
)
