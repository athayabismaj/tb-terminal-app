package com.tbterminal.app.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CashReconciliationViewModel(
    private val repository: CashReconciliationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashReconciliationUiState(isLoading = true))
    val uiState: StateFlow<CashReconciliationUiState> = _uiState.asStateFlow()

    init {
        loadCash()
    }

    fun loadCash(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val sessionResult = repository.getActiveSession()) {
                is RepositoryResult.Success -> {
                    val session = sessionResult.data
                    _uiState.update {
                        it.copy(
                            activeSession = session,
                            page = page,
                            total = 0,
                            totalPages = 1,
                            transactions = emptyList()
                        )
                    }

                    if (session == null) {
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        loadTransactionsForSession(session.id, page)
                    }
                }

                is RepositoryResult.Error -> setLoadError(sessionResult.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Kas harian gagal dimuat.")
            }
        }
    }

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) loadCash(previous)
    }

    fun nextPage() {
        val state = _uiState.value
        val next = (state.page + 1).coerceAtMost(state.totalPages.coerceAtLeast(1))
        if (next != state.page) loadCash(next)
    }

    fun onOpeningCashChanged(value: String) {
        _uiState.update { it.copy(openingCashInput = value.moneyInput()) }
    }

    fun onClosingCashChanged(value: String) {
        _uiState.update { it.copy(closingCashInput = value.moneyInput()) }
    }

    fun onClosingNotesChanged(value: String) {
        _uiState.update { it.copy(closingNotesInput = value) }
    }

    fun openSession() {
        val startingCash = _uiState.value.openingCashInput.toBigDecimalOrNull()
            ?: return setActionError("Modal awal wajib diisi dengan angka valid.")
        if (startingCash < BigDecimal.ZERO) return setActionError("Modal awal tidak boleh kurang dari nol.")

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (val result = repository.openSession(startingCash)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            activeSession = result.data,
                            openingCashInput = "",
                            isSubmitting = false,
                            message = "Sesi kas berhasil dibuka."
                        )
                    }
                    loadCash(page = 1)
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Sesi kas gagal dibuka karena koneksi bermasalah.")
            }
        }
    }

    fun closeSession() {
        if (!_uiState.value.hasActiveSession) return setActionError("Tidak ada sesi kas aktif.")
        val closingCash = _uiState.value.closingCashInput.toBigDecimalOrNull()
            ?: return setActionError("Kas fisik akhir wajib diisi dengan angka valid.")
        if (closingCash < BigDecimal.ZERO) return setActionError("Kas fisik akhir tidak boleh kurang dari nol.")

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (
                val result = repository.closeSession(
                    endingCashPhysical = closingCash,
                    notes = _uiState.value.closingNotesInput.trim().takeIf(String::isNotBlank)
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            activeSession = result.data,
                            closingCashInput = "",
                            closingNotesInput = "",
                            isSubmitting = false,
                            message = "Sesi kas berhasil ditutup."
                        )
                    }
                    loadCash(page = 1)
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Sesi kas gagal ditutup karena koneksi bermasalah.")
            }
        }
    }

    fun showExpenseDialog() {
        _uiState.update { it.copy(isExpenseDialogOpen = true, expenseAmountInput = "", expenseDescriptionInput = "") }
    }

    fun hideExpenseDialog() {
        _uiState.update { it.copy(isExpenseDialogOpen = false) }
    }

    fun onExpenseAmountChanged(value: String) {
        _uiState.update { it.copy(expenseAmountInput = value.moneyInput()) }
    }

    fun onExpenseDescriptionChanged(value: String) {
        _uiState.update { it.copy(expenseDescriptionInput = value) }
    }

    fun addExpense() {
        val amount = _uiState.value.expenseAmountInput.toBigDecimalOrNull()
            ?: return setActionError("Nominal pengeluaran wajib diisi dengan angka valid.")
        if (amount <= BigDecimal.ZERO) return setActionError("Nominal pengeluaran harus lebih besar dari nol.")
        val desc = _uiState.value.expenseDescriptionInput.trim()
        if (desc.isEmpty()) return setActionError("Keterangan pengeluaran wajib diisi.")

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (val result = repository.addExpense(amount, desc)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExpenseDialogOpen = false,
                            isSubmitting = false,
                            message = "Pengeluaran berhasil dicatat."
                        )
                    }
                    loadCash(page = 1) // Refresh session data
                }

                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Gagal mencatat pengeluaran karena koneksi bermasalah.")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, message = null) }
    }

    private suspend fun loadTransactionsForSession(sessionId: String, page: Int) {
        val state = _uiState.value
        val txResult = repository.getTransactions(page = page, limit = state.limit, sessionId = sessionId)
        val exResult = repository.getExpenses(sessionId)

        if (txResult is RepositoryResult.Success) {
            val transactionsList = txResult.data.data.toMutableList()
            
            if (exResult is RepositoryResult.Success) {
                val mappedExpenses = exResult.data.map { ex ->
                    com.tbterminal.app.data.model.CashTransaction(
                        id = ex.id,
                        receiptId = ex.description,
                        sessionId = sessionId,
                        customerId = null,
                        customerName = null,
                        type = "EXPENSE",
                        status = "LUNAS",
                        total = ex.amount,
                        paidAmount = ex.amount,
                        createdAt = ex.createdAt
                    )
                }
                transactionsList.addAll(mappedExpenses)
                transactionsList.sortByDescending { it.createdAt }
            }

            _uiState.update {
                it.copy(
                    transactions = transactionsList,
                    page = txResult.data.page,
                    limit = txResult.data.limit,
                    total = txResult.data.total,
                    totalPages = txResult.data.totalPages.coerceAtLeast(1),
                    isLoading = false
                )
            }
        } else if (txResult is RepositoryResult.Error) {
            setLoadError(txResult.message)
        } else {
            setLoadError("Koneksi bermasalah. Transaksi sesi gagal dimuat.")
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, isSubmitting = false, errorMessage = message) }
    }

    private fun setActionError(message: String) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = message) }
    }

    companion object {
        fun factory(repository: CashReconciliationRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                CashReconciliationViewModel(repository)
            }
        }
    }
}

private fun String.moneyInput(): String {
    return filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
}
