package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreateReceivablePaymentCommand
import com.tbterminal.app.data.model.CreateOpeningReceivableCommand
import com.tbterminal.app.data.model.CreateReceivableAdjustmentCommand
import com.tbterminal.app.data.model.CustomerReceivableSummary
import com.tbterminal.app.data.model.CustomerReceivableSummaryPage
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.model.ReceivablePage
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.model.ReceivablePaymentHistoryPage
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import com.tbterminal.app.data.model.ReverseReceivablePaymentCommand
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ReceivableApi
import com.tbterminal.app.data.remote.ReceivablePaymentRequestDto
import com.tbterminal.app.data.remote.ReceivablePaymentHistoryResponseDto
import com.tbterminal.app.data.remote.ReceivablePaymentResponseDto
import com.tbterminal.app.data.remote.ReceivableResponseDto
import com.tbterminal.app.data.remote.ReverseReceivablePaymentRequestDto
import com.tbterminal.app.data.remote.CreateStandaloneReceivableRequestDto
import com.tbterminal.app.data.remote.CustomerReceivableSummaryResponseDto
import com.tbterminal.app.data.remote.safeApiCall

interface ReceivableRepository {
    suspend fun getReceivables(
        page: Int = 1,
        limit: Int = 20,
        customerId: String? = null,
        status: String? = null,
        dueFilter: String? = null,
        dueFrom: String? = null,
        dueTo: String? = null
    ): RepositoryResult<ReceivablePage>

    suspend fun createOpeningBalance(command: CreateOpeningReceivableCommand): RepositoryResult<Receivable>

    suspend fun createAdjustment(command: CreateReceivableAdjustmentCommand): RepositoryResult<Receivable>

    suspend fun getCustomerSummaries(
        page: Int = 1,
        limit: Int = 20,
        dueFilter: String? = null
    ): RepositoryResult<CustomerReceivableSummaryPage>

    suspend fun getReceivableById(id: String): RepositoryResult<Receivable>

    suspend fun createReceivablePayment(
        command: CreateReceivablePaymentCommand
    ): RepositoryResult<ReceivablePaymentReceipt>

    suspend fun getReceivablePayments(
        page: Int = 1,
        limit: Int = 20,
        receivableId: String? = null,
        customerId: String? = null,
        method: String? = null,
        userId: String? = null,
        customerSearch: String? = null,
        receiverSearch: String? = null,
        status: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): RepositoryResult<ReceivablePaymentHistoryPage>

    suspend fun getReceivablePaymentReceipt(paymentId: String): RepositoryResult<ReceivablePaymentHistory>

    suspend fun reverseReceivablePayment(
        command: ReverseReceivablePaymentCommand
    ): RepositoryResult<ReceivablePaymentReceipt>
}

