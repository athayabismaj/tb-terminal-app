package com.tbterminal.app.ui.checkout

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.checkout.LocalCheckoutLookupService
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.database.CheckoutDraftRecord
import com.tbterminal.app.data.local.database.CheckoutDraftStore
import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.database.RoomCheckoutDraftStore
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.CheckoutDraftEntity
import com.tbterminal.app.data.local.entity.LocalCustomerEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CheckoutPreviewItem
import com.tbterminal.app.data.model.CheckoutReceipt
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.model.DiscountType
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
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.NetworkMonitor
import com.tbterminal.app.data.sync.OfflineStatusRepository
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CheckoutCartDraftSurvivalTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: TbTerminalDatabase
    private lateinit var productLocalDataSource: ProductLocalDataSource
    private lateinit var customerLocalDataSource: CustomerLocalDataSource
    private var currentOwner = "kasir-1"
    private lateinit var draftStore: RoomCheckoutDraftStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { command -> command.run() }
            .setTransactionExecutor { command -> command.run() }
            .build()
        productLocalDataSource = ProductLocalDataSource(database.productDao())
        customerLocalDataSource = CustomerLocalDataSource(database.customerDao())
        draftStore = RoomCheckoutDraftStore(
            dao = database.checkoutDraftDao(),
            ownerUserIdProvider = { currentOwner }
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun activeCartCustomerDiscountsDeliveryAndHeldCarts_surviveProcessDeath_andIsolateByUser() = runTest(dispatcher) {
        val semen = Product(
            productId = "prod-semen",
            name = "Semen Tiga Roda",
            unitPrice = BigDecimal("70000"),
            sku = "SMN-1",
            unitName = "Sak",
            stockQty = BigDecimal("50"),
            secondaryUnitId = "unit-palet",
            secondaryUnitName = "Palet",
            secondaryUnitFactor = BigDecimal("10"),
            baseUnitId = "unit-sak"
        )
        val paku = Product(
            productId = "prod-paku",
            name = "Paku 5cm",
            unitPrice = BigDecimal("15000"),
            sku = "PK-5",
            unitName = "Kg",
            stockQty = BigDecimal("100"),
            baseUnitId = "unit-kg"
        )
        val customer = Customer(
            id = "cust-1",
            name = "CV Bangun Jaya",
            phone = "08123456789",
            address = "Jl. Proyek 12",
            isContractor = true,
            creditLimit = BigDecimal("10000000"),
            paymentTermDays = 30,
            isActive = true,
            createdAt = "2026-11-01T00:00:00Z",
            updatedAt = "2026-11-01T00:00:00Z"
        )
        cacheProduct(semen, priceContractor = BigDecimal("68000"))
        cacheProduct(paku, priceContractor = BigDecimal("14000"))
        cacheCustomer(customer)

        val vm1 = buildVm(debounceMs = 0L)
        advanceUntilIdle()

        // Create held cart #1
        vm1.addProduct(paku)
        vm1.holdCurrentCart("Parkir 1 - Paku")
        advanceUntilIdle()

        // Create held cart #2 with customer
        vm1.selectCustomer(customer)
        vm1.addProduct(semen)
        vm1.holdCurrentCart("Parkir 2 - Semen")
        advanceUntilIdle()

        // Build active cart with secondary unit + item discount + tx discount + customer + delivery
        vm1.selectCustomer(customer)
        vm1.addProduct(semen, isSecondary = true)
        vm1.addProduct(paku)
        vm1.setItemDiscount("prod-paku", CheckoutDiscount(DiscountType.FIXED_AMOUNT, BigDecimal("1000")))
        vm1.setTransactionDiscount(CheckoutDiscount(DiscountType.PERCENTAGE, BigDecimal("5")))
        vm1.toggleDelivery(true)
        vm1.updateDeliveryFee("25000")
        vm1.updateDeliveryAddress("Jl. Proyek 12")
        advanceUntilIdle()

        // Simulate process death & VM recreation with same Room store
        val vm2 = buildVm(debounceMs = 0L)
        advanceUntilIdle()

        val restored = vm2.uiState.value
        assertEquals(2, restored.heldCarts.size)
        assertEquals(setOf("Parkir 1 - Paku", "Parkir 2 - Semen"), restored.heldCarts.map { it.note }.toSet())
        assertEquals("cust-1", restored.selectedCustomer?.id)
        assertEquals(2, restored.cartItems.size)

        val restoredSemen = restored.cartItems.first { it.productId == "prod-semen" }
        assertTrue(restoredSemen.isSecondaryUnit)
        assertEquals("Palet", restoredSemen.unitName)
        assertEquals(0, BigDecimal("10").compareTo(restoredSemen.secondaryUnitFactor))

        val restoredPaku = restored.cartItems.first { it.productId == "prod-paku" }
        assertEquals(DiscountType.FIXED_AMOUNT, restoredPaku.discountRequest?.type)
        assertEquals(0, BigDecimal("1000").compareTo(restoredPaku.discountRequest?.value))

        assertEquals(DiscountType.PERCENTAGE, restored.transactionDiscount?.type)
        assertEquals(0, BigDecimal("5").compareTo(restored.transactionDiscount?.value))
        assertTrue(restored.isDeliveryEnabled)
        assertEquals(0, BigDecimal("25000").compareTo(restored.deliveryFee))
        assertEquals("Jl. Proyek 12", restored.deliveryAddress)

        // Different cashier user must NOT receive Kasir 1's cart or held carts
        currentOwner = "kasir-2"
        val vmKasir2 = buildVm(debounceMs = 0L)
        advanceUntilIdle()
        assertTrue(vmKasir2.uiState.value.cartItems.isEmpty())
        assertTrue(vmKasir2.uiState.value.heldCarts.isEmpty())
    }

    @Test
    fun productMissingFromRoomAndServer_isRestoredFromFallbackSnapshotWithoutSkipping() = runTest(dispatcher) {
        val offlineUncachedProduct = Product(
            productId = "prod-uncached",
            name = "Besi Ulir 13mm",
            unitPrice = BigDecimal("95000"),
            sku = "BS-13",
            unitName = "Batang",
            stockQty = BigDecimal("30"),
            baseUnitId = "unit-btg"
        )
        val offlineUncachedCustomer = Customer(
            id = "cust-uncached",
            name = "Mandor Ali",
            phone = "08999",
            address = "Blok C",
            isContractor = false,
            creditLimit = BigDecimal.ZERO,
            paymentTermDays = 0,
            isActive = true,
            createdAt = "",
            updatedAt = ""
        )

        val vm1 = buildVm(debounceMs = 0L)
        advanceUntilIdle()
        vm1.selectCustomer(offlineUncachedCustomer)
        vm1.addProduct(offlineUncachedProduct)
        vm1.setItemDiscount("prod-uncached", CheckoutDiscount(DiscountType.FIXED_AMOUNT, BigDecimal("5000")))
        advanceUntilIdle()

        // Recreate VM while Room product/customer tables are empty AND inventory/customer repos return errors
        val vm2 = buildVm(
            inventoryRepo = FailingInventoryRepository(),
            customerRepo = FailingCustomerRepository(),
            debounceMs = 0L
        )
        advanceUntilIdle()

        val state2 = vm2.uiState.value
        assertEquals(1, state2.cartItems.size)
        val item = state2.cartItems.single()
        assertEquals("prod-uncached", item.productId)
        assertEquals("Besi Ulir 13mm", item.productName)
        assertEquals("Batang", item.unitName)
        assertEquals(0, BigDecimal("95000").compareTo(item.unitPrice))
        assertEquals(0, BigDecimal("5000").compareTo(item.discountRequest?.value))
        assertEquals("cust-uncached", state2.selectedCustomer?.id)
        assertEquals("Mandor Ali", state2.selectedCustomer?.name)
    }

    @Test
    fun onlineAndLocalCheckoutSuccess_clearsActiveCartDraftWhilePreservingHeldCarts() = runTest(dispatcher) {
        val semen = Product("prod-1", "Semen", BigDecimal("70000"), unitName = "Sak", stockQty = BigDecimal("50"), baseUnitId = "unit-sak")
        val paku = Product("prod-2", "Paku", BigDecimal("10000"), unitName = "Kg", stockQty = BigDecimal("50"), baseUnitId = "unit-kg")
        cacheProduct(semen)
        cacheProduct(paku)

        // 1. Online checkout clears active cart, keeps held cart
        val vmOnline = buildVm(debounceMs = 0L)
        advanceUntilIdle()
        vmOnline.addProduct(paku)
        vmOnline.holdCurrentCart("Parkir Tetap")
        vmOnline.addProduct(semen)
        advanceUntilIdle()

        vmOnline.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        val afterOnline = draftStore.load()
        assertNotNull(afterOnline)
        assertNull(afterOnline!!.activeCartJson)
        assertNotNull(afterOnline.heldCartsJson)

        val vmAfterOnline = buildVm(debounceMs = 0L)
        advanceUntilIdle()
        assertTrue(vmAfterOnline.uiState.value.cartItems.isEmpty())
        assertEquals(1, vmAfterOnline.uiState.value.heldCarts.size)
        assertEquals("Parkir Tetap", vmAfterOnline.uiState.value.heldCarts.single().note)

        // 2. Local offline checkout also clears active cart, keeps held cart
        val cashSessionLocalDs = CashSessionLocalDataSource(database.cashSessionDao())
        val appSettingsDs = LocalAppSettingsDataSource(database.appSettingDao())
        appSettingsDs.setDataMode(AppDataMode.OFFLINE_ONLY)
        val offlineStatusRepo = OfflineStatusRepository(
            networkMonitor = NetworkMonitor(context),
            backendHealthMonitor = BackendHealthMonitor(
                healthApi = object : HealthApi {
                    override suspend fun getReadiness(): Response<HealthResponseDto> = throw IOException("offline")
                },
                networkMonitor = NetworkMonitor(context)
            ),
            localAppSettingsDataSource = appSettingsDs,
            syncQueueDao = database.syncQueueDao()
        )
        val lookupService = LocalCheckoutLookupService(
            productLocalDataSource = productLocalDataSource,
            customerLocalDataSource = customerLocalDataSource,
            cashSessionDao = database.cashSessionDao()
        )
        val localCheckoutService = LocalCheckoutService(database, "android-test")
        val offlineCashRepo = object : FakeDraftCashRepository() {
            override suspend fun getActiveSession() = RepositoryResult.Error("NETWORK_TIMEOUT", "offline")
        }
        // Seed open local cash session
        database.cashSessionDao().upsert(
            com.tbterminal.app.data.local.entity.LocalCashSessionEntity(
                serverId = "srv-sess-off",
                clientGeneratedId = "cli-sess-off",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = 1_000_000L,
                startingCash = ScaledDecimal2.ofMinorUnits(100_000_00L),
                syncStatus = SyncStatus.SYNCED
            )
        )

        val vmOffline = CheckoutViewModel(
            checkoutRepository = FakeDraftCheckoutRepository(),
            inventoryRepository = FailingInventoryRepository(),
            customerRepository = FailingCustomerRepository(),
            cashReconciliationRepository = offlineCashRepo,
            cashSessionLocalDataSource = cashSessionLocalDs,
            localCheckoutLookupService = lookupService,
            localCheckoutService = localCheckoutService,
            offlineStatusRepository = offlineStatusRepo,
            localAppSettingsDataSource = appSettingsDs,
            checkoutDraftStore = draftStore,
            productLocalDataSource = productLocalDataSource,
            customerLocalDataSource = customerLocalDataSource,
            cartDraftSaveDebounceMs = 0L,
        )
        advanceUntilIdle()
        vmOffline.addProduct(semen)
        advanceUntilIdle()
        assertNotNull(draftStore.load()?.activeCartJson)

        vmOffline.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        val afterLocal = draftStore.load()
        assertNotNull(afterLocal)
        assertNull(afterLocal!!.activeCartJson)
        assertNotNull(afterLocal.heldCartsJson)
    }

    @Test
    fun rapidCartMutations_areDebouncedIntoSingleRoomWrite() = runTest(dispatcher) {
        val countingStore = CountingCheckoutDraftStore(draftStore)
        val p1 = Product("prod-1", "Cat", BigDecimal("50000"), unitName = "Pail", stockQty = BigDecimal("20"))
        val p2 = Product("prod-2", "Kuas", BigDecimal("10000"), unitName = "Pcs", stockQty = BigDecimal("20"))

        val vm = CheckoutViewModel(
            checkoutRepository = FakeDraftCheckoutRepository(),
            inventoryRepository = FailingInventoryRepository(),
            customerRepository = FailingCustomerRepository(),
            cashReconciliationRepository = FakeDraftCashRepository(),
            checkoutDraftStore = countingStore,
            productLocalDataSource = productLocalDataSource,
            customerLocalDataSource = customerLocalDataSource,
            cartDraftSaveDebounceMs = 400L,
        )
        runCurrent()
        val initialSaves = countingStore.saveCalls

        // Rapid burst of edits inside 400ms debounce window
        vm.addProduct(p1)
        vm.addProduct(p1)
        vm.addProduct(p2)
        vm.setItemDiscount("prod-1", CheckoutDiscount(DiscountType.FIXED_AMOUNT, BigDecimal("2000")))
        advanceTimeBy(300L)
        runCurrent()
        assertEquals(initialSaves, countingStore.saveCalls)

        advanceTimeBy(150L)
        runCurrent()
        assertEquals(initialSaves + 1, countingStore.saveCalls)
        assertNotNull(draftStore.load()?.activeCartJson)
    }

    @Test
    fun corruptDraftJsonOrUnknownSchemaVersion_startsCleanlyWithoutCrashing() = runTest(dispatcher) {
        database.checkoutDraftDao().upsert(
            CheckoutDraftEntity(
                ownerUserId = "kasir-1",
                activeCartJson = "{corrupt-active-json",
                heldCartsJson = "{corrupt-held-json",
                schemaVersion = 1,
                updatedAt = 1000L
            )
        )

        val vm1 = buildVm(debounceMs = 0L)
        advanceUntilIdle()
        assertTrue(vm1.uiState.value.cartItems.isEmpty())
        assertTrue(vm1.uiState.value.heldCarts.isEmpty())
        assertNull(database.checkoutDraftDao().get("kasir-1")?.activeCartJson)

        database.checkoutDraftDao().upsert(
            CheckoutDraftEntity(
                ownerUserId = "kasir-1",
                activeCartJson = """{"items":[]}""",
                heldCartsJson = null,
                schemaVersion = 999,
                updatedAt = 2000L
            )
        )

        val vm2 = buildVm(debounceMs = 0L)
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.cartItems.isEmpty())
        assertNull(database.checkoutDraftDao().get("kasir-1"))
    }

    private fun buildVm(
        inventoryRepo: InventoryRepository = FailingInventoryRepository(),
        customerRepo: CustomerRepository = FailingCustomerRepository(),
        debounceMs: Long = 0L
    ): CheckoutViewModel {
        return CheckoutViewModel(
            checkoutRepository = FakeDraftCheckoutRepository(),
            inventoryRepository = inventoryRepo,
            customerRepository = customerRepo,
            cashReconciliationRepository = FakeDraftCashRepository(),
            checkoutDraftStore = draftStore,
            productLocalDataSource = productLocalDataSource,
            customerLocalDataSource = customerLocalDataSource,
            cartDraftSaveDebounceMs = debounceMs,
        )
    }

    private suspend fun cacheProduct(product: Product, priceContractor: BigDecimal = BigDecimal.ZERO) {
        productLocalDataSource.cacheProducts(
            listOf(
                LocalProductEntity(
                    serverId = product.productId,
                    clientGeneratedId = "server-product-${product.productId}",
                    name = product.name,
                    sku = product.sku,
                    unitServerId = product.baseUnitId,
                    unitName = product.unitName,
                    secondaryUnitServerId = product.secondaryUnitId,
                    secondaryUnitName = product.secondaryUnitName,
                    secondaryUnitFactor = product.secondaryUnitFactor,
                    priceSell = ScaledDecimal2.ofExact(product.unitPrice),
                    priceBuy = ScaledDecimal2.ofExact(BigDecimal("10000")),
                    priceRetail = ScaledDecimal2.ofExact(product.unitPrice),
                    priceContractor = ScaledDecimal2.ofExact(priceContractor),
                    discount = ScaledDecimal2.ZERO,
                    stock = ScaledDecimal2.ofExact(product.stockQty),
                    minimumStock = ScaledDecimal2.ZERO,
                    isActive = true,
                    syncStatus = SyncStatus.SYNCED
                )
            )
        )
    }

    private suspend fun cacheCustomer(customer: Customer) {
        customerLocalDataSource.cacheCustomer(
            LocalCustomerEntity(
                serverId = customer.id,
                clientGeneratedId = "server-customer-${customer.id}",
                name = customer.name,
                phone = customer.phone,
                address = customer.address,
                isContractor = customer.isContractor,
                creditAllowed = true,
                creditLimit = ScaledDecimal2.ofExact(customer.creditLimit),
                paymentTermDays = customer.paymentTermDays,
                isActive = true,
                syncStatus = SyncStatus.SYNCED
            )
        )
    }

    private class CountingCheckoutDraftStore(
        private val delegate: CheckoutDraftStore
    ) : CheckoutDraftStore {
        var saveCalls = 0
        override suspend fun load(): CheckoutDraftRecord? = delegate.load()
        override suspend fun save(activeCartJson: String?, heldCartsJson: String?) {
            saveCalls++
            delegate.save(activeCartJson, heldCartsJson)
        }
        override suspend fun clearAll() = delegate.clearAll()
    }

    private class FakeDraftCheckoutRepository : CheckoutRepository {
        override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> {
            val total = command.items.fold(BigDecimal.ZERO) { acc, item ->
                acc.add(item.quantity.multiply(BigDecimal("70000")))
            }
            return RepositoryResult.Success(
                CheckoutPreview(
                    checkoutAttemptId = "att-1",
                    discountFingerprint = "fp",
                    items = command.items.map {
                        CheckoutPreviewItem(it.productId, it.quantity, BigDecimal("70000"), total, null, BigDecimal.ZERO, BigDecimal.ZERO, total)
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
            return RepositoryResult.Success(CheckoutReceipt("RCP-1", "tx-1", command.amountPaid, "0", false))
        }
    }

    private class FailingInventoryRepository : InventoryRepository {
        override suspend fun getProductStocks(page: Int, limit: Int, search: String?): RepositoryResult<ProductStockPage> =
            RepositoryResult.Exception(IOException("offline"))
        override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> =
            RepositoryResult.Exception(IOException("offline"))
        override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): RepositoryResult<StockAdjustmentPage> = error("unused")
        override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): RepositoryResult<StockCard> = error("unused")
        override suspend fun getProduct(id: String) = error("unused")
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

    private class FailingCustomerRepository : CustomerRepository {
        override suspend fun getCustomers(page: Int, limit: Int, search: String?, status: String?): RepositoryResult<CustomerPage> =
            RepositoryResult.Exception(IOException("offline"))
        override suspend fun getCustomerById(id: String): RepositoryResult<Customer> =
            RepositoryResult.Exception(IOException("offline"))
        override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> = error("unused")
        override suspend fun updateCustomer(id: String, command: CustomerCommand): RepositoryResult<Customer> = error("unused")
        override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun activateCustomer(id: String): RepositoryResult<Customer> = error("unused")
    }

    private open class FakeDraftCashRepository : CashReconciliationRepository {
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
