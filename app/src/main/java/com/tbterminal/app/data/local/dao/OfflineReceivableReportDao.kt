package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineReceivableReportDao {
    @Query(
        """
        SELECT
            r.localId AS localId,
            COALESCE(c.name, 'Pelanggan') AS customerName,
            COALESCE(t.transactionCode, '-') AS transactionCode,
            r.totalAmount AS totalAmount,
            r.paidAmount AS paidAmount,
            r.remainingAmount AS remainingAmount,
            r.status AS status,
            r.dueDate AS dueDate,
            r.occurredAt AS occurredAt,
            r.syncStatus AS syncStatus
        FROM local_receivables r
        LEFT JOIN local_customers c ON c.localId = r.customerLocalId
        LEFT JOIN local_transactions t ON t.localId = r.transactionLocalId
        WHERE r.deletedAt IS NULL
            AND r.occurredAt >= :startAt
            AND r.occurredAt < :endAt
        ORDER BY r.occurredAt DESC
        LIMIT :limit
        """
    )
    fun observeReceivables(
        startAt: Long,
        endAt: Long,
        limit: Int
    ): Flow<List<OfflineReceivableReportRow>>
}

data class OfflineReceivableReportRow(
    val localId: Long,
    val customerName: String,
    val transactionCode: String,
    val totalAmount: ScaledDecimal2,
    val paidAmount: ScaledDecimal2,
    val remainingAmount: ScaledDecimal2,
    val status: String,
    val dueDate: Long?,
    val occurredAt: Long,
    val syncStatus: SyncStatus
)
