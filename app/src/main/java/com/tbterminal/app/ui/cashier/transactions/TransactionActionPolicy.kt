package com.tbterminal.app.ui.cashier.transactions

import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability

internal data class TransactionActionAccess(
    val canVoid: Boolean,
    val canRefund: Boolean,
    val requiresManagerApproval: Boolean,
)

internal fun transactionActionAccess(
    role: String,
    transactionStatus: String,
    transactionType: String,
): TransactionActionAccess {
    val normalizedStatus = transactionStatus.trim().uppercase()
    val directCorrection = AppAccessPolicy.can(role, AppCapability.DIRECT_TRANSACTION_CORRECTION)
    val approvedCorrection = AppAccessPolicy.can(role, AppCapability.APPROVED_TRANSACTION_CORRECTION)
    val supportedRole = directCorrection || approvedCorrection
    val eligibleSale = transactionType.equals("PENJUALAN", ignoreCase = true)
    val terminalStatus = normalizedStatus in setOf("VOIDED", "REFUNDED")
    val eligible = supportedRole && eligibleSale && !terminalStatus
    return TransactionActionAccess(
        canVoid = eligible,
        canRefund = eligible,
        requiresManagerApproval = eligible && !directCorrection && approvedCorrection,
    )
}

internal fun validateActionApproval(
    expectedAction: ManagerApprovalAction,
    transaction: CashTransactionDetail,
    grant: ManagerApprovalGrant,
): String? = when {
    grant.action != expectedAction -> "Persetujuan manager tidak sesuai dengan tindakan yang dipilih."
    grant.resourceId != transaction.id -> "Persetujuan manager tidak sesuai dengan transaksi ini."
    else -> null
}

internal fun transactionActionErrorMessage(code: String, fallback: String): String = when (code) {
    "MANAGER_APPROVAL_REQUIRED" -> "Tindakan ini memerlukan persetujuan owner atau admin."
    "MANAGER_APPROVAL_EXPIRED" -> "Persetujuan telah kedaluwarsa. Minta persetujuan baru."
    "MANAGER_APPROVAL_ALREADY_USED" -> "Persetujuan sudah digunakan. Minta persetujuan baru."
    "MANAGER_APPROVAL_SCOPE_MISMATCH" -> "Persetujuan tidak cocok dengan transaksi atau tindakan ini."
    "TRANSACTION_ALREADY_VOIDED" -> "Transaksi sudah pernah dibatalkan."
    "TRANSACTION_ALREADY_REFUNDED" -> "Transaksi sudah pernah direfund."
    "TRANSACTION_NOT_VOIDABLE" -> "Status transaksi ini tidak dapat dibatalkan."
    "TRANSACTION_NOT_REFUNDABLE" -> "Status transaksi ini tidak dapat direfund."
    else -> fallback
}

internal fun requiresNewActionApproval(code: String): Boolean = code in setOf(
    "MANAGER_APPROVAL_REQUIRED",
    "MANAGER_APPROVAL_EXPIRED",
    "MANAGER_APPROVAL_ALREADY_USED",
    "MANAGER_APPROVAL_SCOPE_MISMATCH",
)

internal fun isAmbiguousTransactionActionError(code: String): Boolean =
    code.startsWith("HTTP_5") || code in setOf(
        "NETWORK_TIMEOUT",
        "NETWORK_UNAVAILABLE",
        "CONNECTION_FAILED",
        "CONNECT_TIMEOUT",
        "CONNECTION_INTERRUPTED",
        "INVALID_RESPONSE",
        "EMPTY_BODY",
    )

internal fun isTransactionAlreadyRefundedError(code: String, message: String): Boolean {
    val normalizedCode = code.trim().uppercase()
    if (normalizedCode == "TRANSACTION_ALREADY_REFUNDED" || normalizedCode == "DUPLICATE_DATA") {
        return true
    }
    val lowerMessage = message.trim().lowercase()
    return normalizedCode in setOf("VALIDATION_ERROR", "HTTP_400", "HTTP_409") &&
        (lowerMessage.contains("sudah pernah direfund") || lowerMessage.contains("sudah direfund"))
}

internal fun isTransactionAlreadyVoidedError(code: String, message: String): Boolean {
    val normalizedCode = code.trim().uppercase()
    if (normalizedCode == "TRANSACTION_ALREADY_VOIDED" || normalizedCode == "DUPLICATE_DATA") {
        return true
    }
    val lowerMessage = message.trim().lowercase()
    return normalizedCode in setOf("VALIDATION_ERROR", "HTTP_400", "HTTP_409") &&
        (lowerMessage.contains("sudah pernah dibatalkan") ||
            lowerMessage.contains("sudah dibatalkan") ||
            lowerMessage.contains("sudah di-void") ||
            lowerMessage.contains("sudah divoid"))
}

internal fun isTransactionActionIdempotencyConflictError(code: String, message: String): Boolean {
    val normalizedCode = code.trim().uppercase()
    if (normalizedCode !in setOf("VALIDATION_ERROR", "HTTP_400", "IDEMPOTENCY_CONFLICT")) {
        return false
    }
    val lowerMessage = message.trim().lowercase()
    return lowerMessage.contains("idempotencykey") || lowerMessage.contains("idempotency key")
}
