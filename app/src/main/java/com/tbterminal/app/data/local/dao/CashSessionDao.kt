package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.sync.CashSessionActiveBlockingMatcher
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

    @Query(
        """
        SELECT * FROM local_cash_sessions
        WHERE serverId IS NULL
          AND deletedAt IS NULL
          AND syncStatus != :syncedStatus
        ORDER BY openedAt ASC
        """
    )
    suspend fun getUnsyncedOfflineSessions(syncedStatus: SyncStatus = SyncStatus.SYNCED): List<LocalCashSessionEntity>

    @Query("SELECT COUNT(*) FROM local_cash_sessions WHERE status = :status AND deletedAt IS NULL")
    suspend fun countByStatus(status: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM local_cash_sessions
        WHERE serverId IS NULL
          AND deletedAt IS NULL
          AND syncStatus = :pending
        """
    )
    suspend fun countPendingOpenSessions(pending: SyncStatus = SyncStatus.PENDING): Int

    @Query(
        """
        SELECT COUNT(*) FROM local_cash_sessions s
        WHERE s.status = :closedStatus
          AND s.serverId IS NOT NULL
          AND s.deletedAt IS NULL
          AND s.syncStatus = :pending
          AND NOT EXISTS (
              SELECT 1 FROM local_transactions t
              WHERE t.cashSessionLocalId = s.localId
                AND t.deletedAt IS NULL
                AND t.syncStatus IN (:pending, :syncing, :failed, :conflict)
          )
          AND NOT EXISTS (
              SELECT 1 FROM local_cash_expenses e
              WHERE e.cashSessionLocalId = s.localId
                AND e.deletedAt IS NULL
                AND e.syncStatus IN (:pending, :syncing, :failed, :conflict)
          )
        """
    )
    suspend fun countActionablePendingCloseSessions(
        closedStatus: String = "CLOSED",
        pending: SyncStatus = SyncStatus.PENDING,
        syncing: SyncStatus = SyncStatus.SYNCING,
        failed: SyncStatus = SyncStatus.FAILED,
        conflict: SyncStatus = SyncStatus.CONFLICT,
    ): Int

    @Query(
        """
        UPDATE local_cash_sessions
        SET syncStatus = :targetStatus,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE syncStatus = :syncing
          AND updatedAt < :cutoffUpdatedAt
        """
    )
    suspend fun recoverStaleSyncing(
        syncing: SyncStatus,
        targetStatus: SyncStatus,
        cutoffUpdatedAt: Long,
        updatedAt: Long,
    ): Int

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

    @Query(
        """
        SELECT * FROM local_cash_sessions s
        WHERE s.deletedAt IS NULL
          AND (
              (s.serverId IS NULL AND s.syncStatus IN (:pending, :failed))
              OR (s.status = :closedStatus AND s.serverId IS NOT NULL AND s.syncStatus IN (:pending, :failed))
              OR EXISTS (
                  SELECT 1 FROM local_transactions t
                  WHERE t.cashSessionLocalId = s.localId
                    AND t.deletedAt IS NULL
                    AND t.syncStatus = :pending
              )
              OR EXISTS (
                  SELECT 1 FROM local_cash_expenses e
                  WHERE e.cashSessionLocalId = s.localId
                    AND e.deletedAt IS NULL
                    AND e.syncStatus = :pending
              )
          )
        ORDER BY s.openedAt ASC, s.localId ASC
        LIMIT :limit
        """
    )
    suspend fun getSessionsNeedingSync(
        closedStatus: String = "CLOSED",
        pending: SyncStatus = SyncStatus.PENDING,
        failed: SyncStatus = SyncStatus.FAILED,
        limit: Int = 50,
    ): List<LocalCashSessionEntity>

    @Query(
        """
        SELECT localId FROM local_cash_sessions
        WHERE deletedAt IS NULL
          AND serverId IS NULL
          AND cashierUserId = :cashierUserId
          AND (
              syncStatus IN (:failed, :conflict)
              OR localId IN (
                  SELECT entityLocalId FROM sync_queue
                  WHERE entityType = :entityType
                    AND operation = :createOperation
                    AND lastErrorCode = :blockedErrorCode
              )
          )
        """
    )
    suspend fun getBlockedOpenSessionIdsForCashier(
        cashierUserId: String,
        blockedErrorCode: String = CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN,
        failed: SyncStatus = SyncStatus.FAILED,
        conflict: SyncStatus = SyncStatus.CONFLICT,
        entityType: SyncEntityType = SyncEntityType.CASH_SESSION,
        createOperation: SyncOperation = SyncOperation.CREATE,
    ): List<Long>

    @Query(
        """
        UPDATE local_cash_sessions
        SET syncStatus = :pendingStatus,
            updatedAt = :updatedAt
        WHERE localId IN (:localIds)
          AND serverId IS NULL
        """
    )
    suspend fun resetSessionsToPending(
        localIds: List<Long>,
        updatedAt: Long,
        pendingStatus: SyncStatus = SyncStatus.PENDING,
    ): Int

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
        expectedCash: ScaledDecimal2?,
        actualCash: ScaledDecimal2?,
        difference: ScaledDecimal2?,
        closingNote: String?,
        syncStatus: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_cash_sessions
        SET status = :closedStatus,
            closedAt = COALESCE(closedAt, :closedAt),
            syncStatus = :syncedStatus,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE deletedAt IS NULL
          AND status = :openStatus
          AND serverId IS NOT NULL
          AND syncStatus = :syncedStatus
          AND (:cashierUserId IS NULL OR cashierUserId = :cashierUserId OR cashierUserId IS NULL)
        """
    )
    suspend fun markMirroredOpenSessionsClosed(
        cashierUserId: String?,
        closedAt: Long,
        updatedAt: Long,
        syncedAt: Long = updatedAt,
        openStatus: String = "OPEN",
        closedStatus: String = "CLOSED",
        syncedStatus: SyncStatus = SyncStatus.SYNCED,
    ): Int
}
