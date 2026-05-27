package com.tbterminal.app.ui.salestransactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.CashReconciliationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AdminTransactionHistoryUiState(
    val transactions: List<CashTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val statusFilter: String = "Semua", // Semua, Lunas, DP, Hutang
    val selectedDate: String? = LocalDate.now().toString(),
    val currentStart: Int = 0,
    val currentEnd: Int = 0,
    val total: Long = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val hasMorePages: Boolean = false
)

class AdminTransactionHistoryViewModel(
    private val repository: CashReconciliationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminTransactionHistoryUiState())
    val uiState: StateFlow<AdminTransactionHistoryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTransactions()
    }

    fun loadTransactions(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Untuk Admin, kita pass sessionId = null agar menarik semua transaksi
            val statusParam = if (_uiState.value.statusFilter == "Semua") null else _uiState.value.statusFilter
            val searchParam = _uiState.value.query.takeIf { it.isNotBlank() }

            // Convert selectedDate (yyyy-MM-dd) to exact UTC bounds in local timezone to avoid backend timezone mismatch
            val startOfDay = _uiState.value.selectedDate?.let { dateStr ->
                try {
                    java.time.LocalDate.parse(dateStr).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime().toString()
                } catch (e: Exception) { dateStr }
            }
            val endOfDay = _uiState.value.selectedDate?.let { dateStr ->
                try {
                    java.time.LocalDate.parse(dateStr).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime().toString()
                } catch (e: Exception) { dateStr }
            }

            when (
                val result = repository.getTransactions(
                    page = page,
                    limit = 50,
                    sessionId = null,
                    search = searchParam,
                    status = statusParam,
                    startDate = startOfDay,
                    endDate = endOfDay
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        val data = result.data.data
                        val currentStart = if (data.isEmpty()) 0 else (result.data.page - 1) * 50 + 1
                        val currentEnd = currentStart + data.size - 1
                        it.copy(
                            transactions = data,
                            page = result.data.page,
                            totalPages = result.data.totalPages,
                            total = result.data.total,
                            currentStart = currentStart,
                            currentEnd = currentEnd,
                            hasMorePages = result.data.page < result.data.totalPages,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, error = "Koneksi ke server gagal") }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            loadTransactions(page = 1)
        }
    }

    fun updateStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        loadTransactions(page = 1)
    }

    fun setDate(date: String?) {
        _uiState.update { it.copy(selectedDate = date) }
        loadTransactions(page = 1)
    }

    fun nextDate() {
        _uiState.value.selectedDate?.let { date ->
            setDate(LocalDate.parse(date).plusDays(1).toString())
        }
    }

    fun previousDate() {
        _uiState.value.selectedDate?.let { date ->
            setDate(LocalDate.parse(date).minusDays(1).toString())
        }
    }

    fun nextPage() {
        if (_uiState.value.hasMorePages) {
            loadTransactions(_uiState.value.page + 1)
        }
    }

    fun previousPage() {
        if (_uiState.value.page > 1) {
            loadTransactions(_uiState.value.page - 1)
        }
    }

    companion object {
        fun factory(repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminTransactionHistoryViewModel(repository) as T
                }
            }
        }
    }
}
