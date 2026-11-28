package com.tbterminal.app.data.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashsession.LocalCashSessionResult
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
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
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
class OfflineCashSessionAdoptionTest {
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
    private lateinit var localCashSessionService: LocalCashSessionService
    private lateinit var cashSessionLocalDataSource: CashSessionLocalDataSource

    // 2026-11-28T09:00:00Z
    private var fakeClockMillis = Instant.parse("2026-11-28T09:00:00Z").toEpochMilli()

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

        sessionManager = SessionManager(AdoptionFakeTokenStore())
        healthMonitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> =
                    Response.success(HealthResponseDto(status = "ready", database = "up"))
            },
            networkMonitor = AdoptionAlwaysOnlineConnectivity,
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
            networkMonitor = AdoptionAlwaysOnlineConnectivity,
            backendHealthMonitor = healthMonitor,
            localAppSettingsDataSource = settings,
            sessionManager = sessionManager,
            offlineCashSessionSyncService = cashSessionSync,
            offlineCheckoutSyncService = checkoutSync,
            offlineCashExpenseSyncService = cashExpenseSync,
        )
        localCashSessionService = LocalCashSessionService(database, "android-dev-1", roleProvider = { "KASIR" })
        cashSessionLocalDataSource = CashSessionLocalDataSource(
            cashSessionDao = database.cashSessionDao(),
            transactionDao = database.transactionDao(),
            cashExpenseDao = database.cashExpenseDao(),
            syncQueueDao = database.syncQueueDao(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun reconnectWithAdoptFlag_adoptsActiveRemoteSessionAndAttachesOfflineTxAndExpenseWithoutSyncOpen() = runBlocking {
        // Buka sesi lokal akibat timeout ambigu; transaksi & pengeluaran terjadi dalam batas skew 4 menit sebelum remote openedAt (09:00Z)
        val openResult = localCashSessionService.openLocalSession(
            cashierUserId = "kasir-1",
            startingCash = BigDecimal("100000"),
            openingNote = null,
            adoptRemoteIfActive = true,
        )
        assertTrue(openResult is LocalCashSessionResult.Success)
        val localSessionId = (openResult as LocalCashSessionResult.Success).session.localId

        val txOccurredAt = Instant.parse("2026-11-28T08:56:00Z").toEpochMilli()
        val expOccurredAt = Instant.parse("2026-11-28T08:57:00Z").toEpochMilli()
        val txId = insertLocalCheckout(localSessionId, "tx-adopt-1", occurredAt = txOccurredAt)
        val expId = insertLocalExpense(localSessionId, "exp-adopt-1", occurredAt = expOccurredAt)

        val capturedRequests = mutableListOf<Pair<String, String>>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                val body = request.body.readUtf8()
                capturedRequests += path to body
                return when (path) {
                    "/api/sales/sessions/active" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"id":"srv-sess-adopted","userId":"kasir-1","userName":"Kasir Satu","openedAt":"2026-11-28T09:00:00Z","closedAt":null,"openingCash":"100000","closingCash":null,"systemCash":"100000","difference":null,"totalExpenses":"0","notes":null,"status":"OPEN"}}"""
                    )
                    "/api/sales/checkout/sync" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverTransactionId":"srv-tx-adopt-1","receiptId":"RCP-1","serverPaymentIds":["pay-1"],"syncedAt":"2026-11-28T09:05:00Z"}}"""
                    )
                    "/api/sales/cash-expenses/sync" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"srv-exp-adopt-1","syncedAt":"2026-11-28T09:05:01Z"}}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val syncResult = syncManager.runOnce()
        assertTrue(syncResult is SyncResult.Success)
        assertFalse((syncResult as SyncResult.Success).hasMorePending)

        val paths = capturedRequests.map { it.first }
        assertFalse("Must NOT call /api/sales/sessions/sync/open when remote OPEN session is adopted", "/api/sales/sessions/sync/open" in paths)
        assertEquals(
            listOf("/api/sales/sessions/active", "/api/sales/checkout/sync", "/api/sales/cash-expenses/sync"),
            paths,
        )
        assertTrue(capturedRequests[1].second.contains("\"cashSessionId\":\"srv-sess-adopted\""))
        assertTrue(capturedRequests[2].second.contains("\"serverCashSessionId\":\"srv-sess-adopted\""))

        val updatedSession = database.cashSessionDao().getByLocalId(localSessionId)!!
        assertEquals("srv-sess-adopted", updatedSession.serverId)
        assertEquals(SyncStatus.SYNCED, updatedSession.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(txId)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(expId)!!.syncStatus)
    }

    @Test
    fun reconnectWithAdoptFlag_whenOfflineTxViolates5MinWindow_rejectsAdoptionPreservesOccurredAtAndDefers() = runBlocking {
        // Sesi server openedAt = 09:00:00Z, sedangkan transaksi offline occurredAt = 08:54:00Z (> 5 menit lebih awal)
        val openResult = localCashSessionService.openLocalSession(
            cashierUserId = "kasir-1",
            startingCash = BigDecimal("100000"),
            openingNote = null,
            adoptRemoteIfActive = true,
        ) as LocalCashSessionResult.Success
        val localSessionId = openResult.session.localId
        val txOccurredAt = Instant.parse("2026-11-28T08:54:00Z").toEpochMilli()
        val txId = insertLocalCheckout(localSessionId, "tx-window-reject", occurredAt = txOccurredAt)

        val paths = mutableListOf<String>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                paths += request.path.orEmpty()
                return when (request.path) {
                    "/api/sales/sessions/active" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"id":"srv-sess-newer","userId":"kasir-1","userName":"Kasir Satu","openedAt":"2026-11-28T09:00:00Z","closedAt":null,"openingCash":"100000","closingCash":null,"systemCash":"100000","difference":null,"totalExpenses":"0","notes":null,"status":"OPEN"}}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val syncResult = syncManager.runOnce()
        assertTrue(syncResult is SyncResult.Success)
        assertEquals(listOf("/api/sales/sessions/active"), paths)

        val sessionAfter = database.cashSessionDao().getByLocalId(localSessionId)!!
        assertNull(sessionAfter.serverId)
        assertEquals(SyncStatus.PENDING, sessionAfter.syncStatus)

        val queueAfter = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = localSessionId,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.CREATE,
        )!!
        assertEquals(SyncStatus.PENDING, queueAfter.status)
        assertEquals(CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN, queueAfter.lastErrorCode)

        // occurredAt transaksi asli tidak boleh diubah atau dipalsukan
        assertEquals(txOccurredAt, database.transactionDao().getByLocalId(txId)!!.occurredAt)
    }

    @Test
    fun reconnectWithAdoptFlag_whenNoActiveRemoteOrDifferentCashier_syncsOpenNormally() = runBlocking {
        // Case A: getActiveSession returns null -> lanjut POST /api/sales/sessions/sync/open
        val s1Id = (
            localCashSessionService.openLocalSession(
                cashierUserId = "kasir-1",
                startingCash = BigDecimal("100000"),
                openingNote = null,
                adoptRemoteIfActive = true,
            ) as LocalCashSessionResult.Success
        ).session.localId

        val recordedPaths = mutableListOf<String>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                recordedPaths += path
                return when (path) {
                    "/api/sales/sessions/active" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":null}"""
                    )
                    "/api/sales/sessions/sync/open" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-open-normal","openedAt":"2026-11-28T09:00:00Z","syncedAt":"2026-11-28T09:00:01Z"}}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        syncManager.runOnce()
        assertEquals(listOf("/api/sales/sessions/active", "/api/sales/sessions/sync/open"), recordedPaths)
        assertEquals("srv-open-normal", database.cashSessionDao().getByLocalId(s1Id)!!.serverId)

        // Case B: getActiveSession returns OPEN session milik kasir-2 -> bukan milik kasir-1, tetap sync open normal
        database.openHelper.writableDatabase.execSQL("DELETE FROM sync_queue")
        database.openHelper.writableDatabase.execSQL("DELETE FROM local_cash_sessions")
        recordedPaths.clear()

        val s2Id = (
            localCashSessionService.openLocalSession(
                cashierUserId = "kasir-1",
                startingCash = BigDecimal("100000"),
                openingNote = null,
                adoptRemoteIfActive = true,
            ) as LocalCashSessionResult.Success
        ).session.localId

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                recordedPaths += path
                return when (path) {
                    "/api/sales/sessions/active" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"id":"srv-other-kasir","userId":"kasir-2","userName":"Kasir Dua","openedAt":"2026-11-28T09:00:00Z","closedAt":null,"openingCash":"100000","closingCash":null,"systemCash":"100000","difference":null,"totalExpenses":"0","notes":null,"status":"OPEN"}}"""
                    )
                    "/api/sales/sessions/sync/open" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-open-kasir1","openedAt":"2026-11-28T09:00:00Z","syncedAt":"2026-11-28T09:00:01Z"}}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        syncManager.runOnce()
        assertEquals(listOf("/api/sales/sessions/active", "/api/sales/sessions/sync/open"), recordedPaths)
        assertEquals("srv-open-kasir1", database.cashSessionDao().getByLocalId(s2Id)!!.serverId)
    }

    @Test
    fun reconnectWithAdoptFlag_whenRemoteIdAlreadyOwnedByAnotherLocalRow_doesNotAdoptAndDefers() = runBlocking {
        // Sesi lokal S1 sudah terikat ke srv-shared-id dan sudah ditutup secara lokal (belum diantrekan CLOSE agar fokus ke guard adopsi)
        database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-shared-id",
                clientGeneratedId = "srv-shared-id",
                cashierUserId = "kasir-1",
                status = "CLOSED",
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                expectedCash = ScaledDecimal2.ofWhole(100_000L),
                actualCash = ScaledDecimal2.ofWhole(100_000L),
                difference = ScaledDecimal2.ZERO,
                openedAt = fakeClockMillis - 300_000L,
                closedAt = fakeClockMillis - 120_000L,
                syncStatus = SyncStatus.PENDING,
                createdAt = fakeClockMillis - 300_000L,
                updatedAt = fakeClockMillis - 120_000L,
            )
        )
        val s2Id = (
            localCashSessionService.openLocalSession(
                cashierUserId = "kasir-1",
                startingCash = BigDecimal("150000"),
                openingNote = null,
                adoptRemoteIfActive = true,
            ) as LocalCashSessionResult.Success
        ).session.localId

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/sales/sessions/active" -> MockResponse().setResponseCode(200).setBody(
                        """{"success":true,"data":{"id":"srv-shared-id","userId":"kasir-1","userName":"Kasir Satu","openedAt":"2026-11-28T08:55:00Z","closedAt":null,"openingCash":"100000","closingCash":null,"systemCash":"100000","difference":null,"totalExpenses":"0","notes":null,"status":"OPEN"}}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val result = syncManager.runOnce()
        assertTrue(result is SyncResult.Success)
        val s2After = database.cashSessionDao().getByLocalId(s2Id)!!
        assertNull(s2After.serverId)
        assertEquals(SyncStatus.PENDING, s2After.syncStatus)
        val q2 = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = s2Id,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.CREATE,
        )!!
        assertEquals(CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN, q2.lastErrorCode)
    }

    @Test
    fun foregroundMirrorRemoteSession_adoptsLocalOpenSessionWithAdoptFlagInsteadOfDuplicating() = runBlocking {
        val localSession = (
            localCashSessionService.openLocalSession(
                cashierUserId = "kasir-1",
                startingCash = BigDecimal("100000"),
                openingNote = null,
                adoptRemoteIfActive = true,
            ) as LocalCashSessionResult.Success
        ).session
        // Pastikan transaksi offline berada di dalam batas toleransi 5 menit sebelum remote openedAt (2026-11-28T09:00:00Z)
        insertLocalCheckout(
            sessionLocalId = localSession.localId,
            clientId = "tx-fg-adopt",
            occurredAt = Instant.parse("2026-11-28T08:58:00Z").toEpochMilli(),
        )

        val remote = CashSession(
            id = "srv-fg-adopted",
            userId = "kasir-1",
            userName = "Kasir Satu",
            openedAt = "2026-11-28T09:00:00Z",
            closedAt = null,
            openingCash = BigDecimal("100000"),
            closingCash = null,
            systemCash = BigDecimal("100000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
        )
        val mirrored = cashSessionLocalDataSource.mirrorRemoteSession(remote)
        assertEquals(localSession.localId, mirrored.localId)
        assertEquals("srv-fg-adopted", mirrored.serverId)
        assertEquals(SyncStatus.SYNCED, mirrored.syncStatus)

        val openQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = localSession.localId,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.CREATE,
        )!!
        assertEquals(SyncStatus.SYNCED, openQueue.status)
    }

    private suspend fun insertLocalExpense(
        sessionLocalId: Long,
        clientId: String,
        occurredAt: Long,
    ): Long {
        val session = database.cashSessionDao().getByLocalId(sessionLocalId)
        val id = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = clientId,
                deviceId = "android-dev-1",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = session?.serverId,
                category = "CASH_OUT: OPERASIONAL",
                description = "Bensin kurir",
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
                createdAt = occurredAt,
                updatedAt = occurredAt,
            )
        )
        return id
    }

    private suspend fun insertLocalCheckout(
        sessionLocalId: Long,
        clientId: String,
        occurredAt: Long,
    ): Long {
        val session = database.cashSessionDao().getByLocalId(sessionLocalId)
        val txId = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = clientId,
                deviceId = "android-dev-1",
                transactionCode = "LOCAL-$clientId",
                cashSessionLocalId = sessionLocalId,
                cashSessionServerId = session?.serverId,
                cashierUserId = session?.cashierUserId ?: "kasir-1",
                status = "PAID",
                subtotal = ScaledDecimal2.ofWhole(50_000L),
                total = ScaledDecimal2.ofWhole(50_000L),
                paidAmount = ScaledDecimal2.ofWhole(50_000L),
                remainingAmount = ScaledDecimal2.ZERO,
                syncStatus = SyncStatus.PENDING,
                createdAt = occurredAt,
                updatedAt = occurredAt,
                occurredAt = occurredAt,
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
                createdAt = occurredAt,
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                transactionLocalId = txId,
                clientGeneratedId = "pay-$clientId",
                method = "tunai",
                amount = ScaledDecimal2.ofWhole(50_000L),
                paidAt = occurredAt,
                syncStatus = SyncStatus.PENDING,
                createdAt = occurredAt,
                updatedAt = occurredAt,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = txId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                createdAt = occurredAt,
                updatedAt = occurredAt,
            )
        )
        return txId
    }
}

private object AdoptionAlwaysOnlineConnectivity : NetworkConnectivity {
    private val flow = MutableStateFlow(true)
    override fun isOnline(): Boolean = true
    override fun observeOnline(): Flow<Boolean> = flow
}

private class AdoptionFakeTokenStore : TokenStore {
    private var accessToken: String? = "token-valid"
    private var refreshToken: String? = "refresh-valid"
    private var sessionUser: SessionUser? = SessionUser(
        name = "Kasir Satu",
        role = "KASIR",
        isActive = true,
        joinedAt = "2026-01-01",
        lastLoginAt = null,
        userId = "kasir-1",
    )

    override fun readAccessToken(): String? = accessToken
    override fun readRefreshToken(): String? = refreshToken
    override fun saveAccessToken(token: String) {
        accessToken = token
    }
    override fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    override fun readSessionUser(): SessionUser? = sessionUser
    override fun saveSessionUser(user: SessionUser) {
        sessionUser = user
    }
    override fun clearAccessToken() {
        accessToken = null
        refreshToken = null
        sessionUser = null
    }
}
