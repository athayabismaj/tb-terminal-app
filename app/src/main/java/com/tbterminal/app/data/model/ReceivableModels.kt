package com.tbterminal.app.data.model

import java.math.BigDecimal

data class Receivable(
    val id: String,
    val customerId: String,
    val customerName: String,
    val transactionId: String?,
    val source: String,
    val amount: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val debtDate: String,
    val dueDate: String,
    val status: String,
    val legacyInvoiceNumber: String?,
    val notes: String?,
    val createdBy: String,
    val isActive: Boolean,
    val createdAt: String
)

data class CreateOpeningReceivableCommand(
    val customerId: String,
    val amount: BigDecimal,
    val debtDate: String,
    val dueDate: String,
    val legacyInvoiceNumber: String?,
    val notes: String?
)

data class CreateReceivableAdjustmentCommand(
    val customerId: String,
    val amount: BigDecimal,
    val debtDate: String,
    val dueDate: String,
    val reference: String,
    val reason: String
)

data class CustomerReceivableSummary(
    val customerId: String,
    val customerName: String,
    val totalAmount: BigDecimal,
    val totalPaid: BigDecimal,
    val totalRemaining: BigDecimal,
    val unpaidCount: Long,
    val overdueCount: Long,
    val nearestDueDate: String?
)

data class CustomerReceivableSummaryPage(
    val data: List<CustomerReceivableSummary>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
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
    val notes: String?,
    val idempotencyKey: String
)

data class ReverseReceivablePaymentCommand(
    val paymentId: String,
    val reason: String,
    val idempotencyKey: String
)

data class ReceivablePaymentReceipt(
    val id: String,
    val paymentNumber: String,
    val receivableId: String,
    val customerId: String,
    val customerName: String,
    val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?,
    val paidAt: String,
    val paymentDate: String,
    val entryType: String,
    val reversedPaymentId: String?,
    val receivedBy: String,
    val receivedByName: String,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal,
    val receivableStatus: String,
    val receivableRemainingAmount: BigDecimal,
    val idempotentReplay: Boolean
)

data class ReceivablePaymentHistory(
    val id: String,
    val paymentNumber: String,
    val receivableId: String,
    val customerId: String,
    val customerName: String,
    val transactionId: String?,
    val source: String,
    val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?,
    val paidAt: String,
    val paymentDate: String,
    val entryType: String,
    val reversedPaymentId: String?,
    val receivedBy: String,
    val receivedByName: String,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal,
    val isReversed: Boolean,
    val receivableStatus: String,
    val receivableRemainingAmount: BigDecimal
)

data class ReceivablePaymentHistoryPage(
    val data: List<ReceivablePaymentHistory>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)
