package com.tbterminal.app.ui.cashier.sync

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tbterminal.app.data.repository.CashierSyncItemUi
import com.tbterminal.app.data.repository.CashierSyncRepository
import com.tbterminal.app.data.repository.CashierSyncSnapshot
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineSessionAgeAlert
import com.tbterminal.app.data.sync.OfflineSyncCoordinator
import com.tbterminal.app.data.sync.OfflineSyncScheduler
import com.tbterminal.app.data.sync.SyncManager
import com.tbterminal.app.data.sync.SyncNowScheduleResult
import com.tbterminal.app.data.sync.SyncRunOutcome
import com.tbterminal.app.data.sync.SyncRunSummary
import com.tbterminal.app.data.sync.SyncWorker
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface CashierSyncRunner {
    val isAnySyncRunning: Flow<Boolean>
    val lastRunSummary: Flow<SyncRunSummary?>
    suspend fun triggerManualSyncNow(): SyncNowScheduleResult
    suspend fun refreshServerStatus(): BackendStatus
}

class WorkManagerCashierSyncRunner(
    appContext: Context,
    private val offlineSyncScheduler: OfflineSyncScheduler,
    private val syncManager: SyncManager,
    private val backendHealthMonitor: BackendHealthMonitor,
) : CashierSyncRunner {
    private val workManager = WorkManager.getInstance(appContext.applicationContext)

    override val isAnySyncRunning: Flow<Boolean> = combine(
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.SYNC_NOW_WORK_NAME),
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME),
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.DELAYED_WORK_NAME),
        OfflineSyncCoordinator.isSyncing,
    ) { syncNowInfos, periodicInfos, delayedInfos, lockHeld ->
        val syncNowActive = syncNowInfos.any {
            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        }
        val otherRunning = periodicInfos.any { it.state == WorkInfo.State.RUNNING } ||
            delayedInfos.any { it.state == WorkInfo.State.RUNNING }
        lockHeld || syncNowActive || otherRunning
    }.distinctUntilChanged()

    override val lastRunSummary: Flow<SyncRunSummary?> = syncManager.lastRunSummary

    override suspend fun triggerManualSyncNow(): SyncNowScheduleResult {
        return offlineSyncScheduler.scheduleSyncNowIfConnected(includeFailed = true)
    }

    override suspend fun refreshServerStatus(): BackendStatus {
        return backendHealthMonitor.refresh(force = true)
    }
}

data class CashierSyncUiState(
    val isInitialLoading: Boolean = true,
    val isServerOnline: Boolean = false,
    val isInternetOnline: Boolean = true,
    val backendStatus: BackendStatus = BackendStatus.UNKNOWN,
    val lastSuccessfulSyncAtMillis: Long? = null,
    val pendingCount: Int = 0,
    val sendingCount: Int = 0,
    val failedCount: Int = 0,
    val needsCheckCount: Int = 0,
    val items: List<CashierSyncItemUi> = emptyList(),
    val offlineSessionAgeAlert: OfflineSessionAgeAlert? = null,
    val otherUsersPendingSyncCount: Int = 0,
    val isSyncing: Boolean = false,
    val feedbackMessage: String? = null,
    val isFeedbackError: Boolean = false,
)

