package com.tbterminal.app.data.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import java.time.Instant
import java.time.ZoneId
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
import org.junit.Assert.assertNotNull
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
class OfflineTimeConflictAndRecoveryTest {
    private lateinit var context: Context
    private lateinit var database: TbTerminalDatabase
    private lateinit var server: MockWebServer
    private lateinit var salesApi: SalesApi
    private lateinit var settings: LocalAppSettingsDataSource
    private lateinit var sessionManager: SessionManager
    private lateinit var trustedClock: TrustedOfflineClock
    private lateinit var healthMonitor: BackendHealthMonitor
    private lateinit var cashSessionSync: OfflineCashSessionSyncService
    private lateinit var cashExpenseSync: OfflineCashExpenseSyncService
    private lateinit var checkoutSync: OfflineCheckoutSyncService
    private lateinit var syncManager: SyncManager

    private var wallClockMillis = 1_795_873_593_000L
    private var elapsedMillis = 500_000L
    private var bootCount: Long? = 1L
    private var readinessTimeProvider: (() -> String?)? = null

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

        sessionManager = SessionManager(
            RecoveryFakeTokenStore(
                user = SessionUser(
                    name = "Kasir",
                    role = "KASIR",
                    isActive = true,
                    joinedAt = "2026-01-01",
                    lastLoginAt = null,
                    userId = CASHIER_USER_ID,
                )
            )
        )

        trustedClock = TrustedOfflineClock(
            localAppSettingsDataSource = settings,
            wallClockMillis = { wallClockMillis },
            elapsedRealtimeMillis = { elapsedMillis },
            bootCountProvider = { bootCount },
        )

        healthMonitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> {
                    val timeIso = readinessTimeProvider?.invoke()
                    return Response.success(
                        HealthResponseDto(
                            status = "ready",
                            database = "up",
                            time = timeIso,
                        )
                    )
                }
            },
            networkMonitor = RecoveryAlwaysOnlineConnectivity,
            throttleMillis = 0L,
            timeoutMillis = 1_000L,
            clockMillis = { elapsedMillis },
            trustedOfflineClock = trustedClock,
        )

        cashSessionSync = OfflineCashSessionSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = DEVICE_ID,
            clockMillis = { wallClockMillis },
            roleProvider = { sessionManager.readSessionUser()?.role },
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
        )
        cashExpenseSync = OfflineCashExpenseSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = DEVICE_ID,
            cashSessionSyncService = cashSessionSync,
            clockMillis = { wallClockMillis },
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
            trustedOfflineClock = trustedClock,
        )
        checkoutSync = OfflineCheckoutSyncService(
            database = database,
            salesApi = salesApi,
            deviceId = DEVICE_ID,
            cashSessionSyncService = cashSessionSync,
            cashExpenseSyncService = cashExpenseSync,
            clockMillis = { wallClockMillis },
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
            trustedOfflineClock = trustedClock,
        )
        syncManager = SyncManager(
            database = database,
            networkMonitor = RecoveryAlwaysOnlineConnectivity,
            backendHealthMonitor = healthMonitor,
            localAppSettingsDataSource = settings,
            sessionManager = sessionManager,
            offlineCashSessionSyncService = cashSessionSync,
            offlineCheckoutSyncService = checkoutSync,
            offlineCashExpenseSyncService = cashExpenseSync,
            clockMillis = { wallClockMillis },
            trustedOfflineClock = trustedClock,
            deviceId = DEVICE_ID,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun futureTimeRejectionUsesTrustedClockNotMessageTextAndDefersUntilEligible() = runBlocking {
        val serverNow = 1_795_860_000_000L
        calibrateClockAt(serverNow)

        val sessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-future",
                clientGeneratedId = "SESSION-future",
                deviceId = DEVICE_ID,
                cashierUserId = CASHIER_USER_ID,
                status = "OPEN",
                openedAt = serverNow - 3_600_000L,
                startingCash = ScaledDecimal2.ofWhole(500_000L),
                syncStatus = SyncStatus.SYNCED,
            )
        )

        // occurredAt is +10 minutes ahead of trustedNow (> trustedNow - 1 min -> future case)
        val futureOccurredAt = serverNow + 600_000L
        val expenseId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "EXP-FUTURE-1",
                deviceId = DEVICE_ID,
                cashSessionLocalId = sessionId,
                cashSessionServerId = "srv-sess-future",
                category = "CASH_OUT: Bensin",
                description = "Bensin",
                amount = ScaledDecimal2.ofWhole(10_000L),
                occurredAt = futureOccurredAt,
                syncStatus = SyncStatus.PENDING,
            )
        )
        val queueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expenseId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
            )
        )

        // Deliberately use a generic error message without "masa depan" to prove classification uses trusted clock
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("""{"success":false,"code":"OFFLINE_OCCURRED_AT_INVALID","message":"Pelanggaran business rule waktu tidak valid"}""")
        )

        val firstRun = syncManager.runOnce()
        assertTrue(firstRun is SyncResult.Success)
        firstRun as SyncResult.Success
        assertEquals(0, firstRun.successCount)
        assertEquals(0, firstRun.failedCount)
        assertEquals(1, server.requestCount)
        assertNotNull(firstRun.deferredRetryAfterSeconds)
        assertNull(settings.getLastSuccessfulSyncAt(CASHIER_USER_ID))

        val deferredQueue = database.syncQueueDao().getById(queueId)!!
        assertEquals(SyncStatus.PENDING, deferredQueue.status)
        assertEquals("OFFLINE_OCCURRED_AT_INVALID", deferredQueue.lastErrorCode)

        // Advance only 5 minutes (still < occurredAt + 1 min) -> must NOT hit server again
        elapsedMillis += 300_000L
        val secondRun = syncManager.runOnce() as SyncResult.Success
        assertEquals(1, server.requestCount)
        assertNotNull(secondRun.deferredRetryAfterSeconds)

        // Advance past occurredAt + 1 min -> now sends to server and succeeds
        elapsedMillis += 400_000L // total +700s > +660s
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"srv-exp-future","syncedAt":"2026-11-28T17:00:00+07:00"}}""")
        )
        val thirdRun = syncManager.runOnce() as SyncResult.Success
        assertEquals(1, thirdRun.successCount)
        assertEquals(2, server.requestCount)
        assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(expenseId)!!.syncStatus)
        assertEquals(wallClockMillis, settings.getLastSuccessfulSyncAt(CASHIER_USER_ID))
    }

    @Test
    fun pastTimeRejectionBeforeSessionOpenStaysConflictEvenWithValidAnchor() = runBlocking {
        val serverNow = 1_795_860_000_000L
        calibrateClockAt(serverNow)

        val sessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-past",
                clientGeneratedId = "SESSION-past",
                deviceId = DEVICE_ID,
                cashierUserId = CASHIER_USER_ID,
                status = "OPEN",
                openedAt = serverNow - 1_800_000L,
                startingCash = ScaledDecimal2.ofWhole(500_000L),
                syncStatus = SyncStatus.SYNCED,
            )
        )

        // Far in the past (before session openedAt - 5 min)
        val pastOccurredAt = serverNow - 7_200_000L
        val expenseId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "EXP-PAST-1",
                deviceId = DEVICE_ID,
                cashSessionLocalId = sessionId,
                cashSessionServerId = "srv-sess-past",
                category = "CASH_OUT: Bensin",
                description = "Bensin lama",
                amount = ScaledDecimal2.ofWhole(10_000L),
                occurredAt = pastOccurredAt,
                syncStatus = SyncStatus.PENDING,
            )
        )
        val queueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expenseId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
            )
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("""{"success":false,"code":"OFFLINE_OCCURRED_AT_INVALID","message":"Waktu pengeluaran kas lebih awal dari pembukaan sesi"}""")
        )

        val result = syncManager.runOnce() as SyncResult.Success
        assertEquals(0, result.successCount)
        assertEquals(1, result.failedCount)
        assertEquals(SyncStatus.CONFLICT, database.syncQueueDao().getById(queueId)!!.status)
        assertEquals(SyncStatus.CONFLICT, database.cashExpenseDao().getByLocalId(expenseId)!!.syncStatus)
        assertNull(settings.getLastSuccessfulSyncAt(CASHIER_USER_ID))
    }

    @Test
    fun legacyConflictRecoveryRunsOnlyOncePreservesKeyAndPayloadAndSkipsWhenNoAnchor() = runBlocking {
        val openedAt = 1_795_855_058_980L
        val occurredAt = 1_795_858_646_234L
        val sessionId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-once",
                clientGeneratedId = "SESSION-once",
                deviceId = DEVICE_ID,
                cashierUserId = CASHIER_USER_ID,
                status = "OPEN",
                openedAt = openedAt,
                startingCash = ScaledDecimal2.ofWhole(500_000L),
                syncStatus = SyncStatus.SYNCED,
            )
        )
        val expenseId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "EXP-ONCE-KEY",
                deviceId = DEVICE_ID,
                cashSessionLocalId = sessionId,
                cashSessionServerId = "srv-sess-once",
                category = "CASH_OUT: Bensin",
                description = "Bensin",
                amount = ScaledDecimal2.ofWhole(10_000L),
                occurredAt = occurredAt,
                syncStatus = SyncStatus.CONFLICT,
            )
        )
        val queueId = database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expenseId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.CONFLICT,
                lastError = "Waktu pengeluaran kas berada di masa depan",
                lastErrorCode = "OFFLINE_OCCURRED_AT_INVALID",
            )
        )

        // 1. Without valid anchor (readiness returns null time), CONFLICT must NOT be touched
        readinessTimeProvider = null
        syncManager.runOnce()
        assertEquals(SyncStatus.CONFLICT, database.syncQueueDao().getById(queueId)!!.status)
        assertEquals(0, server.requestCount)

        // 2. Calibrate at later time -> auto-recovers once, sends exact same key & occurredAt, then server rejects again
        calibrateClockAt(1_795_873_593_000L)
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("""{"success":false,"code":"OFFLINE_OCCURRED_AT_INVALID","message":"Pelanggaran business rule: ditolak lagi"}""")
        )
        syncManager.runOnce()
        assertEquals(1, server.requestCount)
        val reqBody = server.takeRequest().body.readUtf8()
        assertTrue(reqBody.contains("EXP-ONCE-KEY"))
        val expectedOccurredIso = Instant.ofEpochMilli(occurredAt)
            .atZone(ZoneId.of("Asia/Jakarta"))
            .toOffsetDateTime()
            .toString()
        assertTrue(reqBody.contains(expectedOccurredIso))

        // Returned to CONFLICT because server rejected again (and occurredAt <= trustedNow - 1m so not future-deferred)
        assertEquals(SyncStatus.CONFLICT, database.syncQueueDao().getById(queueId)!!.status)

        // Audit log recorded for the single auto-recovery
        val audits = database.localAuditLogDao().observeAuditLogs().first()
        assertEquals(1, audits.count { it.action == "AUTO_RECOVER_TIME_CONFLICT" && it.recordId == "EXP-ONCE-KEY" })

        // 3. Subsequent sync runs must NOT auto-recover the same item a second time
        syncManager.runOnce()
        assertEquals(1, server.requestCount)
        assertEquals(SyncStatus.CONFLICT, database.syncQueueDao().getById(queueId)!!.status)
    }

    @Test
    fun realDeviceTbDebugScenarioAutoRecoversAndSyncsAllSixItemsInOrder() = runBlocking {
        // Exact state from ~/tb-debug/tb_terminal_local.db:
        // Session #4 (CLOSED, PENDING, serverId = 8cd05741-...)
        // Expense #2 (CONFLICT, OFFLINE_OCCURRED_AT_INVALID, 16:37:26)
        // Session #5 (CLOSED, PENDING open+close, ACTIVE_SESSION_BLOCKING_OPEN)
        // Session #6 (OPEN, PENDING, ACTIVE_SESSION_BLOCKING_OPEN)
        // Transaction #8 (PENDING, 18:15:57)
        val s4Id = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "8cd05741-9eed-4c56-aa9f-6e0dcdef758b",
                clientGeneratedId = "SESSION-f8e020f4-f1b4-41e1-ba45-c08064992c07",
                deviceId = null,
                cashierUserId = CASHIER_USER_ID,
                status = "CLOSED",
                openedAt = 1_795_855_058_980L, // 15:37:38
                closedAt = 1_795_859_183_551L, // 16:46:23
                startingCash = ScaledDecimal2.ofWhole(500_000L),
                expectedCash = ScaledDecimal2.ofWhole(490_000L),
                actualCash = ScaledDecimal2.ofWhole(490_000L),
                difference = ScaledDecimal2.ZERO,
                syncStatus = SyncStatus.PENDING,
            )
        )
        val exp2Id = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "EXPENSE-76e0a03e-adec-4a74-a350-258a90f10f64",
                deviceId = DEVICE_ID,
                cashSessionLocalId = s4Id,
                cashSessionServerId = "8cd05741-9eed-4c56-aa9f-6e0dcdef758b",
                category = "CASH_OUT: Bensin Armada",
                description = "Bensin Armada",
                amount = ScaledDecimal2.ofWhole(10_000L),
                occurredAt = 1_795_858_646_234L, // 16:37:26
                syncStatus = SyncStatus.CONFLICT,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = exp2Id,
                operation = SyncOperation.CREATE,
                payloadJson = """{"clientGeneratedId":"EXPENSE-76e0a03e-adec-4a74-a350-258a90f10f64"}""",
                status = SyncStatus.CONFLICT,
                retryCount = 1,
                lastError = "Pelanggaran business rule: Waktu pengeluaran kas offline berada di masa depan",
                lastErrorCode = "OFFLINE_OCCURRED_AT_INVALID",
                createdAt = 1_795_858_646_234L,
                updatedAt = 1_795_861_775_804L,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = s4Id,
                operation = SyncOperation.UPDATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
                createdAt = 1_795_859_183_551L,
                updatedAt = 1_795_859_183_551L,
            )
        )

        val s5Id = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = null,
                clientGeneratedId = "SESSION-62e570ba-1b61-4e8d-b9c5-d0a5da8d7330",
                deviceId = DEVICE_ID,
                cashierUserId = CASHIER_USER_ID,
                status = "CLOSED",
                openedAt = 1_795_864_328_331L, // 18:12:08
                closedAt = 1_795_864_352_207L, // 18:12:32
                startingCash = ScaledDecimal2.ofWhole(500_000L),
                expectedCash = ScaledDecimal2.ofWhole(500_000L),
                actualCash = ScaledDecimal2.ofWhole(500_000L),
                difference = ScaledDecimal2.ZERO,
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = s5Id,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
                lastError = "Anda masih memiliki sesi kasir yang belum ditutup",
                lastErrorCode = CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN,
                createdAt = 1_795_864_328_331L,
                updatedAt = 1_795_873_593_658L,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = s5Id,
                operation = SyncOperation.UPDATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
                createdAt = 1_795_864_352_207L,
                updatedAt = 1_795_864_352_207L,
            )
        )

        val s6Id = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = null,
                clientGeneratedId = "SESSION-117bcf91-c3ef-4988-876c-c10774d015cf",
                deviceId = DEVICE_ID,
                cashierUserId = CASHIER_USER_ID,
                status = "OPEN",
                openedAt = 1_795_864_356_477L, // 18:12:36
                startingCash = ScaledDecimal2.ofWhole(500_000L),
                expectedCash = ScaledDecimal2.ofWhole(500_000L),
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = s6Id,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
                lastError = "Anda masih memiliki sesi kasir yang belum ditutup",
                lastErrorCode = CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN,
                createdAt = 1_795_864_356_477L,
                updatedAt = 1_795_873_593_725L,
            )
        )

        val tx8Id = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = "TRX-efc625c2-36bc-46cb-bd90-0fee5c4e0789",
                deviceId = DEVICE_ID,
                transactionCode = "TRX-20261128-87DA63B7",
                cashSessionLocalId = s6Id,
                cashSessionServerId = "",
                cashierUserId = CASHIER_USER_ID,
                status = "PAID",
                subtotal = ScaledDecimal2.ofWhole(40_000L),
                total = ScaledDecimal2.ofWhole(40_000L),
                paidAmount = ScaledDecimal2.ofWhole(40_000L),
                remainingAmount = ScaledDecimal2.ZERO,
                occurredAt = 1_795_864_557_222L, // 18:15:57
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.transactionItemDao().upsert(
            LocalTransactionItemEntity(
                transactionLocalId = tx8Id,
                productServerId = "3b175dec-bd6e-4fed-9ecb-58a30f21a28c",
                productNameSnapshot = "Semen Merdeka",
                quantity = ScaledDecimal2.ofWhole(1L),
                priceAtTransaction = ScaledDecimal2.ofWhole(40_000L),
                cogsAtTransaction = ScaledDecimal2.ofWhole(38_000L),
                subtotal = ScaledDecimal2.ofWhole(40_000L),
            )
        )
        database.paymentDao().upsert(
            LocalPaymentEntity(
                clientGeneratedId = "PAY-8",
                transactionLocalId = tx8Id,
                method = "tunai",
                amount = ScaledDecimal2.ofWhole(40_000L),
                paidAt = 1_795_864_557_222L,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = tx8Id,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
                createdAt = 1_795_864_557_224L,
                updatedAt = 1_795_864_557_224L,
            )
        )

        // Prior stale lastSuccessfulSyncAt should NOT advance if 0 items succeeded
        val staleSyncAt = 1_795_854_783_547L
        settings.setLastSuccessfulSyncAt(CASHIER_USER_ID, staleSyncAt)

        // Readiness calibrates clock at 20:46:33 (+07:00)
        val serverNow = 1_795_873_593_758L
        val baseElapsed = elapsedMillis
        readinessTimeProvider = {
            val currentServer = serverNow + (elapsedMillis - baseElapsed)
            Instant.ofEpochMilli(currentServer)
                .atZone(ZoneId.of("Asia/Jakarta"))
                .toOffsetDateTime()
                .toString()
        }

        // Enqueue 6 server responses in exact dependency order:
        // 1. Expense #2 -> CREATED
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverExpenseId":"srv-exp-2","syncedAt":"2026-11-28T20:46:34+07:00"}}"""
            )
        )
        // 2. Close Session #4 -> UPDATED
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"data":{"syncStatus":"UPDATED","serverCashSessionId":"8cd05741-9eed-4c56-aa9f-6e0dcdef758b","closedAt":"2026-11-28T16:46:23+07:00","syncedAt":"2026-11-28T20:46:34+07:00"}}"""
            )
        )
        // 3. Open Session #5 -> CREATED
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-sess-5","openedAt":"2026-11-28T18:12:08+07:00","syncedAt":"2026-11-28T20:46:34+07:00"}}"""
            )
        )
        // 4. Close Session #5 -> UPDATED
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"success":true,"data":{"syncStatus":"UPDATED","serverCashSessionId":"srv-sess-5","closedAt":"2026-11-28T18:12:32+07:00","syncedAt":"2026-11-28T20:46:35+07:00"}}"""
            )
        )
        // 5. Open Session #6 -> CREATED
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverCashSessionId":"srv-sess-6","openedAt":"2026-11-28T18:12:36+07:00","syncedAt":"2026-11-28T20:46:35+07:00"}}"""
            )
        )
        // 6. Transaction #8 -> CREATED
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"success":true,"data":{"syncStatus":"CREATED","serverTransactionId":"srv-tx-8","receiptId":"RCP-srv-tx-8","serverPaymentIds":["srv-pay-8"],"syncedAt":"2026-11-28T20:46:35+07:00"}}"""
            )
        )

        val result = syncManager.runOnce(includeFailed = false) as SyncResult.Success
        assertEquals(6, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(6, server.requestCount)

        val req1 = server.takeRequest()
        val req2 = server.takeRequest()
        val req3 = server.takeRequest()
        val req4 = server.takeRequest()
        val req5 = server.takeRequest()
        val req6 = server.takeRequest()

        assertEquals("/api/sales/cash-expenses/sync", req1.path)
        val req1Body = req1.body.readUtf8()
        assertTrue(req1Body.contains("EXPENSE-76e0a03e-adec-4a74-a350-258a90f10f64"))
        assertTrue(req1Body.contains("2026-11-28T16:37:26.234+07:00"))

        assertEquals("/api/sales/sessions/sync/close", req2.path)
        assertTrue(req2.body.readUtf8().contains("8cd05741-9eed-4c56-aa9f-6e0dcdef758b"))

        assertEquals("/api/sales/sessions/sync/open", req3.path)
        assertTrue(req3.body.readUtf8().contains("SESSION-62e570ba-1b61-4e8d-b9c5-d0a5da8d7330"))

        assertEquals("/api/sales/sessions/sync/close", req4.path)
        assertTrue(req4.body.readUtf8().contains("srv-sess-5"))

        assertEquals("/api/sales/sessions/sync/open", req5.path)
        assertTrue(req5.body.readUtf8().contains("SESSION-117bcf91-c3ef-4988-876c-c10774d015cf"))

        assertEquals("/api/sales/checkout/sync", req6.path)
        assertTrue(req6.body.readUtf8().contains("TRX-efc625c2-36bc-46cb-bd90-0fee5c4e0789"))

        // All local rows and queue items are now SYNCED
        assertEquals(SyncStatus.SYNCED, database.cashExpenseDao().getByLocalId(exp2Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(s4Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(s5Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.cashSessionDao().getByLocalId(s6Id)!!.syncStatus)
        assertEquals(SyncStatus.SYNCED, database.transactionDao().getByLocalId(tx8Id)!!.syncStatus)
        val remainingQueue = database.syncQueueDao().observeQueue().first().filter { it.status != SyncStatus.SYNCED }
        assertTrue(remainingQueue.isEmpty())

        // Audit log recorded and lastSuccessfulSyncAt updated
        val audits = database.localAuditLogDao().observeAuditLogs().first()
        assertTrue(audits.any { it.action == "AUTO_RECOVER_TIME_CONFLICT" && it.recordId == "EXPENSE-76e0a03e-adec-4a74-a350-258a90f10f64" })
        assertEquals(wallClockMillis, settings.getLastSuccessfulSyncAt(CASHIER_USER_ID))
    }

    @Test
    fun lastSuccessfulSyncAtUpdatesOnlyWhenSucceededOrAllOwnUnsyncedEmpty() = runBlocking {
        calibrateClockAt(1_795_873_593_000L)
        val initialSyncAt = 1_795_850_000_000L
        settings.setLastSuccessfulSyncAt(CASHIER_USER_ID, initialSyncAt)

        // Seed 1 CONFLICT item and 1 PENDING close session blocked by it -> 0 succeeded, 2 unsynced remain
        val sId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-sess-check",
                clientGeneratedId = "SESSION-check",
                cashierUserId = CASHIER_USER_ID,
                status = "CLOSED",
                openedAt = 1_795_855_000_000L,
                closedAt = 1_795_856_000_000L,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                actualCash = ScaledDecimal2.ofWhole(90_000L),
                syncStatus = SyncStatus.PENDING,
            )
        )
        val expId = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "EXP-BLOCKING",
                deviceId = DEVICE_ID,
                cashSessionLocalId = sId,
                cashSessionServerId = "srv-sess-check",
                category = "CASH_OUT: Bensin",
                description = "Bensin",
                amount = ScaledDecimal2.ofWhole(10_000L),
                occurredAt = 1_795_855_500_000L,
                syncStatus = SyncStatus.CONFLICT,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expId,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.CONFLICT,
                lastErrorCode = "OFFLINE_PRICE_CONFLICT",
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sId,
                operation = SyncOperation.UPDATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
            )
        )

        val blockedRun = syncManager.runOnce() as SyncResult.Success
        assertEquals(0, blockedRun.successCount)
        assertEquals(0, blockedRun.failedCount)
        // Must NOT overwrite lastSuccessfulSyncAt because 0 items succeeded and unsynced work remains!
        assertEquals(initialSyncAt, settings.getLastSuccessfulSyncAt(CASHIER_USER_ID))

        // Mark both synced -> empty queue -> now lastSuccessfulSyncAt updates
        database.cashExpenseDao().markSynced(expId, "srv-exp", SyncStatus.SYNCED, wallClockMillis, wallClockMillis)
        database.cashSessionDao().markCloseSynced(sId, SyncStatus.SYNCED, wallClockMillis, wallClockMillis)
        val emptyRun = syncManager.runOnce() as SyncResult.Success
        assertEquals(0, emptyRun.successCount)
        assertEquals(wallClockMillis, settings.getLastSuccessfulSyncAt(CASHIER_USER_ID))
    }

    private suspend fun calibrateClockAt(serverTimeMillis: Long) {
        val baseElapsed = elapsedMillis
        readinessTimeProvider = {
            val currentServer = serverTimeMillis + (elapsedMillis - baseElapsed)
            Instant.ofEpochMilli(currentServer)
                .atZone(ZoneId.of("Asia/Jakarta"))
                .toOffsetDateTime()
                .toString()
        }
        assertEquals(BackendStatus.CONNECTED, healthMonitor.refresh(force = true))
    }

    private companion object {
        const val CASHIER_USER_ID = "f56a34c7-f6ec-473f-a108-9b4e53c7df9c"
        const val DEVICE_ID = "android-0798efbc65875e43"
    }
}

private object RecoveryAlwaysOnlineConnectivity : NetworkConnectivity {
    private val flow = MutableStateFlow(true)
    override fun isOnline(): Boolean = true
    override fun observeOnline(): Flow<Boolean> = flow
}

private class RecoveryFakeTokenStore(
    private var user: SessionUser?,
) : TokenStore {
    override fun readAccessToken(): String? = "token-valid"
    override fun readRefreshToken(): String? = "refresh-valid"
    override fun saveAccessToken(token: String) = Unit
    override fun saveTokens(accessToken: String, refreshToken: String) = Unit
    override fun readSessionUser(): SessionUser? = user
    override fun saveSessionUser(user: SessionUser) { this.user = user }
    override fun clearAccessToken() { user = null }
}
