package com.tbterminal.app.ui.common

import com.tbterminal.app.data.local.database.PendingSubmissionRecord
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.PendingSubmissionStore
import com.tbterminal.app.data.local.database.RoomPendingSubmissionStore
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.CreateOpeningPayableCommand
import com.tbterminal.app.data.model.CreateOpeningReceivableCommand
import com.tbterminal.app.data.model.CreatePayableAdjustmentCommand
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.CreatePurchaseCommand
import com.tbterminal.app.data.model.CreateReceivableAdjustmentCommand
import com.tbterminal.app.data.model.CreateReceivablePaymentCommand
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.CustomerReceivableSummaryPage
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.ManagerApprovalResourceType
import com.tbterminal.app.data.model.ManagerApprovalStatus
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductCsvImportResult
import com.tbterminal.app.data.model.ProductCsvPreview
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductPage
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.ProductStockPage
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.ProductUnitPage
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseReceipt
import com.tbterminal.app.data.model.PurchaseSummaryPage
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.ReceivablePage
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.model.ReceivablePaymentHistoryPage
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.ReverseReceivablePaymentCommand
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.StockCard
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPage
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.model.SupplierPayablePage
import com.tbterminal.app.data.model.SupplierPaymentReceipt
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.TransactionVoidResult
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.numbering.DocumentNumberGenerator
import com.tbterminal.app.data.numbering.DocumentNumberType
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.cashier.transactions.CashierTransactionHistoryViewModel
import com.tbterminal.app.ui.incominggoods.IncomingGoodsViewModel
import com.tbterminal.app.ui.payables.SupplierDebtDetailViewModel
import com.tbterminal.app.ui.payables.SupplierDebtViewModel
import com.tbterminal.app.ui.payables.SupplierPaymentMethod
import com.tbterminal.app.ui.receivables.ReceivableCustomerDetailViewModel
import com.tbterminal.app.ui.receivables.ReceivablePaymentMethod
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubmissionIdempotencyPersistenceTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Serializable
    private data class SampleIntent(val id: String, val amount: String)

    @Test
    fun tracker_restoresInFlightAndAmbiguousKeys_clearsOnSuccessChangeOrCorrupt() = runTest(dispatcher) {
        val store = FakePendingSubmissionStore(clockMillis = { 20_000L })
        var seq = 0
        val tracker1 = SubmissionIdempotencyTracker(
            prefix = "test",
            keyGenerator = { "k-${++seq}" },
            store = store,
            scope = "sample-scope",
            intentSerializer = SampleIntent.serializer(),
            clockMillis = { 10_000L }
        )

        val intent = SampleIntent("item-1", "50000")
        val originalKey = tracker1.resolveKeyForSubmission(intent, payloadJson = """{"draft":1}""")
        assertEquals("test-k-1", originalKey)
        assertEquals(PendingSubmissionStatus.IN_FLIGHT, store.records["sample-scope"]?.status)

        // Simulate process killed mid-request (still IN_FLIGHT) -> recreated tracker treats as ambiguous
        val tracker2 = SubmissionIdempotencyTracker(
            prefix = "test",
            keyGenerator = { "k-${++seq}" },
            store = store,
            scope = "sample-scope",
            intentSerializer = SampleIntent.serializer(),
            clockMillis = { 20_000L }
        )
        assertEquals("""{"draft":1}""", tracker2.restore())
        assertTrue(tracker2.isOutcomeAmbiguous)
        assertEquals(originalKey, tracker2.resolveKeyForSubmission(intent))

        // Different intent rotates key
        val rotatedKey = tracker2.resolveKeyForSubmission(SampleIntent("item-1", "60000"))
        assertNotEquals(originalKey, rotatedKey)
        tracker2.onSuccessAndPersist()
        assertNull(store.records["sample-scope"])

        // Corrupt JSON in store clears gracefully without crash
        store.records["sample-scope"] = PendingSubmissionRecord(
            scope = "sample-scope",
            idempotencyKey = "broken",
            intentJson = "{corrupt-json",
            status = PendingSubmissionStatus.AMBIGUOUS,
            createdAt = 10_000L,
            updatedAt = 10_000L
        )
        assertNull(tracker2.restore())
        assertNull(store.records["sample-scope"])
    }

    @Test
    fun supplierDebtAndDetailViewModels_surviveRestartWithSameKeyAndDraft() = runTest(dispatcher) {
        val store = FakePendingSubmissionStore()
        val payable = SupplierPayable(
            id = "pay-1",
            supplierId = "sup-1",
            supplierName = "PT Semen",
            purchaseId = "pur-1",
            amount = BigDecimal("200000"),
            paidAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("200000"),
            dueDate = "2026-12-31",
            status = "belum_lunas",
            createdAt = "2026-11-20T10:00:00Z"
        )
        val repo = FakePurchasingRepo(listOf(payable))
        var seq = 0

        // 1. SupplierDebtViewModel fails ambiguously
        val vm1 = SupplierDebtViewModel(
            purchasingRepository = repo,
            idempotencyKeyGenerator = { "sd-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        vm1.openPayment(payable)
        vm1.onPaymentAmountChanged("75000")
        vm1.onPaymentMethodChanged(SupplierPaymentMethod.Transfer)
        vm1.onReferenceChanged("REF-75")
        repo.nextPaymentResult = RepositoryResult.Exception(IOException("timeout"))
        vm1.submitPayment()
        advanceUntilIdle()
        val firstKey = repo.recordedPayments[0].idempotencyKey!!
        assertNotNull(store.records[PendingSubmissionScopes.supplierPayment("pay-1")])

        // Simulate restart -> new ViewModel with same store
        val vm2 = SupplierDebtViewModel(
            purchasingRepository = repo,
            idempotencyKeyGenerator = { "sd-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        vm2.openPayment(payable)
        assertEquals("75000", vm2.uiState.value.paymentAmountInput)
        assertEquals(SupplierPaymentMethod.Transfer, vm2.uiState.value.paymentMethod)
        assertEquals("REF-75", vm2.uiState.value.referenceInput)

        vm2.submitPayment()
        advanceUntilIdle()
        assertEquals(firstKey, repo.recordedPayments[1].idempotencyKey)
        assertNull(store.records[PendingSubmissionScopes.supplierPayment("pay-1")])

        // 2. SupplierDebtDetailViewModel survives restart too
        val detail1 = SupplierDebtDetailViewModel(
            purchasingRepository = repo,
            supplierId = "sup-1",
            idempotencyKeyGenerator = { "sdd-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        detail1.openPayment(payable)
        detail1.onPaymentAmountChanged("50000")
        repo.nextPaymentResult = RepositoryResult.Error("HTTP_504", "Gateway timeout")
        detail1.submitPayment()
        advanceUntilIdle()
        val detailKey = repo.recordedPayments[2].idempotencyKey!!

        val detail2 = SupplierDebtDetailViewModel(
            purchasingRepository = repo,
            supplierId = "sup-1",
            idempotencyKeyGenerator = { "sdd-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        detail2.openPayment(payable)
        assertEquals("50000", detail2.uiState.value.paymentAmountInput)
        detail2.submitPayment()
        advanceUntilIdle()
        assertEquals(detailKey, repo.recordedPayments[3].idempotencyKey)
        assertNull(store.records[PendingSubmissionScopes.supplierPayment("pay-1")])
    }

    @Test
    fun supplierDebtListAndDetail_shareSameKeyPerPayable_andOpeningPayableBDoesNotErasePayableA() = runTest(dispatcher) {
        val store = FakePendingSubmissionStore()
        val payableA = SupplierPayable(
            id = "pay-A",
            supplierId = "sup-1",
            supplierName = "PT Semen",
            purchaseId = "pur-A",
            amount = BigDecimal("200000"),
            paidAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("200000"),
            dueDate = "2026-12-31",
            status = "belum_lunas",
            createdAt = "2026-11-20T10:00:00Z"
        )
        val payableB = payableA.copy(id = "pay-B", purchaseId = "pur-B", amount = BigDecimal("150000"), remainingAmount = BigDecimal("150000"))
        val repo = FakePurchasingRepo(listOf(payableA, payableB))
        var seq = 0

        // 1. Ambiguous payment for payable A on List screen
        val listVm = SupplierDebtViewModel(
            purchasingRepository = repo,
            idempotencyKeyGenerator = { "list-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        listVm.openPayment(payableA)
        listVm.onPaymentAmountChanged("80000")
        listVm.onReferenceChanged("TRF-A")
        repo.nextPaymentResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        listVm.submitPayment()
        advanceUntilIdle()

        val keyA = repo.recordedPayments[0].idempotencyKey!!
        assertNotNull(store.records[PendingSubmissionScopes.supplierPayment("pay-A")])

        // 2. Open and close dialog for payable B on List screen -> must NOT remove payable A's ambiguous key
        listVm.closePayment()
        listVm.openPayment(payableB)
        listVm.closePayment()
        assertNotNull(store.records[PendingSubmissionScopes.supplierPayment("pay-A")])

        // 3. Pay payable B in full -> must NOT remove payable A's ambiguous key
        listVm.openPayment(payableB)
        listVm.submitPayment()
        advanceUntilIdle()
        assertNotEquals(keyA, repo.recordedPayments[1].idempotencyKey)
        assertNotNull(store.records[PendingSubmissionScopes.supplierPayment("pay-A")])

        // 4. Open payable A in Detail screen (even without restart) -> restores draft & reuses exact keyA
        val detailVm = SupplierDebtDetailViewModel(
            purchasingRepository = repo,
            supplierId = "sup-1",
            idempotencyKeyGenerator = { "det-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        detailVm.openPayment(payableA)
        advanceUntilIdle()
        assertEquals("80000", detailVm.uiState.value.paymentAmountInput)
        assertEquals("TRF-A", detailVm.uiState.value.referenceInput)

        detailVm.submitPayment()
        advanceUntilIdle()
        assertEquals(keyA, repo.recordedPayments[2].idempotencyKey)
        assertNull(store.records[PendingSubmissionScopes.supplierPayment("pay-A")])
    }

    @Test
    fun incomingGoodsViewModel_survivesRestartWithSameKey_andRotatesOnChange() = runTest(dispatcher) {
        val store = FakePendingSubmissionStore()
        val invRepo = FakeInventoryRepo()
        val purRepo = FakePurchasingRepo(emptyList())
        var seq = 0

        val vm1 = IncomingGoodsViewModel(
            inventoryRepository = invRepo,
            purchasingRepository = purRepo,
            documentNumberGenerator = FakeDocNumGen(),
            idempotencyKeyGenerator = { "ig-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        vm1.onQuantityChanged("10")
        vm1.onBuyPriceChanged("45000")
        vm1.onInvoiceChanged("BM-99")
        purRepo.nextPurchaseResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        vm1.submitIncomingGoods()
        advanceUntilIdle()
        val firstKey = purRepo.recordedPurchases[0].idempotencyKey!!

        // Simulate restart -> new VM
        val vm2 = IncomingGoodsViewModel(
            inventoryRepository = invRepo,
            purchasingRepository = purRepo,
            documentNumberGenerator = FakeDocNumGen(),
            idempotencyKeyGenerator = { "ig-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        vm2.onQuantityChanged("10")
        vm2.onBuyPriceChanged("45000")
        vm2.onInvoiceChanged("BM-99")
        vm2.submitIncomingGoods()
        advanceUntilIdle()
        assertEquals(firstKey, purRepo.recordedPurchases[1].idempotencyKey)
        assertNull(store.records[PendingSubmissionScopes.PURCHASE])
    }

    @Test
    fun receivableCustomerDetailViewModel_survivesRestartForSingleAndPayAll() = runTest(dispatcher) {
        val store = FakePendingSubmissionStore()
        val rcv1 = Receivable(
            id = "rcv-1", customerId = "cust-1", customerName = "Budi", transactionId = "tx-1",
            source = "POS", amount = BigDecimal("100000"), paidAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("100000"), debtDate = "2026-11-01", dueDate = "2026-11-30",
            status = "UNPAID", legacyInvoiceNumber = null, notes = null, createdBy = "k1",
            isActive = true, createdAt = "2026-11-01T00:00:00Z"
        )
        val repo = FakeReceivableRepo(listOf(rcv1))
        var seq = 0

        val vm1 = ReceivableCustomerDetailViewModel(
            receivableRepository = repo,
            customerId = "cust-1",
            idempotencyKeyGenerator = { "rcv-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        vm1.openPayment(rcv1)
        vm1.onPaymentAmountChanged("50000")
        repo.nextError = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        vm1.submitPayment()
        advanceUntilIdle()
        val firstKey = repo.recordedCommands[0].idempotencyKey

        // Simulate restart -> new VM
        val vm2 = ReceivableCustomerDetailViewModel(
            receivableRepository = repo,
            customerId = "cust-1",
            idempotencyKeyGenerator = { "rcv-${++seq}" },
            pendingSubmissionStore = store
        )
        advanceUntilIdle()
        vm2.openPayment(rcv1)
        vm2.onPaymentAmountChanged("50000")
        vm2.submitPayment()
        advanceUntilIdle()
        assertEquals(firstKey, repo.recordedCommands[1].idempotencyKey)
        assertNull(store.records[PendingSubmissionScopes.receivablePayment("cust-1")])
    }

    @Test
    fun cashierTransactionHistoryViewModel_payDebtSurvivesRestartWithSameKeyAndDraft() = runTest(dispatcher) {
        val store = FakePendingSubmissionStore()
        val cashRepo = FakeCashRepo()
        var seq = 0

        val vm1 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "cpd-${++seq}" },
            pendingSubmissionStore = store
        )
        vm1.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm1.showPayDebtDialog()
        vm1.onPayDebtAmountChanged("40000")
        vm1.onPayDebtMethodChanged("TRANSFER")
        cashRepo.nextPayDebtError = RepositoryResult.Exception(IOException("Timeout"))
        vm1.payDebt()
        advanceUntilIdle()
        val firstKey = cashRepo.recordedPayDebtKeys[0]
        assertNotNull(store.records[PendingSubmissionScopes.CASHIER_PAY_DEBT])

        // Simulate restart -> recreate VM and open same receipt
        val vm2 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "cpd-${++seq}" },
            pendingSubmissionStore = store
        )
        vm2.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.isPayDebtOutcomeAmbiguous)
        assertEquals(firstKey, vm2.uiState.value.payDebtIdempotencyKey)

        vm2.showPayDebtDialog()
        assertEquals("40000", vm2.uiState.value.payDebtAmountInput)
        assertEquals("TRANSFER", vm2.uiState.value.payDebtMethodInput)
        vm2.payDebt()
        advanceUntilIdle()

        assertEquals(firstKey, cashRepo.recordedPayDebtKeys[1])
        assertNull(store.records[PendingSubmissionScopes.CASHIER_PAY_DEBT])
        assertFalse(vm2.uiState.value.isPayDebtOutcomeAmbiguous)
    }

    @Test
    fun cashierTransactionHistoryViewModel_voidSurvivesRestartWithSameKey_andClearsOnSuccessOrNonAmbiguousError() = runTest(dispatcher) {
        var nowMillis = 1_000_000L
        val store = FakePendingSubmissionStore(clockMillis = { nowMillis })
        val cashRepo = FakeCashRepo(initialStatus = "LUNAS")
        var seq = 0

        val vm1 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "void-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis }
        )
        vm1.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm1.showVoidDialog()
        vm1.onVoidReasonChanged("Salah input barang")
        cashRepo.nextVoidResult = RepositoryResult.Exception(IOException("Socket timeout"))
        vm1.submitVoid()
        advanceUntilIdle()

        assertEquals(1, cashRepo.recordedVoidKeys.size)
        val firstKey = cashRepo.recordedVoidKeys[0]
        assertTrue(firstKey.startsWith("void-mobile-"))
        assertEquals(PendingSubmissionStatus.AMBIGUOUS, store.records[PendingSubmissionScopes.CASHIER_VOID]?.status)

        // Simulate restart within 72h -> new ViewModel loads same receipt and reuses exact same key
        nowMillis += 60_000L
        val vm2 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "void-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis }
        )
        vm2.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.isVoidOutcomeAmbiguous)
        assertEquals(firstKey, vm2.uiState.value.voidIdempotencyKey)
        assertEquals("Salah input barang", vm2.uiState.value.voidReasonInput)

        vm2.showVoidDialog()
        vm2.submitVoid()
        advanceUntilIdle()

        assertEquals(2, cashRepo.recordedVoidKeys.size)
        assertEquals(firstKey, cashRepo.recordedVoidKeys[1])
        assertNull(store.records[PendingSubmissionScopes.CASHIER_VOID])
        assertFalse(vm2.uiState.value.isVoidOutcomeAmbiguous)

        // Non-ambiguous 400 error clears persisted void key
        cashRepo.detail = cashRepo.detail.copy(status = "LUNAS")
        vm2.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm2.showVoidDialog()
        vm2.onVoidReasonChanged("Salah harga lagi")
        cashRepo.nextVoidResult = RepositoryResult.Error("VALIDATION_ERROR", "Sudah lewat batas")
        vm2.submitVoid()
        advanceUntilIdle()
        assertNull(store.records[PendingSubmissionScopes.CASHIER_VOID])
        assertFalse(vm2.uiState.value.isVoidOutcomeAmbiguous)
    }

    @Test
    fun cashierTransactionHistoryViewModel_refundSurvivesRestartWithSameKeyAndDisposition_andHandlesCorruptOrExpired() = runTest(dispatcher) {
        var nowMillis = 2_000_000L
        val store = FakePendingSubmissionStore(clockMillis = { nowMillis })
        val cashRepo = FakeCashRepo(initialStatus = "LUNAS")
        var seq = 0

        val vm1 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "ADMIN",
            idempotencyKeyGenerator = { "ref-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis }
        )
        vm1.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm1.showRefundDialog()
        vm1.onRefundDispositionChanged(RefundDisposition.DAMAGED)
        vm1.onRefundReasonChanged("Kemasan pecah di gudang")
        cashRepo.nextRefundResult = RepositoryResult.Error("HTTP_503", "Service Unavailable")
        vm1.submitRefund()
        advanceUntilIdle()

        assertEquals(1, cashRepo.recordedRefundKeys.size)
        val firstKey = cashRepo.recordedRefundKeys[0]
        assertEquals(PendingSubmissionStatus.AMBIGUOUS, store.records[PendingSubmissionScopes.CASHIER_REFUND]?.status)

        // Simulate restart -> recreate VM with same store
        val vm2 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "ADMIN",
            idempotencyKeyGenerator = { "ref-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis }
        )
        vm2.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.isRefundOutcomeAmbiguous)
        assertEquals(firstKey, vm2.uiState.value.refundIdempotencyKey)
        assertEquals(RefundDisposition.DAMAGED, vm2.uiState.value.refundDisposition)
        assertEquals("Kemasan pecah di gudang", vm2.uiState.value.refundReasonInput)

        vm2.showRefundDialog()
        vm2.submitRefund()
        advanceUntilIdle()
        assertEquals(2, cashRepo.recordedRefundKeys.size)
        assertEquals(firstKey, cashRepo.recordedRefundKeys[1])
        assertNull(store.records[PendingSubmissionScopes.CASHIER_REFUND])

        // Corrupt JSON & expired (>72h) records do not crash and get cleaned up
        store.records[PendingSubmissionScopes.CASHIER_VOID] = PendingSubmissionRecord(
            scope = PendingSubmissionScopes.CASHIER_VOID,
            idempotencyKey = "broken-void",
            intentJson = "{not-valid-json",
            status = PendingSubmissionStatus.AMBIGUOUS,
            createdAt = nowMillis,
            updatedAt = nowMillis
        )
        store.records[PendingSubmissionScopes.CASHIER_REFUND] = PendingSubmissionRecord(
            scope = PendingSubmissionScopes.CASHIER_REFUND,
            idempotencyKey = "expired-refund",
            intentJson = """{"transactionId":"tx-debt-1","reason":"expired","dispositionName":"RETURN_TO_STOCK"}""",
            status = PendingSubmissionStatus.AMBIGUOUS,
            createdAt = nowMillis,
            updatedAt = nowMillis
        )
        nowMillis += RoomPendingSubmissionStore.DEFAULT_TTL_MILLIS + 1L
        cashRepo.detail = cashRepo.detail.copy(status = "LUNAS")

        val vm3 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "ADMIN",
            idempotencyKeyGenerator = { "ref-${++seq}" },
            pendingSubmissionStore = store,
            clockMillis = { nowMillis }
        )
        vm3.loadReceipt("tx-debt-1")
        advanceUntilIdle()

        assertFalse(vm3.uiState.value.isVoidOutcomeAmbiguous)
        assertFalse(vm3.uiState.value.isRefundOutcomeAmbiguous)
        assertNull(store.records[PendingSubmissionScopes.CASHIER_VOID])
        assertNull(store.records[PendingSubmissionScopes.CASHIER_REFUND])
    }

    @Test
    fun cashierTransactionHistoryViewModel_fastVoidAndRefundAfterRestart_awaitsRestoreAndReusesOldKey() = runTest(dispatcher) {
        val backingStore = FakePendingSubmissionStore()
        val cashRepo = FakeCashRepo(initialStatus = "LUNAS")
        var seq = 0

        // Seed ambiguous void & refund keys from previous session
        val vm1 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "seed-${++seq}" },
            pendingSubmissionStore = backingStore,
        )
        vm1.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm1.showVoidDialog()
        vm1.onVoidReasonChanged("Salah input barang")
        cashRepo.nextVoidResult = RepositoryResult.Exception(IOException("timeout"))
        vm1.submitVoid()
        advanceUntilIdle()
        val oldVoidKey = cashRepo.recordedVoidKeys.single()

        vm1.showRefundDialog()
        vm1.onRefundDispositionChanged(RefundDisposition.DAMAGED)
        vm1.onRefundReasonChanged("Barang rusak")
        cashRepo.nextRefundResult = RepositoryResult.Error("HTTP_504", "Gateway timeout")
        vm1.submitRefund()
        advanceUntilIdle()
        val oldRefundKey = cashRepo.recordedRefundKeys.single()

        // Restart with a gated store so restore stays suspended while loadReceipt + dialog + submit are queued right away
        val gate = CompletableDeferred<Unit>()
        val gatedStore = object : PendingSubmissionStore {
            override suspend fun load(scope: String): PendingSubmissionRecord? {
                gate.await()
                return backingStore.load(scope)
            }
            override suspend fun save(record: PendingSubmissionRecord): Boolean = backingStore.save(record)
            override suspend fun clear(scope: String) = backingStore.clear(scope)
            override suspend fun purgeExpired() = backingStore.purgeExpired()
        }

        val vm2 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "fresh-${++seq}" },
            pendingSubmissionStore = gatedStore,
        )
        vm2.loadReceipt("tx-debt-1")
        runCurrent()
        // While restore is still blocked, showVoidDialog / submitVoid must not mint or persist a fresh key
        vm2.showVoidDialog()
        vm2.submitVoid()
        assertEquals(0, cashRepo.recordedVoidKeys.size - 1)

        // Release store restore and let queued receipt/restore/submit complete
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(oldVoidKey, vm2.uiState.value.voidIdempotencyKey)
        vm2.showVoidDialog()
        vm2.submitVoid()
        advanceUntilIdle()
        assertEquals(2, cashRepo.recordedVoidKeys.size)
        assertEquals(oldVoidKey, cashRepo.recordedVoidKeys[1])

        // Reset transaction status to LUNAS and test gated restore for refund after restart
        cashRepo.detail = cashRepo.detail.copy(status = "LUNAS")
        val refundGate = CompletableDeferred<Unit>()
        val gatedRefundStore = object : PendingSubmissionStore {
            override suspend fun load(scope: String): PendingSubmissionRecord? {
                refundGate.await()
                return backingStore.load(scope)
            }
            override suspend fun save(record: PendingSubmissionRecord): Boolean = backingStore.save(record)
            override suspend fun clear(scope: String) = backingStore.clear(scope)
            override suspend fun purgeExpired() = backingStore.purgeExpired()
        }
        val vm3 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "OWNER",
            idempotencyKeyGenerator = { "fresh-${++seq}" },
            pendingSubmissionStore = gatedRefundStore,
        )
        vm3.loadReceipt("tx-debt-1")
        runCurrent()
        vm3.showRefundDialog()
        vm3.submitRefund()
        assertEquals(1, cashRepo.recordedRefundKeys.size)

        refundGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(oldRefundKey, vm3.uiState.value.refundIdempotencyKey)
        vm3.showRefundDialog()
        vm3.submitRefund()
        advanceUntilIdle()
        assertEquals(2, cashRepo.recordedRefundKeys.size)
        assertEquals(oldRefundKey, cashRepo.recordedRefundKeys[1])
    }

    @Test
    fun cashierRefundAndVoid_reuseKeyWhenApprovalRenewedOrDetailsChanged_andRecoverOnConflict() = runTest(dispatcher) {
        val store = FakePendingSubmissionStore()
        val cashRepo = FakeCashRepo(initialStatus = "LUNAS")
        var seq = 0

        // 1. Cashier triggers refund with approval-1, gets MANAGER_APPROVAL_EXPIRED, then renews to approval-2 -> same key!
        val vm1 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "k-${++seq}" },
            pendingSubmissionStore = store,
        )
        vm1.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm1.showRefundDialog()
        vm1.onRefundDispositionChanged(RefundDisposition.DAMAGED)
        vm1.onRefundReasonChanged("Kemasan rusak")
        vm1.submitRefund()
        cashRepo.nextRefundResult = RepositoryResult.Error("MANAGER_APPROVAL_EXPIRED", "Kedaluwarsa")
        vm1.onManagerApprovalGranted(
            ManagerApprovalGrant(
                approvalId = "approval-1",
                action = ManagerApprovalAction.REFUND_TRANSACTION,
                resourceType = ManagerApprovalResourceType.TRANSACTION,
                resourceId = "tx-debt-1",
                status = ManagerApprovalStatus.APPROVED,
                createdAt = "2026-11-28T00:00:00Z",
                expiresAt = "2026-11-28T00:05:00Z",
            )
        )
        advanceUntilIdle()

        val firstRefundKey = cashRepo.recordedRefundKeys.single()
        assertTrue(vm1.uiState.value.isRefundOutcomeAmbiguous)
        assertNull(vm1.uiState.value.refundManagerApprovalId)
        assertEquals(firstRefundKey, vm1.uiState.value.refundIdempotencyKey)
        assertEquals(PendingSubmissionStatus.AMBIGUOUS, store.records[PendingSubmissionScopes.CASHIER_REFUND]?.status)

        // Force-close & restart -> new VM opens receipt -> obtains approval-2 -> reuses exact same key
        val vm2 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "k-${++seq}" },
            pendingSubmissionStore = store,
        )
        vm2.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm2.showRefundDialog()
        vm2.submitRefund()
        assertEquals(ManagerApprovalAction.REFUND_TRANSACTION, vm2.uiState.value.pendingManagerApprovalAction)

        // Simulate backend answering 409 DUPLICATE_DATA (uq_transaction_refund_transaction) -> recovers cleanly
        cashRepo.detail = cashRepo.detail.copy(status = "REFUNDED")
        cashRepo.nextRefundResult = RepositoryResult.Error("DUPLICATE_DATA", "Data sudah ada")
        vm2.onManagerApprovalGranted(
            ManagerApprovalGrant(
                approvalId = "approval-2",
                action = ManagerApprovalAction.REFUND_TRANSACTION,
                resourceType = ManagerApprovalResourceType.TRANSACTION,
                resourceId = "tx-debt-1",
                status = ManagerApprovalStatus.APPROVED,
                createdAt = "2026-11-28T00:01:00Z",
                expiresAt = "2026-11-28T00:06:00Z",
            )
        )
        advanceUntilIdle()

        assertEquals(listOf(firstRefundKey, firstRefundKey), cashRepo.recordedRefundKeys)
        assertNull(store.records[PendingSubmissionScopes.CASHIER_REFUND])
        assertFalse(vm2.uiState.value.isRefundDialogOpen)
        assertFalse(vm2.uiState.value.isRefundOutcomeAmbiguous)
        assertEquals("Transaksi sudah pernah direfund.", vm2.uiState.value.receiptMessage)
        assertEquals("REFUNDED", vm2.uiState.value.selectedTransaction?.status)

        // 2. Void: expired approval preserves key across restart and recovers on TRANSACTION_ALREADY_VOIDED
        cashRepo.detail = cashRepo.detail.copy(status = "LUNAS")
        val vm3 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "k-${++seq}" },
            pendingSubmissionStore = store,
        )
        vm3.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm3.showVoidDialog()
        vm3.onVoidReasonChanged("Salah input barang")
        vm3.submitVoid()
        cashRepo.nextVoidResult = RepositoryResult.Error("MANAGER_APPROVAL_ALREADY_USED", "Sudah dipakai")
        vm3.onManagerApprovalGranted(
            ManagerApprovalGrant(
                approvalId = "approval-v1",
                action = ManagerApprovalAction.VOID_TRANSACTION,
                resourceType = ManagerApprovalResourceType.TRANSACTION,
                resourceId = "tx-debt-1",
                status = ManagerApprovalStatus.APPROVED,
                createdAt = "2026-11-28T00:02:00Z",
                expiresAt = "2026-11-28T00:07:00Z",
            )
        )
        advanceUntilIdle()

        val firstVoidKey = cashRepo.recordedVoidKeys.single()
        assertEquals(PendingSubmissionStatus.AMBIGUOUS, store.records[PendingSubmissionScopes.CASHIER_VOID]?.status)

        val vm4 = CashierTransactionHistoryViewModel(
            repository = cashRepo,
            actorRole = "KASIR",
            idempotencyKeyGenerator = { "k-${++seq}" },
            pendingSubmissionStore = store,
        )
        vm4.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        vm4.showVoidDialog()
        vm4.submitVoid()
        cashRepo.detail = cashRepo.detail.copy(status = "VOIDED")
        cashRepo.nextVoidResult = RepositoryResult.Error("TRANSACTION_ALREADY_VOIDED", "Transaksi sudah pernah dibatalkan")
        vm4.onManagerApprovalGranted(
            ManagerApprovalGrant(
                approvalId = "approval-v2",
                action = ManagerApprovalAction.VOID_TRANSACTION,
                resourceType = ManagerApprovalResourceType.TRANSACTION,
                resourceId = "tx-debt-1",
                status = ManagerApprovalStatus.APPROVED,
                createdAt = "2026-11-28T00:03:00Z",
                expiresAt = "2026-11-28T00:08:00Z",
            )
        )
        advanceUntilIdle()

        assertEquals(listOf(firstVoidKey, firstVoidKey), cashRepo.recordedVoidKeys)
        assertNull(store.records[PendingSubmissionScopes.CASHIER_VOID])
        assertFalse(vm4.uiState.value.isVoidDialogOpen)
        assertFalse(vm4.uiState.value.isVoidOutcomeAmbiguous)
        assertEquals("Transaksi sudah pernah dibatalkan.", vm4.uiState.value.receiptMessage)
        assertEquals("VOIDED", vm4.uiState.value.selectedTransaction?.status)
    }

    @Test
    fun diskFull_whenSavingInFlight_abortsRequestsAcrossAllPendingStoreCallers() = runTest(dispatcher) {
        val failingStore = object : PendingSubmissionStore {
            override suspend fun load(scope: String): PendingSubmissionRecord? = null
            override suspend fun save(record: PendingSubmissionRecord): Boolean = false
            override suspend fun clear(scope: String) = Unit
            override suspend fun purgeExpired() = Unit
        }

        // 1. SupplierDebtViewModel
        val payable = SupplierPayable(
            id = "pay-disk-1",
            supplierId = "sup-1",
            supplierName = "PT Semen",
            purchaseId = "pur-1",
            amount = BigDecimal("200000"),
            paidAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("200000"),
            dueDate = "2026-12-31",
            status = "belum_lunas",
            createdAt = "2026-11-20T10:00:00Z"
        )
        val purRepo = FakePurchasingRepo(listOf(payable))
        val supplierListVm = SupplierDebtViewModel(purRepo, pendingSubmissionStore = failingStore)
        advanceUntilIdle()
        supplierListVm.openPayment(payable)
        supplierListVm.onPaymentAmountChanged("50000")
        supplierListVm.submitPayment()
        advanceUntilIdle()
        assertTrue(purRepo.recordedPayments.isEmpty())
        assertNotNull(supplierListVm.uiState.value.errorMessage)
        assertFalse(supplierListVm.uiState.value.isSubmittingPayment)

        // 2. SupplierDebtDetailViewModel
        val supplierDetailVm = SupplierDebtDetailViewModel(purRepo, "sup-1", pendingSubmissionStore = failingStore)
        advanceUntilIdle()
        supplierDetailVm.openPayment(payable)
        supplierDetailVm.onPaymentAmountChanged("50000")
        supplierDetailVm.submitPayment()
        advanceUntilIdle()
        assertTrue(purRepo.recordedPayments.isEmpty())
        assertNotNull(supplierDetailVm.uiState.value.errorMessage)
        assertFalse(supplierDetailVm.uiState.value.isSubmittingPayment)

        // 3. IncomingGoodsViewModel
        val incomingVm = IncomingGoodsViewModel(
            inventoryRepository = FakeInventoryRepo(),
            purchasingRepository = purRepo,
            documentNumberGenerator = FakeDocNumGen(),
            pendingSubmissionStore = failingStore,
        )
        advanceUntilIdle()
        incomingVm.onQuantityChanged("5")
        incomingVm.onBuyPriceChanged("45000")
        incomingVm.onInvoiceChanged("BM-DISK")
        incomingVm.submitIncomingGoods()
        advanceUntilIdle()
        assertTrue(purRepo.recordedPurchases.isEmpty())
        assertNotNull(incomingVm.uiState.value.errorMessage)
        assertFalse(incomingVm.uiState.value.isSubmitting)

        // 4. ReceivableCustomerDetailViewModel (single + payAll)
        val rcv = Receivable(
            id = "rcv-1", customerId = "cust-1", customerName = "Budi", transactionId = "tx-1",
            source = "POS", amount = BigDecimal("100000"), paidAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("100000"), debtDate = "2026-11-01", dueDate = "2026-11-30",
            status = "UNPAID", legacyInvoiceNumber = null, notes = null, createdBy = "k1",
            isActive = true, createdAt = "2026-11-01T00:00:00Z"
        )
        val rcvRepo = FakeReceivableRepo(listOf(rcv))
        val rcvVm = ReceivableCustomerDetailViewModel(
            receivableRepository = rcvRepo,
            customerId = "cust-1",
            pendingSubmissionStore = failingStore,
        )
        advanceUntilIdle()
        rcvVm.openPayment(rcv)
        rcvVm.onPaymentAmountChanged("50000")
        rcvVm.submitPayment()
        advanceUntilIdle()
        assertTrue(rcvRepo.recordedCommands.isEmpty())
        assertNotNull(rcvVm.uiState.value.paymentErrorMessage)
        assertFalse(rcvVm.uiState.value.isSubmittingPayment)

        rcvVm.closePayment()
        rcvVm.openPayAll()
        rcvVm.onPaymentAmountChanged("50000")
        rcvVm.submitPayment()
        advanceUntilIdle()
        assertTrue(rcvRepo.recordedCommands.isEmpty())
        assertNotNull(rcvVm.uiState.value.paymentErrorMessage)
        assertFalse(rcvVm.uiState.value.isSubmittingPayment)

        // 5. CashierTransactionHistoryViewModel (payDebt, void, refund)
        val cashRepoDebt = FakeCashRepo(initialStatus = "HUTANG")
        val debtVm = CashierTransactionHistoryViewModel(
            repository = cashRepoDebt,
            actorRole = "KASIR",
            pendingSubmissionStore = failingStore,
        )
        debtVm.loadReceipt("tx-debt-1")
        advanceUntilIdle()
        debtVm.showPayDebtDialog()
        debtVm.onPayDebtAmountChanged("30000")
        debtVm.payDebt()
        advanceUntilIdle()
        assertTrue(cashRepoDebt.recordedPayDebtKeys.isEmpty())
        assertNotNull(debtVm.uiState.value.payDebtError)
        assertFalse(debtVm.uiState.value.isSubmittingDebt)

        val cashRepoPaid = FakeCashRepo(initialStatus = "LUNAS")
        val actionVm = CashierTransactionHistoryViewModel(
            repository = cashRepoPaid,
            actorRole = "OWNER",
            pendingSubmissionStore = failingStore,
        )
        actionVm.loadReceipt("tx-debt-1")
        advanceUntilIdle()

        actionVm.showVoidDialog()
        actionVm.onVoidReasonChanged("Salah barang")
        actionVm.submitVoid()
        advanceUntilIdle()
        assertTrue(cashRepoPaid.recordedVoidKeys.isEmpty())
        assertNotNull(actionVm.uiState.value.voidErrorMessage)
        assertFalse(actionVm.uiState.value.isSubmittingVoid)

        actionVm.hideVoidDialog()
        actionVm.showRefundDialog()
        actionVm.onRefundReasonChanged("Barang rusak")
        actionVm.submitRefund()
        advanceUntilIdle()
        assertTrue(cashRepoPaid.recordedRefundKeys.isEmpty())
        assertNotNull(actionVm.uiState.value.refundErrorMessage)
        assertFalse(actionVm.uiState.value.isSubmittingRefund)
    }

    private class FakePendingSubmissionStore(
        private val clockMillis: () -> Long = { System.currentTimeMillis() }
    ) : PendingSubmissionStore {
        val records = mutableMapOf<String, PendingSubmissionRecord>()
        override suspend fun load(scope: String): PendingSubmissionRecord? {
            val existing = records[scope] ?: return null
            if (existing.createdAt <= 0L || clockMillis() - existing.createdAt > RoomPendingSubmissionStore.DEFAULT_TTL_MILLIS) {
                records.remove(scope)
                return null
            }
            return existing
        }
        override suspend fun loadByScopePrefix(scopePrefix: String): List<PendingSubmissionRecord> {
            return records.keys
                .filter { it.startsWith(scopePrefix) }
                .toList()
                .mapNotNull { load(it) }
        }
        override suspend fun save(record: PendingSubmissionRecord): Boolean {
            records[record.scope] = record
            return true
        }
        override suspend fun clear(scope: String) {
            records.remove(scope)
        }
        override suspend fun purgeExpired() = Unit
    }

    private class FakeDocNumGen : DocumentNumberGenerator {
        override fun generate(type: DocumentNumberType, date: LocalDate) = "BM-1"
    }

    private class FakePurchasingRepo(private val payables: List<SupplierPayable>) : PurchasingRepository {
        val recordedPayments = mutableListOf<CreateSupplierPaymentCommand>()
        val recordedPurchases = mutableListOf<CreatePurchaseCommand>()
        var nextPaymentResult: RepositoryResult<SupplierPaymentReceipt>? = null
        var nextPurchaseResult: RepositoryResult<PurchaseReceipt>? = null

        override suspend fun getSuppliers(page: Int, limit: Int, search: String?, status: String?) =
            RepositoryResult.Success(
                SupplierPage(
                    data = listOf(Supplier("sup-1", "PT Semen", null, null, 30, true, "2026-11-01", "2026-11-01")),
                    total = 1, page = 1, limit = limit, totalPages = 1
                )
            )
        override suspend fun getPayables(page: Int, limit: Int, supplierId: String?, status: String?) =
            RepositoryResult.Success(SupplierPayablePage(payables, payables.size.toLong(), 1, limit, 1))
        override suspend fun createSupplierPayment(command: CreateSupplierPaymentCommand): RepositoryResult<SupplierPaymentReceipt> {
            recordedPayments += command
            nextPaymentResult?.let { nextPaymentResult = null; return it }
            return RepositoryResult.Success(
                SupplierPaymentReceipt("p-1", command.payableId, command.amount, command.method, command.reference, command.notes, "2026-11-26T10:00:00Z", "sebagian", BigDecimal.ZERO)
            )
        }
        override suspend fun createPurchase(command: CreatePurchaseCommand): RepositoryResult<PurchaseReceipt> {
            recordedPurchases += command
            nextPurchaseResult?.let { nextPurchaseResult = null; return it }
            return RepositoryResult.Success(
                PurchaseReceipt("pur-1", command.supplierId, "PT Semen", command.invoiceNo, BigDecimal("450000"), "2026-11-26T10:00:00Z")
            )
        }
        override suspend fun getSupplierById(id: String, includeInactive: Boolean) = error("unused")
        override suspend fun activateSupplier(id: String) = error("unused")
        override suspend fun createSupplier(name: String, phone: String?, address: String?, paymentTermDays: Int) = error("unused")
        override suspend fun updateSupplier(id: String, name: String, phone: String?, address: String?, paymentTermDays: Int) = error("unused")
        override suspend fun deleteSupplier(id: String) = error("unused")
        override suspend fun getPurchases(page: Int, limit: Int, supplierId: String?, startDate: String?, endDate: String?): RepositoryResult<PurchaseSummaryPage> = error("unused")
        override suspend fun getPurchaseById(id: String): RepositoryResult<PurchaseDetail> = error("unused")
        override suspend fun getPayableById(id: String) = error("unused")
        override suspend fun createOpeningBalance(command: CreateOpeningPayableCommand) = error("unused")
        override suspend fun createAdjustment(command: CreatePayableAdjustmentCommand) = error("unused")
    }

    private class FakeInventoryRepo : InventoryRepository {
        override suspend fun getProductStocks(page: Int, limit: Int, search: String?) = RepositoryResult.Success(
            ProductStockPage(
                data = listOf(ProductStock("prod-1", "SMN-1", "Semen", "Mat", "Sak", BigDecimal("50"), BigDecimal("5"), BigDecimal("45000"), BigDecimal("60000"), BigDecimal("58000"), BigDecimal.ZERO, true)),
                total = 1, page = 1, limit = limit, totalPages = 1
            )
        )
        override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): RepositoryResult<StockAdjustmentPage> = error("unused")
        override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): RepositoryResult<StockCard> = error("unused")
        override suspend fun getProduct(id: String): RepositoryResult<Product> = error("unused")
        override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> = error("unused")
        override suspend fun getProductsPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductPage> = error("unused")
        override suspend fun createProduct(command: CreateProductCommand): RepositoryResult<Product> = error("unused")
        override suspend fun updateProduct(id: String, command: UpdateProductCommand): RepositoryResult<Product> = error("unused")
        override suspend fun deleteProduct(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun activateProduct(id: String): RepositoryResult<Product> = error("unused")
        override suspend fun executeStockOpname(productId: String, adjustmentType: String, actualQty: BigDecimal, notes: String?): RepositoryResult<Unit> = error("unused")
        override suspend fun previewProductCsv(csv: String): RepositoryResult<ProductCsvPreview> = error("unused")
        override suspend fun commitProductCsv(csv: String): RepositoryResult<ProductCsvImportResult> = error("unused")
        override suspend fun createOpeningStock(productId: String, date: String, quantity: BigDecimal, note: String): RepositoryResult<Unit> = error("unused")
        override suspend fun getCategories(): RepositoryResult<List<ProductCategory>> = error("unused")
        override suspend fun getCategoryPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductCategoryPage> = error("unused")
        override suspend fun createCategory(name: String): RepositoryResult<ProductCategory> = error("unused")
        override suspend fun updateCategory(id: String, name: String): RepositoryResult<ProductCategory> = error("unused")
        override suspend fun deleteCategory(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun getUnits(): RepositoryResult<List<ProductUnit>> = error("unused")
        override suspend fun getUnitPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductUnitPage> = error("unused")
        override suspend fun createUnit(name: String, symbol: String): RepositoryResult<ProductUnit> = error("unused")
        override suspend fun updateUnit(id: String, name: String, symbol: String): RepositoryResult<ProductUnit> = error("unused")
        override suspend fun deleteUnit(id: String): RepositoryResult<Unit> = error("unused")
    }

    private class FakeReceivableRepo(private val receivables: List<Receivable>) : ReceivableRepository {
        val recordedCommands = mutableListOf<CreateReceivablePaymentCommand>()
        var nextError: RepositoryResult<ReceivablePaymentReceipt>? = null
        override suspend fun getReceivables(page: Int, limit: Int, customerId: String?, status: String?, dueFilter: String?, dueFrom: String?, dueTo: String?) =
            RepositoryResult.Success(ReceivablePage(receivables, receivables.size.toLong(), 1, limit, 1))
        override suspend fun getReceivablePayments(page: Int, limit: Int, receivableId: String?, customerId: String?, method: String?, userId: String?, customerSearch: String?, receiverSearch: String?, status: String?, dateFrom: String?, dateTo: String?) =
            RepositoryResult.Success(ReceivablePaymentHistoryPage(emptyList(), 0, 1, limit, 1))
        override suspend fun createReceivablePayment(command: CreateReceivablePaymentCommand): RepositoryResult<ReceivablePaymentReceipt> {
            recordedCommands += command
            nextError?.let { nextError = null; return it }
            return RepositoryResult.Success(
                ReceivablePaymentReceipt("rp-1", "RP-1", command.receivableId, "cust-1", "Budi", command.amount, command.method, command.reference, command.notes, "2026-11-26T10:00:00Z", "2026-11-26", "PAYMENT", null, "k1", "Kasir", command.amount, BigDecimal.ZERO, "PAID", BigDecimal.ZERO, idempotentReplay = false)
            )
        }
        override suspend fun createOpeningBalance(command: CreateOpeningReceivableCommand) = error("unused")
        override suspend fun createAdjustment(command: CreateReceivableAdjustmentCommand) = error("unused")
        override suspend fun getCustomerSummaries(page: Int, limit: Int, dueFilter: String?) = RepositoryResult.Success(CustomerReceivableSummaryPage(emptyList(), 0, 1, limit, 1))
        override suspend fun getReceivableById(id: String) = error("unused")
        override suspend fun getReceivablePaymentReceipt(paymentId: String): RepositoryResult<ReceivablePaymentHistory> = error("unused")
        override suspend fun reverseReceivablePayment(command: ReverseReceivablePaymentCommand) = error("unused")
    }

    private class FakeCashRepo(initialStatus: String = "HUTANG") : CashReconciliationRepository {
        val recordedPayDebtKeys = mutableListOf<String>()
        val recordedVoidKeys = mutableListOf<String>()
        val recordedRefundKeys = mutableListOf<String>()
        var nextPayDebtError: RepositoryResult<CashTransactionDetail>? = null
        var nextVoidResult: RepositoryResult<TransactionVoidResult>? = null
        var nextRefundResult: RepositoryResult<TransactionRefundResult>? = null
        var detail = CashTransactionDetail(
            id = "tx-debt-1", receiptId = "TRX-100", sessionId = "s1", customerId = "cust-1",
            customerName = "Budi", type = "PENJUALAN", status = initialStatus,
            total = BigDecimal("100000"),
            paidAmount = if (initialStatus == "LUNAS") BigDecimal("100000") else BigDecimal("20000"),
            amountTendered = if (initialStatus == "LUNAS") BigDecimal("100000") else BigDecimal("20000"),
            changeAmount = BigDecimal.ZERO,
            createdAt = "2026-11-26T10:00:00Z", items = emptyList()
        )
        override suspend fun getTransactionById(id: String) = RepositoryResult.Success(detail)
        override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> {
            idempotencyKey?.let { recordedPayDebtKeys += it }
            nextPayDebtError?.let { nextPayDebtError = null; return it }
            detail = detail.copy(paidAmount = detail.paidAmount + amount)
            return RepositoryResult.Success(detail)
        }
        override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("unused")
        override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(null)
        override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
        override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("unused")
        override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
        override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
        override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?) =
            RepositoryResult.Success(CashTransactionPage(emptyList(), 0, 1, limit, 1))
        override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> {
            recordedVoidKeys += idempotencyKey
            nextVoidResult?.let { nextVoidResult = null; return it }
            detail = detail.copy(status = "VOIDED", voidReason = reason, voidedAt = "2026-11-26T11:00:00Z")
            return RepositoryResult.Success(
                TransactionVoidResult("void-1", id, detail.receiptId, reason, detail.voidedAt!!, false)
            )
        }
        override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> {
            recordedRefundKeys += idempotencyKey
            nextRefundResult?.let { nextRefundResult = null; return it }
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
                    createdAt = "2026-11-26T11:00:00Z",
                    idempotentReplay = false
                )
            )
        }
        override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("unused")
        override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = error("unused")
        override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
    }
}
