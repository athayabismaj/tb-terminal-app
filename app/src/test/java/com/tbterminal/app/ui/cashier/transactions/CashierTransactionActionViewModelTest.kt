package com.tbterminal.app.ui.cashier.transactions

import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.ManagerApprovalResourceType
import com.tbterminal.app.data.model.ManagerApprovalStatus
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.TransactionVoidResult
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CashierTransactionActionViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun ownerVoidRunsDirectlyAndRefreshesDetail() = runTest(dispatcher) {
        val repository = FakeTransactionActionRepository()
        val viewModel = loadedViewModel(repository, "OWNER")

        viewModel.showVoidDialog()
        viewModel.onVoidReasonChanged("Salah memilih barang")
        viewModel.submitVoid()
        advanceUntilIdle()

        assertEquals(1, repository.voidCalls)
        assertEquals(null, repository.lastVoidApprovalId)
        assertEquals("VOIDED", viewModel.uiState.value.selectedTransaction?.status)
        assertFalse(viewModel.uiState.value.isVoidDialogOpen)
    }

    @Test
    fun cashierVoidWaitsForCorrectApproval() = runTest(dispatcher) {
        val repository = FakeTransactionActionRepository()
        val viewModel = loadedViewModel(repository, "KASIR")

        viewModel.showVoidDialog()
        viewModel.onVoidReasonChanged("Salah memilih barang")
        viewModel.submitVoid()

        assertEquals(ManagerApprovalAction.VOID_TRANSACTION, viewModel.uiState.value.pendingManagerApprovalAction)
        assertEquals(0, repository.voidCalls)

        viewModel.onManagerApprovalGranted(grant(ManagerApprovalAction.VOID_TRANSACTION))
        advanceUntilIdle()

        assertEquals(1, repository.voidCalls)
        assertEquals("approval-1", repository.lastVoidApprovalId)
    }

    @Test
    fun wrongApprovalActionIsRejected() = runTest(dispatcher) {
        val repository = FakeTransactionActionRepository()
        val viewModel = loadedViewModel(repository, "KASIR")

        viewModel.showVoidDialog()
        viewModel.onVoidReasonChanged("Salah memilih barang")
        viewModel.submitVoid()
        viewModel.onManagerApprovalGranted(grant(ManagerApprovalAction.REFUND_TRANSACTION))
        advanceUntilIdle()

        assertEquals(0, repository.voidCalls)
        assertNotNull(viewModel.uiState.value.voidErrorMessage)
        assertTrue(viewModel.uiState.value.isVoidDialogOpen)
    }

    @Test
    fun adminRefundSendsDispositionAndRefreshesStatus() = runTest(dispatcher) {
        val repository = FakeTransactionActionRepository()
        val viewModel = loadedViewModel(repository, "ADMIN")

        viewModel.showRefundDialog()
        viewModel.onRefundDispositionChanged(RefundDisposition.DAMAGED)
        viewModel.onRefundReasonChanged("Barang diterima dalam kondisi rusak")
        viewModel.submitRefund()
        advanceUntilIdle()

        assertEquals(1, repository.refundCalls)
        assertEquals(RefundDisposition.DAMAGED, repository.lastRefundDisposition)
        assertEquals(null, repository.lastRefundApprovalId)
        assertEquals("REFUNDED", viewModel.uiState.value.selectedTransaction?.status)
        assertEquals("RF-001", viewModel.uiState.value.refundResult?.refundNumber)
    }

    @Test
    fun cashierRefundWaitsForApproval() = runTest(dispatcher) {
        val repository = FakeTransactionActionRepository()
        val viewModel = loadedViewModel(repository, "KASIR")

        viewModel.showRefundDialog()
        viewModel.onRefundReasonChanged("Pelanggan mengembalikan seluruh barang")
        viewModel.submitRefund()

        assertEquals(ManagerApprovalAction.REFUND_TRANSACTION, viewModel.uiState.value.pendingManagerApprovalAction)
        assertEquals(0, repository.refundCalls)

        viewModel.onManagerApprovalGranted(grant(ManagerApprovalAction.REFUND_TRANSACTION))
        advanceUntilIdle()
        assertEquals(1, repository.refundCalls)
        assertEquals("approval-1", repository.lastRefundApprovalId)
    }

    @Test
    fun doubleVoidSubmitStartsOnlyOneRequest() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeTransactionActionRepository(voidGate = gate)
        val viewModel = loadedViewModel(repository, "OWNER")
        viewModel.showVoidDialog()
        viewModel.onVoidReasonChanged("Salah memilih barang")

        viewModel.submitVoid()
        viewModel.submitVoid()
        runCurrent()

        assertEquals(1, repository.voidCalls)
        assertTrue(viewModel.uiState.value.isSubmittingVoid)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun doubleRefundSubmitStartsOnlyOneRequest() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeTransactionActionRepository(refundGate = gate)
        val viewModel = loadedViewModel(repository, "ADMIN")
        viewModel.showRefundDialog()
        viewModel.onRefundReasonChanged("Pelanggan mengembalikan seluruh barang")

        viewModel.submitRefund()
        viewModel.submitRefund()
        runCurrent()

        assertEquals(1, repository.refundCalls)
        assertTrue(viewModel.uiState.value.isSubmittingRefund)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun ambiguousVoidRetryKeepsSameIdempotencyKeyAndReason() = runTest(dispatcher) {
        val repository = FakeTransactionActionRepository(voidErrorOnce = "NETWORK_TIMEOUT")
        val viewModel = loadedViewModel(repository, "OWNER")
        viewModel.showVoidDialog()
        viewModel.onVoidReasonChanged("Salah memilih barang")

        viewModel.submitVoid()
        advanceUntilIdle()
        val preservedReason = viewModel.uiState.value.voidReasonInput
        assertTrue(viewModel.uiState.value.isVoidOutcomeAmbiguous)

        viewModel.onVoidReasonChanged("Alasan diubah")
        assertEquals(preservedReason, viewModel.uiState.value.voidReasonInput)
        viewModel.submitVoid()
        advanceUntilIdle()

        assertEquals(2, repository.voidCalls)
        assertEquals(repository.voidKeys[0], repository.voidKeys[1])
        assertEquals("VOIDED", viewModel.uiState.value.selectedTransaction?.status)
    }

    private suspend fun loadedViewModel(
        repository: FakeTransactionActionRepository,
        role: String,
    ): CashierTransactionHistoryViewModel {
        val viewModel = CashierTransactionHistoryViewModel(repository = repository, actorRole = role)
        viewModel.loadReceipt(repository.detail.id)
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    private fun grant(action: ManagerApprovalAction) = ManagerApprovalGrant(
        approvalId = "approval-1",
        action = action,
        resourceType = ManagerApprovalResourceType.TRANSACTION,
        resourceId = "transaction-1",
        status = ManagerApprovalStatus.APPROVED,
        createdAt = "2026-09-01T00:00:00Z",
        expiresAt = "2026-09-01T00:05:00Z",
    )
}

