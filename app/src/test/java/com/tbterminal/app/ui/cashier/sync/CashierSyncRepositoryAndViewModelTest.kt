package com.tbterminal.app.ui.cashier.sync

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.PendingSubmissionScopes
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashExpenseEntity
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.entity.LocalTransactionEntity
import com.tbterminal.app.data.local.entity.PendingSubmissionEntity
import com.tbterminal.app.data.local.entity.SyncQueueEntity
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.local.model.ScaledDecimal2
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import com.tbterminal.app.data.repository.CashierClockWarningKind
import com.tbterminal.app.data.repository.CashierSyncItemKind
import com.tbterminal.app.data.repository.CashierSyncItemStatus
import com.tbterminal.app.data.repository.CashierSyncRepository
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.CashSessionActiveBlockingMatcher
import com.tbterminal.app.data.sync.NetworkConnectivity
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.data.sync.SyncNowScheduleResult
import com.tbterminal.app.data.sync.SyncRunOutcome
import com.tbterminal.app.data.sync.SyncRunSummary
import com.tbterminal.app.data.sync.TrustedOfflineClock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CashierSyncRepositoryAndViewModelTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var settings: LocalAppSettingsDataSource
    private lateinit var sessionManager: SessionManager
    private lateinit var onlineFlow: MutableStateFlow<Boolean>
    private lateinit var healthMonitor: BackendHealthMonitor
    private lateinit var offlineStatusRepository: OfflineStatusRepository
    private lateinit var repository: CashierSyncRepository

    private var fakeClockMillis = 1_795_000_000_000L
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = LocalAppSettingsDataSource(database.appSettingDao())
        runBlocking { settings.setDataMode(AppDataMode.SYNC_SERVER) }

        sessionManager = SessionManager(
            FakeSyncTokenStore(
                user = SessionUser(
                    name = "Kasir A",
                    role = "KASIR",
                    isActive = true,
                    joinedAt = "2026-01-01",
                    lastLoginAt = null,
                    userId = "kasir-a",
                )
            )
        )
        onlineFlow = MutableStateFlow(true)
        val connectivity = object : NetworkConnectivity {
            override fun isOnline(): Boolean = onlineFlow.value
            override fun observeOnline(): Flow<Boolean> = onlineFlow
        }
        healthMonitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> =
                    Response.success(HealthResponseDto(status = "ready", database = "up"))
            },
            networkMonitor = connectivity,
            throttleMillis = 0L,
            timeoutMillis = 1_000L,
        )
        healthMonitor.markConnected()

        offlineStatusRepository = OfflineStatusRepository(
            networkMonitor = connectivity,
            backendHealthMonitor = healthMonitor,
            localAppSettingsDataSource = settings,
            syncQueueDao = database.syncQueueDao(),
            cashSessionDao = database.cashSessionDao(),
            transactionDao = database.transactionDao(),
            cashExpenseDao = database.cashExpenseDao(),
            activeUserIdFlow = sessionManager.activeUserIdFlow,
            pendingSubmissionDao = database.pendingSubmissionDao(),
            clockMillis = { fakeClockMillis },
            monotonicClockMillis = { fakeClockMillis },
            bootCountProvider = { 1L },
        )

        repository = CashierSyncRepository(
            offlineStatusRepository = offlineStatusRepository,
            syncQueueDao = database.syncQueueDao(),
            cashSessionDao = database.cashSessionDao(),
            transactionDao = database.transactionDao(),
            cashExpenseDao = database.cashExpenseDao(),
            pendingSubmissionDao = database.pendingSubmissionDao(),
            localAppSettingsDataSource = settings,
            activeUserIdFlow = sessionManager.activeUserIdFlow,
            activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
            clockMillis = { fakeClockMillis },
            monotonicClockMillis = { fakeClockMillis },
            bootCountProvider = { 1L },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun repositoryIsolatesCashiersCountsFourBucketsAndFormatsIndonesianErrors() = runBlocking {
        withTimeout(10_000L) {
            // Kasir A data: 1 PENDING open session, 1 FAILED tx, 1 SYNCING expense, 1 AMBIGUOUS refund, 1 SYNCED tx (excluded)
        val sessA = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-a",
                cashierUserId = "kasir-a",
                status = "OPEN",
                startingCash = ScaledDecimal2.ofWhole(150_000L),
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sessA,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
            )
        )
        val txFailedA = database.transactionDao().upsert(
            LocalTransactionEntity(
                clientGeneratedId = "tx-a-failed",
                transactionCode = "TRX-A-01",
                cashSessionLocalId = sessA,
                cashierUserId = "kasir-a",
                status = "PAID",
                total = ScaledDecimal2.ofWhole(75_000L),
                paidAmount = ScaledDecimal2.ofWhole(75_000L),
                syncStatus = SyncStatus.FAILED,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityLocalId = txFailedA,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.FAILED,
                lastError = "OFFLINE_PRICE_CONFLICT",
                lastErrorCode = "OFFLINE_PRICE_CONFLICT",
            )
        )
        val expA = database.cashExpenseDao().upsert(
            LocalCashExpenseEntity(
                clientGeneratedId = "exp-a-syncing",
                cashSessionLocalId = sessA,
                category = "CASH_OUT: Bensin",
                description = "Bensin kirim barang",
                amount = ScaledDecimal2.ofWhole(30_000L),
                syncStatus = SyncStatus.SYNCING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_EXPENSE,
                entityLocalId = expA,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.SYNCING,
            )
        )
        database.pendingSubmissionDao().upsert(
            PendingSubmissionEntity(
                ownerUserId = "kasir-a",
                scope = PendingSubmissionScopes.CASHIER_REFUND,
                idempotencyKey = "idem-refund-a",
                intentJson = """{"transactionId":"tx-1","reason":"Barang rusak","dispositionName":"RETURN_TO_STOCK"}""",
                status = PendingSubmissionStatus.AMBIGUOUS.name,
                createdAt = fakeClockMillis - 10_000L,
                updatedAt = fakeClockMillis - 10_000L,
                createdAtElapsedMillis = fakeClockMillis - 10_000L,
                bootCount = 1L,
            )
        )
        // Expired pending submission for kasir-a (> 72h) must be ignored
        database.pendingSubmissionDao().upsert(
            PendingSubmissionEntity(
                ownerUserId = "kasir-a",
                scope = PendingSubmissionScopes.CASHIER_VOID,
                idempotencyKey = "idem-void-expired",
                intentJson = """{"transactionId":"tx-old","reason":"Batal"}""",
                status = PendingSubmissionStatus.AMBIGUOUS.name,
                createdAt = fakeClockMillis - 80L * 3_600_000L,
                updatedAt = fakeClockMillis - 80L * 3_600_000L,
                createdAtElapsedMillis = fakeClockMillis - 80L * 3_600_000L,
                bootCount = 1L,
            )
        )

        // Kasir B data must NOT appear in Kasir A snapshot
        val sessB = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-b",
                cashierUserId = "kasir-b",
                status = "OPEN",
                startingCash = ScaledDecimal2.ofWhole(200_000L),
                syncStatus = SyncStatus.PENDING,
            )
        )
        database.syncQueueDao().enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.CASH_SESSION,
                entityLocalId = sessB,
                operation = SyncOperation.CREATE,
                payloadJson = "{}",
                status = SyncStatus.PENDING,
            )
        )
        database.pendingSubmissionDao().upsert(
            PendingSubmissionEntity(
                ownerUserId = "kasir-b",
                scope = PendingSubmissionScopes.CASHIER_PAY_DEBT,
                idempotencyKey = "idem-pay-b",
                intentJson = """{"transactionId":"tx-b","amountInput":"50000","method":"tunai","fingerprint":"fp"}""",
                status = PendingSubmissionStatus.AMBIGUOUS.name,
                createdAt = fakeClockMillis - 5_000L,
                updatedAt = fakeClockMillis - 5_000L,
                createdAtElapsedMillis = fakeClockMillis - 5_000L,
                bootCount = 1L,
            )
        )

        settings.setLastSuccessfulSyncAt("kasir-a", fakeClockMillis - 60_000L)

        val snapshot = repository.observeSnapshot().first { it.items.size == 4 }
        assertTrue(snapshot.isServerOnline)
        assertEquals(fakeClockMillis - 60_000L, snapshot.lastSuccessfulSyncAtMillis)
        assertEquals(1, snapshot.pendingCount)
        assertEquals(1, snapshot.sendingCount)
        assertEquals(1, snapshot.failedCount)
        assertEquals(1, snapshot.needsCheckCount)
        assertEquals(1, snapshot.otherUsersPendingSyncCount)

        val failedItem = snapshot.items.first { it.status == CashierSyncItemStatus.FAILED }
        assertEquals(CashierSyncItemKind.TRANSACTION, failedItem.kind)
        assertTrue(failedItem.summary.contains("TRX-A-01"))
        assertNotNull(failedItem.failureReason)
        assertTrue(failedItem.failureReason!!.contains("Harga produk di server berubah"))

        val refundItem = snapshot.items.first { it.isPendingSubmission }
        assertEquals(CashierSyncItemKind.REFUND_TRANSACTION, refundItem.kind)
        assertEquals(CashierSyncItemStatus.NEEDS_CHECK, refundItem.status)
        assertTrue(refundItem.summary.contains("Barang rusak"))

            val statusBadge = offlineStatusRepository.status.first {
                it.pendingSyncCount == 3 && it.pendingSubmissionCount == 1
            }
            assertEquals(1, statusBadge.pendingSubmissionCount)
            assertEquals(4, statusBadge.totalSyncBadgeCount)
        }
    }

    @Test
    fun viewModelBlocksSyncWhenServerOfflineAndShowsSummaryAfterManualSync() = runBlocking {
        withTimeout(10_000L) {
            val runner = FakeCashierSyncRunner()
            val store = ViewModelStore()
            try {
                val viewModel = createViewModel(store, runner)
                viewModel.uiState.first { !it.isInitialLoading && it.isServerOnline }

                // Server goes offline -> tap must NOT enqueue anything
                healthMonitor.markUnreachable()
                viewModel.uiState.first { !it.isServerOnline }

                viewModel.onSyncNowClick()
                val offlineState = viewModel.uiState.first { it.feedbackMessage?.contains("Server sedang offline") == true }
                assertEquals(0, runner.triggerCalls)
                assertFalse(offlineState.isSyncing)

                // Server back online -> tap triggers sync, disables button, then displays summary
                healthMonitor.markConnected()
                viewModel.uiState.first { it.isServerOnline }

                viewModel.onSyncNowClick()
                viewModel.uiState.first { it.isSyncing }
                assertEquals(1, runner.triggerCalls)

                runner.completeRun(
                    SyncRunSummary(
                        runId = 1L,
                        ownerUserId = "kasir-a",
                        includedFailed = true,
                        syncedCount = 4,
                        failedCount = 1,
                        outcome = SyncRunOutcome.SUCCESS,
                        completedAtMillis = fakeClockMillis,
                    )
                )

                val finishedState = viewModel.uiState.first {
                    !it.isSyncing && it.feedbackMessage == "Sinkron selesai: berhasil 4, gagal 1."
                }
                assertFalse(finishedState.isSyncing)
                assertEquals("Sinkron selesai: berhasil 4, gagal 1.", finishedState.feedbackMessage)
            } finally {
                store.clear()
            }
        }
    }

    @Test
    fun viewModelTapWhileAutoSyncRunningShowsProgressAndPromptsRetryIfFailedRemains() = runBlocking {
        withTimeout(10_000L) {
            val txFailedA = database.transactionDao().upsert(
                LocalTransactionEntity(
                    clientGeneratedId = "tx-failed-hint",
                    transactionCode = "TRX-FAIL",
                    cashierUserId = "kasir-a",
                    status = "PAID",
                    total = ScaledDecimal2.ofWhole(10_000L),
                    paidAmount = ScaledDecimal2.ofWhole(10_000L),
                    syncStatus = SyncStatus.FAILED,
                )
            )
            database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.TRANSACTION,
                    entityLocalId = txFailedA,
                    operation = SyncOperation.CREATE,
                    payloadJson = "{}",
                    status = SyncStatus.FAILED,
                    lastError = "Validasi gagal",
                )
            )

            val runner = FakeCashierSyncRunner()
            runner.setRunning(true)
            val store = ViewModelStore()
            try {
                val viewModel = createViewModel(store, runner)
                viewModel.uiState.first { !it.isInitialLoading && it.failedCount == 1 && it.isSyncing }

                // Tap while auto-sync is already running -> do NOT trigger new work, show "Sedang sinkron…"
                viewModel.onSyncNowClick()
                val progressState = viewModel.uiState.first { it.feedbackMessage == "Sedang sinkron…" }
                assertEquals(0, runner.triggerCalls)
                assertEquals("Sedang sinkron…", progressState.feedbackMessage)
                assertTrue(progressState.isSyncing)

                // Running auto-sync (includedFailed = false) finishes, and 1 FAILED item remains
                runner.completeRun(
                    SyncRunSummary(
                        runId = 1L,
                        ownerUserId = "kasir-a",
                        includedFailed = false,
                        syncedCount = 2,
                        failedCount = 0,
                        outcome = SyncRunOutcome.SUCCESS,
                        completedAtMillis = fakeClockMillis,
                    )
                )

                val finishedState = viewModel.uiState.first {
                    !it.isSyncing && it.feedbackMessage?.startsWith("Sinkron selesai") == true
                }
                assertFalse(finishedState.isSyncing)
                assertEquals(
                    "Sinkron selesai: berhasil 2, gagal 0. Tekan lagi untuk mencoba ulang data yang gagal.",
                    finishedState.feedbackMessage,
                )
            } finally {
                store.clear()
            }
        }
    }

    @Test
    fun blockingChainAttributesFiveBlockedItemsToRootConflictAndShowsBlockedFeedback() = runBlocking {
        withTimeout(10_000L) {
            val s4 = database.cashSessionDao().upsert(
                LocalCashSessionEntity(
                    serverId = "srv-s4",
                    clientGeneratedId = "sess-4",
                    cashierUserId = "kasir-a",
                    status = "CLOSED",
                    openedAt = fakeClockMillis - 3_600_000L,
                    closedAt = fakeClockMillis - 1_800_000L,
                    startingCash = ScaledDecimal2.ofWhole(500_000L),
                    actualCash = ScaledDecimal2.ofWhole(490_000L),
                    syncStatus = SyncStatus.PENDING,
                )
            )
            val exp2 = database.cashExpenseDao().upsert(
                LocalCashExpenseEntity(
                    clientGeneratedId = "exp-2",
                    cashSessionLocalId = s4,
                    cashSessionServerId = "srv-s4",
                    category = "CASH_OUT: Bensin Armada",
                    description = "Bensin Armada",
                    amount = ScaledDecimal2.ofWhole(10_000L),
                    occurredAt = fakeClockMillis - 2_000_000L,
                    syncStatus = SyncStatus.CONFLICT,
                )
            )
            val qExp = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.CASH_EXPENSE,
                    entityLocalId = exp2,
                    operation = SyncOperation.CREATE,
                    payloadJson = "{}",
                    status = SyncStatus.CONFLICT,
                    lastErrorCode = "OFFLINE_OCCURRED_AT_INVALID",
                )
            )
            val qClose4 = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.CASH_SESSION,
                    entityLocalId = s4,
                    operation = SyncOperation.UPDATE,
                    payloadJson = "{}",
                    status = SyncStatus.PENDING,
                )
            )
            val s5 = database.cashSessionDao().upsert(
                LocalCashSessionEntity(
                    clientGeneratedId = "sess-5",
                    cashierUserId = "kasir-a",
                    status = "CLOSED",
                    openedAt = fakeClockMillis - 1_200_000L,
                    closedAt = fakeClockMillis - 1_100_000L,
                    startingCash = ScaledDecimal2.ofWhole(500_000L),
                    actualCash = ScaledDecimal2.ofWhole(500_000L),
                    syncStatus = SyncStatus.PENDING,
                )
            )
            val qOpen5 = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.CASH_SESSION,
                    entityLocalId = s5,
                    operation = SyncOperation.CREATE,
                    payloadJson = "{}",
                    status = SyncStatus.PENDING,
                    lastErrorCode = CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN,
                )
            )
            val qClose5 = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.CASH_SESSION,
                    entityLocalId = s5,
                    operation = SyncOperation.UPDATE,
                    payloadJson = "{}",
                    status = SyncStatus.PENDING,
                )
            )
            val s6 = database.cashSessionDao().upsert(
                LocalCashSessionEntity(
                    clientGeneratedId = "sess-6",
                    cashierUserId = "kasir-a",
                    status = "OPEN",
                    openedAt = fakeClockMillis - 900_000L,
                    startingCash = ScaledDecimal2.ofWhole(500_000L),
                    syncStatus = SyncStatus.PENDING,
                )
            )
            val qOpen6 = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.CASH_SESSION,
                    entityLocalId = s6,
                    operation = SyncOperation.CREATE,
                    payloadJson = "{}",
                    status = SyncStatus.PENDING,
                    lastErrorCode = CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN,
                )
            )
            val tx8 = database.transactionDao().upsert(
                LocalTransactionEntity(
                    clientGeneratedId = "tx-8",
                    transactionCode = "TRX-8",
                    cashSessionLocalId = s6,
                    cashierUserId = "kasir-a",
                    status = "PAID",
                    total = ScaledDecimal2.ofWhole(40_000L),
                    paidAmount = ScaledDecimal2.ofWhole(40_000L),
                    occurredAt = fakeClockMillis - 600_000L,
                    syncStatus = SyncStatus.PENDING,
                )
            )
            val qTx8 = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.TRANSACTION,
                    entityLocalId = tx8,
                    operation = SyncOperation.CREATE,
                    payloadJson = "{}",
                    status = SyncStatus.PENDING,
                )
            )

            val snapshot = repository.observeSnapshot().first { it.items.size == 6 }
            assertEquals(5, snapshot.blockedCount)
            val expItem = snapshot.items.first { it.id == "queue-$qExp" }
            assertTrue(expItem.isConflict)
            assertEquals(5, expItem.blockingCount)
            assertEquals(null, expItem.blockedByLabel)

            listOf(qClose4, qOpen5, qClose5, qOpen6, qTx8).forEach { queueId ->
                val dependent = snapshot.items.first { it.id == "queue-$queueId" }
                assertEquals(0, dependent.blockingCount)
                assertEquals("Pengeluaran Kas Offline · Bensin Armada", dependent.blockedByLabel)
            }

            val runner = FakeCashierSyncRunner()
            val store = ViewModelStore()
            try {
                val viewModel = createViewModel(store, runner)
                viewModel.uiState.first { !it.isInitialLoading && it.blockedCount == 5 }
                viewModel.onSyncNowClick()
                runner.completeRun(
                    SyncRunSummary(
                        runId = 1L,
                        ownerUserId = "kasir-a",
                        includedFailed = true,
                        syncedCount = 0,
                        failedCount = 0,
                        outcome = SyncRunOutcome.SUCCESS,
                        completedAtMillis = fakeClockMillis,
                    )
                )
                val finishedState = viewModel.uiState.first {
                    !it.isSyncing && it.feedbackMessage?.startsWith("Belum ada data yang terkirim") == true
                }
                assertEquals(
                    "Belum ada data yang terkirim. 5 data tertahan oleh 1 data yang perlu dicek.",
                    finishedState.feedbackMessage,
                )
                assertTrue(finishedState.isFeedbackError)
            } finally {
                store.clear()
            }
        }
    }

    @Test
    fun deferredFutureClockItemShowsHhmmMessageAndRepositoryEmitsClockWarnings() = runBlocking {
        withTimeout(10_000L) {
            var wallMillis = fakeClockMillis + 3_600_000L // +1h skew
            var elapsedMillis = 100_000L
            var currentBoot = 1L
            val trustedClock = TrustedOfflineClock(
                localAppSettingsDataSource = settings,
                wallClockMillis = { wallMillis },
                elapsedRealtimeMillis = { elapsedMillis },
                bootCountProvider = { currentBoot },
            )
            trustedClock.recordServerSample(
                serverTimeIso = Instant.ofEpochMilli(fakeClockMillis).toString(),
                requestElapsedMillis = 100_000L,
                responseElapsedMillis = 100_000L,
            )

            val clockRepo = CashierSyncRepository(
                offlineStatusRepository = offlineStatusRepository,
                syncQueueDao = database.syncQueueDao(),
                cashSessionDao = database.cashSessionDao(),
                transactionDao = database.transactionDao(),
                cashExpenseDao = database.cashExpenseDao(),
                pendingSubmissionDao = database.pendingSubmissionDao(),
                localAppSettingsDataSource = settings,
                activeUserIdFlow = sessionManager.activeUserIdFlow,
                activeUserIdProvider = { sessionManager.resolveOwnerUserId() },
                clockMillis = { wallMillis },
                monotonicClockMillis = { elapsedMillis },
                bootCountProvider = { currentBoot },
                trustedOfflineClock = trustedClock,
            )

            val sess = database.cashSessionDao().upsert(
                LocalCashSessionEntity(
                    serverId = "srv-clock",
                    clientGeneratedId = "sess-clock",
                    cashierUserId = "kasir-a",
                    status = "OPEN",
                    openedAt = fakeClockMillis - 600_000L,
                    startingCash = ScaledDecimal2.ofWhole(100_000L),
                    syncStatus = SyncStatus.SYNCED,
                )
            )
            val futureOccurredAt = fakeClockMillis + 300_000L
            val expId = database.cashExpenseDao().upsert(
                LocalCashExpenseEntity(
                    clientGeneratedId = "exp-future-ui",
                    cashSessionLocalId = sess,
                    cashSessionServerId = "srv-clock",
                    category = "CASH_OUT: Bensin",
                    description = "Bensin",
                    amount = ScaledDecimal2.ofWhole(10_000L),
                    occurredAt = futureOccurredAt,
                    syncStatus = SyncStatus.PENDING,
                )
            )
            val qId = database.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.CASH_EXPENSE,
                    entityLocalId = expId,
                    operation = SyncOperation.CREATE,
                    payloadJson = "{}",
                    status = SyncStatus.PENDING,
                    lastError = "Waktu pengeluaran kas di masa depan",
                    lastErrorCode = "OFFLINE_OCCURRED_AT_INVALID",
                )
            )

            val expectedHhmm = Instant.ofEpochMilli(futureOccurredAt + 60_000L)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID")))

            val skewedSnapshot = clockRepo.observeSnapshot().first { it.items.size == 1 }
            assertEquals(CashierClockWarningKind.DEVICE_CLOCK_SKEWED, skewedSnapshot.clockWarning?.kind)
            val item = skewedSnapshot.items.first { it.id == "queue-$qId" }
            assertTrue(item.isDeferredByClock)
            assertEquals(
                "Jam data lebih maju dari jam server. Akan dikirim otomatis setelah $expectedHhmm.",
                item.failureReason,
            )

            // Simulate reboot -> UNCALIBRATED_AFTER_REBOOT warning
            currentBoot = 2L
            elapsedMillis = 1_000L
            wallMillis = fakeClockMillis
            trustedClock.recordServerSample(
                serverTimeIso = Instant.ofEpochMilli(fakeClockMillis).toString(),
                requestElapsedMillis = 1_000L,
                responseElapsedMillis = 1_000L,
            )
            currentBoot = 3L
            elapsedMillis = 500L
            val rebootSnapshot = clockRepo.observeSnapshot().first {
                it.clockWarning?.kind == CashierClockWarningKind.UNCALIBRATED_AFTER_REBOOT
            }
            assertEquals(
                CashierClockWarningKind.UNCALIBRATED_AFTER_REBOOT,
                rebootSnapshot.clockWarning?.kind,
            )
        }
    }

    private fun createViewModel(
        store: ViewModelStore,
        runner: CashierSyncRunner,
    ): CashierSyncViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CashierSyncViewModel(repository = repository, runner = runner) as T
            }
        }
        return ViewModelProvider(store, factory)[CashierSyncViewModel::class.java]
    }

    private class FakeCashierSyncRunner : CashierSyncRunner {
        private val runningFlow = MutableStateFlow(false)
        private val summaryFlow = MutableStateFlow<SyncRunSummary?>(null)
        var triggerCalls = 0

        override val isAnySyncRunning: Flow<Boolean> = runningFlow
        override val lastRunSummary: Flow<SyncRunSummary?> = summaryFlow

        override suspend fun triggerManualSyncNow(): SyncNowScheduleResult {
            triggerCalls += 1
            runningFlow.value = true
            return SyncNowScheduleResult.ENQUEUED
        }

        override suspend fun refreshServerStatus(): BackendStatus = BackendStatus.CONNECTED

        fun setRunning(running: Boolean) {
            runningFlow.value = running
        }

        fun completeRun(summary: SyncRunSummary) {
            summaryFlow.value = summary
            runningFlow.value = false
        }
    }

    private class FakeSyncTokenStore(
        private var user: SessionUser?,
    ) : TokenStore {
        override fun readAccessToken(): String? = "token"
        override fun readRefreshToken(): String? = "refresh"
        override fun saveAccessToken(token: String) = Unit
        override fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override fun readSessionUser(): SessionUser? = user
        override fun saveSessionUser(user: SessionUser) { this.user = user }
        override fun clearAccessToken() { user = null }
    }
}
