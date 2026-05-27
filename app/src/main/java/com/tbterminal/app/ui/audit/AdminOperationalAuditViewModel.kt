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
    val limit: Int = 20,
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
            systemRepository.getAuditLogs(
                page = page,
                limit = _uiState.value.limit,
                action = action
            ).fold(
                onSuccess = { pageData ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        logs = pageData.data,
                        currentPage = pageData.page,
                        totalPages = pageData.totalPages,
                        selectedAction = action
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Terjadi kesalahan saat memuat log audit"
                    )
                }
            )
        }
    }

    fun setActionFilter(action: String?) {
        loadLogs(page = 1, action = action)
    }
}
