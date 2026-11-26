package com.tbterminal.app.ui.receivables

import com.tbterminal.app.data.model.CreateOpeningReceivableCommand
import com.tbterminal.app.data.model.CreateReceivableAdjustmentCommand
import com.tbterminal.app.data.model.CreateReceivablePaymentCommand
import com.tbterminal.app.data.model.CustomerReceivableSummaryPage
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.ReceivablePage
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.model.ReceivablePaymentHistoryPage
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import com.tbterminal.app.data.model.ReverseReceivablePaymentCommand
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceivableCustomerDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun singlePaymentReusesKeyOnTimeoutAndCloseReopenThenRotatesOnChangeOrSuccess() = runTest(dispatcher) {
        val repo = FakeReceivableDetailRepository(
            receivables = listOf(
                receivable("rcv-1", remaining = BigDecimal("100000")),
            )
        )
        var counter = 0
        val vm = ReceivableCustomerDetailViewModel(
            receivableRepository = repo,
            customerId = "cust-1",
            idempotencyKeyGenerator = { "key-${++counter}" },
        )
        advanceUntilIdle()

        vm.openPayment(repo.receivables.first())
        vm.onPaymentAmountChanged("50000")
        repo.nextError = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(1, repo.recordedCommands.size)
        val firstKey = repo.recordedCommands[0].idempotencyKey
        assertNotEquals("idempotency-check", firstKey)
        assertTrue(firstKey.startsWith("receivable-pay-all-key-"))

        // Close and reopen before definitive answer, keep same amount & method -> must reuse exact key
        vm.closePayment()
        vm.openPayment(repo.receivables.first())
        vm.onPaymentAmountChanged("50.000") // equivalent numeric amount 50000
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(2, repo.recordedCommands.size)
        assertEquals(firstKey, repo.recordedCommands[1].idempotencyKey)

        // Now change amount -> must rotate key
        repo.nextError = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        vm.onPaymentAmountChanged("60000")
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(3, repo.recordedCommands.size)
        val amountChangedKey = repo.recordedCommands[2].idempotencyKey
        assertNotEquals(firstKey, amountChangedKey)

        // Change method -> must rotate key
        vm.onPaymentMethodChanged(ReceivablePaymentMethod.Transfer)
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(4, repo.recordedCommands.size)
        val methodChangedKey = repo.recordedCommands[3].idempotencyKey
        assertNotEquals(amountChangedKey, methodChangedKey)

        // Subsequent payment after success -> must generate a fresh key even with same inputs
        vm.openPayment(repo.receivables.first().copy(remainingAmount = BigDecimal("40000")))
        vm.onPaymentAmountChanged("40000")
        vm.onPaymentMethodChanged(ReceivablePaymentMethod.Transfer)
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(5, repo.recordedCommands.size)
        assertNotEquals(methodChangedKey, repo.recordedCommands[4].idempotencyKey)
    }

    @Test
    fun payAllReusesPerInvoiceKeysOnRetryAndRotatesWhenSelectedInvoicesChange() = runTest(dispatcher) {
        val repo = FakeReceivableDetailRepository(
            receivables = listOf(
                receivable("rcv-1", remaining = BigDecimal("50000"), dueDate = "2026-11-01"),
                receivable("rcv-2", remaining = BigDecimal("75000"), dueDate = "2026-11-02"),
            )
        )
        var counter = 0
        val vm = ReceivableCustomerDetailViewModel(
            receivableRepository = repo,
            customerId = "cust-1",
            idempotencyKeyGenerator = { "batch-${++counter}" },
        )
        advanceUntilIdle()

        vm.openPayAll()
        repo.nextException = RuntimeException("Socket timeout")
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(1, repo.recordedCommands.size)
        val firstRcv1Key = repo.recordedCommands[0].idempotencyKey
        assertFalse(firstRcv1Key.contains("idempotency-check"))

        // Retry with same customer + amount + method + invoices -> rcv-1 gets exact same key, rcv-2 gets its deterministic key
        vm.submitPayment()
        advanceUntilIdle()
        assertEquals(3, repo.recordedCommands.size)
        assertEquals(firstRcv1Key, repo.recordedCommands[1].idempotencyKey)
        val firstRcv2Key = repo.recordedCommands[2].idempotencyKey
        assertNotEquals(firstRcv1Key, firstRcv2Key)

        // Open PayAll again and deselect rcv-2 -> invoice list changed -> new key
        repo.receivables = listOf(
            receivable("rcv-1", remaining = BigDecimal("50000"), dueDate = "2026-11-01"),
            receivable("rcv-2", remaining = BigDecimal("75000"), dueDate = "2026-11-02"),
        )
        vm.load()
        advanceUntilIdle()
        vm.openPayAll()
        vm.togglePayAllInvoice("rcv-2")
        vm.submitPayment()
        advanceUntilIdle()

        assertEquals(4, repo.recordedCommands.size)
        assertNotEquals(firstRcv1Key, repo.recordedCommands[3].idempotencyKey)
    }
}

