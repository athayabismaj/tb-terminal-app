package com.tbterminal.app.data.repository

import com.tbterminal.app.data.local.dao.OfflineDashboardDao
import java.math.BigDecimal
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
                    localRevenueToday = aggregate.localRevenueToday.toBigDecimal(),
                    localCollectedToday = aggregate.localCollectedToday.toBigDecimal(),
                    localReceivableOutstanding = aggregate.localReceivableOutstanding.toBigDecimal(),
                    localExpenseToday = aggregate.localExpenseToday.toBigDecimal(),
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
    val localRevenueToday: BigDecimal = BigDecimal.ZERO,
    val localCollectedToday: BigDecimal = BigDecimal.ZERO,
    val localReceivableOutstanding: BigDecimal = BigDecimal.ZERO,
    val localExpenseToday: BigDecimal = BigDecimal.ZERO,
    val localTransactionCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
    val lastRefresh: Long = System.currentTimeMillis()
)
