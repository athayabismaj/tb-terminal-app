package com.tbterminal.app.ui.cashier.transactions

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.RoomPendingSubmissionStore
import com.tbterminal.app.data.local.database.TbTerminalDatabase
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
import java.io.IOException
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CashierRefundVoidRoomSurvivalTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: TbTerminalDatabase
    private lateinit var store: RoomPendingSubmissionStore
    private var nowMillis = 1_800_000_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { command -> command.run() }
            .setTransactionExecutor { command -> command.run() }
            .build()
        store = RoomPendingSubmissionStore(
            dao = database.pendingSubmissionDao(),
            ownerUserIdProvider = { "kasir-1" },
            clockMillis = { nowMillis },
            monotonicClockMillis = { nowMillis },
            bootCountProvider = { 1L },
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun forceCloseMidRefund_reopen_retryWithNewApprovalOrDifferentReason_reusesExactSameKeyInRoom() = runTest(dispatcher) {
        val inFlightGate = CompletableDeferred<Unit>()
        val repo = FakeRoomRefundVoidRepo().apply {
            refundGate = inFlightGate
        }
        var seq = 0

        // 1. Kasir opens receipt, fills refund, gets approval-1, submits -> process killed while IN_FLIGHT
        val vmBeforeKill = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "uuid-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        vmBeforeKill.loadReceipt("tx-hp-1")
        advanceUntilIdle()
        vmBeforeKill.showRefundDialog()
        vmBeforeKill.onRefundDispositionChanged(RefundDisposition.RETURN_TO_STOCK)
        vmBeforeKill.onRefundReasonChanged("Salah beli barang")
        vmBeforeKill.submitRefund()
        vmBeforeKill.onManagerApprovalGranted(grant("approval-1", ManagerApprovalAction.REFUND_TRANSACTION, "tx-hp-1"))
        runCurrent()

        val firstSent = repo.refundCalls.single()
        assertEquals(PendingSubmissionStatus.IN_FLIGHT, store.load(PendingSubmissionScopes.CASHIER_REFUND)?.status)

        // 2. Simulate force-close + reopen app with new ViewModel reading from same Room DB
        val vmAfterRestart = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "uuid-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        repo.refundGate = null
        vmAfterRestart.loadReceipt("tx-hp-1")
        advanceUntilIdle()

        assertTrue(vmAfterRestart.uiState.value.isRefundOutcomeAmbiguous)
        assertEquals(firstSent.idempotencyKey, vmAfterRestart.uiState.value.refundIdempotencyKey)

        // Retry gets MANAGER_APPROVAL_EXPIRED (e.g. approval-1 already used or expired on server)
        repo.nextRefundResult = RepositoryResult.Error("MANAGER_APPROVAL_EXPIRED", "Persetujuan kedaluwarsa")
        vmAfterRestart.showRefundDialog()
        vmAfterRestart.submitRefund()
        advanceUntilIdle()

        assertEquals(2, repo.refundCalls.size)
        assertEquals(firstSent.idempotencyKey, repo.refundCalls[1].idempotencyKey)
        assertNotNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
        assertTrue(vmAfterRestart.uiState.value.isRefundOutcomeAmbiguous)
        assertNull(vmAfterRestart.uiState.value.refundManagerApprovalId)

        // 3. Kasir requests new approval-2 and submits -> MUST STILL reuse firstSent.idempotencyKey!
        vmAfterRestart.submitRefund()
        assertEquals(ManagerApprovalAction.REFUND_TRANSACTION, vmAfterRestart.uiState.value.pendingManagerApprovalAction)
        vmAfterRestart.onManagerApprovalGranted(grant("approval-2", ManagerApprovalAction.REFUND_TRANSACTION, "tx-hp-1"))
        advanceUntilIdle()

        assertEquals(3, repo.refundCalls.size)
        assertEquals(firstSent.idempotencyKey, repo.refundCalls[2].idempotencyKey)
        assertEquals("approval-2", repo.refundCalls[2].managerApprovalId)
        assertNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
        assertEquals("REFUNDED", vmAfterRestart.uiState.value.selectedTransaction?.status)
    }

    @Test
    fun refundConflictResponses_uniqueViolation409_validation400_andIdempotencyConflictWhenRefunded_recoverCleanly() = runTest(dispatcher) {
        val repo = FakeRoomRefundVoidRepo()
        var seq = 0

        suspend fun seedAmbiguousRefund(vm: CashierTransactionHistoryViewModel): String {
            vm.loadReceipt("tx-hp-1")
            advanceUntilIdle()
            vm.showRefundDialog()
            vm.onRefundReasonChanged("Salah beli barang")
            repo.nextRefundResult = RepositoryResult.Exception(IOException("timeout"))
            vm.submitRefund()
            advanceUntilIdle()
            assertNotNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
            return repo.refundCalls.last().idempotencyKey
        }

        // A. HTTP 409 DUPLICATE_DATA (backend uq_transaction_refund_transaction unique violation)
        val vmA = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "uuid-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        seedAmbiguousRefund(vmA)
        repo.detail = repo.detail.copy(status = "REFUNDED")
        repo.nextRefundResult = RepositoryResult.Error("DUPLICATE_DATA", "Data sudah ada")
        vmA.submitRefund()
        advanceUntilIdle()

        assertNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
        assertFalse(vmA.uiState.value.isRefundDialogOpen)
        assertFalse(vmA.uiState.value.isRefundOutcomeAmbiguous)
        assertEquals("Transaksi sudah pernah direfund.", vmA.uiState.value.receiptMessage)
        assertEquals("REFUNDED", vmA.uiState.value.selectedTransaction?.status)

        // B. HTTP 400 VALIDATION_ERROR "Transaksi sudah pernah direfund"
        repo.detail = repo.detail.copy(status = "LUNAS")
        val vmB = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "uuid-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        seedAmbiguousRefund(vmB)
        repo.detail = repo.detail.copy(status = "REFUNDED")
        repo.nextRefundResult = RepositoryResult.Error("VALIDATION_ERROR", "Transaksi sudah pernah direfund")
        vmB.submitRefund()
        advanceUntilIdle()

        assertNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
        assertFalse(vmB.uiState.value.isRefundDialogOpen)
        assertEquals("Transaksi sudah pernah direfund.", vmB.uiState.value.receiptMessage)
        assertEquals("REFUNDED", vmB.uiState.value.selectedTransaction?.status)

        // C. HTTP 400 VALIDATION_ERROR "idempotencyKey sudah digunakan untuk permintaan refund yang berbeda"
        //    (e.g. old refund succeeded on server with approval-1, retry reused key with approval-2)
        repo.detail = repo.detail.copy(status = "LUNAS")
        val vmC = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "uuid-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        seedAmbiguousRefund(vmC)
        repo.detail = repo.detail.copy(status = "REFUNDED")
        repo.nextRefundResult = RepositoryResult.Error(
            "VALIDATION_ERROR",
            "idempotencyKey sudah digunakan untuk permintaan refund yang berbeda",
        )
        vmC.submitRefund()
        advanceUntilIdle()

        assertNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
        assertFalse(vmC.uiState.value.isRefundDialogOpen)
        assertEquals("Transaksi sudah pernah direfund.", vmC.uiState.value.receiptMessage)
        assertEquals("REFUNDED", vmC.uiState.value.selectedTransaction?.status)
    }

    @Test
    fun loadReceipt_whenAlreadyRefundedOrVoided_clearsStalePendingKeysInRoom() = runTest(dispatcher) {
        val repo = FakeRoomRefundVoidRepo()
        val vm1 = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "stale-1" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        vm1.loadReceipt("tx-hp-1")
        advanceUntilIdle()
        vm1.showRefundDialog()
        vm1.onRefundReasonChanged("Salah beli barang")
        repo.nextRefundResult = RepositoryResult.Exception(IOException("timeout"))
        vm1.submitRefund()
        advanceUntilIdle()
        assertNotNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))

        // After restart, receipt status on server is already REFUNDED -> stale key is cleared
        repo.detail = repo.detail.copy(status = "REFUNDED")
        val vm2 = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "fresh-2" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        vm2.loadReceipt("tx-hp-1")
        advanceUntilIdle()

        assertNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
        assertFalse(vm2.uiState.value.isRefundOutcomeAmbiguous)
        assertNull(vm2.uiState.value.refundIdempotencyKey)
    }

    @Test
    fun afterRestartWithPendingRefund_evenIfReasonAndDispositionDiffer_reusesPersistedKeyForSameTransaction() = runTest(dispatcher) {
        val repo = FakeRoomRefundVoidRepo()
        val vm1 = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "orig-key-1" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        vm1.loadReceipt("tx-hp-1")
        advanceUntilIdle()
        vm1.showRefundDialog()
        vm1.onRefundDispositionChanged(RefundDisposition.RETURN_TO_STOCK)
        vm1.onRefundReasonChanged("Alasan awal")
        repo.nextRefundResult = RepositoryResult.Exception(IOException("network dropped"))
        vm1.submitRefund()
        advanceUntilIdle()
        val originalKey = repo.refundCalls.single().idempotencyKey

        // Simulate restart where Room holds pending refund for tx-hp-1
        val vm2 = CashierTransactionHistoryViewModel(
            repository = repo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "should-never-use-2" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis },
        )
        vm2.loadReceipt("tx-hp-1")
        advanceUntilIdle()
        vm2.showRefundDialog()
        // Even if UI callbacks are invoked, key for tx-hp-1 remains locked and unchanged
        vm2.onRefundDispositionChanged(RefundDisposition.DAMAGED)
        vm2.onRefundReasonChanged("Alasan berbeda setelah restart")
        vm2.submitRefund()
        advanceUntilIdle()

        assertEquals(2, repo.refundCalls.size)
        assertEquals(originalKey, repo.refundCalls[1].idempotencyKey)
        assertNull(store.load(PendingSubmissionScopes.CASHIER_REFUND))
    }

    private fun grant(approvalId: String, action: ManagerApprovalAction, txId: String) = ManagerApprovalGrant(
        approvalId = approvalId,
        action = action,
        resourceType = ManagerApprovalResourceType.TRANSACTION,
        resourceId = txId,
        status = ManagerApprovalStatus.APPROVED,
        createdAt = "2026-11-28T00:00:00Z",
        expiresAt = "2026-11-28T00:05:00Z",
    )
}

