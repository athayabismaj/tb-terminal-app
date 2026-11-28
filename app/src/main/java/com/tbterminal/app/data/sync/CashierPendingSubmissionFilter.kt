package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.RoomPendingSubmissionStore
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity

private const val RECEIVABLE_PAYMENT_PREFIX = "receivable-pay:"

internal fun isCashierPendingSubmissionScope(scope: String): Boolean {
    val normalized = scope.trim()
    return normalized == PendingSubmissionScopes.CHECKOUT ||
        normalized == PendingSubmissionScopes.CASHIER_PAY_DEBT ||
        normalized == PendingSubmissionScopes.CASHIER_VOID ||
        normalized == PendingSubmissionScopes.CASHIER_REFUND ||
        normalized.startsWith(RECEIVABLE_PAYMENT_PREFIX)
}

internal fun isPendingSubmissionActive(
    entity: PendingSubmissionEntity,
    wallNow: Long,
    elapsedNow: Long,
    bootNow: Long?,
    ttlMillis: Long = RoomPendingSubmissionStore.DEFAULT_TTL_MILLIS,
    expectedSchemaVersion: Int = 1,
): Boolean {
    if (entity.schemaVersion != expectedSchemaVersion) return false
    if (entity.idempotencyKey.isBlank() || entity.intentJson.isBlank()) return false
    if (PendingSubmissionStatus.fromRaw(entity.status) == null) return false
    if (entity.createdAt <= 0L) return false

    val createdElapsed = entity.createdAtElapsedMillis
    if (createdElapsed != null &&
        isSameBootSession(entity.bootCount, bootNow) &&
        elapsedNow >= createdElapsed
    ) {
        return (elapsedNow - createdElapsed) <= ttlMillis
    }
    val wallAge = wallNow - entity.createdAt
    if (wallAge < 0L) return true
    return wallAge <= ttlMillis
}
