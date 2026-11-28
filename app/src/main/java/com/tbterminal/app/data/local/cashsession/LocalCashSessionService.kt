package com.tbterminal.app.data.local.cashsession

import androidx.room.withTransaction
import com.tbterminal.app.data.local.checkout.exceedsMaxBaseQuantityScale
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.id.LocalIdGenerator
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.BigDecimalStringSerializer
import com.tbterminal.app.navigation.AppAccessPolicy
import java.math.BigDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalCashSessionService(
    private val database: TbTerminalDatabase,
    private val deviceId: String? = null,
    private val roleProvider: () -> String? = { "KASIR" },
) {
    suspend fun openLocalSession(
        cashierUserId: String,
        startingCash: BigDecimal,
        openingNote: String? = null,
        adoptRemoteIfActive: Boolean = false,
    ): LocalCashSessionResult {
        if (!AppAccessPolicy.canOpenCashSession(roleProvider())) {
            return LocalCashSessionResult.Failed("Hanya kasir yang dapat membuka sesi kas.")
        }
        val validationError = validateOpenInput(cashierUserId, startingCash)
            ?: validateSingleOpenSession(cashierUserId)
        if (validationError != null) {
            return LocalCashSessionResult.Failed(validationError)
        }

        return runCatching {
            database.withTransaction {
                val now = System.currentTimeMillis()
                val startingScaled = ScaledDecimal2.ofExact(startingCash)
                val localId = database.cashSessionDao().upsert(
                    LocalCashSessionEntity(
                        serverId = null,
                        clientGeneratedId = LocalIdGenerator.clientGeneratedId("SESSION"),
                        deviceId = deviceId,
                        cashierUserId = cashierUserId.trim(),
                        status = CASH_SESSION_OPEN,
                        openedAt = now,
                        closedAt = null,
                        startingCash = startingScaled,
                        expectedCash = startingScaled,
                        actualCash = null,
                        difference = null,
                        openingNote = openingNote?.trim()?.takeIf(String::isNotBlank),
                        closingNote = null,
                        syncStatus = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        deletedAt = null
                    )
                )
                val session = database.cashSessionDao().getByLocalId(localId)
                    ?: error("Sesi kas lokal gagal dibaca ulang.")
                database.syncQueueDao().enqueue(
                    SyncQueueEntity(
                        entityType = SyncEntityType.CASH_SESSION,
                        entityLocalId = localId,
                        operation = SyncOperation.CREATE,
                        payloadJson = session.toOpenSyncPayloadJson(adoptRemoteIfActive = adoptRemoteIfActive),
                        status = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                LocalCashSessionResult.Success(session)
            }
        }.getOrElse { error ->
            LocalCashSessionResult.Failed(error.message ?: "Sesi kas lokal gagal dibuka.")
        }
    }

    suspend fun closeLocalSession(
        localId: Long,
        closingCash: BigDecimal,
        closingNote: String? = null
    ): LocalCashSessionResult {
        if (localId <= 0L) return LocalCashSessionResult.Failed("Sesi kas lokal belum valid.")
        if (closingCash < BigDecimal.ZERO) {
            return LocalCashSessionResult.Failed("Kas fisik akhir tidak boleh kurang dari nol.")
        }
        if (exceedsMaxBaseQuantityScale(closingCash)) {
            return LocalCashSessionResult.Failed("Kas fisik akhir maksimal 2 angka desimal.")
        }

        return runCatching {
            database.withTransaction {
                val session = database.cashSessionDao().getByLocalId(localId)
                    ?: return@withTransaction LocalCashSessionResult.Failed("Sesi kas lokal tidak ditemukan.")
                if (!session.status.equals(CASH_SESSION_OPEN, ignoreCase = true)) {
                    return@withTransaction LocalCashSessionResult.Failed("Sesi kas lokal sudah ditutup.")
                }

                val now = System.currentTimeMillis()
                val cashSales = database.paymentDao().sumCashPaymentsByCashSession(localId)
                val cashIn = database.cashExpenseDao().sumCashInByCashSession(localId)
                val cashOut = database.cashExpenseDao().sumCashOutByCashSession(localId)
                val expectedCash = session.startingCash + cashSales + cashIn - cashOut
                val actualCash = ScaledDecimal2.ofExact(closingCash)
                database.cashSessionDao().closeSession(
                    localId = localId,
                    status = CASH_SESSION_CLOSED,
                    closedAt = now,
                    expectedCash = expectedCash,
                    actualCash = actualCash,
                    difference = actualCash - expectedCash,
                    closingNote = closingNote?.trim()?.takeIf(String::isNotBlank),
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now
                )

                val closedSession = database.cashSessionDao().getByLocalId(localId)
                    ?: error("Sesi kas lokal gagal dibaca ulang.")
                database.syncQueueDao().enqueue(
                    SyncQueueEntity(
                        entityType = SyncEntityType.CASH_SESSION,
                        entityLocalId = localId,
                        operation = SyncOperation.UPDATE,
                        payloadJson = closedSession.toCloseSyncPayloadJson(),
                        status = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                LocalCashSessionResult.Success(closedSession)
            }
        }.getOrElse { error ->
            LocalCashSessionResult.Failed(error.message ?: "Sesi kas lokal gagal ditutup.")
        }
    }

    suspend fun validateSingleOpenSession(cashierUserId: String): String? {
        val normalizedCashierId = cashierUserId.trim()
        if (normalizedCashierId.isBlank()) return "Kasir sesi lokal belum valid."

        val openByCashier = database.cashSessionDao().getLatestByStatusAndCashier(
            status = CASH_SESSION_OPEN,
            cashierUserId = normalizedCashierId
        )
        if (openByCashier != null) return "Masih ada sesi kas lokal yang terbuka."

        return null
    }

    private fun validateOpenInput(cashierUserId: String, startingCash: BigDecimal): String? {
        if (cashierUserId.isBlank()) return "Kasir sesi lokal belum valid."
        if (startingCash < BigDecimal.ZERO) return "Modal awal tidak boleh kurang dari nol."
        if (exceedsMaxBaseQuantityScale(startingCash)) return "Modal awal maksimal 2 angka desimal."
        return null
    }

    internal fun LocalCashSessionEntity.toOpenSyncPayloadJson(
        adoptRemoteIfActive: Boolean = false,
    ): String {
        val payload = LocalCashSessionOpenSyncPayload(
            clientGeneratedId = clientGeneratedId,
            deviceId = deviceId.orEmpty(),
            cashierUserId = cashierUserId,
            openedAt = openedAt,
            startingCash = startingCash.toBigDecimal(),
            openingNote = openingNote,
            adoptRemoteIfActive = adoptRemoteIfActive,
        )
        return SYNC_PAYLOAD_JSON.encodeToString(payload)
    }

    internal fun LocalCashSessionEntity.toCloseSyncPayloadJson(): String {
        val payload = LocalCashSessionCloseSyncPayload(
            clientGeneratedId = clientGeneratedId,
            deviceId = deviceId.orEmpty(),
            cashierUserId = cashierUserId,
            serverId = serverId.orEmpty(),
            closedAt = closedAt ?: 0L,
            actualCash = (actualCash ?: ScaledDecimal2.ZERO).toBigDecimal(),
            expectedCash = (expectedCash ?: ScaledDecimal2.ZERO).toBigDecimal(),
            difference = (difference ?: ScaledDecimal2.ZERO).toBigDecimal(),
            closingNote = closingNote,
        )
        return SYNC_PAYLOAD_JSON.encodeToString(payload)
    }

    internal companion object {
        const val CASH_SESSION_OPEN = "OPEN"
        const val CASH_SESSION_CLOSED = "CLOSED"
        val SYNC_PAYLOAD_JSON: Json = Json {
            encodeDefaults = false
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

sealed interface LocalCashSessionResult {
    data class Success(val session: LocalCashSessionEntity) : LocalCashSessionResult
    data class Failed(val message: String) : LocalCashSessionResult
}

@Serializable
internal data class LocalCashSessionOpenSyncPayload(
    val clientGeneratedId: String,
    val deviceId: String,
    val cashierUserId: String,
    val openedAt: Long,
    @Serializable(with = BigDecimalStringSerializer::class) val startingCash: BigDecimal,
    val openingNote: String? = null,
    val adoptRemoteIfActive: Boolean = false,
)

@Serializable
internal data class LocalCashSessionCloseSyncPayload(
    val clientGeneratedId: String,
    val deviceId: String,
    val cashierUserId: String,
    val serverId: String,
    val closedAt: Long,
    @Serializable(with = BigDecimalStringSerializer::class) val actualCash: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val expectedCash: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val difference: BigDecimal,
    val closingNote: String? = null,
)
