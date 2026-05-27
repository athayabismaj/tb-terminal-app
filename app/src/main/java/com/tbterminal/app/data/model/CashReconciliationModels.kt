package com.tbterminal.app.data.model

import java.math.BigDecimal

data class CashSession(
    val id: String,
    val userId: String,
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

data class CashExpense(
    val id: String,
    val amount: BigDecimal,
    val description: String,
    val createdAt: String
)

data class CashTransaction(
    val id: String,
    val receiptId: String,
    val sessionId: String,
    val customerId: String?,
    val customerName: String?,
    val type: String,
    val status: String,
    val total: BigDecimal,
    val paidAmount: BigDecimal,
    val createdAt: String
)

data class CashTransactionDetail(
    val id: String,
    val receiptId: String,
    val sessionId: String,
    val customerId: String?,
    val customerName: String?,
    val type: String,
    val status: String,
    val total: BigDecimal,
    val paidAmount: BigDecimal,
    val createdAt: String,
    val items: List<CashTransactionItem>
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
