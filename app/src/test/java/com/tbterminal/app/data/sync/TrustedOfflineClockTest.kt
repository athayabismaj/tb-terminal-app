package com.tbterminal.app.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbterminal.app.data.local.cashexpense.CashMovementType
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionResult
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.checkout.LocalCheckoutCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutItemCommand
import com.tbterminal.app.data.local.checkout.LocalCheckoutResult
import com.tbterminal.app.data.local.checkout.LocalCheckoutService
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.TbTerminalDatabase
import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TrustedOfflineClockTest {
    private lateinit var database: TbTerminalDatabase
    private lateinit var settings: LocalAppSettingsDataSource

    private var wallClockMillis = 1_795_855_000_000L
    private var elapsedMillis = 100_000L
    private var bootCount: Long? = 10L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TbTerminalDatabase::class.java,
        ).allowMainThreadQueries().build()
        settings = LocalAppSettingsDataSource(database.appSettingDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun calibratesFromReadinessMidpointRttAndPersistsAnchor() = runBlocking {
        val serverTimeMillis = 1_795_855_058_980L
        val serverTimeIso = Instant.ofEpochMilli(serverTimeMillis)
            .atZone(ZoneId.of("Asia/Jakarta"))
            .toOffsetDateTime()
            .toString()

        val clock = createClock()
        var readinessCalls = 0
        val connectivity = MutableStateFlow(false)
        val monitor = BackendHealthMonitor(
            healthApi = object : HealthApi {
                override suspend fun getReadiness(): Response<HealthResponseDto> {
                    readinessCalls += 1
                    elapsedMillis += 200L
                    return Response.success(
                        HealthResponseDto(
                            status = "ready",
                            database = "up",
                            time = serverTimeIso,
                        )
                    )
                }
            },
            networkMonitor = object : NetworkConnectivity {
                override fun isOnline(): Boolean = true
                override fun observeOnline(): Flow<Boolean> = connectivity
            },
            throttleMillis = 0L,
            timeoutMillis = 1_000L,
            clockMillis = { elapsedMillis },
            trustedOfflineClock = clock,
        )

        assertEquals(BackendStatus.CONNECTED, monitor.refresh(force = true))
        assertEquals(1, readinessCalls)

        val persisted = settings.getTrustedClockAnchor()
        assertNotNull(persisted)
        assertEquals(serverTimeMillis, persisted!!.serverAnchorMillis)
        assertEquals(100_100L, persisted.elapsedAnchorMillis)
        assertEquals(10L, persisted.bootCount)

        // Advance elapsed time by 5s -> trusted clock advances by 5s from midpoint (100_100 -> 105_100)
        elapsedMillis = 105_100L
        wallClockMillis = serverTimeMillis + 5_000L
        val reading = clock.readNow()
        assertTrue(reading.isAnchorValid)
        assertEquals(TrustedClockStatus.CALIBRATED, reading.status)
        assertEquals(serverTimeMillis + 5_000L, reading.millis)
    }

    @Test
    fun ignoresDeviceWallClockMovedForwardOrBackwardWhileCalibrated() = runBlocking {
        val serverTimeMillis = 1_795_855_000_000L
        val clock = createClock()
        assertTrue(
            clock.recordServerSample(
                serverTimeIso = Instant.ofEpochMilli(serverTimeMillis).toString(),
                requestElapsedMillis = 100_000L,
                responseElapsedMillis = 100_000L,
            )
        )

        elapsedMillis = 130_000L // +30s real time
        val expectedTrusted = serverTimeMillis + 30_000L

        // Device wall clock moved +1 hour forward
        wallClockMillis = expectedTrusted + 3_600_000L
        val forwardReading = clock.readNow()
        assertTrue(forwardReading.isAnchorValid)
        assertEquals(expectedTrusted, forwardReading.millis)
        assertEquals(TrustedClockStatus.DEVICE_CLOCK_SKEWED, forwardReading.status)
        assertEquals(3_600_000L, forwardReading.skewMillis)

        // Device wall clock moved -1 hour backward
        wallClockMillis = expectedTrusted - 3_600_000L
        val backwardReading = clock.readNow()
        assertTrue(backwardReading.isAnchorValid)
        assertEquals(expectedTrusted, backwardReading.millis)
        assertEquals(TrustedClockStatus.DEVICE_CLOCK_SKEWED, backwardReading.status)
        assertEquals(-3_600_000L, backwardReading.skewMillis)
    }

    @Test
    fun detectsRebootAndClampsFallbackToLastTrustedMillis() = runBlocking {
        val serverTimeMillis = 1_795_855_000_000L
        val clock = createClock()
        assertTrue(
            clock.recordServerSample(
                serverTimeIso = Instant.ofEpochMilli(serverTimeMillis).toString(),
                requestElapsedMillis = 100_000L,
                responseElapsedMillis = 100_000L,
            )
        )
        elapsedMillis = 160_000L
        clock.advanceLastTrustedFloor(serverTimeMillis + 60_000L)

        // Simulate device reboot: bootCount increments and elapsedRealtime resets
        bootCount = 11L
        elapsedMillis = 5_000L
        wallClockMillis = serverTimeMillis - 500_000L // device clock behind last trusted

        val reading = clock.readNow()
        assertFalse(reading.isAnchorValid)
        assertEquals(TrustedClockStatus.UNCALIBRATED_AFTER_REBOOT, reading.status)
        assertEquals(serverTimeMillis + 60_000L, reading.millis)
        assertNull(reading.skewMillis)

        // If wall clock is ahead of lastTrustedMillis, fallback uses wall clock
        wallClockMillis = serverTimeMillis + 120_000L
        assertEquals(serverTimeMillis + 120_000L, clock.readNow().millis)
    }

    @Test
    fun withoutAnchorFallsBackToWallClockAndReportsNoAnchor() = runBlocking {
        val clock = createClock()
        val reading = clock.readNow()
        assertFalse(reading.isAnchorValid)
        assertEquals(TrustedClockStatus.NO_ANCHOR, reading.status)
        assertEquals(wallClockMillis, reading.millis)
    }

    @Test
    fun offlineWritesUseTrustedClockAndNeverMoveBackwardsWithinSession() = runBlocking {
        val serverBase = 1_795_855_000_000L
        val clock = createClock()
        assertTrue(
            clock.recordServerSample(
                serverTimeIso = Instant.ofEpochMilli(serverBase).toString(),
                requestElapsedMillis = 100_000L,
                responseElapsedMillis = 100_000L,
            )
        )

        val sessionService = LocalCashSessionService(
            database = database,
            deviceId = "dev-1",
            clockMillis = { wallClockMillis },
            trustedOfflineClock = clock,
        )
        val expenseService = LocalCashExpenseService(
            database = database,
            deviceId = "dev-1",
            clockMillis = { wallClockMillis },
            trustedOfflineClock = clock,
        )
        val checkoutService = LocalCheckoutService(
            database = database,
            deviceId = "dev-1",
            clockMillis = { wallClockMillis },
            trustedOfflineClock = clock,
        )

        // Skew device clock +2 hours when opening session -> must record trusted time
        wallClockMillis = serverBase + 7_200_000L
        elapsedMillis = 110_000L
        val openRes = sessionService.openLocalSession("kasir-1", BigDecimal("500000.00")) as LocalCashSessionResult.Success
        assertEquals(serverBase + 10_000L, openRes.session.openedAt)

        // Skew device clock -2 hours when recording expense -> must record trusted time
        wallClockMillis = serverBase - 7_200_000L
        elapsedMillis = 120_000L
        val expRes = expenseService.addLocalCashMovement(
            cashSessionLocalId = openRes.session.localId,
            type = CashMovementType.CASH_OUT,
            amount = BigDecimal("10000"),
            description = "Bensin",
        ) as com.tbterminal.app.data.local.cashexpense.LocalCashExpenseResult.Success
        assertEquals(serverBase + 20_000L, expRes.expense.occurredAt)

        // Simulate reboot + wall clock moving BEFORE expense occurredAt -> checkout & close must clamp >= 20_000L
        bootCount = 11L
        elapsedMillis = 1_000L
        wallClockMillis = serverBase - 600_000L
        // Wait: lastTrustedMillis was serverBase + 20_000L, also check clamping against session events
        // even if lastTrustedMillis were lower (simulate fresh clock instance without anchor)
        val uncalibratedClock = TrustedOfflineClock(
            localAppSettingsDataSource = LocalAppSettingsDataSource(
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TbTerminalDatabase::class.java,
                ).allowMainThreadQueries().build().appSettingDao()
            ),
            wallClockMillis = { wallClockMillis },
            elapsedRealtimeMillis = { elapsedMillis },
            bootCountProvider = { bootCount },
        )
        val uncalibratedCheckout = LocalCheckoutService(
            database = database,
            deviceId = "dev-1",
            clockMillis = { wallClockMillis },
            trustedOfflineClock = uncalibratedClock,
        )
        val uncalibratedSessionService = LocalCashSessionService(
            database = database,
            deviceId = "dev-1",
            clockMillis = { wallClockMillis },
            trustedOfflineClock = uncalibratedClock,
        )

        val txRes = uncalibratedCheckout.saveCheckout(
            LocalCheckoutCommand(
                cashierUserId = "kasir-1",
                cashSessionLocalId = openRes.session.localId,
                paymentMethod = "tunai",
                subtotal = BigDecimal("40000"),
                total = BigDecimal("40000"),
                paidAmount = BigDecimal("40000"),
                items = listOf(
                    LocalCheckoutItemCommand(
                        productServerId = "prod-1",
                        productNameSnapshot = "Semen",
                        quantity = BigDecimal.ONE,
                        priceAtTransaction = BigDecimal("40000"),
                        subtotal = BigDecimal("40000"),
                    )
                ),
            )
        ) as LocalCheckoutResult.Success
        val savedTx = database.transactionDao().getByLocalId(txRes.transactionLocalId)!!
        assertEquals(serverBase + 20_000L, savedTx.occurredAt)

        val closeRes = uncalibratedSessionService.closeLocalSession(
            localId = openRes.session.localId,
            closingCash = BigDecimal("530000"),
        ) as LocalCashSessionResult.Success
        assertEquals(serverBase + 20_000L, closeRes.session.closedAt)
    }

    private fun createClock(): TrustedOfflineClock {
        return TrustedOfflineClock(
            localAppSettingsDataSource = settings,
            wallClockMillis = { wallClockMillis },
            elapsedRealtimeMillis = { elapsedMillis },
            bootCountProvider = { bootCount },
        )
    }
}

private object AlwaysOnlineConnectivityForClock : NetworkConnectivity {
    private val flow = MutableStateFlow(true)
    override fun isOnline(): Boolean = true
    override fun observeOnline(): Flow<Boolean> = flow
}
