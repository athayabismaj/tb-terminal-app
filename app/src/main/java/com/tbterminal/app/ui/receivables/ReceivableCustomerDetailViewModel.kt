package com.tbterminal.app.ui.receivables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ReceivableCustomerDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val receivables: List<Receivable> = emptyList(),
    val payments: List<ReceivablePaymentHistory> = emptyList()
) {
    val customerName: String
        get() = receivables.firstOrNull()?.customerName
            ?: payments.firstOrNull()?.customerName
            ?: "Pelanggan"
    val totalAmount: BigDecimal get() = receivables.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
    val totalPaid: BigDecimal get() = receivables.fold(BigDecimal.ZERO) { sum, item -> sum + item.paidAmount }
    val totalRemaining: BigDecimal get() = receivables.fold(BigDecimal.ZERO) { sum, item -> sum + item.remainingAmount }
}

internal class ReceivableCustomerDetailViewModel(
    private val receivableRepository: ReceivableRepository,
    private val customerId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceivableCustomerDetailUiState())
    val uiState: StateFlow<ReceivableCustomerDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val receivables = receivableRepository.getReceivables(
                page = 1,
                limit = DETAIL_LIMIT,
                customerId = customerId
            )
            val payments = receivableRepository.getReceivablePayments(
                page = 1,
                limit = DETAIL_LIMIT,
                customerId = customerId
            )
            if (receivables is RepositoryResult.Success) {
                val sortedReceivables = receivables.data.data.sortedWith(
                    compareByDescending<Receivable> { it.debtDate }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.id }
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        receivables = sortedReceivables,
                        payments = (payments as? RepositoryResult.Success)?.data?.data.orEmpty()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = (receivables as? RepositoryResult.Error)?.message
                            ?: "Detail piutang gagal dimuat. Periksa koneksi lalu coba lagi."
                    )
                }
            }
        }
    }

    companion object {
        private const val DETAIL_LIMIT = 100

        fun factory(receivableRepository: ReceivableRepository, customerId: String): ViewModelProvider.Factory =
            viewModelFactory { ReceivableCustomerDetailViewModel(receivableRepository, customerId) }
    }
}
