package com.tbterminal.app.data.local.model

data class LocalPendingTransactionWithError(
    val localId: Long,
    val transactionCode: String,
    val occurredAt: Long,
    val total: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val syncStatus: SyncStatus,
    val lastError: String?
)