class RemoteReceivableRepository(
    private val receivableApi: ReceivableApi
) : ReceivableRepository {
    override suspend fun getReceivables(
        page: Int,
        limit: Int,
        customerId: String?,
        status: String?,
        dueFilter: String?,
        dueFrom: String?,
        dueTo: String?
    ): RepositoryResult<ReceivablePage> {
        return safeApiCall {
            receivableApi.getReceivables(
                page = page,
                limit = limit,
                customerId = customerId?.takeIf(String::isNotBlank),
                status = status?.takeIf(String::isNotBlank),
                dueFilter = dueFilter?.takeIf(String::isNotBlank),
                dueFrom = dueFrom?.takeIf(String::isNotBlank),
                dueTo = dueTo?.takeIf(String::isNotBlank)
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

    override suspend fun createOpeningBalance(
        command: CreateOpeningReceivableCommand
    ): RepositoryResult<Receivable> {
        val request = CreateStandaloneReceivableRequestDto(
            customerId = command.customerId,
            amount = command.amount,
            debtDate = command.debtDate,
            dueDate = command.dueDate,
            legacyInvoiceNumber = command.legacyInvoiceNumber?.takeIf(String::isNotBlank),
            notes = command.notes?.takeIf(String::isNotBlank)
        )
        return safeApiCall { receivableApi.createOpeningBalance(request) }
            .toRepositoryResult { response ->
                val receivable = response.data
                if (!response.success || receivable == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_OPENING_RECEIVABLE_FAILED",
                        message = response.message ?: response.error ?: "Saldo awal piutang gagal dicatat."
                    )
                } else RepositoryResult.Success(receivable.toReceivable())
            }
    }

    override suspend fun createAdjustment(
        command: CreateReceivableAdjustmentCommand
    ): RepositoryResult<Receivable> {
        val request = CreateStandaloneReceivableRequestDto(
            customerId = command.customerId,
            amount = command.amount,
            debtDate = command.debtDate,
            dueDate = command.dueDate,
            legacyInvoiceNumber = command.reference.trim(),
            source = "ADJUSTMENT",
            notes = command.reason.trim()
        )
        return safeApiCall { receivableApi.createAdjustment(request) }
            .toRepositoryResult { response ->
                val receivable = response.data
                if (!response.success || receivable == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_RECEIVABLE_ADJUSTMENT_FAILED",
                        message = response.message ?: response.error ?: "Adjustment piutang gagal dicatat."
                    )
                } else RepositoryResult.Success(receivable.toReceivable())
            }
    }

    override suspend fun getCustomerSummaries(
        page: Int,
        limit: Int,
        dueFilter: String?
    ): RepositoryResult<CustomerReceivableSummaryPage> {
        return safeApiCall {
            receivableApi.getCustomerReceivableSummaries(page, limit, dueFilter?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val summaryPage = response.data
            if (!response.success || summaryPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "RECEIVABLE_SUMMARY_FAILED",
                    message = response.message ?: response.error ?: "Ringkasan piutang gagal dimuat."
                )
            } else RepositoryResult.Success(summaryPage.toCustomerReceivableSummaryPage())
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
            notes = command.notes?.takeIf(String::isNotBlank),
            idempotencyKey = command.idempotencyKey
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
        receivableId: String?,
        customerId: String?,
        method: String?,
        userId: String?,
        customerSearch: String?,
        receiverSearch: String?,
        status: String?,
        dateFrom: String?,
        dateTo: String?
    ): RepositoryResult<ReceivablePaymentHistoryPage> {
        return safeApiCall {
            receivableApi.getReceivablePayments(
                page = page,
                limit = limit,
                receivableId = receivableId?.takeIf(String::isNotBlank),
                customerId = customerId?.takeIf(String::isNotBlank),
                method = method?.takeIf(String::isNotBlank),
                userId = userId?.takeIf(String::isNotBlank),
                customerSearch = customerSearch?.takeIf(String::isNotBlank),
                receiverSearch = receiverSearch?.takeIf(String::isNotBlank),
                status = status?.takeIf(String::isNotBlank),
                dateFrom = dateFrom?.takeIf(String::isNotBlank),
                dateTo = dateTo?.takeIf(String::isNotBlank)
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

    override suspend fun getReceivablePaymentReceipt(
        paymentId: String
    ): RepositoryResult<ReceivablePaymentHistory> {
        return safeApiCall { receivableApi.getReceivablePaymentReceipt(paymentId) }
            .toRepositoryResult { response ->
                val receipt = response.data
                if (!response.success || receipt == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "PAYMENT_RECEIPT_FAILED",
                        message = response.message ?: response.error ?: "Bukti pembayaran gagal dimuat."
                    )
                } else RepositoryResult.Success(receipt.toPaymentHistory())
            }
    }

    override suspend fun reverseReceivablePayment(
        command: ReverseReceivablePaymentCommand
    ): RepositoryResult<ReceivablePaymentReceipt> {
        return safeApiCall {
            receivableApi.reverseReceivablePayment(
                command.paymentId,
                ReverseReceivablePaymentRequestDto(command.idempotencyKey, command.reason)
            )
        }.toRepositoryResult { response ->
            val reversal = response.data
            if (!response.success || reversal == null) {
                RepositoryResult.Error(
                    code = response.code ?: "PAYMENT_REVERSAL_FAILED",
                    message = response.message ?: response.error ?: "Reversal pembayaran gagal dicatat."
                )
            } else RepositoryResult.Success(reversal.toReceipt())
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
        source = source,
        amount = amount,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        debtDate = debtDate,
        dueDate = dueDate,
        status = status,
        legacyInvoiceNumber = legacyInvoiceNumber,
        notes = notes,
        createdBy = createdBy,
        isActive = isActive,
        createdAt = createdAt
    )
}

private fun ReceivablePaymentResponseDto.toReceipt(): ReceivablePaymentReceipt {
    return ReceivablePaymentReceipt(
        id = id,
        paymentNumber = paymentNumber,
        receivableId = receivableId,
        customerId = customerId,
        customerName = customerName,
        amount = amount,
        method = method,
        reference = reference,
        notes = notes,
        paidAt = paidAt,
        paymentDate = paymentDate,
        entryType = entryType,
        reversedPaymentId = reversedPaymentId,
        receivedBy = receivedBy,
        receivedByName = receivedByName,
        balanceBefore = balanceBefore,
        balanceAfter = balanceAfter,
        receivableStatus = receivableStatus,
        receivableRemainingAmount = receivableRemainingAmount,
        idempotentReplay = idempotentReplay
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
        paymentNumber = paymentNumber,
        receivableId = receivableId,
        customerId = customerId,
        customerName = customerName,
        transactionId = transactionId,
        source = source,
        amount = amount,
        method = method,
        reference = reference,
        notes = notes,
        paidAt = paidAt,
        paymentDate = paymentDate,
        entryType = entryType,
        reversedPaymentId = reversedPaymentId,
        receivedBy = receivedBy,
        receivedByName = receivedByName,
        balanceBefore = balanceBefore,
        balanceAfter = balanceAfter,
        isReversed = isReversed,
        receivableStatus = receivableStatus,
        receivableRemainingAmount = receivableRemainingAmount
    )
}

private fun PaginatedResponse<CustomerReceivableSummaryResponseDto>.toCustomerReceivableSummaryPage() =
    CustomerReceivableSummaryPage(
        data = data.map { row ->
            CustomerReceivableSummary(
                customerId = row.customerId,
                customerName = row.customerName,
                totalAmount = row.totalAmount,
                totalPaid = row.totalPaid,
                totalRemaining = row.totalRemaining,
                unpaidCount = row.unpaidCount,
                overdueCount = row.overdueCount,
                nearestDueDate = row.nearestDueDate
            )
        },
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
