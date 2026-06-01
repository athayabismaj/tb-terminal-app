package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionItem
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.remote.CashSessionResponseDto
import com.tbterminal.app.data.remote.CloseSessionRequestDto
import com.tbterminal.app.data.remote.OpenSessionRequestDto
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.SalesApi
import com.tbterminal.app.data.remote.SalesTransactionSummaryDto
import com.tbterminal.app.data.remote.TransactionDetailDto
import com.tbterminal.app.data.remote.TransactionItemDto
import com.tbterminal.app.data.remote.safeApiCall
import java.math.BigDecimal

interface CashReconciliationRepository {
    suspend fun getSessions(page: Int, limit: Int, status: String? = null): RepositoryResult<CashSessionPage>
    suspend fun getActiveSession(): RepositoryResult<CashSession?>
    suspend fun getSessionById(id: String): RepositoryResult<CashSession>
    suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession>
    suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession>
    suspend fun getTransactions(
        page: Int,
        limit: Int,
        sessionId: String?,
        search: String? = null,
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): RepositoryResult<CashTransactionPage>
    suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail>
    suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense>
    suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>>
    suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String? = null): RepositoryResult<CashExpensePage>
    suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String): RepositoryResult<CashTransactionDetail>
}

class RemoteCashReconciliationRepository(
    private val salesApi: SalesApi
) : CashReconciliationRepository {
    override suspend fun getSessions(page: Int, limit: Int, status: String?): RepositoryResult<CashSessionPage> {
        return safeApiCall { salesApi.getSessions(page, limit, status) }
            .toRepositoryResult { response ->
                val sessionPage = response.data
                if (!response.success || sessionPage == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CASH_SESSIONS_FAILED",
                        message = response.message ?: response.error ?: "Riwayat sesi kas gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(sessionPage.toCashSessionPage())
                }
            }
    }

    override suspend fun getActiveSession(): RepositoryResult<CashSession?> {
        return safeApiCall { salesApi.getActiveSession() }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "CASH_SESSION_FAILED",
                        message = response.message ?: response.error ?: "Sesi kas gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(response.data?.toCashSession())
                }
            }
    }

    override suspend fun getSessionById(id: String): RepositoryResult<CashSession> {
        return safeApiCall { salesApi.getSessionById(id) }
            .toRepositoryResult { response ->
                val session = response.data
                if (!response.success || session == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CASH_SESSION_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail sesi kas gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(session.toCashSession())
                }
            }
    }

    override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> {
        return safeApiCall { salesApi.openSession(OpenSessionRequestDto(startingCash)) }
            .toRepositoryResult { response ->
                val session = response.data
                if (!response.success || session == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "OPEN_CASH_SESSION_FAILED",
                        message = response.message ?: response.error ?: "Sesi kas gagal dibuka."
                    )
                } else {
                    RepositoryResult.Success(session.toCashSession())
                }
            }
    }

    override suspend fun closeSession(
        endingCashPhysical: BigDecimal,
        notes: String?
    ): RepositoryResult<CashSession> {
        val request = CloseSessionRequestDto(
            endingCashPhysical = endingCashPhysical,
            notes = notes
        )
        return safeApiCall { salesApi.closeSession(request) }
            .toRepositoryResult { response ->
                val session = response.data
                if (!response.success || session == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CLOSE_CASH_SESSION_FAILED",
                        message = response.message ?: response.error ?: "Sesi kas gagal ditutup."
                    )
                } else {
                    RepositoryResult.Success(session.toCashSession())
                }
            }
    }

    override suspend fun getTransactions(
        page: Int,
        limit: Int,
        sessionId: String?,
        search: String?,
        status: String?,
        startDate: String?,
        endDate: String?
    ): RepositoryResult<CashTransactionPage> {
        return safeApiCall {
            salesApi.getTransactions(
                page = page, 
                limit = limit, 
                sessionId = sessionId,
                search = search,
                status = status,
                startDate = startDate,
                endDate = endDate
            )
        }.toRepositoryResult { response ->
            val transactionPage = response.data
            if (!response.success || transactionPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "CASH_TRANSACTIONS_FAILED",
                    message = response.message ?: response.error ?: "Transaksi kas gagal dimuat."
                )
            } else {
                RepositoryResult.Success(transactionPage.toCashTransactionPage())
            }
        }
    }

    override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> {
        return safeApiCall { salesApi.getTransactionById(id) }
            .toRepositoryResult { response ->
                val transaction = response.data
                if (!response.success || transaction == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "TRANSACTION_DETAIL_FAILED",
                        message = response.message ?: response.error ?: "Detail transaksi gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(transaction.toCashTransactionDetail())
                }
            }
    }

    override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> {
        val request = com.tbterminal.app.data.remote.CashExpenseRequestDto(
            amount = amount,
            description = description
        )
        return safeApiCall { salesApi.addExpense(request) }
            .toRepositoryResult { response ->
                val expense = response.data
                if (!response.success || expense == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "ADD_EXPENSE_FAILED",
                        message = response.message ?: response.error ?: "Gagal mencatat pengeluaran."
                    )
                } else {
                    RepositoryResult.Success(expense.toCashExpense())
                }
            }
    }

    override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> {
        return safeApiCall { salesApi.getExpenses(sessionId) }
            .toRepositoryResult { response ->
                val expenses = response.data ?: emptyList()
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "GET_EXPENSES_FAILED",
                        message = response.message ?: response.error ?: "Gagal memuat pengeluaran."
                    )
                } else {
                    RepositoryResult.Success(expenses.map { it.toCashExpense() })
                }
            }
    }

    override suspend fun getExpenseHistory(
        page: Int,
        limit: Int,
        sessionId: String?
    ): RepositoryResult<CashExpensePage> {
        return safeApiCall { salesApi.getExpenseHistory(page, limit, sessionId) }
            .toRepositoryResult { response ->
                val expenses = response.data
                if (!response.success || expenses == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "GET_EXPENSE_HISTORY_FAILED",
                        message = response.message ?: response.error ?: "Gagal memuat riwayat pengeluaran."
                    )
                } else {
                    RepositoryResult.Success(expenses.toCashExpensePage())
                }
            }
    }

    override suspend fun payTransactionDebt(
        transactionId: String,
        amount: BigDecimal,
        method: String
    ): RepositoryResult<CashTransactionDetail> {
        val request = com.tbterminal.app.data.remote.PayDebtRequestDto(
            amount = amount,
            method = method
        )
        return safeApiCall { salesApi.payTransactionDebt(transactionId, request) }
            .toRepositoryResult { response ->
                val transaction = response.data
                if (!response.success || transaction == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "PAY_DEBT_FAILED",
                        message = response.message ?: response.error ?: "Gagal mencatat pelunasan."
                    )
                } else {
                    RepositoryResult.Success(transaction.toCashTransactionDetail())
                }
            }
    }
}

