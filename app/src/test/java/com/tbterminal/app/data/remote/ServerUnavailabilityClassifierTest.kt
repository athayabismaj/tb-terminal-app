package com.tbterminal.app.data.remote

import com.tbterminal.app.data.repository.RepositoryResult
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUnavailabilityClassifierTest {
    @Test
    fun transportAndGateway502To504AreClassifiedAsServerUnavailable() {
        val unavailableCodes = listOf(
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
            " connection_failed ",
        )

        unavailableCodes.forEach { code ->
            val result = RepositoryResult.Error(code = code, message = "error")
            assertTrue("Expected $code to be server-unavailable", ServerUnavailabilityClassifier.isServerUnavailable(result))
        }

        assertTrue(ServerUnavailabilityClassifier.isServerUnavailable(RepositoryResult.Exception(IOException("boom"))))
        assertTrue(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(502))
        assertTrue(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(503))
        assertTrue(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(504))
    }

    @Test
    fun http500And4xxAndSuccessAreNeverClassifiedAsServerUnavailable() {
        val serverAliveCodes = listOf(
            "HTTP_500",
            "INTERNAL_SERVER_ERROR",
            "HTTP_400",
            "HTTP_401",
            "HTTP_403",
            "HTTP_404",
            "HTTP_409",
            "HTTP_422",
            "HTTP_423",
            "HTTP_429",
            "CREDIT_LIMIT_EXCEEDED",
            "EMPTY_BODY",
            "INVALID_RESPONSE",
            "UNAUTHORIZED",
            "FORBIDDEN",
        )

        serverAliveCodes.forEach { code ->
            val result = RepositoryResult.Error(code = code, message = "error")
            assertFalse("Expected $code NOT to be server-unavailable", ServerUnavailabilityClassifier.isServerUnavailable(result))
        }

        assertFalse(ServerUnavailabilityClassifier.isServerUnavailable(RepositoryResult.Success(Unit)))
        assertFalse(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(500))
        assertFalse(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(401))
        assertFalse(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(403))
        assertFalse(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(422))
        assertFalse(ServerUnavailabilityClassifier.isServerUnavailableHttpStatus(423))
    }
}
