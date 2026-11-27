package com.tbterminal.app.ui.checkout

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.checkout.LocalCheckoutLookupService
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.database.CustomerLocalDataSource
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.database.RoomCheckoutDraftStore
import com.tbterminal.app.data.local.database.RoomPendingSubmissionStore
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalProductEntity
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
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.NetworkConnectivity
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.ui.common.UiText
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
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
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CheckoutViewModelOfflineFallbackTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: TbTerminalDatabase
    private lateinit var productLocalDataSource: ProductLocalDataSource
    private lateinit var customerLocalDataSource: CustomerLocalDataSource
    private lateinit var cashSessionLocalDataSource: CashSessionLocalDataSource
    private lateinit var localCashSessionService: LocalCashSessionService
    private lateinit var pendingSubmissionStore: RoomPendingSubmissionStore
    private lateinit var checkoutDraftStore: RoomCheckoutDraftStore
    private lateinit var connectivity: FakeCheckoutConnectivity
    private lateinit var healthApi: FakeCheckoutHealthApi
    private lateinit var backendHealthMonitor: BackendHealthMonitor
    private lateinit var offlineStatusRepository: OfflineStatusRepository
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
        productLocalDataSource = ProductLocalDataSource(database.productDao())
        customerLocalDataSource = CustomerLocalDataSource(database.customerDao())
        cashSessionLocalDataSource = CashSessionLocalDataSource(database.cashSessionDao())
        localCashSessionService = LocalCashSessionService(database, "device-test", roleProvider = { "KASIR" })
        pendingSubmissionStore = RoomPendingSubmissionStore(
            dao = database.pendingSubmissionDao(),
            ownerUserIdProvider = { "kasir-1" },
            clockMillis = { nowMillis },
        )
        checkoutDraftStore = RoomCheckoutDraftStore(
            dao = database.checkoutDraftDao(),
            ownerUserIdProvider = { "kasir-1" },
            clockMillis = { nowMillis },
        )
        connectivity = FakeCheckoutConnectivity(true)
        healthApi = FakeCheckoutHealthApi(Response.success(HealthResponseDto(status = "ready", database = "up")))
        backendHealthMonitor = BackendHealthMonitor(
            healthApi = healthApi,
            networkMonitor = connectivity,
            throttleMillis = 30_000L,
            timeoutMillis = 1_000L,
            clockMillis = { nowMillis },
        )
        offlineStatusRepository = OfflineStatusRepository(
            networkMonitor = connectivity,
            backendHealthMonitor = backendHealthMonitor,
            localAppSettingsDataSource = LocalAppSettingsDataSource(database.appSettingDao()),
            syncQueueDao = database.syncQueueDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun scenarioB_previewTransportAndGateway503_fallBackToLocalCheckout_clearDraftAndMarkUnreachable() = runTest(dispatcher) {
        seedProductAndOpenSession(serverSessionId = "srv-sess-1")
        backendHealthMonitor.refresh(force = true)
        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)

        listOf("CONNECTION_FAILED", "NETWORK_TIMEOUT", "HTTP_503").forEachIndexed { idx, errorCode ->
            backendHealthMonitor.markConnected()
            val checkoutRepo = FakeOfflineFallbackCheckoutRepo().apply {
                previewResult = RepositoryResult.Error(errorCode, "Koneksi bermasalah")
            }
            val vm = buildVm(checkoutRepo = checkoutRepo)
            advanceUntilIdle()

            vm.addProduct(sampleProduct())
            advanceUntilIdle()
            assertNotNull(checkoutDraftStore.load()?.activeCartJson)

            vm.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
            advanceUntilIdle()

            assertEquals("preview called once for $errorCode", 1, checkoutRepo.previewCalls)
            assertEquals("submitCheckout must NOT be called", 0, checkoutRepo.submittedCommands.size)
            assertEquals(BackendStatus.UNREACHABLE, backendHealthMonitor.status.value)
            assertEquals(idx + 1, database.transactionDao().getPendingLocalIdsForAutoSync(SyncStatus.PENDING, 100).size)
            assertNull(checkoutDraftStore.load()?.activeCartJson)
            assertNull(pendingSubmissionStore.load(PendingSubmissionScopes.CHECKOUT))
            assertTrue(vm.uiState.value.cartItems.isEmpty())
        }
    }

    @Test
    fun scenarioB_previewConnectionFailedWithDiscount_blocksOfflineAndMarksUnreachable() = runTest(dispatcher) {
        seedProductAndOpenSession(serverSessionId = "srv-sess-1")
        backendHealthMonitor.refresh(force = true)

        val checkoutRepo = FakeOfflineFallbackCheckoutRepo().apply {
            previewResult = RepositoryResult.Error("CONNECTION_FAILED", "Koneksi bermasalah")
        }
        val vm = buildVm(checkoutRepo = checkoutRepo)
        advanceUntilIdle()

        vm.addProduct(sampleProduct())
        vm.setTransactionDiscount(CheckoutDiscount(DiscountType.FIXED_AMOUNT, BigDecimal("5000")))
        advanceUntilIdle()

        vm.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("65000"))
        advanceUntilIdle()

        assertEquals(BackendStatus.UNREACHABLE, backendHealthMonitor.status.value)
        assertEquals(0, database.transactionDao().getPendingLocalIdsForAutoSync(SyncStatus.PENDING, 100).size)
        val err = vm.uiState.value.errorEvent as? UiText.DynamicString
        assertEquals("Diskon memerlukan koneksi ke server dan tidak tersedia saat offline.", err?.value)
    }

    @Test
    fun scenarioB_previewHttp500And400And401_stayConnectedAndNeverFallBackLocally() = runTest(dispatcher) {
        seedProductAndOpenSession(serverSessionId = "srv-sess-1")
        backendHealthMonitor.refresh(force = true)

        listOf("HTTP_500", "HTTP_400", "HTTP_401").forEach { code ->
            backendHealthMonitor.markConnected()
            val checkoutRepo = FakeOfflineFallbackCheckoutRepo().apply {
                previewResult = RepositoryResult.Error(code, "Error $code")
            }
            val vm = buildVm(checkoutRepo = checkoutRepo)
            advanceUntilIdle()

            vm.addProduct(sampleProduct())
            advanceUntilIdle()
            vm.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
            advanceUntilIdle()

            assertEquals("HTTP $code must keep CONNECTED", BackendStatus.CONNECTED, backendHealthMonitor.status.value)
            assertEquals("HTTP $code must not create local tx", 0, database.transactionDao().getPendingLocalIdsForAutoSync(SyncStatus.PENDING, 100).size)
            assertEquals(1, vm.uiState.value.cartItems.size)
        }
    }

    @Test
    fun staleConnectedHealth_refreshesReadinessAndSkipsPreviewWhenServerDown() = runTest(dispatcher) {
        seedProductAndOpenSession(serverSessionId = "srv-sess-1")
        backendHealthMonitor.refresh(force = true)
        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)

        // Advance clock past 30s throttle and make /api/readiness return 503
        nowMillis += 31_000L
        healthApi.nextResponse = Response.error(503, "down".toResponseBody())

        val checkoutRepo = FakeOfflineFallbackCheckoutRepo()
        val vm = buildVm(checkoutRepo = checkoutRepo)
        advanceUntilIdle()

        vm.addProduct(sampleProduct())
        advanceUntilIdle()
        vm.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        assertEquals(0, checkoutRepo.previewCalls)
        assertEquals(BackendStatus.UNREACHABLE, backendHealthMonitor.status.value)
        assertEquals(1, database.transactionDao().getPendingLocalIdsForAutoSync(SyncStatus.PENDING, 100).size)
    }

    @Test
    fun scenarioC_midOnlineSessionServerDrops_restoresMirroredSessionAndAttachesOfflineTxToIt() = runTest(dispatcher) {
        seedProductAndOpenSession(serverSessionId = "srv-online-session-uuid")
        backendHealthMonitor.refresh(force = true)

        val cashRepo = FakeOfflineFallbackCashRepo().apply {
            activeSessionResult = RepositoryResult.Error("CONNECTION_FAILED", "Koneksi bermasalah")
        }
        val checkoutRepo = FakeOfflineFallbackCheckoutRepo().apply {
            previewResult = RepositoryResult.Error("CONNECTION_FAILED", "Koneksi bermasalah")
        }
        val vm = buildVm(checkoutRepo = checkoutRepo, cashRepo = cashRepo)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasActiveCashSession)
        assertEquals("srv-online-session-uuid", vm.uiState.value.activeCashSession?.serverId)
        assertEquals(1, database.cashSessionDao().observeSessionsCountSnapshot())

        vm.addProduct(sampleProduct())
        advanceUntilIdle()
        vm.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        val localIds = database.transactionDao().getPendingLocalIdsForAutoSync(SyncStatus.PENDING, 100)
        assertEquals(1, localIds.size)
        val savedTx = database.transactionDao().getByLocalId(localIds.single())!!
        assertEquals("srv-online-session-uuid", savedTx.cashSessionServerId)
        assertEquals(1, database.cashSessionDao().observeSessionsCountSnapshot())
    }

    @Test
    fun ambiguousOnlineSubmit_neverFallsBackToOffline_evenAfterRestartAndWhileUnreachable() = runTest(dispatcher) {
        seedProductAndOpenSession(serverSessionId = "srv-sess-1")
        backendHealthMonitor.refresh(force = true)

        val checkoutRepo = FakeOfflineFallbackCheckoutRepo().apply {
            submitResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout saat submit")
        }
        val vm1 = buildVm(checkoutRepo = checkoutRepo)
        advanceUntilIdle()

        vm1.addProduct(sampleProduct())
        advanceUntilIdle()
        vm1.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        assertEquals(1, checkoutRepo.submittedCommands.size)
        val firstKey = checkoutRepo.submittedCommands.single().idempotencyKey
        assertTrue(vm1.uiState.value.hasAmbiguousCheckout)
        assertEquals(PendingSubmissionStatus.AMBIGUOUS, pendingSubmissionStore.load(PendingSubmissionScopes.CHECKOUT)?.status)
        assertEquals(0, database.transactionDao().getPendingLocalIdsForAutoSync(SyncStatus.PENDING, 100).size)

        // Simulate process death + server UNREACHABLE upon restart
        backendHealthMonitor.markUnreachable()
        checkoutRepo.submitResult = RepositoryResult.Success(
            CheckoutReceipt(
                receiptId = "TRX-999",
                transactionId = "tx-999",
                amountTendered = "70000",
                changeAmount = "0",
                idempotentReplay = true,
            )
        )
        val vm2 = buildVm(checkoutRepo = checkoutRepo)
        advanceUntilIdle()

        assertTrue(vm2.uiState.value.hasAmbiguousCheckout)
        vm2.prepareCheckout(PaymentMethod.TUNAI, BigDecimal("70000"))
        advanceUntilIdle()

        assertEquals(2, checkoutRepo.submittedCommands.size)
        assertEquals(firstKey, checkoutRepo.submittedCommands[1].idempotencyKey)
        assertEquals(0, database.transactionDao().getPendingLocalIdsForAutoSync(SyncStatus.PENDING, 100).size)
        assertNull(pendingSubmissionStore.load(PendingSubmissionScopes.CHECKOUT))
    }

    @Test
    fun openCashSession_whenOffline_reusesExistingOpenSessionOrOpensSingleLocalSessionForKasirOnly() = runTest(dispatcher) {
        backendHealthMonitor.refresh(force = true)
        val cashRepo = FakeOfflineFallbackCashRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
            openSessionResult = RepositoryResult.Error("CONNECTION_FAILED", "Koneksi bermasalah")
        }

        // 1. No existing open session, KASIR -> creates 1 local OPEN session
        val vmKasir = buildVm(cashRepo = cashRepo, actorRole = "KASIR")
        advanceUntilIdle()
        assertFalse(vmKasir.uiState.value.hasActiveCashSession)
        vmKasir.onStartingCashChanged("150000")
        vmKasir.openCashSession()
        advanceUntilIdle()

        assertTrue(vmKasir.uiState.value.hasActiveCashSession)
        val existingOpen = cashSessionLocalDataSource.getLatestOpenSession("kasir-1")
        assertNotNull(existingOpen)
        assertEquals(1, database.cashSessionDao().observeSessionsCountSnapshot())

        // 2. If another VM tries openCashSession while OPEN session exists -> reuses it, NO duplicate local session
        val vmSecond = buildVm(cashRepo = cashRepo, actorRole = "KASIR")
        advanceUntilIdle()
        vmSecond.onStartingCashChanged("200000")
        vmSecond.openCashSession()
        advanceUntilIdle()

        assertTrue(vmSecond.uiState.value.hasActiveCashSession)
        assertEquals(existingOpen!!.localId, vmSecond.uiState.value.activeCashSession?.localId)
        assertEquals(1, database.cashSessionDao().observeSessionsCountSnapshot())

        // 3. Non-KASIR is rejected
        val vmAdmin = buildVm(cashRepo = cashRepo, actorRole = "ADMIN")
        advanceUntilIdle()
        vmAdmin.onStartingCashChanged("100000")
        vmAdmin.openCashSession()
        advanceUntilIdle()
        val err = vmAdmin.uiState.value.cashSessionError as? UiText.DynamicString
        assertEquals("Hanya kasir yang dapat membuka sesi kas.", err?.value)
    }

    @Test
    fun loadCashSession_reconcilesClosedRemoteAndProtectsUnsyncedClosedMirror() = runTest(dispatcher) {
        val cashRepo = FakeOfflineFallbackCashRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
        }
        seedProductAndOpenSession(serverSessionId = "sess-mirrored-1")

        val vm = buildVm(cashRepo = cashRepo)
        advanceUntilIdle()

        val closedMirror = database.cashSessionDao().getByServerId("sess-mirrored-1")!!
        assertEquals("CLOSED", closedMirror.status)
        assertEquals(SyncStatus.SYNCED, closedMirror.syncStatus)
        assertFalse(vm.uiState.value.hasActiveCashSession)

        val reopenedMirror = cashSessionLocalDataSource.mirrorRemoteSession(
            CashSession(
                id = "sess-mirrored-2",
                userId = "kasir-1",
                openedAt = "2026-04-08T02:00:00Z",
                closedAt = null,
                openingCash = BigDecimal("100000"),
                closingCash = null,
                systemCash = null,
                difference = null,
                totalExpenses = BigDecimal.ZERO,
                notes = null,
                status = "OPEN",
            )
        )
        database.cashSessionDao().closeSession(
            localId = reopenedMirror.localId,
            status = "CLOSED",
            closedAt = nowMillis + 500L,
            expectedCash = null,
            actualCash = com.tbterminal.app.data.local.model.ScaledDecimal2.ofExact(BigDecimal("100000")),
            difference = null,
            closingNote = "tutup offline",
            syncStatus = SyncStatus.PENDING,
            updatedAt = nowMillis + 500L,
        )
        cashRepo.activeSessionResult = RepositoryResult.Success(
            CashSession(
                id = "sess-mirrored-2",
                userId = "kasir-1",
                openedAt = "2026-04-08T02:00:00Z",
                closedAt = null,
                openingCash = BigDecimal("100000"),
                closingCash = null,
                systemCash = null,
                difference = null,
                totalExpenses = BigDecimal.ZERO,
                notes = null,
                status = "OPEN",
            )
        )

        vm.loadCashSession()
        advanceUntilIdle()

        val protectedRow = database.cashSessionDao().getByServerId("sess-mirrored-2")!!
        assertEquals("CLOSED", protectedRow.status)
        assertEquals(SyncStatus.PENDING, protectedRow.syncStatus)
        assertFalse(vm.uiState.value.hasActiveCashSession)
    }

    private suspend fun seedProductAndOpenSession(serverSessionId: String) {
        database.productDao().upsert(
            LocalProductEntity(
                serverId = "prod-1",
                clientGeneratedId = "PROD-1",
                name = "Semen Tiga Roda",
                sku = "SMN-01",
                unitServerId = "unit-sak",
                unitName = "Sak",
                priceSell = ScaledDecimal2.ofExact(BigDecimal("70000")),
                priceBuy = ScaledDecimal2.ofExact(BigDecimal("60000")),
                priceRetail = ScaledDecimal2.ofExact(BigDecimal("70000")),
                priceContractor = ScaledDecimal2.ofExact(BigDecimal("68000")),
                stock = ScaledDecimal2.ofExact(BigDecimal("50")),
                minimumStock = ScaledDecimal2.ofExact(BigDecimal("5")),
                syncStatus = SyncStatus.SYNCED,
            )
        )
        database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = serverSessionId,
                clientGeneratedId = "SESSION-$serverSessionId",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = nowMillis - 60_000L,
                startingCash = ScaledDecimal2.ofExact(BigDecimal("100000")),
                syncStatus = SyncStatus.SYNCED,
            )
        )
    }

    private fun sampleProduct() = Product(
        productId = "prod-1",
        name = "Semen Tiga Roda",
        unitPrice = BigDecimal("70000"),
        unitName = "Sak",
        stockQty = BigDecimal("50"),
        baseUnitId = "unit-sak",
    )

    private fun buildVm(
        checkoutRepo: CheckoutRepository = FakeOfflineFallbackCheckoutRepo(),
        cashRepo: CashReconciliationRepository = FakeOfflineFallbackCashRepo(),
        actorRole: String = "KASIR",
    ): CheckoutViewModel {
        val lookup = LocalCheckoutLookupService(
            productLocalDataSource = productLocalDataSource,
            customerLocalDataSource = customerLocalDataSource,
            cashSessionDao = database.cashSessionDao(),
        )
        return CheckoutViewModel(
            checkoutRepository = checkoutRepo,
            inventoryRepository = FakeEmptyInventoryRepo(),
            customerRepository = FakeEmptyCustomerRepo(),
            cashReconciliationRepository = cashRepo,
            cashSessionLocalDataSource = cashSessionLocalDataSource,
            localCheckoutLookupService = lookup,
            localCheckoutService = LocalCheckoutService(database, "device-test"),
            offlineStatusRepository = offlineStatusRepository,
            actorRole = actorRole,
            clockMillis = { nowMillis },
            pendingSubmissionStore = pendingSubmissionStore,
            checkoutDraftStore = checkoutDraftStore,
            productLocalDataSource = productLocalDataSource,
            customerLocalDataSource = customerLocalDataSource,
            cartDraftSaveDebounceMs = 0L,
            localCashSessionService = localCashSessionService,
            cashierUserId = "kasir-1",
        )
    }
}

