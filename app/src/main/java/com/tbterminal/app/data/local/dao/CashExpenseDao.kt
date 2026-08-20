package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CashExpenseDao {
    @Query("SELECT * FROM local_cash_expenses WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun observeExpenses(): Flow<List<LocalCashExpenseEntity>>

    @Query("SELECT * FROM local_cash_expenses WHERE cashSessionLocalId = :cashSessionLocalId AND deletedAt IS NULL ORDER BY occurredAt DESC")
    suspend fun getByCashSession(cashSessionLocalId: Long): List<LocalCashExpenseEntity>

    @Query("SELECT * FROM local_cash_expenses WHERE localId = :localId AND deletedAt IS NULL LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalCashExpenseEntity?

    @Query("SELECT * FROM local_cash_expenses WHERE syncStatus = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(status: SyncStatus, limit: Int): List<LocalCashExpenseEntity>

    @Query(
        """
        SELECT * FROM local_cash_expenses
        WHERE deletedAt IS NULL
            AND (syncStatus = :pending OR syncStatus = :failed)
        ORDER BY occurredAt ASC
        LIMIT :limit
        """
    )
    suspend fun getSyncCandidates(
        pending: SyncStatus,
        failed: SyncStatus,
        limit: Int
    ): List<LocalCashExpenseEntity>

    @Query(
        """
        SELECT * FROM local_cash_expenses
        WHERE cashSessionLocalId = :cashSessionLocalId
            AND deletedAt IS NULL
            AND (syncStatus = :pending OR syncStatus = :failed)
        ORDER BY occurredAt ASC
        LIMIT :limit
        """
    )
    suspend fun getSyncCandidatesByCashSession(
        cashSessionLocalId: Long,
        pending: SyncStatus,
        failed: SyncStatus,
        limit: Int
    ): List<LocalCashExpenseEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM local_cash_expenses
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

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM local_cash_expenses WHERE cashSessionLocalId = :cashSessionLocalId AND deletedAt IS NULL")
    suspend fun sumExpensesByCashSession(cashSessionLocalId: Long): Double

    @Query(
        """
        UPDATE local_cash_expenses
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
        UPDATE local_cash_expenses
        SET syncStatus = :syncing,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE localId = :localId
            AND deletedAt IS NULL
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
        UPDATE local_cash_expenses
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
    )

    @Query(
        """
        UPDATE local_cash_expenses
        SET syncStatus = :status,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun updateSyncStatus(
        localId: Long,
        status: SyncStatus,
        syncedAt: Long?,
        updatedAt: Long
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: LocalCashExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenses(expenses: List<LocalCashExpenseEntity>): Unit
}
