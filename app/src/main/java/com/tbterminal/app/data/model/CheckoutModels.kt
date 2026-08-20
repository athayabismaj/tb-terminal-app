package com.tbterminal.app.data.model

data class CheckoutSubmitCommand(
    val idempotencyKey: String,
    val items: List<CheckoutSubmitItem>,
    val paymentMethod: String,
    val amountPaid: String,
    val customerId: String? = null,
    val notes: String? = null,
    val dueDays: Int = 30
)

data class CheckoutSubmitItem(
    val productId: String,
    val quantity: Int
)

data class CheckoutReceipt(
    val receiptId: String,
    val transactionId: String,
    val amountTendered: String,
    val changeAmount: String,
    val idempotentReplay: Boolean
)
