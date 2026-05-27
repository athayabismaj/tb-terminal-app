package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequest(
    val customerId: String? = null,
    val items: List<CheckoutLineItemRequest>,
    val paymentMethod: String,
    val amountPaid: String,
    val notes: String? = null,
    val dueDays: Int = 30
)

@Serializable
data class CheckoutLineItemRequest(
    val productId: String,
    val qty: String,
    val discount: String = "0"
)

@Serializable
data class CheckoutResponse(
    val id: String,
    val receiptId: String? = null
)
