package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_receivables",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true),
        Index("transactionLocalId"),
        Index("transactionServerId"),
        Index("customerLocalId"),
        Index("customerServerId"),
        Index("status"),
        Index("dueDate"),
        Index("syncStatus")
    ]
)
data class LocalReceivableEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val transactionLocalId: Long,
    val transactionServerId: String? = null,
    val customerLocalId: Long? = null,
    val customerServerId: String? = null,
    val totalAmount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val paidAmount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val remainingAmount: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val status: String,
    val dueDate: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val occurredAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
