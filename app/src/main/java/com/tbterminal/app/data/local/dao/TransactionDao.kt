package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.model.LocalPendingTransactionWithError
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM local_transactions WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun observeTransactions(): Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM local_transactions WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalTransactionEntity?

    @Query("SELECT * FROM local_transactions WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): LocalTransactionEntity?

    @Query(
        """
        SELECT * FROM local_transactions
        WHERE cashSessionLocalId = :cashSessionLocalId
          AND deletedAt IS NULL
        ORDER BY occurredAt ASC
        """
    )
    suspend fun getByCashSession(cashSessionLocalId: Long): List<LocalTransactionEntity>

    @Query("SELECT * FROM local_transactions WHERE syncStatus = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(status: SyncStatus, limit: Int): List<LocalTransactionEntity>

    @Query(
        """
        SELECT localId
        FROM local_transactions
        WHERE deletedAt IS NULL
            AND (syncStatus = :pending OR syncStatus = :failed)
        ORDER BY occurredAt ASC
        """
    )
    suspend fun getSyncableLocalIds(
        pending: SyncStatus,
        failed: SyncStatus
    ): List<Long>

    @Query(
        """
        SELECT localId
        FROM local_transactions
        WHERE deletedAt IS NULL
            AND syncStatus = :pending
            AND (:cashierUserId IS NULL OR cashierUserId = :cashierUserId)
        ORDER BY occurredAt ASC
        LIMIT :limit
        """
    )
    suspend fun getPendingLocalIdsForAutoSync(
        pending: SyncStatus,
        limit: Int,
        cashierUserId: String? = null,
    ): List<Long>

    @Query(
        """
        SELECT localId
        FROM local_transactions
        WHERE cashSessionLocalId = :cashSessionLocalId
          AND deletedAt IS NULL
          AND syncStatus IN (:pending, :failed)
          AND (:cashierUserId IS NULL OR cashierUserId = :cashierUserId)
        ORDER BY occurredAt ASC, localId ASC
        LIMIT :limit
        """
    )
    suspend fun getPendingLocalIdsForSession(
        cashSessionLocalId: Long,
        pending: SyncStatus = SyncStatus.PENDING,
        failed: SyncStatus = SyncStatus.PENDING,
        limit: Int,
        cashierUserId: String? = null,
    ): List<Long>

    @Query(
        """
        SELECT localId
        FROM local_transactions
        WHERE cashSessionLocalId IS NULL
          AND deletedAt IS NULL
          AND syncStatus IN (:pending, :failed)
          AND (:cashierUserId IS NULL OR cashierUserId = :cashierUserId)
        ORDER BY occurredAt ASC, localId ASC
        LIMIT :limit
        """
    )
    suspend fun getPendingLocalIdsWithoutLocalSession(
        pending: SyncStatus = SyncStatus.PENDING,
        failed: SyncStatus = SyncStatus.PENDING,
        limit: Int,
        cashierUserId: String? = null,
    ): List<Long>

    @Query(
        """
        SELECT COUNT(*)
        FROM local_transactions
        WHERE deletedAt IS NULL
            AND syncStatus = :pending
            AND (:cashierUserId IS NULL OR cashierUserId = :cashierUserId)
        """
    )
    suspend fun countPendingTransactions(
        pending: SyncStatus = SyncStatus.PENDING,
        cashierUserId: String? = null,
    ): Int

    @Query(
        """
        SELECT * FROM local_transactions
        WHERE deletedAt IS NULL
            AND (syncStatus = :pending OR syncStatus = :syncing OR syncStatus = :failed OR syncStatus = :conflict)
        ORDER BY occurredAt DESC
        LIMIT :limit
        """
    )
    fun observeUnsyncedTransactions(
        pending: SyncStatus,
        syncing: SyncStatus,
        failed: SyncStatus,
        conflict: SyncStatus,
        limit: Int
    ): Flow<List<LocalTransactionEntity>>

    @Query(
        """
        SELECT
            localId,
            transactionCode,
            occurredAt,
            total,
            paidAmount,
            remainingAmount,
            syncStatus,
            (
                SELECT q.lastError
                FROM sync_queue q
                WHERE q.entityLocalId = local_transactions.localId
                    AND q.entityType = :entityType
                ORDER BY q.createdAt DESC
                LIMIT 1
            ) AS lastError
        FROM local_transactions
        WHERE deletedAt IS NULL
            AND (syncStatus = :pending OR syncStatus = :syncing OR syncStatus = :failed OR syncStatus = :conflict)
        ORDER BY occurredAt DESC
        LIMIT :limit
        """
    )
    fun observeUnsyncedTransactionsWithError(
        pending: SyncStatus,
        syncing: SyncStatus,
        failed: SyncStatus,
        conflict: SyncStatus,
        entityType: SyncEntityType,
        limit: Int
    ): Flow<List<LocalPendingTransactionWithError>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: LocalTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<LocalTransactionEntity>): Unit

    @Query("UPDATE local_transactions SET syncStatus = :status, syncedAt = :syncedAt, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: Long, status: SyncStatus, syncedAt: Long?, updatedAt: Long): Unit

    @Query(
        """
        UPDATE local_transactions
        SET cashSessionServerId = :serverId,
            updatedAt = :updatedAt
        WHERE cashSessionLocalId = :cashSessionLocalId
            AND cashSessionServerId IS NULL
        """
    )
    suspend fun updateCashSessionServerIdForLocalSession(
        cashSessionLocalId: Long,
        serverId: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM local_transactions
        WHERE cashSessionLocalId = :cashSessionLocalId
            AND deletedAt IS NULL
            AND (syncStatus = :pending OR syncStatus = :syncing OR syncStatus = :failed OR syncStatus = :conflict)
        """
    )
    suspend fun countUnsyncedByCashSession(
        cashSessionLocalId: Long,
        pending: SyncStatus,
        syncing: SyncStatus,
        failed: SyncStatus,
        conflict: SyncStatus
    ): Int

    @Query(
        """
        UPDATE local_transactions
        SET syncStatus = :syncing,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE localId = :localId
            AND (syncStatus = :pending OR syncStatus = :failed OR syncStatus = :conflict)
        """
    )
    suspend fun markSyncingIfPendingOrFailed(
        localId: Long,
        syncing: SyncStatus,
        pending: SyncStatus,
        failed: SyncStatus,
        conflict: SyncStatus,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE local_transactions
        SET syncStatus = :failed,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE syncStatus = :syncing
            AND (updatedAt < :cutoffUpdatedAt OR updatedAt > :updatedAt)
        """
    )
    suspend fun recoverStaleSyncing(
        syncing: SyncStatus,
        failed: SyncStatus,
        cutoffUpdatedAt: Long,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE local_transactions
        SET serverId = :serverId,
            syncStatus = :status,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun markSynced(
        localId: Long,
        serverId: String,
        status: SyncStatus,
        syncedAt: Long,
        updatedAt: Long
    ): Unit

    @Query("UPDATE local_transactions SET deletedAt = :deletedAt, syncStatus = :status, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun markDeleted(localId: Long, deletedAt: Long, status: SyncStatus, updatedAt: Long): Unit
}
