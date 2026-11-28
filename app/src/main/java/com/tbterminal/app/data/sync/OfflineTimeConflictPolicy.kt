package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.model.SyncStatus

/**
 * Classifies `OFFLINE_OCCURRED_AT_INVALID` rejections and governs one-time recovery of legacy
 * time-conflict items using [TrustedOfflineClock] (never message text).
 */
internal object OfflineTimeConflictPolicy {
    const val CODE_OFFLINE_OCCURRED_AT_INVALID = "OFFLINE_OCCURRED_AT_INVALID"
    const val FUTURE_REJECTION_MARGIN_MILLIS = 60_000L
    const val FUTURE_RETRY_BUFFER_MILLIS = 60_000L
    const val SESSION_OPEN_SKEW_TOLERANCE_MILLIS = 5L * 60_000L

    fun isFutureTimeRejection(
        errorCode: String?,
        occurredAtMillis: Long,
        trustedNowMillis: Long?,
    ): Boolean {
        if (trustedNowMillis == null) return false
        if (!isOccurredAtInvalidCode(errorCode)) return false
        return occurredAtMillis > trustedNowMillis - FUTURE_REJECTION_MARGIN_MILLIS
    }

    fun eligibleAtMillis(occurredAtMillis: Long): Long {
        return occurredAtMillis + FUTURE_RETRY_BUFFER_MILLIS
    }

    fun shouldDeferBeforeSend(
        queueStatus: SyncStatus?,
        queueErrorCode: String?,
        occurredAtMillis: Long,
        trustedNowMillis: Long?,
    ): Boolean {
        if (trustedNowMillis == null) return false
        if (queueStatus != SyncStatus.PENDING) return false
        if (!isOccurredAtInvalidCode(queueErrorCode)) return false
        return trustedNowMillis < eligibleAtMillis(occurredAtMillis)
    }

    fun secondsUntilEligible(occurredAtMillis: Long, trustedNowMillis: Long): Long {
        val remainingMillis = eligibleAtMillis(occurredAtMillis) - trustedNowMillis
        return ((remainingMillis + 999L) / 1000L).coerceAtLeast(1L)
    }

    fun isLegacyConflictEligibleForAutoRecovery(
        errorCode: String?,
        occurredAtMillis: Long,
        sessionOpenedAtMillis: Long,
        sessionServerId: String?,
        trustedNowMillis: Long?,
    ): Boolean {
        if (trustedNowMillis == null) return false
        if (sessionServerId.isNullOrBlank()) return false
        if (!isOccurredAtInvalidCode(errorCode)) return false
        return occurredAtMillis <= trustedNowMillis &&
            occurredAtMillis >= sessionOpenedAtMillis - SESSION_OPEN_SKEW_TOLERANCE_MILLIS
    }

    fun isOccurredAtInvalidCode(errorCode: String?): Boolean {
        return errorCode?.trim()?.equals(CODE_OFFLINE_OCCURRED_AT_INVALID, ignoreCase = true) == true
    }
}
