package com.tbterminal.app.data.sync

import com.tbterminal.app.data.remote.HealthApi
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response

class BackendHealthMonitor(
    private val healthApi: HealthApi,
    private val networkMonitor: NetworkConnectivity,
    private val throttleMillis: Long = DEFAULT_THROTTLE_MILLIS,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val clockMillis: () -> Long = { defaultElapsedRealtimeMillis() },
    private val ioDispatcher: CoroutineDispatcher? = null,
    private val trustedOfflineClock: TrustedOfflineClock? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + (ioDispatcher ?: Dispatchers.IO))
    private val refreshMutex = Mutex()
    private val _status = MutableStateFlow(BackendStatus.UNKNOWN)
    private var lastCheckedAtMillis: Long? = null

    @Volatile
    private var lastKnownOnline: Boolean = true

    val status: StateFlow<BackendStatus> = _status.asStateFlow()

    init {
        scope.launch {
            networkMonitor.observeOnline()
                .distinctUntilChanged()
                .collect { isOnline ->
                    lastKnownOnline = isOnline
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
        val last = lastCheckedAtMillis ?: return true
        val elapsed = nowMillis - last
        return elapsed < 0L || elapsed >= throttleMillis
    }

    suspend fun refresh(force: Boolean = false): BackendStatus {
        val dispatcher = ioDispatcher
        return if (dispatcher != null) {
            withContext(dispatcher) { refreshInternal(force) }
        } else {
            refreshInternal(force)
        }
    }

    private suspend fun refreshInternal(force: Boolean): BackendStatus {
        val online = networkMonitor.isOnline()
        lastKnownOnline = online
        if (!online) {
            _status.value = BackendStatus.UNKNOWN
            return _status.value
        }

        val now = clockMillis()
        val lastBeforeLock = lastCheckedAtMillis
        val elapsedBeforeLock = if (lastBeforeLock == null) null else now - lastBeforeLock
        if (!force &&
            _status.value != BackendStatus.UNKNOWN &&
            elapsedBeforeLock != null &&
            elapsedBeforeLock >= 0L &&
            elapsedBeforeLock < throttleMillis
        ) {
            return _status.value
        }

        return refreshMutex.withLock {
            val lockedNow = clockMillis()
            val lastLocked = lastCheckedAtMillis
            val elapsedLocked = if (lastLocked == null) null else lockedNow - lastLocked
            if (!force &&
                _status.value != BackendStatus.UNKNOWN &&
                elapsedLocked != null &&
                elapsedLocked >= 0L &&
                elapsedLocked < throttleMillis
            ) {
                return@withLock _status.value
            }

            val requestElapsedMillis = clockMillis()
            val response = withTimeoutOrNull(timeoutMillis) {
                runSuspendCatching { healthApi.getReadiness() }.getOrNull()
            }
            val responseElapsedMillis = clockMillis()
            lastCheckedAtMillis = responseElapsedMillis
            val nextStatus = response.toBackendStatus()
            if (nextStatus == BackendStatus.CONNECTED) {
                val serverTimeIso = response?.body()?.time
                if (!serverTimeIso.isNullOrBlank()) {
                    trustedOfflineClock?.recordServerSample(
                        serverTimeIso = serverTimeIso,
                        requestElapsedMillis = requestElapsedMillis,
                        responseElapsedMillis = responseElapsedMillis,
                    )
                }
            }
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
