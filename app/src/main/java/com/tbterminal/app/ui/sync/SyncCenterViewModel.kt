package com.tbterminal.app.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import com.tbterminal.app.data.repository.SyncQueueUiModel
import com.tbterminal.app.data.repository.SyncRetryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncCenterViewModel(
    private val repository: SyncMonitoringRepository,
    canRetry: Boolean
) : ViewModel() {
    private val _uiState = MutableStateFlow(SyncCenterUiState(canRetry = canRetry))
    val uiState: StateFlow<SyncCenterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        totalPending = snapshot.totalPending,
                        totalSyncing = snapshot.totalSyncing,
                        totalFailed = snapshot.totalFailed,
                        totalConflict = snapshot.totalConflict,
                        totalSyncedToday = snapshot.totalSyncedToday,
                        queueItems = snapshot.queueItems,
                        groupedItems = snapshot.queueItems.groupBy { item -> item.entityType }
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null, errorMessage = null) }
            runCatching { repository.refresh() }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            totalPending = snapshot.totalPending,
                            totalSyncing = snapshot.totalSyncing,
                            totalFailed = snapshot.totalFailed,
                            totalConflict = snapshot.totalConflict,
                            totalSyncedToday = snapshot.totalSyncedToday,
                            queueItems = snapshot.queueItems,
                            groupedItems = snapshot.queueItems.groupBy { item -> item.entityType },
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = error.message ?: "Gagal memuat status sinkronisasi."
                        )
                    }
                }
        }
    }

    fun retryItem(queueLocalId: Long) {
        if (!_uiState.value.canRetry) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true, message = null, errorMessage = null) }
            when (val result = repository.retryItem(queueLocalId)) {
                is SyncRetryResult.Success -> {
                    _uiState.update { it.copy(isRetrying = false, message = result.message) }
                }
                is SyncRetryResult.Failed -> {
                    _uiState.update { it.copy(isRetrying = false, errorMessage = result.message) }
                }
                is SyncRetryResult.Unsupported -> {
                    _uiState.update { it.copy(isRetrying = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun retryAllPending() {
        retryBatch(SyncBatchType.Pending)
    }

    fun retryAllFailed() {
        retryBatch(SyncBatchType.Failed)
    }

    fun markConflictReviewed(queueLocalId: Long) {
        if (!_uiState.value.canRetry) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true, message = null, errorMessage = null) }
            when (val result = repository.markConflictReviewed(queueLocalId)) {
                is SyncRetryResult.Success -> {
                    _uiState.update { it.copy(isRetrying = false, message = result.message) }
                }
                is SyncRetryResult.Failed -> {
                    _uiState.update { it.copy(isRetrying = false, errorMessage = result.message) }
                }
                is SyncRetryResult.Unsupported -> {
                    _uiState.update { it.copy(isRetrying = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun retryBatch(type: SyncBatchType) {
        if (!_uiState.value.canRetry) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true, message = null, errorMessage = null) }
            runCatching {
                when (type) {
                    SyncBatchType.Pending -> repository.retryAllPending()
                    SyncBatchType.Failed -> repository.retryAllFailed()
                }
            }.onSuccess { result ->
                val message = when {
                    result.total == 0 -> "Tidak ada item yang perlu disinkronkan."
                    result.failedCount == 0 -> "${result.successCount} item berhasil disinkronkan."
                    else -> "${result.successCount} berhasil, ${result.failedCount} gagal. Periksa detail error."
                }
                _uiState.update { it.copy(isRetrying = false, message = message) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRetrying = false,
                        errorMessage = error.message ?: "Retry sinkronisasi gagal."
                    )
                }
            }
        }
    }

    private enum class SyncBatchType {
        Pending,
        Failed
    }

    companion object {
        fun factory(
            repository: SyncMonitoringRepository,
            canRetry: Boolean
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                SyncCenterViewModel(repository = repository, canRetry = canRetry)
            }
        }
    }
}

data class SyncCenterUiState(
    val totalPending: Int = 0,
    val totalSyncing: Int = 0,
    val totalFailed: Int = 0,
    val totalConflict: Int = 0,
    val totalSyncedToday: Int = 0,
    val queueItems: List<SyncQueueUiModel> = emptyList(),
    val groupedItems: Map<SyncEntityType, List<SyncQueueUiModel>> = emptyMap(),
    val isRefreshing: Boolean = false,
    val isRetrying: Boolean = false,
    val canRetry: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)
