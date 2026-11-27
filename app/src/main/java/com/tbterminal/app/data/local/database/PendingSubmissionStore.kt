package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.PendingSubmissionDao
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity
import com.tbterminal.app.data.sync.runSuspendCatching

object PendingSubmissionScopes {
    const val CHECKOUT = "checkout"
    const val PURCHASE = "purchase"
    const val SUPPLIER_PAYMENT_LIST = "supplier-pay"
    const val CASHIER_PAY_DEBT = "cashier-pay-debt"
    const val CASHIER_VOID = "cashier-void"
    const val CASHIER_REFUND = "cashier-refund"

    fun supplierPaymentDetail(supplierId: String): String = "supplier-pay:${supplierId.trim()}"

    fun receivablePayment(customerId: String): String = "receivable-pay:${customerId.trim()}"
}

enum class PendingSubmissionStatus {
    IN_FLIGHT,
    AMBIGUOUS;

    companion object {
        fun fromRaw(raw: String): PendingSubmissionStatus? {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}

data class PendingSubmissionRecord(
    val scope: String,
    val idempotencyKey: String,
    val intentJson: String,
    val payloadJson: String? = null,
    val status: PendingSubmissionStatus,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

interface PendingSubmissionStore {
    suspend fun load(scope: String): PendingSubmissionRecord?
    suspend fun save(record: PendingSubmissionRecord)
    suspend fun clear(scope: String)
    suspend fun purgeExpired()
}

class RoomPendingSubmissionStore(
    private val dao: PendingSubmissionDao,
    private val ownerUserIdProvider: () -> String,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS
) : PendingSubmissionStore {

    override suspend fun load(scope: String): PendingSubmissionRecord? {
        val ownerUserId = resolveOwnerUserId()
        val entity = runSuspendCatching { dao.get(ownerUserId, scope) }.getOrElse { return null }
            ?: return null

        val status = PendingSubmissionStatus.fromRaw(entity.status)
        val now = clockMillis()
        val isExpired = entity.createdAt <= 0L || now - entity.createdAt > ttlMillis
        val isInvalid = entity.schemaVersion != PendingSubmissionRecord.CURRENT_SCHEMA_VERSION ||
            entity.idempotencyKey.isBlank() ||
            entity.intentJson.isBlank() ||
            status == null ||
            isExpired

        if (isInvalid) {
            runSuspendCatching { dao.delete(ownerUserId, scope) }
            return null
        }

        return PendingSubmissionRecord(
            scope = entity.scope,
            idempotencyKey = entity.idempotencyKey,
            intentJson = entity.intentJson,
            payloadJson = entity.payloadJson,
            status = status!!,
            schemaVersion = entity.schemaVersion,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun save(record: PendingSubmissionRecord) {
        val ownerUserId = resolveOwnerUserId()
        val entity = PendingSubmissionEntity(
            ownerUserId = ownerUserId,
            scope = record.scope,
            idempotencyKey = record.idempotencyKey,
            intentJson = record.intentJson,
            payloadJson = record.payloadJson,
            status = record.status.name,
            schemaVersion = record.schemaVersion,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
        runSuspendCatching { dao.upsert(entity) }
    }

    override suspend fun clear(scope: String) {
        val ownerUserId = resolveOwnerUserId()
        runSuspendCatching { dao.delete(ownerUserId, scope) }
    }

    override suspend fun purgeExpired() {
        val cutoff = clockMillis() - ttlMillis
        runSuspendCatching { dao.deleteExpired(cutoff) }
    }

    private fun resolveOwnerUserId(): String {
        return ownerUserIdProvider().trim().ifBlank { DEFAULT_OWNER_USER_ID }
    }

    companion object {
        const val DEFAULT_TTL_MILLIS: Long = 72L * 60L * 60L * 1000L
        const val DEFAULT_OWNER_USER_ID: String = "anonymous"
    }
}
