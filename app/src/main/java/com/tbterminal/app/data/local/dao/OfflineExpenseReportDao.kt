package com.tbterminal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.tbterminal.app.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineExpenseReportDao {
    @Query(
        """
        SELECT
            localId AS localId,
            occurredAt AS occurredAt,
            COALESCE(category, '-') AS category,
            amount AS amount,
            description AS description,
            syncStatus AS syncStatus
        FROM local_cash_expenses
        WHERE deletedAt IS NULL
            AND occurredAt >= :startAt
            AND occurredAt < :endAt
        ORDER BY occurredAt DESC
        LIMIT :limit
        """
    )
    fun observeExpenses(
        startAt: Long,
        endAt: Long,
        limit: Int
    ): Flow<List<OfflineExpenseReportRow>>
}

data class OfflineExpenseReportRow(
    val localId: Long,
    val occurredAt: Long,
    val category: String,
    val amount: Double,
    val description: String?,
    val syncStatus: SyncStatus
)
