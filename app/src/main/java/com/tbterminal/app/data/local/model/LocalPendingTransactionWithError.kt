package com.tbterminal.app.data.local.model

data class LocalPendingTransactionWithError(
    val localId: Long,
    val transactionCode: String,
    val occurredAt: Long,
    val total: ScaledDecimal2,
    val paidAmount: ScaledDecimal2,
    val remainingAmount: ScaledDecimal2,
    val syncStatus: SyncStatus,
    val lastError: String?
)
