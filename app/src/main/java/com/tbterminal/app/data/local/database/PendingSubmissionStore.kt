package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.PendingSubmissionDao
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity
import com.tbterminal.app.data.sync.isSameBootSession
import com.tbterminal.app.data.sync.runSuspendCatching

object PendingSubmissionScopes {
    const val CHECKOUT = "checkout"
    const val PURCHASE = "purchase"
    const val SUPPLIER_PAYMENT_PREFIX = "supplier-pay:payable:"
    const val CASHIER_PAY_DEBT = "cashier-pay-debt"
    const val CASHIER_VOID = "cashier-void"
    const val CASHIER_REFUND = "cashier-refund"

    fun supplierPayment(payableId: String): String = "$SUPPLIER_PAYMENT_PREFIX${payableId.trim()}"

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
    val updatedAt: Long,
    val createdAtElapsedMillis: Long? = null,
    val bootCount: Long? = null,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

interface PendingSubmissionStore {
    suspend fun load(scope: String): PendingSubmissionRecord?
    suspend fun loadByScopePrefix(scopePrefix: String): List<PendingSubmissionRecord> = emptyList()
    suspend fun save(record: PendingSubmissionRecord): Boolean
    suspend fun saveForOwner(ownerUserId: String, record: PendingSubmissionRecord): Boolean {
        return save(record)
    }
    suspend fun clear(scope: String)
    suspend fun purgeExpired()
}

class RoomPendingSubmissionStore(
    private val dao: PendingSubmissionDao,
    private val ownerUserIdProvider: () -> String,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val skipWhenOwnerBlank: Boolean = false,
    private val monotonicClockMillis: () -> Long = clockMillis,
    private val bootCountProvider: () -> Long? = { 1L },
) : PendingSubmissionStore {

    override suspend fun load(scope: String): PendingSubmissionRecord? {
        val ownerUserId = resolveOwnerUserIdOrNull() ?: return null
        val entity = runSuspendCatching { dao.get(ownerUserId, scope) }.getOrElse { return null }
            ?: return null
        return validateOrDeleteEntity(ownerUserId, entity)
    }

    override suspend fun loadByScopePrefix(scopePrefix: String): List<PendingSubmissionRecord> {
        val ownerUserId = resolveOwnerUserIdOrNull() ?: return emptyList()
        val entities = runSuspendCatching { dao.getByScopePrefix(ownerUserId, scopePrefix) }
            .getOrElse { return emptyList() }
        return entities.mapNotNull { entity -> validateOrDeleteEntity(ownerUserId, entity) }
    }

    private suspend fun validateOrDeleteEntity(
        ownerUserId: String,
        entity: PendingSubmissionEntity
    ): PendingSubmissionRecord? {
        val status = PendingSubmissionStatus.fromRaw(entity.status)
        val isExpired = isEntityExpired(
            entity = entity,
            wallNow = clockMillis(),
            elapsedNow = monotonicClockMillis(),
            bootNow = bootCountProvider(),
        )
        val isInvalid = entity.schemaVersion != PendingSubmissionRecord.CURRENT_SCHEMA_VERSION ||
            entity.idempotencyKey.isBlank() ||
            entity.intentJson.isBlank() ||
            status == null ||
            isExpired

        if (isInvalid) {
            runSuspendCatching { dao.delete(ownerUserId, entity.scope) }
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
            updatedAt = entity.updatedAt,
            createdAtElapsedMillis = entity.createdAtElapsedMillis,
            bootCount = entity.bootCount,
        )
    }

    override suspend fun save(record: PendingSubmissionRecord): Boolean {
        val ownerUserId = resolveOwnerUserIdOrNull() ?: return false
        return saveForOwner(ownerUserId, record)
    }

    override suspend fun saveForOwner(ownerUserId: String, record: PendingSubmissionRecord): Boolean {
        val resolvedOwner = ownerUserId.trim().takeIf(String::isNotBlank) ?: return false
        val existing = runSuspendCatching { dao.get(resolvedOwner, record.scope) }.getOrNull()
        val sameKeyExisting = existing?.takeIf { it.idempotencyKey == record.idempotencyKey }
        val resolvedElapsed = record.createdAtElapsedMillis
            ?: sameKeyExisting?.createdAtElapsedMillis
            ?: monotonicClockMillis()
        val resolvedBootCount = record.bootCount
            ?: sameKeyExisting?.bootCount
            ?: bootCountProvider()
        val entity = PendingSubmissionEntity(
            ownerUserId = resolvedOwner,
            scope = record.scope,
            idempotencyKey = record.idempotencyKey,
            intentJson = record.intentJson,
            payloadJson = record.payloadJson,
            status = record.status.name,
            schemaVersion = record.schemaVersion,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            createdAtElapsedMillis = resolvedElapsed,
            bootCount = resolvedBootCount,
        )
        return runSuspendCatching { dao.upsert(entity) }.isSuccess
    }

    override suspend fun clear(scope: String) {
        val ownerUserId = resolveOwnerUserIdOrNull() ?: return
        runSuspendCatching { dao.delete(ownerUserId, scope) }
    }

    override suspend fun purgeExpired() {
        val wallNow = clockMillis()
        val elapsedNow = monotonicClockMillis()
        val bootNow = bootCountProvider()
        val allEntities = runSuspendCatching { dao.getAll() }.getOrElse { return }
        for (entity in allEntities) {
            if (isEntityExpired(entity, wallNow, elapsedNow, bootNow)) {
                runSuspendCatching { dao.delete(entity.ownerUserId, entity.scope) }
            }
        }
    }

    private fun isEntityExpired(
        entity: PendingSubmissionEntity,
        wallNow: Long,
        elapsedNow: Long,
        bootNow: Long?,
    ): Boolean {
        if (entity.createdAt <= 0L) return true
        val createdElapsed = entity.createdAtElapsedMillis
        if (createdElapsed != null &&
            isSameBootSession(entity.bootCount, bootNow) &&
            elapsedNow >= createdElapsed
        ) {
            return (elapsedNow - createdElapsed) > ttlMillis
        }
        val wallAge = wallNow - entity.createdAt
        if (wallAge < 0L) return false
        return wallAge > ttlMillis
    }

    private fun resolveOwnerUserIdOrNull(): String? {
        val trimmed = ownerUserIdProvider().trim()
        if (trimmed.isNotBlank()) return trimmed
        return if (skipWhenOwnerBlank) null else DEFAULT_OWNER_USER_ID
    }

    companion object {
        const val DEFAULT_TTL_MILLIS: Long = 72L * 60L * 60L * 1000L
        const val DEFAULT_OWNER_USER_ID: String = "anonymous"
    }
}
