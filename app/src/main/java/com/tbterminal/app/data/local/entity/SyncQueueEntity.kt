package com.tbterminal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus

@Entity(
    tableName = "sync_queue",
    indices = [
        Index("status"),
        Index("entityType"),
        Index("createdAt")
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: SyncEntityType,
    val entityLocalId: Long,
    val operation: SyncOperation,
    val payloadJson: String,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(id != 0L || entityType.isSupported) {
            "Entity ${entityType.name} tidak didukung oleh offline sync"
        }
    }
}
