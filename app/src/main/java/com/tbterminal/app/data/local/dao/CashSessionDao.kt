package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CashSessionDao {
    @Query("SELECT * FROM local_cash_sessions WHERE deletedAt IS NULL ORDER BY openedAt DESC")
    fun observeSessions(): Flow<List<LocalCashSessionEntity>>

    @Query("SELECT * FROM local_cash_sessions WHERE status = :status AND deletedAt IS NULL ORDER BY openedAt DESC LIMIT 1")
    suspend fun getLatestByStatus(status: String): LocalCashSessionEntity?

    @Query(
        """
        SELECT * FROM local_cash_sessions
        WHERE status = :status
          AND cashierUserId = :cashierUserId
          AND deletedAt IS NULL
        ORDER BY openedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestByStatusAndCashier(
        status: String,
        cashierUserId: String
    ): LocalCashSessionEntity?

    @Query(
        """
        SELECT * FROM local_cash_sessions
        WHERE status = :status
          AND deviceId = :deviceId
          AND deletedAt IS NULL
        ORDER BY openedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestByStatusAndDevice(
        status: String,
        deviceId: String
    ): LocalCashSessionEntity?

    @Query(
        """
        SELECT * FROM local_cash_sessions
        WHERE status = :status
          AND cashierUserId = :cashierUserId
          AND serverId IS NOT NULL
          AND deletedAt IS NULL
        ORDER BY openedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestMirroredByStatusAndCashier(
        status: String,
        cashierUserId: String
    ): LocalCashSessionEntity?

    @Query(
        """
        SELECT * FROM local_cash_sessions
        WHERE status = :status
          AND serverId IS NOT NULL
          AND deletedAt IS NULL
        ORDER BY openedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestMirroredByStatus(status: String): LocalCashSessionEntity?

    @Query("SELECT * FROM local_cash_sessions WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): LocalCashSessionEntity?

    @Query("SELECT * FROM local_cash_sessions WHERE serverId = :serverId AND deletedAt IS NULL LIMIT 1")
    suspend fun getByServerId(serverId: String): LocalCashSessionEntity?

    @Query("SELECT * FROM local_cash_sessions WHERE syncStatus = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(status: SyncStatus, limit: Int): List<LocalCashSessionEntity>

    @Query("SELECT COUNT(*) FROM local_cash_sessions WHERE status = :status AND deletedAt IS NULL")
    suspend fun countByStatus(status: String): Int

    @Query(
        """
        SELECT * FROM local_cash_sessions
        WHERE serverId IS NULL
          AND deletedAt IS NULL
          AND (syncStatus = :pending OR syncStatus = :failed)
        ORDER BY openedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getOpenSyncCandidates(
        pending: SyncStatus,
        failed: SyncStatus,
        limit: Int
    ): List<LocalCashSessionEntity>

    @Query(
        """
        SELECT * FROM local_cash_sessions
        WHERE status = :closedStatus
          AND serverId IS NOT NULL
          AND deletedAt IS NULL
          AND (syncStatus = :pending OR syncStatus = :failed)
        ORDER BY closedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getCloseSyncCandidates(
        closedStatus: String,
        pending: SyncStatus,
        failed: SyncStatus,
        limit: Int
    ): List<LocalCashSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: LocalCashSessionEntity): Long

    @Query(
        """
        UPDATE local_cash_sessions
        SET syncStatus = :syncing,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE localId = :localId
          AND serverId IS NULL
          AND (syncStatus = :pending OR syncStatus = :failed OR syncStatus = :conflict)
        """
    )
    suspend fun markOpenSyncingIfPendingOrFailed(
        localId: Long,
        syncing: SyncStatus,
        pending: SyncStatus,
        failed: SyncStatus,
        conflict: SyncStatus,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE local_cash_sessions
        SET syncStatus = :syncing,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE localId = :localId
          AND status = :closedStatus
          AND serverId IS NOT NULL
          AND (syncStatus = :pending OR syncStatus = :failed OR syncStatus = :conflict)
        """
    )
    suspend fun markCloseSyncingIfPendingOrFailed(
        localId: Long,
        closedStatus: String,
        syncing: SyncStatus,
        pending: SyncStatus,
        failed: SyncStatus,
        conflict: SyncStatus,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE local_cash_sessions
        SET serverId = :serverId,
            syncStatus = :syncStatus,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun markOpenSynced(
        localId: Long,
        serverId: String,
        syncStatus: SyncStatus,
        syncedAt: Long,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_cash_sessions
        SET syncStatus = :syncStatus,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun markCloseSynced(
        localId: Long,
        syncStatus: SyncStatus,
        syncedAt: Long,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_cash_sessions
        SET syncStatus = :syncStatus,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun updateSyncStatus(
        localId: Long,
        syncStatus: SyncStatus,
        syncedAt: Long?,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_cash_sessions
        SET status = :status,
            closedAt = :closedAt,
            expectedCash = :expectedCash,
            actualCash = :actualCash,
            difference = :difference,
            closingNote = :closingNote,
            syncStatus = :syncStatus,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun closeSession(
        localId: Long,
        status: String,
        closedAt: Long,
        expectedCash: Double?,
        actualCash: Double?,
        difference: Double?,
        closingNote: String?,
        syncStatus: SyncStatus,
        updatedAt: Long
    ): Unit
}
