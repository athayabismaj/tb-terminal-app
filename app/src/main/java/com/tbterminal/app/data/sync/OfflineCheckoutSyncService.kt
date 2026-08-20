package com.tbterminal.app.data.sync

import androidx.room.withTransaction
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.OfflineCheckoutSyncItemRequestDto
import com.tbterminal.app.data.remote.OfflineCheckoutSyncRequestDto
import com.tbterminal.app.data.remote.SalesApi
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

class OfflineCheckoutSyncService(
    private val database: TbTerminalDatabase,
    private val salesApi: SalesApi,
    private val deviceId: String,
    private val cashSessionSyncService: OfflineCashSessionSyncService? = null,
    private val cashExpenseSyncService: OfflineCashExpenseSyncService? = null
) {
    suspend fun recoverStaleSyncing() {
        val now = System.currentTimeMillis()
        val cutoff = now - STALE_SYNCING_TIMEOUT_MS
        database.withTransaction {
            database.transactionItemDao().recoverStaleSyncingItems(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.FAILED,
                syncedStatus = SyncStatus.SYNCED,
                cutoffUpdatedAt = cutoff,
                updatedAt = now
            )
            database.paymentDao().recoverStaleSyncingPayments(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.FAILED,
                syncedStatus = SyncStatus.SYNCED,
                cutoffUpdatedAt = cutoff,
                updatedAt = now
            )
            database.receivableDao().recoverStaleSyncingReceivables(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.FAILED,
                syncedStatus = SyncStatus.SYNCED,
                cutoffUpdatedAt = cutoff,
                updatedAt = now
            )
            database.syncQueueDao().recoverStaleSyncingTransactions(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.FAILED,
                syncedStatus = SyncStatus.SYNCED,
                entityType = SyncEntityType.TRANSACTION,
                cutoffUpdatedAt = cutoff,
                lastError = STALE_SYNCING_ERROR,
                updatedAt = now
            )
            database.transactionDao().recoverStaleSyncing(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.FAILED,
                cutoffUpdatedAt = cutoff,
                updatedAt = now
            )
        }
    }

    suspend fun syncOne(transactionLocalId: Long): OfflineCheckoutSyncResult {
        return OfflineSyncCoordinator.withSyncLock {
            syncOneInternal(
                transactionLocalId = transactionLocalId,
                transientFailuresRetryable = false
            )
        }
    }

    private suspend fun syncOneInternal(
        transactionLocalId: Long,
        transientFailuresRetryable: Boolean
    ): OfflineCheckoutSyncResult {
        recoverStaleSyncing()
        if (!markSyncingIfAllowed(transactionLocalId)) {
            return OfflineCheckoutSyncResult.Failed("Transaksi sedang disinkronkan atau sudah selesai.")
        }

        return runCatching {
            val transaction = database.transactionDao().getByLocalId(transactionLocalId)
                ?: return@runCatching fail(transactionLocalId, "Transaksi lokal tidak ditemukan", retryIncrement = 1)

            val transactionForSync = when (val ensureResult = ensureCashSessionServerId(transaction)) {
                is EnsureCashSessionResult.Success -> ensureResult.transaction
                is EnsureCashSessionResult.Error -> {
                    return@runCatching fail(
                        transactionLocalId = transactionLocalId,
                        message = ensureResult.message,
                        retryIncrement = 1,
                        kind = ensureResult.kind,
                        transientFailuresRetryable = transientFailuresRetryable
                    )
                }
            }

            val request = when (val buildResult = buildRequest(transactionForSync)) {
                is BuildSyncRequestResult.Success -> buildResult.request
                is BuildSyncRequestResult.Error -> {
                    return@runCatching fail(
                        transactionLocalId = transactionLocalId,
                        message = buildResult.message,
                        retryIncrement = 1,
                        kind = SyncErrorClassifier.classifyMessage(buildResult.message).toFailureKind()
                    )
                }
            }

            val response = salesApi.syncOfflineCheckout(request)
            if (!response.isSuccessful) {
                val message = when (response.code()) {
                    401, 403 -> "Sesi login tidak valid. Login ulang sebelum sinkronisasi."
                    in 400..499 -> response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Server menolak transaksi offline (${response.code()})"
                    else -> "Gagal terhubung ke server (${response.code()})"
                }
                return@runCatching fail(
                    transactionLocalId = transactionLocalId,
                    message = message,
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classify(
                        httpCode = response.code(),
                        message = message
                    ).toFailureKind(),
                    transientFailuresRetryable = transientFailuresRetryable
                )
            }

            val body = response.body()
            val syncResponse = body?.data
            if (body?.success != true || syncResponse == null) {
                return@runCatching fail(
                    transactionLocalId = transactionLocalId,
                    message = body?.message ?: body?.error ?: "Response sync tidak valid",
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classifyMessage(body?.message ?: body?.error).toFailureKind()
                )
            }

            val acceptedStatus = syncResponse.syncStatus.uppercase()
            if (acceptedStatus != "CREATED" && acceptedStatus != "DUPLICATE") {
                return@runCatching fail(
                    transactionLocalId = transactionLocalId,
                    message = "Status sync tidak dikenali: ${syncResponse.syncStatus}",
                    retryIncrement = 1,
                    kind = OfflineCheckoutSyncFailureKind.VALIDATION
                )
            }

            database.withTransaction {
                val syncedAt = System.currentTimeMillis()
                database.transactionDao().markSynced(
                    localId = transactionLocalId,
                    serverId = syncResponse.serverTransactionId,
                    status = SyncStatus.SYNCED,
                    syncedAt = syncedAt,
                    updatedAt = syncedAt
                )
                database.transactionItemDao().markTransactionItemsSynced(
                    transactionLocalId = transactionLocalId,
                    transactionServerId = syncResponse.serverTransactionId,
                    status = SyncStatus.SYNCED,
                    syncedAt = syncedAt,
                    updatedAt = syncedAt
                )
                database.paymentDao().getByTransaction(transactionLocalId).forEachIndexed { index, payment ->
                    database.paymentDao().markSynced(
                        localId = payment.localId,
                        serverId = syncResponse.serverPaymentIds.getOrNull(index),
                        transactionServerId = syncResponse.serverTransactionId,
                        status = SyncStatus.SYNCED,
                        syncedAt = syncedAt,
                        updatedAt = syncedAt
                    )
                }
                database.receivableDao().getByTransaction(transactionLocalId).forEach { receivable ->
                    database.receivableDao().markSynced(
                        localId = receivable.localId,
                        serverId = syncResponse.serverReceivableId,
                        transactionServerId = syncResponse.serverTransactionId,
                        syncStatus = SyncStatus.SYNCED,
                        syncedAt = syncedAt,
                        updatedAt = syncedAt
                    )
                }
                database.syncQueueDao().getLatestForEntity(
                    entityLocalId = transactionLocalId,
                    entityType = SyncEntityType.TRANSACTION
                )?.let { queue ->
                    database.syncQueueDao().updateStatus(
                        id = queue.id,
                        status = SyncStatus.SYNCED,
                        lastError = null,
                        retryIncrement = 0,
                        updatedAt = syncedAt
                    )
                }
            }

            transactionForSync.cashSessionLocalId?.let { cashSessionLocalId ->
                cashExpenseSyncService?.syncPendingExpensesForSession(cashSessionLocalId)
                cashSessionSyncService?.syncCloseSessionIfReady(cashSessionLocalId)
            }
            OfflineCheckoutSyncResult.Success(syncResponse.serverTransactionId, syncResponse.syncStatus)
        }.getOrElse { error ->
            fail(
                transactionLocalId = transactionLocalId,
                message = error.message ?: "Sinkronisasi transaksi offline gagal",
                retryIncrement = 1,
                kind = SyncErrorClassifier.classify(
                    message = error.message,
                    throwable = error
                ).toFailureKind(),
                transientFailuresRetryable = transientFailuresRetryable
            )
        }
    }

    private suspend fun ensureCashSessionServerId(
        transaction: LocalTransactionEntity
    ): EnsureCashSessionResult {
        if (!transaction.cashSessionServerId.isNullOrBlank()) {
            return EnsureCashSessionResult.Success(transaction)
        }

        val cashSessionLocalId = transaction.cashSessionLocalId
            ?: return EnsureCashSessionResult.Error(
                message = "Sesi kas lokal transaksi belum tersedia.",
                kind = OfflineCheckoutSyncFailureKind.VALIDATION
            )
        val syncService = cashSessionSyncService
            ?: return EnsureCashSessionResult.Error(
                message = "Service sync sesi kas lokal belum tersedia.",
                kind = OfflineCheckoutSyncFailureKind.VALIDATION
            )

        return when (val result = syncService.syncOpenSession(cashSessionLocalId)) {
            is OfflineCashSessionSyncResult.Success -> {
                val updatedTransaction = database.transactionDao().getByLocalId(transaction.localId)
                    ?: return EnsureCashSessionResult.Error(
                        message = "Transaksi lokal tidak ditemukan setelah sync sesi kas.",
                        kind = OfflineCheckoutSyncFailureKind.VALIDATION
                    )
                if (updatedTransaction.cashSessionServerId.isNullOrBlank()) {
                    EnsureCashSessionResult.Error(
                        message = "Sesi kas server belum tersedia setelah sync sesi lokal.",
                        kind = OfflineCheckoutSyncFailureKind.VALIDATION
                    )
                } else {
                    EnsureCashSessionResult.Success(updatedTransaction)
                }
            }
            is OfflineCashSessionSyncResult.Failed -> {
                EnsureCashSessionResult.Error(result.message, result.kind)
            }
            is OfflineCashSessionSyncResult.Skipped -> {
                EnsureCashSessionResult.Error(
                    message = result.message,
                    kind = OfflineCheckoutSyncFailureKind.VALIDATION
                )
            }
        }
    }

    suspend fun syncPendingAndFailed(
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): OfflineCheckoutBulkSyncResult {
        return OfflineSyncCoordinator.withSyncLock {
            recoverStaleSyncing()
            val localIds = database.transactionDao().getSyncableLocalIds(
                pending = SyncStatus.PENDING,
                failed = SyncStatus.FAILED
            )
            syncBatch(
                localIds = localIds,
                transientFailuresRetryable = false,
                onProgress = onProgress
            )
        }
    }

    suspend fun syncPendingOnly(
        limit: Int = DEFAULT_AUTO_SYNC_LIMIT,
        maxRetryCount: Int = DEFAULT_AUTO_SYNC_MAX_RETRY,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): OfflineCheckoutBulkSyncResult {
        return OfflineSyncCoordinator.withSyncLock {
            recoverStaleSyncing()
            val localIds = database.transactionDao().getPendingLocalIdsForAutoSync(
                pending = SyncStatus.PENDING,
                entityType = SyncEntityType.TRANSACTION,
                maxRetryCount = maxRetryCount,
                limit = limit
            )
            syncBatch(
                localIds = localIds,
                transientFailuresRetryable = true,
                onProgress = onProgress
            )
        }
    }

    private suspend fun syncBatch(
        localIds: List<Long>,
        transientFailuresRetryable: Boolean,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): OfflineCheckoutBulkSyncResult {
        if (localIds.isEmpty()) {
            return OfflineCheckoutBulkSyncResult(total = 0, successCount = 0, failedCount = 0)
        }

        var successCount = 0
        var failedCount = 0
        var authFailureCount = 0
        var transientFailureCount = 0
        var validationFailureCount = 0
        localIds.forEachIndexed { index, localId ->
            onProgress?.invoke(index + 1, localIds.size)
            when (val result = syncOneInternal(localId, transientFailuresRetryable)) {
                is OfflineCheckoutSyncResult.Success -> successCount += 1
                is OfflineCheckoutSyncResult.Failed -> {
                    failedCount += 1
                    when (result.kind) {
                        OfflineCheckoutSyncFailureKind.AUTH -> authFailureCount += 1
                        OfflineCheckoutSyncFailureKind.TRANSIENT -> transientFailureCount += 1
                        OfflineCheckoutSyncFailureKind.VALIDATION -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.CONFLICT -> validationFailureCount += 1
                        OfflineCheckoutSyncFailureKind.UNKNOWN -> validationFailureCount += 1
                    }
                    if (result.kind == OfflineCheckoutSyncFailureKind.AUTH) {
                        return OfflineCheckoutBulkSyncResult(
                            total = localIds.size,
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

        return OfflineCheckoutBulkSyncResult(
            total = localIds.size,
            successCount = successCount,
            failedCount = failedCount,
            authFailureCount = authFailureCount,
            transientFailureCount = transientFailureCount,
            validationFailureCount = validationFailureCount
        )
    }

    private suspend fun buildRequest(transaction: LocalTransactionEntity): BuildSyncRequestResult {
        if (transaction.clientGeneratedId.isBlank()) {
            return BuildSyncRequestResult.Error("Client ID transaksi lokal belum tersedia.")
        }
        val resolvedDeviceId = transaction.deviceId?.takeIf { it.isNotBlank() } ?: deviceId.takeIf { it.isNotBlank() }
        val cashierUserId = transaction.cashierUserId?.takeIf { it.isNotBlank() }
            ?: return BuildSyncRequestResult.Error("Kasir transaksi lokal belum tersedia.")
        val cashSessionId = transaction.cashSessionServerId?.takeIf { it.isNotBlank() }
            ?: return BuildSyncRequestResult.Error("Sesi kas server belum tersedia. Buka ulang sesi sebelum sinkronisasi.")
        if (resolvedDeviceId == null) {
            return BuildSyncRequestResult.Error("Device ID belum tersedia. Transaksi belum bisa disinkronkan.")
        }
        if (requiresCustomer(transaction) && transaction.customerServerId.isNullOrBlank()) {
            return BuildSyncRequestResult.Error("Pelanggan transaksi hutang/DP belum punya ID server.")
        }

        val localItems = database.transactionItemDao().getByTransaction(transaction.localId)
        if (localItems.isEmpty()) {
            return BuildSyncRequestResult.Error("Item transaksi lokal tidak ditemukan.")
        }
        if (localItems.any { it.productServerId.isNullOrBlank() }) {
            return BuildSyncRequestResult.Error("Produk lokal belum punya ID server. Transaksi belum bisa disinkronkan.")
        }
        val items = localItems.map { item ->
            val productServerId = item.productServerId.orEmpty()
            OfflineCheckoutSyncItemRequestDto(
                productId = productServerId,
                productNameSnapshot = item.productNameSnapshot,
                quantity = item.quantity.toMoney(),
                priceAtTransaction = item.priceAtTransaction.toMoney(),
                cogsAtTransaction = item.cogsAtTransaction.toMoney(),
                discount = item.discount.toMoney(),
                subtotal = item.subtotal.toMoney()
            )
        }

        return BuildSyncRequestResult.Success(
            OfflineCheckoutSyncRequestDto(
                clientGeneratedId = transaction.clientGeneratedId,
                deviceId = resolvedDeviceId,
                localTransactionCode = transaction.transactionCode,
                cashierUserId = cashierUserId,
                cashSessionId = cashSessionId,
                customerId = transaction.customerServerId,
                paymentMethod = paymentMethodForSync(transaction),
                subtotal = transaction.subtotal.toMoney(),
                discount = transaction.discount.toMoney(),
                total = transaction.total.toMoney(),
                paidAmount = transaction.paidAmount.toMoney(),
                remainingAmount = transaction.remainingAmount.toMoney(),
                occurredAt = transaction.occurredAt.toOffsetDateTimeString(),
                note = null,
                items = items
            )
        )
    }

    private suspend fun markSyncingIfAllowed(transactionLocalId: Long): Boolean {
        val now = System.currentTimeMillis()
        var locked = false
        database.withTransaction {
            locked = database.transactionDao().markSyncingIfPendingOrFailed(
                localId = transactionLocalId,
                syncing = SyncStatus.SYNCING,
                pending = SyncStatus.PENDING,
                failed = SyncStatus.FAILED,
                conflict = SyncStatus.CONFLICT,
                updatedAt = now
            ) > 0
            if (locked) {
                database.syncQueueDao().getLatestForEntity(
                    entityLocalId = transactionLocalId,
                    entityType = SyncEntityType.TRANSACTION
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

    private suspend fun fail(
        transactionLocalId: Long,
        message: String,
        retryIncrement: Int,
        kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.VALIDATION,
        transientFailuresRetryable: Boolean = false
    ): OfflineCheckoutSyncResult.Failed {
        if (transientFailuresRetryable && kind == OfflineCheckoutSyncFailureKind.TRANSIENT) {
            markPendingWithChildren(transactionLocalId, message, retryIncrement)
        } else {
            val status = if (kind == OfflineCheckoutSyncFailureKind.CONFLICT) {
                SyncStatus.CONFLICT
            } else {
                SyncStatus.FAILED
            }
            markFailedWithChildren(transactionLocalId, message, retryIncrement, status)
        }
        return OfflineCheckoutSyncResult.Failed(message, kind)
    }

    private suspend fun markFailedWithChildren(
        transactionLocalId: Long,
        message: String,
        retryIncrement: Int,
        status: SyncStatus
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.transactionDao().updateSyncStatus(
                localId = transactionLocalId,
                status = status,
                syncedAt = null,
                updatedAt = now
            )
            database.transactionItemDao().markTransactionItemsFailed(
                transactionLocalId = transactionLocalId,
                status = status,
                syncedStatus = SyncStatus.SYNCED,
                updatedAt = now
            )
            database.paymentDao().markPaymentsFailed(
                transactionLocalId = transactionLocalId,
                status = status,
                syncedStatus = SyncStatus.SYNCED,
                updatedAt = now
            )
            database.receivableDao().markReceivablesFailed(
                transactionLocalId = transactionLocalId,
                syncStatus = status,
                syncedStatus = SyncStatus.SYNCED,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntity(
                entityLocalId = transactionLocalId,
                entityType = SyncEntityType.TRANSACTION
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
    }

    private suspend fun markPendingWithChildren(
        transactionLocalId: Long,
        message: String,
        retryIncrement: Int
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.transactionDao().updateSyncStatus(
                localId = transactionLocalId,
                status = SyncStatus.PENDING,
                syncedAt = null,
                updatedAt = now
            )
            database.transactionItemDao().markTransactionItemsFailed(
                transactionLocalId = transactionLocalId,
                status = SyncStatus.PENDING,
                syncedStatus = SyncStatus.SYNCED,
                updatedAt = now
            )
            database.paymentDao().markPaymentsFailed(
                transactionLocalId = transactionLocalId,
                status = SyncStatus.PENDING,
                syncedStatus = SyncStatus.SYNCED,
                updatedAt = now
            )
            database.receivableDao().markReceivablesFailed(
                transactionLocalId = transactionLocalId,
                syncStatus = SyncStatus.PENDING,
                syncedStatus = SyncStatus.SYNCED,
                updatedAt = now
            )
            database.syncQueueDao().getLatestForEntity(
                entityLocalId = transactionLocalId,
                entityType = SyncEntityType.TRANSACTION
            )?.let { queue ->
                database.syncQueueDao().updateStatus(
                    id = queue.id,
                    status = SyncStatus.PENDING,
                    lastError = message,
                    retryIncrement = retryIncrement,
                    updatedAt = now
                )
            }
        }
    }

    private fun requiresCustomer(transaction: LocalTransactionEntity): Boolean {
        return transaction.remainingAmount > 0.0 || transaction.status.equals("UNPAID", ignoreCase = true)
    }

    private suspend fun paymentMethodForSync(transaction: LocalTransactionEntity): String {
        val payment = database.paymentDao().getByTransaction(transaction.localId).firstOrNull()
        return payment?.method?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: if (transaction.remainingAmount > 0.0) "hutang" else "tunai"
    }

    private fun Double.toMoney(): BigDecimal = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

    private fun Long.toOffsetDateTimeString(): String =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .toString()

    private sealed interface BuildSyncRequestResult {
        data class Success(val request: OfflineCheckoutSyncRequestDto) : BuildSyncRequestResult
        data class Error(val message: String) : BuildSyncRequestResult
    }

    private sealed interface EnsureCashSessionResult {
        data class Success(val transaction: LocalTransactionEntity) : EnsureCashSessionResult
        data class Error(
            val message: String,
            val kind: OfflineCheckoutSyncFailureKind
        ) : EnsureCashSessionResult
    }

    private companion object {
        const val STALE_SYNCING_TIMEOUT_MS = 10 * 60 * 1000L
        const val STALE_SYNCING_ERROR = "Sync terputus. Silakan coba lagi."
        const val DEFAULT_AUTO_SYNC_LIMIT = 10
        const val DEFAULT_AUTO_SYNC_MAX_RETRY = 3
    }
}

sealed interface OfflineCheckoutSyncResult {
    data class Success(
        val serverTransactionId: String,
        val syncStatus: String
    ) : OfflineCheckoutSyncResult

    data class Failed(
        val message: String,
        val kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.UNKNOWN
    ) : OfflineCheckoutSyncResult
}

enum class OfflineCheckoutSyncFailureKind {
    AUTH,
    VALIDATION,
    CONFLICT,
    TRANSIENT,
    UNKNOWN
}

internal fun SyncErrorClassification.toFailureKind(): OfflineCheckoutSyncFailureKind {
    return when (category) {
        SyncErrorCategory.AUTH -> OfflineCheckoutSyncFailureKind.AUTH
        SyncErrorCategory.NETWORK -> OfflineCheckoutSyncFailureKind.TRANSIENT
        SyncErrorCategory.CONFLICT -> OfflineCheckoutSyncFailureKind.CONFLICT
        SyncErrorCategory.VALIDATION -> OfflineCheckoutSyncFailureKind.VALIDATION
        SyncErrorCategory.UNKNOWN -> OfflineCheckoutSyncFailureKind.UNKNOWN
    }
}

data class OfflineCheckoutBulkSyncResult(
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
