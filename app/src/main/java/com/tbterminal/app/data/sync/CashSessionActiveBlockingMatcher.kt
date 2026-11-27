package com.tbterminal.app.data.sync

/**
 * Centralized matcher for backend errors indicating that an existing open cash session on the
 * server blocks syncing a newer offline-opened session for the same cashier.
 */
object CashSessionActiveBlockingMatcher {
    const val ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN = "ACTIVE_SESSION_BLOCKING_OPEN"

    val BACKEND_ERROR_CODES: Set<String> = setOf(
        "ACTIVE_CASH_SESSION_EXISTS",
        "OPEN_CASH_SESSION_EXISTS",
        "CASH_SESSION_ALREADY_OPEN",
        "ACTIVE_SESSION_EXISTS",
    )

    val FALLBACK_MESSAGE_PHRASES: List<String> = listOf(
        "sesi kasir yang belum ditutup",
        "tutup sesi tersebut sebelum sinkronisasi",
        "masih memiliki sesi kasir yang belum ditutup",
        "masih ada sesi kas yang belum ditutup",
        "active cash session",
    )

    fun isActiveSessionBlockingOpen(
        httpCode: Int,
        errorCode: String?,
        errorMessage: String?,
    ): Boolean {
        if (httpCode !in 400..499) return false
        val normalizedCode = errorCode?.trim()?.uppercase()
        if (!normalizedCode.isNullOrBlank() && normalizedCode in BACKEND_ERROR_CODES) {
            return true
        }
        if (httpCode != 400 && httpCode != 409) return false
        val normalizedMessage = errorMessage?.trim()?.lowercase().orEmpty()
        if (normalizedMessage.isBlank()) return false
        return FALLBACK_MESSAGE_PHRASES.any { phrase -> normalizedMessage.contains(phrase) }
    }
}
