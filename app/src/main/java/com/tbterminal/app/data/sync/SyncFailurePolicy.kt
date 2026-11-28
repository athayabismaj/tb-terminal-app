package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.model.SyncStatus

internal const val SYNC_STALE_TIMEOUT_MS = 10 * 60 * 1000L
internal const val SYNC_STALE_ERROR = "Sync terputus. Silakan coba lagi."
internal const val SYNC_MAX_UNKNOWN_RETRY = 3

internal fun resolveSyncFailureStatus(
    kind: OfflineCheckoutSyncFailureKind,
    existingRetryCount: Int,
    retryIncrement: Int,
    maxUnknownRetryCount: Int = SYNC_MAX_UNKNOWN_RETRY,
): SyncStatus {
    return when (kind) {
        OfflineCheckoutSyncFailureKind.CONFLICT -> SyncStatus.CONFLICT
        OfflineCheckoutSyncFailureKind.TRANSIENT -> SyncStatus.PENDING
        OfflineCheckoutSyncFailureKind.AUTH -> SyncStatus.PENDING
        OfflineCheckoutSyncFailureKind.VALIDATION -> SyncStatus.FAILED
        OfflineCheckoutSyncFailureKind.UNKNOWN -> {
            if (existingRetryCount + retryIncrement >= maxUnknownRetryCount) {
                SyncStatus.FAILED
            } else {
                SyncStatus.PENDING
            }
        }
    }
}

internal fun maxNullableSeconds(current: Long?, candidate: Long?): Long? {
    return when {
        current == null -> candidate
        candidate == null -> current
        else -> maxOf(current, candidate)
    }
}

internal fun minNullableSeconds(current: Long?, candidate: Long?): Long? {
    return when {
        current == null -> candidate
        candidate == null -> current
        else -> minOf(current, candidate)
    }
}
