package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.cashsession.LocalCashSessionOpenSyncPayload
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.serialization.json.Json

internal sealed interface CashSessionAdoptionDecision {
    data object Eligible : CashSessionAdoptionDecision
    data object NotApplicable : CashSessionAdoptionDecision
    data class Rejected(val reason: String) : CashSessionAdoptionDecision
}

internal object CashSessionAdoptionPolicy {
    const val TIME_WINDOW_TOLERANCE_MILLIS: Long = 5L * 60L * 1000L

    private val payloadJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseAdoptRemoteIfActive(rawPayloadJson: String?): Boolean {
        val raw = rawPayloadJson?.takeIf(String::isNotBlank) ?: return false
        return runCatching {
            payloadJson.decodeFromString<LocalCashSessionOpenSyncPayload>(raw).adoptRemoteIfActive
        }.getOrDefault(false)
    }

    fun parseIsoTimestampMillis(rawTimestamp: String?): Long? {
        val trimmed = rawTimestamp?.trim()?.takeIf(String::isNotBlank) ?: return null
        return runCatching { OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { Instant.parse(trimmed).toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(trimmed)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
    }

    fun evaluateAdoption(
        localSession: LocalCashSessionEntity,
        remoteServerId: String,
        remoteCashierUserId: String,
        remoteStatus: String,
        remoteOpenedAtMillis: Long?,
        existingByServerId: LocalCashSessionEntity?,
        transactions: List<LocalTransactionEntity>,
        expenses: List<LocalCashExpenseEntity>,
    ): CashSessionAdoptionDecision {
        if (!remoteStatus.equals("OPEN", ignoreCase = true)) {
            return CashSessionAdoptionDecision.NotApplicable
        }
        val normalizedLocalCashier = localSession.cashierUserId.trim()
        val normalizedRemoteCashier = remoteCashierUserId.trim()
        if (normalizedLocalCashier.isBlank() || normalizedRemoteCashier != normalizedLocalCashier) {
            return CashSessionAdoptionDecision.NotApplicable
        }
        if (existingByServerId != null && existingByServerId.localId != localSession.localId) {
            return CashSessionAdoptionDecision.Rejected(
                "Sesi kas aktif di server ($remoteServerId) sudah tertaut ke sesi lokal lain. Tutup sesi tersebut terlebih dahulu."
            )
        }
        if (remoteOpenedAtMillis == null || remoteOpenedAtMillis <= 0L) {
            return CashSessionAdoptionDecision.Rejected(
                "Waktu buka sesi kas aktif di server tidak valid untuk adopsi otomatis."
            )
        }

        val minAllowedOccurredAt = remoteOpenedAtMillis - TIME_WINDOW_TOLERANCE_MILLIS
        if (transactions.any { it.deletedAt == null && it.occurredAt < minAllowedOccurredAt }) {
            return CashSessionAdoptionDecision.Rejected(
                "Transaksi offline terjadi lebih dari 5 menit sebelum sesi server dibuka sehingga tidak dapat diadopsi."
            )
        }
        if (expenses.any { it.deletedAt == null && it.occurredAt < minAllowedOccurredAt }) {
            return CashSessionAdoptionDecision.Rejected(
                "Pengeluaran kas offline terjadi lebih dari 5 menit sebelum sesi server dibuka sehingga tidak dapat diadopsi."
            )
        }
        if (localSession.status.equals("CLOSED", ignoreCase = true)) {
            val closedAt = localSession.closedAt
            if (closedAt == null || closedAt < remoteOpenedAtMillis) {
                return CashSessionAdoptionDecision.Rejected(
                    "Waktu tutup sesi lokal lebih awal dari waktu buka sesi aktif di server."
                )
            }
        }
        return CashSessionAdoptionDecision.Eligible
    }
}
