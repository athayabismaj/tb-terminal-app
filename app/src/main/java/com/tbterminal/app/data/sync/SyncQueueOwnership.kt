package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.dao.CashExpenseDao
import com.tbterminal.app.data.local.dao.CashSessionDao
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus

internal data class PartitionedSyncQueue(
    val ownItems: List<SyncQueueEntity>,
    val otherUsersPendingSyncCount: Int,
)

internal suspend fun resolveSyncQueueOwnerUserId(
    item: SyncQueueEntity,
    sessionsById: Map<Long, LocalCashSessionEntity>,
    cashSessionDao: CashSessionDao?,
    transactionDao: TransactionDao?,
    cashExpenseDao: CashExpenseDao?,
): String? {
    val rawOwner = when (item.entityType) {
        SyncEntityType.CASH_SESSION -> {
            sessionsById[item.entityLocalId]?.cashierUserId
                ?: cashSessionDao?.getByLocalId(item.entityLocalId)?.cashierUserId
        }
        SyncEntityType.TRANSACTION -> {
            val tx = transactionDao?.getByLocalId(item.entityLocalId)
            tx?.cashierUserId?.takeIf(String::isNotBlank)
                ?: tx?.cashSessionLocalId?.let { sid ->
                    sessionsById[sid]?.cashierUserId ?: cashSessionDao?.getByLocalId(sid)?.cashierUserId
                }
        }
        SyncEntityType.CASH_EXPENSE -> {
            val exp = cashExpenseDao?.getByLocalId(item.entityLocalId)
            exp?.cashSessionLocalId?.let { sid ->
                sessionsById[sid]?.cashierUserId ?: cashSessionDao?.getByLocalId(sid)?.cashierUserId
            }
        }
        else -> null
    }
    return rawOwner?.trim()?.takeIf(String::isNotBlank)
}

internal suspend fun partitionSyncQueueByUser(
    queue: List<SyncQueueEntity>,
    activeUserId: String?,
    sessions: List<LocalCashSessionEntity>,
    cashSessionDao: CashSessionDao?,
    transactionDao: TransactionDao?,
    cashExpenseDao: CashExpenseDao?,
    applyIsolationFilter: Boolean,
): PartitionedSyncQueue {
    if (!applyIsolationFilter) {
        return PartitionedSyncQueue(ownItems = queue, otherUsersPendingSyncCount = 0)
    }
    val normalizedActiveId = activeUserId?.trim()?.takeIf(String::isNotBlank)
    val sessionsById = sessions.associateBy { it.localId }
    val ownItems = mutableListOf<SyncQueueEntity>()
    var otherUsersPendingCount = 0

    for (item in queue) {
        val owner = resolveSyncQueueOwnerUserId(
            item = item,
            sessionsById = sessionsById,
            cashSessionDao = cashSessionDao,
            transactionDao = transactionDao,
            cashExpenseDao = cashExpenseDao,
        )
        val belongsToActiveUser = when {
            normalizedActiveId == null -> owner == null
            owner == null -> true
            else -> owner == normalizedActiveId
        }
        if (belongsToActiveUser) {
            ownItems += item
        } else if (item.status != SyncStatus.SYNCED) {
            otherUsersPendingCount += 1
        }
    }
    return PartitionedSyncQueue(
        ownItems = ownItems,
        otherUsersPendingSyncCount = otherUsersPendingCount,
    )
}
