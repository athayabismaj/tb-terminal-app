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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class CashMovementType {
    CASH_IN,
    CASH_OUT
}

class LocalCashExpenseService(
    private val database: TbTerminalDatabase,
    private val deviceId: String? = null
) {
    suspend fun addLocalCashMovement(
        cashSessionLocalId: Long,
        type: CashMovementType,
        amount: BigDecimal,
        description: String,
        category: String = DEFAULT_CATEGORY
    ): LocalCashExpenseResult {
        val normalizedDescription = description.trim()
        val baseCategory = category.trim().ifBlank { DEFAULT_CATEGORY }
        val normalizedCategory = if (type == CashMovementType.CASH_IN) {
            if (baseCategory.startsWith("CASH_IN", ignoreCase = true)) baseCategory else "CASH_IN: $baseCategory"
        } else {
            if (baseCategory.startsWith("CASH_OUT", ignoreCase = true)) baseCategory else "CASH_OUT: $baseCategory"
        }
        val prefix = if (type == CashMovementType.CASH_IN) "CASH_IN" else "EXPENSE"
        val actionLabel = if (type == CashMovementType.CASH_IN) "kas masuk" else "pengeluaran"

        if (cashSessionLocalId <= 0L) {
            return LocalCashExpenseResult.Failed("Sesi kas lokal belum valid.")
        }
        if (amount <= BigDecimal.ZERO) {
            return LocalCashExpenseResult.Failed("Nominal $actionLabel harus lebih besar dari nol.")
        }
        if (normalizedDescription.isBlank()) {
            return LocalCashExpenseResult.Failed("Keterangan $actionLabel wajib diisi.")
        }

        return runCatching {
            database.withTransaction {
                val session = database.cashSessionDao().getByLocalId(cashSessionLocalId)
                    ?: return@withTransaction LocalCashExpenseResult.Failed("Sesi kas lokal tidak ditemukan.")
                if (!session.status.equals(CASH_SESSION_OPEN, ignoreCase = true)) {
                    return@withTransaction LocalCashExpenseResult.Failed("Mutasi kas hanya bisa dicatat pada sesi kas lokal terbuka.")
                }

                val now = System.currentTimeMillis()
                val expense = LocalCashExpenseEntity(
                    serverId = null,
                    clientGeneratedId = LocalIdGenerator.clientGeneratedId(prefix),
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
                    ?: error("Mutasi kas lokal gagal dibaca ulang.")
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
            LocalCashExpenseResult.Failed(error.message ?: "Mutasi kas lokal gagal dicatat.")
        }
    }

    suspend fun addLocalExpense(
        cashSessionLocalId: Long,
        amount: BigDecimal,
        description: String,
        category: String = DEFAULT_CATEGORY
    ): LocalCashExpenseResult = addLocalCashMovement(
        cashSessionLocalId = cashSessionLocalId,
        type = CashMovementType.CASH_OUT,
        amount = amount,
        description = description,
        category = category
    )

    suspend fun getLocalExpenses(cashSessionLocalId: Long): List<LocalCashExpenseEntity> {
        return database.cashExpenseDao().getByCashSession(cashSessionLocalId)
    }

    private fun BigDecimal.toSafeDouble(): Double {
        return runCatching { toDouble() }.getOrDefault(0.0)
    }

    internal fun LocalCashExpenseEntity.toSyncPayloadJson(session: LocalCashSessionEntity): String {
        val payload = LocalCashExpenseSyncPayload(
            clientGeneratedId = clientGeneratedId,
            deviceId = (deviceId ?: this@LocalCashExpenseService.deviceId).orEmpty(),
            cashierUserId = session.cashierUserId,
            serverCashSessionId = cashSessionServerId.orEmpty(),
            amount = amount,
            category = category ?: DEFAULT_CATEGORY,
            note = description.orEmpty(),
            occurredAt = occurredAt,
        )
        return SYNC_PAYLOAD_JSON.encodeToString(payload)
    }

    internal companion object {
        const val CASH_SESSION_OPEN = "OPEN"
        const val DEFAULT_CATEGORY = "OPERASIONAL"
        val SYNC_PAYLOAD_JSON: Json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

sealed interface LocalCashExpenseResult {
    data class Success(val expense: LocalCashExpenseEntity) : LocalCashExpenseResult
    data class Failed(val message: String) : LocalCashExpenseResult
}

@Serializable
internal data class LocalCashExpenseSyncPayload(
    val clientGeneratedId: String,
    val deviceId: String,
    val cashierUserId: String,
    val serverCashSessionId: String,
    val amount: Double,
    val category: String,
    val note: String,
    val occurredAt: Long,
)
