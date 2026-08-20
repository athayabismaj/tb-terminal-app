package com.tbterminal.app.data.local.cashsession

import androidx.room.withTransaction
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.id.LocalIdGenerator
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import java.math.BigDecimal

class LocalCashSessionService(
    private val database: TbTerminalDatabase,
    private val deviceId: String? = null
) {
    suspend fun openLocalSession(
        cashierUserId: String,
        startingCash: BigDecimal,
        openingNote: String? = null
    ): LocalCashSessionResult {
        val validationError = validateOpenInput(cashierUserId, startingCash)
            ?: validateSingleOpenSession(cashierUserId)
        if (validationError != null) {
            return LocalCashSessionResult.Failed(validationError)
        }

        return runCatching {
            database.withTransaction {
                val now = System.currentTimeMillis()
                val localId = database.cashSessionDao().upsert(
                    LocalCashSessionEntity(
                        serverId = null,
                        clientGeneratedId = LocalIdGenerator.clientGeneratedId("SESSION"),
                        deviceId = deviceId,
                        cashierUserId = cashierUserId.trim(),
                        status = CASH_SESSION_OPEN,
                        openedAt = now,
                        closedAt = null,
                        startingCash = startingCash.toSafeDouble(),
                        expectedCash = startingCash.toSafeDouble(),
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
                        payloadJson = session.toOpenSyncPayloadJson(),
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

        return runCatching {
            database.withTransaction {
                val session = database.cashSessionDao().getByLocalId(localId)
                    ?: return@withTransaction LocalCashSessionResult.Failed("Sesi kas lokal tidak ditemukan.")
                if (!session.status.equals(CASH_SESSION_OPEN, ignoreCase = true)) {
                    return@withTransaction LocalCashSessionResult.Failed("Sesi kas lokal sudah ditutup.")
                }

                val now = System.currentTimeMillis()
                val cashSales = database.paymentDao().sumCashPaymentsByCashSession(localId)
                val cashExpenses = database.cashExpenseDao().sumExpensesByCashSession(localId)
                val expectedCash = session.startingCash + cashSales - cashExpenses
                val actualCash = closingCash.toSafeDouble()
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

        val openByDevice = deviceId
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let { device ->
                database.cashSessionDao().getLatestByStatusAndDevice(
                    status = CASH_SESSION_OPEN,
                    deviceId = device
                )
            }
        if (openByDevice != null) return "Device ini masih memiliki sesi kas terbuka."

        val anyOpen = database.cashSessionDao().getLatestByStatus(CASH_SESSION_OPEN)
        if (anyOpen != null) return "Masih ada sesi kas terbuka di device ini."

        return null
    }

    private fun validateOpenInput(cashierUserId: String, startingCash: BigDecimal): String? {
        if (cashierUserId.isBlank()) return "Kasir sesi lokal belum valid."
        if (startingCash < BigDecimal.ZERO) return "Modal awal tidak boleh kurang dari nol."
        return null
    }

    private fun BigDecimal.toSafeDouble(): Double {
        return runCatching { toDouble() }.getOrDefault(0.0)
    }

    private fun LocalCashSessionEntity.toOpenSyncPayloadJson(): String {
        return buildString {
            append('{')
            append("\"clientGeneratedId\":\"").append(clientGeneratedId.escapeJson()).append("\",")
            append("\"deviceId\":\"").append(deviceId.orEmpty().escapeJson()).append("\",")
            append("\"cashierUserId\":\"").append(cashierUserId.escapeJson()).append("\",")
            append("\"openedAt\":").append(openedAt).append(',')
            append("\"startingCash\":").append(startingCash)
            openingNote?.let { note ->
                append(",\"openingNote\":\"").append(note.escapeJson()).append("\"")
            }
            append('}')
        }
    }

    private fun LocalCashSessionEntity.toCloseSyncPayloadJson(): String {
        return buildString {
            append('{')
            append("\"clientGeneratedId\":\"").append(clientGeneratedId.escapeJson()).append("\",")
            append("\"deviceId\":\"").append(deviceId.orEmpty().escapeJson()).append("\",")
            append("\"cashierUserId\":\"").append(cashierUserId.escapeJson()).append("\",")
            append("\"serverId\":\"").append(serverId.orEmpty().escapeJson()).append("\",")
            append("\"closedAt\":").append(closedAt ?: 0L).append(',')
            append("\"actualCash\":").append(actualCash ?: 0.0).append(',')
            append("\"expectedCash\":").append(expectedCash ?: 0.0).append(',')
            append("\"difference\":").append(difference ?: 0.0)
            closingNote?.let { note ->
                append(",\"closingNote\":\"").append(note.escapeJson()).append("\"")
            }
            append('}')
        }
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private companion object {
        const val CASH_SESSION_OPEN = "OPEN"
        const val CASH_SESSION_CLOSED = "CLOSED"
    }
}

sealed interface LocalCashSessionResult {
    data class Success(val session: LocalCashSessionEntity) : LocalCashSessionResult
    data class Failed(val message: String) : LocalCashSessionResult
}
