package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_cash_sessions",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true),
        Index("cashierUserId"),
        Index("status"),
        Index("openedAt"),
        Index("closedAt"),
        Index("syncStatus")
    ]
)
data class LocalCashSessionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val cashierUserId: String,
    val status: String,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val startingCash: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val expectedCash: ScaledDecimal2? = null,
    val actualCash: ScaledDecimal2? = null,
    val difference: ScaledDecimal2? = null,
    val openingNote: String? = null,
    val closingNote: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
