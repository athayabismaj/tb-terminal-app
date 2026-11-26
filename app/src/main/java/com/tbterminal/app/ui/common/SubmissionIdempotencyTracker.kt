package com.tbterminal.app.ui.common

import java.math.BigDecimal
import java.util.UUID

/**
 * Tracks a single submission intent and its idempotency key so retries after
 * ambiguous outcomes reuse the exact key while modified payloads rotate it.
 */
internal class SubmissionIdempotencyTracker<Intent : Any>(
    private val prefix: String,
    private val keyGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    private var activeIntent: Intent? = null
    private var activeKey: String? = null
    var isOutcomeAmbiguous: Boolean = false
        private set

    fun resolveKey(intent: Intent): String {
        val currentKey = activeKey
        if (activeIntent == intent && currentKey != null) {
            return currentKey
        }
        val freshKey = "$prefix-${keyGenerator()}"
        activeIntent = intent
        activeKey = freshKey
        isOutcomeAmbiguous = false
        return freshKey
    }

    fun onSuccess() {
        clear()
    }

    fun onError(code: String, message: String? = null) {
        if (isIdempotencyKeyConflict(code, message)) {
            clear()
        } else {
            isOutcomeAmbiguous = isAmbiguousSubmissionErrorCode(code)
        }
    }

    fun onException() {
        isOutcomeAmbiguous = true
    }

    fun onDialogDismissed() {
        if (!isOutcomeAmbiguous) {
            clear()
        }
    }

    fun hasPendingAmbiguousIntent(predicate: (Intent) -> Boolean): Boolean {
        return isOutcomeAmbiguous && activeIntent?.let(predicate) == true
    }

    fun clear() {
        activeIntent = null
        activeKey = null
        isOutcomeAmbiguous = false
    }
}

internal fun BigDecimal.toNormalizedIdempotencyNumber(): String =
    stripTrailingZeros().toPlainString()

internal fun isAmbiguousSubmissionErrorCode(code: String): Boolean {
    val normalized = code.trim().uppercase()
    return normalized.startsWith("HTTP_5") || normalized in AMBIGUOUS_SUBMISSION_ERROR_CODES
}

internal fun isIdempotencyKeyConflict(code: String, message: String?): Boolean {
    val normalized = code.trim().uppercase()
    return normalized in CONFLICT_ERROR_CODES &&
        message?.contains("idempotency key", ignoreCase = true) == true
}

private val AMBIGUOUS_SUBMISSION_ERROR_CODES = setOf(
    "NETWORK_TIMEOUT",
    "NETWORK_UNAVAILABLE",
    "CONNECTION_FAILED",
    "CONNECTION_INTERRUPTED",
    "INVALID_RESPONSE",
    "EMPTY_BODY"
)

private val CONFLICT_ERROR_CODES = setOf(
    "VALIDATION_ERROR",
    "HTTP_400",
    "IDEMPOTENCY_CONFLICT"
)
