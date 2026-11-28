package com.tbterminal.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.database.PendingSubmissionRecord
import com.tbterminal.app.data.local.database.PendingSubmissionStatus
import com.tbterminal.app.data.local.database.RoomPendingSubmissionStore
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.SharedPreferencesTokenStore
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.NetworkConnectivity
import com.tbterminal.app.navigation.AppRoute
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MainActivityLifecycleAndMainThreadTest {
    @Test
    fun shouldNavigateToPinOnStart_skipsWhenAlreadyOnPinRouteOrNotLocked() {
        assertFalse(shouldNavigateToPinOnStart(requiresPinUnlock = true, currentRoute = AppRoute.Pin.route))
        assertFalse(shouldNavigateToPinOnStart(requiresPinUnlock = false, currentRoute = AppRoute.Dashboard.route))
        assertTrue(shouldNavigateToPinOnStart(requiresPinUnlock = true, currentRoute = AppRoute.Dashboard.route))
        assertTrue(shouldNavigateToPinOnStart(requiresPinUnlock = true, currentRoute = null))
    }

    @Test
    fun sharedPreferencesTokenStore_cachesSessionUserAndClearsCacheCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("tb_terminal_session", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val store = SharedPreferencesTokenStore(context)
        assertFalse(store.hasStoredAccessToken())
        assertNull(store.readSessionUser())

        val user = SessionUser(
            name = "Kasir Cache",
            role = "KASIR",
            isActive = true,
            joinedAt = "2026-01-01T00:00:00Z",
            lastLoginAt = null,
            userId = "kasir-cache",
            username = "kasircache",
            email = null,
        )
        store.saveSessionUser(user)
        assertEquals(user, store.readSessionUser())

        // Mutate underlying SharedPreferences behind the store to prove cached read is used without re-reading disk
        prefs.edit().putString("session_user_name", "Mutated Disk Value").commit()
        assertEquals("Kasir Cache", store.readSessionUser()?.name)

        store.clearAccessToken()
        assertFalse(store.hasStoredAccessToken())
        assertNull(store.readSessionUser())
    }

    @Test
    fun backendHealthMonitorRefresh_andRoomPendingSubmissionStore_dispatchOntoIoDispatcher() = runBlocking {
        val ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "test-io-worker")
        }
        val ioDispatcher = ioExecutor.asCoroutineDispatcher()
        try {
            val healthThreads = CopyOnWriteArrayList<String>()
            val monitor = BackendHealthMonitor(
                healthApi = object : HealthApi {
                    override suspend fun getReadiness(): Response<HealthResponseDto> {
                        healthThreads += Thread.currentThread().name
                        return Response.success(HealthResponseDto(status = "ready", database = "up"))
                    }
                },
                networkMonitor = object : NetworkConnectivity {
                    private val onlineFlow = MutableStateFlow(true)
                    override fun isOnline(): Boolean {
                        healthThreads += Thread.currentThread().name
                        return true
                    }

                    override fun observeOnline(): Flow<Boolean> = onlineFlow
                },
                throttleMillis = 0L,
                ioDispatcher = ioDispatcher,
            )

            assertEquals(BackendStatus.CONNECTED, monitor.refresh(force = true))
            assertTrue("Expected health callbacks to execute, got empty", healthThreads.isNotEmpty())
            assertTrue(
                "Expected all health callbacks on test-io-worker, got: $healthThreads",
                healthThreads.all { it.startsWith("test-io-worker") },
            )

            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, TbTerminalDatabase::class.java)
                .allowMainThreadQueries()
                .setQueryExecutor { it.run() }
                .setTransactionExecutor { it.run() }
                .build()
            try {
                val bootThreads = CopyOnWriteArrayList<String>()
                val bootCalls = AtomicInteger(0)
                val store = RoomPendingSubmissionStore(
                    dao = db.pendingSubmissionDao(),
                    ownerUserIdProvider = { "kasir-1" },
                    bootCountProvider = {
                        bootCalls.incrementAndGet()
                        bootThreads += Thread.currentThread().name
                        1L
                    },
                    ioDispatcher = ioDispatcher,
                )
                assertTrue(
                    store.saveForOwner(
                        ownerUserId = "kasir-1",
                        record = PendingSubmissionRecord(
                            scope = "checkout",
                            idempotencyKey = "idem-1",
                            intentJson = "{}",
                            status = PendingSubmissionStatus.IN_FLIGHT,
                            createdAt = 1_800_000_000_000L,
                            updatedAt = 1_800_000_000_000L,
                        ),
                    )
                )
                val loaded = store.load("checkout")
                assertNotNull(loaded)
                assertEquals(2, bootCalls.get())
                assertTrue(
                    "Expected all bootCount reads on test-io-worker, got: $bootThreads",
                    bootThreads.all { it.startsWith("test-io-worker") },
                )
            } finally {
                db.close()
            }
        } finally {
            ioDispatcher.close()
            ioExecutor.shutdownNow()
        }
    }
}
