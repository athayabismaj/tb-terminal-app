package com.tbterminal.app.ui.quotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.QuotationDetail
import com.tbterminal.app.data.model.QuotationStatus
import com.tbterminal.app.data.model.QuotationSummary
import com.tbterminal.app.data.repository.QuotationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuotationsUiState(
    val quotations: List<QuotationSummary> = emptyList(),
    val selectedStatus: String = ALL_QUOTATION_STATUSES,
    val selectedPeriodPreset: String = "Semua",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val searchQuery: String = "",
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Long = 0,
    val isLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val isMutating: Boolean = false,
    val selectedDetail: QuotationDetail? = null,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
)

const val ALL_QUOTATION_STATUSES = "SEMUA"

class QuotationsViewModel(
    private val quotationRepository: QuotationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuotationsUiState(isLoading = true))
    val uiState: StateFlow<QuotationsUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadQuotations(page = 1)
    }

    fun loadQuotations(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            val statusParam = state.selectedStatus
                .takeIf { it != ALL_QUOTATION_STATUSES }
            val searchParam = state.searchQuery.trim().takeIf { it.isNotBlank() }
            val startParam = state.startDate?.toString()
            val endParam = state.endDate?.toString()
            _uiState.update { it.copy(isLoading = true, errorMessage = null, page = page) }

            when (
                val result = quotationRepository.getQuotations(
                    page = page,
                    limit = 20,
                    status = statusParam,
                    search = searchParam,
                    startDate = startParam,
                    endDate = endParam,
                )
            ) {
                is RepositoryResult.Success -> {
                    val rawData = result.data.data
                    val filteredData = if (state.startDate != null || state.endDate != null) {
                        rawData.filter { q ->
                            val date = runCatching { LocalDate.parse(q.createdAt.take(10)) }.getOrNull()
                            if (date == null) true
                            else {
                                val afterStart = state.startDate == null || !date.isBefore(state.startDate)
                                val beforeEnd = state.endDate == null || !date.isAfter(state.endDate)
                                afterStart && beforeEnd
                            }
                        }
                    } else {
                        rawData
                    }
                    val isClientFiltered = (state.startDate != null || state.endDate != null) && filteredData.size < rawData.size
                    val finalTotal = if (isClientFiltered) filteredData.size.toLong() else result.data.total

                    _uiState.update {
                        it.copy(
                            quotations = filteredData,
                            page = result.data.page,
                            totalPages = result.data.totalPages.coerceAtLeast(1),
                            total = finalTotal,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Koneksi bermasalah. Penawaran gagal dimuat.") }
                }
            }
        }
    }

    fun applyFilters(
        status: String,
        preset: String,
        start: LocalDate?,
        end: LocalDate?,
    ) {
        _uiState.update {
            it.copy(
                selectedStatus = status,
                selectedPeriodPreset = preset,
                startDate = start,
                endDate = end,
            )
        }
        loadQuotations(page = 1)
    }

    fun onStatusSelected(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadQuotations(page = 1)
    }

    fun clearStatusFilter() {
        _uiState.update { it.copy(selectedStatus = ALL_QUOTATION_STATUSES) }
        loadQuotations(page = 1)
    }

    fun clearDateFilter() {
        _uiState.update {
            it.copy(
                selectedPeriodPreset = "Semua",
                startDate = null,
                endDate = null,
            )
        }
        loadQuotations(page = 1)
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedStatus = ALL_QUOTATION_STATUSES,
                selectedPeriodPreset = "Semua",
                startDate = null,
                endDate = null,
            )
        }
        loadQuotations(page = 1)
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadQuotations(page = 1)
        }
    }

    fun openDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true) }
            when (val result = quotationRepository.getQuotationById(id)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(isDetailLoading = false, selectedDetail = result.data) }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isDetailLoading = false, actionMessage = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isDetailLoading = false, actionMessage = "Gagal memuat detail penawaran.") }
                }
            }
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedDetail = null) }
    }

    fun updateStatus(id: String, status: QuotationStatus) {
        if (_uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            when (val result = quotationRepository.updateQuotationStatus(id, status)) {
                is RepositoryResult.Success -> {
                    val updated = result.data
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            selectedDetail = if (it.selectedDetail?.id == id) updated else it.selectedDetail,
                            actionMessage = "Penawaran ${updated.quotationNumber} ditandai ${updated.status.displayLabel}.",
                        )
                    }
                    loadQuotations(_uiState.value.page)
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isMutating = false, actionMessage = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isMutating = false, actionMessage = "Gagal memperbarui status penawaran.") }
                }
            }
        }
    }

    fun nextPage() {
        val s = _uiState.value
        if (s.page < s.totalPages) loadQuotations(s.page + 1)
    }

    fun previousPage() {
        val s = _uiState.value
        if (s.page > 1) loadQuotations(s.page - 1)
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    companion object {
        fun factory(quotationRepository: QuotationRepository): ViewModelProvider.Factory = viewModelFactory {
            QuotationsViewModel(quotationRepository = quotationRepository)
        }
    }
}
