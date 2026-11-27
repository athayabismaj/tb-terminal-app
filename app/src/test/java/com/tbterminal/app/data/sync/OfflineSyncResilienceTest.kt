package com.tbterminal.app.data.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalPaymentEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionItemEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import com.tbterminal.app.data.remote.OfflineCashExpenseSyncRequestDto
import com.tbterminal.app.data.remote.OfflineCashExpenseSyncResponseDto
import com.tbterminal.app.data.remote.OfflineCashSessionCloseSyncRequestDto
import com.tbterminal.app.data.remote.OfflineCashSessionCloseSyncResponseDto
import com.tbterminal.app.data.remote.OfflineCashSessionOpenSyncRequestDto
import com.tbterminal.app.data.remote.OfflineCashSessionOpenSyncResponseDto
import com.tbterminal.app.data.remote.OfflineCheckoutSyncRequestDto
import com.tbterminal.app.data.remote.OfflineCheckoutSyncResponseDto
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import java.io.IOException
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OfflineSyncResilienceTest {
    private lateinit var context: Context
    private lateinit var database: TbTerminalDatabase
    private lateinit var server: MockWebServer
    private lateinit var salesApi: SalesApi
    private lateinit var settings: LocalAppSettingsDataSource
    private lateinit var sessionManager: SessionManager
    private lateinit var healthMonitor: BackendHealthMonitor
    private lateinit var cashSessionSync: OfflineCashSessionSyncService
    private lateinit var cashExpenseSync: OfflineCashExpenseSyncService
    private lateinit var checkoutSync: OfflineCheckoutSyncService
    private lateinit var syncManager: SyncManager

    private var fakeClockMillis = 1_795_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
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

        settings = LocalAppSettingsDataSource(database.appSettingDao())
        runBlocking { settings.setDataMode(AppDataMode.SYNC_SERVER) }

        val tokenStore = FakeTokenStore(accessToken = "token-valid", refreshToken = "refresh-valid")
        sessionManager = SessionManager(tokenStore)
        healthMonitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> =
                    Response.success(HealthResponseDto(status = "ready", database = "up"))
            },
            networkMonitor = AlwaysOnlineConnectivity,
            throttleMillis = 0L,
            timeoutMillis = 1_000L,
        )

        cashSessionSync = OfflineCashSessionSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = "android-dev-1",
            clockMillis = { fakeClockMillis },
            roleProvider = { sessionManager.readSessionUser()?.role },
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
        )
        cashExpenseSync = OfflineCashExpenseSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = "android-dev-1",
            cashSessionSyncService = cashSessionSync,
            clockMillis = { fakeClockMillis },
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
        )
        checkoutSync = OfflineCheckoutSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = "android-dev-1",
            cashSessionSyncService = cashSessionSync,
            cashExpenseSyncService = cashExpenseSync,
            clockMillis = { fakeClockMillis },
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
        )
        syncManager = SyncManager(
            database = database,
            networkMonitor = AlwaysOnlineConnectivity,
            backendHealthMonitor = healthMonitor,
            localAppSettingsDataSource = settings,
            sessionManager = sessionManager,
            offlineCashSessionSyncService = cashSessionSync,
            offlineCheckoutSyncService = checkoutSync,
            offlineCashExpenseSyncService = cashExpenseSync,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun transientFailuresOnSessionExpenseAndCheckoutStayPendingPastRetryCount3AndSyncOnRetry() = runBlocking {
        // 1. Offline open session: 4 consecutive 503 transient failures (> old limit of 3) stay PENDING and then sync
        val openSessionId = insertOpenSession("sess-open-retry", openedAt = fakeClockMillis - 4_000_000L)
        val openQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            openSessionId,
            SyncEntityType.CASH_SESSION,
            SyncOperation.CREATE,
        )!!
        repeat(4) {
            server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Service down"}"""))
            val result = syncManager.runOnce()
            assertTrue("Expected SyncResult.Retry on transient open failure", result is SyncResult.Retry)
        }
        assertEquals(SyncStatus.PENDING, database.cashSessionDao().getByLocalId(openSessionId)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.syncQueueDao().getById(openQueue.id)!!.status)
        assertEquals(4, database.syncQueueDao().getById(openQueue.id)!!.retryCount)

        enqueueOpenSessionSuccess("srv-sess-open-retry")
        val openSuccess = syncManager.runOnce()
        assertTrue(openSuccess is SyncResult.Success)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(openSessionId)!!.syncStatus)

        // 2. Offline checkout on synced session: 4 consecutive 503 transient failures stay PENDING and then sync
        val expenseSessionId = insertSyncedOpenSession("srv-sess-exp")
        val txId = insertCheckout(expenseSessionId, "tx-retry")
        val txQueue = database.syncQueueDao().getLatestForEntity(txId, SyncEntityType.TRANSACTION)!!
        repeat(4) {
            server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Service down"}"""))
            val result = syncManager.runOnce()
            assertTrue("Expected SyncResult.Retry on transient checkout failure", result is SyncResult.Retry)
        }
        assertEquals(SyncStatus.PENDING, database.transactionDao().getByLocalId(txId)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.syncQueueDao().getById(txQueue.id)!!.status)
        assertEquals(4, database.syncQueueDao().getById(txQueue.id)!!.retryCount)

        enqueueCheckoutSuccess("srv-tx-retry")
        val txSuccess = syncManager.runOnce()
        assertTrue(txSuccess is SyncResult.Success)
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(txId)!!.syncStatus)

        // 3. Offline expense on synced session: 4 consecutive 503 transient failures stay PENDING and then sync
        val expenseId = insertExpense(expenseSessionId, "exp-retry")
        val expQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            expenseId,
            SyncEntityType.CASH_EXPENSE,
            SyncOperation.CREATE,
        )!!
        repeat(4) {
            server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Service down"}"""))
            val result = syncManager.runOnce()
            assertTrue("Expected SyncResult.Retry on transient expense failure", result is SyncResult.Retry)
        }
        assertEquals(SyncStatus.PENDING, database.cashExpenseDao().getByLocalId(expenseId)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.syncQueueDao().getById(expQueue.id)!!.status)
        assertEquals(4, database.syncQueueDao().getById(expQueue.id)!!.retryCount)

        enqueueExpenseSuccess("srv-exp-retry")
        val expSuccess = syncManager.runOnce()
        assertTrue(expSuccess is SyncResult.Success)
        assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(expenseId)!!.syncStatus)
    }

    @Test
    fun permanentValidation400MarksFailedWhile429WithRetryAfterStaysPendingAndPropagatesDelay() = runBlocking {
        val badSessionId = insertOpenSession("sess-bad-400", openedAt = fakeClockMillis - 4_000_000L)
        val rateLimitedSessionId = insertSyncedOpenSession("srv-sess-rl")
        val rateLimitedTxId = insertCheckout(rateLimitedSessionId, "tx-429")

        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"code":"VALIDATION_ERROR","message":"Device ID tidak valid"}""")
        )
        server.enqueue(
            MockResponse().setResponseCode(429)
                .addHeader("Retry-After", "45")
                .setBody("""{"code":"RATE_LIMITED","message":"Terlalu banyak permintaan"}""")
        )

        val result = syncManager.runOnce()
        assertTrue(result is SyncResult.Retry)
        assertEquals(45L, (result as SyncResult.Retry).retryAfterSeconds)

        // 400 validation is permanent -> FAILED
        assertEquals(SyncStatus.FAILED, database.cashSessionDao().getByLocalId(badSessionId)!!.syncStatus)
        val badQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            badSessionId,
            SyncEntityType.CASH_SESSION,
            SyncOperation.CREATE,
        )!!
        assertEquals(SyncStatus.FAILED, badQueue.status)

        // 429 rate limit is transient -> stays PENDING
        assertEquals(SyncStatus.PENDING, database.transactionDao().getByLocalId(rateLimitedTxId)!!.syncStatus)
        val rlQueue = database.syncQueueDao().getLatestForEntity(rateLimitedTxId, SyncEntityType.TRANSACTION)!!
        assertEquals(SyncStatus.PENDING, rlQueue.status)
    }

    @Test
    fun recoverStaleSyncingRestoresExpiredSyncingSessionExpenseAndCheckoutToPendingWithoutTouchingFresh() = runBlocking {
        val staleTimestamp = fakeClockMillis - (11 * 60 * 1000L)
        val freshTimestamp = fakeClockMillis - (2 * 60 * 1000L)

        val staleSessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-stale",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.SYNCING,
                updatedAt = staleTimestamp,
            )
        )
        val staleSessQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = staleSessionId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.SYNCING,
                updatedAt = staleTimestamp,
            )
        )

        val freshSessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-fresh",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.SYNCING,
                updatedAt = freshTimestamp,
            )
        )

        val staleExpenseId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "exp-stale",
                deviceId = "android-dev-1",
                cashSessionLocalId = freshSessionId,
                cashSessionServerId = "srv-sess",
                description = "Plastik",
                amount = ScaledDecimal2.ofWhole(10_000L),
                syncStatus = SyncStatus.SYNCING,
                updatedAt = staleTimestamp,
            )
        )
        val staleExpQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = staleExpenseId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.SYNCING,
                updatedAt = staleTimestamp,
            )
        )

        val staleTxId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = "tx-stale",
                deviceId = "android-dev-1",
                transactionCode = "TRX-STALE",
                cashSessionLocalId = freshSessionId,
                cashSessionServerId = "srv-sess",
                cashierUserId = "kasir-1",
                status = "PAID",
                subtotal = ScaledDecimal2.ofWhole(10_000L),
                total = ScaledDecimal2.ofWhole(10_000L),
                paidAmount = ScaledDecimal2.ofWhole(10_000L),
                remainingAmount = ScaledDecimal2.ZERO,
                syncStatus = SyncStatus.SYNCING,
                updatedAt = staleTimestamp,
            )
        )
        val staleTxQueueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = staleTxId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.SYNCING,
                updatedAt = staleTimestamp,
            )
        )

        cashSessionSync.recoverStaleSyncing(fakeClockMillis)
        cashExpenseSync.recoverStaleSyncing(fakeClockMillis)
        checkoutSync.recoverStaleSyncing(fakeClockMillis)

        assertEquals(SyncStatus.PENDING, database.cashSessionDao().getByLocalId(staleSessionId)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.syncQueueDao().getById(staleSessQueueId)!!.status)
        assertEquals(SyncStatus.SYNCING, database.cashSessionDao().getByLocalId(freshSessionId)!!.syncStatus)

        assertEquals(SyncStatus.PENDING, database.cashExpenseDao().getByLocalId(staleExpenseId)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.syncQueueDao().getById(staleExpQueueId)!!.status)

        assertEquals(SyncStatus.PENDING, database.transactionDao().getByLocalId(staleTxId)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.syncQueueDao().getById(staleTxQueueId)!!.status)
    }

    @Test
    fun queueOver10ItemsDrainsCompletelyInStrictOrderOpenThenTransactionsAndExpensesThenClose() = runBlocking {
        // 1 closed session (not yet synced open OR close), 15 transactions, 2 expenses
        val sessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = null,
                clientGeneratedId = "sess-drain-1",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "CLOSED",
                openedAt = fakeClockMillis - 3_600_000L,
                closedAt = fakeClockMillis - 60_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                expectedCash = ScaledDecimal2.ofWhole(200_000L),
                actualCash = ScaledDecimal2.ofWhole(200_000L),
                difference = ScaledDecimal2.ZERO,
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sessionId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sessionId,
                operation = SyncOperation.UPDATE,
                payloadJson = "{}",
            )
        )

        val txIds = (1..15).map { idx ->
            insertCheckout(sessionId, "tx-drain-$idx", occurredAt = fakeClockMillis - 3_000_000L + idx * 1000L)
        }
        val expIds = (1..2).map { idx ->
            insertExpense(sessionId, "exp-drain-$idx", occurredAt = fakeClockMillis - 2_000_000L + idx * 1000L)
        }

        var txCounter = 0
        var expCounter = 0
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/sales/sessions/sync/open" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-sess-drain-1","openedAt":"2026-11-24T08:00:00+07:00","syncedAt":"2026-11-24T08:00:05+07:00"}}"""
                    )
                    "/api/sales/checkout/sync" -> {
                        val id = ++txCounter
                        MockResponse().setResponseCode(200).setBody(
                            """{"success":true,"data":{"syncStatus":"CREATED","serverTransactionId":"srv-tx-$id","receiptId":"RCP-srv-tx-$id","serverPaymentIds":["pay-1"],"syncedAt":"2026-11-24T10:20:00+07:00"}}"""
                        )
                    }
                    "/api/sales/cash-expenses/sync" -> {
                        val id = ++expCounter
                        MockResponse().setResponseCode(200).setBody(
                            """{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"srv-exp-$id","syncedAt":"2026-11-24T10:15:00+07:00"}}"""
                        )
                    }
                    "/api/sales/sessions/sync/close" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"UPDATED","serverCashSessionId":"srv-sess-drain-1","closedAt":"2026-11-24T17:00:00+07:00","syncedAt":"2026-11-24T17:00:05+07:00"}}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val result = syncManager.runOnce()
        assertTrue(result is SyncResult.Success)
        result as SyncResult.Success
        assertFalse(result.hasMorePending)

        val paths = mutableListOf<String>()
        while (server.requestCount > paths.size) {
            paths += server.takeRequest().path!!
        }
        assertEquals("/api/sales/sessions/sync/open", paths.first())
        assertEquals("/api/sales/sessions/sync/close", paths.last())
        assertEquals(1, paths.count { it == "/api/sales/sessions/sync/open" })
        assertEquals(15, paths.count { it == "/api/sales/checkout/sync" })
        assertEquals(2, paths.count { it == "/api/sales/cash-expenses/sync" })
        assertEquals(1, paths.count { it == "/api/sales/sessions/sync/close" })

        txIds.forEach { id ->
            assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(id)!!.syncStatus)
        }
        expIds.forEach { id ->
            assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(id)!!.syncStatus)
        }
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(sessionId)!!.syncStatus)
    }

    @Test
    fun cancellationExceptionIsRethrownAndDoesNotMarkEntityOrQueueFailed() = runBlocking {
        val sessionId = insertSyncedOpenSession("srv-sess-cancel")
        val txId = insertCheckout(sessionId, "tx-cancel")
        val txQueue = database.syncQueueDao().getLatestForEntity(txId, SyncEntityType.TRANSACTION)!!

        val cancellingApi = CancellingSalesApi()
        val cancellingSessionSync = OfflineCashSessionSyncService(database, cancellingApi, "android-dev-1")
        val cancellingExpenseSync = OfflineCashExpenseSyncService(database, cancellingApi, "android-dev-1", cancellingSessionSync)
        val cancellingCheckoutSync = OfflineCheckoutSyncService(
            database,
            cancellingApi,
            "android-dev-1",
            cancellingSessionSync,
            cancellingExpenseSync,
        )
        val cancellingSyncManager = SyncManager(
            database = database,
            networkMonitor = AlwaysOnlineConnectivity,
            backendHealthMonitor = healthMonitor,
            localAppSettingsDataSource = settings,
            sessionManager = sessionManager,
            offlineCashSessionSyncService = cancellingSessionSync,
            offlineCheckoutSyncService = cancellingCheckoutSync,
            offlineCashExpenseSyncService = cancellingExpenseSync,
        )

        var caughtCancellation = false
        try {
            cancellingSyncManager.runOnce()
        } catch (_: CancellationException) {
            caughtCancellation = true
        }
        assertTrue("CancellationException must be rethrown", caughtCancellation)

        // Note: transaction remained in SYNCING (not marked FAILED) and will recover via recoverStaleSyncing
        assertNotEquals(SyncStatus.FAILED, database.transactionDao().getByLocalId(txId)!!.syncStatus)
        assertNotEquals(SyncStatus.FAILED, database.syncQueueDao().getById(txQueue.id)!!.status)
    }

    @Test
    fun schedulerUsesSeparateDelayedWorkNameSoRunningWorkerNeverCancelsItselfOn429() {
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(
                context,
                Configuration.Builder()
                    .setExecutor(Executors.newSingleThreadExecutor())
                    .build(),
            )
        }
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork().result.get()

        val scheduler = OfflineSyncScheduler(context, settings, AlwaysOnlineConnectivity)
        scheduler.schedule()
        val primaryBefore = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get()
        assertEquals(1, primaryBefore.size)
        val primaryId = primaryBefore.single().id

        // Enqueue delayed work (simulating HTTP 429 Retry-After: 45 from inside running WORK_NAME)
        scheduler.scheduleDelayed(45L)

        val primaryAfter = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get()
        assertEquals(1, primaryAfter.size)
        assertEquals(primaryId, primaryAfter.single().id)
        assertNotEquals(WorkInfo.State.CANCELLED, primaryAfter.single().state)

        val delayedWork = workManager.getWorkInfosForUniqueWork(SyncWorker.DELAYED_WORK_NAME).get()
        assertEquals(1, delayedWork.size)
        assertEquals(WorkInfo.State.ENQUEUED, delayedWork.single().state)

        // Enqueueing a second delayed work uses APPEND_OR_REPLACE (existing delayed work not cancelled)
        val firstDelayedId = delayedWork.single().id
        scheduler.scheduleDelayed(60L)
        val delayedAfterSecond = workManager.getWorkInfosForUniqueWork(SyncWorker.DELAYED_WORK_NAME).get()
        assertTrue(delayedAfterSecond.any { it.id == firstDelayedId && it.state != WorkInfo.State.CANCELLED })
    }

    @Test
    fun exhaustedWorkerTransientAttemptsKeepItemsPendingAndSyncOnReconnect() = runBlocking {
        val sessionId = insertSyncedOpenSession("srv-sess-exhaust")
        val txId = insertCheckout(sessionId, "tx-exhaust")

        // Simulate transient error during worker run at attempt == MAX_TRANSIENT_WORKER_ATTEMPTS
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Temp unavailable"}"""))
        var delayedCalled = false
        var continuationCalled = false

        val workerResult = SyncWorker.executeSyncWork(
            runOnce = { syncManager.runOnce() },
            scheduleDelayed = { delayedCalled = true },
            scheduleContinuation = { continuationCalled = true },
            runAttemptCount = SyncWorker.MAX_TRANSIENT_WORKER_ATTEMPTS,
        )

        assertEquals(ListenableWorker.Result.success(), workerResult)
        assertFalse(delayedCalled)
        assertFalse(continuationCalled)
        // Item MUST remain PENDING (not FAILED) so reconnect / foreground sync picks it up
        assertEquals(SyncStatus.PENDING, database.transactionDao().getByLocalId(txId)!!.syncStatus)

        // Now network/server recovers and foreground/reconnect triggers syncManager.runOnce()
        enqueueCheckoutSuccess("srv-tx-exhaust")
        val reconnectResult = syncManager.runOnce()
        assertTrue(reconnectResult is SyncResult.Success)
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(txId)!!.syncStatus)
    }

    private suspend fun insertOpenSession(
        clientId: String,
        openedAt: Long = fakeClockMillis - 60_000L,
        cashierUserId: String = "kasir-1",
    ): Long {
        val id = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = clientId,
                deviceId = "android-dev-1",
                cashierUserId = cashierUserId,
                status = "OPEN",
                openedAt = openedAt,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = id,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )
        return id
    }

    private suspend fun insertSyncedOpenSession(serverId: String): Long {
        return database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = serverId,
                clientGeneratedId = "sess-$serverId",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = fakeClockMillis - 3_600_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.SYNCED,
            )
        )
    }

    private suspend fun insertExpense(
        sessionLocalId: Long,
        clientId: String,
        occurredAt: Long = fakeClockMillis - 30_000L,
    ): Long {
        val session = database.cashSessionDao().getByLocalId(sessionLocalId)
        val id = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = clientId,
                deviceId = "android-dev-1",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = session?.serverId,
                category = "CASH_OUT: OPERASIONAL",
                description = "Beli plastik",
                amount = ScaledDecimal2.ofWhole(15_000L),
                occurredAt = occurredAt,
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = id,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )
        return id
    }

    private suspend fun insertCheckout(
        sessionLocalId: Long,
        clientId: String,
        occurredAt: Long = fakeClockMillis - 20_000L,
    ): Long {
        val session = database.cashSessionDao().getByLocalId(sessionLocalId)
        val txId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = clientId,
                deviceId = "android-dev-1",
                transactionCode = "TRX-$clientId",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = session?.serverId,
                cashierUserId = session?.cashierUserId ?: "kasir-1",
                status = "PAID",
                subtotal = ScaledDecimal2.ofWhole(50_000L),
                total = ScaledDecimal2.ofWhole(50_000L),
                paidAmount = ScaledDecimal2.ofWhole(50_000L),
                remainingAmount = ScaledDecimal2.ZERO,
                occurredAt = occurredAt,
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.transactionItemDao().upsert(
            LocalTransactionItemEntity(
                transactionLocalId = txId,
                productServerId = "prod-1",
                productNameSnapshot = "Semen 50kg",
                quantity = ScaledDecimal2.ofWhole(1L),
                priceAtTransaction = ScaledDecimal2.ofWhole(50_000L),
                cogsAtTransaction = ScaledDecimal2.ofWhole(40_000L),
                subtotal = ScaledDecimal2.ofWhole(50_000L),
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                clientGeneratedId = "pay-$clientId",
                transactionLocalId = txId,
                method = "tunai",
                amount = ScaledDecimal2.ofWhole(50_000L),
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = txId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
            )
        )
        return txId
    }

    private fun enqueueOpenSessionSuccess(serverSessionId: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"$serverSessionId","openedAt":"2026-11-24T08:00:00+07:00","syncedAt":"2026-11-24T08:00:05+07:00"}}"""
            )
        )
    }

    private fun enqueueCloseSessionSuccess(serverSessionId: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"data":{"syncStatus":"UPDATED","serverCashSessionId":"$serverSessionId","closedAt":"2026-11-24T17:00:00+07:00","syncedAt":"2026-11-24T17:00:05+07:00"}}"""
            )
        )
    }

    private fun enqueueExpenseSuccess(serverExpenseId: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"$serverExpenseId","syncedAt":"2026-11-24T10:15:00+07:00"}}"""
            )
        )
    }

    private fun enqueueCheckoutSuccess(serverTxId: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverTransactionId":"$serverTxId","receiptId":"RCP-$serverTxId","serverPaymentIds":["pay-1"],"syncedAt":"2026-11-24T10:20:00+07:00"}}"""
            )
        )
    }

    @Test
    fun chronologicalSync_openS1Online_closeS1Offline_openS2Offline_syncsInStrictPerSessionOrder() = runBlocking {
        // S1 dibuka online (openedAt lebih awal) + transaksi & expense offline + tutup S1 offline
        val s1Id = insertSyncedOpenSession("srv-s1")
        val tx1Id = insertCheckout(s1Id, "tx-s1", occurredAt = fakeClockMillis - 3_500_000L)
        val exp1Id = insertExpense(s1Id, "exp-s1", occurredAt = fakeClockMillis - 3_400_000L)
        database.cashSessionDao().closeSession(
            localId = s1Id,
            status = "CLOSED",
            closedAt = fakeClockMillis - 2_000_000L,
            expectedCash = ScaledDecimal2.ofWhole(135_000L),
            actualCash = ScaledDecimal2.ofWhole(135_000L),
            difference = ScaledDecimal2.ZERO,
            closingNote = "tutup S1 offline",
            syncStatus = SyncStatus.PENDING,
            updatedAt = fakeClockMillis - 2_000_000L,
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = s1Id,
                operation = SyncOperation.UPDATE,
                payloadJson = "{}",
                createdAt = fakeClockMillis - 2_000_000L,
                updatedAt = fakeClockMillis - 2_000_000L,
            )
        )

        // S2 dibuka offline setelah S1 ditutup + transaksi di S2
        val s2Id = insertOpenSession("sess-s2-offline")
        val tx2Id = insertCheckout(s2Id, "tx-s2", occurredAt = fakeClockMillis - 30_000L)

        var txCounter = 0
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/sales/checkout/sync" -> {
                        val id = ++txCounter
                        MockResponse().setResponseCode(200).setBody(
                            """{"success":true,"data":{"syncStatus":"CREATED","serverTransactionId":"srv-tx-$id","receiptId":"RCP-$id","serverPaymentIds":["pay-$id"],"syncedAt":"2026-11-24T10:20:00+07:00"}}"""
                        )
                    }
                    "/api/sales/cash-expenses/sync" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"srv-exp-1","syncedAt":"2026-11-24T10:21:00+07:00"}}"""
                    )
                    "/api/sales/sessions/sync/close" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"UPDATED","serverCashSessionId":"srv-s1","closedAt":"2026-11-24T12:00:00+07:00","syncedAt":"2026-11-24T12:00:05+07:00"}}"""
                    )
                    "/api/sales/sessions/sync/open" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-s2","openedAt":"2026-11-24T13:00:00+07:00","syncedAt":"2026-11-24T13:00:05+07:00"}}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val result = syncManager.runOnce()
        assertTrue(result is SyncResult.Success)
        result as SyncResult.Success
        assertFalse(result.hasMorePending)

        val recordedPaths = (0 until server.requestCount).map { server.takeRequest().path!! }
        assertEquals(
            listOf(
                "/api/sales/checkout/sync",
                "/api/sales/cash-expenses/sync",
                "/api/sales/sessions/sync/close",
                "/api/sales/sessions/sync/open",
                "/api/sales/checkout/sync",
            ),
            recordedPaths,
        )

        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(tx1Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(exp1Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(s1Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(s2Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(tx2Id)!!.syncStatus)
    }

    @Test
    fun blockedOpenSession_staysPendingAndAutoRecoversAfterOlderSessionCloses() = runBlocking {
        // S2 dibuka offline ketika server masih menolak karena ada sesi lama belum ditutup
        val s2Id = insertOpenSession("sess-s2-blocked")
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"success":false,"code":"VALIDATION_ERROR","message":"Anda masih memiliki sesi kasir yang belum ditutup (ID: srv-s1-block). Tutup sesi tersebut sebelum sinkronisasi sesi lokal."}"""
            )
        )

        val firstRun = syncManager.runOnce()
        assertTrue(firstRun is SyncResult.Success)

        val s2AfterReject = database.cashSessionDao().getByLocalId(s2Id)!!
        assertEquals(SyncStatus.PENDING, s2AfterReject.syncStatus)
        val s2Queue = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = s2Id,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.CREATE,
        )!!
        assertEquals(SyncStatus.PENDING, s2Queue.status)
        assertEquals(CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN, s2Queue.lastErrorCode)
        assertEquals(0, s2Queue.retryCount)

        // S1 (dibuka lebih awal dari S2) sekarang punya tutup offline yang siap disinkronkan
        val s1Id = insertSyncedOpenSession("srv-s1-block")
        database.cashSessionDao().closeSession(
            localId = s1Id,
            status = "CLOSED",
            closedAt = fakeClockMillis - 100_000L,
            expectedCash = ScaledDecimal2.ofWhole(100_000L),
            actualCash = ScaledDecimal2.ofWhole(100_000L),
            difference = ScaledDecimal2.ZERO,
            closingNote = "tutup s1",
            syncStatus = SyncStatus.PENDING,
            updatedAt = fakeClockMillis - 100_000L,
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = s1Id,
                operation = SyncOperation.UPDATE,
                payloadJson = "{}",
                createdAt = fakeClockMillis - 100_000L,
                updatedAt = fakeClockMillis - 100_000L,
            )
        )

        enqueueCloseSessionSuccess("srv-s1-block")
        enqueueOpenSessionSuccess("srv-s2-recovered")

        val secondRun = syncManager.runOnce()
        assertTrue(secondRun is SyncResult.Success)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(s1Id)!!.syncStatus)
        val s2Recovered = database.cashSessionDao().getByLocalId(s2Id)!!
        assertEquals(SyncStatus.SYNCED, s2Recovered.syncStatus)
        assertEquals("srv-s2-recovered", s2Recovered.serverId)
    }

    @Test
    fun multiUserSharedDevice_otherUserQueueUntouchedHiddenAndCounted_thenSyncedOnRelogin() = runBlocking {
        // Kasir A membuat sesi lokal + transaksi + pengeluaran saat login
        sessionManager.saveAuthenticatedSession(
            token = "token-a",
            refreshToken = "refresh-a",
            user = SessionUser(
                name = "Kasir A",
                role = "KASIR",
                isActive = true,
                joinedAt = "2026-01-01",
                lastLoginAt = null,
                userId = "kasir-a",
            ),
        )
        val sA = insertOpenSession("sess-kasir-a", cashierUserId = "kasir-a")
        val txA = insertCheckout(sA, "tx-kasir-a")
        val expA = insertExpense(sA, "exp-kasir-a")

        val monitoringRepo = SyncMonitoringRepository(
            syncQueueDao = database.syncQueueDao(),
            offlineCashSessionSyncService = cashSessionSync,
            offlineCheckoutSyncService = checkoutSync,
            offlineCashExpenseSyncService = cashExpenseSync,
            cashSessionDao = database.cashSessionDao(),
            transactionDao = database.transactionDao(),
            cashExpenseDao = database.cashExpenseDao(),
            clockMillis = { fakeClockMillis },
            activeUserIdFlow = sessionManager.activeUserIdFlow,
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
        )
        val statusRepo = OfflineStatusRepository(
            networkMonitor = AlwaysOnlineConnectivity,
            backendHealthMonitor = healthMonitor,
            localAppSettingsDataSource = settings,
            syncQueueDao = database.syncQueueDao(),
            cashSessionDao = database.cashSessionDao(),
            transactionDao = database.transactionDao(),
            cashExpenseDao = database.cashExpenseDao(),
            activeUserIdFlow = sessionManager.activeUserIdFlow,
        )

        // Kasir A logout -> Kasir B login
        sessionManager.clearSession()
        sessionManager.saveAuthenticatedSession(
            token = "token-b",
            refreshToken = "refresh-b",
            user = SessionUser(
                name = "Kasir B",
                role = "KASIR",
                isActive = true,
                joinedAt = "2026-01-01",
                lastLoginAt = null,
                userId = "kasir-b",
            ),
        )

        val kasirBRun = syncManager.runOnce()
        assertTrue(kasirBRun is SyncResult.Success)
        assertEquals(0, server.requestCount)
        assertEquals(SyncStatus.PENDING, database.cashSessionDao().getByLocalId(sA)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.transactionDao().getByLocalId(txA)!!.syncStatus)
        assertEquals(SyncStatus.PENDING, database.cashExpenseDao().getByLocalId(expA)!!.syncStatus)

        val kasirBSnapshot = monitoringRepo.refresh()
        assertTrue(kasirBSnapshot.queueItems.isEmpty())
        assertEquals(0, kasirBSnapshot.totalPending)
        assertEquals(3, kasirBSnapshot.otherUsersPendingSyncCount)
        val kasirBStatus = statusRepo.status.first()
        assertEquals(0, kasirBStatus.pendingSyncCount)
        assertEquals(3, kasirBStatus.otherUsersPendingSyncCount)

        // Ganti ke Owner -> data Kasir A tetap PENDING, tidak terlihat di queue, hanya jumlah ringkas
        sessionManager.saveAuthenticatedSession(
            token = "token-owner",
            refreshToken = "refresh-owner",
            user = SessionUser(
                name = "Owner",
                role = "OWNER",
                isActive = true,
                joinedAt = "2026-01-01",
                lastLoginAt = null,
                userId = "owner-1",
            ),
        )
        val ownerRun = syncManager.runOnce()
        assertTrue(ownerRun is SyncResult.Success)
        assertEquals(0, server.requestCount)
        val ownerSnapshot = monitoringRepo.refresh()
        assertTrue(ownerSnapshot.queueItems.isEmpty())
        assertEquals(3, ownerSnapshot.otherUsersPendingSyncCount)
        assertEquals(3, statusRepo.status.first().otherUsersPendingSyncCount)

        // Kasir A login lagi -> seluruh antrean Kasir A disinkronkan bersih
        sessionManager.saveAuthenticatedSession(
            token = "token-a-2",
            refreshToken = "refresh-a-2",
            user = SessionUser(
                name = "Kasir A",
                role = "KASIR",
                isActive = true,
                joinedAt = "2026-01-01",
                lastLoginAt = null,
                userId = "kasir-a",
            ),
        )
        val kasirASnapshotBefore = monitoringRepo.refresh()
        assertEquals(3, kasirASnapshotBefore.queueItems.size)
        assertEquals(0, kasirASnapshotBefore.otherUsersPendingSyncCount)

        enqueueOpenSessionSuccess("srv-kasir-a")
        enqueueCheckoutSuccess("srv-tx-kasir-a")
        enqueueExpenseSuccess("srv-exp-kasir-a")
        val kasirARun = syncManager.runOnce()
        assertTrue(kasirARun is SyncResult.Success)
        assertEquals(3, server.requestCount)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(sA)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(txA)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(expA)!!.syncStatus)
    }
}

private object AlwaysOnlineConnectivity : NetworkConnectivity {
    private val flow = MutableStateFlow(true)
    override fun isOnline(): Boolean = true
    override fun observeOnline(): Flow<Boolean> = flow
}

private class FakeTokenStore(
    private var accessToken: String?,
    private var refreshToken: String?,
    private var sessionUser: SessionUser? = SessionUser(
        name = "Kasir Satu",
        role = "KASIR",
        isActive = true,
        joinedAt = "2026-01-01",
        lastLoginAt = null,
        userId = "kasir-1",
    ),
) : TokenStore {
    override fun readAccessToken(): String? = accessToken
    override fun readRefreshToken(): String? = refreshToken
    override fun saveAccessToken(token: String) { accessToken = token }
    override fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    override fun readSessionUser(): SessionUser? = sessionUser
    override fun saveSessionUser(user: SessionUser) { sessionUser = user }
    override fun clearAccessToken() {
        accessToken = null
        refreshToken = null
        sessionUser = null
    }
}

private class CancellingSalesApi : SalesApi {
    override suspend fun syncOfflineCheckout(request: OfflineCheckoutSyncRequestDto): Response<com.tbterminal.app.data.remote.ApiResponse<OfflineCheckoutSyncResponseDto>> {
        throw CancellationException("Simulated worker cancellation")
    }
    override suspend fun syncOpenCashSession(request: OfflineCashSessionOpenSyncRequestDto): Response<com.tbterminal.app.data.remote.ApiResponse<OfflineCashSessionOpenSyncResponseDto>> {
        throw CancellationException("Simulated worker cancellation")
    }
    override suspend fun syncCloseCashSession(request: OfflineCashSessionCloseSyncRequestDto): Response<com.tbterminal.app.data.remote.ApiResponse<OfflineCashSessionCloseSyncResponseDto>> {
        throw CancellationException("Simulated worker cancellation")
    }
    override suspend fun syncCashExpense(request: OfflineCashExpenseSyncRequestDto): Response<com.tbterminal.app.data.remote.ApiResponse<OfflineCashExpenseSyncResponseDto>> {
        throw CancellationException("Simulated worker cancellation")
    }
    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?) = error("unused")
    override suspend fun getActiveSession() = error("unused")
    override suspend fun getSessionById(sessionId: String) = error("unused")
    override suspend fun openSession(request: com.tbterminal.app.data.remote.OpenSessionRequestDto) = error("unused")
    override suspend fun closeSession(request: com.tbterminal.app.data.remote.CloseSessionRequestDto) = error("unused")
    override suspend fun forceCloseSession(sessionId: String, request: com.tbterminal.app.data.remote.CloseSessionRequestDto) = error("unused")
    override suspend fun addExpense(request: com.tbterminal.app.data.remote.CashExpenseRequestDto) = error("unused")
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?) = error("unused")
    override suspend fun getExpenses(sessionId: String) = error("unused")
    override suspend fun payTransactionDebt(id: String, request: com.tbterminal.app.data.remote.PayDebtRequestDto) = error("unused")
    override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?) = error("unused")
    override suspend fun getTransactionById(id: String) = error("unused")
    override suspend fun voidTransaction(id: String, request: com.tbterminal.app.data.remote.VoidTransactionRequestDto) = error("unused")
    override suspend fun refundTransaction(id: String, request: com.tbterminal.app.data.remote.RefundTransactionRequestDto) = error("unused")
    override suspend fun getQuotations(page: Int, limit: Int, status: String?, search: String?, startDate: String?, endDate: String?) = error("unused")
    override suspend fun createQuotation(request: com.tbterminal.app.data.remote.CreateQuotationRequestDto) = error("unused")
    override suspend fun getQuotationById(id: String) = error("unused")
    override suspend fun updateQuotationStatus(id: String, request: com.tbterminal.app.data.remote.UpdateQuotationStatusRequestDto) = error("unused")
}
