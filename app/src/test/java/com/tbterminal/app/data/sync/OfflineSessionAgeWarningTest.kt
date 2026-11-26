package com.tbterminal.app.data.sync

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.TransactionVoidResult
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import com.tbterminal.app.ui.cash.CashReconciliationViewModel
import com.tbterminal.app.ui.sync.SyncCenterViewModel
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockWebServer
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
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OfflineSessionAgeWarningTest {
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
    fun policyBoundaries_triggerWarningAt48HoursAndCriticalAt66Hours() {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR

        assertNull(
            OfflineSessionAgePolicy.evaluateOpenedAt(
                openedAtMillis = openedAt,
                nowMillis = openedAt + (48 * hourMs) - 1L
            )
        )

        val at48h = OfflineSessionAgePolicy.evaluateOpenedAt(
            openedAtMillis = openedAt,
            nowMillis = openedAt + (48 * hourMs)
        )
        assertNotNull(at48h)
        at48h!!
        assertEquals(OfflineSessionAgeSeverity.WARNING, at48h.severity)
        assertEquals(48L, at48h.ageHours)
        assertEquals(24L, at48h.remainingHours)
        assertFalse(at48h.isExpired)
        assertTrue(at48h.message.contains("Segera sambungkan internet agar transaksi bisa tersinkron"))

        val at65h59m = OfflineSessionAgePolicy.evaluateOpenedAt(
            openedAtMillis = openedAt,
            nowMillis = openedAt + (66 * hourMs) - 1L
        )!!
        assertEquals(OfflineSessionAgeSeverity.WARNING, at65h59m.severity)

        val at66h = OfflineSessionAgePolicy.evaluateOpenedAt(
            openedAtMillis = openedAt,
            nowMillis = openedAt + (66 * hourMs)
        )!!
        assertEquals(OfflineSessionAgeSeverity.CRITICAL, at66h.severity)
        assertEquals(66L, at66h.ageHours)
        assertEquals(6L, at66h.remainingHours)
        assertFalse(at66h.isExpired)
        assertTrue(at66h.message.contains("Segera sambungkan internet agar transaksi bisa tersinkron"))

        val at72h = OfflineSessionAgePolicy.evaluateOpenedAt(
            openedAtMillis = openedAt,
            nowMillis = openedAt + (72 * hourMs)
        )!!
        assertEquals(OfflineSessionAgeSeverity.CRITICAL, at72h.severity)
        assertTrue(at72h.isExpired)
        assertTrue(at72h.message.contains("Segera sambungkan internet agar transaksi bisa tersinkron"))
    }

    @Test
    fun policyEvaluateUnsyncedSessions_ignoresSyncedSessionsAndUsesOldestUnsynced() {
        val baseTime = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        val now = baseTime + (70 * hourMs)

        val sessions = listOf(
            // Synced / server-backed session (80h old) must be ignored
            LocalCashSessionEntity(
                localId = 1L,
                serverId = "srv-sess-old",
                clientGeneratedId = "sess-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = baseTime - (10 * hourMs),
                syncStatus = SyncStatus.SYNCED
            ),
            // Unsynced session 50h old
            LocalCashSessionEntity(
                localId = 2L,
                serverId = null,
                clientGeneratedId = "sess-2",
                cashierUserId = "kasir-1",
                status = "CLOSED",
                openedAt = baseTime + (20 * hourMs),
                syncStatus = SyncStatus.PENDING
            ),
            // Oldest unsynced session 67h old
            LocalCashSessionEntity(
                localId = 3L,
                serverId = null,
                clientGeneratedId = "sess-3",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = baseTime + (3 * hourMs),
                syncStatus = SyncStatus.FAILED
            )
        )

        val alert = OfflineSessionAgePolicy.evaluateUnsyncedSessions(sessions, nowMillis = now)
        assertNotNull(alert)
        alert!!
        assertEquals(OfflineSessionAgeSeverity.CRITICAL, alert.severity)
        assertEquals(67L, alert.ageHours)
        assertEquals(2, alert.affectedSessionCount)
    }

    @Test
    fun cashReconciliationViewModel_evaluatesSessionAgeUsingInjectedClock() = runTest(testDispatcher) {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        var fakeClock = openedAt + (47 * hourMs)

        database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-offline-age",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = openedAt,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.PENDING
            )
        )

        val vm = CashReconciliationViewModel(
            repository = OfflineThrowingCashRepository(),
            cashSessionLocalDataSource = CashSessionLocalDataSource(database.cashSessionDao()),
            cashierUserId = "kasir-1",
            actorRole = "KASIR",
            clockMillis = { fakeClock }
        )
        advanceUntilIdle()
        assertNull(vm.uiState.value.offlineSessionAgeAlert)

        fakeClock = openedAt + (48 * hourMs)
        vm.refresh()
        advanceUntilIdle()
        val warning = vm.uiState.value.offlineSessionAgeAlert
        assertNotNull(warning)
        assertEquals(OfflineSessionAgeSeverity.WARNING, warning!!.severity)
        assertTrue(warning.message.contains("Segera sambungkan internet agar transaksi bisa tersinkron"))

        fakeClock = openedAt + (66 * hourMs)
        vm.refresh()
        advanceUntilIdle()
        val critical = vm.uiState.value.offlineSessionAgeAlert
        assertNotNull(critical)
        assertEquals(OfflineSessionAgeSeverity.CRITICAL, critical!!.severity)
        assertTrue(critical.message.contains("Segera sambungkan internet agar transaksi bisa tersinkron"))
    }

    @Test
    fun cashReconciliationViewModel_clearsAlertWhenSessionAlreadyHasServerId() = runTest(testDispatcher) {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        val fakeClock = openedAt + (70 * hourMs)

        database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-synced",
                clientGeneratedId = "sess-mirrored",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = openedAt,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.SYNCED
            )
        )

        val vm = CashReconciliationViewModel(
            repository = OfflineThrowingCashRepository(),
            cashSessionLocalDataSource = CashSessionLocalDataSource(database.cashSessionDao()),
            cashierUserId = "kasir-1",
            actorRole = "KASIR",
            clockMillis = { fakeClock }
        )
        advanceUntilIdle()
        assertNull(vm.uiState.value.offlineSessionAgeAlert)
    }

    @Test
    fun syncCenterViewModel_evaluatesOldestUnsyncedOfflineSessionWithInjectedClock() = runTest(testDispatcher) {
        val openedAt = 1_700_000_000_000L
        val hourMs = OfflineSessionAgePolicy.MILLIS_PER_HOUR
        var fakeClock = openedAt + (47 * hourMs)

        database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-sync-age",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = openedAt,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.PENDING
            )
        )

        val sessionSync = OfflineCashSessionSyncService(database, salesApi, "android-dev-1")
        val checkoutSync = OfflineCheckoutSyncService(database, salesApi, "android-dev-1")
        val expenseSync = OfflineCashExpenseSyncService(database, salesApi, "android-dev-1", sessionSync)
        val repo = SyncMonitoringRepository(
            syncQueueDao = database.syncQueueDao(),
            offlineCashSessionSyncService = sessionSync,
            offlineCheckoutSyncService = checkoutSync,
            offlineCashExpenseSyncService = expenseSync,
            cashSessionDao = database.cashSessionDao(),
            clockMillis = { fakeClock }
        )
        val viewModelStore = ViewModelStore()
        try {
            val vm = ViewModelProvider(
                viewModelStore,
                SyncCenterViewModel.factory(repository = repo, canRetry = true)
            )[SyncCenterViewModel::class.java]
            advanceUntilIdle()
            assertNull(vm.uiState.value.offlineSessionAgeAlert)

            fakeClock = openedAt + (48 * hourMs)
            vm.refresh()
            advanceUntilIdle()
            assertEquals(OfflineSessionAgeSeverity.WARNING, vm.uiState.value.offlineSessionAgeAlert?.severity)

            fakeClock = openedAt + (66 * hourMs)
            vm.refresh()
            advanceUntilIdle()
            val alert = vm.uiState.value.offlineSessionAgeAlert
            assertEquals(OfflineSessionAgeSeverity.CRITICAL, alert?.severity)
            assertTrue(alert!!.message.contains("Segera sambungkan internet agar transaksi bisa tersinkron"))
        } finally {
            viewModelStore.clear()
            advanceUntilIdle()
        }
    }
}

private class OfflineThrowingCashRepository : CashReconciliationRepository {
    override suspend fun getActiveSession(): RepositoryResult<CashSession?> =
        RepositoryResult.Exception(IOException("offline"))
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
}
