package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val occurredAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
