package com.tbterminal.app.data.sync

import androidx.room.withTransaction
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.OfflineCashExpenseSyncRequestDto
import com.tbterminal.app.data.remote.SalesApi
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

class OfflineCashExpenseSyncService(
    private val database: TbTerminalDatabase,
    private val salesApi: SalesApi,
    private val deviceId: String,
    private val cashSessionSyncService: OfflineCashSessionSyncService
) {
    suspend fun syncExpense(localExpenseId: Long): OfflineCashExpenseSyncResult {
        if (!markSyncingIfAllowed(localExpenseId)) {
            return OfflineCashExpenseSyncResult.Failed("Pengeluaran kas sedang disinkronkan atau sudah selesai.")
        }

        return runCatching {
            val expense = database.cashExpenseDao().getByLocalId(localExpenseId)
                ?: return@runCatching fail(localExpenseId, "Pengeluaran kas lokal tidak ditemukan.", 1)

            val expenseForSync = when (val ensureResult = ensureCashSessionServerId(expense)) {
                is EnsureExpenseSessionResult.Success -> ensureResult.expense
                is EnsureExpenseSessionResult.Error -> {
                    return@runCatching fail(
                        localExpenseId = localExpenseId,
                        message = ensureResult.message,
                        retryIncrement = 1,
                        kind = ensureResult.kind
                    )
                }
            }

            val request = when (val buildResult = buildRequest(expenseForSync)) {
                is BuildExpenseSyncRequestResult.Success -> buildResult.request
                is BuildExpenseSyncRequestResult.Error -> {
                    return@runCatching fail(
                        localExpenseId = localExpenseId,
                        message = buildResult.message,
                        retryIncrement = 1,
                        kind = SyncErrorClassifier.classifyMessage(buildResult.message).toFailureKind()
                    )
                }
            }

            val response = salesApi.syncCashExpense(request)
            if (!response.isSuccessful) {
                val message = when (response.code()) {
                    401, 403 -> "Sesi login tidak valid. Login ulang sebelum sinkronisasi pengeluaran kas."
                    in 400..499 -> response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Server menolak pengeluaran kas offline (${response.code()})"
                    else -> "Gagal terhubung ke server (${response.code()})"
                }
                return@runCatching fail(
                    localExpenseId = localExpenseId,
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
                    localExpenseId = localExpenseId,
                    message = body?.message ?: body?.error ?: "Response sync pengeluaran kas tidak valid",
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classifyMessage(body?.message ?: body?.error).toFailureKind()
                )
            }

            val acceptedStatus = syncResponse.syncStatus.uppercase()
            if (acceptedStatus != "CREATED" && acceptedStatus != "DUPLICATE") {
                return@runCatching fail(
                    localExpenseId = localExpenseId,
                    message = "Status sync pengeluaran kas tidak dikenali: ${syncResponse.syncStatus}",
                    retryIncrement = 1,
                    kind = OfflineCheckoutSyncFailureKind.VALIDATION
                )
            }

            markSynced(localExpenseId, syncResponse.serverExpenseId)
            OfflineCashExpenseSyncResult.Success(syncResponse.serverExpenseId, syncResponse.syncStatus)
        }.getOrElse { error ->
            fail(
                localExpenseId = localExpenseId,
                message = error.message ?: "Sinkronisasi pengeluaran kas offline gagal",
                retryIncrement = 1,
                kind = SyncErrorClassifier.classify(
                    message = error.message,
                    throwable = error
                ).toFailureKind()
            )
        }
    }

    suspend fun syncPendingExpenses(
        limit: Int = DEFAULT_AUTO_SYNC_LIMIT,
        includeFailed: Boolean = false
    ): OfflineCashExpenseBulkSyncResult {
        val failedStatus = if (includeFailed) SyncStatus.FAILED else SyncStatus.PENDING
        val expenses = database.cashExpenseDao().getSyncCandidates(
            pending = SyncStatus.PENDING,
            failed = failedStatus,
            limit = limit
        )
        return syncBatch(expenses)
    }

    suspend fun syncPendingExpensesForSession(
        cashSessionLocalId: Long,
        limit: Int = DEFAULT_AUTO_SYNC_LIMIT,
        includeFailed: Boolean = false
    ): OfflineCashExpenseBulkSyncResult {
        val failedStatus = if (includeFailed) SyncStatus.FAILED else SyncStatus.PENDING
        val expenses = database.cashExpenseDao().getSyncCandidatesByCashSession(
            cashSessionLocalId = cashSessionLocalId,
            pending = SyncStatus.PENDING,
            failed = failedStatus,
            limit = limit
        )
        return syncBatch(expenses)
    }

    private suspend fun syncBatch(expenses: List<LocalCashExpenseEntity>): OfflineCashExpenseBulkSyncResult {
        if (expenses.isEmpty()) {
            return OfflineCashExpenseBulkSyncResult(total = 0, successCount = 0, failedCount = 0)
        }

        var successCount = 0
        var failedCount = 0
        var authFailureCount = 0
        var transientFailureCount = 0
        var validationFailureCount = 0
        expenses.forEach { expense ->
            when (val result = syncExpense(expense.localId)) {
                is OfflineCashExpenseSyncResult.Success -> successCount += 1
                is OfflineCashExpenseSyncResult.Failed -> {
                    failedCount += 1
                    when (result.kind) {
                        OfflineCheckoutSyncFailureKind.AUTH -> authFailureCount += 1
                        OfflineCheckoutSyncFailureKind.TRANSIENT -> transientFailureCount += 1
                        OfflineCheckoutSyncFailureKind.VALIDATION -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.CONFLICT -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.UNKNOWN -> validationFailureCount += 1
                    }
                    if (result.kind == OfflineCheckoutSyncFailureKind.AUTH) {
                        return OfflineCashExpenseBulkSyncResult(
                            total = expenses.size,
                            successCount = successCount,
                            failedCount = failedCount,
                            authFailureCount = authFailureCount,
                            transientFailureCount = transientFailureCount,
                            validationFailureCount = validationFailureCount
                        )
                    }
                }
            }
        }

        return OfflineCashExpenseBulkSyncResult(
            total = expenses.size,
            successCount = successCount,
            failedCount = failedCount,
            authFailureCount = authFailureCount,
            transientFailureCount = transientFailureCount,
            validationFailureCount = validationFailureCount
        )
    }

    private suspend fun ensureCashSessionServerId(
        expense: LocalCashExpenseEntity
    ): EnsureExpenseSessionResult {
        if (!expense.cashSessionServerId.isNullOrBlank()) {
            return EnsureExpenseSessionResult.Success(expense)
        }

        val cashSessionLocalId = expense.cashSessionLocalId
            ?: return EnsureExpenseSessionResult.Error(
                message = "Sesi kas lokal pengeluaran belum tersedia.",
                kind = OfflineCheckoutSyncFailureKind.VALIDATION
            )

        return when (val result = cashSessionSyncService.syncOpenSession(cashSessionLocalId)) {
            is OfflineCashSessionSyncResult.Success -> {
                database.cashExpenseDao().updateCashSessionServerIdForLocalSession(
                    cashSessionLocalId = cashSessionLocalId,
                    serverId = result.serverCashSessionId,
                    updatedAt = System.currentTimeMillis()
                )
                val updatedExpense = database.cashExpenseDao().getByLocalId(expense.localId)
                    ?: return EnsureExpenseSessionResult.Error(
                        message = "Pengeluaran kas lokal tidak ditemukan setelah sync sesi kas.",
                        kind = OfflineCheckoutSyncFailureKind.VALIDATION
                    )
                if (updatedExpense.cashSessionServerId.isNullOrBlank()) {
                    EnsureExpenseSessionResult.Error(
                        message = "Sesi kas server belum tersedia untuk pengeluaran kas.",
                        kind = OfflineCheckoutSyncFailureKind.VALIDATION
                    )
                } else {
                    EnsureExpenseSessionResult.Success(updatedExpense)
                }
            }
            is OfflineCashSessionSyncResult.Failed -> EnsureExpenseSessionResult.Error(result.message, result.kind)
            is OfflineCashSessionSyncResult.Skipped -> EnsureExpenseSessionResult.Error(
                message = result.message,
                kind = OfflineCheckoutSyncFailureKind.VALIDATION
            )
        }
    }

    private suspend fun buildRequest(expense: LocalCashExpenseEntity): BuildExpenseSyncRequestResult {
        val resolvedDeviceId = expense.deviceId?.takeIf { it.isNotBlank() } ?: deviceId.takeIf { it.isNotBlank() }
        val sessionServerId = expense.cashSessionServerId?.takeIf { it.isNotBlank() }
        val sessionLocalId = expense.cashSessionLocalId
        if (expense.clientGeneratedId.isBlank()) {
            return BuildExpenseSyncRequestResult.Error("Client ID pengeluaran kas lokal belum tersedia.")
        }
        if (resolvedDeviceId == null) {
            return BuildExpenseSyncRequestResult.Error("Device ID belum tersedia. Pengeluaran kas belum bisa disinkronkan.")
        }
        if (sessionServerId == null) {
            return BuildExpenseSyncRequestResult.Error("Sesi kas server belum tersedia. Sync open harus berhasil lebih dulu.")
        }
        if (sessionLocalId == null) {
            return BuildExpenseSyncRequestResult.Error("Sesi kas lokal pengeluaran belum tersedia.")
        }
        val session = runCatching { database.cashSessionDao().getByLocalId(sessionLocalId) }.getOrNull()
            ?: return BuildExpenseSyncRequestResult.Error("Sesi kas lokal pengeluaran tidak ditemukan.")
        if (session.cashierUserId.isBlank()) {
            return BuildExpenseSyncRequestResult.Error("Kasir pengeluaran kas lokal belum tersedia.")
        }
        if (expense.amount <= 0.0) {
            return BuildExpenseSyncRequestResult.Error("Nominal pengeluaran kas harus lebih besar dari nol.")
        }
        val note = expense.description?.trim()?.takeIf { it.isNotBlank() }
            ?: return BuildExpenseSyncRequestResult.Error("Catatan pengeluaran kas wajib diisi.")

        return BuildExpenseSyncRequestResult.Success(
            OfflineCashExpenseSyncRequestDto(
                clientGeneratedId = expense.clientGeneratedId,
                deviceId = resolvedDeviceId,
                cashierUserId = session.cashierUserId,
                serverCashSessionId = sessionServerId,
                amount = expense.amount.toMoney(),
                category = expense.category?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY,
                note = note,
                occurredAt = expense.occurredAt.toOffsetDateTimeString()
            )
        )
    }

    private suspend fun markSyncingIfAllowed(localExpenseId: Long): Boolean {
        val now = System.currentTimeMillis()
        var locked = false
        database.withTransaction {
            locked = database.cashExpenseDao().markSyncingIfPendingOrFailed(
                localId = localExpenseId,
                syncing = SyncStatus.SYNCING,
                pending = SyncStatus.PENDING,
                failed = SyncStatus.FAILED,
                conflict = SyncStatus.CONFLICT,
                updatedAt = now
            ) > 0
            if (locked) {
                database.syncQueueDao().getLatestForEntityAndOperation(
                    entityLocalId = localExpenseId,
                    entityType = SyncEntityType.CASH_EXPENSE,
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

    private suspend fun markSynced(localExpenseId: Long, serverId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.cashExpenseDao().markSynced(
                localId = localExpenseId,
                serverId = serverId,
                status = SyncStatus.SYNCED,
                syncedAt = now,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntityAndOperation(
                entityLocalId = localExpenseId,
                entityType = SyncEntityType.CASH_EXPENSE,
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

    private suspend fun fail(
        localExpenseId: Long,
        message: String,
        retryIncrement: Int,
        kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.VALIDATION
    ): OfflineCashExpenseSyncResult.Failed {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val status = if (kind == OfflineCheckoutSyncFailureKind.CONFLICT) {
                SyncStatus.CONFLICT
            } else {
                SyncStatus.FAILED
            }
            database.cashExpenseDao().updateSyncStatus(
                localId = localExpenseId,
                status = status,
                syncedAt = null,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntityAndOperation(
                entityLocalId = localExpenseId,
                entityType = SyncEntityType.CASH_EXPENSE,
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
        return OfflineCashExpenseSyncResult.Failed(message, kind)
    }

    private fun Double.toMoney(): BigDecimal = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

    private fun Long.toOffsetDateTimeString(): String =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .toString()

    private sealed interface EnsureExpenseSessionResult {
        data class Success(val expense: LocalCashExpenseEntity) : EnsureExpenseSessionResult
        data class Error(
            val message: String,
            val kind: OfflineCheckoutSyncFailureKind
        ) : EnsureExpenseSessionResult
    }

    private sealed interface BuildExpenseSyncRequestResult {
        data class Success(val request: OfflineCashExpenseSyncRequestDto) : BuildExpenseSyncRequestResult
        data class Error(val message: String) : BuildExpenseSyncRequestResult
    }

    private companion object {
        const val DEFAULT_AUTO_SYNC_LIMIT = 10
        const val DEFAULT_CATEGORY = "OPERASIONAL"
    }
}

sealed interface OfflineCashExpenseSyncResult {
    data class Success(
        val serverExpenseId: String,
        val syncStatus: String
    ) : OfflineCashExpenseSyncResult

    data class Failed(
        val message: String,
        val kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.UNKNOWN
    ) : OfflineCashExpenseSyncResult
}

data class OfflineCashExpenseBulkSyncResult(
    val total: Int,
    val successCount: Int,
    val failedCount: Int,
    val authFailureCount: Int = 0,
    val transientFailureCount: Int = 0,
    val validationFailureCount: Int = 0
) {
    val hasAuthFailure: Boolean get() = authFailureCount > 0
    val hasTransientFailure: Boolean get() = transientFailureCount > 0
}
