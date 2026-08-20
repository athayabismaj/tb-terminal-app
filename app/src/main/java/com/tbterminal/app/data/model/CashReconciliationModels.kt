package com.tbterminal.app.data.model

import java.math.BigDecimal

data class CashSession(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val openedAt: String,
    val closedAt: String?,
    val openingCash: BigDecimal,
    val closingCash: BigDecimal?,
    val systemCash: BigDecimal?,
    val difference: BigDecimal?,
    val totalExpenses: BigDecimal,
    val notes: String?,
    val status: String
)

data class CashSessionPage(
    val data: List<CashSession>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class CashExpense(
    val id: String,
    val sessionId: String,
    val userId: String,
    val userName: String? = null,
    val amount: BigDecimal,
    val description: String,
    val createdAt: String
)

data class CashExpensePage(
    val data: List<CashExpense>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

data class CashTransaction(
    val id: String,
    val receiptId: String,
    val sessionId: String,
    val customerId: String?,
    val customerName: String?,
    val cashierId: String = "",
    val cashierName: String? = null,
    val paymentMethods: List<String> = emptyList(),
    val type: String,
    val status: String,
    val total: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal? = null,
    val createdAt: String,
    val voidedAt: String? = null,
    val voidReason: String? = null
)

data class CashTransactionDetail(
    val id: String,
    val receiptId: String,
    val sessionId: String,
    val customerId: String?,
    val customerName: String?,
    val userId: String = "",
    val cashierName: String? = null,
    val paymentMethods: List<String> = emptyList(),
    val type: String,
    val status: String,
    val total: BigDecimal,
    val paidAmount: BigDecimal,
    val amountTendered: BigDecimal,
    val changeAmount: BigDecimal,
    val createdAt: String,
    val voidedAt: String? = null,
    val voidedByName: String? = null,
    val voidReason: String? = null,
    val items: List<CashTransactionItem>
)

data class TransactionVoidResult(
    val voidId: String,
    val transactionId: String,
    val receiptId: String,
    val reason: String,
    val voidedAt: String,
    val idempotentReplay: Boolean
)

data class CashTransactionItem(
    val productId: String,
    val productName: String,
    val unitId: String,
    val quantity: BigDecimal,
    val priceAtTransaction: BigDecimal,
    val subtotal: BigDecimal
)

data class CashTransactionPage(
    val data: List<CashTransaction>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)
