package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreateReceivablePaymentCommand
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.ReceivablePage
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.model.ReceivablePaymentHistoryPage
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ReceivableApi
import com.tbterminal.app.data.remote.ReceivablePaymentRequestDto
import com.tbterminal.app.data.remote.ReceivablePaymentHistoryResponseDto
import com.tbterminal.app.data.remote.ReceivablePaymentResponseDto
import com.tbterminal.app.data.remote.ReceivableResponseDto
import com.tbterminal.app.data.remote.safeApiCall

interface ReceivableRepository {
    suspend fun getReceivables(
        page: Int = 1,
        limit: Int = 20,
        customerId: String? = null,
        status: String? = null
    ): RepositoryResult<ReceivablePage>

    suspend fun getReceivableById(id: String): RepositoryResult<Receivable>

    suspend fun createReceivablePayment(
        command: CreateReceivablePaymentCommand
    ): RepositoryResult<ReceivablePaymentReceipt>

    suspend fun getReceivablePayments(
        page: Int = 1,
        limit: Int = 20,
        customerId: String? = null
    ): RepositoryResult<ReceivablePaymentHistoryPage>
}

class RemoteReceivableRepository(
    private val receivableApi: ReceivableApi
) : ReceivableRepository {
    override suspend fun getReceivables(
        page: Int,
        limit: Int,
        customerId: String?,
        status: String?
    ): RepositoryResult<ReceivablePage> {
        return safeApiCall {
            receivableApi.getReceivables(
                page = page,
                limit = limit,
                customerId = customerId?.takeIf(String::isNotBlank),
                status = status?.takeIf(String::isNotBlank)
            )
        }.toRepositoryResult { response ->
            val receivablePage = response.data
            if (!response.success || receivablePage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "RECEIVABLES_FAILED",
                    message = response.message ?: response.error ?: "Piutang pelanggan gagal dimuat."
                )
            } else {
                RepositoryResult.Success(receivablePage.toReceivablePage())
            }
        }
    }

    override suspend fun getReceivableById(id: String): RepositoryResult<Receivable> {
        return safeApiCall { receivableApi.getReceivableById(id) }
            .toRepositoryResult { response ->
                val receivable = response.data
                if (!response.success || receivable == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "RECEIVABLE_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail piutang gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(receivable.toReceivable())
                }
            }
    }

    override suspend fun createReceivablePayment(
        command: CreateReceivablePaymentCommand
    ): RepositoryResult<ReceivablePaymentReceipt> {
        val request = ReceivablePaymentRequestDto(
            receivableId = command.receivableId,
            amount = command.amount,
            method = command.method,
            reference = command.reference?.takeIf(String::isNotBlank),
            notes = command.notes?.takeIf(String::isNotBlank)
        )

        return safeApiCall { receivableApi.createReceivablePayment(request) }
            .toRepositoryResult { response ->
                val payment = response.data
                if (!response.success || payment == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "RECEIVABLE_PAYMENT_FAILED",
                        message = response.message ?: response.error ?: "Pembayaran piutang gagal dicatat."
                    )
                } else {
                    RepositoryResult.Success(payment.toReceipt())
                }
            }
    }

    override suspend fun getReceivablePayments(
        page: Int,
        limit: Int,
        customerId: String?
    ): RepositoryResult<ReceivablePaymentHistoryPage> {
        return safeApiCall {
            receivableApi.getReceivablePayments(
                page = page,
                limit = limit,
                customerId = customerId?.takeIf(String::isNotBlank)
            )
        }.toRepositoryResult { response ->
            val paymentPage = response.data
            if (!response.success || paymentPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "RECEIVABLE_PAYMENTS_FAILED",
                    message = response.message ?: response.error ?: "Riwayat pembayaran piutang gagal dimuat."
                )
            } else {
                RepositoryResult.Success(paymentPage.toReceivablePaymentHistoryPage())
            }
        }
    }
}

private fun PaginatedResponse<ReceivableResponseDto>.toReceivablePage(): ReceivablePage {
    return ReceivablePage(
        data = data.map(ReceivableResponseDto::toReceivable),
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun ReceivableResponseDto.toReceivable(): Receivable {
    return Receivable(
        id = id,
        customerId = customerId,
        customerName = customerName,
        transactionId = transactionId,
        amount = amount,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        dueDate = dueDate,
        status = status,
        createdAt = createdAt
    )
}

private fun ReceivablePaymentResponseDto.toReceipt(): ReceivablePaymentReceipt {
    return ReceivablePaymentReceipt(
        id = id,
        receivableId = receivableId,
        amount = amount,
        method = method,
        reference = reference,
        notes = notes,
        paidAt = paidAt,
        receivableStatus = receivableStatus,
        receivableRemainingAmount = receivableRemainingAmount
    )
}

private fun PaginatedResponse<ReceivablePaymentHistoryResponseDto>.toReceivablePaymentHistoryPage(): ReceivablePaymentHistoryPage {
    return ReceivablePaymentHistoryPage(
        data = data.map(ReceivablePaymentHistoryResponseDto::toPaymentHistory),
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun ReceivablePaymentHistoryResponseDto.toPaymentHistory(): ReceivablePaymentHistory {
    return ReceivablePaymentHistory(
        id = id,
        receivableId = receivableId,
        customerId = customerId,
        customerName = customerName,
        transactionId = transactionId,
        amount = amount,
        method = method,
        reference = reference,
        notes = notes,
        paidAt = paidAt,
        receivableStatus = receivableStatus,
        receivableRemainingAmount = receivableRemainingAmount
    )
}
