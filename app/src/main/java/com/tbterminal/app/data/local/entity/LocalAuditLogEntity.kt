package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "local_audit_logs",
    indices = [
        Index("serverId"),
        Index(value = ["clientGeneratedId"], unique = true),
        Index("actorUserId"),
        Index("action"),
        Index("tableName"),
        Index("recordId"),
        Index("occurredAt"),
        Index("syncStatus")
    ]
)
data class LocalAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: String? = null,
    val clientGeneratedId: String,
    val deviceId: String? = null,
    val actorUserId: String? = null,
    val action: String,
    val tableName: String,
    val recordId: String? = null,
    val description: String? = null,
    val metadataJson: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val deletedAt: Long? = null
)
