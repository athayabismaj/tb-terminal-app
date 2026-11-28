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
import com.tbterminal.app.data.sync.OfflineTimeConflictPolicy
import com.tbterminal.app.data.sync.TrustedClockStatus
import com.tbterminal.app.data.sync.TrustedOfflineClock
import com.tbterminal.app.data.sync.isCashierPendingSubmissionScope
import com.tbterminal.app.data.sync.isPendingSubmissionActive
import com.tbterminal.app.data.sync.partitionSyncQueueByUser
import com.tbterminal.app.ui.cashier.sync.CashierSyncErrorFormatter
import com.tbterminal.app.ui.common.toRupiahText
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
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

enum class CashierClockWarningKind {
    DEVICE_CLOCK_SKEWED,
    UNCALIBRATED_AFTER_REBOOT,
}

data class CashierClockWarning(
    val kind: CashierClockWarningKind,
    val message: String,
)

data class CashierSyncItemUi(
    val id: String,
    val kind: CashierSyncItemKind,
    val title: String,
    val summary: String,
    val createdAtMillis: Long,
    val status: CashierSyncItemStatus,
    val failureReason: String? = null,
    val isPendingSubmission: Boolean = false,
    val isConflict: Boolean = false,
    val isDeferredByClock: Boolean = false,
    val blockingCount: Int = 0,
    val blockedByLabel: String? = null,
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
    val blockedCount: Int = 0,
    val deferredClockCount: Int = 0,
    val items: List<CashierSyncItemUi> = emptyList(),
    val offlineSessionAgeAlert: OfflineSessionAgeAlert? = null,
    val clockWarning: CashierClockWarning? = null,
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
    private val trustedOfflineClock: TrustedOfflineClock? = null,
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
        val clockUpdatesFlow = trustedOfflineClock?.anchorUpdates ?: flowOf(null)
        return combine(
            offlineStatusRepository.status,
            syncQueueDao.observeQueue(),
            cashSessionDao.observeSessions(),
            userScopedFlow,
            clockUpdatesFlow,
        ) { status, queue, sessions, userScoped, _ ->
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

        val sessionsById = sessions.associateBy { it.localId }
        val supportedQueueItems = partitioned.ownItems
            .filter { it.status != SyncStatus.SYNCED && it.entityType.isSupported }
        val resolvedItems = supportedQueueItems.map { item ->
            resolveQueueItemMeta(item, sessionsById)
        }
        val blockingByQueueId = CashierSyncBlockingAnalyzer.analyze(
            resolvedItems.map { resolved ->
                SyncBlockingNode(
                    queueId = resolved.queue.id,
                    entityType = resolved.queue.entityType,
                    entityLocalId = resolved.queue.entityLocalId,
                    operation = resolved.queue.operation,
                    status = resolved.queue.status,
                    lastErrorCode = resolved.queue.lastErrorCode,
                    sessionLocalId = resolved.sessionLocalId,
                    sessionServerId = resolved.sessionServerId,
                    sessionOpenedAtMillis = resolved.sessionOpenedAtMillis,
                    createdAtMillis = resolved.meta.createdAtMillis,
                    displayLabel = resolved.displayLabel,
                )
            }
        )
        val queueUiItems = resolvedItems.map { resolved ->
            mapResolvedQueueItem(
                resolved = resolved,
                blockingInfo = blockingByQueueId[resolved.queue.id],
            )
        }

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
                nowMillis = trustedOfflineClock?.nowMillis() ?: wallNow,
            )
        }
        val clockWarning = buildClockWarning()

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
            blockedCount = queueUiItems.count { !it.blockedByLabel.isNullOrBlank() },
            deferredClockCount = queueUiItems.count { it.isDeferredByClock },
            items = allItems,
            offlineSessionAgeAlert = ageAlert,
            clockWarning = clockWarning,
            otherUsersPendingSyncCount = partitioned.otherUsersPendingSyncCount,
        )
    }

    private suspend fun buildClockWarning(): CashierClockWarning? {
        val reading = trustedOfflineClock?.readNow() ?: return null
        return when (reading.status) {
            TrustedClockStatus.DEVICE_CLOCK_SKEWED -> {
                val diffMinutes = max(1L, abs(reading.skewMillis ?: 0L) / 60_000L)
                CashierClockWarning(
                    kind = CashierClockWarningKind.DEVICE_CLOCK_SKEWED,
                    message = "Jam HP melenceng ±$diffMinutes menit dari jam server. Data offline tetap memakai jam terpercaya; sesuaikan jam perangkat.",
                )
            }
            TrustedClockStatus.UNCALIBRATED_AFTER_REBOOT -> CashierClockWarning(
                kind = CashierClockWarningKind.UNCALIBRATED_AFTER_REBOOT,
                message = "Jam belum terkalibrasi sejak perangkat dinyalakan ulang. Hubungkan ke server untuk menyelaraskan jam.",
            )
            TrustedClockStatus.CALIBRATED,
            TrustedClockStatus.NO_ANCHOR -> null
        }
    }

    private suspend fun resolveQueueItemMeta(
        item: SyncQueueEntity,
        sessionsById: Map<Long, LocalCashSessionEntity>,
    ): ResolvedQueueItem {
        return when (item.entityType) {
            SyncEntityType.TRANSACTION -> {
                val tx = transactionDao.getByLocalId(item.entityLocalId)
                val code = tx?.transactionCode?.takeIf(String::isNotBlank) ?: "Transaksi #${item.entityLocalId}"
                val totalText = tx?.total?.toBigDecimal()?.toRupiahText() ?: "-"
                val paymentStatus = if (tx != null && tx.remainingAmount.isPositive) "Piutang/DP" else "Lunas"
                val session = tx?.cashSessionLocalId?.let { sid ->
                    sessionsById[sid] ?: cashSessionDao.getByLocalId(sid)
                }
                val occurredAt = tx?.occurredAt ?: item.createdAt
                ResolvedQueueItem(
                    queue = item,
                    meta = ItemMeta(
                        kind = CashierSyncItemKind.TRANSACTION,
                        title = "Transaksi Offline",
                        summary = "$code · $totalText ($paymentStatus)",
                        createdAtMillis = occurredAt,
                    ),
                    sessionLocalId = tx?.cashSessionLocalId,
                    sessionServerId = session?.serverId ?: tx?.cashSessionServerId,
                    sessionOpenedAtMillis = session?.openedAt,
                    displayLabel = "Transaksi Offline · $code",
                )
            }

            SyncEntityType.CASH_SESSION -> {
                val session = sessionsById[item.entityLocalId] ?: cashSessionDao.getByLocalId(item.entityLocalId)
                if (item.operation == SyncOperation.UPDATE) {
                    val actualText = (session?.actualCash ?: ScaledDecimal2.ZERO).toBigDecimal().toRupiahText()
                    val closedAt = session?.closedAt ?: item.createdAt
                    ResolvedQueueItem(
                        queue = item,
                        meta = ItemMeta(
                            kind = CashierSyncItemKind.CLOSE_CASH_SESSION,
                            title = "Tutup Sesi Kas",
                            summary = "Kas fisik akhir $actualText",
                            createdAtMillis = closedAt,
                        ),
                        sessionLocalId = item.entityLocalId,
                        sessionServerId = session?.serverId,
                        sessionOpenedAtMillis = session?.openedAt,
                        displayLabel = "Tutup Sesi Kas (${formatShortTime(closedAt)})",
                    )
                } else {
                    val openingText = (session?.startingCash ?: ScaledDecimal2.ZERO).toBigDecimal().toRupiahText()
                    val openedAt = session?.openedAt ?: item.createdAt
                    ResolvedQueueItem(
                        queue = item,
                        meta = ItemMeta(
                            kind = CashierSyncItemKind.OPEN_CASH_SESSION,
                            title = "Buka Sesi Kas",
                            summary = "Modal awal $openingText",
                            createdAtMillis = openedAt,
                        ),
                        sessionLocalId = item.entityLocalId,
                        sessionServerId = session?.serverId,
                        sessionOpenedAtMillis = session?.openedAt,
                        displayLabel = "Buka Sesi Kas (${formatShortTime(openedAt)})",
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
                val title = if (isCashIn) "Kas Masuk Offline" else "Pengeluaran Kas Offline"
                val session = expense?.cashSessionLocalId?.let { sid ->
                    sessionsById[sid] ?: cashSessionDao.getByLocalId(sid)
                }
                val occurredAt = expense?.occurredAt ?: item.createdAt
                ResolvedQueueItem(
                    queue = item,
                    meta = ItemMeta(
                        kind = if (isCashIn) CashierSyncItemKind.CASH_IN else CashierSyncItemKind.CASH_EXPENSE,
                        title = title,
                        summary = "${note ?: fallbackLabel} · $amountText",
                        createdAtMillis = occurredAt,
                    ),
                    sessionLocalId = expense?.cashSessionLocalId,
                    sessionServerId = session?.serverId ?: expense?.cashSessionServerId,
                    sessionOpenedAtMillis = session?.openedAt,
                    displayLabel = "$title · ${note ?: fallbackLabel}",
                )
            }

            SyncEntityType.UNSUPPORTED -> ResolvedQueueItem(
                queue = item,
                meta = ItemMeta(
                    kind = CashierSyncItemKind.TRANSACTION,
                    title = "Data Lokal",
                    summary = "Item #${item.entityLocalId}",
                    createdAtMillis = item.createdAt,
                ),
                sessionLocalId = null,
                sessionServerId = null,
                sessionOpenedAtMillis = null,
                displayLabel = "Data Lokal #${item.entityLocalId}",
            )
        }
    }

    private fun mapResolvedQueueItem(
        resolved: ResolvedQueueItem,
        blockingInfo: SyncBlockingInfo?,
    ): CashierSyncItemUi {
        val item = resolved.queue
        val status = when (item.status) {
            SyncStatus.PENDING -> CashierSyncItemStatus.PENDING
            SyncStatus.SYNCING -> CashierSyncItemStatus.SENDING
            SyncStatus.FAILED -> CashierSyncItemStatus.FAILED
            SyncStatus.CONFLICT -> CashierSyncItemStatus.NEEDS_CHECK
            SyncStatus.SYNCED -> CashierSyncItemStatus.PENDING
        }

        val isDeferredByClock = item.status == SyncStatus.PENDING &&
            OfflineTimeConflictPolicy.isOccurredAtInvalidCode(item.lastErrorCode)
        val deferredUntilMillis = if (isDeferredByClock) {
            OfflineTimeConflictPolicy.eligibleAtMillis(resolved.meta.createdAtMillis)
        } else {
            null
        }

        val failureReason = CashierSyncErrorFormatter.formatReason(
            syncStatus = item.status,
            lastError = item.lastError,
            lastErrorCode = item.lastErrorCode,
            deferredUntilMillis = deferredUntilMillis,
        )

        return CashierSyncItemUi(
            id = "queue-${item.id}",
            kind = resolved.meta.kind,
            title = resolved.meta.title,
            summary = resolved.meta.summary,
            createdAtMillis = resolved.meta.createdAtMillis,
            status = status,
            failureReason = failureReason,
            isPendingSubmission = false,
            isConflict = item.status == SyncStatus.CONFLICT,
            isDeferredByClock = isDeferredByClock,
            blockingCount = blockingInfo?.blockingCount ?: 0,
            blockedByLabel = blockingInfo?.blockedByRootLabel,
        )
    }

    private fun formatShortTime(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(SHORT_TIME_FORMATTER)
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

    private data class ResolvedQueueItem(
        val queue: SyncQueueEntity,
        val meta: ItemMeta,
        val sessionLocalId: Long?,
        val sessionServerId: String?,
        val sessionOpenedAtMillis: Long?,
        val displayLabel: String,
    )

    private companion object {
        val SHORT_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID"))
    }
}

private fun CashierSyncItemStatus.sortPriority(): Int = when (this) {
    CashierSyncItemStatus.FAILED -> 0
    CashierSyncItemStatus.NEEDS_CHECK -> 1
    CashierSyncItemStatus.SENDING -> 2
    CashierSyncItemStatus.PENDING -> 3
}
