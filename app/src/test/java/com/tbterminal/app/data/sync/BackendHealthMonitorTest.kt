package com.tbterminal.app.data.sync

import com.tbterminal.app.data.remote.HealthApi
import com.tbterminal.app.data.remote.HealthResponseDto
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
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
}

private class FakeHealthApi(private val response: Response<HealthResponseDto>) : HealthApi {
    val calls = AtomicInteger(0)
    override suspend fun getReadiness(): Response<HealthResponseDto> {
        calls.incrementAndGet()
        return response
    }
}

private class FakeConnectivity(initial: Boolean) : NetworkConnectivity {
    private val online = MutableStateFlow(initial)
    override fun isOnline(): Boolean = online.value
    override fun observeOnline(): Flow<Boolean> = online
}
