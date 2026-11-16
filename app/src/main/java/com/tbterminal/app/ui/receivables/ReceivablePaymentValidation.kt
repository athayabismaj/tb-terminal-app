package com.tbterminal.app.ui.receivables

import com.tbterminal.app.data.model.Receivable
import java.math.BigDecimal

internal fun validateReceivablePaymentInput(
    amountInput: String,
    remainingAmount: BigDecimal,
    idempotencyKey: String
): Result<BigDecimal> = runCatching {
    val amount = amountInput.toBigDecimalOrNull()
        ?: error("Nominal pembayaran wajib diisi dengan angka valid.")
    require(amount > BigDecimal.ZERO) { "Nominal pembayaran harus lebih dari nol." }
    require(amount.scale() <= 2) { "Nominal pembayaran maksimal dua angka desimal." }
    require(amount <= remainingAmount) { "Pembayaran tidak boleh melebihi sisa piutang." }
    require(idempotencyKey.length in 8..100) { "Kunci retry pembayaran tidak valid." }
    amount
}

internal data class InvoicePaymentAllocation(
    val receivable: Receivable,
    val isSelected: Boolean,
    val allocatedAmount: BigDecimal,
    val remainingAfterPayment: BigDecimal
) {
    val isFullyPaid: Boolean get() = isSelected && remainingAfterPayment <= BigDecimal.ZERO
    val isPartiallyPaid: Boolean get() = isSelected && allocatedAmount > BigDecimal.ZERO && remainingAfterPayment > BigDecimal.ZERO
    val isUnallocated: Boolean get() = isSelected && allocatedAmount <= BigDecimal.ZERO
}

internal fun calculateFifoAllocations(
    invoices: List<Receivable>,
    selectedIds: Set<String>,
    totalPaymentAmount: BigDecimal
): List<InvoicePaymentAllocation> {
    val sortedInvoices = invoices.sortedWith(
        compareBy<Receivable>({ it.dueDate }, { it.debtDate }, { it.id })
    )

    var remainingCash = totalPaymentAmount.max(BigDecimal.ZERO)

    return sortedInvoices.map { invoice ->
        val isSelected = selectedIds.contains(invoice.id)
        if (!isSelected) {
            InvoicePaymentAllocation(
                receivable = invoice,
                isSelected = false,
                allocatedAmount = BigDecimal.ZERO,
                remainingAfterPayment = invoice.remainingAmount
            )
        } else {
            val needed = invoice.remainingAmount.max(BigDecimal.ZERO)
            val allocated = needed.min(remainingCash)
            remainingCash = (remainingCash - allocated).max(BigDecimal.ZERO)
            val remainingAfter = (needed - allocated).max(BigDecimal.ZERO)

            InvoicePaymentAllocation(
                receivable = invoice,
                isSelected = true,
                allocatedAmount = allocated,
                remainingAfterPayment = remainingAfter
            )
        }
    }
}

