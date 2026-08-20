package com.tbterminal.app.data.model

import java.math.BigDecimal

data class StockMovement(
    val id: String,
    val productId: String,
    val sku: String,
    val productName: String,
    val unitName: String,
    val type: String,
    val balanceBefore: BigDecimal,
    val qtyIn: BigDecimal,
    val qtyOut: BigDecimal,
    val balanceAfter: BigDecimal,
    val referenceType: String,
    val referenceNumber: String?,
    val occurredAt: String
)

data class StockCard(
    val data: List<StockMovement>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
    val currentStock: BigDecimal?,
    val ledgerBalance: BigDecimal?,
    val reconciled: Boolean
)