private suspend fun com.tbterminal.app.data.local.dao.CashSessionDao.observeSessionsCountSnapshot(): Int {
    return getUnsyncedOfflineSessions().size +
        listOfNotNull(getLatestByStatus("OPEN")?.takeIf { it.serverId != null }).size
}

private class FakeCheckoutConnectivity(initial: Boolean) : NetworkConnectivity {
    private val flow = MutableStateFlow(initial)
    override fun isOnline(): Boolean = flow.value
    override fun observeOnline(): Flow<Boolean> = flow
}

private class FakeCheckoutHealthApi(
    var nextResponse: Response<HealthResponseDto>,
) : HealthApi {
    override suspend fun getReadiness(): Response<HealthResponseDto> = nextResponse
}

private class FakeOfflineFallbackCheckoutRepo : CheckoutRepository {
    var previewCalls = 0
    val submittedCommands = mutableListOf<CheckoutSubmitCommand>()
    var previewResult: RepositoryResult<CheckoutPreview>? = null
    var submitResult: RepositoryResult<CheckoutReceipt> = RepositoryResult.Success(
        CheckoutReceipt(
            receiptId = "TRX-1",
            transactionId = "tx-1",
            amountTendered = "70000",
            changeAmount = "0",
            idempotentReplay = false,
        )
    )

