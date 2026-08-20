package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_categories",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true)
    ]
)
data class LocalCategoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val name: String,
    val isActive: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val remoteCreatedAt: String? = null,
    val remoteUpdatedAt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
