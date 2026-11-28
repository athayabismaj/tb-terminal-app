package com.tbterminal.app.ui.cash

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashexpense.CashMovementType
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.entity.LocalCashSessionEntity
import com.tbterminal.app.data.local.mapper.toLocalCashSessionEntity
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
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.TransactionVoidResult
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import com.tbterminal.app.data.remote.PreDispatchIOException
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.CashSessionAdoptionPolicy
import com.tbterminal.app.data.sync.NetworkConnectivity
import com.tbterminal.app.data.sync.OfflineStatusRepository
import java.io.EOFException
import java.io.IOException
import java.math.BigDecimal
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class CashReconciliationViewModelOfflineTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: TbTerminalDatabase
    private lateinit var cashSessionLocalDataSource: CashSessionLocalDataSource
    private lateinit var localCashSessionService: LocalCashSessionService
    private lateinit var localCashExpenseService: LocalCashExpenseService
    private lateinit var backendHealthMonitor: BackendHealthMonitor
    private lateinit var offlineStatusRepository: OfflineStatusRepository
    private var readinessCalls = 0
    private var readinessResponder: suspend () -> Response<HealthResponseDto> = {
        Response.success(HealthResponseDto(status = "ready", database = "up"))
    }
    private var nowMillis = 1_800_000_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        readinessCalls = 0
        readinessResponder = {
            Response.success(HealthResponseDto(status = "ready", database = "up"))
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { command -> command.run() }
            .setTransactionExecutor { command -> command.run() }
            .build()
        cashSessionLocalDataSource = CashSessionLocalDataSource(
            cashSessionDao = database.cashSessionDao(),
            transactionDao = database.transactionDao(),
            cashExpenseDao = database.cashExpenseDao(),
            syncQueueDao = database.syncQueueDao(),
        )
        localCashSessionService = LocalCashSessionService(database, "device-test", roleProvider = { "KASIR" })
        localCashExpenseService = LocalCashExpenseService(database, "device-test")
        val connectivity = object : NetworkConnectivity {
            private val flow = MutableStateFlow(true)
            override fun isOnline(): Boolean = true
            override fun observeOnline(): Flow<Boolean> = flow
        }
        backendHealthMonitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> {
                    readinessCalls++
                    return readinessResponder()
                }
            },
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
    fun loadCashOnline_mirrorsSessionToRoom_andCloseOrPettyCashFallsBackWhenServerUnavailable() = runTest(dispatcher) {
        backendHealthMonitor.refresh(force = true)
        val remoteSession = CashSession(
            id = "srv-cash-uuid-1",
            userId = "kasir-1",
            userName = "Kasir Satu",
            openedAt = "2026-11-27T08:00:00+07:00",
            closedAt = null,
            openingCash = BigDecimal("250000"),
            closingCash = null,
            systemCash = BigDecimal("250000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
        )
        val repo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(remoteSession)
        }
        val vm = buildVm(repo)
        advanceUntilIdle()

        val mirrored = cashSessionLocalDataSource.getCachedCashSessionByServerId("srv-cash-uuid-1")
        assertNotNull(mirrored)
        assertEquals(mirrored!!.localId, vm.uiState.value.localCashSessionId)
        assertEquals("srv-cash-uuid-1", vm.uiState.value.activeSession?.id)

        // HTTP 400 on closeSession while connected does NOT fallback
        repo.closeSessionResult = RepositoryResult.Error("HTTP_400", "Bad request")
        vm.onClosingCashChanged("250000")
        vm.closeSession()
        advanceUntilIdle()
        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)
        assertEquals("OPEN", cashSessionLocalDataSource.getCachedCashSessionByServerId("srv-cash-uuid-1")?.status)

        // HTTP 500 on petty cash stays online error (no local fallback)
        repo.addExpenseResult = RepositoryResult.Error("HTTP_500", "Internal error")
        vm.showPettyCashDialog(CashMovementType.CASH_OUT)
        vm.onPettyCashAmountChanged("15000")
        vm.onPettyCashDescriptionChanged("Parkir")
        vm.addPettyCash()
        advanceUntilIdle()
        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)
        assertTrue(localCashExpenseService.getLocalExpenses(mirrored.localId).isEmpty())

        // CONNECTION_FAILED on petty cash falls back to local movement on mirrored session
        repo.addExpenseResult = RepositoryResult.Error("CONNECTION_FAILED", "Koneksi bermasalah")
        vm.onPettyCashAmountChanged("15000")
        vm.onPettyCashDescriptionChanged("Parkir")
        vm.addPettyCash()
        advanceUntilIdle()
        assertEquals(BackendStatus.UNREACHABLE, backendHealthMonitor.status.value)
        assertEquals(1, localCashExpenseService.getLocalExpenses(mirrored.localId).size)

        // Restore CONNECTED + reload online state, then HTTP 503 on closeSession falls back & closes mirrored session
        backendHealthMonitor.markConnected()
        vm.loadCash()
        advanceUntilIdle()
        repo.closeSessionResult = RepositoryResult.Error("HTTP_503", "Gateway down")
        vm.onClosingCashChanged("235000")
        vm.closeSession()
        advanceUntilIdle()

        assertEquals(BackendStatus.UNREACHABLE, backendHealthMonitor.status.value)
        val closedLocal = cashSessionLocalDataSource.getCachedCashSessionByServerId("srv-cash-uuid-1")!!
        assertEquals("CLOSED", closedLocal.status)
        assertEquals(SyncStatus.PENDING, closedLocal.syncStatus)
        val closeQueue = database.syncQueueDao().getLatestForEntityAndOperation(
            entityLocalId = closedLocal.localId,
            entityType = SyncEntityType.CASH_SESSION,
            operation = SyncOperation.UPDATE,
        )
        assertNotNull(closeQueue)
        assertEquals(SyncStatus.PENDING, closeQueue?.status)
    }

    @Test
    fun openSession_whenServerUnavailable_reusesExistingOpenSessionOrOpensNewLocal_neverOnHttp500Or400() = runTest(dispatcher) {
        backendHealthMonitor.refresh(force = true)
        val repo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
            openSessionResult = RepositoryResult.Error("HTTP_500", "Server error")
        }
        val vm = buildVm(repo)
        advanceUntilIdle()

        // 1. HTTP 500 -> does NOT fallback
        vm.onOpeningCashChanged("100000")
        vm.openSession()
        advanceUntilIdle()
        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)
        assertFalse(vm.uiState.value.hasActiveSession)
        assertEquals(null, cashSessionLocalDataSource.getLatestOpenSession("kasir-1"))

        // 2. HTTP 503 with no existing session -> opens 1 local session
        repo.openSessionResult = RepositoryResult.Error("HTTP_503", "Service unavailable")
        vm.onOpeningCashChanged("100000")
        vm.openSession()
        advanceUntilIdle()

        assertEquals(BackendStatus.UNREACHABLE, backendHealthMonitor.status.value)
        assertTrue(vm.uiState.value.hasActiveSession)
        val created = cashSessionLocalDataSource.getLatestOpenSession("kasir-1")
        assertNotNull(created)

        // 3. Another openSession attempt while OPEN session already exists -> reuses existing, no duplicate
        val vm2 = buildVm(repo)
        advanceUntilIdle()
        backendHealthMonitor.markConnected()
        repo.openSessionResult = RepositoryResult.Error("CONNECTION_FAILED", "Koneksi bermasalah")
        vm2.onOpeningCashChanged("300000")
        vm2.openSession()
        advanceUntilIdle()

        assertEquals(created!!.localId, vm2.uiState.value.localCashSessionId)
        assertEquals(1, database.cashSessionDao().getUnsyncedOfflineSessions().size)
    }

    @Test
    fun mirrorRemoteSession_doesNotOverwriteUnsyncedClosedLocalSession() = runTest(dispatcher) {
        val repo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(
                CashSession(
                    id = "srv-close-protect",
                    userId = "kasir-1",
                    openedAt = "2026-04-08T01:00:00Z",
                    closedAt = null,
                    openingCash = BigDecimal("100000"),
                    closingCash = null,
                    systemCash = null,
                    difference = null,
                    totalExpenses = BigDecimal.ZERO,
                    notes = "shift pagi",
                    status = "OPEN"
                )
            )
        }
        val vm = buildVm(repo)
        advanceUntilIdle()

        val mirroredLocalId = vm.uiState.value.localCashSessionId!!
        database.cashSessionDao().closeSession(
            localId = mirroredLocalId,
            status = "CLOSED",
            closedAt = nowMillis + 1_000L,
            expectedCash = ScaledDecimal2.ofExact(BigDecimal("100000")),
            actualCash = ScaledDecimal2.ofExact(BigDecimal("100000")),
            difference = ScaledDecimal2.ZERO,
            closingNote = "tutup offline",
            syncStatus = SyncStatus.PENDING,
            updatedAt = nowMillis + 1_000L,
        )

        val remoteOpen = (repo.activeSessionResult as RepositoryResult.Success).data!!
        val existingBefore = database.cashSessionDao().getByLocalId(mirroredLocalId)!!
        val mapped = remoteOpen.toLocalCashSessionEntity(existingBefore)
        assertEquals("CLOSED", mapped.status)
        assertEquals(SyncStatus.PENDING, mapped.syncStatus)

        cashSessionLocalDataSource.mirrorRemoteSession(remoteOpen)
        val afterDirectMirror = database.cashSessionDao().getByLocalId(mirroredLocalId)!!
        assertEquals("CLOSED", afterDirectMirror.status)
        assertEquals(SyncStatus.PENDING, afterDirectMirror.syncStatus)

        vm.loadCash()
        advanceUntilIdle()
        val afterVmLoad = database.cashSessionDao().getByLocalId(mirroredLocalId)!!
        assertEquals("CLOSED", afterVmLoad.status)
        assertEquals(SyncStatus.PENDING, afterVmLoad.syncStatus)
        assertFalse(vm.uiState.value.hasActiveSession)
    }

    @Test
    fun closeSessionOnline_marksMirroredLocalSessionClosedAndSynced_soOfflineDoesNotRestoreOpen() = runTest(dispatcher) {
        val openRemote = CashSession(
            id = "srv-close-online",
            userId = "kasir-1",
            openedAt = "2026-04-08T01:00:00Z",
            closedAt = null,
            openingCash = BigDecimal("100000"),
            closingCash = null,
            systemCash = null,
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN"
        )
        val closedRemote = openRemote.copy(
            closedAt = "2026-04-08T09:00:00Z",
            closingCash = BigDecimal("125000"),
            systemCash = BigDecimal("125000"),
            difference = BigDecimal.ZERO,
            notes = "selesai",
            status = "CLOSED"
        )
        val repo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(openRemote)
            closeSessionResult = RepositoryResult.Success(closedRemote)
        }
        val vm = buildVm(repo)
        advanceUntilIdle()
        val mirroredLocalId = vm.uiState.value.localCashSessionId!!

        repo.activeSessionResult = RepositoryResult.Success(null)
        vm.onClosingCashChanged("125000")
        vm.onClosingNotesChanged("selesai")
        vm.closeSession()
        advanceUntilIdle()

        val localAfterOnlineClose = database.cashSessionDao().getByLocalId(mirroredLocalId)!!
        assertEquals("CLOSED", localAfterOnlineClose.status)
        assertEquals(SyncStatus.SYNCED, localAfterOnlineClose.syncStatus)

        repo.activeSessionResult = RepositoryResult.Error("CONNECTION_FAILED", "offline")
        vm.loadCash()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasActiveSession)
        assertFalse(vm.uiState.value.isUsingLocalActiveSession)
    }

    @Test
    fun reconcileNoActiveRemoteSession_closesSyncedMirrorButPreservesUnsyncedOfflineOpenSession() = runTest(dispatcher) {
        val repo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(
                CashSession(
                    id = "srv-stale-mirror",
                    userId = "kasir-1",
                    openedAt = "2026-04-08T01:00:00Z",
                    closedAt = null,
                    openingCash = BigDecimal("50000"),
                    closingCash = null,
                    systemCash = null,
                    difference = null,
                    totalExpenses = BigDecimal.ZERO,
                    notes = null,
                    status = "OPEN"
                )
            )
        }
        val vm = buildVm(repo)
        advanceUntilIdle()
        val syncedMirrorId = vm.uiState.value.localCashSessionId!!

        val offlineOpenId = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "offline-unsynced-open",
                cashierUserId = "kasir-1",
                status = "OPEN",
                openedAt = nowMillis + 2_000L,
                startingCash = ScaledDecimal2.ofExact(BigDecimal("75000")),
                syncStatus = SyncStatus.PENDING,
                createdAt = nowMillis + 2_000L,
                updatedAt = nowMillis + 2_000L,
            )
        )

        repo.activeSessionResult = RepositoryResult.Success(null)
        vm.loadCash()
        advanceUntilIdle()

        val mirrorAfter = database.cashSessionDao().getByLocalId(syncedMirrorId)!!
        val offlineAfter = database.cashSessionDao().getByLocalId(offlineOpenId)!!
        assertEquals("CLOSED", mirrorAfter.status)
        assertEquals(SyncStatus.SYNCED, mirrorAfter.syncStatus)
        assertEquals("OPEN", offlineAfter.status)
        assertEquals(SyncStatus.PENDING, offlineAfter.syncStatus)
    }

    @Test
    fun sharedDevice_isolatesLocalOpenSessionAndAgeAlertByCashierUserId() = runTest(dispatcher) {
        val expiredOpenedAt = nowMillis - 73L * 60L * 60L * 1000L
        val sessionKasirA = database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                clientGeneratedId = "sess-kasir-a-expired",
                deviceId = "android-dev-1",
                cashierUserId = "kasir-a",
                status = "OPEN",
                openedAt = expiredOpenedAt,
                startingCash = ScaledDecimal2.ofWhole(100_000L),
                syncStatus = SyncStatus.PENDING,
                createdAt = expiredOpenedAt,
                updatedAt = expiredOpenedAt,
            )
        )

        val offlineRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Error("CONNECTION_FAILED", "offline")
        }
        val vmKasirB = CashReconciliationViewModel(
            repository = offlineRepo,
            cashSessionLocalDataSource = cashSessionLocalDataSource,
            localCashSessionService = localCashSessionService,
            localCashExpenseService = localCashExpenseService,
            offlineStatusRepository = offlineStatusRepository,
            cashierUserId = "kasir-b",
            cashierNameFallback = "Kasir B",
            actorRole = "KASIR",
            clockMillis = { nowMillis },
        )
        advanceUntilIdle()

        // Kasir B tidak boleh melihat sesi kas lokal Kasir A ataupun peringatan umur sesi Kasir A
        assertFalse(vmKasirB.uiState.value.hasActiveSession)
        assertEquals(null, vmKasirB.uiState.value.offlineSessionAgeAlert)

        // Kasir B dapat membuka sesi kas lokalnya sendiri walaupun Kasir A masih punya sesi OPEN
        vmKasirB.onOpeningCashChanged("150000")
        vmKasirB.openSession()
        advanceUntilIdle()
        assertTrue(vmKasirB.uiState.value.hasActiveSession)
        assertTrue(vmKasirB.uiState.value.localCashSessionId != sessionKasirA)
        assertEquals("kasir-b", vmKasirB.uiState.value.activeSession?.userId)
    }

    @Test
    fun ambiguousErrorsOnExpenseAndOpenSession_doNotFallbackLocally_while502AndRefusedDo() = runTest(dispatcher) {
        backendHealthMonitor.refresh(force = true)
        val openSession = CashSession(
            id = "srv-cash-ambig-1",
            userId = "kasir-1",
            userName = "Kasir Satu",
            openedAt = "2026-11-27T08:00:00+07:00",
            closedAt = null,
            openingCash = BigDecimal("250000"),
            closingCash = null,
            systemCash = BigDecimal("250000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
        )
        val repo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(openSession)
        }
        val vm = buildVm(repo)
        advanceUntilIdle()
        val mirroredLocalId = vm.uiState.value.localCashSessionId!!

        // 1. Ambiguous errors on addPettyCash -> NO local expense created, existing error shown
        listOf(
            RepositoryResult.Error("NETWORK_TIMEOUT", "Request terlalu lama. Periksa koneksi lalu coba lagi."),
            RepositoryResult.Error("CONNECTION_INTERRUPTED", "Koneksi bermasalah. Periksa jaringan dan status server."),
            RepositoryResult.Error("HTTP_504", "Gateway timeout"),
            RepositoryResult.Exception(SocketTimeoutException("read timed out")),
            RepositoryResult.Exception(IOException("stream reset")),
        ).forEach { ambiguousResult ->
            backendHealthMonitor.markConnected()
            repo.addExpenseResult = ambiguousResult
            vm.showPettyCashDialog(CashMovementType.CASH_OUT)
            vm.onPettyCashAmountChanged("20000")
            vm.onPettyCashDescriptionChanged("Bensin")
            vm.addPettyCash()
            advanceUntilIdle()

            assertTrue(localCashExpenseService.getLocalExpenses(mirroredLocalId).isEmpty())
            assertTrue(vm.uiState.value.isPettyCashDialogOpen)
            assertNotNull(vm.uiState.value.errorMessage)

            // Retrying inside same ambiguous dialog does NOT auto-fallback locally even though health became UNREACHABLE
            vm.addPettyCash()
            advanceUntilIdle()
            assertTrue(localCashExpenseService.getLocalExpenses(mirroredLocalId).isEmpty())
        }

        // 2. Definitely unprocessed errors on addPettyCash (HTTP_502, NETWORK_UNAVAILABLE, ConnectException) -> DO fallback locally
        backendHealthMonitor.markConnected()
        repo.addExpenseResult = RepositoryResult.Error("HTTP_502", "Bad Gateway")
        vm.showPettyCashDialog(CashMovementType.CASH_OUT)
        vm.onPettyCashAmountChanged("15000")
        vm.onPettyCashDescriptionChanged("Tol")
        vm.addPettyCash()
        advanceUntilIdle()
        assertEquals(1, localCashExpenseService.getLocalExpenses(mirroredLocalId).size)

        backendHealthMonitor.markConnected()
        repo.addExpenseResult = RepositoryResult.Exception(ConnectException("Connection refused"))
        vm.showPettyCashDialog(CashMovementType.CASH_OUT)
        vm.onPettyCashAmountChanged("10000")
        vm.onPettyCashDescriptionChanged("Parkir")
        vm.addPettyCash()
        advanceUntilIdle()
        assertEquals(2, localCashExpenseService.getLocalExpenses(mirroredLocalId).size)

        // 3. Ambiguous errors on openSession -> wait on first attempt, then unlock local (with adoptRemoteIfActive=true) when readiness fails
        listOf(
            RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout"),
            RepositoryResult.Error("CONNECTION_INTERRUPTED", "Interrupted"),
            RepositoryResult.Error("HTTP_504", "Gateway timeout"),
            RepositoryResult.Exception(SocketTimeoutException("read timed out")),
        ).forEach { ambiguousResult ->
            database.openHelper.writableDatabase.execSQL("DELETE FROM sync_queue")
            database.openHelper.writableDatabase.execSQL("DELETE FROM local_cash_sessions")
            readinessResponder = { Response.success(HealthResponseDto(status = "ready", database = "up")) }
            val openRepo = FakeCashReconciliationOfflineRepo().apply {
                activeSessionResult = RepositoryResult.Success(null)
            }
            val openVm = buildVm(openRepo)
            advanceUntilIdle()

            backendHealthMonitor.markConnected()
            openRepo.openSessionResult = ambiguousResult
            openVm.onOpeningCashChanged("100000")
            openVm.openSession()
            advanceUntilIdle()

            assertFalse(openVm.uiState.value.hasActiveSession)
            assertEquals(null, cashSessionLocalDataSource.getLatestOpenSession("kasir-1"))
            assertNotNull(openVm.uiState.value.errorMessage)

            // Retry while readiness is STILL 200 stays on server check path (no premature local open)
            openVm.openSession()
            advanceUntilIdle()
            assertEquals(null, cashSessionLocalDataSource.getLatestOpenSession("kasir-1"))

            // Retry WITHOUT changing nominal when readiness confirms server unreachable -> opens locally with adoptRemoteIfActive=true
            readinessResponder = { throw EOFException("server down after ambiguous open") }
            val openCallsBeforeOfflineRetry = openRepo.openSessionCalls
            openVm.openSession()
            advanceUntilIdle()

            assertEquals(openCallsBeforeOfflineRetry, openRepo.openSessionCalls)
            assertTrue(openVm.uiState.value.hasActiveSession)
            val adoptedLocal = cashSessionLocalDataSource.getLatestOpenSession("kasir-1")
            assertNotNull(adoptedLocal)
            val openQueue = database.syncQueueDao().getLatestForEntityAndOperation(
                entityLocalId = adoptedLocal!!.localId,
                entityType = SyncEntityType.CASH_SESSION,
                operation = SyncOperation.CREATE,
            )!!
            assertTrue(CashSessionAdoptionPolicy.parseAdoptRemoteIfActive(openQueue.payloadJson))
        }

        // 4. Ambiguous openSession when server ALREADY opened session -> immediate retry adopts remote without re-calling openSession()
        database.openHelper.writableDatabase.execSQL("DELETE FROM sync_queue")
        database.openHelper.writableDatabase.execSQL("DELETE FROM local_cash_sessions")
        readinessResponder = { Response.success(HealthResponseDto(status = "ready", database = "up")) }
        backendHealthMonitor.refresh(force = true)
        val serverCreatedRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
            openSessionResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        }
        val serverCreatedVm = buildVm(serverCreatedRepo)
        advanceUntilIdle()

        serverCreatedVm.onOpeningCashChanged("100000")
        serverCreatedVm.openSession()
        advanceUntilIdle()
        assertFalse(serverCreatedVm.uiState.value.hasActiveSession)

        val remoteAlreadyOpened = CashSession(
            id = "srv-created-during-timeout",
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
        serverCreatedRepo.activeSessionResult = RepositoryResult.Success(remoteAlreadyOpened)
        serverCreatedVm.openSession()
        advanceUntilIdle()

        assertEquals(1, serverCreatedRepo.openSessionCalls)
        assertTrue(serverCreatedVm.uiState.value.hasActiveSession)
        assertEquals("srv-created-during-timeout", serverCreatedVm.uiState.value.activeSession?.id)

        // 5. HTTP_502 / ConnectException on openSession -> DO fallback locally
        database.openHelper.writableDatabase.execSQL("DELETE FROM sync_queue")
        database.openHelper.writableDatabase.execSQL("DELETE FROM local_cash_sessions")
        val fallbackRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
            openSessionResult = RepositoryResult.Error("HTTP_502", "Bad Gateway")
        }
        val fallbackVm = buildVm(fallbackRepo)
        advanceUntilIdle()
        backendHealthMonitor.markConnected()
        fallbackVm.onOpeningCashChanged("100000")
        fallbackVm.openSession()
        advanceUntilIdle()
        assertTrue(fallbackVm.uiState.value.hasActiveSession)
        assertNotNull(cashSessionLocalDataSource.getLatestOpenSession("kasir-1"))
    }

    @Test
    fun nonIoExceptions_neverMarkBackendUnreachableOrFallbackLocally() = runTest(dispatcher) {
        backendHealthMonitor.refresh(force = true)
        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)

        val openSession = CashSession(
            id = "srv-cash-local-ex",
            userId = "kasir-1",
            userName = "Kasir Satu",
            openedAt = "2026-11-27T08:00:00+07:00",
            closedAt = null,
            openingCash = BigDecimal("250000"),
            closingCash = null,
            systemCash = BigDecimal("250000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
        )
        val repo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(openSession)
            addExpenseResult = RepositoryResult.Exception(IllegalStateException("Local state bug"))
            closeSessionResult = RepositoryResult.Exception(SecurityException("Permission error"))
        }
        val vm = buildVm(repo)
        advanceUntilIdle()
        val mirroredLocalId = vm.uiState.value.localCashSessionId!!

        vm.showPettyCashDialog(CashMovementType.CASH_OUT)
        vm.onPettyCashAmountChanged("10000")
        vm.onPettyCashDescriptionChanged("Tes")
        vm.addPettyCash()
        advanceUntilIdle()

        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)
        assertTrue(localCashExpenseService.getLocalExpenses(mirroredLocalId).isEmpty())

        vm.onClosingCashChanged("250000")
        vm.closeSession()
        advanceUntilIdle()

        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)
        assertEquals("OPEN", cashSessionLocalDataSource.getCachedCashSessionByServerId("srv-cash-local-ex")?.status)
    }

    @Test
    fun preflightReadiness_skipsMutationRequestWhenFreshUnreachableOrReadinessProbeFails() = runTest(dispatcher) {
        // 1. Fresh CONNECTED cache, then server dies behind adb reverse -> /api/readiness probe throws EOFException
        backendHealthMonitor.refresh(force = true)
        assertEquals(BackendStatus.CONNECTED, backendHealthMonitor.status.value)

        val openRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
        }
        val openVm = buildVm(openRepo)
        advanceUntilIdle()

        readinessResponder = { throw EOFException("adb reverse closed") }
        val readinessBeforeOpen = readinessCalls
        openVm.onOpeningCashChanged("100000")
        openVm.openSession()
        advanceUntilIdle()

        assertEquals(readinessBeforeOpen + 1, readinessCalls)
        assertEquals(0, openRepo.openSessionCalls)
        assertTrue(openVm.uiState.value.hasActiveSession)
        assertNotNull(cashSessionLocalDataSource.getLatestOpenSession("kasir-1"))
        assertEquals(BackendStatus.UNREACHABLE, backendHealthMonitor.status.value)

        // 2. Fresh UNREACHABLE -> skips readiness probe AND skips addExpense mutation call
        val remoteOpenSession = CashSession(
            id = "srv-preflight-expense",
            userId = "kasir-1",
            userName = "Kasir Satu",
            openedAt = "2026-11-27T08:00:00+07:00",
            closedAt = null,
            openingCash = BigDecimal("250000"),
            closingCash = null,
            systemCash = BigDecimal("250000"),
            difference = null,
            totalExpenses = BigDecimal.ZERO,
            notes = null,
            status = "OPEN",
        )
        readinessResponder = { Response.success(HealthResponseDto(status = "ready", database = "up")) }
        backendHealthMonitor.refresh(force = true)
        val expenseRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(remoteOpenSession)
        }
        val expenseVm = buildVm(expenseRepo)
        advanceUntilIdle()
        val mirroredLocalId = expenseVm.uiState.value.localCashSessionId!!

        backendHealthMonitor.markUnreachable()
        val readinessBeforeExpense = readinessCalls
        expenseVm.showPettyCashDialog(CashMovementType.CASH_OUT)
        expenseVm.onPettyCashAmountChanged("15000")
        expenseVm.onPettyCashDescriptionChanged("Parkir")
        expenseVm.addPettyCash()
        advanceUntilIdle()

        assertEquals(readinessBeforeExpense, readinessCalls)
        assertEquals(0, expenseRepo.addExpenseCalls)
        assertEquals(1, localCashExpenseService.getLocalExpenses(mirroredLocalId).size)
    }

    @Test
    fun preDispatchConnectTimeoutAndPreDispatchEof_fallbackLocallyOnOpenSessionAndExpense() = runTest(dispatcher) {
        backendHealthMonitor.refresh(force = true)
        val openRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
            openSessionResult = RepositoryResult.Error("CONNECT_TIMEOUT", "Koneksi bermasalah")
        }
        val openVm = buildVm(openRepo)
        advanceUntilIdle()

        openVm.onOpeningCashChanged("100000")
        openVm.openSession()
        advanceUntilIdle()

        assertEquals(1, openRepo.openSessionCalls)
        assertTrue(openVm.uiState.value.hasActiveSession)
        assertNotNull(cashSessionLocalDataSource.getLatestOpenSession("kasir-1"))

        val expenseRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(
                CashSession(
                    id = "srv-predispatch-exp",
                    userId = "kasir-1",
                    userName = "Kasir Satu",
                    openedAt = "2026-11-27T08:00:00+07:00",
                    closedAt = null,
                    openingCash = BigDecimal("250000"),
                    closingCash = null,
                    systemCash = BigDecimal("250000"),
                    difference = null,
                    totalExpenses = BigDecimal.ZERO,
                    notes = null,
                    status = "OPEN",
                )
            )
            addExpenseResult = RepositoryResult.Exception(PreDispatchIOException(EOFException("dropped before headers")))
        }
        backendHealthMonitor.markConnected()
        val expenseVm = buildVm(expenseRepo)
        advanceUntilIdle()
        val mirroredLocalId = expenseVm.uiState.value.localCashSessionId!!

        expenseVm.showPettyCashDialog(CashMovementType.CASH_OUT)
        expenseVm.onPettyCashAmountChanged("20000")
        expenseVm.onPettyCashDescriptionChanged("Bensin")
        expenseVm.addPettyCash()
        advanceUntilIdle()

        assertEquals(1, expenseRepo.addExpenseCalls)
        assertEquals(1, localCashExpenseService.getLocalExpenses(mirroredLocalId).size)
    }

    @Test
    fun offlineLoadWithoutActiveSession_neverEmitsCloseSuccessEvent_onlyCloseSessionDoes() = runTest(dispatcher) {
        backendHealthMonitor.markUnreachable()
        val repo = FakeCashReconciliationOfflineRepo()
        val vm = buildVm(repo)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasActiveSession)
        assertNotNull(vm.uiState.value.message)
        assertEquals(0L, vm.uiState.value.closeSessionSuccessEventId)

        vm.onOpeningCashChanged("100000")
        vm.openSession()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasActiveSession)
        assertEquals(0L, vm.uiState.value.closeSessionSuccessEventId)

        vm.onClosingCashChanged("100000")
        vm.closeSession()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasActiveSession)
        assertEquals(1L, vm.uiState.value.closeSessionSuccessEventId)
    }

    @Test
    fun openSessionOffline_phoneStepsWithDeadServerOverUsbOrWifi_succeedsEvenWithStaleMirrorOrOtherCashierOpen() = runTest(dispatcher) {
        // Simulasi HP nyata:
        // 1) Stale mirror OPEN/SYNCED sesi lama di DB lokal + sesi kasir lain OPEN di DB lokal
        database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = "srv-stale-old",
                clientGeneratedId = "srv-stale-old",
                cashierUserId = "kasir-1",
                status = "OPEN",
                startingCash = ScaledDecimal2.ofWhole(50_000L),
                expectedCash = ScaledDecimal2.ofWhole(50_000L),
                openedAt = nowMillis - 86_400_000L,
                syncStatus = SyncStatus.SYNCED,
                createdAt = nowMillis - 86_400_000L,
                updatedAt = nowMillis - 86_400_000L,
            )
        )
        database.cashSessionDao().upsert(
            LocalCashSessionEntity(
                serverId = null,
                clientGeneratedId = "sess-other-kasir",
                cashierUserId = "kasir-2",
                status = "OPEN",
                startingCash = ScaledDecimal2.ofWhole(75_000L),
                expectedCash = ScaledDecimal2.ofWhole(75_000L),
                openedAt = nowMillis - 60_000L,
                syncStatus = SyncStatus.PENDING,
                createdAt = nowMillis - 60_000L,
                updatedAt = nowMillis - 60_000L,
            )
        )

        // Scenario A: Online load sees Success(null) (reconciling stale mirror), then USB adb reverse mati -> openSession lokal berhasil
        backendHealthMonitor.markConnected()
        val usbRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Success(null)
        }
        val usbVm = buildVm(usbRepo)
        advanceUntilIdle()

        assertFalse(usbVm.uiState.value.hasActiveSession)
        assertEquals("CLOSED", database.cashSessionDao().getByServerId("srv-stale-old")?.status)

        readinessResponder = { throw EOFException("adb reverse tunnel closed") }
        usbVm.onOpeningCashChanged("200000")
        usbVm.openSession()
        advanceUntilIdle()

        assertTrue(usbVm.uiState.value.hasActiveSession)
        assertEquals(0, usbRepo.openSessionCalls)
        assertNotNull(cashSessionLocalDataSource.getLatestOpenSession("kasir-1"))
        assertNotNull(cashSessionLocalDataSource.getLatestOpenSession("kasir-2"))

        // Scenario B: WiFi server mati (ConnectException pada readiness / openSession) -> tetap bisa buka lokal
        database.openHelper.writableDatabase.execSQL("DELETE FROM sync_queue")
        database.openHelper.writableDatabase.execSQL("DELETE FROM local_cash_sessions WHERE cashierUserId = 'kasir-1'")
        backendHealthMonitor.markConnected()
        readinessResponder = { throw ConnectException("Failed to connect to /192.168.1.10:3000") }
        val wifiRepo = FakeCashReconciliationOfflineRepo().apply {
            activeSessionResult = RepositoryResult.Exception(ConnectException("Failed to connect"))
        }
        val wifiVm = buildVm(wifiRepo)
        advanceUntilIdle()

        wifiVm.onOpeningCashChanged("175000")
        wifiVm.openSession()
        advanceUntilIdle()

        assertTrue(wifiVm.uiState.value.hasActiveSession)
        assertEquals(0, wifiRepo.openSessionCalls)
        assertEquals(BigDecimal("175000.00"), cashSessionLocalDataSource.getLatestOpenSession("kasir-1")?.startingCash?.toBigDecimal())
    }

    private fun buildVm(repository: CashReconciliationRepository): CashReconciliationViewModel {
        return CashReconciliationViewModel(
            repository = repository,
            cashSessionLocalDataSource = cashSessionLocalDataSource,
            localCashSessionService = localCashSessionService,
            localCashExpenseService = localCashExpenseService,
            offlineStatusRepository = offlineStatusRepository,
            cashierUserId = "kasir-1",
            cashierNameFallback = "Kasir Satu",
            actorRole = "KASIR",
            clockMillis = { nowMillis },
        )
    }
}

private class FakeCashReconciliationOfflineRepo : CashReconciliationRepository {
    var openSessionCalls = 0
    var addExpenseCalls = 0
    var activeSessionResult: RepositoryResult<CashSession?> = RepositoryResult.Success(null)
    var openSessionResult: RepositoryResult<CashSession> = RepositoryResult.Error("CONNECTION_FAILED", "offline")
    var closeSessionResult: RepositoryResult<CashSession> = RepositoryResult.Error("CONNECTION_FAILED", "offline")
    var addExpenseResult: RepositoryResult<CashExpense> = RepositoryResult.Error("CONNECTION_FAILED", "offline")

    override suspend fun getActiveSession(): RepositoryResult<CashSession?> = activeSessionResult
    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> {
        openSessionCalls++
        return openSessionResult
    }
    override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = closeSessionResult
    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> {
        addExpenseCalls++
        return addExpenseResult
    }
    override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashTransactionPage> =
        RepositoryResult.Success(CashTransactionPage(emptyList(), 0L, page, limit, 1))
    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> =
        RepositoryResult.Success(emptyList())
    override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("unused")
    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
    override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
    override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("unused")
    override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("unused")
    override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("unused")
    override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
    override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
}
