package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbterminal.app.data.local.entity.LocalPaymentEntity
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM local_payments WHERE deletedAt IS NULL ORDER BY paidAt DESC")
    fun observePayments(): Flow<List<LocalPaymentEntity>>

    @Query("SELECT * FROM local_payments WHERE transactionLocalId = :transactionLocalId AND deletedAt IS NULL ORDER BY paidAt ASC")
    suspend fun getByTransaction(transactionLocalId: Long): List<LocalPaymentEntity>

    @Query(
        """
        SELECT COALESCE(SUM(p.amount), 0)
        FROM local_payments p
        INNER JOIN local_transactions t ON t.localId = p.transactionLocalId
        WHERE t.cashSessionLocalId = :cashSessionLocalId
            AND t.deletedAt IS NULL
            AND p.deletedAt IS NULL
            AND UPPER(p.method) IN ('TUNAI', 'CASH')
        """
    )
    suspend fun sumCashPaymentsByCashSession(cashSessionLocalId: Long): Double

    @Query("SELECT * FROM local_payments WHERE syncStatus = :status ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(status: SyncStatus, limit: Int): List<LocalPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: LocalPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayments(payments: List<LocalPaymentEntity>): Unit

    @Query("UPDATE local_payments SET syncStatus = :status, syncedAt = :syncedAt, updatedAt = :updatedAt WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: Long, status: SyncStatus, syncedAt: Long?, updatedAt: Long): Unit

    @Query(
        """
        UPDATE local_payments
        SET syncStatus = :status,
            syncedAt = NULL,
            updatedAt = :updatedAt
        WHERE transactionLocalId = :transactionLocalId
            AND syncStatus != :syncedStatus
        """
    )
    suspend fun markPaymentsFailed(
        transactionLocalId: Long,
        status: SyncStatus,
        syncedStatus: SyncStatus,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_payments
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
    suspend fun recoverStaleSyncingPayments(
        syncing: SyncStatus,
        failed: SyncStatus,
        syncedStatus: SyncStatus,
        cutoffUpdatedAt: Long,
        updatedAt: Long
    ): Unit

    @Query(
        """
        UPDATE local_payments
        SET serverId = :serverId,
            transactionServerId = :transactionServerId,
            syncStatus = :status,
            syncedAt = :syncedAt,
            updatedAt = :updatedAt
        WHERE localId = :localId
        """
    )
    suspend fun markSynced(
        localId: Long,
        serverId: String?,
        transactionServerId: String,
        status: SyncStatus,
        syncedAt: Long,
        updatedAt: Long
    ): Unit
}
