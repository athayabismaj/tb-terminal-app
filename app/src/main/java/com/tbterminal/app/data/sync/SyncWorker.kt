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
        return when (app.appContainer.syncManager.runOnce()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Skipped -> Result.success()
            is SyncResult.AuthRequired -> Result.success()
            is SyncResult.Retry -> Result.retry()
            is SyncResult.Failed -> {
                if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "tb_terminal_sync_worker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
