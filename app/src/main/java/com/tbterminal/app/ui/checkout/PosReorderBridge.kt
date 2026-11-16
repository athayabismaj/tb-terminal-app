package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.QuotationDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PosReorderBridge {
    private val _pendingReorder = MutableStateFlow<CashTransactionDetail?>(null)
    val pendingReorder: StateFlow<CashTransactionDetail?> = _pendingReorder.asStateFlow()

    private val _pendingQuotation = MutableStateFlow<QuotationDetail?>(null)
    val pendingQuotation: StateFlow<QuotationDetail?> = _pendingQuotation.asStateFlow()

    fun enqueue(transaction: CashTransactionDetail) {
        _pendingReorder.value = transaction
    }

    fun consume(): CashTransactionDetail? {
        var captured: CashTransactionDetail? = null
        _pendingReorder.update { current ->
            captured = current
            null
        }
        return captured
    }

    fun enqueueQuotation(quotation: QuotationDetail) {
        _pendingQuotation.value = quotation
    }

    fun consumeQuotation(): QuotationDetail? {
        var captured: QuotationDetail? = null
        _pendingQuotation.update { current ->
            captured = current
            null
        }
        return captured
    }
}
