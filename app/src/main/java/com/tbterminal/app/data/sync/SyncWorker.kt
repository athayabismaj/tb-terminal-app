package com.tbterminal.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tbterminal.app.TbTerminalApplication

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? TbTerminalApplication ?: return Result.failure()
        return executeSyncWork(
            runOnce = { app.appContainer.syncManager.runOnce() },
            scheduleDelayed = { delaySeconds -> app.appContainer.offlineSyncScheduler.scheduleDelayed(delaySeconds) },
            scheduleContinuation = { app.appContainer.offlineSyncScheduler.scheduleImmediateContinuation() },
            runAttemptCount = runAttemptCount,
        )
    }

    companion object {
        const val WORK_NAME = "tb_terminal_sync_worker"
        const val DELAYED_WORK_NAME = "tb_terminal_sync_worker_delayed"
        internal const val MAX_RETRY_ATTEMPTS = 3
        internal const val MAX_TRANSIENT_WORKER_ATTEMPTS = 20

        internal suspend fun executeSyncWork(
            runOnce: suspend () -> SyncResult,
            scheduleDelayed: (Long) -> Unit,
            scheduleContinuation: () -> Unit,
            runAttemptCount: Int,
        ): Result {
            return when (val result = runOnce()) {
                is SyncResult.Success -> {
                    if (result.hasMorePending) {
                        scheduleContinuation()
                    }
                    Result.success()
                }
                is SyncResult.Skipped -> Result.success()
                is SyncResult.AuthRequired -> Result.success()
                is SyncResult.Retry -> {
                    if (result.retryAfterSeconds != null) {
                        scheduleDelayed(result.retryAfterSeconds)
                        Result.success()
                    } else if (runAttemptCount < MAX_TRANSIENT_WORKER_ATTEMPTS) {
                        Result.retry()
                    } else {
                        // Biarkan item tetap PENDING untuk dicoba lagi saat koneksi/foreground berikutnya.
                        Result.success()
                    }
                }
                is SyncResult.Failed -> {
                    if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
                        Result.failure()
                    } else {
                        Result.retry()
                    }
                }
            }
        }
    }
}
