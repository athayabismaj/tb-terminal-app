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
    private val networkMonitor: NetworkMonitor,
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

        return runCatching {
            val sessionResult = offlineCashSessionSyncService.syncPendingOpenSessions()
            if (sessionResult.hasAuthFailure) {
                return SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
            }
            val result = offlineCheckoutSyncService.syncPendingOnly()
            val expenseResult = offlineCashExpenseSyncService.syncPendingExpenses()
            val closeSessionResult = offlineCashSessionSyncService.syncPendingCloseSessions()
            when {
                result.hasAuthFailure || expenseResult.hasAuthFailure || closeSessionResult.hasAuthFailure -> SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.")
                result.hasTransientFailure -> SyncResult.Retry("Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.")
                sessionResult.hasTransientFailure -> SyncResult.Retry("Koneksi server belum stabil. Sinkronisasi sesi kas akan dicoba lagi.")
                expenseResult.hasTransientFailure -> SyncResult.Retry("Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.")
                closeSessionResult.hasTransientFailure -> SyncResult.Retry("Koneksi server belum stabil. Sinkronisasi tutup sesi kas akan dicoba lagi.")
                else -> SyncResult.Success(
                    processedCount = sessionResult.successCount +
                        sessionResult.failedCount +
                        result.successCount +
                        result.failedCount +
                        expenseResult.successCount +
                        expenseResult.failedCount +
                        closeSessionResult.successCount +
                        closeSessionResult.failedCount
                )
            }
        }.getOrElse { error ->
            SyncResult.Failed(error)
        }
    }
}

sealed interface SyncResult {
    data class Success(val processedCount: Int) : SyncResult
    data class Skipped(val reason: String) : SyncResult
    data class Retry(val reason: String) : SyncResult
    data class AuthRequired(val reason: String) : SyncResult
    data class Failed(val error: Throwable) : SyncResult
}
