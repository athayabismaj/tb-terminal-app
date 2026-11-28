package com.tbterminal.app.data.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.local.model.AppDataMode
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import java.io.IOException
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CashierSyncSchedulerAndWorkerTest {
    private lateinit var context: Context
    private lateinit var database: TbTerminalDatabase
    private lateinit var settings: LocalAppSettingsDataSource
    private lateinit var workManager: WorkManager

    companion object {
        @Volatile
        private var workerReleaseGate: CompletableDeferred<Unit> = CompletableDeferred(Unit)
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = LocalAppSettingsDataSource(database.appSettingDao())
        runBlocking { settings.setDataMode(AppDataMode.SYNC_SERVER) }
        workerReleaseGate = CompletableDeferred()

        if (!WorkManager.isInitialized()) {
            val executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "wm-test").apply { isDaemon = true }
            }
            WorkManager.initialize(
                context,
                Configuration.Builder()
                    .setExecutor(executor)
                    .setWorkerFactory(
                        object : WorkerFactory() {
                            override fun createWorker(
                                appContext: Context,
                                workerClassName: String,
                                workerParameters: WorkerParameters,
                            ): ListenableWorker? {
                                if (workerClassName != SyncWorker::class.java.name) return null
                                return object : CoroutineWorker(appContext, workerParameters) {
                                    override suspend fun doWork(): Result {
                                        workerReleaseGate.await()
                                        return Result.success()
                                    }
                                }
                            }
                        }
                    )
                    .build(),
            )
        }
        workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork().result.get()
    }

    @After
    fun tearDown() {
        workerReleaseGate.complete(Unit)
        workManager.cancelAllWork().result.get()
        database.close()
    }

    @Test
    fun syncNowUsesSeparateKeepWorkWithoutNetworkConstraintAndNeverCancelsOtherWorks() {
        val onlineFlow = MutableStateFlow(true)
        val healthMonitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> =
                    Response.success(HealthResponseDto(status = "ready", database = "up"))
            },
            networkMonitor = object : NetworkConnectivity {
                override fun isOnline(): Boolean = onlineFlow.value
                override fun observeOnline(): Flow<Boolean> = onlineFlow
            },
            throttleMillis = 0L,
            timeoutMillis = 1_000L,
        )
        healthMonitor.markConnected()

        val scheduler = OfflineSyncScheduler(
            context = context,
            localAppSettingsDataSource = settings,
            networkMonitor = object : NetworkConnectivity {
                override fun isOnline(): Boolean = onlineFlow.value
                override fun observeOnline(): Flow<Boolean> = onlineFlow
            },
            backendHealthMonitor = healthMonitor,
        )

        scheduler.schedule()
        scheduler.scheduleDelayed(120L)
        val primaryBefore = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get().single()
        val delayedBefore = workManager.getWorkInfosForUniqueWork(SyncWorker.DELAYED_WORK_NAME).get().single()

        val scheduleResult = runBlocking { scheduler.scheduleSyncNowIfConnected(includeFailed = true) }
        assertEquals(SyncNowScheduleResult.ENQUEUED, scheduleResult)

        val syncNowInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.SYNC_NOW_WORK_NAME).get()
        assertEquals(1, syncNowInfos.size)
        val firstSyncNow = syncNowInfos.single()
        assertTrue(
            firstSyncNow.state == WorkInfo.State.ENQUEUED ||
                firstSyncNow.state == WorkInfo.State.RUNNING
        )
        assertNotEquals(WorkInfo.State.CANCELLED, firstSyncNow.state)
        assertNotEquals(WorkInfo.State.FAILED, firstSyncNow.state)
        assertEquals(NetworkType.NOT_REQUIRED, firstSyncNow.constraints.requiredNetworkType)

        // Second enqueue uses KEEP so the first sync_now work is neither cancelled nor duplicated
        scheduler.scheduleSyncNow(includeFailed = false)
        val syncNowAfterKeep = workManager.getWorkInfosForUniqueWork(SyncWorker.SYNC_NOW_WORK_NAME).get()
        assertEquals(1, syncNowAfterKeep.size)
        assertEquals(firstSyncNow.id, syncNowAfterKeep.single().id)
        assertNotEquals(WorkInfo.State.CANCELLED, syncNowAfterKeep.single().state)

        // Existing WORK_NAME and DELAYED_WORK_NAME are NEVER cancelled
        val primaryAfter = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get().single()
        val delayedAfter = workManager.getWorkInfosForUniqueWork(SyncWorker.DELAYED_WORK_NAME).get().single()
        assertEquals(primaryBefore.id, primaryAfter.id)
        assertEquals(delayedBefore.id, delayedAfter.id)
        assertNotEquals(WorkInfo.State.CANCELLED, primaryAfter.state)
        assertNotEquals(WorkInfo.State.CANCELLED, delayedAfter.state)
    }

    @Test
    fun syncNowIsNotEnqueuedWhenServerOfflineOrUnreachable() = runBlocking {
        val onlineFlow = MutableStateFlow(true)
        val connectivity = object : NetworkConnectivity {
            override fun isOnline(): Boolean = onlineFlow.value
            override fun observeOnline(): Flow<Boolean> = onlineFlow
        }
        val healthMonitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> = Response.error(
                    503,
                    okhttp3.ResponseBody.create(null, ""),
                )
            },
            networkMonitor = connectivity,
            throttleMillis = 0L,
            timeoutMillis = 1_000L,
        )
        healthMonitor.markUnreachable()

        val scheduler = OfflineSyncScheduler(
            context = context,
            localAppSettingsDataSource = settings,
            networkMonitor = connectivity,
            backendHealthMonitor = healthMonitor,
        )

        val unreachableResult = scheduler.scheduleSyncNowIfConnected(includeFailed = true)
        assertEquals(SyncNowScheduleResult.SKIPPED_OFFLINE_OR_DISCONNECTED, unreachableResult)
        assertTrue(workManager.getWorkInfosForUniqueWork(SyncWorker.SYNC_NOW_WORK_NAME).get().isEmpty())

        healthMonitor.markConnected()
        onlineFlow.value = false
        val offlineResult = scheduler.scheduleSyncNowIfConnected(includeFailed = true)
        assertEquals(SyncNowScheduleResult.SKIPPED_OFFLINE_OR_DISCONNECTED, offlineResult)
        assertTrue(workManager.getWorkInfosForUniqueWork(SyncWorker.SYNC_NOW_WORK_NAME).get().isEmpty())
    }

    @Test
    fun syncNowWorkerNeverReturnsRetryOnTransientOrUnexpectedErrors() = runBlocking {
        var delayedCalled = false
        val retryWithoutDelayResult = SyncWorker.executeSyncWork(
            runOnce = {
                SyncResult.Retry(
                    reason = "Server belum tersambung.",
                    retryAfterSeconds = null,
                    successCount = 1,
                    failedCount = 0,
                )
            },
            scheduleDelayed = { delayedCalled = true },
            scheduleContinuation = {},
            runAttemptCount = 0,
            isSyncNow = true,
            includeFailed = true,
        )

        assertTrue(retryWithoutDelayResult is ListenableWorker.Result.Success)
        assertFalse(delayedCalled)
        val retryOutput = (retryWithoutDelayResult as ListenableWorker.Result.Success).outputData
        assertEquals(SyncRunOutcome.RETRY.name, retryOutput.getString(SyncWorker.KEY_OUTCOME))
        assertEquals(1, retryOutput.getInt(SyncWorker.KEY_SYNCED_COUNT, -1))
        assertEquals(true, retryOutput.getBoolean(SyncWorker.KEY_INCLUDED_FAILED, false))

        val retryWith429Result = SyncWorker.executeSyncWork(
            runOnce = {
                SyncResult.Retry(
                    reason = "Rate limited",
                    retryAfterSeconds = 45L,
                    successCount = 0,
                    failedCount = 1,
                )
            },
            scheduleDelayed = { delayedCalled = true },
            scheduleContinuation = {},
            runAttemptCount = 0,
            isSyncNow = true,
            includeFailed = true,
        )
        assertTrue(retryWith429Result is ListenableWorker.Result.Success)
        assertFalse(delayedCalled)

        val failedResult = SyncWorker.executeSyncWork(
            runOnce = { SyncResult.Failed(IOException("Socket reset")) },
            scheduleDelayed = { delayedCalled = true },
            scheduleContinuation = {},
            runAttemptCount = 0,
            isSyncNow = true,
            includeFailed = false,
        )
        assertTrue(failedResult is ListenableWorker.Result.Success)
        val failedOutput = (failedResult as ListenableWorker.Result.Success).outputData
        assertEquals(SyncRunOutcome.FAILED.name, failedOutput.getString(SyncWorker.KEY_OUTCOME))
        assertEquals("Socket reset", failedOutput.getString(SyncWorker.KEY_REASON))
    }

    @Test
    fun autoSyncTriggerFiresAfterPinUnlockedAndWhenBackendRecovers() = runBlocking {
        val onlineFlow = MutableStateFlow(true)
        val backendStatusFlow = MutableStateFlow(BackendStatus.CONNECTED)
        val pinRequiredFlow = MutableStateFlow(true)
        var triggerCount = 0
        val unlockedTriggered = CompletableDeferred<Unit>()
        val recoveredTriggered = CompletableDeferred<Unit>()

        val job = launch {
            observeAutoSyncTriggers(
                onlineFlow = onlineFlow,
                backendStatusFlow = backendStatusFlow,
                pinUnlockRequiredFlow = pinRequiredFlow,
                onTriggerSyncNow = {
                    triggerCount += 1
                    if (triggerCount == 1) unlockedTriggered.complete(Unit)
                    if (triggerCount == 2) recoveredTriggered.complete(Unit)
                },
            )
        }

        // Still locked by PIN -> must NOT trigger
        kotlinx.coroutines.delay(50L)
        assertEquals(0, triggerCount)

        // Kasir unlocks PIN -> triggers immediately
        pinRequiredFlow.value = false
        withTimeout(2_000L) { unlockedTriggered.await() }
        assertEquals(1, triggerCount)

        // Backend goes unreachable then recovers -> triggers again
        backendStatusFlow.value = BackendStatus.UNREACHABLE
        kotlinx.coroutines.delay(30L)
        backendStatusFlow.value = BackendStatus.CONNECTED
        withTimeout(2_000L) { recoveredTriggered.await() }
        assertEquals(2, triggerCount)

        job.cancel()
    }
}
