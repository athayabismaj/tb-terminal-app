package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineSalesReportDao {
    @Query(
        """
        SELECT
            (
                SELECT COUNT(*)
                FROM local_transactions
                WHERE deletedAt IS NULL
                    AND occurredAt >= :startAt
                    AND occurredAt < :endAt
            ) AS totalTransactions,
            (
                SELECT COALESCE(SUM(total), 0.0)
                FROM local_transactions
                WHERE deletedAt IS NULL
                    AND occurredAt >= :startAt
                    AND occurredAt < :endAt
            ) AS localRevenue,
            (
                SELECT COALESCE(SUM(amount), 0.0)
                FROM local_payments
                WHERE deletedAt IS NULL
                    AND paidAt >= :startAt
                    AND paidAt < :endAt
            ) AS collectedAmount,
            (
                SELECT COALESCE(SUM(remainingAmount), 0.0)
                FROM local_receivables
                WHERE deletedAt IS NULL
                    AND occurredAt >= :startAt
                    AND occurredAt < :endAt
            ) AS outstandingAmount
        """
    )
    fun observeSummary(startAt: Long, endAt: Long): Flow<OfflineSalesReportSummary>

    @Query(
        """
        SELECT
            t.localId AS localId,
            t.transactionCode AS transactionCode,
            COALESCE(c.name, 'Umum') AS customerName,
            t.status AS status,
            t.total AS total,
            t.paidAmount AS paidAmount,
            t.remainingAmount AS remainingAmount,
            t.occurredAt AS occurredAt,
            t.syncStatus AS syncStatus
        FROM local_transactions t
        LEFT JOIN local_customers c ON c.localId = t.customerLocalId
        WHERE t.deletedAt IS NULL
            AND t.occurredAt >= :startAt
            AND t.occurredAt < :endAt
        ORDER BY t.occurredAt DESC
        LIMIT :limit
        """
    )
    fun observeTransactions(
        startAt: Long,
        endAt: Long,
        limit: Int
    ): Flow<List<OfflineSalesReportRow>>
}

data class OfflineSalesReportSummary(
    val totalTransactions: Int = 0,
    val localRevenue: Double = 0.0,
    val collectedAmount: Double = 0.0,
    val outstandingAmount: Double = 0.0
)

data class OfflineSalesReportRow(
    val localId: Long,
    val transactionCode: String,
    val customerName: String,
    val status: String,
    val total: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val occurredAt: Long,
    val syncStatus: SyncStatus
)