    override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> {
        previewCalls += 1
        previewResult?.let { return it }
        val subtotal = command.items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(BigDecimal("70000").multiply(item.quantity))
        }
        return RepositoryResult.Success(
            CheckoutPreview(
                checkoutAttemptId = "attempt-1",
                discountFingerprint = "fp-1",
                items = command.items.map { item ->
                    CheckoutPreviewItem(
                        productId = item.productId,
                        quantity = item.quantity,
                        unitPrice = BigDecimal("70000"),
                        grossLineTotal = BigDecimal("70000").multiply(item.quantity),
                        discountType = null,
                        discountValue = BigDecimal.ZERO,
                        discountAmount = BigDecimal.ZERO,
                        netLineTotal = BigDecimal("70000").multiply(item.quantity),
                    )
                },
                grossSubtotal = subtotal,
                itemDiscountTotal = BigDecimal.ZERO,
                transactionDiscountAmount = BigDecimal.ZERO,
                totalDiscountAmount = BigDecimal.ZERO,
                effectiveDiscountPercent = BigDecimal.ZERO,
                netTotal = subtotal,
                cashierDiscountLimitPercent = BigDecimal("10"),
                approvalRequired = false,
                expiresAt = "2099-01-01T00:00:00Z",
            )
        )
    }

    override suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt> {
        submittedCommands += command
        return submitResult
    }
}

