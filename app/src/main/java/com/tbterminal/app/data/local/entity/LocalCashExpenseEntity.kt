package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_cash_expenses",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true),
        Index("cashSessionLocalId"),
        Index("cashSessionServerId"),
        Index("category"),
        Index("occurredAt"),
        Index("syncStatus")
    ]
)
data class LocalCashExpenseEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val cashSessionLocalId: Long? = null,
    val cashSessionServerId: String? = null,
    val category: String? = null,
    val description: String? = null,
    val amount: Double = 0.0,
    val occurredAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
