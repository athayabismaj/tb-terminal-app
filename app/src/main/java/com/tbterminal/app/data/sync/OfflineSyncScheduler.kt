package com.tbterminal.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.model.AppDataMode
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SyncNowScheduleResult {
    ENQUEUED,
    SKIPPED_OFFLINE_OR_DISCONNECTED,
    SKIPPED_MODE_DISABLED,
}

class OfflineSyncScheduler(
    context: Context,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource,
    private val networkMonitor: NetworkConnectivity,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val backendHealthMonitor: BackendHealthMonitor? = null,
) {
    private val appContext = context.applicationContext

    suspend fun scheduleIfEnabled() = withContext(ioDispatcher) {
        if (localAppSettingsDataSource.getDataMode() != AppDataMode.SYNC_SERVER) return@withContext
        if (!networkMonitor.isOnline()) return@withContext
        schedule()
    }

    suspend fun scheduleSyncNowIfConnected(
        includeFailed: Boolean = false,
    ): SyncNowScheduleResult = withContext(ioDispatcher) {
        if (localAppSettingsDataSource.getDataMode() != AppDataMode.SYNC_SERVER) {
            return@withContext SyncNowScheduleResult.SKIPPED_MODE_DISABLED
        }
        if (!networkMonitor.isOnline()) {
            return@withContext SyncNowScheduleResult.SKIPPED_OFFLINE_OR_DISCONNECTED
        }
        val health = backendHealthMonitor
        if (health != null && health.status.value != BackendStatus.CONNECTED) {
            return@withContext SyncNowScheduleResult.SKIPPED_OFFLINE_OR_DISCONNECTED
        }
        scheduleSyncNow(includeFailed = includeFailed)
        SyncNowScheduleResult.ENQUEUED
    }

    fun scheduleSyncNow(includeFailed: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(
                workDataOf(
                    SyncWorker.KEY_IS_SYNC_NOW to true,
                    SyncWorker.KEY_INCLUDE_FAILED to includeFailed,
                )
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SyncWorker.SYNC_NOW_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun schedule() {
        val request = buildSyncWorkRequest()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleDelayed(delaySeconds: Long) {
        val clampedDelay = delaySeconds.coerceIn(MIN_DELAYED_SECONDS, MAX_DELAYED_SECONDS)
        val request = buildSyncWorkRequest(initialDelaySeconds = clampedDelay)
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SyncWorker.DELAYED_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun scheduleImmediateContinuation() {
        val request = buildSyncWorkRequest()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SyncWorker.DELAYED_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    private fun buildSyncWorkRequest(initialDelaySeconds: Long? = null) =
        OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .apply {
                if (initialDelaySeconds != null && initialDelaySeconds > 0L) {
                    setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
                }
            }
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

    private companion object {
        const val BACKOFF_DELAY_SECONDS = 60L
        const val MIN_DELAYED_SECONDS = 1L
        const val MAX_DELAYED_SECONDS = 3600L
    }
}