private class FakeOfflineFallbackCashRepo : CashReconciliationRepository {
    var activeSessionResult: RepositoryResult<CashSession?> = RepositoryResult.Success(
        CashSession(
            id = "srv-sess-1",
            userId = "kasir-1",
            userName = "Kasir Satu",
            openedAt = "2026-11-27T08:00:00+07:00",
            closedAt = null,
            openingCash = BigDecimal("100000"),
            closingCash = null,
            systemCash = BigDecimal("100000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
        )
    )
    var openSessionResult: RepositoryResult<CashSession> = RepositoryResult.Error("CONNECTION_FAILED", "Koneksi bermasalah")

    override suspend fun getActiveSession(): RepositoryResult<CashSession?> = activeSessionResult
    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = openSessionResult
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
}

private class FakeEmptyInventoryRepo : InventoryRepository {
    override suspend fun getProductStocks(page: Int, limit: Int, search: String?): RepositoryResult<ProductStockPage> =
        RepositoryResult.Success(ProductStockPage(emptyList(), 0L, page, limit, 1))
    override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): RepositoryResult<StockAdjustmentPage> = error("unused")
    override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): RepositoryResult<StockCard> = error("unused")
    override suspend fun getProduct(id: String) = error("unused")
    override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> = error("unused")
    override suspend fun getProductsPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductPage> = error("unused")
    override suspend fun createProduct(command: CreateProductCommand) = error("unused")
    override suspend fun updateProduct(id: String, command: UpdateProductCommand) = error("unused")
    override suspend fun deleteProduct(id: String) = error("unused")
    override suspend fun activateProduct(id: String) = error("unused")
    override suspend fun executeStockOpname(productId: String, adjustmentType: String, actualQty: BigDecimal, notes: String?) = error("unused")
    override suspend fun previewProductCsv(csv: String): RepositoryResult<ProductCsvPreview> = error("unused")
    override suspend fun commitProductCsv(csv: String): RepositoryResult<ProductCsvImportResult> = error("unused")
    override suspend fun createOpeningStock(productId: String, date: String, quantity: BigDecimal, note: String) = error("unused")
    override suspend fun getCategories(): RepositoryResult<List<ProductCategory>> = error("unused")
    override suspend fun getCategoryPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductCategoryPage> = error("unused")
    override suspend fun createCategory(name: String) = error("unused")
    override suspend fun updateCategory(id: String, name: String) = error("unused")
    override suspend fun deleteCategory(id: String) = error("unused")
    override suspend fun getUnits(): RepositoryResult<List<ProductUnit>> = error("unused")
    override suspend fun getUnitPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductUnitPage> = error("unused")
    override suspend fun createUnit(name: String, symbol: String) = error("unused")
    override suspend fun updateUnit(id: String, name: String, symbol: String) = error("unused")
    override suspend fun deleteUnit(id: String) = error("unused")
}

private class FakeEmptyCustomerRepo : CustomerRepository {
    override suspend fun getCustomers(page: Int, limit: Int, search: String?, status: String?): RepositoryResult<CustomerPage> =
        RepositoryResult.Success(CustomerPage(emptyList(), 0L, page, limit, 1))
    override suspend fun getCustomerById(id: String): RepositoryResult<Customer> = error("unused")
    override suspend fun createCustomer(command: CustomerCommand) = error("unused")
    override suspend fun updateCustomer(id: String, command: CustomerCommand) = error("unused")
    override suspend fun deactivateCustomer(id: String) = error("unused")
    override suspend fun activateCustomer(id: String) = error("unused")
}
