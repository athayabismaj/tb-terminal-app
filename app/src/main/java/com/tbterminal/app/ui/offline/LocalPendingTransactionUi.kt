package com.tbterminal.app.ui.offline

import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.model.LocalPendingTransactionWithError
import com.tbterminal.app.data.local.model.SyncStatus
import java.math.BigDecimal

data class LocalPendingTransactionUi(
    val localId: Long,
    val transactionCode: String,
    val occurredAt: Long,
    val total: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val syncStatus: SyncStatus,
    val lastError: String? = null
)

fun LocalTransactionEntity.toLocalPendingTransactionUi(): LocalPendingTransactionUi =
    LocalPendingTransactionUi(
        localId = localId,
        transactionCode = transactionCode,
        occurredAt = occurredAt,
        total = total.toBigDecimal(),
        paidAmount = paidAmount.toBigDecimal(),
        remainingAmount = remainingAmount.toBigDecimal(),
        syncStatus = syncStatus
    )

fun LocalPendingTransactionWithError.toLocalPendingTransactionUi(): LocalPendingTransactionUi =
    LocalPendingTransactionUi(
        localId = localId,
        transactionCode = transactionCode,
        occurredAt = occurredAt,
        total = total.toBigDecimal(),
        paidAmount = paidAmount.toBigDecimal(),
        remainingAmount = remainingAmount.toBigDecimal(),
        syncStatus = syncStatus,
        lastError = lastError
    )
