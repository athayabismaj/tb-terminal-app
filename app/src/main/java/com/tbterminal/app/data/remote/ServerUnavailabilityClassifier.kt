package com.tbterminal.app.data.remote

import com.tbterminal.app.data.repository.RepositoryResult
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.Collections
import java.util.IdentityHashMap
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
        "CONNECT_TIMEOUT",
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

    private val PRE_DISPATCH_UNAVAILABLE_CODES = setOf(
        "CONNECTION_FAILED",
        "CONNECT_TIMEOUT",
        "NETWORK_UNAVAILABLE",
        "HTTP_502",
        "HTTP_503",
        "BAD_GATEWAY",
        "SERVICE_UNAVAILABLE",
    )

    fun isServerUnavailableHttpStatus(httpStatus: Int): Boolean {
        return httpStatus == 502 || httpStatus == 503 || httpStatus == 504
    }

    fun isServerUnavailableCode(code: String?): Boolean {
        val normalized = code?.trim()?.uppercase(Locale.ROOT) ?: return false
        return normalized in TRANSPORT_AND_GATEWAY_CODES
    }

    fun isDefinitelyUnprocessedCode(code: String?): Boolean {
        val normalized = code?.trim()?.uppercase(Locale.ROOT) ?: return false
        return normalized in PRE_DISPATCH_UNAVAILABLE_CODES
    }

    fun isServerUnavailable(result: RepositoryResult<*>): Boolean {
        return when (result) {
            is RepositoryResult.Success -> false
            is RepositoryResult.Exception -> isServerUnavailableThrowable(result.throwable)
            is RepositoryResult.Error -> !isAuthError(result) && isServerUnavailableCode(result.code)
        }
    }

    /**
     * Classifies whether a failure happened before the server could process the mutation.
     * Ambiguous transport failures (timeout, interrupted socket, HTTP 504) return false so
     * non-idempotent mutations (e.g. cash expense / open session) do not fall back locally.
     */
    fun isDefinitelyUnprocessedServerUnavailable(result: RepositoryResult<*>): Boolean {
        return when (result) {
            is RepositoryResult.Success -> false
            is RepositoryResult.Exception -> isDefinitelyUnprocessedThrowable(result.throwable)
            is RepositoryResult.Error -> !isAuthError(result) && isDefinitelyUnprocessedCode(result.code)
        }
    }

    private fun isServerUnavailableThrowable(throwable: Throwable?): Boolean {
        return hasCauseMatching(throwable) { it is IOException }
    }

    private fun isDefinitelyUnprocessedThrowable(throwable: Throwable?): Boolean {
        return hasCauseMatching(throwable) {
            it is PreDispatchIOException ||
                it is ConnectException ||
                it is UnknownHostException ||
                it is NoRouteToHostException
        }
    }

    private fun hasCauseMatching(
        throwable: Throwable?,
        predicate: (Throwable) -> Boolean,
    ): Boolean {
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current = throwable
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH && visited.add(current)) {
            if (predicate(current)) {
                return true
            }
            current = current.cause
            depth++
        }
        return false
    }

    private const val MAX_CAUSE_DEPTH = 16

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
