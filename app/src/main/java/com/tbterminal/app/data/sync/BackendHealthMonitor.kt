package com.tbterminal.app.data.sync

import com.tbterminal.app.data.remote.HealthApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response

class BackendHealthMonitor(
    private val healthApi: HealthApi,
    private val networkMonitor: NetworkConnectivity,
    private val throttleMillis: Long = DEFAULT_THROTTLE_MILLIS,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val _status = MutableStateFlow(BackendStatus.UNKNOWN)
    private var lastCheckedAtMillis: Long = 0L

    val status: StateFlow<BackendStatus> = _status.asStateFlow()

    init {
        scope.launch {
            networkMonitor.observeOnline()
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (isOnline) {
                        refresh(force = true)
                    } else {
                        _status.value = BackendStatus.UNKNOWN
                    }
                }
        }
    }

    fun refreshAsync(force: Boolean = false) {
        scope.launch {
            refresh(force = force)
        }
    }

    fun markConnected() {
        lastCheckedAtMillis = clockMillis()
        _status.value = BackendStatus.CONNECTED
    }

    fun markUnreachable() {
        if (!networkMonitor.isOnline()) {
            _status.value = BackendStatus.UNKNOWN
            return
        }
        lastCheckedAtMillis = clockMillis()
        _status.value = BackendStatus.UNREACHABLE
    }

    fun isStale(nowMillis: Long = clockMillis()): Boolean {
        return lastCheckedAtMillis <= 0L || (nowMillis - lastCheckedAtMillis) >= throttleMillis
    }

    suspend fun refresh(force: Boolean = false): BackendStatus {
        if (!networkMonitor.isOnline()) {
            _status.value = BackendStatus.UNKNOWN
            return _status.value
        }

        val now = clockMillis()
        if (!force && _status.value != BackendStatus.UNKNOWN && now - lastCheckedAtMillis < throttleMillis) {
            return _status.value
        }

        return refreshMutex.withLock {
            val lockedNow = clockMillis()
            if (!force && _status.value != BackendStatus.UNKNOWN && lockedNow - lastCheckedAtMillis < throttleMillis) {
                return@withLock _status.value
            }

            lastCheckedAtMillis = lockedNow
            val response = withTimeoutOrNull(timeoutMillis) {
                runSuspendCatching { healthApi.getReadiness() }.getOrNull()
            }
            val nextStatus = response.toBackendStatus()
            _status.value = nextStatus
            nextStatus
        }
    }

    private fun Response<*>?.toBackendStatus(): BackendStatus {
        return if (this != null && isSuccessful) {
            BackendStatus.CONNECTED
        } else {
            BackendStatus.UNREACHABLE
        }
    }

    private companion object {
        const val DEFAULT_THROTTLE_MILLIS = 30_000L
        const val DEFAULT_TIMEOUT_MILLIS = 2_000L
    }
}
