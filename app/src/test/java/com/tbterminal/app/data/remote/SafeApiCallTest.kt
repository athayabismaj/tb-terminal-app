package com.tbterminal.app.data.remote

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    fun returnsExceptionForIoFailure() = runBlocking {
        val failure = IOException("offline")

        val result = safeApiCall<String> {
            throw failure
        }

        assertTrue(result is NetworkResult.Exception)
        result as NetworkResult.Exception
        assertSame(failure, result.e)
    }
}