private data class RecordedRefundCall(
    val idempotencyKey: String,
    val reason: String,
    val disposition: RefundDisposition,
    val managerApprovalId: String?,
)

private class FakeRoomRefundVoidRepo : CashReconciliationRepository {
    var detail = CashTransactionDetail(
        id = "tx-hp-1",
        receiptId = "TRX-HP-1",
        sessionId = "s1",
        customerId = null,
        customerName = null,
        type = "PENJUALAN",
        status = "LUNAS",
        total = BigDecimal("100000"),
        paidAmount = BigDecimal("100000"),
        amountTendered = BigDecimal("100000"),
        changeAmount = BigDecimal.ZERO,
        createdAt = "2026-11-28T00:00:00Z",
        items = emptyList(),
    )
    var refundGate: CompletableDeferred<Unit>? = null
    var nextRefundResult: RepositoryResult<TransactionRefundResult>? = null
    var nextVoidResult: RepositoryResult<TransactionVoidResult>? = null
    val refundCalls = mutableListOf<RecordedRefundCall>()

    override suspend fun getTransactionById(id: String) = RepositoryResult.Success(detail)

    override suspend fun refundTransaction(
        id: String,
        reason: String,
        disposition: RefundDisposition,
        idempotencyKey: String,
        managerApprovalId: String?,
    ): RepositoryResult<TransactionRefundResult> {
        refundCalls += RecordedRefundCall(idempotencyKey, reason, disposition, managerApprovalId)
        refundGate?.await()
        nextRefundResult?.let {
            nextRefundResult = null
            return it
        }
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
                createdAt = "2026-11-28T00:05:00Z",
                idempotentReplay = false,
            )
        )
    }

    override suspend fun voidTransaction(
        id: String,
        reason: String,
        idempotencyKey: String,
        managerApprovalId: String?,
    ): RepositoryResult<TransactionVoidResult> {
        nextVoidResult?.let {
            nextVoidResult = null
            return it
        }
        detail = detail.copy(status = "VOIDED", voidReason = reason, voidedAt = "2026-11-28T00:05:00Z")
        return RepositoryResult.Success(
            TransactionVoidResult("void-1", id, detail.receiptId, reason, detail.voidedAt!!, false)
        )
    }

    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("unused")
    override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(null)
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("unused")
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?) =
        RepositoryResult.Success(CashTransactionPage(emptyList(), 0, 1, limit, 1))
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("unused")
    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = error("unused")
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
    override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
}
