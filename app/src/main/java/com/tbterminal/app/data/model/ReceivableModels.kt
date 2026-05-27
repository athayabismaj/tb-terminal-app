package com.tbterminal.app.data.model

import java.math.BigDecimal

data class Receivable(
    val id: String,
    val customerId: String,
    val customerName: String,
    val transactionId: String,
    val amount: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val dueDate: String,
    val status: String,
    val createdAt: String
)

data class ReceivablePage(
    val data: List<Receivable>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class CreateReceivablePaymentCommand(
    val receivableId: String,
    val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?
)

data class ReceivablePaymentReceipt(
    val id: String,
    val receivableId: String,
    val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?,
    val paidAt: String,
    val receivableStatus: String,
    val receivableRemainingAmount: BigDecimal
)