class CashierSyncViewModel(
    private val repository: CashierSyncRepository,
    private val runner: CashierSyncRunner,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashierSyncUiState())
    val uiState: StateFlow<CashierSyncUiState> = _uiState.asStateFlow()

    private var latestSnapshot: CashierSyncSnapshot = CashierSyncSnapshot()
    private var latestSummary: SyncRunSummary? = null
    private var latestRunnerRunning: Boolean = false
    private var awaitingRunCompletion: Boolean = false
    private var sawRunningDuringAwait: Boolean = false
    private var runIdAtButtonTap: Long = 0L

    init {
        viewModelScope.launch {
            runner.refreshServerStatus()
        }
        viewModelScope.launch {
            combine(
                repository.observeSnapshot(),
                runner.isAnySyncRunning.onEach { running -> latestRunnerRunning = running },
                runner.lastRunSummary,
            ) { snapshot, runnerRunning, summary ->
                Triple(snapshot, runnerRunning, summary)
            }.collect { (snapshot, runnerRunning, summary) ->
                latestSnapshot = snapshot
                latestSummary = summary
                latestRunnerRunning = runnerRunning

                if (awaitingRunCompletion && runnerRunning) {
                    sawRunningDuringAwait = true
                }

                val hasNewSummary = summary != null && summary.runId > runIdAtButtonTap
                val shouldFinalize = awaitingRunCompletion &&
                    !runnerRunning &&
                    (hasNewSummary || sawRunningDuringAwait)

                var nextFeedback = _uiState.value.feedbackMessage
                var nextFeedbackError = _uiState.value.isFeedbackError

                if (shouldFinalize) {
                    awaitingRunCompletion = false
                    sawRunningDuringAwait = false
                    val (msg, isError) = formatCompletionFeedback(summary, hasNewSummary, snapshot.failedCount)
                    nextFeedback = msg
                    nextFeedbackError = isError
                }

                _uiState.update { current ->
                    current.copy(
                        isInitialLoading = false,
                        isServerOnline = snapshot.isServerOnline,
                        isInternetOnline = snapshot.isInternetOnline,
                        backendStatus = snapshot.backendStatus,
                        lastSuccessfulSyncAtMillis = snapshot.lastSuccessfulSyncAtMillis,
                        pendingCount = snapshot.pendingCount,
                        sendingCount = snapshot.sendingCount,
                        failedCount = snapshot.failedCount,
                        needsCheckCount = snapshot.needsCheckCount,
                        items = snapshot.items,
                        offlineSessionAgeAlert = snapshot.offlineSessionAgeAlert,
                        otherUsersPendingSyncCount = snapshot.otherUsersPendingSyncCount,
                        isSyncing = runnerRunning || awaitingRunCompletion,
                        feedbackMessage = nextFeedback,
                        isFeedbackError = nextFeedbackError,
                    )
                }
            }
        }
    }

    fun onSyncNowClick() {
        if (latestRunnerRunning || awaitingRunCompletion) {
            if (!awaitingRunCompletion) {
                runIdAtButtonTap = latestSummary?.runId ?: 0L
                awaitingRunCompletion = true
                sawRunningDuringAwait = latestRunnerRunning
            }
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    feedbackMessage = "Sedang sinkron…",
                    isFeedbackError = false,
                )
            }
            return
        }

        viewModelScope.launch {
            val serverReachable = if (latestSnapshot.isServerOnline) {
                true
            } else if (latestSnapshot.isInternetOnline && latestSnapshot.backendStatus == BackendStatus.UNKNOWN) {
                runner.refreshServerStatus() == BackendStatus.CONNECTED
            } else {
                false
            }

            if (!serverReachable) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Server sedang offline. Data belum bisa dikirim sekarang.",
                        isFeedbackError = true,
                    )
                }
                return@launch
            }

            runIdAtButtonTap = latestSummary?.runId ?: 0L
            awaitingRunCompletion = true
            sawRunningDuringAwait = false
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    feedbackMessage = null,
                    isFeedbackError = false,
                )
            }

            when (runner.triggerManualSyncNow()) {
                SyncNowScheduleResult.ENQUEUED -> Unit
                SyncNowScheduleResult.SKIPPED_OFFLINE_OR_DISCONNECTED -> {
                    awaitingRunCompletion = false
                    sawRunningDuringAwait = false
                    _uiState.update {
                        it.copy(
                            isSyncing = latestRunnerRunning,
                            feedbackMessage = "Server sedang offline. Data belum bisa dikirim sekarang.",
                            isFeedbackError = true,
                        )
                    }
                }
                SyncNowScheduleResult.SKIPPED_MODE_DISABLED -> {
                    awaitingRunCompletion = false
                    sawRunningDuringAwait = false
                    _uiState.update {
                        it.copy(
                            isSyncing = latestRunnerRunning,
                            feedbackMessage = "Mode sinkronisasi server belum aktif.",
                            isFeedbackError = true,
                        )
                    }
                }
            }
        }
    }

    fun refreshServerStatus() {
        viewModelScope.launch {
            runner.refreshServerStatus()
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, isFeedbackError = false) }
    }

    private fun formatCompletionFeedback(
        summary: SyncRunSummary?,
        hasNewSummary: Boolean,
        remainingFailedCount: Int,
    ): Pair<String, Boolean> {
        if (!hasNewSummary || summary == null) {
            return if (remainingFailedCount > 0) {
                "Sinkron selesai. Masih ada $remainingFailedCount data gagal. Tekan lagi untuk mencoba ulang data yang gagal." to true
            } else {
                "Sinkron selesai." to false
            }
        }

        val retryFailedHint = if (remainingFailedCount > 0 && !summary.includedFailed) {
            " Tekan lagi untuk mencoba ulang data yang gagal."
        } else {
            ""
        }

        return when (summary.outcome) {
            SyncRunOutcome.SUCCESS -> {
                val base = if (summary.syncedCount == 0 && summary.failedCount == 0) {
                    "Sinkron selesai: berhasil 0, gagal 0."
                } else {
                    "Sinkron selesai: berhasil ${summary.syncedCount}, gagal ${summary.failedCount}."
                }
                "$base$retryFailedHint" to (summary.failedCount > 0)
            }

            SyncRunOutcome.RETRY -> {
                val reason = summary.reason?.takeIf(String::isNotBlank) ?: "Koneksi server belum stabil."
                "Sinkron belum selesai: berhasil ${summary.syncedCount}, gagal ${summary.failedCount}. $reason$retryFailedHint" to true
            }

            SyncRunOutcome.AUTH_REQUIRED -> {
                "Sesi login habis. Silakan login ulang sebelum sinkronisasi." to true
            }

            SyncRunOutcome.SKIPPED -> {
                (summary.reason ?: "Sinkronisasi dilewati.") to false
            }

            SyncRunOutcome.FAILED -> {
                val reason = summary.reason?.takeIf(String::isNotBlank) ?: "Silakan coba beberapa saat lagi."
                "Sinkron gagal: berhasil ${summary.syncedCount}, gagal ${summary.failedCount}. $reason$retryFailedHint" to true
            }
        }
    }

    companion object {
        fun factory(
            repository: CashierSyncRepository,
            runner: CashierSyncRunner,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                CashierSyncViewModel(repository = repository, runner = runner)
            }
        }
    }
}
