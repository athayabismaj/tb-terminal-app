package com.tbterminal.app.data.remote

import com.tbterminal.app.data.repository.RepositoryResult
import java.util.Locale

/**
 * Centralized classification of whether a failed backend call indicates the server
 * itself is unreachable (transport failure or HTTP 502/503/504 gateway unavailability).
 *
 * HTTP 500 and all 4xx responses mean the server responded and is alive, so callers
 * must NOT switch to offline fallback or mark backend health as UNREACHABLE.
 */
object ServerUnavailabilityClassifier {
    private val TRANSPORT_AND_GATEWAY_CODES = setOf(
        "CONNECTION_FAILED",
        "CONNECTION_INTERRUPTED",
        "NETWORK_UNAVAILABLE",
        "NETWORK_TIMEOUT",
        "HTTP_502",
        "HTTP_503",
        "HTTP_504",
        "BAD_GATEWAY",
        "SERVICE_UNAVAILABLE",
        "GATEWAY_TIMEOUT",
    )

    fun isServerUnavailableHttpStatus(httpStatus: Int): Boolean {
        return httpStatus == 502 || httpStatus == 503 || httpStatus == 504
    }

    fun isServerUnavailableCode(code: String?): Boolean {
        val normalized = code?.trim()?.uppercase(Locale.ROOT) ?: return false
        return normalized in TRANSPORT_AND_GATEWAY_CODES
    }

    fun isServerUnavailable(result: RepositoryResult<*>): Boolean {
        return when (result) {
            is RepositoryResult.Success -> false
            is RepositoryResult.Exception -> true
            is RepositoryResult.Error -> !isAuthError(result) && isServerUnavailableCode(result.code)
        }
    }

    fun isAuthError(error: RepositoryResult.Error): Boolean {
        val normalizedCode = error.code.trim().uppercase(Locale.ROOT)
        val normalizedMessage = error.message.trim().uppercase(Locale.ROOT)
        return normalizedCode == "HTTP_401" ||
            normalizedCode == "HTTP_403" ||
            normalizedCode.contains("UNAUTHORIZED") ||
            normalizedCode.contains("FORBIDDEN") ||
            normalizedMessage.contains("UNAUTHORIZED") ||
            normalizedMessage.contains("FORBIDDEN")
    }
}
