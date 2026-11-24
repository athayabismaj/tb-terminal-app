package com.tbterminal.app.data.remote

import java.io.IOException
import java.net.SocketTimeoutException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.Headers
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
        assertEquals("Data berubah atau sedang diproses. Muat ulang lalu coba lagi.", result.message)
    }

    @Test
    fun returnsFriendlyMessageForForbiddenResponse() = runBlocking {
        val result = safeApiCall<String> {
            Response.error(
                403,
                """
                {
                  "code": "FORBIDDEN",
                  "message": "internal permission detail"
                }
                """.trimIndent().toResponseBody("application/json".toMediaType())
            )
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("FORBIDDEN", result.code)
        assertEquals("Anda tidak memiliki akses ke fitur ini.", result.message)
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
        assertEquals("Koneksi bermasalah. Periksa jaringan dan status server.", result.message)
    }

    @Test
    fun returnsStableErrorForTimeout() = runBlocking {
        val result = safeApiCall<String> {
            throw SocketTimeoutException("timed out")
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("NETWORK_TIMEOUT", result.code)
        assertEquals("Request terlalu lama. Periksa koneksi lalu coba lagi.", result.message)
    }

    @Test
    fun returnsStableErrorForInvalidSuccessPayload() = runBlocking {
        val result = safeApiCall<String> {
            throw SerializationException("malformed JSON")
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("INVALID_RESPONSE", result.code)
        assertEquals("Respons server tidak dapat diproses. Silakan coba lagi.", result.message)
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
        assertEquals("Server sedang mengalami gangguan. Silakan coba beberapa saat lagi.", result.message)
    }

    @Test
    fun rejectsSuccessfulResponseWithoutBody() = runBlocking {
        val result = safeApiCall<String> {
            Response.success(null)
        }

        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals("EMPTY_BODY", result.code)
        assertEquals("Respons server tidak dapat diproses. Silakan coba lagi.", result.message)
    }

    @Test
    fun propagatesRetryAfterHeaderOn429TooManyAttemptsAndApproverLocked() = runBlocking {
        val rawTooMany = Response.error<String>(
            429,
            """{"success":false,"code":"TOO_MANY_ATTEMPTS","error":"locked"}"""
                .toResponseBody("application/json".toMediaType())
        ).raw().newBuilder()
            .headers(Headers.headersOf("Retry-After", "90"))
            .build()
        val tooManyResult = safeApiCall {
            Response.error<String>(
                """{"success":false,"code":"TOO_MANY_ATTEMPTS","error":"locked"}"""
                    .toResponseBody("application/json".toMediaType()),
                rawTooMany
            )
        } as NetworkResult.Error

        assertEquals("TOO_MANY_ATTEMPTS", tooManyResult.code)
        assertEquals(90L, tooManyResult.retryAfterSeconds)
        assertEquals("Terlalu banyak percobaan gagal. Coba lagi dalam 1 menit 30 detik.", tooManyResult.message)

        val rawApprover = Response.error<String>(
            429,
            """{"success":false,"code":"MANAGER_APPROVER_LOCKED","message":"approver locked"}"""
                .toResponseBody("application/json".toMediaType())
        ).raw().newBuilder()
            .headers(Headers.headersOf("Retry-After", "300"))
            .build()
        val approverResult = safeApiCall {
            Response.error<String>(
                """{"success":false,"code":"MANAGER_APPROVER_LOCKED","message":"approver locked"}"""
                    .toResponseBody("application/json".toMediaType()),
                rawApprover
            )
        } as NetworkResult.Error

        assertEquals("MANAGER_APPROVER_LOCKED", approverResult.code)
        assertEquals(300L, approverResult.retryAfterSeconds)
        assertTrue(approverResult.message.contains("5 menit"))
    }

    @Test
    fun parsesRetryAfterNumericAndHttpDateHeaders() {
        val fixedNowMillis = 1_795_000_000_000L
        assertEquals(45L, parseRetryAfterSeconds("45", clockMillis = { fixedNowMillis }))
        val futureDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(fixedNowMillis + 120_000L), ZoneOffset.UTC)
        )
        assertEquals(120L, parseRetryAfterSeconds(futureDate, clockMillis = { fixedNowMillis }))
    }
}
