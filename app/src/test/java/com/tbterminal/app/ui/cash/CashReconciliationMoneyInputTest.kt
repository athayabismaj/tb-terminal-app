package com.tbterminal.app.ui.cash

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashexpense.CashMovementType
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
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
import com.tbterminal.app.data.sync.OfflineCashExpenseSyncResult
import com.tbterminal.app.data.sync.OfflineCashExpenseSyncService
import com.tbterminal.app.data.sync.OfflineCashSessionSyncService
import com.tbterminal.app.ui.checkout.CheckoutViewModel
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class CashReconciliationMoneyInputTest {
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
        )
            .allowMainThreadQueries()
            .setQueryCoroutineContext(testDispatcher)
            .build()

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
    fun offlineExpense_parsesPlainAndThousandSeparatedInputs_andSyncsExactAmount() = runTest(testDispatcher) {
        val sessionLocalId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-1",
                clientGeneratedId = "sess-local-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = 1_700_000_000_000L,
                startingCash = 500_000.0,
                syncStatus = SyncStatus.SYNCED
            )
        )

        val localSessionService = LocalCashSessionService(database, deviceId = "android-dev-1")
        val localExpenseService = LocalCashExpenseService(database, deviceId = "android-dev-1")
        val vm = CashReconciliationViewModel(
            repository = OfflineCashRepo(),
            cashSessionLocalDataSource = CashSessionLocalDataSource(database.cashSessionDao()),
            localCashSessionService = localSessionService,
            localCashExpenseService = localExpenseService,
            cashierUserId = "kasir-1",
            actorRole = "KASIR"
        )
        advanceUntilIdle()

        val sessionSync = OfflineCashSessionSyncService(database, salesApi, "android-dev-1")
        val expenseSync = OfflineCashExpenseSyncService(database, salesApi, "android-dev-1", sessionSync)

        val cases = listOf(
            Triple("20000", 20_000.0, "20000.00"),
            Triple("20.000", 20_000.0, "20000.00"),
            Triple("1.250.000", 1_250_000.0, "1250000.00")
        )

        cases.forEachIndexed { index, (input, expectedDouble, expectedSyncAmount) ->
            vm.showPettyCashDialog(CashMovementType.CASH_OUT)
            vm.onPettyCashAmountChanged(input)
            vm.onPettyCashDescriptionChanged("Bensin $index")
            vm.addPettyCash()
            advanceUntilIdle()

            assertNull("Unexpected error for input '$input': ${vm.uiState.value.errorMessage}", vm.uiState.value.errorMessage)

            val savedExpenses = database.cashExpenseDao().getByCashSession(sessionLocalId)
            assertEquals(index + 1, savedExpenses.size)
            val saved = savedExpenses.maxBy { it.localId }
            assertEquals("Wrong Room amount for input '$input'", expectedDouble, saved.amount, 0.001)

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"srv-exp-$index","syncedAt":"2026-11-26T09:00:00Z"}}"""
                    )
            )
            val syncResult = expenseSync.syncExpense(saved.localId)
            assertTrue(
                "Expected sync success for '$input', got ${(syncResult as? OfflineCashExpenseSyncResult.Failed)?.message}",
                syncResult is OfflineCashExpenseSyncResult.Success
            )
            val requestJson = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertEquals("Wrong synced amount for input '$input'", expectedSyncAmount, requestJson["amount"]?.jsonPrimitive?.content)
        }

        val countBeforeZero = database.cashExpenseDao().getByCashSession(sessionLocalId).size
        vm.showPettyCashDialog(CashMovementType.CASH_OUT)
        vm.onPettyCashAmountChanged("0")
        vm.onPettyCashDescriptionChanged("Bensin 0")
        vm.addPettyCash()
        advanceUntilIdle()

        assertEquals("Nominal harus lebih besar dari nol.", vm.uiState.value.errorMessage)
        assertEquals(countBeforeZero, database.cashExpenseDao().getByCashSession(sessionLocalId).size)
    }

    @Test
    fun onlineExpense_parsesThousandSeparatedInputsWithoutTruncation() = runTest(testDispatcher) {
        val onlineRepo = RecordingOnlineCashRepo()
        val vm = CashReconciliationViewModel(
            repository = onlineRepo,
            cashSessionLocalDataSource = CashSessionLocalDataSource(database.cashSessionDao()),
            localCashSessionService = LocalCashSessionService(database, deviceId = "android-dev-1"),
            localCashExpenseService = LocalCashExpenseService(database, deviceId = "android-dev-1"),
            cashierUserId = "kasir-1",
            actorRole = "KASIR"
        )
        advanceUntilIdle()

        val validCases = listOf(
            "20000" to BigDecimal("20000"),
            "20.000" to BigDecimal("20000"),
            "1.250.000" to BigDecimal("1250000")
        )

        validCases.forEachIndexed { index, (input, expected) ->
            vm.showPettyCashDialog(CashMovementType.CASH_OUT)
            vm.onPettyCashAmountChanged(input)
            vm.onPettyCashDescriptionChanged("Operasional $index")
            vm.addPettyCash()
            advanceUntilIdle()

            assertNull("Unexpected error for input '$input': ${vm.uiState.value.errorMessage}", vm.uiState.value.errorMessage)
            assertEquals(index + 1, onlineRepo.recordedExpenses.size)
            assertEquals(0, expected.compareTo(onlineRepo.recordedExpenses.last().first))
        }

        val countBeforeZero = onlineRepo.recordedExpenses.size
        vm.showPettyCashDialog(CashMovementType.CASH_OUT)
        vm.onPettyCashAmountChanged("0")
        vm.onPettyCashDescriptionChanged("Zero")
        vm.addPettyCash()
        advanceUntilIdle()

        assertEquals("Nominal harus lebih besar dari nol.", vm.uiState.value.errorMessage)
        assertEquals(countBeforeZero, onlineRepo.recordedExpenses.size)
    }

    @Test
    fun offlineCashSession_openingAndClosingCash_parsePlainThousandSeparatedAndZero() = runTest(testDispatcher) {
        val vm = CashReconciliationViewModel(
            repository = OfflineCashRepo(),
            cashSessionLocalDataSource = CashSessionLocalDataSource(database.cashSessionDao()),
            localCashSessionService = LocalCashSessionService(database, deviceId = "android-dev-1"),
            localCashExpenseService = LocalCashExpenseService(database, deviceId = "android-dev-1"),
            cashierUserId = "kasir-1",
            actorRole = "KASIR"
        )
        advanceUntilIdle()

        val inputs = listOf(
            "0" to 0.0,
            "20000" to 20_000.0,
            "20.000" to 20_000.0,
            "1.250.000" to 1_250_000.0
        )

        inputs.forEachIndexed { index, (input, expectedDouble) ->
            vm.onOpeningCashChanged(input)
            vm.openSession()
            advanceUntilIdle()

            assertNull("Open session error for '$input': ${vm.uiState.value.errorMessage}", vm.uiState.value.errorMessage)
            val localId = vm.uiState.value.localCashSessionId!!
            val openedEntity = database.cashSessionDao().getByLocalId(localId)!!
            assertEquals("Wrong startingCash for '$input'", expectedDouble, openedEntity.startingCash, 0.001)

            vm.onClosingCashChanged(input)
            vm.onClosingNotesChanged("Tutup $index")
            vm.closeSession()
            advanceUntilIdle()

            assertNull("Close session error for '$input': ${vm.uiState.value.errorMessage}", vm.uiState.value.errorMessage)
            val closedEntity = database.cashSessionDao().getByLocalId(localId)!!
            assertEquals("Wrong actualCash for '$input'", expectedDouble, closedEntity.actualCash!!, 0.001)
        }
    }

    @Test
    fun checkoutCashPaymentInput_stripsThousandSeparatorsCleanly() = runTest(testDispatcher) {
        val checkoutVm = buildCheckoutViewModel()
        advanceUntilIdle()

        val cases = listOf(
            "20000" to "20000",
            "20.000" to "20000",
            "1.250.000" to "1250000",
            "0" to "0"
        )

        cases.forEach { (raw, expectedNormalized) ->
            checkoutVm.onAmountPaidChanged(raw)
            assertEquals(expectedNormalized, checkoutVm.uiState.value.amountPaidInput)

            checkoutVm.onStartingCashChanged(raw)
            assertEquals(expectedNormalized, checkoutVm.uiState.value.startingCashInput)

            checkoutVm.onPettyCashAmountChanged(raw)
            assertEquals(expectedNormalized, checkoutVm.uiState.value.pettyCashAmountInput)
        }
    }

    private fun buildCheckoutViewModel(): CheckoutViewModel {
        return CheckoutViewModel(
            checkoutRepository = object : CheckoutRepository {
                override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> = error("unused")
                override suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt> = error("unused")
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
            cashReconciliationRepository = OfflineCashRepo()
        )
    }
}

private class OfflineCashRepo : CashReconciliationRepository {
    override suspend fun getActiveSession(): RepositoryResult<CashSession?> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashTransactionPage> =
        RepositoryResult.Success(CashTransactionPage(emptyList(), 0L, page, limit, 1))
    override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("unused")
    override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("unused")
    override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("unused")
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> =
        RepositoryResult.Exception(IOException("offline"))
    override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
}

private class RecordingOnlineCashRepo : CashReconciliationRepository {
    val recordedExpenses = mutableListOf<Pair<BigDecimal, String>>()

    override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(
        CashSession(
            id = "srv-sess-online-1",
            userId = "kasir-1",
            openedAt = "2026-11-26T08:00:00+07:00",
            closedAt = null,
            openingCash = BigDecimal("100000"),
            closingCash = null,
            systemCash = BigDecimal("100000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN"
        )
    )

    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("unused")
    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("unused")
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashTransactionPage> =
        RepositoryResult.Success(CashTransactionPage(emptyList(), 0L, page, limit, 1))
    override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("unused")
    override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("unused")
    override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("unused")
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> {
        recordedExpenses += amount to description
        return RepositoryResult.Success(
            CashExpense(
                id = "srv-exp-online-${recordedExpenses.size}",
                sessionId = "srv-sess-online-1",
                userId = "kasir-1",
                amount = amount,
                description = description,
                createdAt = "2026-11-26T09:00:00+07:00"
            )
        )
    }

    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> =
        RepositoryResult.Success(emptyList())
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
    override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
}
