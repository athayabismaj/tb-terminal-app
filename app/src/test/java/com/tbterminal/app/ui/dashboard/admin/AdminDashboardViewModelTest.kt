package com.tbterminal.app.ui.dashboard.admin

import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.TransactionVoidResult
import com.tbterminal.app.data.remote.DailySalesSummaryDto
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportRangeDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import com.tbterminal.app.data.remote.SalesReportTotalsDto
import com.tbterminal.app.data.remote.SalesReceivableSummaryDto
import com.tbterminal.app.data.remote.TopProductSalesDto
import com.tbterminal.app.data.remote.VoidedSalesSummaryDto
import com.tbterminal.app.data.repository.AnalyticsFilter
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.ReportCsvType
import com.tbterminal.app.data.repository.RepositoryResult
import java.io.OutputStream
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminDashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fakeAnalytics = FakeDashboardAnalyticsRepository()
    private val fakeCashRepo = FakeDashboardCashReconciliationRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadFetchesDashboardMetrics() = runTest {
        val viewModel = AdminDashboardViewModel(
            analyticsRepository = fakeAnalytics,
            cashReconciliationRepository = null,
            includeFinancialSummary = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.metrics)
        assertEquals(5000000.0, state.metrics?.totalRevenueToday ?: 0.0, 0.01)
        assertNull(state.fullSalesReport)
        assertNull(state.financialTotals)
    }

    @Test
    fun ownerModeLoadsFinancialReportAndDailySalesTrend() = runTest {
        val viewModel = AdminDashboardViewModel(
            analyticsRepository = fakeAnalytics,
            cashReconciliationRepository = null,
            includeFinancialSummary = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.fullSalesReport)
        assertNotNull(state.financialTotals)
        assertEquals(BigDecimal("4800000"), state.financialTotals?.netRevenue)
        assertEquals(BigDecimal("1200000"), state.financialTotals?.grossProfit)
        assertEquals(7, state.dailySalesTrend.size)
        assertEquals(1, state.fullSalesReport?.topProducts?.size)
    }

    @Test
    fun reconciliationRepositoryLoadsActiveSessionAndRecentTransactions() = runTest {
        val viewModel = AdminDashboardViewModel(
            analyticsRepository = fakeAnalytics,
            cashReconciliationRepository = fakeCashRepo,
            includeFinancialSummary = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.activeCashierSession)
        assertEquals("session-1", state.activeCashierSession?.id)
        assertEquals("Budi Kasir", state.activeCashierSession?.userName)
        assertEquals(1, state.recentTransactions.size)
        assertEquals("TRX-001", state.recentTransactions.first().receiptId)
    }

    @Test
    fun setPeriodUpdatesPeriodAndReloadsMetrics() = runTest {
        val viewModel = AdminDashboardViewModel(
            analyticsRepository = fakeAnalytics,
            cashReconciliationRepository = null,
            includeFinancialSummary = true
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedPeriodDays)

        viewModel.setPeriod(7)
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.selectedPeriodDays)
        assertNotNull(viewModel.uiState.value.fullSalesReport)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// FAKES
// ═════════════════════════════════════════════════════════════════════════════

private class FakeDashboardAnalyticsRepository : AnalyticsRepository {
    override suspend fun getDashboardMetrics(): DashboardMetricsDto = DashboardMetricsDto(
        totalRevenueToday = 5000000.0,
        totalRevenueThisMonth = 85000000.0,
        totalActiveReceivables = 12500000.0,
        activeReceivableCount = 4,
        lowStockCount = 3,
        lowStockItems = emptyList()
    )

    override suspend fun getDailySales(startDate: String?, endDate: String?): List<DailySalesSummaryDto> =
        (0 until 7).map { index ->
            DailySalesSummaryDto(
                date = "2026-10-1$index",
                transactionCount = 10L,
                totalRevenue = 1500000.0,
                totalDp = 0.0
            )
        }

    override suspend fun getSalesReport(
        startDate: String?,
        endDate: String?,
        cashierId: String?,
        sessionId: String?,
        customerId: String?,
        productId: String?,
        categoryId: String?,
        paymentMethod: String?,
        status: String?,
        topProductsLimit: Int
    ): SalesReportResponseDto = SalesReportResponseDto(
        range = SalesReportRangeDto(startDate = startDate ?: "", endDate = endDate ?: ""),
        totals = SalesReportTotalsDto(
            transactionCount = 15,
            grossRevenue = BigDecimal("5000000"),
            discountAmount = BigDecimal("200000"),
            paidAmount = BigDecimal("4500000"),
            outstandingAmount = BigDecimal("500000"),
            grossProfit = BigDecimal("1200000"),
            refundAmount = BigDecimal.ZERO,
            netRevenue = BigDecimal("4800000")
        ),
        topProducts = listOf(
            TopProductSalesDto(
                productId = "prod-1",
                productName = "Semen Tiga Roda 50kg",
                qtySold = BigDecimal("50"),
                revenue = BigDecimal("3250000"),
                grossProfit = BigDecimal("450000")
            )
        ),
        receivables = SalesReceivableSummaryDto(
            createdReceivableAmount = BigDecimal("12500000"),
            paidAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("12500000"),
            receivableCount = 4L
        ),
        voided = VoidedSalesSummaryDto(
            transactionCount = 1,
            amount = BigDecimal("150000"),
            paidAmount = BigDecimal("150000")
        )
    )

    override suspend fun exportCsv(type: ReportCsvType, filter: AnalyticsFilter, output: OutputStream) = Unit
}

private class FakeDashboardCashReconciliationRepository : CashReconciliationRepository {
    override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(
        CashSession(
            id = "session-1",
            userId = "user-1",
            userName = "Budi Kasir",
            openingCash = BigDecimal("200000"),
            closingCash = null,
            systemCash = null,
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
            openedAt = "2026-10-19T08:00:00Z",
            closedAt = null
        )
    )

    override suspend fun getTransactions(
        page: Int,
        limit: Int,
        sessionId: String?,
        search: String?,
        receiptNumber: String?,
        cashierId: String?,
        customerId: String?,
        paymentMethod: String?,
        status: String?,
        startDate: String?,
        endDate: String?
    ): RepositoryResult<CashTransactionPage> = RepositoryResult.Success(
        CashTransactionPage(
            data = listOf(
                CashTransaction(
                    id = "trx-1",
                    receiptId = "TRX-001",
                    sessionId = "session-1",
                    customerId = "cust-1",
                    customerName = "Pak Slamet",
                    cashierName = "Budi Kasir",
                    paymentMethods = listOf("TUNAI"),
                    type = "SALE",
                    status = "Lunas",
                    total = BigDecimal("150000"),
                    paidAmount = BigDecimal("150000"),
                    createdAt = "2026-10-19T09:30:00Z"
                )
            ),
            total = 1,
            page = 1,
            limit = 5,
            totalPages = 1
        )
    )

    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("Not used")
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("Not used")
    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("Not used")
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("Not used")
    override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("Not used")
    override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("Not used")
    override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("Not used")
    override suspend fun refundTransaction(id: String, reason: String, disposition: com.tbterminal.app.data.model.RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("Not used")
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("Not used")
    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = error("Not used")
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("Not used")
    override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String): RepositoryResult<CashTransactionDetail> = error("Not used")
}
