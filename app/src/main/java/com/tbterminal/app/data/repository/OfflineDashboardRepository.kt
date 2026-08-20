package com.tbterminal.app.data.repository

import com.tbterminal.app.data.local.dao.OfflineDashboardDao
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineDashboardRepository(
    private val offlineDashboardDao: OfflineDashboardDao
) {
    fun observeTodaySnapshot(): Flow<OfflineDashboardSnapshot> {
        val (startOfDay, endOfDay) = todayBounds()
        return offlineDashboardDao
            .observeDashboardSnapshot(startOfDay = startOfDay, endOfDay = endOfDay)
            .map { aggregate ->
                OfflineDashboardSnapshot(
                    localRevenueToday = aggregate.localRevenueToday,
                    localCollectedToday = aggregate.localCollectedToday,
                    localReceivableOutstanding = aggregate.localReceivableOutstanding,
                    localExpenseToday = aggregate.localExpenseToday,
                    localTransactionCount = aggregate.localTransactionCount,
                    pendingSyncCount = aggregate.pendingSyncCount,
                    failedSyncCount = aggregate.failedSyncCount,
                    lastRefresh = System.currentTimeMillis()
                )
            }
    }

    private fun todayBounds(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val end = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        return start to end
    }
}

data class OfflineDashboardSnapshot(
    val localRevenueToday: Double = 0.0,
    val localCollectedToday: Double = 0.0,
    val localReceivableOutstanding: Double = 0.0,
    val localExpenseToday: Double = 0.0,
    val localTransactionCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
    val lastRefresh: Long = System.currentTimeMillis()
)
