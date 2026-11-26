package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.session.SessionManager

class SyncManager(
    private val database: TbTerminalDatabase,
    private val networkMonitor: NetworkConnectivity,
    private val backendHealthMonitor: BackendHealthMonitor,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource,
    private val sessionManager: SessionManager,
    private val offlineCashSessionSyncService: OfflineCashSessionSyncService,
    private val offlineCheckoutSyncService: OfflineCheckoutSyncService,
    private val offlineCashExpenseSyncService: OfflineCashExpenseSyncService
) {
    suspend fun enqueue(
        entityType: SyncEntityType,
        entityLocalId: Long,
        operation: SyncOperation,
        payloadJson: String
    ): Long {
        require(entityType in SyncEntityType.supported) {
            "Offline sync hanya mendukung TRANSACTION, CASH_SESSION, dan CASH_EXPENSE"
        }
        return database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = entityType,
                entityLocalId = entityLocalId,
                operation = operation,
                payloadJson = payloadJson
            )
        )
    }

    suspend fun runOnce(): SyncResult {
        if (localAppSettingsDataSource.getDataMode() != AppDataMode.SYNC_SERVER) {
            return SyncResult.Skipped("Mode sinkronisasi server belum aktif.")
        }

        if (!sessionManager.hasAccessToken()) {
            return SyncResult.AuthRequired("Login ulang sebelum sinkronisasi.")
        }

        if (!networkMonitor.isOnline()) {
            return SyncResult.Retry("Perangkat sedang offline.")
        }

        if (backendHealthMonitor.refresh() != BackendStatus.CONNECTED) {
            return SyncResult.Retry("Server belum tersambung.")
        }

        return runSuspendCatching {
            OfflineSyncCoordinator.withSyncLock {
                var totalProcessed = 0
                var completedAllBatches = false
                for (batch in 0 until MAX_BATCHES_PER_RUN) {
                    val sessionResult = offlineCashSessionSyncService.syncPendingOpenSessionsUnlocked()
                    if (sessionResult.hasAuthFailure) {
                        return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                    }
                    val result = offlineCheckoutSyncService.syncPendingOnlyUnlocked()
                    val expenseResult = offlineCashExpenseSyncService.syncPendingExpensesUnlocked()
                    val closeSessionResult = offlineCashSessionSyncService.syncPendingCloseSessionsUnlocked()

                    if (result.hasAuthFailure || expenseResult.hasAuthFailure || closeSessionResult.hasAuthFailure) {
                        return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                    }

                    val maxRetryAfter = listOfNotNull(
                        sessionResult.maxRetryAfterSeconds,
                        result.maxRetryAfterSeconds,
                        expenseResult.maxRetryAfterSeconds,
                        closeSessionResult.maxRetryAfterSeconds,
                    ).maxOrNull()

                    when {
                        result.hasTransientFailure -> return@withSyncLock SyncResult.Retry(
                            reason = "Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.",
                            retryAfterSeconds = maxRetryAfter,
                        )
                        sessionResult.hasTransientFailure -> return@withSyncLock SyncResult.Retry(
                            reason = "Koneksi server belum stabil. Sinkronisasi sesi kas akan dicoba lagi.",
                            retryAfterSeconds = maxRetryAfter,
                        )
                        expenseResult.hasTransientFailure -> return@withSyncLock SyncResult.Retry(
                            reason = "Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.",
                            retryAfterSeconds = maxRetryAfter,
                        )
                        closeSessionResult.hasTransientFailure -> return@withSyncLock SyncResult.Retry(
                            reason = "Koneksi server belum stabil. Sinkronisasi tutup sesi kas akan dicoba lagi.",
                            retryAfterSeconds = maxRetryAfter,
                        )
                    }

                    val batchProcessed = sessionResult.successCount +
                        sessionResult.failedCount +
                        result.successCount +
                        result.failedCount +
                        expenseResult.successCount +
                        expenseResult.failedCount +
                        closeSessionResult.successCount +
                        closeSessionResult.failedCount
                    totalProcessed += batchProcessed

                    if (batchProcessed == 0) {
                        completedAllBatches = true
                        break
                    }
                }
                val hasMorePending = !completedAllBatches && hasRemainingPendingWork()
                SyncResult.Success(
                    processedCount = totalProcessed,
                    hasMorePending = hasMorePending,
                )
            }
        }.getOrElse { error ->
            SyncResult.Failed(error)
        }
    }

    private suspend fun hasRemainingPendingWork(): Boolean {
        return database.cashSessionDao().countPendingOpenSessions() > 0 ||
            database.transactionDao().countPendingTransactions() > 0 ||
            database.cashExpenseDao().countPendingExpenses() > 0 ||
            database.cashSessionDao().countActionablePendingCloseSessions() > 0
    }

    private companion object {
        const val MAX_BATCHES_PER_RUN = 10
    }
}

sealed interface SyncResult {
    data class Success(
        val processedCount: Int,
        val hasMorePending: Boolean = false,
    ) : SyncResult
    data class Skipped(val reason: String) : SyncResult
    data class Retry(
        val reason: String,
        val retryAfterSeconds: Long? = null,
    ) : SyncResult
    data class AuthRequired(val reason: String) : SyncResult
    data class Failed(val error: Throwable) : SyncResult
}
