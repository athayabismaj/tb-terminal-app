package com.tbterminal.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tbterminal.app.data.local.database.LocalAppSettingsDataSource
import com.tbterminal.app.data.local.model.AppDataMode
import java.util.concurrent.TimeUnit

class OfflineSyncScheduler(
    context: Context,
    private val localAppSettingsDataSource: LocalAppSettingsDataSource,
    private val networkMonitor: NetworkMonitor
) {
    private val appContext = context.applicationContext

    suspend fun scheduleIfEnabled() {
        if (localAppSettingsDataSource.getDataMode() != AppDataMode.SYNC_SERVER) return
        if (!networkMonitor.isOnline()) return
        schedule()
    }

    fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_MINUTES,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private companion object {
        const val BACKOFF_DELAY_MINUTES = 10L
    }
}
