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
                offlineCashSessionSyncService.recoverStaleSyncingUnlocked()
                offlineCheckoutSyncService.recoverStaleSyncingUnlocked()
                offlineCashExpenseSyncService.recoverStaleSyncingUnlocked()

                var totalProcessed = 0
                var completedAllBatches = false
                val deferredSessionIds = mutableSetOf<Long>()

                for (batch in 0 until MAX_BATCHES_PER_RUN) {
                    var batchProcessed = 0
                    val candidateSessions = database.cashSessionDao().getSessionsNeedingSync(
                        closedStatus = CASH_SESSION_CLOSED,
                        pending = SyncStatus.PENDING,
                        failed = SyncStatus.PENDING,
                        limit = MAX_SESSION_SCAN_LIMIT,
                    ).filterNot { it.localId in deferredSessionIds }

                    for (session in candidateSessions) {
                        var currentSession = session
                        var sessionProcessed = 0
                        var sessionDeferred = false

                        if (currentSession.serverId.isNullOrBlank()) {
                            when (val openResult = offlineCashSessionSyncService.syncOpenSessionUnlocked(currentSession.localId)) {
                                is OfflineCashSessionSyncResult.Success -> {
                                    sessionProcessed += 1
                                    currentSession = database.cashSessionDao().getByLocalId(currentSession.localId)
                                        ?: currentSession
                                }
                                is OfflineCashSessionSyncResult.Deferred,
                                is OfflineCashSessionSyncResult.Skipped -> {
                                    deferredSessionIds += currentSession.localId
                                    sessionDeferred = true
                                }
                                is OfflineCashSessionSyncResult.Failed -> {
                                    when (openResult.kind) {
                                        OfflineCheckoutSyncFailureKind.AUTH ->
                                            return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                                        OfflineCheckoutSyncFailureKind.TRANSIENT ->
                                            return@withSyncLock SyncResult.Retry(
                                                reason = "Koneksi server belum stabil. Sinkronisasi sesi kas akan dicoba lagi.",
                                                retryAfterSeconds = openResult.retryAfterSeconds,
                                            )
                                        else -> {
                                            sessionProcessed += 1
                                            deferredSessionIds += currentSession.localId
                                        }
                                    }
                                }
                            }
                        }

                        if (!sessionDeferred && !currentSession.serverId.isNullOrBlank()) {
                            val txResult = offlineCheckoutSyncService.syncPendingForSessionUnlocked(
                                cashSessionLocalId = currentSession.localId,
                            )
                            if (txResult.hasAuthFailure) {
                                return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                            }
                            if (txResult.hasTransientFailure) {
                                return@withSyncLock SyncResult.Retry(
                                    reason = "Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.",
                                    retryAfterSeconds = txResult.maxRetryAfterSeconds,
                                )
                            }
                            sessionProcessed += txResult.successCount + txResult.failedCount

                            val expResult = offlineCashExpenseSyncService.syncPendingExpensesForSessionUnlocked(
                                cashSessionLocalId = currentSession.localId,
                            )
                            if (expResult.hasAuthFailure) {
                                return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                            }
                            if (expResult.hasTransientFailure) {
                                return@withSyncLock SyncResult.Retry(
                                    reason = "Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.",
                                    retryAfterSeconds = expResult.maxRetryAfterSeconds,
                                )
                            }
                            sessionProcessed += expResult.successCount + expResult.failedCount

                            val refreshedSession = database.cashSessionDao().getByLocalId(currentSession.localId)
                            if (refreshedSession != null &&
                                refreshedSession.status.equals(CASH_SESSION_CLOSED, ignoreCase = true) &&
                                refreshedSession.syncStatus == SyncStatus.PENDING
                            ) {
                                when (val closeResult = offlineCashSessionSyncService.syncCloseSessionIfReadyUnlocked(refreshedSession.localId)) {
                                    is OfflineCashSessionSyncResult.Success -> {
                                        sessionProcessed += 1
                                        deferredSessionIds.clear()
                                    }
                                    is OfflineCashSessionSyncResult.Deferred,
                                    is OfflineCashSessionSyncResult.Skipped -> Unit
                                    is OfflineCashSessionSyncResult.Failed -> {
                                        when (closeResult.kind) {
                                            OfflineCheckoutSyncFailureKind.AUTH ->
                                                return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                                            OfflineCheckoutSyncFailureKind.TRANSIENT ->
                                                return@withSyncLock SyncResult.Retry(
                                                    reason = "Koneksi server belum stabil. Sinkronisasi tutup sesi kas akan dicoba lagi.",
                                                    retryAfterSeconds = closeResult.retryAfterSeconds,
                                                )
                                            else -> {
                                                sessionProcessed += 1
                                                deferredSessionIds += refreshedSession.localId
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (sessionProcessed > 0) {
                            batchProcessed += sessionProcessed
                            break
                        } else if (!sessionDeferred) {
                            deferredSessionIds += currentSession.localId
                        }
                    }

                    if (batchProcessed == 0) {
                        val orphanTxResult = offlineCheckoutSyncService.syncPendingWithoutLocalSessionUnlocked()
                        if (orphanTxResult.hasAuthFailure) {
                            return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                        }
                        if (orphanTxResult.hasTransientFailure) {
                            return@withSyncLock SyncResult.Retry(
                                reason = "Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.",
                                retryAfterSeconds = orphanTxResult.maxRetryAfterSeconds,
                            )
                        }
                        batchProcessed += orphanTxResult.successCount + orphanTxResult.failedCount

                        val orphanExpResult = offlineCashExpenseSyncService.syncPendingWithoutLocalSessionUnlocked()
                        if (orphanExpResult.hasAuthFailure) {
                            return@withSyncLock SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                        }
                        if (orphanExpResult.hasTransientFailure) {
                            return@withSyncLock SyncResult.Retry(
                                reason = "Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.",
                                retryAfterSeconds = orphanExpResult.maxRetryAfterSeconds,
                            )
                        }
                        batchProcessed += orphanExpResult.successCount + orphanExpResult.failedCount
                    }

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
        const val MAX_SESSION_SCAN_LIMIT = 50
        const val CASH_SESSION_CLOSED = "CLOSED"
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
