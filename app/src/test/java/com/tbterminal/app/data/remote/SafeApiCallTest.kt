package com.tbterminal.app.data.remote

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SafeApiCallTest {
    @Test
    fun returnsBackendErrorContract() = runBlocking {
        val result = safeApiCall<String> {
            Response.error(
                409,
                """
                {
                  "code": "PAYMENT_ALREADY_FINALIZED",
                  "message": "Payment cannot be changed."
                }
                """.trimIndent().toResponseBody("application/json".toMediaType())
            )
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("PAYMENT_ALREADY_FINALIZED", result.code)
        assertEquals("Payment cannot be changed.", result.message)
    }

    @Test
    fun returnsStableErrorForIoFailure() = runBlocking {
        val failure = IOException("offline")

        val result = safeApiCall<String> {
            throw failure
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("CONNECTION_INTERRUPTED", result.code)
        assertEquals("Koneksi ke server terputus.", result.message)
    }

    @Test
    fun returnsStableErrorForTimeout() = runBlocking {
        val result = safeApiCall<String> {
            throw SocketTimeoutException("timed out")
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("NETWORK_TIMEOUT", result.code)
        assertEquals("Server terlalu lama merespons. Silakan coba lagi.", result.message)
    }

    @Test
    fun returnsStableErrorForInvalidSuccessPayload() = runBlocking {
        val result = safeApiCall<String> {
            throw SerializationException("malformed JSON")
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("INVALID_RESPONSE", result.code)
        assertEquals("Response server tidak sesuai format yang diharapkan.", result.message)
    }

    @Test
    fun returnsStableErrorForMalformedErrorPayload() = runBlocking {
        val result = safeApiCall<String> {
            Response.error(
                502,
                "not-json".toResponseBody("text/plain".toMediaType())
            )
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("HTTP_502", result.code)
        assertEquals("Format error server tidak sesuai kontrak.", result.message)
    }

    @Test
    fun rejectsSuccessfulResponseWithoutBody() = runBlocking {
        val result = safeApiCall<String> {
            Response.success(null)
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("EMPTY_BODY", result.code)
    }
}
