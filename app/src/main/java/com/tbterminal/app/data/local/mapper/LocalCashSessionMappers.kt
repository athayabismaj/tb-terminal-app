package com.tbterminal.app.data.local.mapper

import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.id.LocalIdGenerator
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.model.CashSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

fun CashSession.toLocalCashSessionEntity(
    existing: LocalCashSessionEntity? = null,
    now: Long = System.currentTimeMillis()
): LocalCashSessionEntity {
    return LocalCashSessionEntity(
        localId = existing?.localId ?: 0L,
        serverId = id,
        clientGeneratedId = existing?.clientGeneratedId ?: LocalIdGenerator.clientGeneratedId("SESSION"),
        deviceId = existing?.deviceId,
        cashierUserId = userId,
        status = status,
        openedAt = openedAt.toEpochMillisOrDefault(existing?.openedAt ?: now),
        closedAt = closedAt?.toEpochMillisOrNull(),
        startingCash = ScaledDecimal2.ofExact(openingCash),
        expectedCash = systemCash?.let(ScaledDecimal2::ofExact),
        actualCash = closingCash?.let(ScaledDecimal2::ofExact),
        difference = difference?.let(ScaledDecimal2::ofExact),
        openingNote = existing?.openingNote ?: notes,
        closingNote = if (closedAt != null) notes else existing?.closingNote,
        syncStatus = SyncStatus.SYNCED,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        syncedAt = now,
        deletedAt = null
    )
}

private fun String.toEpochMillisOrDefault(defaultValue: Long): Long {
    return toEpochMillisOrNull() ?: defaultValue
}

private fun String.toEpochMillisOrNull(): Long? {
    return runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(this)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
}
