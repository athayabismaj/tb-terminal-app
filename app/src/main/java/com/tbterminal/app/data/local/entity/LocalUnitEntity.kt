package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_units",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true)
    ]
)
data class LocalUnitEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val name: String,
    val symbol: String,
    val isActive: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val remoteCreatedAt: String? = null,
    val remoteUpdatedAt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
