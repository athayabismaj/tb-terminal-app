package com.tbterminal.app.data.sync

import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.database.TrustedClockAnchor
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TrustedClockStatus {
    CALIBRATED,
    DEVICE_CLOCK_SKEWED,
    UNCALIBRATED_AFTER_REBOOT,
    NO_ANCHOR,
}

data class TrustedClockReading(
    val millis: Long,
    val isAnchorValid: Boolean,
    val status: TrustedClockStatus,
    val skewMillis: Long? = null,
)

/**
 * Provides a monotonic server-calibrated clock resistant to manual device wall-clock changes.
 *
 * Calibrated from `/api/readiness` server timestamps using the RTT midpoint on
 * `SystemClock.elapsedRealtime()`. Persists anchor metadata in `app_settings` without
 * requiring Room schema changes.
 */
class TrustedOfflineClock(
    private val localAppSettingsDataSource: LocalAppSettingsDataSource,
    private val wallClockMillis: () -> Long = { System.currentTimeMillis() },
    private val elapsedRealtimeMillis: () -> Long = { defaultElapsedRealtimeMillis() },
    private val bootCountProvider: () -> Long? = { null },
    private val maxAcceptedRttMillis: Long = DEFAULT_MAX_ACCEPTED_RTT_MILLIS,
    private val skewWarningThresholdMillis: Long = DEFAULT_SKEW_WARNING_THRESHOLD_MILLIS,
) {
    private val anchorMutex = Mutex()
    private var loadedFromStore = false
    @Volatile
    private var cachedAnchor: TrustedClockAnchor? = null
    private val _anchorUpdates = MutableStateFlow<TrustedClockAnchor?>(null)
    val anchorUpdates: StateFlow<TrustedClockAnchor?> = _anchorUpdates.asStateFlow()

    suspend fun ensureLoaded(): TrustedClockAnchor? {
        if (loadedFromStore) return cachedAnchor
        return anchorMutex.withLock {
            if (!loadedFromStore) {
                val loaded = localAppSettingsDataSource.getTrustedClockAnchor()
                cachedAnchor = loaded
                _anchorUpdates.value = loaded
                loadedFromStore = true
            }
            cachedAnchor
        }
    }

    suspend fun recordServerSample(
        serverTimeIso: String?,
        requestElapsedMillis: Long,
        responseElapsedMillis: Long,
    ): Boolean {
        val serverTimeMillis = parseServerTimeMillis(serverTimeIso) ?: return false
        val rtt = responseElapsedMillis - requestElapsedMillis
        if (rtt < 0L || rtt > maxAcceptedRttMillis) {
            return false
        }
        val elapsedMid = requestElapsedMillis + (rtt / 2L)
        return anchorMutex.withLock {
            if (!loadedFromStore) {
                cachedAnchor = localAppSettingsDataSource.getTrustedClockAnchor()
                loadedFromStore = true
            }
            val existing = cachedAnchor
            val newLastTrusted = maxOf(existing?.lastTrustedMillis ?: 0L, serverTimeMillis)
            val anchor = TrustedClockAnchor(
                serverAnchorMillis = serverTimeMillis,
                elapsedAnchorMillis = elapsedMid,
                bootCount = bootCountProvider(),
                lastTrustedMillis = newLastTrusted,
            )
            localAppSettingsDataSource.setTrustedClockAnchor(anchor)
            cachedAnchor = anchor
            _anchorUpdates.value = anchor
            true
        }
    }

    suspend fun readNow(): TrustedClockReading {
        val anchor = ensureLoaded()
        return evaluateAnchor(anchor)
    }

    fun readNowCached(): TrustedClockReading {
        return evaluateAnchor(cachedAnchor)
    }

    suspend fun nowMillis(): Long = readNow().millis

    suspend fun nowMillisClamped(floorMillis: Long): Long = maxOf(readNow().millis, floorMillis)

    suspend fun readValidNowMillis(): Long? = readNow().takeIf { it.isAnchorValid }?.millis

    suspend fun hasValidAnchor(): Boolean = readNow().isAnchorValid

    suspend fun advanceLastTrustedFloor(timestampMillis: Long) {
        val reading = readNow()
        if (!reading.isAnchorValid) return
        anchorMutex.withLock {
            val existing = cachedAnchor ?: return@withLock
            if (timestampMillis > existing.lastTrustedMillis) {
                localAppSettingsDataSource.updateLastTrustedClockMillis(timestampMillis)
                val updated = existing.copy(lastTrustedMillis = timestampMillis)
                cachedAnchor = updated
                _anchorUpdates.value = updated
            }
        }
    }

    private fun evaluateAnchor(anchor: TrustedClockAnchor?): TrustedClockReading {
        val wallNow = wallClockMillis()
        val elapsedNow = elapsedRealtimeMillis()
        val currentBoot = bootCountProvider()
        if (anchor == null) {
            return TrustedClockReading(
                millis = wallNow,
                isAnchorValid = false,
                status = TrustedClockStatus.NO_ANCHOR,
                skewMillis = null,
            )
        }
        val validForCurrentBoot = isSameBootSession(anchor.bootCount, currentBoot) &&
            elapsedNow >= anchor.elapsedAnchorMillis
        if (validForCurrentBoot) {
            val trusted = anchor.serverAnchorMillis + (elapsedNow - anchor.elapsedAnchorMillis)
            val skew = wallNow - trusted
            val status = if (abs(skew) > skewWarningThresholdMillis) {
                TrustedClockStatus.DEVICE_CLOCK_SKEWED
            } else {
                TrustedClockStatus.CALIBRATED
            }
            return TrustedClockReading(
                millis = trusted,
                isAnchorValid = true,
                status = status,
                skewMillis = skew,
            )
        }
        val clampedFallback = maxOf(wallNow, anchor.lastTrustedMillis)
        return TrustedClockReading(
            millis = clampedFallback,
            isAnchorValid = false,
            status = TrustedClockStatus.UNCALIBRATED_AFTER_REBOOT,
            skewMillis = null,
        )
    }

    companion object {
        const val DEFAULT_MAX_ACCEPTED_RTT_MILLIS = 15_000L
        const val DEFAULT_SKEW_WARNING_THRESHOLD_MILLIS = 2L * 60_000L

        internal fun parseServerTimeMillis(raw: String?): Long? {
            val trimmed = raw?.trim()?.takeIf(String::isNotBlank) ?: return null
            return runCatching { OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }.getOrNull()
                ?: runCatching { Instant.parse(trimmed).toEpochMilli() }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(trimmed)
                        .atZone(ZoneId.of("Asia/Jakarta"))
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
        }
    }
}
