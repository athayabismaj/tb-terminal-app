package com.tbterminal.app.ui.checkout

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tbterminal.app.data.auth.PinUnlockService
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CheckoutReceipt
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductCsvImportResult
import com.tbterminal.app.data.model.ProductCsvPreview
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductPage
import com.tbterminal.app.data.model.ProductStockPage
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.ProductUnitPage
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.StockCard
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.TransactionVoidResult
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.checkout.components.QuickPinLockOverlay
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CheckoutQuickPinLockTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun arbitraryPinDoesNotUnlockPosLock_andValidPinUnlocks() = runTest(testDispatcher) {
        val fakeService = FakeCheckoutPinUnlockService { pin ->
            if (pin == "654321") {
                RepositoryResult.Success(PinUnlockResult.Unlocked)
            } else {
                RepositoryResult.Success(PinUnlockResult.Failed(message = "PIN salah", attemptsLeft = 4))
            }
        }
        val vm = buildViewModel(fakeService)
        advanceUntilIdle()

        vm.lockScreen()
        assertTrue(vm.uiState.value.isScreenLocked)

        // Arbitrary PINs must keep POS locked
        vm.updateLockPinInput("1234")
        vm.unlockScreen()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isScreenLocked)
        assertEquals("", vm.uiState.value.lockPinInput)
        assertEquals("PIN salah. Sisa percobaan: 4.", vm.uiState.value.lockPinError)

        vm.updateLockPinInput("000000")
        vm.unlockScreen()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isScreenLocked)
        assertEquals(listOf("1234", "000000"), fakeService.submittedPins)

        // Correct PIN unlocks screen
        vm.updateLockPinInput("654321")
        vm.unlockScreen()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isScreenLocked)
        assertEquals("", vm.uiState.value.lockPinInput)
        assertNull(vm.uiState.value.lockPinError)
        assertFalse(vm.uiState.value.isUnlockingScreen)
    }

    @Test
    fun whileVerifying_isUnlockingScreenIsTrue_andPreventsDuplicateSubmissionsAndInput() = runTest(testDispatcher) {
        val gate = CompletableDeferred<RepositoryResult<PinUnlockResult>>()
        val fakeService = FakeCheckoutPinUnlockService { gate.await() }
        val vm = buildViewModel(fakeService)
        advanceUntilIdle()

        vm.lockScreen()
        vm.updateLockPinInput("654321")
        vm.unlockScreen()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.isUnlockingScreen)
        assertTrue(vm.uiState.value.isScreenLocked)

        // Duplicate clicks and input edits are ignored while verifying
        vm.updateLockPinInput("111111")
        vm.unlockScreen()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, fakeService.submittedPins.size)
        assertEquals("654321", vm.uiState.value.lockPinInput)

        gate.complete(RepositoryResult.Success(PinUnlockResult.Unlocked))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isUnlockingScreen)
        assertFalse(vm.uiState.value.isScreenLocked)
    }

    @Test
    fun offlineWithoutVerifier_showsButuhKoneksiMessageAndKeepsScreenLocked() = runTest(testDispatcher) {
        val fakeService = FakeCheckoutPinUnlockService {
            RepositoryResult.Error("OFFLINE_VERIFIER_UNAVAILABLE", "Butuh koneksi untuk membuka kunci")
        }
        val vm = buildViewModel(fakeService)
        advanceUntilIdle()

        vm.lockScreen()
        vm.updateLockPinInput("654321")
        vm.unlockScreen()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isScreenLocked)
        assertEquals("Butuh koneksi untuk membuka kunci", vm.uiState.value.lockPinError)
        assertEquals("", vm.uiState.value.lockPinInput)
    }

    @Test
    fun lockoutResponseStartsCountdownAndBlocksInputUntilExpired() = runTest(testDispatcher) {
        var nowMillis = 1_800_000_000_000L
        var elapsedMillis = 20_000L
        val fakeService = FakeCheckoutPinUnlockService {
            RepositoryResult.Success(
                PinUnlockResult.Failed(
                    message = "PIN terkunci",
                    attemptsLeft = 0,
                    lockedUntilEpochMillis = nowMillis + 30_000L,
                    isLocked = true,
                    remainingLockMillis = 30_000L,
                )
            )
        }
        val vm = buildViewModel(
            pinUnlockService = fakeService,
            clockMillis = { nowMillis },
            elapsedRealtimeMillis = { elapsedMillis },
            lockoutTickIntervalMillis = 100L,
        )
        advanceUntilIdle()

        vm.lockScreen()
        vm.updateLockPinInput("999999")
        vm.unlockScreen()
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.isScreenLocked)
        assertEquals(30L, vm.uiState.value.lockScreenRemainingSeconds)
        assertTrue(vm.uiState.value.lockPinError?.contains("00:30") == true)

        // Further input is blocked during lockout
        vm.updateLockPinInput("654321")
        vm.unlockScreen()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, fakeService.submittedPins.size)

        // Advance past monotonic lockout expiry so countdown ends cleanly
        nowMillis += 30_010L
        elapsedMillis += 30_010L
        advanceTimeBy(150L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(0L, vm.uiState.value.lockScreenRemainingSeconds)
        assertTrue(vm.uiState.value.isScreenLocked)
    }

    @Test
    fun quickPinLockoutCountdown_ignoresWallClockForwardAndBackwardJumps() = runTest(testDispatcher) {
        var wallMillis = 1_800_000_000_000L
        var elapsedMillis = 20_000L
        val fakeService = FakeCheckoutPinUnlockService {
            RepositoryResult.Success(
                PinUnlockResult.Failed(
                    message = "PIN terkunci",
                    attemptsLeft = 0,
                    lockedUntilEpochMillis = wallMillis + 30_000L,
                    isLocked = true,
                    remainingLockMillis = 30_000L,
                )
            )
        }
        val vm = buildViewModel(
            pinUnlockService = fakeService,
            clockMillis = { wallMillis },
            elapsedRealtimeMillis = { elapsedMillis },
            lockoutTickIntervalMillis = 100L,
        )
        advanceUntilIdle()

        vm.lockScreen()
        vm.updateLockPinInput("999999")
        vm.unlockScreen()
        testDispatcher.scheduler.runCurrent()
        assertEquals(30L, vm.uiState.value.lockScreenRemainingSeconds)

        // Advance wall clock +1 hour while monotonic elapsed advances 1s -> remains locked at 29s
        wallMillis += 3_600_000L
        elapsedMillis += 1_000L
        advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(29L, vm.uiState.value.lockScreenRemainingSeconds)
        assertTrue(vm.uiState.value.lockPinError?.contains("00:29") == true)

        // Move wall clock back -2 hours while monotonic elapsed advances 1s -> 28s, does NOT jump by +1 hour
        wallMillis -= 7_200_000L
        elapsedMillis += 1_000L
        advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(28L, vm.uiState.value.lockScreenRemainingSeconds)
        assertTrue(vm.uiState.value.lockPinError?.contains("00:28") == true)

        // Input remains blocked during monotonic lockout
        vm.updateLockPinInput("654321")
        vm.unlockScreen()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, fakeService.submittedPins.size)

        // After monotonic 28s+ pass -> countdown ends
        elapsedMillis += 28_001L
        advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(0L, vm.uiState.value.lockScreenRemainingSeconds)
        assertNull(vm.uiState.value.lockPinError)
    }

    @Test
    fun failClosedWhenPinUnlockServiceMissing() = runTest(testDispatcher) {
        val vm = buildViewModel(pinUnlockService = null)
        advanceUntilIdle()

        vm.lockScreen()
        vm.updateLockPinInput("123456")
        vm.unlockScreen()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isScreenLocked)
        assertEquals("Layanan verifikasi PIN tidak tersedia.", vm.uiState.value.lockPinError)
    }

    @Test
    fun quickPinLockOverlay_disablesKeypadAndButtonWhileVerifyingOrLockedOut() {
        var pinChanges = 0
        var unlockClicks = 0
        composeRule.setContent {
            TbterminalappTheme {
                QuickPinLockOverlay(
                    cashierName = "Budi",
                    pinInput = "123456",
                    errorMessage = null,
                    onPinChange = { pinChanges++ },
                    onUnlock = { unlockClicks++ },
                    isLoading = true,
                    isLockedOut = false,
                )
            }
        }

        composeRule.onNodeWithText("Memverifikasi...").assertIsNotEnabled()
        composeRule.onNodeWithText("1").performClick()
        assertEquals(0, pinChanges)
        assertEquals(0, unlockClicks)
    }

    private fun buildViewModel(
        pinUnlockService: PinUnlockService?,
        clockMillis: () -> Long = { System.currentTimeMillis() },
        elapsedRealtimeMillis: () -> Long = clockMillis,
        lockoutTickIntervalMillis: Long = 1_000L,
    ): CheckoutViewModel {
        return CheckoutViewModel(
            checkoutRepository = object : CheckoutRepository {
                override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> =
                    error("unused")
                override suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt> =
                    error("unused")
            },
            inventoryRepository = object : InventoryRepository {
                override suspend fun getProductStocks(page: Int, limit: Int, search: String?): RepositoryResult<ProductStockPage> =
                    RepositoryResult.Success(ProductStockPage(emptyList(), 0L, page, limit, 1))
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
            },
            customerRepository = object : CustomerRepository {
                override suspend fun getCustomers(page: Int, limit: Int, search: String?, status: String?): RepositoryResult<CustomerPage> =
                    RepositoryResult.Success(CustomerPage(emptyList(), 0L, page, limit, 1))
                override suspend fun getCustomerById(id: String): RepositoryResult<Customer> = error("unused")
                override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> = error("unused")
                override suspend fun updateCustomer(id: String, command: CustomerCommand): RepositoryResult<Customer> = error("unused")
                override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> = error("unused")
                override suspend fun activateCustomer(id: String): RepositoryResult<Customer> = error("unused")
            },
            cashReconciliationRepository = object : CashReconciliationRepository {
                override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(null)
                override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("unused")
                override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("unused")
                override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
                override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
                override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
                override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashTransactionPage> = error("unused")
                override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("unused")
                override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("unused")
                override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("unused")
                override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("unused")
                override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = error("unused")
                override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
                override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
            },
            pinUnlockService = pinUnlockService,
            clockMillis = clockMillis,
            elapsedRealtimeMillis = elapsedRealtimeMillis,
            lockoutTickIntervalMillis = lockoutTickIntervalMillis,
        )
    }
}

private class FakeCheckoutPinUnlockService(
    private val responder: suspend (String) -> RepositoryResult<PinUnlockResult>,
) : PinUnlockService {
    val submittedPins = mutableListOf<String>()

    override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> {
        submittedPins.add(pin)
        return responder(pin)
    }

    override suspend fun recordOnlineVerifiedPin(pin: String) = Unit
    override fun clearOfflineState() = Unit
}
