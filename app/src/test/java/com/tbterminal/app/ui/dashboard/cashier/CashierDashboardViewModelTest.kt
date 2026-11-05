package com.tbterminal.app.ui.dashboard.cashier

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
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
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
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CashierDashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fakeCashRepo = FakeCashierDashboardCashReconciliationRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDashboardData_withActiveSession_calculatesRevenueAndTransactionCount() = runTest {
        val testSession = CashSession(
            id = "sess-100",
            userId = "cashier-1",
            userName = "Budi Kasir",
            openedAt = "2026-11-05T08:00:00Z",
            closedAt = null,
            openingCash = BigDecimal("100000"),
            closingCash = null,
            systemCash = BigDecimal("350000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            totalCashIn = BigDecimal.ZERO,
            notes = "Shift Pagi",
            status = "OPEN"
        )
        fakeCashRepo.activeSession = testSession

        fakeCashRepo.transactions = listOf(
            CashTransaction(
                id = "trx-1",
                receiptId = "TRX-000001",
                sessionId = "sess-100",
                customerId = null,
                customerName = "Pelanggan Umum",
                type = "SALE",
                status = "PAID",
                total = BigDecimal("150000"),
                paidAmount = BigDecimal("150000"),
                createdAt = "2026-11-05T08:30:00Z"
            ),
            CashTransaction(
                id = "trx-2",
                receiptId = "TRX-000002",
                sessionId = "sess-100",
                customerId = null,
                customerName = "Pak Joko",
                type = "SALE",
                status = "PAID",
                total = BigDecimal("100000"),
                paidAmount = BigDecimal("100000"),
                createdAt = "2026-11-05T09:15:00Z"
            ),
            // EXPENSE should be excluded from sales revenue
            CashTransaction(
                id = "trx-3",
                receiptId = "EXP-000001",
                sessionId = "sess-100",
                customerId = null,
                customerName = null,
                type = "EXPENSE",
                status = "COMPLETED",
                total = BigDecimal("25000"),
                paidAmount = BigDecimal("25000"),
                createdAt = "2026-11-05T09:30:00Z"
            ),
            // BATAL should be excluded from sales revenue
            CashTransaction(
                id = "trx-4",
                receiptId = "TRX-000003",
                sessionId = "sess-100",
                customerId = null,
                customerName = "Ibu Siti",
                type = "SALE",
                status = "BATAL",
                total = BigDecimal("50000"),
                paidAmount = BigDecimal("50000"),
                createdAt = "2026-11-05T09:45:00Z"
            )
        )

        val viewModel = CashierDashboardViewModel(repository = fakeCashRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.activeSession)
        assertEquals("sess-100", state.activeSession?.id)
        assertEquals(BigDecimal("100000"), state.openingCash)
        assertEquals(BigDecimal("350000"), state.systemCash)
        // 150000 + 100000 = 250000
        assertEquals(BigDecimal("250000"), state.todaySalesRevenue)
        assertEquals(2, state.todayTransactionsCount)
        assertEquals(2, state.recentTransactions.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadDashboardData_noActiveSession_resetsMetrics() = runTest {
        fakeCashRepo.activeSession = null

        val viewModel = CashierDashboardViewModel(repository = fakeCashRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.activeSession)
        assertEquals(BigDecimal.ZERO, state.todaySalesRevenue)
        assertEquals(0, state.todayTransactionsCount)
        assertEquals(0, state.recentTransactions.size)
    }

    @Test
    fun clearErrorMessage_resetsError() = runTest {
        fakeCashRepo.errorToReturn = "Server error"

        val viewModel = CashierDashboardViewModel(repository = fakeCashRepo)
        advanceUntilIdle()

        assertEquals("Server error", viewModel.uiState.value.errorMessage)
        viewModel.clearErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

private class FakeCashierDashboardCashReconciliationRepository : CashReconciliationRepository {
    var activeSession: CashSession? = null
    var transactions: List<CashTransaction> = emptyList()
    var errorToReturn: String? = null

    override suspend fun getActiveSession(): RepositoryResult<CashSession?> {
        errorToReturn?.let { return RepositoryResult.Error(code = "ERR_SESSION", message = it) }
        return RepositoryResult.Success(activeSession)
    }

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
    ): RepositoryResult<CashTransactionPage> {
        errorToReturn?.let { return RepositoryResult.Error(code = "ERR_TX", message = it) }
        val filtered = transactions.filter { trx ->
            (sessionId == null || trx.sessionId == sessionId)
        }
        return RepositoryResult.Success(
            CashTransactionPage(
                data = filtered,
                total = filtered.size.toLong(),
                page = page,
                limit = limit,
                totalPages = 1
            )
        )
    }

    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("Not used")
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("Not used")
    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("Not used")
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("Not used")
    override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("Not used")
    override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("Not used")
    override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("Not used")
    override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("Not used")
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("Not used")
    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = error("Not used")
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("Not used")
    override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String): RepositoryResult<CashTransactionDetail> = error("Not used")
}
