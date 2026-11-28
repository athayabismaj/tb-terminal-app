package com.tbterminal.app.data.repository

import com.tbterminal.app.data.local.dao.CashExpenseDao
import com.tbterminal.app.data.local.dao.CashSessionDao
import com.tbterminal.app.data.local.dao.PendingSubmissionDao
import com.tbterminal.app.data.local.dao.SyncQueueDao
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineSessionAgeAlert
import com.tbterminal.app.data.sync.OfflineSessionAgePolicy
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.data.sync.isCashierPendingSubmissionScope
import com.tbterminal.app.data.sync.isPendingSubmissionActive
import com.tbterminal.app.data.sync.partitionSyncQueueByUser
import com.tbterminal.app.ui.cashier.sync.CashierSyncErrorFormatter
import com.tbterminal.app.ui.common.toRupiahText
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class CashierSyncItemKind {
    TRANSACTION,
    OPEN_CASH_SESSION,
    CLOSE_CASH_SESSION,
    CASH_EXPENSE,
    CASH_IN,
    ONLINE_CHECKOUT,
    PAY_DEBT,
    VOID_TRANSACTION,
    REFUND_TRANSACTION,
    RECEIVABLE_PAYMENT,
}

enum class CashierSyncItemStatus {
    PENDING,
    SENDING,
    FAILED,
    NEEDS_CHECK,
}

data class CashierSyncItemUi(
    val id: String,
    val kind: CashierSyncItemKind,
    val title: String,
    val summary: String,
    val createdAtMillis: Long,
    val status: CashierSyncItemStatus,
    val failureReason: String? = null,
    val isPendingSubmission: Boolean = false,
)

