package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun observeQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getByStatus(
        status: SyncStatus,
        limit: Int
    ): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SyncQueueEntity?

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE entityLocalId = :entityLocalId
            AND entityType = :entityType
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestForEntity(
        entityLocalId: Long,
        entityType: com.tbterminal.app.data.local.model.SyncEntityType
    ): SyncQueueEntity?

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE entityLocalId = :entityLocalId
            AND entityType = :entityType
            AND operation = :operation
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestForEntityAndOperation(
        entityLocalId: Long,
        entityType: SyncEntityType,
        operation: com.tbterminal.app.data.local.model.SyncOperation
    ): SyncQueueEntity?

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status != :syncedStatus")
    fun observePendingCount(syncedStatus: SyncStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    fun observeCountByStatus(status: SyncStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity): Unit

    @Query(
        """
        UPDATE sync_queue
        SET status = :status,
            retryCount = retryCount + :retryIncrement,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: Long,
        status: SyncStatus,
        lastError: String?,
        retryIncrement: Int,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE sync_queue
        SET status = :failed,
            retryCount = retryCount + 1,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE entityType = :entityType
            AND status != :syncedStatus
            AND entityLocalId IN (
                SELECT localId
                FROM local_transactions
                WHERE syncStatus = :syncing
                    AND updatedAt < :cutoffUpdatedAt
            )
        """
    )
    suspend fun recoverStaleSyncingTransactions(
        syncing: SyncStatus,
        failed: SyncStatus,
        syncedStatus: SyncStatus,
        entityType: SyncEntityType,
        cutoffUpdatedAt: Long,
        lastError: String,
        updatedAt: Long
    ): Unit

    @Query("DELETE FROM sync_queue WHERE status = :status")
    suspend fun deleteByStatus(status: SyncStatus): Unit
}
