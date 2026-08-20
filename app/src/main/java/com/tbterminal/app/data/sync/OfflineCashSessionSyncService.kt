package com.tbterminal.app.data.sync

import androidx.room.withTransaction
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.OfflineCashSessionCloseSyncRequestDto
import com.tbterminal.app.data.remote.OfflineCashSessionOpenSyncRequestDto
import com.tbterminal.app.data.remote.SalesApi
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

class OfflineCashSessionSyncService(
    private val database: TbTerminalDatabase,
    private val salesApi: SalesApi,
    private val deviceId: String
) {
    suspend fun syncOpenSession(localSessionId: Long): OfflineCashSessionSyncResult {
        val session = database.cashSessionDao().getByLocalId(localSessionId)
            ?: return OfflineCashSessionSyncResult.Failed("Sesi kas lokal tidak ditemukan.")

        session.serverId?.takeIf { it.isNotBlank() }?.let { serverId ->
            updateTransactionsCashSessionServerId(session.localId, serverId)
            return OfflineCashSessionSyncResult.Success(serverId, "LOCAL_ALREADY_SYNCED")
        }

        if (!markOpenSyncingIfAllowed(localSessionId)) {
            return OfflineCashSessionSyncResult.Failed("Sesi kas sedang disinkronkan atau sudah selesai.")
        }

        return runCatching {
            val lockedSession = database.cashSessionDao().getByLocalId(localSessionId)
                ?: return@runCatching fail(localSessionId, "Sesi kas lokal tidak ditemukan.", 1)
            val request = when (val buildResult = buildRequest(lockedSession)) {
                is BuildOpenSessionRequestResult.Success -> buildResult.request
                is BuildOpenSessionRequestResult.Error -> {
                    return@runCatching fail(
                        localSessionId = localSessionId,
                        message = buildResult.message,
                        retryIncrement = 1,
                        kind = SyncErrorClassifier.classifyMessage(buildResult.message).toFailureKind()
                    )
                }
            }

            val response = salesApi.syncOpenCashSession(request)
            if (!response.isSuccessful) {
                val message = when (response.code()) {
                    401, 403 -> "Sesi login tidak valid. Login ulang sebelum sinkronisasi sesi kas."
                    in 400..499 -> response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Server menolak sesi kas offline (${response.code()})"
                    else -> "Gagal terhubung ke server (${response.code()})"
                }
                return@runCatching fail(
                    localSessionId = localSessionId,
                    message = message,
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classify(
                        httpCode = response.code(),
                        message = message
                    ).toFailureKind()
                )
            }

            val body = response.body()
            val syncResponse = body?.data
            if (body?.success != true || syncResponse == null) {
                return@runCatching fail(
                    localSessionId = localSessionId,
                    message = body?.message ?: body?.error ?: "Response sync sesi kas tidak valid",
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classifyMessage(body?.message ?: body?.error).toFailureKind()
                )
            }

            val acceptedStatus = syncResponse.syncStatus.uppercase()
            if (acceptedStatus != "CREATED" && acceptedStatus != "DUPLICATE") {
                return@runCatching fail(
                    localSessionId = localSessionId,
                    message = "Status sync sesi kas tidak dikenali: ${syncResponse.syncStatus}",
                    retryIncrement = 1,
                    kind = OfflineCheckoutSyncFailureKind.VALIDATION
                )
            }

            val serverId = syncResponse.serverCashSessionId.takeIf { it.isNotBlank() }
                ?: return@runCatching fail(localSessionId, "Server ID sesi kas tidak tersedia.", 1)
            markOpenSynced(localSessionId, serverId)
            OfflineCashSessionSyncResult.Success(serverId, syncResponse.syncStatus)
        }.getOrElse { error ->
            fail(
                localSessionId = localSessionId,
                message = error.message ?: "Sinkronisasi sesi kas offline gagal",
                retryIncrement = 1,
                kind = SyncErrorClassifier.classify(
                    message = error.message,
                    throwable = error
                ).toFailureKind()
            )
        }
    }

    suspend fun syncPendingOpenSessions(
        limit: Int = DEFAULT_AUTO_SYNC_LIMIT,
        includeFailed: Boolean = false
    ): OfflineCashSessionBulkSyncResult {
        val failedStatus = if (includeFailed) SyncStatus.FAILED else SyncStatus.PENDING
        val sessions = database.cashSessionDao().getOpenSyncCandidates(
            pending = SyncStatus.PENDING,
            failed = failedStatus,
            limit = limit
        )
        if (sessions.isEmpty()) {
            return OfflineCashSessionBulkSyncResult(total = 0, successCount = 0, failedCount = 0)
        }

        var successCount = 0
        var failedCount = 0
        var authFailureCount = 0
        var transientFailureCount = 0
        var validationFailureCount = 0
        sessions.forEach { session ->
            when (val result = syncOpenSession(session.localId)) {
                is OfflineCashSessionSyncResult.Success -> successCount += 1
                is OfflineCashSessionSyncResult.Failed -> {
                    failedCount += 1
                    when (result.kind) {
                        OfflineCheckoutSyncFailureKind.AUTH -> authFailureCount += 1
                        OfflineCheckoutSyncFailureKind.TRANSIENT -> transientFailureCount += 1
                        OfflineCheckoutSyncFailureKind.VALIDATION -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.CONFLICT -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.UNKNOWN -> validationFailureCount += 1
                    }
                    if (result.kind == OfflineCheckoutSyncFailureKind.AUTH) {
                        return OfflineCashSessionBulkSyncResult(
                            total = sessions.size,
                            successCount = successCount,
                            failedCount = failedCount,
                            authFailureCount = authFailureCount,
                            transientFailureCount = transientFailureCount,
                            validationFailureCount = validationFailureCount
                        )
                    }
                }
                is OfflineCashSessionSyncResult.Skipped -> Unit
            }
        }

        return OfflineCashSessionBulkSyncResult(
            total = sessions.size,
            successCount = successCount,
            failedCount = failedCount,
            authFailureCount = authFailureCount,
            transientFailureCount = transientFailureCount,
            validationFailureCount = validationFailureCount
        )
    }

    suspend fun syncCloseSessionIfReady(localSessionId: Long): OfflineCashSessionSyncResult {
        var session = database.cashSessionDao().getByLocalId(localSessionId)
            ?: return OfflineCashSessionSyncResult.Failed("Sesi kas lokal tidak ditemukan.")
        if (!session.status.equals(CASH_SESSION_CLOSED, ignoreCase = true)) {
            return OfflineCashSessionSyncResult.Skipped("Sesi kas lokal belum ditutup.")
        }

        if (session.serverId.isNullOrBlank()) {
            when (val openResult = syncOpenSession(localSessionId)) {
                is OfflineCashSessionSyncResult.Success -> Unit
                is OfflineCashSessionSyncResult.Failed -> return openResult
                is OfflineCashSessionSyncResult.Skipped -> return openResult
            }
            session = database.cashSessionDao().getByLocalId(localSessionId)
                ?: return OfflineCashSessionSyncResult.Failed("Sesi kas lokal tidak ditemukan setelah sync open.")
        }

        val unsyncedTransactions = database.transactionDao().countUnsyncedByCashSession(
            cashSessionLocalId = localSessionId,
            pending = SyncStatus.PENDING,
            syncing = SyncStatus.SYNCING,
            failed = SyncStatus.FAILED,
            conflict = SyncStatus.CONFLICT
        )
        if (unsyncedTransactions > 0) {
            return OfflineCashSessionSyncResult.Skipped("Sesi kas menunggu $unsyncedTransactions transaksi lokal tersinkron.")
        }
        val unsyncedExpenses = database.cashExpenseDao().countUnsyncedByCashSession(
            cashSessionLocalId = localSessionId,
            pending = SyncStatus.PENDING,
            syncing = SyncStatus.SYNCING,
            failed = SyncStatus.FAILED,
            conflict = SyncStatus.CONFLICT
        )
        if (unsyncedExpenses > 0) {
            return OfflineCashSessionSyncResult.Skipped("Sesi kas menunggu $unsyncedExpenses pengeluaran kas lokal tersinkron.")
        }

        if (!markCloseSyncingIfAllowed(localSessionId)) {
            return OfflineCashSessionSyncResult.Failed("Sesi kas sedang disinkronkan atau sudah selesai.")
        }

        return runCatching {
            val lockedSession = database.cashSessionDao().getByLocalId(localSessionId)
                ?: return@runCatching failClose(localSessionId, "Sesi kas lokal tidak ditemukan.", 1)
            val request = when (val buildResult = buildCloseRequest(lockedSession)) {
                is BuildCloseSessionRequestResult.Success -> buildResult.request
                is BuildCloseSessionRequestResult.Error -> {
                    return@runCatching failClose(
                        localSessionId = localSessionId,
                        message = buildResult.message,
                        retryIncrement = 1,
                        kind = SyncErrorClassifier.classifyMessage(buildResult.message).toFailureKind()
                    )
                }
            }

            val response = salesApi.syncCloseCashSession(request)
            if (!response.isSuccessful) {
                val message = when (response.code()) {
                    401, 403 -> "Sesi login tidak valid. Login ulang sebelum sinkronisasi tutup sesi kas."
                    in 400..499 -> response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Server menolak tutup sesi kas offline (${response.code()})"
                    else -> "Gagal terhubung ke server (${response.code()})"
                }
                return@runCatching failClose(
                    localSessionId = localSessionId,
                    message = message,
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classify(
                        httpCode = response.code(),
                        message = message
                    ).toFailureKind()
                )
            }

            val body = response.body()
            val syncResponse = body?.data
            if (body?.success != true || syncResponse == null) {
                return@runCatching failClose(
                    localSessionId = localSessionId,
                    message = body?.message ?: body?.error ?: "Response sync tutup sesi kas tidak valid",
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classifyMessage(body?.message ?: body?.error).toFailureKind()
                )
            }

            val acceptedStatus = syncResponse.syncStatus.uppercase()
            if (acceptedStatus != "UPDATED" && acceptedStatus != "DUPLICATE" && acceptedStatus != "ALREADY_CLOSED") {
                return@runCatching failClose(
                    localSessionId = localSessionId,
                    message = "Status sync tutup sesi kas tidak dikenali: ${syncResponse.syncStatus}",
                    retryIncrement = 1,
                    kind = OfflineCheckoutSyncFailureKind.VALIDATION
                )
            }

            markCloseSynced(localSessionId)
            OfflineCashSessionSyncResult.Success(syncResponse.serverCashSessionId, syncResponse.syncStatus)
        }.getOrElse { error ->
            failClose(
                localSessionId = localSessionId,
                message = error.message ?: "Sinkronisasi tutup sesi kas offline gagal",
                retryIncrement = 1,
                kind = SyncErrorClassifier.classify(
                    message = error.message,
                    throwable = error
                ).toFailureKind()
            )
        }
    }

    suspend fun syncPendingCloseSessions(
        limit: Int = DEFAULT_AUTO_SYNC_LIMIT,
        includeFailed: Boolean = false
    ): OfflineCashSessionBulkSyncResult {
        val failedStatus = if (includeFailed) SyncStatus.FAILED else SyncStatus.PENDING
        val sessions = database.cashSessionDao().getCloseSyncCandidates(
            closedStatus = CASH_SESSION_CLOSED,
            pending = SyncStatus.PENDING,
            failed = failedStatus,
            limit = limit
        )
        if (sessions.isEmpty()) {
            return OfflineCashSessionBulkSyncResult(total = 0, successCount = 0, failedCount = 0)
        }

        var successCount = 0
        var failedCount = 0
        var skippedCount = 0
        var authFailureCount = 0
        var transientFailureCount = 0
        var validationFailureCount = 0
        sessions.forEach { session ->
            when (val result = syncCloseSessionIfReady(session.localId)) {
                is OfflineCashSessionSyncResult.Success -> successCount += 1
                is OfflineCashSessionSyncResult.Skipped -> skippedCount += 1
                is OfflineCashSessionSyncResult.Failed -> {
                    failedCount += 1
                    when (result.kind) {
                        OfflineCheckoutSyncFailureKind.AUTH -> authFailureCount += 1
                        OfflineCheckoutSyncFailureKind.TRANSIENT -> transientFailureCount += 1
                        OfflineCheckoutSyncFailureKind.VALIDATION -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.CONFLICT -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.UNKNOWN -> validationFailureCount += 1
                    }
                    if (result.kind == OfflineCheckoutSyncFailureKind.AUTH) {
                        return OfflineCashSessionBulkSyncResult(
                            total = sessions.size,
                            successCount = successCount,
                            failedCount = failedCount,
                            skippedCount = skippedCount,
                            authFailureCount = authFailureCount,
                            transientFailureCount = transientFailureCount,
                            validationFailureCount = validationFailureCount
                        )
                    }
                }
            }
        }

        return OfflineCashSessionBulkSyncResult(
            total = sessions.size,
            successCount = successCount,
            failedCount = failedCount,
            skippedCount = skippedCount,
            authFailureCount = authFailureCount,
            transientFailureCount = transientFailureCount,
            validationFailureCount = validationFailureCount
        )
    }

    private suspend fun markOpenSyncingIfAllowed(localSessionId: Long): Boolean {
        val now = System.currentTimeMillis()
        var locked = false
        database.withTransaction {
            locked = database.cashSessionDao().markOpenSyncingIfPendingOrFailed(
                localId = localSessionId,
                syncing = SyncStatus.SYNCING,
                pending = SyncStatus.PENDING,
                failed = SyncStatus.FAILED,
                conflict = SyncStatus.CONFLICT,
                updatedAt = now
            ) > 0
            if (locked) {
                database.syncQueueDao().getLatestForEntityAndOperation(
                    entityLocalId = localSessionId,
                    entityType = SyncEntityType.CASH_SESSION,
                    operation = SyncOperation.CREATE
                )?.let { queue ->
                    database.syncQueueDao().updateStatus(
                        id = queue.id,
                        status = SyncStatus.SYNCING,
                        lastError = null,
                        retryIncrement = 0,
                        updatedAt = now
                    )
                }
            }
        }
        return locked
    }

    private suspend fun markCloseSyncingIfAllowed(localSessionId: Long): Boolean {
        val now = System.currentTimeMillis()
        var locked = false
        database.withTransaction {
            locked = database.cashSessionDao().markCloseSyncingIfPendingOrFailed(
                localId = localSessionId,
                closedStatus = CASH_SESSION_CLOSED,
                syncing = SyncStatus.SYNCING,
                pending = SyncStatus.PENDING,
                failed = SyncStatus.FAILED,
                conflict = SyncStatus.CONFLICT,
                updatedAt = now
            ) > 0
            if (locked) {
                database.syncQueueDao().getLatestForEntityAndOperation(
                    entityLocalId = localSessionId,
                    entityType = SyncEntityType.CASH_SESSION,
                    operation = SyncOperation.UPDATE
                )?.let { queue ->
                    database.syncQueueDao().updateStatus(
                        id = queue.id,
                        status = SyncStatus.SYNCING,
                        lastError = null,
                        retryIncrement = 0,
                        updatedAt = now
                    )
                }
            }
        }
        return locked
    }

    private suspend fun markOpenSynced(localSessionId: Long, serverId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val currentSession = database.cashSessionDao().getByLocalId(localSessionId)
            val nextSyncStatus = if (currentSession?.status.equals("CLOSED", ignoreCase = true)) {
                SyncStatus.PENDING
            } else {
                SyncStatus.SYNCED
            }
            database.cashSessionDao().markOpenSynced(
                localId = localSessionId,
                serverId = serverId,
                syncStatus = nextSyncStatus,
                syncedAt = now,
                updatedAt = now
            )
            database.transactionDao().updateCashSessionServerIdForLocalSession(
                cashSessionLocalId = localSessionId,
                serverId = serverId,
                updatedAt = now
            )
            database.cashExpenseDao().updateCashSessionServerIdForLocalSession(
                cashSessionLocalId = localSessionId,
                serverId = serverId,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntityAndOperation(
                entityLocalId = localSessionId,
                entityType = SyncEntityType.CASH_SESSION,
                operation = SyncOperation.CREATE
            )?.let { queue ->
                database.syncQueueDao().updateStatus(
                    id = queue.id,
                    status = SyncStatus.SYNCED,
                    lastError = null,
                    retryIncrement = 0,
                    updatedAt = now
                )
            }
        }
    }

    private suspend fun markCloseSynced(localSessionId: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.cashSessionDao().markCloseSynced(
                localId = localSessionId,
                syncStatus = SyncStatus.SYNCED,
                syncedAt = now,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntityAndOperation(
                entityLocalId = localSessionId,
                entityType = SyncEntityType.CASH_SESSION,
                operation = SyncOperation.UPDATE
            )?.let { queue ->
                database.syncQueueDao().updateStatus(
                    id = queue.id,
                    status = SyncStatus.SYNCED,
                    lastError = null,
                    retryIncrement = 0,
                    updatedAt = now
                )
            }
        }
    }

    private suspend fun updateTransactionsCashSessionServerId(localSessionId: Long, serverId: String) {
        val now = System.currentTimeMillis()
        database.transactionDao().updateCashSessionServerIdForLocalSession(
            cashSessionLocalId = localSessionId,
            serverId = serverId,
            updatedAt = now
        )
        database.cashExpenseDao().updateCashSessionServerIdForLocalSession(
            cashSessionLocalId = localSessionId,
            serverId = serverId,
            updatedAt = now
        )
    }

    private suspend fun fail(
        localSessionId: Long,
        message: String,
        retryIncrement: Int,
        kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.VALIDATION
    ): OfflineCashSessionSyncResult.Failed {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val status = if (kind == OfflineCheckoutSyncFailureKind.CONFLICT) {
                SyncStatus.CONFLICT
            } else {
                SyncStatus.FAILED
            }
            database.cashSessionDao().updateSyncStatus(
                localId = localSessionId,
                syncStatus = status,
                syncedAt = null,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntityAndOperation(
                entityLocalId = localSessionId,
                entityType = SyncEntityType.CASH_SESSION,
                operation = SyncOperation.CREATE
            )?.let { queue ->
                database.syncQueueDao().updateStatus(
                    id = queue.id,
                    status = status,
                    lastError = message,
                    retryIncrement = retryIncrement,
                    updatedAt = now
                )
            }
        }
        return OfflineCashSessionSyncResult.Failed(message, kind)
    }

    private suspend fun failClose(
        localSessionId: Long,
        message: String,
        retryIncrement: Int,
        kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.VALIDATION
    ): OfflineCashSessionSyncResult.Failed {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val status = if (kind == OfflineCheckoutSyncFailureKind.CONFLICT) {
                SyncStatus.CONFLICT
            } else {
                SyncStatus.FAILED
            }
            database.cashSessionDao().updateSyncStatus(
                localId = localSessionId,
                syncStatus = status,
                syncedAt = null,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntityAndOperation(
                entityLocalId = localSessionId,
                entityType = SyncEntityType.CASH_SESSION,
                operation = SyncOperation.UPDATE
            )?.let { queue ->
                database.syncQueueDao().updateStatus(
                    id = queue.id,
                    status = status,
                    lastError = message,
                    retryIncrement = retryIncrement,
                    updatedAt = now
                )
            }
        }
        return OfflineCashSessionSyncResult.Failed(message, kind)
    }

    private fun buildRequest(session: LocalCashSessionEntity): BuildOpenSessionRequestResult {
        val resolvedDeviceId = session.deviceId?.takeIf { it.isNotBlank() } ?: deviceId.takeIf { it.isNotBlank() }
        if (session.clientGeneratedId.isBlank()) {
            return BuildOpenSessionRequestResult.Error("Client ID sesi kas lokal belum tersedia.")
        }
        if (resolvedDeviceId == null) {
            return BuildOpenSessionRequestResult.Error("Device ID belum tersedia. Sesi kas belum bisa disinkronkan.")
        }
        if (session.cashierUserId.isBlank()) {
            return BuildOpenSessionRequestResult.Error("Kasir sesi kas lokal belum tersedia.")
        }
        if (session.startingCash < 0.0) {
            return BuildOpenSessionRequestResult.Error("Modal awal sesi kas tidak boleh negatif.")
        }

        return BuildOpenSessionRequestResult.Success(
            OfflineCashSessionOpenSyncRequestDto(
                clientGeneratedId = session.clientGeneratedId,
                deviceId = resolvedDeviceId,
                cashierUserId = session.cashierUserId,
                openedAt = session.openedAt.toOffsetDateTimeString(),
                startingCash = session.startingCash.toMoney(),
                openingNote = session.openingNote
            )
        )
    }

    private fun buildCloseRequest(session: LocalCashSessionEntity): BuildCloseSessionRequestResult {
        val resolvedDeviceId = session.deviceId?.takeIf { it.isNotBlank() } ?: deviceId.takeIf { it.isNotBlank() }
        val serverCashSessionId = session.serverId?.takeIf { it.isNotBlank() }
        val closedAt = session.closedAt
        val actualCash = session.actualCash
        if (session.clientGeneratedId.isBlank()) {
            return BuildCloseSessionRequestResult.Error("Client ID sesi kas lokal belum tersedia.")
        }
        if (resolvedDeviceId == null) {
            return BuildCloseSessionRequestResult.Error("Device ID belum tersedia. Tutup sesi kas belum bisa disinkronkan.")
        }
        if (serverCashSessionId == null) {
            return BuildCloseSessionRequestResult.Error("Server ID sesi kas belum tersedia. Sync open harus berhasil lebih dulu.")
        }
        if (session.cashierUserId.isBlank()) {
            return BuildCloseSessionRequestResult.Error("Kasir sesi kas lokal belum tersedia.")
        }
        if (closedAt == null || closedAt <= 0L) {
            return BuildCloseSessionRequestResult.Error("Waktu tutup sesi lokal belum tersedia.")
        }
        if (actualCash == null || actualCash < 0.0) {
            return BuildCloseSessionRequestResult.Error("Kas fisik akhir sesi lokal belum valid.")
        }

        return BuildCloseSessionRequestResult.Success(
            OfflineCashSessionCloseSyncRequestDto(
                deviceId = resolvedDeviceId,
                clientGeneratedId = session.clientGeneratedId,
                serverCashSessionId = serverCashSessionId,
                cashierUserId = session.cashierUserId,
                closedAt = closedAt.toOffsetDateTimeString(),
                actualCash = actualCash.toMoney(),
                expectedCash = session.expectedCash?.toMoney(),
                difference = session.difference?.toMoney(),
                closingNote = session.closingNote
            )
        )
    }

    private fun Double.toMoney(): BigDecimal = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

    private fun Long.toOffsetDateTimeString(): String =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .toString()

    private sealed interface BuildOpenSessionRequestResult {
        data class Success(val request: OfflineCashSessionOpenSyncRequestDto) : BuildOpenSessionRequestResult
        data class Error(val message: String) : BuildOpenSessionRequestResult
    }

    private sealed interface BuildCloseSessionRequestResult {
        data class Success(val request: OfflineCashSessionCloseSyncRequestDto) : BuildCloseSessionRequestResult
        data class Error(val message: String) : BuildCloseSessionRequestResult
    }

    private companion object {
        const val DEFAULT_AUTO_SYNC_LIMIT = 10
        const val CASH_SESSION_CLOSED = "CLOSED"
    }
}

sealed interface OfflineCashSessionSyncResult {
    data class Success(
        val serverCashSessionId: String,
        val syncStatus: String
    ) : OfflineCashSessionSyncResult

    data class Failed(
        val message: String,
        val kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.UNKNOWN
    ) : OfflineCashSessionSyncResult

    data class Skipped(
        val message: String
    ) : OfflineCashSessionSyncResult
}

data class OfflineCashSessionBulkSyncResult(
    val total: Int,
    val successCount: Int,
    val failedCount: Int,
    val skippedCount: Int = 0,
    val authFailureCount: Int = 0,
    val transientFailureCount: Int = 0,
    val validationFailureCount: Int = 0
) {
    val hasAuthFailure: Boolean get() = authFailureCount > 0
    val hasTransientFailure: Boolean get() = transientFailureCount > 0
}