data class CashierSyncSnapshot(
    val isServerOnline: Boolean = false,
    val isInternetOnline: Boolean = true,
    val backendStatus: BackendStatus = BackendStatus.UNKNOWN,
    val lastSuccessfulSyncAtMillis: Long? = null,
    val pendingCount: Int = 0,
    val sendingCount: Int = 0,
    val failedCount: Int = 0,
    val needsCheckCount: Int = 0,
    val items: List<CashierSyncItemUi> = emptyList(),
    val offlineSessionAgeAlert: OfflineSessionAgeAlert? = null,
    val otherUsersPendingSyncCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CashierSyncRepository(
    private val offlineStatusRepository: OfflineStatusRepository,
    private val syncQueueDao: SyncQueueDao,
    private val cashSessionDao: CashSessionDao,
    private val transactionDao: TransactionDao,
    private val cashExpenseDao: CashExpenseDao,
    private val pendingSubmissionDao: PendingSubmissionDao,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource,
    private val activeUserIdFlow: Flow<String?>,
    private val activeUserIdProvider: () -> String?,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val monotonicClockMillis: () -> Long = clockMillis,
    private val bootCountProvider: () -> Long? = { 1L },
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private data class UserScopedData(
        val ownerUserId: String?,
        val pendingSubmissions: List<PendingSubmissionEntity>,
        val lastSuccessfulSyncAtMillis: Long?,
    )

    private val userScopedFlow: Flow<UserScopedData> = activeUserIdFlow
        .distinctUntilChanged()
        .flatMapLatest { rawUserId ->
            val resolvedId = (rawUserId ?: activeUserIdProvider())
                ?.trim()
                ?.takeIf(String::isNotBlank)
            if (resolvedId == null) {
                flowOf(
                    UserScopedData(
                        ownerUserId = null,
                        pendingSubmissions = emptyList(),
                        lastSuccessfulSyncAtMillis = null,
                    )
                )
            } else {
                combine(
                    pendingSubmissionDao.observeForOwner(resolvedId),
                    localAppSettingsDataSource.observeLastSuccessfulSyncAt(resolvedId),
                ) { submissions, lastSyncAt ->
                    UserScopedData(
                        ownerUserId = resolvedId,
                        pendingSubmissions = submissions,
                        lastSuccessfulSyncAtMillis = lastSyncAt,
                    )
                }
            }
        }

    fun observeSnapshot(): Flow<CashierSyncSnapshot> {
        return combine(
            offlineStatusRepository.status,
            syncQueueDao.observeQueue(),
            cashSessionDao.observeSessions(),
            userScopedFlow,
        ) { status, queue, sessions, userScoped ->
            buildSnapshot(
                status = status,
                queue = queue,
                sessions = sessions,
                userScoped = userScoped,
            )
        }
    }

    private suspend fun buildSnapshot(
        status: OfflineStatus,
        queue: List<SyncQueueEntity>,
        sessions: List<LocalCashSessionEntity>,
        userScoped: UserScopedData,
    ): CashierSyncSnapshot {
        val activeUserId = userScoped.ownerUserId ?: activeUserIdProvider()?.trim()?.takeIf(String::isNotBlank)
        val partitioned = partitionSyncQueueByUser(
            queue = queue,
            activeUserId = activeUserId,
            sessions = sessions,
            cashSessionDao = cashSessionDao,
            transactionDao = transactionDao,
            cashExpenseDao = cashExpenseDao,
            applyIsolationFilter = true,
        )

        val queueUiItems = partitioned.ownItems
            .filter { it.status != SyncStatus.SYNCED && it.entityType.isSupported }
            .map { mapQueueItem(it, sessions) }

        val wallNow = clockMillis()
        val elapsedNow = monotonicClockMillis()
        val bootNow = bootCountProvider()
        val submissionUiItems = userScoped.pendingSubmissions
            .filter { entity ->
                isCashierPendingSubmissionScope(entity.scope) &&
                    isPendingSubmissionActive(
                        entity = entity,
                        wallNow = wallNow,
                        elapsedNow = elapsedNow,
                        bootNow = bootNow,
                    )
            }
            .map(::mapPendingSubmissionItem)

        val allItems = (queueUiItems + submissionUiItems)
            .sortedWith(
                compareBy<CashierSyncItemUi> { it.status.sortPriority() }
                    .thenByDescending { it.createdAtMillis }
            )

        val ownUnsyncedSessions = sessions.filter { session ->
            session.deletedAt == null &&
                session.serverId.isNullOrBlank() &&
                session.syncStatus != SyncStatus.SYNCED &&
                activeUserId != null &&
                session.cashierUserId.trim() == activeUserId
        }
        val ageAlert = if (ownUnsyncedSessions.isEmpty()) {
            null
        } else {
            OfflineSessionAgePolicy.evaluateUnsyncedSessions(
                sessions = ownUnsyncedSessions,
                nowMillis = wallNow,
            )
        }

        val isServerOnline = status.isInternetOnline && status.backendStatus == BackendStatus.CONNECTED

        return CashierSyncSnapshot(
            isServerOnline = isServerOnline,
            isInternetOnline = status.isInternetOnline,
            backendStatus = status.backendStatus,
            lastSuccessfulSyncAtMillis = userScoped.lastSuccessfulSyncAtMillis,
            pendingCount = allItems.count { it.status == CashierSyncItemStatus.PENDING },
            sendingCount = allItems.count { it.status == CashierSyncItemStatus.SENDING },
            failedCount = allItems.count { it.status == CashierSyncItemStatus.FAILED },
            needsCheckCount = allItems.count { it.status == CashierSyncItemStatus.NEEDS_CHECK },
            items = allItems,
            offlineSessionAgeAlert = ageAlert,
            otherUsersPendingSyncCount = partitioned.otherUsersPendingSyncCount,
        )
    }

    private suspend fun mapQueueItem(
        item: SyncQueueEntity,
        sessions: List<LocalCashSessionEntity>,
    ): CashierSyncItemUi {
        val sessionsById = sessions.associateBy { it.localId }
        val status = when (item.status) {
            SyncStatus.PENDING -> CashierSyncItemStatus.PENDING
            SyncStatus.SYNCING -> CashierSyncItemStatus.SENDING
            SyncStatus.FAILED -> CashierSyncItemStatus.FAILED
            SyncStatus.CONFLICT -> CashierSyncItemStatus.NEEDS_CHECK
            SyncStatus.SYNCED -> CashierSyncItemStatus.PENDING
        }

        val (kind, title, summary, createdAt) = when (item.entityType) {
            SyncEntityType.TRANSACTION -> {
                val tx = transactionDao.getByLocalId(item.entityLocalId)
                val code = tx?.transactionCode?.takeIf(String::isNotBlank) ?: "Transaksi #${item.entityLocalId}"
                val totalText = tx?.total?.toBigDecimal()?.toRupiahText() ?: "-"
                val paymentStatus = if (tx != null && tx.remainingAmount.isPositive) "Piutang/DP" else "Lunas"
                ItemMeta(
                    kind = CashierSyncItemKind.TRANSACTION,
                    title = "Transaksi Offline",
                    summary = "$code · $totalText ($paymentStatus)",
                    createdAtMillis = tx?.occurredAt ?: item.createdAt,
                )
            }

            SyncEntityType.CASH_SESSION -> {
                val session = sessionsById[item.entityLocalId] ?: cashSessionDao.getByLocalId(item.entityLocalId)
                if (item.operation == SyncOperation.UPDATE) {
                    val actualText = (session?.actualCash ?: ScaledDecimal2.ZERO).toBigDecimal().toRupiahText()
                    ItemMeta(
                        kind = CashierSyncItemKind.CLOSE_CASH_SESSION,
                        title = "Tutup Sesi Kas",
                        summary = "Kas fisik akhir $actualText",
                        createdAtMillis = session?.closedAt ?: item.createdAt,
                    )
                } else {
                    val openingText = (session?.startingCash ?: ScaledDecimal2.ZERO).toBigDecimal().toRupiahText()
                    ItemMeta(
                        kind = CashierSyncItemKind.OPEN_CASH_SESSION,
                        title = "Buka Sesi Kas",
                        summary = "Modal awal $openingText",
                        createdAtMillis = session?.openedAt ?: item.createdAt,
                    )
                }
            }

            SyncEntityType.CASH_EXPENSE -> {
                val expense = cashExpenseDao.getByLocalId(item.entityLocalId)
                val isCashIn = expense?.category.orEmpty().let {
                    it.startsWith("CASH_IN", ignoreCase = true) || it.startsWith("KAS_MASUK", ignoreCase = true)
                }
                val amountText = (expense?.amount ?: ScaledDecimal2.ZERO).toBigDecimal().toRupiahText()
                val note = expense?.description?.trim()?.takeIf(String::isNotBlank)
                val fallbackLabel = if (isCashIn) "Kas masuk" else "Pengeluaran kas"
                ItemMeta(
                    kind = if (isCashIn) CashierSyncItemKind.CASH_IN else CashierSyncItemKind.CASH_EXPENSE,
                    title = if (isCashIn) "Kas Masuk Offline" else "Pengeluaran Kas Offline",
                    summary = "${note ?: fallbackLabel} · $amountText",
                    createdAtMillis = expense?.occurredAt ?: item.createdAt,
                )
            }

            SyncEntityType.UNSUPPORTED -> ItemMeta(
                kind = CashierSyncItemKind.TRANSACTION,
                title = "Data Lokal",
                summary = "Item #${item.entityLocalId}",
                createdAtMillis = item.createdAt,
            )
        }

        val failureReason = CashierSyncErrorFormatter.formatReason(
            syncStatus = item.status,
            lastError = item.lastError,
            lastErrorCode = item.lastErrorCode,
        )

        return CashierSyncItemUi(
            id = "queue-${item.id}",
            kind = kind,
            title = title,
            summary = summary,
            createdAtMillis = createdAt,
            status = status,
            failureReason = failureReason,
            isPendingSubmission = false,
        )
    }

    private fun mapPendingSubmissionItem(entity: PendingSubmissionEntity): CashierSyncItemUi {
        val submissionStatus = PendingSubmissionStatus.fromRaw(entity.status)
        val status = when (submissionStatus) {
            PendingSubmissionStatus.IN_FLIGHT -> CashierSyncItemStatus.SENDING
            PendingSubmissionStatus.AMBIGUOUS, null -> CashierSyncItemStatus.NEEDS_CHECK
        }
        val intentObj = parseJsonObjectOrNull(entity.intentJson)
        val payloadObj = entity.payloadJson?.let(::parseJsonObjectOrNull)

        val (kind, title, summary) = when {
            entity.scope == PendingSubmissionScopes.CHECKOUT -> {
                val cmdObj = payloadObj?.get("command")?.jsonObjectOrNull()
                val cartCount = payloadObj?.get("cartItems")?.jsonArrayOrNull()?.size ?: 0
                val amount = cmdObj?.stringField("amountPaid")?.toBigDecimalOrNull()?.toRupiahText()
                val summaryText = buildList {
                    if (cartCount > 0) add("$cartCount item")
                    if (amount != null) add("Bayar $amount")
                }.joinToString(" · ").ifBlank { "Checkout kasir online" }
                Triple(CashierSyncItemKind.ONLINE_CHECKOUT, "Checkout Online", summaryText)
            }

            entity.scope == PendingSubmissionScopes.CASHIER_PAY_DEBT -> {
                val amount = intentObj?.stringField("amountInput")?.toBigDecimalOrNull()?.toRupiahText()
                val method = intentObj?.stringField("method")?.uppercase()?.takeIf(String::isNotBlank)
                val summaryText = listOfNotNull(amount, method).joinToString(" · ")
                    .ifBlank { "Pelunasan hutang transaksi" }
                Triple(CashierSyncItemKind.PAY_DEBT, "Bayar Hutang Transaksi", summaryText)
            }

            entity.scope == PendingSubmissionScopes.CASHIER_VOID -> {
                val reason = intentObj?.stringField("reason")?.takeIf(String::isNotBlank)
                val summaryText = reason ?: "Pembatalan transaksi (void)"
                Triple(CashierSyncItemKind.VOID_TRANSACTION, "Void Transaksi", summaryText)
            }

            entity.scope == PendingSubmissionScopes.CASHIER_REFUND -> {
                val reason = intentObj?.stringField("reason")?.takeIf(String::isNotBlank)
                val summaryText = reason ?: "Pengembalian transaksi (refund)"
                Triple(CashierSyncItemKind.REFUND_TRANSACTION, "Refund Transaksi", summaryText)
            }

            else -> {
                val amount = intentObj?.stringField("normalizedAmount")?.toBigDecimalOrNull()?.toRupiahText()
                val invoiceCount = intentObj?.get("invoiceIds")?.jsonArrayOrNull()?.size ?: 0
                val summaryText = buildList {
                    if (amount != null) add(amount)
                    if (invoiceCount > 0) add("$invoiceCount nota")
                }.joinToString(" · ").ifBlank { "Pembayaran piutang pelanggan" }
                Triple(CashierSyncItemKind.RECEIVABLE_PAYMENT, "Pembayaran Piutang", summaryText)
            }
        }

        val reasonText = if (status == CashierSyncItemStatus.NEEDS_CHECK) {
            "Status di server belum pasti karena koneksi sempat terputus. Cek dulu di riwayat transaksi sebelum kirim ulang."
        } else {
            "Pengiriman sebelumnya sempat terputus. Cek dulu hasilnya sebelum kirim ulang."
        }

        return CashierSyncItemUi(
            id = "pending-${entity.ownerUserId}-${entity.scope}",
            kind = kind,
            title = title,
            summary = summary,
            createdAtMillis = entity.createdAt,
            status = status,
            failureReason = reasonText,
            isPendingSubmission = true,
        )
    }

    private fun parseJsonObjectOrNull(raw: String): JsonObject? {
        return runCatching { jsonParser.parseToJsonElement(raw).jsonObject }.getOrNull()
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? {
        return runCatching { jsonObject }.getOrNull()
    }

    private fun kotlinx.serialization.json.JsonElement.jsonArrayOrNull() =
        runCatching { jsonArray }.getOrNull()

    private fun JsonObject.stringField(key: String): String? {
        return this[key]?.let { element ->
            runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
        }?.trim()
    }

    private data class ItemMeta(
        val kind: CashierSyncItemKind,
        val title: String,
        val summary: String,
        val createdAtMillis: Long,
    )
}

private fun CashierSyncItemStatus.sortPriority(): Int = when (this) {
    CashierSyncItemStatus.FAILED -> 0
    CashierSyncItemStatus.NEEDS_CHECK -> 1
    CashierSyncItemStatus.SENDING -> 2
    CashierSyncItemStatus.PENDING -> 3
}