private class FakeReceivableDetailRepository(
    var receivables: List<Receivable>,
) : ReceivableRepository {
    val recordedCommands = mutableListOf<CreateReceivablePaymentCommand>()
    var nextError: RepositoryResult.Error? = null
    var nextException: Throwable? = null

    override suspend fun getReceivables(
        page: Int,
        limit: Int,
        customerId: String?,
        status: String?,
        dueFilter: String?,
        dueFrom: String?,
        dueTo: String?,
    ): RepositoryResult<ReceivablePage> {
        return RepositoryResult.Success(
            ReceivablePage(
                data = receivables,
                total = receivables.size.toLong(),
                page = page,
                limit = limit,
                totalPages = 1,
            )
        )
    }

    override suspend fun getReceivablePayments(
        page: Int,
        limit: Int,
        receivableId: String?,
        customerId: String?,
        method: String?,
        userId: String?,
        customerSearch: String?,
        receiverSearch: String?,
        status: String?,
        dateFrom: String?,
        dateTo: String?,
    ): RepositoryResult<ReceivablePaymentHistoryPage> {
        return RepositoryResult.Success(ReceivablePaymentHistoryPage(emptyList(), 0, page, limit, 1))
    }

    override suspend fun createReceivablePayment(
        command: CreateReceivablePaymentCommand,
    ): RepositoryResult<ReceivablePaymentReceipt> {
        recordedCommands += command
        nextException?.let { err ->
            nextException = null
            return RepositoryResult.Exception(err)
        }
        nextError?.let { err ->
            nextError = null
            return err
        }
        return RepositoryResult.Success(
            ReceivablePaymentReceipt(
                id = "pay-${recordedCommands.size}",
                paymentNumber = "PAY-${recordedCommands.size}",
                receivableId = command.receivableId,
                customerId = "cust-1",
                customerName = "Pelanggan 1",
                amount = command.amount,
                method = command.method,
                reference = command.reference,
                notes = command.notes,
                paidAt = "2026-11-26T10:00:00Z",
                paymentDate = "2026-11-26",
                entryType = "PAYMENT",
                reversedPaymentId = null,
                receivedBy = "user-1",
                receivedByName = "Kasir",
                balanceBefore = command.amount,
                balanceAfter = BigDecimal.ZERO,
                receivableStatus = "PAID",
                receivableRemainingAmount = BigDecimal.ZERO,
                idempotentReplay = false,
            )
        )
    }

    override suspend fun createOpeningBalance(command: CreateOpeningReceivableCommand) = error("unused")
    override suspend fun createAdjustment(command: CreateReceivableAdjustmentCommand) = error("unused")
    override suspend fun getCustomerSummaries(page: Int, limit: Int, dueFilter: String?) =
        RepositoryResult.Success(CustomerReceivableSummaryPage(emptyList(), 0, page, limit, 1))
    override suspend fun getReceivableById(id: String) = error("unused")
    override suspend fun getReceivablePaymentReceipt(paymentId: String): RepositoryResult<ReceivablePaymentHistory> = error("unused")
    override suspend fun reverseReceivablePayment(command: ReverseReceivablePaymentCommand) = error("unused")
}

private fun receivable(
    id: String,
    remaining: BigDecimal,
    dueDate: String = "2026-11-30",
) = Receivable(
    id = id,
    customerId = "cust-1",
    customerName = "Pelanggan 1",
    transactionId = "tx-$id",
    source = "POS",
    amount = remaining,
    paidAmount = BigDecimal.ZERO,
    remainingAmount = remaining,
    debtDate = "2026-11-01",
    dueDate = dueDate,
    status = "UNPAID",
    legacyInvoiceNumber = null,
    notes = null,
    createdBy = "kasir-1",
    isActive = true,
    createdAt = "2026-11-01T10:00:00Z",
)
