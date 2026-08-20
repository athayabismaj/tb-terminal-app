package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalReceivableEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM local_receivables WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun observeReceivables(): Flow<List<LocalReceivableEntity>>

    @Query("SELECT * FROM local_receivables WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalReceivableEntity?

    @Query("SELECT * FROM local_receivables WHERE transactionLocalId = :transactionLocalId AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getByTransaction(transactionLocalId: Long): List<LocalReceivableEntity>

    @Query("SELECT * FROM local_receivables WHERE syncStatus = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(status: SyncStatus, limit: Int): List<LocalReceivableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(receivable: LocalReceivableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReceivables(receivables: List<LocalReceivableEntity>): Unit

    @Query("UPDATE local_receivables SET paidAmount = :paidAmount, remainingAmount = :remainingAmount, status = :status, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updatePaymentState(
        localId: Long,
        paidAmount: Double,
        remainingAmount: Double,
        status: String,
        syncStatus: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_receivables
        SET syncStatus = :syncStatus,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE transactionLocalId = :transactionLocalId
            AND syncStatus != :syncedStatus
        """
    )
    suspend fun markReceivablesFailed(
        transactionLocalId: Long,
        syncStatus: SyncStatus,
        syncedStatus: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_receivables
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
    suspend fun recoverStaleSyncingReceivables(
        syncing: SyncStatus,
        failed: SyncStatus,
        syncedStatus: SyncStatus,
        cutoffUpdatedAt: Long,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_receivables
        SET serverId = :serverId,
            transactionServerId = :transactionServerId,
            syncStatus = :syncStatus,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun markSynced(
        localId: Long,
        serverId: String?,
        transactionServerId: String,
        syncStatus: SyncStatus,
        syncedAt: Long,
        updatedAt: Long
    ): Unit
}
