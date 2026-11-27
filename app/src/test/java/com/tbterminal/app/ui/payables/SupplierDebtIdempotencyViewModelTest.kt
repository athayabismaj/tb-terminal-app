package com.tbterminal.app.ui.payables

import com.tbterminal.app.data.model.CreateOpeningPayableCommand
import com.tbterminal.app.data.model.CreatePayableAdjustmentCommand
import com.tbterminal.app.data.model.CreatePurchaseCommand
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseReceipt
import com.tbterminal.app.data.model.PurchaseSummaryPage
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPage
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.model.SupplierPayablePage
import com.tbterminal.app.data.model.SupplierPaymentReceipt
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.io.IOException
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupplierDebtIdempotencyViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun supplierDebtVm_reusesKeyAcrossTimeoutCloseReopen_andRotatesOnChangeOrSuccess() = runTest(dispatcher) {
        val payable1 = payable("pay-1", remaining = BigDecimal("200000"))
        val payable2 = payable("pay-2", remaining = BigDecimal("150000"))
        val repo = FakePurchasingPaymentRepository(payables = listOf(payable1, payable2))
        var seq = 0
        val vm = SupplierDebtViewModel(
            purchasingRepository = repo,
            idempotencyKeyGenerator = { "sp-${++seq}" }
        )
        advanceUntilIdle()

        // 1. Fail with NETWORK_TIMEOUT
        vm.openPayment(payable1)
        vm.onPaymentAmountChanged("100000")
        vm.onPaymentMethodChanged(SupplierPaymentMethod.Transfer)
        vm.onReferenceChanged("TRF-01")
        vm.onNotesChanged("Cicilan pertama")
        repo.nextPaymentResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(1, repo.recordedPayments.size)
        val firstKey = repo.recordedPayments[0].idempotencyKey
        assertNotNull(firstKey)
        assertTrue(firstKey!!.startsWith("supplier-pay-sp-"))

        // 2. Close dialog & reopen same payable before definitive answer -> preserve draft & reuse key
        vm.closePayment()
        vm.openPayment(payable1)
        assertEquals("100000", vm.uiState.value.paymentAmountInput)
        assertEquals(SupplierPaymentMethod.Transfer, vm.uiState.value.paymentMethod)
        assertEquals("TRF-01", vm.uiState.value.referenceInput)
        assertEquals("Cicilan pertama", vm.uiState.value.notesInput)

        repo.nextPaymentResult = RepositoryResult.Exception(IOException("Socket reset"))
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(2, repo.recordedPayments.size)
        assertEquals(firstKey, repo.recordedPayments[1].idempotencyKey)

        // 3. Retry after HTTP_503 with same payload -> still reuses exact key
        repo.nextPaymentResult = RepositoryResult.Error("HTTP_503", "Service Unavailable")
        vm.submitPayment()
        advanceUntilIdle()
        assertEquals(3, repo.recordedPayments.size)
        assertEquals(firstKey, repo.recordedPayments[2].idempotencyKey)

        // 4. Change reference -> must rotate to new key
        vm.onReferenceChanged("TRF-02")
        repo.nextPaymentResult = RepositoryResult.Error("HTTP_503", "Service Unavailable")
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(4, repo.recordedPayments.size)
        val rotatedByRefKey = repo.recordedPayments[3].idempotencyKey
        assertNotEquals(firstKey, rotatedByRefKey)

        // 5. Switch to a different payable -> resets draft and uses new key
        vm.closePayment()
        vm.openPayment(payable2)
        assertEquals("150000", vm.uiState.value.paymentAmountInput)
        assertEquals(SupplierPaymentMethod.Cash, vm.uiState.value.paymentMethod)
        repo.nextPaymentResult = null // success
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(5, repo.recordedPayments.size)
        val payable2Key = repo.recordedPayments[4].idempotencyKey
        assertNotEquals(rotatedByRefKey, payable2Key)
        assertNull(vm.uiState.value.selectedPayable)
    }

    @Test
    fun supplierDebtVm_treatsIdempotentReplayAsNormalSuccess_andHandles400ConflictWithoutAutoRetry() = runTest(dispatcher) {
        val payable1 = payable("pay-1", remaining = BigDecimal("200000"))
        val repo = FakePurchasingPaymentRepository(payables = listOf(payable1))
        var seq = 0
        val vm = SupplierDebtViewModel(
            purchasingRepository = repo,
            idempotencyKeyGenerator = { "replay-${++seq}" }
        )
        advanceUntilIdle()

        // First call returns idempotentReplay = true -> normal single success & closes dialog
        vm.openPayment(payable1)
        vm.onPaymentAmountChanged("50000")
        repo.nextPaymentResult = RepositoryResult.Success(
            paymentReceipt("pmt-1", payable1.id, BigDecimal("50000"), idempotentReplay = true)
        )
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(1, repo.recordedPayments.size)
        val replayKey = repo.recordedPayments[0].idempotencyKey
        assertNull(vm.uiState.value.selectedPayable)
        assertEquals("Pembayaran utang PT Semen Jaya berhasil dicatat.", vm.uiState.value.message)
        assertNull(vm.uiState.value.errorMessage)

        // Subsequent payment even with identical payload gets a fresh key
        vm.openPayment(payable1)
        vm.onPaymentAmountChanged("50000")
        repo.nextPaymentResult = RepositoryResult.Error(
            code = "VALIDATION_ERROR",
            message = "Idempotency key sudah digunakan untuk request pembayaran yang berbeda"
        )
        vm.submitPayment()
        advanceUntilIdle()

        // Must NOT auto-retry on 400 conflict (only 1 additional call)
        assertEquals(2, repo.recordedPayments.size)
        assertNotEquals(replayKey, repo.recordedPayments[1].idempotencyKey)
        assertEquals(
            "Idempotency key sudah digunakan untuk request pembayaran yang berbeda",
            vm.uiState.value.errorMessage
        )
        assertFalse(vm.uiState.value.isSubmittingPayment)

        // Next submission after conflict gets another fresh key (conflict cleared active key)
        repo.nextPaymentResult = null
        vm.submitPayment()
        advanceUntilIdle()
        assertEquals(3, repo.recordedPayments.size)
        assertNotEquals(repo.recordedPayments[1].idempotencyKey, repo.recordedPayments[2].idempotencyKey)
    }

    @Test
    fun supplierDebtDetailVm_reusesKeyAcrossRetryAndCloseReopen_andHandlesReplayAndConflict() = runTest(dispatcher) {
        val payable1 = payable("pay-detail-1", remaining = BigDecimal("300000"))
        val payable2 = payable("pay-detail-2", remaining = BigDecimal("100000"))
        val repo = FakePurchasingPaymentRepository(payables = listOf(payable1, payable2))
        var seq = 0
        val vm = SupplierDebtDetailViewModel(
            purchasingRepository = repo,
            supplierId = "sup-1",
            idempotencyKeyGenerator = { "det-${++seq}" }
        )
        advanceUntilIdle()

        vm.openPayment(payable1)
        vm.onPaymentAmountChanged("125000")
        vm.onNotesChanged("Pelunasan sebagian")
        repo.nextPaymentResult = RepositoryResult.Exception(IOException("Timeout"))
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(1, repo.recordedPayments.size)
        val firstKey = repo.recordedPayments[0].idempotencyKey
        assertTrue(firstKey!!.startsWith("supplier-pay-det-"))

        // Close and reopen before definitive answer -> preserves inputs & reuses exact key
        vm.closePayment()
        vm.openPayment(payable1)
        assertEquals("125000", vm.uiState.value.paymentAmountInput)
        assertEquals("Pelunasan sebagian", vm.uiState.value.notesInput)

        repo.nextPaymentResult = RepositoryResult.Success(
            paymentReceipt("pmt-det-1", payable1.id, BigDecimal("125000"), idempotentReplay = true)
        )
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(2, repo.recordedPayments.size)
        assertEquals(firstKey, repo.recordedPayments[1].idempotencyKey)
        assertNull(vm.uiState.value.selectedPayable)
        assertEquals("Pembayaran hutang PT Semen Jaya berhasil dicatat.", vm.uiState.value.message)
        assertNull(vm.uiState.value.errorMessage)

        // Change notes / payable on next submission -> new key
        vm.openPayment(payable2)
        vm.onPaymentAmountChanged("50000")
        repo.nextPaymentResult = RepositoryResult.Error(
            code = "HTTP_400",
            message = "Idempotency key sudah digunakan untuk request pembayaran yang berbeda"
        )
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(3, repo.recordedPayments.size)
        val conflictKey = repo.recordedPayments[2].idempotencyKey
        assertNotEquals(firstKey, conflictKey)
        assertFalse(vm.uiState.value.isSubmittingPayment)
        assertEquals(
            "Idempotency key sudah digunakan untuk request pembayaran yang berbeda",
            vm.uiState.value.errorMessage
        )

        // Retrying after 400 conflict rotates to a new key instead of reusing conflicted key
        repo.nextPaymentResult = null
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(4, repo.recordedPayments.size)
        assertNotEquals(conflictKey, repo.recordedPayments[3].idempotencyKey)
    }

    @Test
    fun supplierDebtVm_openingAndClosingPayableB_preservesAmbiguousKeyAndDraftOfPayableA() = runTest(dispatcher) {
        val payableA = payable("pay-A", remaining = BigDecimal("200000"))
        val payableB = payable("pay-B", remaining = BigDecimal("150000"))
        val repo = FakePurchasingPaymentRepository(payables = listOf(payableA, payableB))
        var seq = 0
        val vm = SupplierDebtViewModel(
            purchasingRepository = repo,
            idempotencyKeyGenerator = { "multi-${++seq}" }
        )
        advanceUntilIdle()

        // 1. Timeout on payable A
        vm.openPayment(payableA)
        vm.onPaymentAmountChanged("90000")
        vm.onReferenceChanged("TRF-A")
        repo.nextPaymentResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        vm.submitPayment()
        advanceUntilIdle()
        val keyA = repo.recordedPayments[0].idempotencyKey

        // 2. Dismiss A, open B, dismiss B without paying
        vm.closePayment()
        vm.openPayment(payableB)
        assertEquals("150000", vm.uiState.value.paymentAmountInput)
        vm.closePayment()

        // 3. Reopen A -> draft & keyA are still preserved
        vm.openPayment(payableA)
        assertEquals("90000", vm.uiState.value.paymentAmountInput)
        assertEquals("TRF-A", vm.uiState.value.referenceInput)
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(2, repo.recordedPayments.size)
        assertEquals(keyA, repo.recordedPayments[1].idempotencyKey)
    }

    private fun payable(id: String, remaining: BigDecimal) = SupplierPayable(
        id = id,
        supplierId = "sup-1",
        supplierName = "PT Semen Jaya",
        purchaseId = "pur-$id",
        amount = remaining,
        paidAmount = BigDecimal.ZERO,
        remainingAmount = remaining,
        dueDate = "2026-12-31",
        status = "belum_lunas",
        createdAt = "2026-11-20T10:00:00Z"
    )

    private fun paymentReceipt(
        id: String,
        payableId: String,
        amount: BigDecimal,
        idempotentReplay: Boolean = false
    ) = SupplierPaymentReceipt(
        id = id,
        payableId = payableId,
        amount = amount,
        method = "tunai",
        reference = null,
        notes = null,
        paidAt = "2026-11-26T10:00:00Z",
        payableStatus = "sebagian",
        payableRemainingAmount = BigDecimal("100000"),
        idempotentReplay = idempotentReplay
    )

    private class FakePurchasingPaymentRepository(
        private val payables: List<SupplierPayable>
    ) : PurchasingRepository {
        val recordedPayments = mutableListOf<CreateSupplierPaymentCommand>()
        var nextPaymentResult: RepositoryResult<SupplierPaymentReceipt>? = null

        override suspend fun getSuppliers(
            page: Int,
            limit: Int,
            search: String?,
            status: String?
        ): RepositoryResult<SupplierPage> = RepositoryResult.Success(
            SupplierPage(
                data = listOf(
                    Supplier(
                        id = "sup-1",
                        name = "PT Semen Jaya",
                        phone = "08123456789",
                        address = "Jl. Raya",
                        paymentTermDays = 30,
                        isActive = true,
                        createdAt = "2026-11-01T00:00:00Z",
                        updatedAt = "2026-11-01T00:00:00Z"
                    )
                ),
                total = 1,
                page = 1,
                limit = limit,
                totalPages = 1
            )
        )

        override suspend fun getPayables(
            page: Int,
            limit: Int,
            supplierId: String?,
            status: String?
        ): RepositoryResult<SupplierPayablePage> = RepositoryResult.Success(
            SupplierPayablePage(
                data = payables,
                total = payables.size.toLong(),
                page = 1,
                limit = limit,
                totalPages = 1
            )
        )

        override suspend fun createSupplierPayment(
            command: CreateSupplierPaymentCommand
        ): RepositoryResult<SupplierPaymentReceipt> {
            recordedPayments += command
            val override = nextPaymentResult
            nextPaymentResult = null
            if (override != null) return override
            return RepositoryResult.Success(
                SupplierPaymentReceipt(
                    id = "pmt-${recordedPayments.size}",
                    payableId = command.payableId,
                    amount = command.amount,
                    method = command.method,
                    reference = command.reference,
                    notes = command.notes,
                    paidAt = "2026-11-26T10:00:00Z",
                    payableStatus = "lunas",
                    payableRemainingAmount = BigDecimal.ZERO,
                    idempotentReplay = false
                )
            )
        }

        override suspend fun getSupplierById(id: String, includeInactive: Boolean) = error("unused")
        override suspend fun activateSupplier(id: String) = error("unused")
        override suspend fun createSupplier(name: String, phone: String?, address: String?, paymentTermDays: Int) = error("unused")
        override suspend fun updateSupplier(id: String, name: String, phone: String?, address: String?, paymentTermDays: Int) = error("unused")
        override suspend fun deleteSupplier(id: String) = error("unused")
        override suspend fun createPurchase(command: CreatePurchaseCommand): RepositoryResult<PurchaseReceipt> = error("unused")
        override suspend fun getPurchases(page: Int, limit: Int, supplierId: String?, startDate: String?, endDate: String?): RepositoryResult<PurchaseSummaryPage> = error("unused")
        override suspend fun getPurchaseById(id: String): RepositoryResult<PurchaseDetail> = error("unused")
        override suspend fun getPayableById(id: String) = error("unused")
        override suspend fun createOpeningBalance(command: CreateOpeningPayableCommand) = error("unused")
        override suspend fun createAdjustment(command: CreatePayableAdjustmentCommand) = error("unused")
    }
}
