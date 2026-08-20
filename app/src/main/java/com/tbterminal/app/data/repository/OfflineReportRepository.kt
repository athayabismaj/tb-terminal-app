package com.tbterminal.app.data.repository

import com.tbterminal.app.data.local.dao.OfflineCashReportDao
import com.tbterminal.app.data.local.dao.OfflineCashReportRow
import com.tbterminal.app.data.local.dao.OfflineExpenseReportDao
import com.tbterminal.app.data.local.dao.OfflineExpenseReportRow
import com.tbterminal.app.data.local.dao.OfflineReceivableReportDao
import com.tbterminal.app.data.local.dao.OfflineReceivableReportRow
import com.tbterminal.app.data.local.dao.OfflineSalesReportDao
import com.tbterminal.app.data.local.dao.OfflineSalesReportRow
import com.tbterminal.app.data.local.dao.OfflineSalesReportSummary
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class OfflineReportRepository(
    private val salesReportDao: OfflineSalesReportDao,
    private val cashReportDao: OfflineCashReportDao,
    private val receivableReportDao: OfflineReceivableReportDao,
    private val expenseReportDao: OfflineExpenseReportDao
) {
    fun observeReports(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = DEFAULT_LIMIT
    ): Flow<OfflineReportSnapshot> {
        val safeEndDate = if (endDate.isBefore(startDate)) startDate else endDate
        val startAt = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endAt = safeEndDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return combine(
            salesReportDao.observeSummary(startAt, endAt),
            salesReportDao.observeTransactions(startAt, endAt, limit),
            cashReportDao.observeCashSessions(startAt, endAt, limit),
            receivableReportDao.observeReceivables(startAt, endAt, limit),
            expenseReportDao.observeExpenses(startAt, endAt, limit)
        ) { salesSummary, transactions, cashSessions, receivables, expenses ->
            OfflineReportSnapshot(
                salesSummary = salesSummary,
                transactions = transactions,
                cashSessions = cashSessions,
                receivables = receivables,
                expenses = expenses,
                lastRefresh = System.currentTimeMillis()
            )
        }
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}

data class OfflineReportSnapshot(
    val salesSummary: OfflineSalesReportSummary = OfflineSalesReportSummary(),
    val transactions: List<OfflineSalesReportRow> = emptyList(),
    val cashSessions: List<OfflineCashReportRow> = emptyList(),
    val receivables: List<OfflineReceivableReportRow> = emptyList(),
    val expenses: List<OfflineExpenseReportRow> = emptyList(),
    val lastRefresh: Long = System.currentTimeMillis()
)
