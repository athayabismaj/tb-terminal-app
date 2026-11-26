package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineCashReportDao {
    @Query(
        """
        SELECT
            s.localId AS localId,
            s.cashierUserId AS cashierUserId,
            s.status AS status,
            s.openedAt AS openedAt,
            s.closedAt AS closedAt,
            s.startingCash AS startingCash,
            (
                SELECT COALESCE(SUM(p.amount), 0)
                FROM local_payments p
                INNER JOIN local_transactions t ON t.localId = p.transactionLocalId
                WHERE t.cashSessionLocalId = s.localId
                    AND t.deletedAt IS NULL
                    AND p.deletedAt IS NULL
                    AND UPPER(p.method) IN ('TUNAI', 'CASH')
            ) AS totalCashSales,
            (
                SELECT COALESCE(SUM(e.amount), 0)
                FROM local_cash_expenses e
                WHERE e.cashSessionLocalId = s.localId
                    AND e.deletedAt IS NULL
            ) AS totalExpense,
            COALESCE(
                s.actualCash,
                s.startingCash
                    + (
                        SELECT COALESCE(SUM(p.amount), 0)
                        FROM local_payments p
                        INNER JOIN local_transactions t ON t.localId = p.transactionLocalId
                        WHERE t.cashSessionLocalId = s.localId
                            AND t.deletedAt IS NULL
                            AND p.deletedAt IS NULL
                            AND UPPER(p.method) IN ('TUNAI', 'CASH')
                    )
                    - (
                        SELECT COALESCE(SUM(e.amount), 0)
                        FROM local_cash_expenses e
                        WHERE e.cashSessionLocalId = s.localId
                            AND e.deletedAt IS NULL
                    )
            ) AS endingCash,
            s.syncStatus AS syncStatus
        FROM local_cash_sessions s
        WHERE s.deletedAt IS NULL
            AND s.openedAt >= :startAt
            AND s.openedAt < :endAt
        ORDER BY s.openedAt DESC
        LIMIT :limit
        """
    )
    fun observeCashSessions(
        startAt: Long,
        endAt: Long,
        limit: Int
    ): Flow<List<OfflineCashReportRow>>
}

data class OfflineCashReportRow(
    val localId: Long,
    val cashierUserId: String,
    val status: String,
    val openedAt: Long,
    val closedAt: Long?,
    val startingCash: ScaledDecimal2,
    val totalCashSales: ScaledDecimal2,
    val totalExpense: ScaledDecimal2,
    val endingCash: ScaledDecimal2,
    val syncStatus: SyncStatus
)
