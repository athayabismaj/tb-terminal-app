package com.tbterminal.app.ui.checkout

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.RoomPendingSubmissionStore
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CheckoutPreviewItem
import com.tbterminal.app.data.model.CheckoutReceipt
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
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
class CheckoutAmbiguousSurvivalTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: TbTerminalDatabase
    private var nowMillis = 1_000_000L
    private lateinit var store: RoomPendingSubmissionStore

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
            clockMillis = { nowMillis }
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun ambiguousCheckout_survivesRestartWithExactSameKeyAndCart_thenClearsOnSuccess() = runTest(dispatcher) {
        val checkoutRepo = FakeAmbiguousCheckoutRepository().apply {
            nextSubmitResult = RepositoryResult.Exception(IOException("Read timed out"))
        }
        val vm1 = buildVm(checkoutRepo)
        advanceUntilIdle()

        vm1.addProduct(
            Product(
                productId = "prod-1",
                name = "Semen Tiga Roda",
                unitPrice = BigDecimal("70000"),
                unitName = "Sak",
                stockQty = BigDecimal("20")
            )
        )
        vm1.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        assertEquals(1, checkoutRepo.submittedCommands.size)
        val originalKey = checkoutRepo.submittedCommands[0].idempotencyKey
        assertTrue(vm1.uiState.value.hasAmbiguousCheckout)
        assertEquals(PendingSubmissionStatus.AMBIGUOUS, store.load(PendingSubmissionScopes.CHECKOUT)?.status)

        // Simulate app restart -> brand-new CheckoutViewModel backed by same Room store
        val vm2 = buildVm(checkoutRepo)
        advanceUntilIdle()

        assertTrue(vm2.uiState.value.hasAmbiguousCheckout)
        assertEquals(1, vm2.uiState.value.cartItems.size)
        assertEquals("Semen Tiga Roda", vm2.uiState.value.cartItems[0].productName)
        assertEquals(0, BigDecimal("70000").compareTo(vm2.uiState.value.finalTotal))

        // Mutating cart is blocked while ambiguous checkout is pending
        vm2.addProduct(
            Product(
                productId = "prod-2",
                name = "Pasir",
                unitPrice = BigDecimal("50000"),
                unitName = "Kubik",
                stockQty = BigDecimal("10")
            )
        )
        assertEquals(1, vm2.uiState.value.cartItems.size)

        // Retry after restart must reuse the EXACT same idempotencyKey and NOT generate a new one
        checkoutRepo.nextSubmitResult = null // succeeds now
        vm2.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        assertEquals(2, checkoutRepo.submittedCommands.size)
        assertEquals(originalKey, checkoutRepo.submittedCommands[1].idempotencyKey)
        assertFalse(vm2.uiState.value.hasAmbiguousCheckout)
        assertTrue(vm2.uiState.value.cartItems.isEmpty())
        assertNull(store.load(PendingSubmissionScopes.CHECKOUT))
    }

    @Test
    fun inFlightCheckoutAndNonAmbiguousError_behavesSafelyAcrossRestart() = runTest(dispatcher) {
        val checkoutRepo = FakeAmbiguousCheckoutRepository()
        val vm1 = buildVm(checkoutRepo)
        advanceUntilIdle()

        vm1.addProduct(
            Product(
                productId = "prod-1",
                name = "Paku",
                unitPrice = BigDecimal("20000"),
                unitName = "Kg",
                stockQty = BigDecimal("10")
            )
        )
        checkoutRepo.nextSubmitResult = RepositoryResult.Error("HTTP_502", "Bad Gateway")
        vm1.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("20000"))
        advanceUntilIdle()
        val key = checkoutRepo.submittedCommands[0].idempotencyKey

        // Simulate restart and non-ambiguous 400 error -> clears persisted ambiguous state
        val vm2 = buildVm(checkoutRepo)
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.hasAmbiguousCheckout)

        checkoutRepo.nextSubmitResult = RepositoryResult.Error("INSUFFICIENT_STOCK", "Stok kurang")
        vm2.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("20000"))
        advanceUntilIdle()

        assertEquals(key, checkoutRepo.submittedCommands[1].idempotencyKey)
        assertFalse(vm2.uiState.value.hasAmbiguousCheckout)
        assertNull(store.load(PendingSubmissionScopes.CHECKOUT))
    }

    @Test
    fun corruptAndExpiredPendingCheckoutInRoom_doNotCrashNewViewModel() = runTest(dispatcher) {
        database.pendingSubmissionDao().upsert(
            PendingSubmissionEntity(
                ownerUserId = "kasir-1",
                scope = PendingSubmissionScopes.CHECKOUT,
                idempotencyKey = "broken-key",
                intentJson = "fp",
                payloadJson = "{not-valid-json",
                status = "AMBIGUOUS",
                schemaVersion = 1,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )

        val vm = buildVm(FakeAmbiguousCheckoutRepository())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasAmbiguousCheckout)
        assertTrue(vm.uiState.value.cartItems.isEmpty())
        assertNull(database.pendingSubmissionDao().get("kasir-1", PendingSubmissionScopes.CHECKOUT))
    }

    private fun buildVm(checkoutRepo: CheckoutRepository): CheckoutViewModel {
        return CheckoutViewModel(
            checkoutRepository = checkoutRepo,
            inventoryRepository = FakeCheckoutInventoryRepository(),
            customerRepository = FakeCheckoutCustomerRepository(),
            cashReconciliationRepository = FakeCheckoutCashRepository(),
            clockMillis = { nowMillis },
            pendingSubmissionStore = store
        )
    }

    private class FakeAmbiguousCheckoutRepository : CheckoutRepository {
        val submittedCommands = mutableListOf<CheckoutSubmitCommand>()
        var nextSubmitResult: RepositoryResult<CheckoutReceipt>? = null

        override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> {
            val total = command.items.fold(BigDecimal.ZERO) { acc, item ->
                acc.add(item.quantity.multiply(BigDecimal("70000")))
            }
            return RepositoryResult.Success(
                CheckoutPreview(
                    checkoutAttemptId = "attempt-1",
                    discountFingerprint = "fp",
                    items = command.items.map {
                        CheckoutPreviewItem(
                            productId = it.productId,
                            quantity = it.quantity,
                            unitPrice = BigDecimal("70000"),
                            grossLineTotal = total,
                            discountType = null,
                            discountValue = BigDecimal.ZERO,
                            discountAmount = BigDecimal.ZERO,
                            netLineTotal = total
                        )
                    },
                    grossSubtotal = total,
                    itemDiscountTotal = BigDecimal.ZERO,
                    transactionDiscountAmount = BigDecimal.ZERO,
                    totalDiscountAmount = BigDecimal.ZERO,
                    effectiveDiscountPercent = BigDecimal.ZERO,
                    netTotal = total,
                    cashierDiscountLimitPercent = BigDecimal("10"),
                    approvalRequired = false,
                    expiresAt = "2026-11-27T13:00:00Z"
                )
            )
        }

        override suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt> {
            submittedCommands += command
            nextSubmitResult?.let { nextSubmitResult = null; return it }
            return RepositoryResult.Success(
                CheckoutReceipt("RCP-${submittedCommands.size}", "tx-${submittedCommands.size}", command.amountPaid, "0", false)
            )
        }
    }

    private class FakeCheckoutInventoryRepository : InventoryRepository {
        override suspend fun getProductStocks(page: Int, limit: Int, search: String?) = RepositoryResult.Success(
            ProductStockPage(
                data = listOf(
                    ProductStock(
                        productId = "prod-1", sku = "SMN-1", productName = "Semen Tiga Roda",
                        categoryName = "Material", unitName = "Sak", quantity = BigDecimal("20"),
                        minStock = BigDecimal("5"), priceBuy = BigDecimal("60000"),
                        priceRetail = BigDecimal("70000"), priceContractor = BigDecimal("68000"),
                        discount = BigDecimal.ZERO, isActive = true
                    )
                ),
                total = 1, page = 1, limit = limit, totalPages = 1
            )
        )
        override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): RepositoryResult<StockAdjustmentPage> = error("unused")
        override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): RepositoryResult<StockCard> = error("unused")
        override suspend fun getProduct(id: String) = error("unused")
        override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> = error("unused")
        override suspend fun getProductsPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductPage> = error("unused")
        override suspend fun createProduct(command: CreateProductCommand) = error("unused")
        override suspend fun updateProduct(id: String, command: UpdateProductCommand) = error("unused")
        override suspend fun deleteProduct(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun activateProduct(id: String) = error("unused")
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

    private class FakeCheckoutCustomerRepository : CustomerRepository {
        override suspend fun getCustomers(page: Int, limit: Int, search: String?, status: String?) =
            RepositoryResult.Success(CustomerPage(emptyList(), 0, 1, limit, 1))
        override suspend fun getCustomerById(id: String): RepositoryResult<Customer> = error("unused")
        override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> = error("unused")
        override suspend fun updateCustomer(id: String, command: CustomerCommand): RepositoryResult<Customer> = error("unused")
        override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun activateCustomer(id: String): RepositoryResult<Customer> = error("unused")
    }

    private class FakeCheckoutCashRepository : CashReconciliationRepository {
        override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(
            CashSession(
                id = "sess-1", userId = "kasir-1", openedAt = "2026-11-27T08:00:00+07:00",
                closedAt = null, openingCash = BigDecimal("100000"), closingCash = null,
                systemCash = BigDecimal("100000"), difference = null, totalExpenses = BigDecimal.ZERO,
                notes = null, status = "OPEN"
            )
        )
        override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("unused")
        override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
        override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("unused")
        override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
        override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
        override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashTransactionPage> = RepositoryResult.Success(CashTransactionPage(emptyList(), 0, 1, limit, 1))
        override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("unused")
        override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("unused")
        override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("unused")
        override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("unused")
        override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = error("unused")
        override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
        override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
    }
}
