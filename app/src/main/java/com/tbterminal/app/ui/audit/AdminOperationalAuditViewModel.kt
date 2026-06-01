package com.tbterminal.app.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.AuditLogItem
import com.tbterminal.app.data.repository.SystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminOperationalAuditUiState(
    val isLoading: Boolean = false,
    val logs: List<AuditLogItem> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val limit: Int = 100,
    val selectedAction: String? = null
)

class AdminOperationalAuditViewModel(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOperationalAuditUiState())
    val uiState: StateFlow<AdminOperationalAuditUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs(page: Int = _uiState.value.currentPage, action: String? = _uiState.value.selectedAction) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = systemRepository.getAuditLogs(
                page = page,
                limit = _uiState.value.limit,
                action = action
            )
            when (result) {
                is com.tbterminal.app.data.remote.NetworkResult.Success -> {
                    val apiResponse = result.data
                    val pageData = apiResponse.data
                    val operationalLogs = pageData
                        ?.data
                        .orEmpty()
                        .filter(AuditLogItem::isOperationalAudit)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        logs = operationalLogs,
                        currentPage = pageData?.page ?: 1,
                        totalPages = pageData?.totalPages ?: 1,
                        selectedAction = action
                    )
                }
                is com.tbterminal.app.data.remote.NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Terjadi kesalahan saat memuat log audit"
                    )
                }
                is com.tbterminal.app.data.remote.NetworkResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.e.message ?: "Terjadi kesalahan saat memuat log audit"
                    )
                }
            }
        }
    }

    fun setActionFilter(action: String?) {
        loadLogs(page = 1, action = action)
    }

    companion object {
        fun factory(repository: SystemRepository): androidx.lifecycle.ViewModelProvider.Factory = 
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AdminOperationalAuditViewModel(repository) as T
                }
            }
    }
}
