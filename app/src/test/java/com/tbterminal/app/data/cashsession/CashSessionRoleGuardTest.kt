package com.tbterminal.app.data.cashsession

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashsession.LocalCashSessionResult
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
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
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.OfflineCashSessionSyncResult
import com.tbterminal.app.data.sync.OfflineCashSessionSyncService
import com.tbterminal.app.data.sync.OfflineCheckoutSyncFailureKind
import com.tbterminal.app.ui.cash.CashReconciliationViewModel
import com.tbterminal.app.ui.checkout.CheckoutViewModel
import com.tbterminal.app.ui.common.UiText
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CashSessionRoleGuardTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: TbTerminalDatabase
    private lateinit var server: MockWebServer
    private lateinit var salesApi: SalesApi

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java
        ).allowMainThreadQueries().build()

        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        salesApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SalesApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun localCashSessionService_rejectsOwnerAndAdmin_andAllowsKasir() = runBlocking {
        listOf("OWNER", "admin").forEach { forbiddenRole ->
            val service = LocalCashSessionService(database, deviceId = "android-dev-1") { forbiddenRole }
            val result = service.openLocalSession("user-1", BigDecimal("150000"))

            assertTrue("Expected failure for $forbiddenRole", result is LocalCashSessionResult.Failed)
            assertEquals(
                "Hanya kasir yang dapat membuka sesi kas.",
                (result as LocalCashSessionResult.Failed).message,
            )
            assertEquals(0, database.cashSessionDao().countByStatus("OPEN"))
        }

        val kasirService = LocalCashSessionService(database, deviceId = "android-dev-1") { "KASIR" }
        val successResult = kasirService.openLocalSession("kasir-1", BigDecimal("150000"))
        assertTrue(successResult is LocalCashSessionResult.Success)
        assertEquals(1, database.cashSessionDao().countByStatus("OPEN"))
    }

    @Test
    fun offlineCashSessionSyncService_marksConflictAndSkipsApiForOwnerAndAdmin_andSyncsForKasir() = runBlocking {
        listOf("OWNER", "ADMIN").forEachIndexed { idx, forbiddenRole ->
            val sessionId = database.cashSessionDao().upsert(
                LocalCashSessionEntity(
                    clientGeneratedId = "sess-forbidden-$idx",
                    deviceId = "android-dev-1",
                    cashierUserId = "user-$idx",
                    status = "OPEN",
                    startingCash = ScaledDecimal2.ofWhole(100_000L),
                )
            )
            val queueId = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.CASH_SESSION,
                    entityLocalId = sessionId,
                    operation = SyncOperation.CREATE,
                    payloadJson = "{}",
                )
            )
            val service = OfflineCashSessionSyncService(database, salesApi, "android-dev-1") { forbiddenRole }

            val result = service.syncOpenSession(sessionId) as OfflineCashSessionSyncResult.Failed
            assertEquals(OfflineCheckoutSyncFailureKind.CONFLICT, result.kind)
            assertEquals("Hanya kasir yang dapat membuka sesi kas.", result.message)
            assertEquals(0, server.requestCount)

            val queueEntry = database.syncQueueDao().getById(queueId)!!
            assertEquals(SyncStatus.CONFLICT, queueEntry.status)
            assertEquals("FORBIDDEN_CASH_SESSION_ROLE", queueEntry.lastErrorCode)
        }

        val kasirSessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-kasir-ok",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                startingCash = ScaledDecimal2.ofWhole(100_000L),
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = kasirSessionId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-sess-1","openedAt":"2026-11-24T10:00:00Z","syncedAt":"2026-11-24T10:01:00Z"}}"""
            )
        )
        val kasirService = OfflineCashSessionSyncService(database, salesApi, "android-dev-1") { "KASIR" }
        val okResult = kasirService.syncOpenSession(kasirSessionId)
        assertTrue(okResult is OfflineCashSessionSyncResult.Success)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun cashReconciliationViewModel_hidesAndRejectsOpenSessionForOwnerAndAdmin_andAllowsKasir() = runTest(testDispatcher) {
        listOf("OWNER", "ADMIN").forEach { forbiddenRole ->
            val repo = FakeRoleGuardCashRepository()
            val vm = CashReconciliationViewModel(repository = repo, actorRole = forbiddenRole)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.canOpenCashSession)
            vm.onOpeningCashChanged("100000")
            vm.openSession()
            advanceUntilIdle()

            assertEquals("Hanya kasir yang dapat membuka sesi kas.", vm.uiState.value.errorMessage)
            assertEquals(0, repo.openSessionCalls)
        }

        val kasirRepo = FakeRoleGuardCashRepository()
        val kasirVm = CashReconciliationViewModel(repository = kasirRepo, actorRole = "KASIR")
        advanceUntilIdle()

        assertTrue(kasirVm.uiState.value.canOpenCashSession)
        kasirVm.onOpeningCashChanged("100000")
        kasirVm.openSession()
        advanceUntilIdle()

        assertEquals(1, kasirRepo.openSessionCalls)
        assertNotNull(kasirVm.uiState.value.activeSession)
    }

    @Test
    fun checkoutViewModel_rejectsOpenSessionForOwnerAndAdmin_andAllowsKasir() = runTest(testDispatcher) {
        listOf("OWNER", "ADMIN").forEach { forbiddenRole ->
            val cashRepo = FakeRoleGuardCashRepository()
            val vm = CheckoutViewModel(
                checkoutRepository = fakeCheckoutRepo(),
                inventoryRepository = fakeInventoryRepo(),
                customerRepository = fakeCustomerRepo(),
                cashReconciliationRepository = cashRepo,
                actorRole = forbiddenRole,
            )
            advanceUntilIdle()

            assertFalse(vm.uiState.value.canOpenCashSession)
            vm.onStartingCashChanged("100000")
            vm.openCashSession()
            advanceUntilIdle()

            val err = vm.uiState.value.cashSessionError as? UiText.DynamicString
            assertEquals("Hanya kasir yang dapat membuka sesi kas.", err?.value)
            assertEquals(0, cashRepo.openSessionCalls)
        }

        val kasirRepo = FakeRoleGuardCashRepository()
        val kasirVm = CheckoutViewModel(
            checkoutRepository = fakeCheckoutRepo(),
            inventoryRepository = fakeInventoryRepo(),
            customerRepository = fakeCustomerRepo(),
            cashReconciliationRepository = kasirRepo,
            actorRole = "KASIR",
        )
        advanceUntilIdle()

        assertTrue(kasirVm.uiState.value.canOpenCashSession)
        kasirVm.onStartingCashChanged("100000")
        kasirVm.openCashSession()
        advanceUntilIdle()

        assertEquals(1, kasirRepo.openSessionCalls)
        assertTrue(kasirVm.uiState.value.hasActiveCashSession)
    }

    private fun fakeCheckoutRepo(): CheckoutRepository = object : CheckoutRepository {
        override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> =
            error("unused")
        override suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt> =
            error("unused")
    }

    private fun fakeInventoryRepo(): InventoryRepository = object : InventoryRepository {
        override suspend fun getProductStocks(page: Int, limit: Int, search: String?): RepositoryResult<ProductStockPage> =
            RepositoryResult.Success(ProductStockPage(data = emptyList(), total = 0L, page = page, limit = limit, totalPages = 1))
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

    private fun fakeCustomerRepo(): CustomerRepository = object : CustomerRepository {
        override suspend fun getCustomers(
            page: Int,
            limit: Int,
            search: String?,
            status: String?,
        ): RepositoryResult<CustomerPage> = RepositoryResult.Success(
            CustomerPage(data = emptyList(), total = 0L, page = page, limit = limit, totalPages = 1)
        )
        override suspend fun getCustomerById(id: String): RepositoryResult<Customer> = error("unused")
        override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> = error("unused")
        override suspend fun updateCustomer(id: String, command: CustomerCommand): RepositoryResult<Customer> = error("unused")
        override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun activateCustomer(id: String): RepositoryResult<Customer> = error("unused")
    }
}

private class FakeRoleGuardCashRepository : CashReconciliationRepository {
    var openSessionCalls = 0
    private var activeSession: CashSession? = null

    override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(activeSession)

    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> {
        openSessionCalls += 1
        val created = CashSession(
            id = "srv-sess-$openSessionCalls",
            userId = "kasir-1",
            userName = "Kasir",
            openedAt = "2026-11-24T10:00:00Z",
            closedAt = null,
            openingCash = startingCash,
            closingCash = null,
            systemCash = startingCash,
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            totalCashIn = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
        )
        activeSession = created
        return RepositoryResult.Success(created)
    }

    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> =
        RepositoryResult.Success(CashSessionPage(data = emptyList(), total = 0L, page = page, limit = limit, totalPages = 1))
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashTransactionPage> =
        RepositoryResult.Success(CashTransactionPage(data = emptyList(), total = 0L, page = page, limit = limit, totalPages = 1))
    override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("unused")
    override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("unused")
    override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("unused")
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("unused")
    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = RepositoryResult.Success(emptyList())
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
    override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
}
