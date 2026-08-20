package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalTransactionItemEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionItemDao {
    @Query("SELECT * FROM local_transaction_items WHERE transactionLocalId = :transactionLocalId AND deletedAt IS NULL ORDER BY localId ASC")
    fun observeByTransaction(transactionLocalId: Long): Flow<List<LocalTransactionItemEntity>>

    @Query("SELECT * FROM local_transaction_items WHERE transactionLocalId = :transactionLocalId AND deletedAt IS NULL ORDER BY localId ASC")
    suspend fun getByTransaction(transactionLocalId: Long): List<LocalTransactionItemEntity>

    @Query("SELECT * FROM local_transaction_items WHERE syncStatus = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(status: SyncStatus, limit: Int): List<LocalTransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LocalTransactionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<LocalTransactionItemEntity>): Unit

    @Query("UPDATE local_transaction_items SET syncStatus = :status, syncedAt = :syncedAt, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: Long, status: SyncStatus, syncedAt: Long?, updatedAt: Long): Unit

    @Query(
        """
        UPDATE local_transaction_items
        SET syncStatus = :status,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE transactionLocalId = :transactionLocalId
            AND syncStatus != :syncedStatus
        """
    )
    suspend fun markTransactionItemsFailed(
        transactionLocalId: Long,
        status: SyncStatus,
        syncedStatus: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_transaction_items
        SET syncStatus = :failed,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE transactionLocalId IN (
            SELECT localId
            FROM local_transactions
            WHERE syncStatus = :syncing
                AND updatedAt < :cutoffUpdatedAt
        )
            AND syncStatus != :syncedStatus
        """
    )
    suspend fun recoverStaleSyncingItems(
        syncing: SyncStatus,
        failed: SyncStatus,
        syncedStatus: SyncStatus,
        cutoffUpdatedAt: Long,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_transaction_items
        SET transactionServerId = :transactionServerId,
            syncStatus = :status,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE transactionLocalId = :transactionLocalId
        """
    )
    suspend fun markTransactionItemsSynced(
        transactionLocalId: Long,
        transactionServerId: String,
        status: SyncStatus,
        syncedAt: Long,
        updatedAt: Long
    ): Unit
}
