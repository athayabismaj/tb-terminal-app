package com.tbterminal.app.data.sync

import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BackendHealthMonitorTest {
    @Test
    fun readinessSuccessMarksBackendConnected() = runBlocking {
        val api = FakeHealthApi(Response.success(HealthResponseDto(status = "ready", database = "up")))
        val monitor = BackendHealthMonitor(api, FakeConnectivity(true), throttleMillis = 0, timeoutMillis = 1_000)

        assertEquals(BackendStatus.CONNECTED, monitor.refresh(force = true))
        assertTrue(api.calls.get() >= 1)
    }

    @Test
    fun readinessServiceUnavailableDoesNotFallBackToLiveness() = runBlocking {
        val api = FakeHealthApi(Response.error(503, "database down".toResponseBody()))
        val monitor = BackendHealthMonitor(api, FakeConnectivity(true), throttleMillis = 0, timeoutMillis = 1_000)

        assertEquals(BackendStatus.UNREACHABLE, monitor.refresh(force = true))
    }

    @Test
    fun noNetworkSkipsReadinessAndReturnsUnknown() = runBlocking {
        val api = FakeHealthApi(Response.success(HealthResponseDto(status = "ready", database = "up")))
        val monitor = BackendHealthMonitor(api, FakeConnectivity(false), throttleMillis = 0, timeoutMillis = 1_000)

        assertEquals(BackendStatus.UNKNOWN, monitor.refresh(force = true))
        assertEquals(0, api.calls.get())
    }

    @Test
    fun markUnreachableTransitionsConnectedToUnreachableWhenOnlineAndUnknownWhenOffline() = runBlocking {
        val connectivity = FakeConnectivity(true)
        val api = FakeHealthApi(Response.success(HealthResponseDto(status = "ready", database = "up")))
        val monitor = BackendHealthMonitor(api, connectivity, throttleMillis = 30_000L, timeoutMillis = 1_000)

        monitor.markConnected()
        assertEquals(BackendStatus.CONNECTED, monitor.status.value)

        monitor.markUnreachable()
        assertEquals(BackendStatus.UNREACHABLE, monitor.status.value)

        connectivity.setOnline(false)
        monitor.markUnreachable()
        assertEquals(BackendStatus.UNKNOWN, monitor.status.value)
    }

    @Test
    fun isStaleRespectsThrottleAndResetsOnMarkMethodsAndRecoveryRefresh() = runBlocking {
        var nowMillis = 10_000L
        val api = FakeHealthApi(Response.error(503, "down".toResponseBody()))
        val monitor = BackendHealthMonitor(
            healthApi = api,
            networkMonitor = FakeConnectivity(true),
            throttleMillis = 30_000L,
            timeoutMillis = 1_000,
            clockMillis = { nowMillis },
        )

        assertTrue(monitor.isStale())

        monitor.markConnected()
        assertFalse(monitor.isStale())

        nowMillis += 29_999L
        assertFalse(monitor.isStale())

        nowMillis += 1L
        assertTrue(monitor.isStale())

        monitor.markUnreachable()
        assertEquals(BackendStatus.UNREACHABLE, monitor.status.value)
        assertFalse(monitor.isStale())

        api.setResponse(Response.success(HealthResponseDto(status = "ready", database = "up")))
        assertEquals(BackendStatus.CONNECTED, monitor.refresh(force = true))
        assertEquals(BackendStatus.CONNECTED, monitor.status.value)
    }
}

private class FakeHealthApi(initialResponse: Response<HealthResponseDto>) : HealthApi {
    private val responseRef = AtomicReference(initialResponse)
    val calls = AtomicInteger(0)

    fun setResponse(response: Response<HealthResponseDto>) {
        responseRef.set(response)
    }

    override suspend fun getReadiness(): Response<HealthResponseDto> {
        calls.incrementAndGet()
        return responseRef.get()
    }
}

private class FakeConnectivity(initial: Boolean) : NetworkConnectivity {
    private val online = MutableStateFlow(initial)
    fun setOnline(value: Boolean) {
        online.value = value
    }

    override fun isOnline(): Boolean = online.value
    override fun observeOnline(): Flow<Boolean> = online
}
