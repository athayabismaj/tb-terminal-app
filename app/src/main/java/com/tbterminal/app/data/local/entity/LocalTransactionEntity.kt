package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_transactions",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true),
        Index("transactionCode"),
        Index("customerLocalId"),
        Index("customerServerId"),
        Index("cashSessionLocalId"),
        Index("cashSessionServerId"),
        Index("status"),
        Index("occurredAt"),
        Index("syncStatus")
    ]
)
data class LocalTransactionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val transactionCode: String,
    val customerLocalId: Long? = null,
    val customerServerId: String? = null,
    val cashSessionLocalId: Long? = null,
    val cashSessionServerId: String? = null,
    val cashierUserId: String? = null,
    val status: String,
    val subtotal: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val discount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val taxEnabled: Boolean = false,
    val taxMode: String? = null,
    val taxRate: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val taxLabel: String? = null,
    val taxableBase: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val taxAmount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val roundingAmount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val total: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val paidAmount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val remainingAmount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val occurredAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
