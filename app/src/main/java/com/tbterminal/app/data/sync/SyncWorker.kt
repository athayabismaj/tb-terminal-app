package com.tbterminal.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tbterminal.app.TbTerminalApplication

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? TbTerminalApplication ?: return Result.failure()
        val isSyncNow = inputData.getBoolean(KEY_IS_SYNC_NOW, false)
        val includeFailed = inputData.getBoolean(KEY_INCLUDE_FAILED, false)
        return executeSyncWork(
            runOnce = { app.appContainer.syncManager.runOnce(includeFailed = includeFailed) },
            scheduleDelayed = { delaySeconds -> app.appContainer.offlineSyncScheduler.scheduleDelayed(delaySeconds) },
            scheduleContinuation = { app.appContainer.offlineSyncScheduler.scheduleImmediateContinuation() },
            runAttemptCount = runAttemptCount,
            isSyncNow = isSyncNow,
            includeFailed = includeFailed,
        )
    }

    companion object {
        const val WORK_NAME = "tb_terminal_sync_worker"
        const val DELAYED_WORK_NAME = "tb_terminal_sync_worker_delayed"
        const val SYNC_NOW_WORK_NAME = "tb_terminal_sync_now"
        const val KEY_IS_SYNC_NOW = "is_sync_now"
        const val KEY_INCLUDE_FAILED = "include_failed"
        const val KEY_OUTCOME = "outcome"
        const val KEY_SYNCED_COUNT = "synced_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_INCLUDED_FAILED = "included_failed"
        const val KEY_HAS_MORE_PENDING = "has_more_pending"
        const val KEY_REASON = "reason"
        internal const val MAX_RETRY_ATTEMPTS = 3
        internal const val MAX_TRANSIENT_WORKER_ATTEMPTS = 20

        internal suspend fun executeSyncWork(
            runOnce: suspend () -> SyncResult,
            scheduleDelayed: (Long) -> Unit,
            scheduleContinuation: () -> Unit,
            runAttemptCount: Int,
            isSyncNow: Boolean = false,
            includeFailed: Boolean = false,
        ): Result {
            return when (val result = runOnce()) {
                is SyncResult.Success -> {
                    if (result.hasMorePending) {
                        scheduleContinuation()
                    } else if (!isSyncNow && result.deferredRetryAfterSeconds != null) {
                        scheduleDelayed(result.deferredRetryAfterSeconds)
                    }
                    if (isSyncNow) {
                        Result.success(
                            buildOutputData(
                                outcome = SyncRunOutcome.SUCCESS.name,
                                syncedCount = result.successCount,
                                failedCount = result.failedCount,
                                includedFailed = includeFailed,
                                hasMorePending = result.hasMorePending,
                                reason = null,
                            )
                        )
                    } else {
                        Result.success()
                    }
                }
                is SyncResult.Skipped -> if (isSyncNow) {
                    Result.success(
                        buildOutputData(
                            outcome = SyncRunOutcome.SKIPPED.name,
                            syncedCount = 0,
                            failedCount = 0,
                            includedFailed = includeFailed,
                            hasMorePending = false,
                            reason = result.reason,
                        )
                    )
                } else {
                    Result.success()
                }
                is SyncResult.AuthRequired -> if (isSyncNow) {
                    Result.success(
                        buildOutputData(
                            outcome = SyncRunOutcome.AUTH_REQUIRED.name,
                            syncedCount = 0,
                            failedCount = 0,
                            includedFailed = includeFailed,
                            hasMorePending = false,
                            reason = result.reason,
                        )
                    )
                } else {
                    Result.success()
                }
                is SyncResult.Retry -> {
                    if (isSyncNow) {
                        // Work "sync now" bersifat sekali jalan: jangan pernah Result.retry() agar
                        // tombol manual tidak terkunci backoff.
                        Result.success(
                            buildOutputData(
                                outcome = SyncRunOutcome.RETRY.name,
                                syncedCount = result.successCount,
                                failedCount = result.failedCount,
                                includedFailed = includeFailed,
                                hasMorePending = false,
                                reason = result.reason,
                            )
                        )
                    } else if (result.retryAfterSeconds != null) {
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
                    if (isSyncNow) {
                        Result.success(
                            buildOutputData(
                                outcome = SyncRunOutcome.FAILED.name,
                                syncedCount = 0,
                                failedCount = 0,
                                includedFailed = includeFailed,
                                hasMorePending = false,
                                reason = result.error.message,
                            )
                        )
                    } else if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
                        Result.failure()
                    } else {
                        Result.retry()
                    }
                }
            }
        }

        private fun buildOutputData(
            outcome: String,
            syncedCount: Int,
            failedCount: Int,
            includedFailed: Boolean,
            hasMorePending: Boolean,
            reason: String?,
        ): Data = workDataOf(
            KEY_OUTCOME to outcome,
            KEY_SYNCED_COUNT to syncedCount,
            KEY_FAILED_COUNT to failedCount,
            KEY_INCLUDED_FAILED to includedFailed,
            KEY_HAS_MORE_PENDING to hasMorePending,
            KEY_REASON to reason,
        )
    }
}
