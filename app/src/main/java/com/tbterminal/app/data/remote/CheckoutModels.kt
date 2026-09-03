package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequest(
    val idempotencyKey: String,
    val customerId: String? = null,
    val items: List<CheckoutLineItemRequest>,
    val paymentMethod: String,
    val amountPaid: String,
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
    val total: String = "0",
)
