package com.tbterminal.app.ui.cashier.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CashierTransactionHistoryViewModel(
    private val repository: CashReconciliationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashierTransactionHistoryUiState(isLoading = true))
    val uiState: StateFlow<CashierTransactionHistoryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var allTransactions: List<CashTransaction> = emptyList()

    fun loadTransactions(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            when (val sessionResult = repository.getActiveSession()) {
                is RepositoryResult.Success -> {
                    val session = sessionResult.data
                    _uiState.update { state ->
                        state.copy(
                            activeSession = session,
                            page = page,
                            total = 0,
                            totalPages = 1,
                            transactions = emptyList()
                        )
                    }

                    if (session == null) {
                        loadSessionTransactions(null)
                    } else {
                        loadSessionTransactions(null)
                    }
                }

                is RepositoryResult.Error -> setListError(sessionResult.message)
                is RepositoryResult.Exception -> setListError("Koneksi bermasalah. Riwayat transaksi gagal dimuat.")
            }
        }
    }

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) {
            _uiState.update { it.copy(page = previous) }
            applyClientFilters()
        }
    }

    fun nextPage() {
        val state = _uiState.value
        val next = (state.page + 1).coerceAtMost(state.totalPages.coerceAtLeast(1))
        if (next != state.page) {
            _uiState.update { it.copy(page = next) }
            applyClientFilters()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(query = query, page = 1) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            applyClientFilters()
        }
    }

    fun updateStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status, page = 1) }
        applyClientFilters()
    }

    fun setDate(date: String?) {
        _uiState.update { it.copy(selectedDate = date, page = 1) }
        applyClientFilters()
    }

    fun previousDate() {
        val current = _uiState.value.selectedDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
        setDate(current.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    fun nextDate() {
        val current = _uiState.value.selectedDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val next = current.plusDays(1)
        // Don't allow future dates
        if (!next.isAfter(LocalDate.now())) {
            setDate(next.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
    }

    private fun applyClientFilters() {
        val state = _uiState.value
        var filtered = allTransactions

        // Filter by status
        if (state.statusFilter != "Semua") {
            val targetStatus = state.statusFilter.lowercase()
            filtered = filtered.filter { tx ->
                tx.status.lowercase() == targetStatus ||
                (targetStatus == "lunas" && tx.status.lowercase() in listOf("lunas", "paid", "success", "completed")) ||
                (targetStatus == "dp" && tx.status.lowercase() in listOf("dp", "partial")) ||
                (targetStatus == "hutang" && tx.status.lowercase() in listOf("hutang", "unpaid"))
            }
        }

        // Filter by search query
        val query = state.query.trim()
        if (query.isNotBlank()) {
            filtered = filtered.filter { tx ->
                tx.receiptId.contains(query, ignoreCase = true) ||
                (tx.customerName?.contains(query, ignoreCase = true) == true)
            }
        }

        // Filter by single date
        if (state.selectedDate != null) {
            filtered = filtered.filter { tx ->
                tx.createdAt.take(10) == state.selectedDate
            }
        }

        // Paginate
        val total = filtered.size.toLong()
        val totalPages = ((total + CASHIER_TRANSACTION_PAGE_SIZE - 1) / CASHIER_TRANSACTION_PAGE_SIZE).toInt().coerceAtLeast(1)
        val page = state.page.coerceIn(1, totalPages)
        val startIndex = (page - 1) * CASHIER_TRANSACTION_PAGE_SIZE
        val pageData = filtered.drop(startIndex).take(CASHIER_TRANSACTION_PAGE_SIZE)

        _uiState.update {
            it.copy(
                transactions = pageData,
                total = total,
                totalPages = totalPages,
                page = page,
                isLoading = false
            )
        }
    }

    fun loadReceipt(transactionId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isReceiptLoading = true, errorMessage = null, receiptMessage = null)
            }

            when (val result = repository.getTransactionById(transactionId)) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            selectedTransaction = result.data,
                            isReceiptLoading = false,
                            receiptMessage = null
                        )
                    }
                }
                is RepositoryResult.Error -> setReceiptError(result.message)
                is RepositoryResult.Exception -> setReceiptError("Koneksi bermasalah. Struk gagal dimuat.")
            }
        }
    }

    private suspend fun loadSessionTransactions(sessionId: String?) {
        when (
            val result = repository.getTransactions(
                page = 1,
                limit = 200,
                sessionId = sessionId
            )
        ) {
            is RepositoryResult.Success -> {
                allTransactions = result.data.data
                applyClientFilters()
            }

            is RepositoryResult.Error -> setListError(result.message)
            is RepositoryResult.Exception -> setListError("Koneksi bermasalah. Riwayat transaksi gagal dimuat.")
        }
    }

    private fun setListError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    private fun setReceiptError(message: String) {
        _uiState.update { state ->
            state.copy(isReceiptLoading = false, errorMessage = message)
        }
    }

    fun showPayDebtDialog() {
        _uiState.update { state ->
            val sisa = state.selectedTransaction?.remainingAmount()?.toPlainString() ?: ""
            state.copy(isPayDebtDialogOpen = true, payDebtAmountInput = sisa, payDebtMethodInput = "TUNAI")
        }
    }

    fun hidePayDebtDialog() {
        _uiState.update { it.copy(isPayDebtDialogOpen = false) }
    }

    fun onPayDebtAmountChanged(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        _uiState.update { it.copy(payDebtAmountInput = filtered) }
    }

    fun onPayDebtMethodChanged(method: String) {
        _uiState.update { it.copy(payDebtMethodInput = method) }
    }

    fun payDebt() {
        val state = _uiState.value
        val transactionId = state.selectedTransaction?.id ?: return
        val amount = state.payDebtAmountInput.toBigDecimalOrNull()
        if (amount == null || amount <= java.math.BigDecimal.ZERO) {
            setReceiptError("Nominal bayar tidak valid")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingDebt = true, errorMessage = null, receiptMessage = null) }
            
            when (val result = repository.payTransactionDebt(transactionId, amount, state.payDebtMethodInput)) {
                is RepositoryResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSubmittingDebt = false,
                            isPayDebtDialogOpen = false,
                            selectedTransaction = result.data,
                            receiptMessage = "Pelunasan berhasil disimpan"
                        )
                    }
                    // Refresh history transactions silently
                    loadSessionTransactions(state.activeSession?.id)
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isSubmittingDebt = false, errorMessage = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isSubmittingDebt = false, errorMessage = "Gagal memproses pelunasan") }
                }
            }
        }
    }

    companion object {
        fun factory(repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                CashierTransactionHistoryViewModel(repository)
            }
        }
    }
}

internal fun CashTransaction.remainingAmount() = total.subtract(paidAmount).coerceAtLeast(java.math.BigDecimal.ZERO)
internal fun com.tbterminal.app.data.model.CashTransactionDetail.remainingAmount() = total.subtract(paidAmount).coerceAtLeast(java.math.BigDecimal.ZERO)
