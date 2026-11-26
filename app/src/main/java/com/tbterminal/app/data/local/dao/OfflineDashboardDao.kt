package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.tbterminal.app.data.local.model.ScaledDecimal2
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDashboardDao {
    @Query(
        """
        SELECT
            (
                SELECT COALESCE(SUM(total), 0)
                FROM local_transactions
                WHERE deletedAt IS NULL
                    AND occurredAt >= :startOfDay
                    AND occurredAt < :endOfDay
            ) AS localRevenueToday,
            (
                SELECT COALESCE(SUM(amount), 0)
                FROM local_payments
                WHERE deletedAt IS NULL
                    AND paidAt >= :startOfDay
                    AND paidAt < :endOfDay
            ) AS localCollectedToday,
            (
                SELECT COALESCE(SUM(remainingAmount), 0)
                FROM local_receivables
                WHERE deletedAt IS NULL
                    AND remainingAmount > 0
            ) AS localReceivableOutstanding,
            (
                SELECT COALESCE(SUM(amount), 0)
                FROM local_cash_expenses
                WHERE deletedAt IS NULL
                    AND occurredAt >= :startOfDay
                    AND occurredAt < :endOfDay
            ) AS localExpenseToday,
            (
                SELECT COUNT(*)
                FROM local_transactions
                WHERE deletedAt IS NULL
                    AND occurredAt >= :startOfDay
                    AND occurredAt < :endOfDay
            ) AS localTransactionCount,
            (
                SELECT COUNT(*)
                FROM sync_queue
                WHERE status = 'PENDING'
            ) AS pendingSyncCount,
            (
                SELECT COUNT(*)
                FROM sync_queue
                WHERE status = 'FAILED'
            ) AS failedSyncCount
        """
    )
    fun observeDashboardSnapshot(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<OfflineDashboardAggregate>
}

data class OfflineDashboardAggregate(
    val localRevenueToday: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val localCollectedToday: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val localReceivableOutstanding: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val localExpenseToday: ScaledDecimal2 = ScaledDecimal2.ZERO,
    val localTransactionCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0
)
