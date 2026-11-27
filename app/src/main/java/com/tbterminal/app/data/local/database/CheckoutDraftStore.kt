package com.tbterminal.app.data.local.database

import com.tbterminal.app.data.local.dao.CheckoutDraftDao
import com.tbterminal.app.data.local.entity.CheckoutDraftEntity
import com.tbterminal.app.data.sync.runSuspendCatching

data class CheckoutDraftRecord(
    val activeCartJson: String?,
    val heldCartsJson: String?,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val updatedAt: Long
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

interface CheckoutDraftStore {
    suspend fun load(): CheckoutDraftRecord?
    suspend fun save(activeCartJson: String?, heldCartsJson: String?)
    suspend fun clearAll()
}

class RoomCheckoutDraftStore(
    private val dao: CheckoutDraftDao,
    private val ownerUserIdProvider: () -> String,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : CheckoutDraftStore {

    override suspend fun load(): CheckoutDraftRecord? {
        val ownerUserId = resolveOwnerUserId()
        val entity = runSuspendCatching { dao.get(ownerUserId) }.getOrElse { return null }
            ?: return null

        if (entity.schemaVersion != CheckoutDraftRecord.CURRENT_SCHEMA_VERSION) {
            runSuspendCatching { dao.delete(ownerUserId) }
            return null
        }

        return CheckoutDraftRecord(
            activeCartJson = entity.activeCartJson,
            heldCartsJson = entity.heldCartsJson,
            schemaVersion = entity.schemaVersion,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun save(activeCartJson: String?, heldCartsJson: String?) {
        val ownerUserId = resolveOwnerUserId()
        if (activeCartJson == null && heldCartsJson == null) {
            runSuspendCatching { dao.delete(ownerUserId) }
            return
        }
        val entity = CheckoutDraftEntity(
            ownerUserId = ownerUserId,
            activeCartJson = activeCartJson,
            heldCartsJson = heldCartsJson,
            schemaVersion = CheckoutDraftRecord.CURRENT_SCHEMA_VERSION,
            updatedAt = clockMillis()
        )
        runSuspendCatching { dao.upsert(entity) }
    }

    override suspend fun clearAll() {
        val ownerUserId = resolveOwnerUserId()
        runSuspendCatching { dao.delete(ownerUserId) }
    }

    private fun resolveOwnerUserId(): String {
        return ownerUserIdProvider().trim().ifBlank { DEFAULT_OWNER_USER_ID }
    }

    companion object {
        const val DEFAULT_OWNER_USER_ID: String = "anonymous"
    }
}
