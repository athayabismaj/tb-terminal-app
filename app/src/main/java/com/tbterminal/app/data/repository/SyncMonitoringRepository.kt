package com.tbterminal.app.data.repository

import com.tbterminal.app.data.local.dao.SyncQueueDao
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.sync.OfflineCashExpenseSyncService
import com.tbterminal.app.data.sync.OfflineCashSessionSyncResult
import com.tbterminal.app.data.sync.OfflineCashSessionSyncService
import com.tbterminal.app.data.sync.OfflineCheckoutSyncResult
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.data.sync.SyncErrorCategory
import com.tbterminal.app.data.sync.SyncErrorClassification
import com.tbterminal.app.data.sync.SyncErrorClassifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SyncMonitoringRepository(
    private val syncQueueDao: SyncQueueDao,
    private val offlineCashSessionSyncService: OfflineCashSessionSyncService,
    private val offlineCheckoutSyncService: OfflineCheckoutSyncService,
    private val offlineCashExpenseSyncService: OfflineCashExpenseSyncService
) {
    fun observeSnapshot(): Flow<SyncMonitoringSnapshot> {
        return syncQueueDao.observeQueue().map { queue ->
            val startOfToday = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            SyncMonitoringSnapshot(
                totalPending = queue.count { it.status == SyncStatus.PENDING },
                totalSyncing = queue.count { it.status == SyncStatus.SYNCING },
                totalFailed = queue.count { it.status == SyncStatus.FAILED },
                totalConflict = queue.count { it.status == SyncStatus.CONFLICT },
                totalSyncedToday = queue.count { it.status == SyncStatus.SYNCED && it.updatedAt >= startOfToday },
                queueItems = queue.map { it.toUiModel() }
            )
        }
    }

    suspend fun refresh(): SyncMonitoringSnapshot {
        val queue = syncQueueDao.getByStatus(SyncStatus.PENDING, MAX_QUEUE_READ) +
            syncQueueDao.getByStatus(SyncStatus.SYNCING, MAX_QUEUE_READ) +
            syncQueueDao.getByStatus(SyncStatus.FAILED, MAX_QUEUE_READ) +
            syncQueueDao.getByStatus(SyncStatus.CONFLICT, MAX_QUEUE_READ) +
            syncQueueDao.getByStatus(SyncStatus.SYNCED, MAX_QUEUE_READ)
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return SyncMonitoringSnapshot(
            totalPending = queue.count { it.status == SyncStatus.PENDING },
            totalSyncing = queue.count { it.status == SyncStatus.SYNCING },
            totalFailed = queue.count { it.status == SyncStatus.FAILED },
            totalConflict = queue.count { it.status == SyncStatus.CONFLICT },
            totalSyncedToday = queue.count { it.status == SyncStatus.SYNCED && it.updatedAt >= startOfToday },
            queueItems = queue
                .distinctBy { it.id }
                .sortedByDescending { it.updatedAt }
                .map { it.toUiModel() }
        )
    }

    suspend fun retryItem(queueId: Long): SyncRetryResult {
        val item = syncQueueDao.getById(queueId)
            ?: return SyncRetryResult.Failed("Item sync tidak ditemukan.")
        return retryQueueItem(item)
    }

    suspend fun retryAllPending(): SyncBatchRetryResult {
        val items = syncQueueDao.getByStatus(SyncStatus.PENDING, MAX_QUEUE_READ)
        return retryQueueItemsInSafeOrder(items)
    }

    suspend fun retryAllFailed(): SyncBatchRetryResult {
        val items = syncQueueDao.getByStatus(SyncStatus.FAILED, MAX_QUEUE_READ)
        return retryQueueItemsInSafeOrder(items)
    }

    suspend fun markConflictReviewed(queueId: Long): SyncRetryResult {
        val item = syncQueueDao.getById(queueId)
            ?: return SyncRetryResult.Failed("Item sync tidak ditemukan.")
        if (item.status != SyncStatus.CONFLICT) {
            return SyncRetryResult.Failed("Hanya item conflict yang bisa ditandai sudah ditinjau.")
        }
        val currentError = item.lastError?.takeIf { it.isNotBlank() } ?: "Conflict sudah ditinjau."
        val reviewedError = if (currentError.startsWith(REVIEWED_PREFIX)) {
            currentError
        } else {
            "$REVIEWED_PREFIX $currentError"
        }
        syncQueueDao.updateStatus(
            id = item.id,
            status = SyncStatus.CONFLICT,
            lastError = reviewedError,
            retryIncrement = 0,
            updatedAt = System.currentTimeMillis()
        )
        return SyncRetryResult.Success("Conflict ditandai sudah ditinjau.")
    }

    private suspend fun retryQueueItemsInSafeOrder(items: List<SyncQueueEntity>): SyncBatchRetryResult {
        if (items.isEmpty()) return SyncBatchRetryResult(total = 0, successCount = 0, failedCount = 0)

        var successCount = 0
        var failedCount = 0
        orderedForSync(items).forEach { item ->
            when (retryQueueItem(item)) {
                is SyncRetryResult.Success -> successCount += 1
                is SyncRetryResult.Failed -> failedCount += 1
                is SyncRetryResult.Unsupported -> failedCount += 1
            }
        }
        return SyncBatchRetryResult(total = items.size, successCount = successCount, failedCount = failedCount)
    }

    private fun orderedForSync(items: List<SyncQueueEntity>): List<SyncQueueEntity> {
        return items.sortedWith(
            compareBy<SyncQueueEntity> { item ->
                when {
                    item.entityType == SyncEntityType.CASH_SESSION && item.operation == SyncOperation.CREATE -> 0
                    item.entityType == SyncEntityType.TRANSACTION -> 1
                    item.entityType == SyncEntityType.CASH_EXPENSE -> 2
                    item.entityType == SyncEntityType.CASH_SESSION && item.operation == SyncOperation.UPDATE -> 3
                    else -> 4
                }
            }.thenBy { it.createdAt }
        )
    }

    private suspend fun retryQueueItem(item: SyncQueueEntity): SyncRetryResult {
        return when (item.entityType) {
            SyncEntityType.TRANSACTION -> {
                when (val result = offlineCheckoutSyncService.syncOne(item.entityLocalId)) {
                    is OfflineCheckoutSyncResult.Success -> SyncRetryResult.Success("Transaksi tersinkron.")
                    is OfflineCheckoutSyncResult.Failed -> SyncRetryResult.Failed(result.message)
                }
            }

            SyncEntityType.CASH_SESSION -> {
                val result = when (item.operation) {
                    SyncOperation.CREATE -> offlineCashSessionSyncService.syncOpenSession(item.entityLocalId)
                    SyncOperation.UPDATE -> offlineCashSessionSyncService.syncCloseSessionIfReady(item.entityLocalId)
                    SyncOperation.DELETE -> OfflineCashSessionSyncResult.Failed("Delete sesi kas lokal belum didukung.")
                }
                when (result) {
                    is OfflineCashSessionSyncResult.Success -> SyncRetryResult.Success("Sesi kas tersinkron.")
                    is OfflineCashSessionSyncResult.Failed -> SyncRetryResult.Failed(result.message)
                    is OfflineCashSessionSyncResult.Skipped -> SyncRetryResult.Failed(result.message)
                }
            }

            SyncEntityType.CASH_EXPENSE -> {
                when (val result = offlineCashExpenseSyncService.syncExpense(item.entityLocalId)) {
                    is com.tbterminal.app.data.sync.OfflineCashExpenseSyncResult.Success -> {
                        SyncRetryResult.Success("Pengeluaran kas tersinkron.")
                    }
                    is com.tbterminal.app.data.sync.OfflineCashExpenseSyncResult.Failed -> {
                        SyncRetryResult.Failed(result.message)
                    }
                }
            }

            else -> SyncRetryResult.Unsupported("Retry untuk ${item.entityType.name} belum tersedia.")
        }
    }

    private fun SyncQueueEntity.toUiModel(): SyncQueueUiModel {
        val classification = conflictAwareClassification(status, lastError)
        return SyncQueueUiModel(
            localId = id,
            entityType = entityType,
            entityId = entityLocalId,
            syncStatus = status,
            retryCount = retryCount,
            lastError = lastError,
            errorCategory = classification.category,
            recommendation = classification.recommendation,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun conflictAwareClassification(
        status: SyncStatus,
        lastError: String?
    ): SyncErrorClassification {
        val classification = SyncErrorClassifier.classifyMessage(lastError)
        return if (status == SyncStatus.CONFLICT && classification.category != SyncErrorCategory.CONFLICT) {
            SyncErrorClassification(
                category = SyncErrorCategory.CONFLICT,
                recommendation = "Tinjau data lokal dan aturan bisnis server sebelum mencoba sync ulang."
            )
        } else {
            classification
        }
    }

    private companion object {
        const val MAX_QUEUE_READ = 500
        const val REVIEWED_PREFIX = "[DITINJAU]"
    }
}

data class SyncMonitoringSnapshot(
    val totalPending: Int = 0,
    val totalSyncing: Int = 0,
    val totalFailed: Int = 0,
    val totalConflict: Int = 0,
    val totalSyncedToday: Int = 0,
    val queueItems: List<SyncQueueUiModel> = emptyList()
)

data class SyncQueueUiModel(
    val localId: Long,
    val entityType: SyncEntityType,
    val entityId: Long,
    val syncStatus: SyncStatus,
    val retryCount: Int,
    val lastError: String?,
    val errorCategory: SyncErrorCategory,
    val recommendation: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    val updatedAtText: String
        get() = Instant.ofEpochMilli(updatedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
}

sealed interface SyncRetryResult {
    data class Success(val message: String) : SyncRetryResult
    data class Failed(val message: String) : SyncRetryResult
    data class Unsupported(val message: String) : SyncRetryResult
}

data class SyncBatchRetryResult(
    val total: Int,
    val successCount: Int,
    val failedCount: Int
)
