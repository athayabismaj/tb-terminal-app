package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.SyncStatus

enum class OfflineSessionAgeSeverity {
    WARNING,
    CRITICAL
}

data class OfflineSessionAgeAlert(
    val severity: OfflineSessionAgeSeverity,
    val ageHours: Long,
    val remainingHours: Long,
    val maxAgeHours: Long = OfflineSessionAgePolicy.DEFAULT_MAX_OFFLINE_SESSION_AGE_HOURS,
    val title: String,
    val message: String,
    val affectedSessionCount: Int = 1,
    val isExpired: Boolean = false
)

/**
 * Evaluates offline cash session age against the backend sync age limit
 * (OFFLINE_MAX_SESSION_AGE_HOURS, default 72 hours), warning at >= 48 hours and
 * escalating to a critical warning at >= 66 hours.
 */
object OfflineSessionAgePolicy {
    const val DEFAULT_MAX_OFFLINE_SESSION_AGE_HOURS = 72L
    const val WARNING_THRESHOLD_HOURS = 48L
    const val CRITICAL_THRESHOLD_HOURS = 66L
    const val MILLIS_PER_HOUR = 3_600_000L

    fun evaluateOpenedAt(
        openedAtMillis: Long,
        nowMillis: Long,
        affectedSessionCount: Int = 1,
        maxAgeHours: Long = DEFAULT_MAX_OFFLINE_SESSION_AGE_HOURS
    ): OfflineSessionAgeAlert? {
        if (openedAtMillis <= 0L) return null
        val elapsedMillis = (nowMillis - openedAtMillis).coerceAtLeast(0L)
        if (elapsedMillis < WARNING_THRESHOLD_HOURS * MILLIS_PER_HOUR) {
            return null
        }

        val ageHours = elapsedMillis / MILLIS_PER_HOUR
        val remainingHours = (maxAgeHours - ageHours).coerceAtLeast(0L)
        val isCritical = elapsedMillis >= CRITICAL_THRESHOLD_HOURS * MILLIS_PER_HOUR
        val isExpired = elapsedMillis >= maxAgeHours * MILLIS_PER_HOUR

        return if (isCritical) {
            val title = if (isExpired) {
                "Peringatan keras: sesi offline melewati batas $maxAgeHours jam"
            } else {
                "Peringatan keras: sesi offline mendekati batas $maxAgeHours jam"
            }
            val message = if (isExpired) {
                "Sesi offline sudah berjalan $ageHours jam dan berisiko ditolak server. Segera sambungkan internet agar transaksi bisa tersinkron."
            } else {
                "Sesi offline sudah berjalan $ageHours jam (tersisa sekitar $remainingHours jam). Segera sambungkan internet agar transaksi bisa tersinkron."
            }
            OfflineSessionAgeAlert(
                severity = OfflineSessionAgeSeverity.CRITICAL,
                ageHours = ageHours,
                remainingHours = remainingHours,
                maxAgeHours = maxAgeHours,
                title = title,
                message = message,
                affectedSessionCount = affectedSessionCount.coerceAtLeast(1),
                isExpired = isExpired
            )
        } else {
            OfflineSessionAgeAlert(
                severity = OfflineSessionAgeSeverity.WARNING,
                ageHours = ageHours,
                remainingHours = remainingHours,
                maxAgeHours = maxAgeHours,
                title = "Sesi offline sudah berjalan $ageHours jam",
                message = "Batas sinkronisasi sesi offline adalah $maxAgeHours jam (tersisa sekitar $remainingHours jam). Segera sambungkan internet agar transaksi bisa tersinkron.",
                affectedSessionCount = affectedSessionCount.coerceAtLeast(1),
                isExpired = false
            )
        }
    }

    fun evaluateUnsyncedSessions(
        sessions: List<LocalCashSessionEntity>,
        nowMillis: Long,
        maxAgeHours: Long = DEFAULT_MAX_OFFLINE_SESSION_AGE_HOURS
    ): OfflineSessionAgeAlert? {
        val unsynced = sessions.filter {
            it.deletedAt == null &&
                it.serverId.isNullOrBlank() &&
                it.syncStatus != SyncStatus.SYNCED
        }
        val warningSessions = unsynced.filter { session ->
            val elapsed = (nowMillis - session.openedAt).coerceAtLeast(0L)
            elapsed >= WARNING_THRESHOLD_HOURS * MILLIS_PER_HOUR
        }
        if (warningSessions.isEmpty()) return null

        val oldestOpenedAt = warningSessions.minOf { it.openedAt }
        return evaluateOpenedAt(
            openedAtMillis = oldestOpenedAt,
            nowMillis = nowMillis,
            affectedSessionCount = warningSessions.size,
            maxAgeHours = maxAgeHours
        )
    }
}
