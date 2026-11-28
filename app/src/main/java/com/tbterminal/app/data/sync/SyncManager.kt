package com.tbterminal.app.data.sync

import androidx.room.withTransaction
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalAuditLogEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.id.LocalIdGenerator
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.session.SessionManager
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class SyncRunOutcome {
    SUCCESS,
    RETRY,
    AUTH_REQUIRED,
    SKIPPED,
    FAILED,
}

data class SyncRunSummary(
    val runId: Long,
    val ownerUserId: String?,
    val includedFailed: Boolean,
    val syncedCount: Int,
    val failedCount: Int,
    val outcome: SyncRunOutcome,
    val reason: String? = null,
    val completedAtMillis: Long,
)

class SyncManager(
    private val database: TbTerminalDatabase,
    private val networkMonitor: NetworkConnectivity,
    private val backendHealthMonitor: BackendHealthMonitor,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource,
    private val sessionManager: SessionManager,
    private val offlineCashSessionSyncService: OfflineCashSessionSyncService,
    private val offlineCheckoutSyncService: OfflineCheckoutSyncService,
    private val offlineCashExpenseSyncService: OfflineCashExpenseSyncService,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val trustedOfflineClock: TrustedOfflineClock? = null,
    private val deviceId: String? = null,
) {
    private val runCounter = AtomicLong(0L)
    private val _lastRunSummary = MutableStateFlow<SyncRunSummary?>(null)
    val lastRunSummary: StateFlow<SyncRunSummary?> = _lastRunSummary.asStateFlow()

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

    suspend fun runOnce(includeFailed: Boolean = false): SyncResult {
        if (localAppSettingsDataSource.getDataMode() != AppDataMode.SYNC_SERVER) {
            return recordSummary(
                ownerUserId = sessionManager.resolveOwnerUserId(),
                includedFailed = includeFailed,
                syncedCount = 0,
                failedCount = 0,
                outcome = SyncRunOutcome.SKIPPED,
                reason = "Mode sinkronisasi server belum aktif.",
            ) { SyncResult.Skipped("Mode sinkronisasi server belum aktif.") }
        }

        if (!sessionManager.hasAccessToken()) {
            return recordSummary(
                ownerUserId = null,
                includedFailed = includeFailed,
                syncedCount = 0,
                failedCount = 0,
                outcome = SyncRunOutcome.AUTH_REQUIRED,
                reason = "Login ulang sebelum sinkronisasi.",
            ) { SyncResult.AuthRequired("Login ulang sebelum sinkronisasi.") }
        }

        val activeUserId = sessionManager.resolveOwnerUserId()
            ?: return recordSummary(
                ownerUserId = null,
                includedFailed = includeFailed,
                syncedCount = 0,
                failedCount = 0,
                outcome = SyncRunOutcome.AUTH_REQUIRED,
                reason = "Login ulang sebelum sinkronisasi.",
            ) { SyncResult.AuthRequired("Login ulang sebelum sinkronisasi.") }

        if (!networkMonitor.isOnline()) {
            return recordSummary(
                ownerUserId = activeUserId,
                includedFailed = includeFailed,
                syncedCount = 0,
                failedCount = 0,
                outcome = SyncRunOutcome.RETRY,
                reason = "Perangkat sedang offline.",
            ) { SyncResult.Retry("Perangkat sedang offline.") }
        }

        val needsClockCalibration = trustedOfflineClock != null && !trustedOfflineClock.hasValidAnchor()
        val forceHealthRefresh = backendHealthMonitor.status.value == BackendStatus.UNREACHABLE || needsClockCalibration
        if (backendHealthMonitor.refresh(force = forceHealthRefresh) != BackendStatus.CONNECTED) {
            return recordSummary(
                ownerUserId = activeUserId,
                includedFailed = includeFailed,
                syncedCount = 0,
                failedCount = 0,
                outcome = SyncRunOutcome.RETRY,
                reason = "Server belum tersambung.",
            ) { SyncResult.Retry("Server belum tersambung.") }
        }

        var totalSucceeded = 0
        var totalFailed = 0
        return runSuspendCatching {
            OfflineSyncCoordinator.withSyncLock {
                // No other sync can run while withSyncLock is held; safely return any orphaned
                // SYNCING items (e.g. from process kill/crash) back to PENDING before scanning.
                offlineCashSessionSyncService.recoverStaleSyncingUnlocked(recoverAll = true)
                offlineCheckoutSyncService.recoverStaleSyncingUnlocked(recoverAll = true)
                offlineCashExpenseSyncService.recoverStaleSyncingUnlocked(recoverAll = true)
                recoverEligibleLegacyTimeConflictsUnlocked(activeUserId)

                var totalProcessed = 0
                var completedAllBatches = false
                var minDeferredRetryAfterSeconds: Long? = null
                val deferredSessionIds = mutableSetOf<Long>()
                val failedTxIds = mutableSetOf<Long>()
                val failedExpenseIds = mutableSetOf<Long>()
                val failedCandidateStatus = if (includeFailed) SyncStatus.FAILED else SyncStatus.PENDING

                for (batch in 0 until MAX_BATCHES_PER_RUN) {
                    var batchProcessed = 0
                    val candidateSessions = database.cashSessionDao().getSessionsNeedingSync(
                        closedStatus = CASH_SESSION_CLOSED,
                        pending = SyncStatus.PENDING,
                        failed = failedCandidateStatus,
                        limit = MAX_SESSION_SCAN_LIMIT,
                        cashierUserId = activeUserId,
                    ).filterNot { it.localId in deferredSessionIds }

                    for (session in candidateSessions) {
                        var currentSession = session
                        var sessionProcessed = 0
                        var sessionDeferred = false

                        if (currentSession.serverId.isNullOrBlank()) {
                            when (val openResult = offlineCashSessionSyncService.syncOpenSessionUnlocked(currentSession.localId)) {
                                is OfflineCashSessionSyncResult.Success -> {
                                    sessionProcessed += 1
                                    totalSucceeded += 1
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
                                            return@withSyncLock recordSummary(
                                                ownerUserId = activeUserId,
                                                includedFailed = includeFailed,
                                                syncedCount = totalSucceeded,
                                                failedCount = totalFailed,
                                                outcome = SyncRunOutcome.AUTH_REQUIRED,
                                                reason = "Sesi login tidak valid. Login ulang sebelum sinkronisasi.",
                                            ) { SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.") }
                                        OfflineCheckoutSyncFailureKind.TRANSIENT ->
                                            return@withSyncLock recordSummary(
                                                ownerUserId = activeUserId,
                                                includedFailed = includeFailed,
                                                syncedCount = totalSucceeded,
                                                failedCount = totalFailed,
                                                outcome = SyncRunOutcome.RETRY,
                                                reason = "Koneksi server belum stabil. Sinkronisasi sesi kas akan dicoba lagi.",
                                            ) {
                                                SyncResult.Retry(
                                                    reason = "Koneksi server belum stabil. Sinkronisasi sesi kas akan dicoba lagi.",
                                                    retryAfterSeconds = openResult.retryAfterSeconds,
                                                    successCount = totalSucceeded,
                                                    failedCount = totalFailed,
                                                )
                                            }
                                        else -> {
                                            sessionProcessed += 1
                                            totalFailed += 1
                                            deferredSessionIds += currentSession.localId
                                        }
                                    }
                                }
                            }
                        }

                        if (!sessionDeferred && !currentSession.serverId.isNullOrBlank()) {
                            val txResult = offlineCheckoutSyncService.syncPendingForSessionUnlocked(
                                cashSessionLocalId = currentSession.localId,
                                includeFailed = includeFailed,
                                excludeLocalIds = failedTxIds,
                                cashierUserId = activeUserId,
                            )
                            failedTxIds += txResult.failedLocalIds + txResult.deferredLocalIds
                            minDeferredRetryAfterSeconds = minNullableSeconds(
                                minDeferredRetryAfterSeconds,
                                txResult.minDeferredRetryAfterSeconds,
                            )
                            totalSucceeded += txResult.successCount
                            totalFailed += txResult.failedCount
                            if (txResult.hasAuthFailure) {
                                return@withSyncLock recordSummary(
                                    ownerUserId = activeUserId,
                                    includedFailed = includeFailed,
                                    syncedCount = totalSucceeded,
                                    failedCount = totalFailed,
                                    outcome = SyncRunOutcome.AUTH_REQUIRED,
                                    reason = "Sesi login tidak valid. Login ulang sebelum sinkronisasi.",
                                ) { SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.") }
                            }
                            if (txResult.hasTransientFailure) {
                                return@withSyncLock recordSummary(
                                    ownerUserId = activeUserId,
                                    includedFailed = includeFailed,
                                    syncedCount = totalSucceeded,
                                    failedCount = totalFailed,
                                    outcome = SyncRunOutcome.RETRY,
                                    reason = "Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.",
                                ) {
                                    SyncResult.Retry(
                                        reason = "Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.",
                                        retryAfterSeconds = txResult.maxRetryAfterSeconds,
                                        successCount = totalSucceeded,
                                        failedCount = totalFailed,
                                    )
                                }
                            }
                            sessionProcessed += txResult.successCount + txResult.failedCount

                            val expResult = offlineCashExpenseSyncService.syncPendingExpensesForSessionUnlocked(
                                cashSessionLocalId = currentSession.localId,
                                includeFailed = includeFailed,
                                excludeLocalIds = failedExpenseIds,
                                cashierUserId = activeUserId,
                            )
                            failedExpenseIds += expResult.failedLocalIds + expResult.deferredLocalIds
                            minDeferredRetryAfterSeconds = minNullableSeconds(
                                minDeferredRetryAfterSeconds,
                                expResult.minDeferredRetryAfterSeconds,
                            )
                            totalSucceeded += expResult.successCount
                            totalFailed += expResult.failedCount
                            if (expResult.hasAuthFailure) {
                                return@withSyncLock recordSummary(
                                    ownerUserId = activeUserId,
                                    includedFailed = includeFailed,
                                    syncedCount = totalSucceeded,
                                    failedCount = totalFailed,
                                    outcome = SyncRunOutcome.AUTH_REQUIRED,
                                    reason = "Sesi login tidak valid. Login ulang sebelum sinkronisasi.",
                                ) { SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.") }
                            }
                            if (expResult.hasTransientFailure) {
                                return@withSyncLock recordSummary(
                                    ownerUserId = activeUserId,
                                    includedFailed = includeFailed,
                                    syncedCount = totalSucceeded,
                                    failedCount = totalFailed,
                                    outcome = SyncRunOutcome.RETRY,
                                    reason = "Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.",
                                ) {
                                    SyncResult.Retry(
                                        reason = "Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.",
                                        retryAfterSeconds = expResult.maxRetryAfterSeconds,
                                        successCount = totalSucceeded,
                                        failedCount = totalFailed,
                                    )
                                }
                            }
                            sessionProcessed += expResult.successCount + expResult.failedCount

                            val refreshedSession = database.cashSessionDao().getByLocalId(currentSession.localId)
                            val closeReadyStatus = refreshedSession != null &&
                                refreshedSession.status.equals(CASH_SESSION_CLOSED, ignoreCase = true) &&
                                (refreshedSession.syncStatus == SyncStatus.PENDING ||
                                    (includeFailed && refreshedSession.syncStatus == SyncStatus.FAILED))
                            if (closeReadyStatus) {
                                val targetCloseSession = refreshedSession!!
                                when (val closeResult = offlineCashSessionSyncService.syncCloseSessionIfReadyUnlocked(refreshedSession.localId)) {
                                    is OfflineCashSessionSyncResult.Success -> {
                                        sessionProcessed += 1
                                        totalSucceeded += 1
                                        deferredSessionIds.clear()
                                    }
                                    is OfflineCashSessionSyncResult.Deferred,
                                    is OfflineCashSessionSyncResult.Skipped -> Unit
                                    is OfflineCashSessionSyncResult.Failed -> {
                                        when (closeResult.kind) {
                                            OfflineCheckoutSyncFailureKind.AUTH ->
                                                return@withSyncLock recordSummary(
                                                    ownerUserId = activeUserId,
                                                    includedFailed = includeFailed,
                                                    syncedCount = totalSucceeded,
                                                    failedCount = totalFailed,
                                                    outcome = SyncRunOutcome.AUTH_REQUIRED,
                                                    reason = "Sesi login tidak valid. Login ulang sebelum sinkronisasi.",
                                                ) { SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.") }
                                            OfflineCheckoutSyncFailureKind.TRANSIENT ->
                                                return@withSyncLock recordSummary(
                                                    ownerUserId = activeUserId,
                                                    includedFailed = includeFailed,
                                                    syncedCount = totalSucceeded,
                                                    failedCount = totalFailed,
                                                    outcome = SyncRunOutcome.RETRY,
                                                    reason = "Koneksi server belum stabil. Sinkronisasi tutup sesi kas akan dicoba lagi.",
                                                ) {
                                                    SyncResult.Retry(
                                                        reason = "Koneksi server belum stabil. Sinkronisasi tutup sesi kas akan dicoba lagi.",
                                                        retryAfterSeconds = closeResult.retryAfterSeconds,
                                                        successCount = totalSucceeded,
                                                        failedCount = totalFailed,
                                                    )
                                                }
                                            else -> {
                                                sessionProcessed += 1
                                                totalFailed += 1
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
                        val orphanTxResult = offlineCheckoutSyncService.syncPendingWithoutLocalSessionUnlocked(
                            includeFailed = includeFailed,
                            excludeLocalIds = failedTxIds,
                            cashierUserId = activeUserId,
                        )
                        failedTxIds += orphanTxResult.failedLocalIds + orphanTxResult.deferredLocalIds
                        minDeferredRetryAfterSeconds = minNullableSeconds(
                            minDeferredRetryAfterSeconds,
                            orphanTxResult.minDeferredRetryAfterSeconds,
                        )
                        totalSucceeded += orphanTxResult.successCount
                        totalFailed += orphanTxResult.failedCount
                        if (orphanTxResult.hasAuthFailure) {
                            return@withSyncLock recordSummary(
                                ownerUserId = activeUserId,
                                includedFailed = includeFailed,
                                syncedCount = totalSucceeded,
                                failedCount = totalFailed,
                                outcome = SyncRunOutcome.AUTH_REQUIRED,
                                reason = "Sesi login tidak valid. Login ulang sebelum sinkronisasi.",
                            ) { SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.") }
                        }
                        if (orphanTxResult.hasTransientFailure) {
                            return@withSyncLock recordSummary(
                                ownerUserId = activeUserId,
                                includedFailed = includeFailed,
                                syncedCount = totalSucceeded,
                                failedCount = totalFailed,
                                outcome = SyncRunOutcome.RETRY,
                                reason = "Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.",
                            ) {
                                SyncResult.Retry(
                                    reason = "Koneksi server belum stabil. Sinkronisasi akan dicoba lagi.",
                                    retryAfterSeconds = orphanTxResult.maxRetryAfterSeconds,
                                    successCount = totalSucceeded,
                                    failedCount = totalFailed,
                                )
                            }
                        }
                        batchProcessed += orphanTxResult.successCount + orphanTxResult.failedCount

                        val orphanExpResult = offlineCashExpenseSyncService.syncPendingWithoutLocalSessionUnlocked(
                            includeFailed = includeFailed,
                            excludeLocalIds = failedExpenseIds,
                            cashierUserId = activeUserId,
                        )
                        failedExpenseIds += orphanExpResult.failedLocalIds + orphanExpResult.deferredLocalIds
                        minDeferredRetryAfterSeconds = minNullableSeconds(
                            minDeferredRetryAfterSeconds,
                            orphanExpResult.minDeferredRetryAfterSeconds,
                        )
                        totalSucceeded += orphanExpResult.successCount
                        totalFailed += orphanExpResult.failedCount
                        if (orphanExpResult.hasAuthFailure) {
                            return@withSyncLock recordSummary(
                                ownerUserId = activeUserId,
                                includedFailed = includeFailed,
                                syncedCount = totalSucceeded,
                                failedCount = totalFailed,
                                outcome = SyncRunOutcome.AUTH_REQUIRED,
                                reason = "Sesi login tidak valid. Login ulang sebelum sinkronisasi.",
                            ) { SyncResult.AuthRequired("Sesi login tidak valid. Login ulang sebelum sinkronisasi.") }
                        }
                        if (orphanExpResult.hasTransientFailure) {
                            return@withSyncLock recordSummary(
                                ownerUserId = activeUserId,
                                includedFailed = includeFailed,
                                syncedCount = totalSucceeded,
                                failedCount = totalFailed,
                                outcome = SyncRunOutcome.RETRY,
                                reason = "Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.",
                            ) {
                                SyncResult.Retry(
                                    reason = "Koneksi server belum stabil. Sinkronisasi pengeluaran kas akan dicoba lagi.",
                                    retryAfterSeconds = orphanExpResult.maxRetryAfterSeconds,
                                    successCount = totalSucceeded,
                                    failedCount = totalFailed,
                                )
                            }
                        }
                        batchProcessed += orphanExpResult.successCount + orphanExpResult.failedCount
                    }

                    totalProcessed += batchProcessed

                    if (batchProcessed == 0) {
                        completedAllBatches = true
                        break
                    }
                }
                val remainingUnsynced = countRemainingUnsyncedWork(activeUserId)
                if (totalSucceeded > 0 || remainingUnsynced == 0) {
                    localAppSettingsDataSource.setLastSuccessfulSyncAt(activeUserId, clockMillis())
                }
                val hasMorePending = !completedAllBatches && hasRemainingPendingWork(activeUserId)
                recordSummary(
                    ownerUserId = activeUserId,
                    includedFailed = includeFailed,
                    syncedCount = totalSucceeded,
                    failedCount = totalFailed,
                    outcome = SyncRunOutcome.SUCCESS,
                    reason = null,
                ) {
                    SyncResult.Success(
                        processedCount = totalProcessed,
                        hasMorePending = hasMorePending,
                        successCount = totalSucceeded,
                        failedCount = totalFailed,
                        deferredRetryAfterSeconds = minDeferredRetryAfterSeconds,
                    )
                }
            }
        }.getOrElse { error ->
            recordSummary(
                ownerUserId = activeUserId,
                includedFailed = includeFailed,
                syncedCount = totalSucceeded,
                failedCount = totalFailed,
                outcome = SyncRunOutcome.FAILED,
                reason = error.message,
            ) { SyncResult.Failed(error) }
        }
    }

    private inline fun <T : SyncResult> recordSummary(
        ownerUserId: String?,
        includedFailed: Boolean,
        syncedCount: Int,
        failedCount: Int,
        outcome: SyncRunOutcome,
        reason: String?,
        buildResult: () -> T,
    ): T {
        _lastRunSummary.value = SyncRunSummary(
            runId = runCounter.incrementAndGet(),
            ownerUserId = ownerUserId,
            includedFailed = includedFailed,
            syncedCount = syncedCount,
            failedCount = failedCount,
            outcome = outcome,
            reason = reason,
            completedAtMillis = clockMillis(),
        )
        return buildResult()
    }

    private suspend fun countRemainingUnsyncedWork(cashierUserId: String): Int {
        return database.cashSessionDao().countAllUnsyncedForUser(cashierUserId = cashierUserId) +
            database.transactionDao().countAllUnsyncedForUser(cashierUserId = cashierUserId) +
            database.cashExpenseDao().countAllUnsyncedForUser(cashierUserId = cashierUserId)
    }

    private suspend fun recoverEligibleLegacyTimeConflictsUnlocked(activeUserId: String): Int {
        val trustedNow = trustedOfflineClock?.readValidNowMillis() ?: return 0
        val candidates = database.syncQueueDao().getByStatusAndErrorCode(
            status = SyncStatus.CONFLICT,
            errorCode = OfflineTimeConflictPolicy.CODE_OFFLINE_OCCURRED_AT_INVALID,
            limit = MAX_LEGACY_RECOVERY_SCAN_LIMIT,
        )
        if (candidates.isEmpty()) return 0

        var recoveredCount = 0
        for (queue in candidates) {
            when (queue.entityType) {
                SyncEntityType.CASH_EXPENSE -> {
                    val expense = database.cashExpenseDao().getByLocalId(queue.entityLocalId) ?: continue
                    if (expense.deletedAt != null) continue
                    val sessionLocalId = expense.cashSessionLocalId ?: continue
                    val session = database.cashSessionDao().getByLocalId(sessionLocalId) ?: continue
                    if (session.deletedAt != null) continue
                    val owner = session.cashierUserId.trim().takeIf(String::isNotBlank)
                    if (owner != null && owner != activeUserId) continue
                    if (!OfflineTimeConflictPolicy.isLegacyConflictEligibleForAutoRecovery(
                            errorCode = queue.lastErrorCode,
                            occurredAtMillis = expense.occurredAt,
                            sessionOpenedAtMillis = session.openedAt,
                            sessionServerId = session.serverId,
                            trustedNowMillis = trustedNow,
                        )
                    ) {
                        continue
                    }
                    if (localAppSettingsDataSource.isTimeConflictAlreadyRecovered(
                            entityType = queue.entityType.name,
                            clientGeneratedId = expense.clientGeneratedId,
                        )
                    ) {
                        continue
                    }
                    val recovered = database.withTransaction {
                        if (localAppSettingsDataSource.isTimeConflictAlreadyRecovered(
                                entityType = queue.entityType.name,
                                clientGeneratedId = expense.clientGeneratedId,
                            )
                        ) {
                            return@withTransaction false
                        }
                        database.cashExpenseDao().updateSyncStatus(
                            localId = expense.localId,
                            status = SyncStatus.PENDING,
                            syncedAt = null,
                            updatedAt = trustedNow,
                        )
                        database.syncQueueDao().updateStatus(
                            id = queue.id,
                            status = SyncStatus.PENDING,
                            lastError = null,
                            lastErrorCode = null,
                            retryIncrement = 0,
                            updatedAt = trustedNow,
                        )
                        localAppSettingsDataSource.markTimeConflictRecovered(
                            entityType = queue.entityType.name,
                            clientGeneratedId = expense.clientGeneratedId,
                            recoveredAtMillis = trustedNow,
                        )
                        database.localAuditLogDao().upsert(
                            LocalAuditLogEntity(
                                clientGeneratedId = LocalIdGenerator.clientGeneratedId("AUDIT"),
                                deviceId = expense.deviceId ?: deviceId,
                                actorUserId = activeUserId,
                                action = ACTION_AUTO_RECOVER_TIME_CONFLICT,
                                tableName = "sales.cash_expenses",
                                recordId = expense.clientGeneratedId,
                                description = "Pemulihan otomatis konflik waktu pengeluaran kas ke PENDING",
                                metadataJson = buildJsonObject {
                                    put("entityType", queue.entityType.name)
                                    put("entityLocalId", expense.localId)
                                    put("queueId", queue.id)
                                    put("clientGeneratedId", expense.clientGeneratedId)
                                    put("occurredAt", expense.occurredAt)
                                    put("sessionOpenedAt", session.openedAt)
                                    put("sessionServerId", session.serverId.orEmpty())
                                    put("trustedNow", trustedNow)
                                    put("previousErrorCode", queue.lastErrorCode.orEmpty())
                                }.toString(),
                                occurredAt = trustedNow,
                                syncStatus = SyncStatus.PENDING,
                                createdAt = trustedNow,
                                updatedAt = trustedNow,
                            )
                        )
                        true
                    }
                    if (recovered) recoveredCount += 1
                }

                SyncEntityType.TRANSACTION -> {
                    val tx = database.transactionDao().getByLocalId(queue.entityLocalId) ?: continue
                    if (tx.deletedAt != null) continue
                    val sessionLocalId = tx.cashSessionLocalId ?: continue
                    val session = database.cashSessionDao().getByLocalId(sessionLocalId) ?: continue
                    if (session.deletedAt != null) continue
                    val owner = (tx.cashierUserId ?: session.cashierUserId).trim().takeIf(String::isNotBlank)
                    if (owner != null && owner != activeUserId) continue
                    if (!OfflineTimeConflictPolicy.isLegacyConflictEligibleForAutoRecovery(
                            errorCode = queue.lastErrorCode,
                            occurredAtMillis = tx.occurredAt,
                            sessionOpenedAtMillis = session.openedAt,
                            sessionServerId = session.serverId,
                            trustedNowMillis = trustedNow,
                        )
                    ) {
                        continue
                    }
                    if (localAppSettingsDataSource.isTimeConflictAlreadyRecovered(
                            entityType = queue.entityType.name,
                            clientGeneratedId = tx.clientGeneratedId,
                        )
                    ) {
                        continue
                    }
                    val recovered = database.withTransaction {
                        if (localAppSettingsDataSource.isTimeConflictAlreadyRecovered(
                                entityType = queue.entityType.name,
                                clientGeneratedId = tx.clientGeneratedId,
                            )
                        ) {
                            return@withTransaction false
                        }
                        database.transactionDao().updateSyncStatus(
                            localId = tx.localId,
                            status = SyncStatus.PENDING,
                            syncedAt = null,
                            updatedAt = trustedNow,
                        )
                        database.transactionItemDao().markTransactionItemsFailed(
                            transactionLocalId = tx.localId,
                            status = SyncStatus.PENDING,
                            syncedStatus = SyncStatus.SYNCED,
                            updatedAt = trustedNow,
                        )
                        database.paymentDao().markPaymentsFailed(
                            transactionLocalId = tx.localId,
                            status = SyncStatus.PENDING,
                            syncedStatus = SyncStatus.SYNCED,
                            updatedAt = trustedNow,
                        )
                        database.receivableDao().markReceivablesFailed(
                            transactionLocalId = tx.localId,
                            syncStatus = SyncStatus.PENDING,
                            syncedStatus = SyncStatus.SYNCED,
                            updatedAt = trustedNow,
                        )
                        database.syncQueueDao().updateStatus(
                            id = queue.id,
                            status = SyncStatus.PENDING,
                            lastError = null,
                            lastErrorCode = null,
                            retryIncrement = 0,
                            updatedAt = trustedNow,
                        )
                        localAppSettingsDataSource.markTimeConflictRecovered(
                            entityType = queue.entityType.name,
                            clientGeneratedId = tx.clientGeneratedId,
                            recoveredAtMillis = trustedNow,
                        )
                        database.localAuditLogDao().upsert(
                            LocalAuditLogEntity(
                                clientGeneratedId = LocalIdGenerator.clientGeneratedId("AUDIT"),
                                deviceId = tx.deviceId ?: deviceId,
                                actorUserId = activeUserId,
                                action = ACTION_AUTO_RECOVER_TIME_CONFLICT,
                                tableName = "sales.transactions",
                                recordId = tx.clientGeneratedId,
                                description = "Pemulihan otomatis konflik waktu transaksi ke PENDING",
                                metadataJson = buildJsonObject {
                                    put("entityType", queue.entityType.name)
                                    put("entityLocalId", tx.localId)
                                    put("queueId", queue.id)
                                    put("clientGeneratedId", tx.clientGeneratedId)
                                    put("occurredAt", tx.occurredAt)
                                    put("sessionOpenedAt", session.openedAt)
                                    put("sessionServerId", session.serverId.orEmpty())
                                    put("trustedNow", trustedNow)
                                    put("previousErrorCode", queue.lastErrorCode.orEmpty())
                                }.toString(),
                                occurredAt = trustedNow,
                                syncStatus = SyncStatus.PENDING,
                                createdAt = trustedNow,
                                updatedAt = trustedNow,
                            )
                        )
                        true
                    }
                    if (recovered) recoveredCount += 1
                }

                else -> Unit
            }
        }
        return recoveredCount
    }

    private suspend fun hasRemainingPendingWork(cashierUserId: String?): Boolean {
        return database.cashSessionDao().countPendingOpenSessions(cashierUserId = cashierUserId) > 0 ||
            database.transactionDao().countPendingTransactions(cashierUserId = cashierUserId) > 0 ||
            database.cashExpenseDao().countPendingExpenses(cashierUserId = cashierUserId) > 0 ||
            database.cashSessionDao().countActionablePendingCloseSessions(cashierUserId = cashierUserId) > 0
    }

    private companion object {
        const val MAX_BATCHES_PER_RUN = 10
        const val MAX_SESSION_SCAN_LIMIT = 50
        const val MAX_LEGACY_RECOVERY_SCAN_LIMIT = 100
        const val CASH_SESSION_CLOSED = "CLOSED"
        const val ACTION_AUTO_RECOVER_TIME_CONFLICT = "AUTO_RECOVER_TIME_CONFLICT"
    }
}

sealed interface SyncResult {
    data class Success(
        val processedCount: Int,
        val hasMorePending: Boolean = false,
        val successCount: Int = processedCount,
        val failedCount: Int = 0,
        val deferredRetryAfterSeconds: Long? = null,
    ) : SyncResult
    data class Skipped(val reason: String) : SyncResult
    data class Retry(
        val reason: String,
        val retryAfterSeconds: Long? = null,
        val successCount: Int = 0,
        val failedCount: Int = 0,
    ) : SyncResult
    data class AuthRequired(val reason: String) : SyncResult
    data class Failed(val error: Throwable) : SyncResult
}
