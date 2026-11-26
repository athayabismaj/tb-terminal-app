package com.tbterminal.app.data.sync

import androidx.room.withTransaction
import com.tbterminal.app.data.local.checkout.normalizeExactDecimal
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.CheckoutPaymentLineRequestDto
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
    private val cashExpenseSyncService: OfflineCashExpenseSyncService? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun recoverStaleSyncing(nowMillis: Long = clockMillis()) {
        OfflineSyncCoordinator.withSyncLock {
            recoverStaleSyncingUnlocked(nowMillis)
        }
    }

    internal suspend fun recoverStaleSyncingUnlocked(nowMillis: Long = clockMillis()) {
        val cutoff = nowMillis - SYNC_STALE_TIMEOUT_MS
        database.withTransaction {
            database.transactionItemDao().recoverStaleSyncingItems(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.PENDING,
                syncedStatus = SyncStatus.SYNCED,
                cutoffUpdatedAt = cutoff,
                updatedAt = nowMillis
            )
            database.paymentDao().recoverStaleSyncingPayments(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.PENDING,
                syncedStatus = SyncStatus.SYNCED,
                cutoffUpdatedAt = cutoff,
                updatedAt = nowMillis
            )
            database.receivableDao().recoverStaleSyncingReceivables(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.PENDING,
                syncedStatus = SyncStatus.SYNCED,
                cutoffUpdatedAt = cutoff,
                updatedAt = nowMillis
            )
            database.syncQueueDao().recoverStaleSyncingTransactions(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.PENDING,
                syncedStatus = SyncStatus.SYNCED,
                entityType = SyncEntityType.TRANSACTION,
                cutoffUpdatedAt = cutoff,
                lastError = SYNC_STALE_ERROR,
                updatedAt = nowMillis
            )
            database.transactionDao().recoverStaleSyncing(
                syncing = SyncStatus.SYNCING,
                failed = SyncStatus.PENDING,
                cutoffUpdatedAt = cutoff,
                updatedAt = nowMillis
            )
        }
    }

    suspend fun syncOne(transactionLocalId: Long): OfflineCheckoutSyncResult {
        return OfflineSyncCoordinator.withSyncLock {
            syncOneInternal(transactionLocalId = transactionLocalId)
        }
    }

    private suspend fun syncOneInternal(
        transactionLocalId: Long,
    ): OfflineCheckoutSyncResult {
        recoverStaleSyncingUnlocked()
        if (!markSyncingIfAllowed(transactionLocalId)) {
            return OfflineCheckoutSyncResult.Failed("Transaksi sedang disinkronkan atau sudah selesai.")
        }

        return runSuspendCatching {
            val transaction = database.transactionDao().getByLocalId(transactionLocalId)
                ?: return@runSuspendCatching fail(transactionLocalId, "Transaksi lokal tidak ditemukan", retryIncrement = 1)

            val transactionForSync = when (val ensureResult = ensureCashSessionServerId(transaction)) {
                is EnsureCashSessionResult.Success -> ensureResult.transaction
                is EnsureCashSessionResult.Error -> {
                    return@runSuspendCatching fail(
                        transactionLocalId = transactionLocalId,
                        message = ensureResult.message,
                        retryIncrement = 1,
                        kind = ensureResult.kind,
                        retryAfterSeconds = ensureResult.retryAfterSeconds,
                    )
                }
            }

            val request = when (val buildResult = buildRequest(transactionForSync)) {
                is BuildSyncRequestResult.Success -> buildResult.request
                is BuildSyncRequestResult.Error -> {
                    return@runSuspendCatching fail(
                        transactionLocalId = transactionLocalId,
                        message = buildResult.message,
                        retryIncrement = 1,
                        kind = SyncErrorClassifier.classifyMessage(buildResult.message).toFailureKind()
                    )
                }
            }

            val response = salesApi.syncOfflineCheckout(request)
            if (!response.isSuccessful) {
                val parsedError = SyncErrorResponseParser.parseHttpError(
                    response = response,
                    defaultMessage = if (response.code() in 400..499) {
                        "Server menolak transaksi offline (${response.code()})"
                    } else {
                        "Gagal terhubung ke server (${response.code()})"
                    },
                    clockMillis = clockMillis,
                )
                val message = if (response.code() >= 500) {
                    "Gagal terhubung ke server (${response.code()})"
                } else {
                    parsedError.message
                }
                val errorCode = if (response.code() >= 500) null else parsedError.code
                return@runSuspendCatching fail(
                    transactionLocalId = transactionLocalId,
                    message = message,
                    errorCode = errorCode,
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classify(
                        httpCode = response.code(),
                        message = message,
                        errorCode = errorCode
                    ).toFailureKind(),
                    retryAfterSeconds = parsedError.retryAfterSeconds,
                )
            }

            val body = response.body()
            val syncResponse = body?.data
            if (body?.success != true || syncResponse == null) {
                val errorCode = body?.code?.takeIf(String::isNotBlank)
                val message = body?.message ?: body?.error ?: "Response sync tidak valid"
                return@runSuspendCatching fail(
                    transactionLocalId = transactionLocalId,
                    message = message,
                    errorCode = errorCode,
                    retryIncrement = 1,
                    kind = SyncErrorClassifier.classifyMessage(message, errorCode).toFailureKind()
                )
            }

            val acceptedStatus = syncResponse.syncStatus.uppercase()
            if (acceptedStatus != "CREATED" && acceptedStatus != "DUPLICATE") {
                return@runSuspendCatching fail(
                    transactionLocalId = transactionLocalId,
                    message = "Status sync tidak dikenali: ${syncResponse.syncStatus}",
                    retryIncrement = 1,
                    kind = OfflineCheckoutSyncFailureKind.VALIDATION
                )
            }

            database.withTransaction {
                val syncedAt = clockMillis()
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
                        lastErrorCode = null,
                        retryIncrement = 0,
                        updatedAt = syncedAt
                    )
                }
            }

            transactionForSync.cashSessionLocalId?.let { cashSessionLocalId ->
                cashExpenseSyncService?.syncPendingExpensesForSessionUnlocked(cashSessionLocalId)
                cashSessionSyncService?.syncCloseSessionIfReadyUnlocked(cashSessionLocalId)
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

        return when (val result = syncService.syncOpenSessionUnlocked(cashSessionLocalId)) {
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
                EnsureCashSessionResult.Error(result.message, result.kind, result.retryAfterSeconds)
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
            recoverStaleSyncingUnlocked()
            val localIds = database.transactionDao().getSyncableLocalIds(
                pending = SyncStatus.PENDING,
                failed = SyncStatus.FAILED
            )
            syncBatch(
                localIds = localIds,
                onProgress = onProgress
            )
        }
    }

    suspend fun syncPendingOnly(
        limit: Int = DEFAULT_AUTO_SYNC_LIMIT,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): OfflineCheckoutBulkSyncResult {
        return OfflineSyncCoordinator.withSyncLock {
            syncPendingOnlyUnlocked(limit = limit, onProgress = onProgress)
        }
    }

    internal suspend fun syncPendingOnlyUnlocked(
        limit: Int = DEFAULT_AUTO_SYNC_LIMIT,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): OfflineCheckoutBulkSyncResult {
        recoverStaleSyncingUnlocked()
        val localIds = database.transactionDao().getPendingLocalIdsForAutoSync(
            pending = SyncStatus.PENDING,
            limit = limit
        )
        return syncBatch(
            localIds = localIds,
            onProgress = onProgress
        )
    }

    private suspend fun syncBatch(
        localIds: List<Long>,
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
        var maxRetryAfterSeconds: Long? = null
        localIds.forEachIndexed { index, localId ->
            onProgress?.invoke(index + 1, localIds.size)
            when (val result = syncOneInternal(localId)) {
                is OfflineCheckoutSyncResult.Success -> successCount += 1
                is OfflineCheckoutSyncResult.Failed -> {
                    failedCount += 1
                    maxRetryAfterSeconds = maxNullableSeconds(maxRetryAfterSeconds, result.retryAfterSeconds)
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
                            validationFailureCount = validationFailureCount,
                            maxRetryAfterSeconds = maxRetryAfterSeconds,
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
            validationFailureCount = validationFailureCount,
            maxRetryAfterSeconds = maxRetryAfterSeconds,
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
            val resolvedUnitId = item.unitServerId?.trim()?.takeIf(String::isNotBlank)
            val resolvedBaseQuantity = item.baseQuantity
                ?.takeIf { it > BigDecimal.ZERO }
                ?.let(::normalizeExactDecimal)
            // Always send unitId and baseQuantity together; legacy rows without both are sent
            // without either field so the server handles conversion safely.
            val sendUnitPair = resolvedUnitId != null && resolvedBaseQuantity != null
            OfflineCheckoutSyncItemRequestDto(
                productId = productServerId,
                productNameSnapshot = item.productNameSnapshot,
                quantity = item.quantity.toBigDecimal(),
                priceAtTransaction = item.priceAtTransaction.toBigDecimal(),
                cogsAtTransaction = item.cogsAtTransaction.toBigDecimal(),
                discount = item.discount.toBigDecimal(),
                subtotal = item.subtotal.toBigDecimal(),
                note = item.note,
                unitId = if (sendUnitPair) resolvedUnitId else null,
                baseQuantity = if (sendUnitPair) resolvedBaseQuantity else null
            )
        }
        val localPayments = database.paymentDao().getByTransaction(transaction.localId)
        val useSplitLines = localPayments.size > 1 || localPayments.any { !it.reference.isNullOrBlank() }
        val syncPayments = if (useSplitLines) {
            localPayments.map { payment ->
                CheckoutPaymentLineRequestDto(
                    method = payment.method.trim().lowercase(),
                    amount = payment.amount.toBigDecimal().toPlainString(),
                    reference = payment.reference
                )
            }
        } else {
            null
        }
        val syncMethod = if (useSplitLines) {
            if (transaction.remainingAmount.isPositive) "dp" else ""
        } else {
            paymentMethodForSync(transaction)
        }

        return BuildSyncRequestResult.Success(
            OfflineCheckoutSyncRequestDto(
                clientGeneratedId = transaction.clientGeneratedId,
                deviceId = resolvedDeviceId,
                localTransactionCode = transaction.transactionCode,
                cashierUserId = cashierUserId,
                cashSessionId = cashSessionId,
                customerId = transaction.customerServerId,
                paymentMethod = syncMethod,
                subtotal = transaction.subtotal.toBigDecimal(),
                discount = transaction.discount.toBigDecimal(),
                total = transaction.total.toBigDecimal(),
                paidAmount = transaction.paidAmount.toBigDecimal(),
                remainingAmount = transaction.remainingAmount.toBigDecimal(),
                occurredAt = transaction.occurredAt.toOffsetDateTimeString(),
                note = null,
                items = items,
                payments = syncPayments,
                taxEnabled = transaction.taxEnabled,
                taxMode = transaction.taxMode,
                taxRate = transaction.taxRate.toBigDecimal(),
                taxLabel = transaction.taxLabel,
                taxableBase = transaction.taxableBase.toBigDecimal(),
                taxAmount = transaction.taxAmount.toBigDecimal(),
                roundingAmount = transaction.roundingAmount.toBigDecimal()
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
                        lastErrorCode = null,
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
        errorCode: String? = null,
        retryAfterSeconds: Long? = null,
    ): OfflineCheckoutSyncResult.Failed {
        val now = clockMillis()
        val effectiveRetryIncrement = if (kind == OfflineCheckoutSyncFailureKind.AUTH) 0 else retryIncrement
        database.withTransaction {
            val queue = database.syncQueueDao().getLatestForEntity(
                entityLocalId = transactionLocalId,
                entityType = SyncEntityType.TRANSACTION
            )
            val status = resolveSyncFailureStatus(
                kind = kind,
                existingRetryCount = queue?.retryCount ?: 0,
                retryIncrement = effectiveRetryIncrement,
            )
            updateFailureStatusWithChildren(
                transactionLocalId = transactionLocalId,
                queueId = queue?.id,
                status = status,
                message = message,
                retryIncrement = effectiveRetryIncrement,
                errorCode = errorCode,
                now = now,
            )
        }
        return OfflineCheckoutSyncResult.Failed(message, kind, retryAfterSeconds)
    }

    private suspend fun updateFailureStatusWithChildren(
        transactionLocalId: Long,
        queueId: Long?,
        status: SyncStatus,
        message: String,
        retryIncrement: Int,
        errorCode: String? = null,
        now: Long = clockMillis(),
    ) {
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
        queueId?.let { id ->
            database.syncQueueDao().updateStatus(
                id = id,
                status = status,
                lastError = message,
                lastErrorCode = errorCode,
                retryIncrement = retryIncrement,
                updatedAt = now
            )
        }
    }

    private fun requiresCustomer(transaction: LocalTransactionEntity): Boolean {
        return transaction.remainingAmount.isPositive || transaction.status.equals("UNPAID", ignoreCase = true)
    }

    private suspend fun paymentMethodForSync(transaction: LocalTransactionEntity): String {
        val payment = database.paymentDao().getByTransaction(transaction.localId).firstOrNull()
        return payment?.method?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: if (transaction.remainingAmount.isPositive) "hutang" else "tunai"
    }

    private fun Long.toOffsetDateTimeString(): String =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.of("Asia/Jakarta"))
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
            val kind: OfflineCheckoutSyncFailureKind,
            val retryAfterSeconds: Long? = null,
        ) : EnsureCashSessionResult
    }

    private companion object {
        const val DEFAULT_AUTO_SYNC_LIMIT = 10
    }
}

sealed interface OfflineCheckoutSyncResult {
    data class Success(
        val serverTransactionId: String,
        val syncStatus: String
    ) : OfflineCheckoutSyncResult

    data class Failed(
        val message: String,
        val kind: OfflineCheckoutSyncFailureKind = OfflineCheckoutSyncFailureKind.UNKNOWN,
        val retryAfterSeconds: Long? = null,
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
    val validationFailureCount: Int = 0,
    val maxRetryAfterSeconds: Long? = null,
) {
    val hasAuthFailure: Boolean get() = authFailureCount > 0
    val hasTransientFailure: Boolean get() = transientFailureCount > 0
}
