package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalAuditLogEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAuditLogDao {
    @Query("SELECT * FROM local_audit_logs WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun observeAuditLogs(): Flow<List<LocalAuditLogEntity>>

    @Query("SELECT * FROM local_audit_logs WHERE syncStatus = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(status: SyncStatus, limit: Int): List<LocalAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: LocalAuditLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLogs(logs: List<LocalAuditLogEntity>): Unit
}