private class FakeTransactionActionRepository(
    private val voidGate: CompletableDeferred<Unit>? = null,
    private val refundGate: CompletableDeferred<Unit>? = null,
    voidErrorOnce: String? = null,
) : CashReconciliationRepository {
    var detail = transactionDetail()
    var voidCalls = 0
    var refundCalls = 0
    var lastVoidApprovalId: String? = null
    var lastRefundApprovalId: String? = null
    var lastRefundDisposition: RefundDisposition? = null
    val voidKeys = mutableListOf<String>()
    private var pendingVoidErrorCode = voidErrorOnce

    override suspend fun getTransactionById(id: String) = RepositoryResult.Success(detail)

    override suspend fun voidTransaction(
        id: String,
        reason: String,
        idempotencyKey: String,
        managerApprovalId: String?,
    ): RepositoryResult<TransactionVoidResult> {
        voidCalls++
        voidKeys += idempotencyKey
        lastVoidApprovalId = managerApprovalId
        voidGate?.await()
        pendingVoidErrorCode?.let { code ->
            pendingVoidErrorCode = null
            return RepositoryResult.Error(code, "timeout")
        }
        detail = detail.copy(status = "VOIDED", voidReason = reason, voidedAt = "2026-09-01T01:00:00Z")
        return RepositoryResult.Success(
            TransactionVoidResult("void-1", id, detail.receiptId, reason, detail.voidedAt!!, false),
        )
    }

    override suspend fun refundTransaction(
        id: String,
        reason: String,
        disposition: RefundDisposition,
        idempotencyKey: String,
        managerApprovalId: String?,
    ): RepositoryResult<TransactionRefundResult> {
        refundCalls++
        lastRefundApprovalId = managerApprovalId
        lastRefundDisposition = disposition
        refundGate?.await()
        detail = detail.copy(status = "REFUNDED")
        return RepositoryResult.Success(
            TransactionRefundResult(
                refundId = "refund-1",
                refundNumber = "RF-001",
                transactionId = id,
                status = "COMPLETED",
                transactionAmount = BigDecimal("100000"),
                refundedAmount = BigDecimal("100000"),
                returnDisposition = disposition,
                reason = reason,
                createdAt = "2026-09-01T01:00:00Z",
                idempotentReplay = false,
            ),
        )
    }

    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?) =
        RepositoryResult.Success(CashSessionPage(emptyList(), 0, page, limit, 1))
    override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(null)
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("Not used")
    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("Not used")
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("Not used")
    override suspend fun getTransactions(
        page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?,
        cashierId: String?, customerId: String?, paymentMethod: String?, status: String?,
        startDate: String?, endDate: String?,
    ) = RepositoryResult.Success(CashTransactionPage(emptyList(), 0, page, limit, 1))
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("Not used")
    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = RepositoryResult.Success(emptyList())
    override suspend fun getExpenseHistory(
        page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?,
    ) = RepositoryResult.Success(CashExpensePage(emptyList(), 0, page, limit, 1))
    override suspend fun payTransactionDebt(
        transactionId: String,
        amount: BigDecimal,
        method: String,
    ) = RepositoryResult.Success(detail)
}

private fun transactionDetail() = CashTransactionDetail(
    id = "transaction-1",
    receiptId = "TRX-1",
    sessionId = "session-1",
    customerId = null,
    customerName = null,
    type = "PENJUALAN",
    status = "LUNAS",
    total = BigDecimal("100000"),
    paidAmount = BigDecimal("100000"),
    amountTendered = BigDecimal("100000"),
    changeAmount = BigDecimal.ZERO,
    createdAt = "2026-09-01T00:00:00Z",
    items = emptyList(),
)
