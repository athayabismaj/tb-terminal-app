package com.tbterminal.app.data.local.cashexpense

import androidx.room.withTransaction
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.id.LocalIdGenerator
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import java.math.BigDecimal

class LocalCashExpenseService(
    private val database: TbTerminalDatabase,
    private val deviceId: String? = null
) {
    suspend fun addLocalExpense(
        cashSessionLocalId: Long,
        amount: BigDecimal,
        description: String,
        category: String = DEFAULT_CATEGORY
    ): LocalCashExpenseResult {
        val normalizedDescription = description.trim()
        val normalizedCategory = category.trim().ifBlank { DEFAULT_CATEGORY }
        if (cashSessionLocalId <= 0L) {
            return LocalCashExpenseResult.Failed("Sesi kas lokal belum valid.")
        }
        if (amount <= BigDecimal.ZERO) {
            return LocalCashExpenseResult.Failed("Nominal pengeluaran harus lebih besar dari nol.")
        }
        if (normalizedDescription.isBlank()) {
            return LocalCashExpenseResult.Failed("Keterangan pengeluaran wajib diisi.")
        }

        return runCatching {
            database.withTransaction {
                val session = database.cashSessionDao().getByLocalId(cashSessionLocalId)
                    ?: return@withTransaction LocalCashExpenseResult.Failed("Sesi kas lokal tidak ditemukan.")
                if (!session.status.equals(CASH_SESSION_OPEN, ignoreCase = true)) {
                    return@withTransaction LocalCashExpenseResult.Failed("Pengeluaran hanya bisa dicatat pada sesi kas lokal terbuka.")
                }

                val now = System.currentTimeMillis()
                val expense = LocalCashExpenseEntity(
                    serverId = null,
                    clientGeneratedId = LocalIdGenerator.clientGeneratedId("EXPENSE"),
                    deviceId = deviceId,
                    cashSessionLocalId = cashSessionLocalId,
                    cashSessionServerId = session.serverId,
                    category = normalizedCategory,
                    description = normalizedDescription,
                    amount = amount.toSafeDouble(),
                    occurredAt = now,
                    syncStatus = SyncStatus.PENDING,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    deletedAt = null
                )
                val localId = database.cashExpenseDao().upsert(expense)
                val savedExpense = database.cashExpenseDao().getByLocalId(localId)
                    ?: error("Pengeluaran kas lokal gagal dibaca ulang.")
                database.syncQueueDao().enqueue(
                    SyncQueueEntity(
                        entityType = SyncEntityType.CASH_EXPENSE,
                        entityLocalId = localId,
                        operation = SyncOperation.CREATE,
                        payloadJson = savedExpense.toSyncPayloadJson(session),
                        status = SyncStatus.PENDING,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                LocalCashExpenseResult.Success(savedExpense)
            }
        }.getOrElse { error ->
            LocalCashExpenseResult.Failed(error.message ?: "Pengeluaran kas lokal gagal dicatat.")
        }
    }

    suspend fun getLocalExpenses(cashSessionLocalId: Long): List<LocalCashExpenseEntity> {
        return database.cashExpenseDao().getByCashSession(cashSessionLocalId)
    }

    private fun BigDecimal.toSafeDouble(): Double {
        return runCatching { toDouble() }.getOrDefault(0.0)
    }

    private fun LocalCashExpenseEntity.toSyncPayloadJson(session: LocalCashSessionEntity): String {
        return buildString {
            append('{')
            append("\"clientGeneratedId\":\"").append(clientGeneratedId.escapeJson()).append("\",")
            append("\"deviceId\":\"").append((deviceId ?: this@LocalCashExpenseService.deviceId).orEmpty().escapeJson()).append("\",")
            append("\"cashierUserId\":\"").append(session.cashierUserId.escapeJson()).append("\",")
            append("\"serverCashSessionId\":\"").append(cashSessionServerId.orEmpty().escapeJson()).append("\",")
            append("\"amount\":").append(amount).append(',')
            append("\"category\":\"").append((category ?: DEFAULT_CATEGORY).escapeJson()).append("\",")
            append("\"note\":\"").append(description.orEmpty().escapeJson()).append("\",")
            append("\"occurredAt\":").append(occurredAt)
            append('}')
        }
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private companion object {
        const val CASH_SESSION_OPEN = "OPEN"
        const val DEFAULT_CATEGORY = "OPERASIONAL"
    }
}

sealed interface LocalCashExpenseResult {
    data class Success(val expense: LocalCashExpenseEntity) : LocalCashExpenseResult
    data class Failed(val message: String) : LocalCashExpenseResult
}