private fun com.tbterminal.app.data.remote.CashExpenseResponseDto.toCashExpense(): CashExpense {
    return CashExpense(
        id = id,
        sessionId = sessionId,
        userId = userId,
        userName = userName,
        amount = amount,
        description = description,
        createdAt = createdAt
    )
}

private fun PaginatedResponse<com.tbterminal.app.data.remote.CashExpenseResponseDto>.toCashExpensePage(): CashExpensePage {
    return CashExpensePage(
        data = data.map { it.toCashExpense() },
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun CashSessionResponseDto.toCashSession(): CashSession {
    return CashSession(
        id = id,
        userId = userId,
        userName = userName,
        openedAt = openedAt,
        closedAt = closedAt,
        openingCash = openingCash,
        closingCash = closingCash,
        systemCash = systemCash,
        difference = difference,
        totalExpenses = totalExpenses,
        notes = notes,
        status = status
    )
}

private fun PaginatedResponse<CashSessionResponseDto>.toCashSessionPage(): CashSessionPage {
    return CashSessionPage(
        data = data.map { it.toCashSession() },
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun SalesTransactionSummaryDto.toCashTransaction(): CashTransaction {
    return CashTransaction(
        id = id,
        receiptId = receiptId.orGeneratedReceiptId(id),
        sessionId = sessionId,
        customerId = customerId,
        customerName = customerName,
        type = type,
        status = status,
        total = total,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        createdAt = createdAt
    )
}

private fun TransactionDetailDto.toCashTransactionDetail(): CashTransactionDetail {
    return CashTransactionDetail(
        id = id,
        receiptId = receiptId.orGeneratedReceiptId(id),
        sessionId = sessionId,
        customerId = customerId,
        customerName = customerName,
        type = type,
        status = status,
        total = total,
        paidAmount = paidAmount,
        createdAt = createdAt,
        items = items.map { it.toCashTransactionItem() }
    )
}

private fun String?.orGeneratedReceiptId(transactionId: String): String {
    return this?.takeIf(String::isNotBlank)
        ?: "TRX-${transactionId.take(8).uppercase()}"
}

private fun TransactionItemDto.toCashTransactionItem(): CashTransactionItem {
    return CashTransactionItem(
        productId = productId,
        productName = productName,
        unitId = unitId,
        quantity = quantity,
        priceAtTransaction = priceAtTransaction,
        subtotal = subtotal
    )
}

private fun PaginatedResponse<SalesTransactionSummaryDto>.toCashTransactionPage(): CashTransactionPage {
    return CashTransactionPage(
        data = data.map { it.toCashTransaction() },
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}
