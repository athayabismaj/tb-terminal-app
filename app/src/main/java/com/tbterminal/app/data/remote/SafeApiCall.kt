package com.tbterminal.app.data.remote

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response

suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = apiCall()

        if (!response.isSuccessful) {
            val apiError = response.toApiErrorResponse()
            return NetworkResult.Error(
                code = apiError.code,
                message = apiError.message
            )
        }

        val body = response.body()
        if (body != null) {
            NetworkResult.Success(body)
        } else {
            NetworkResult.Error(
                code = "EMPTY_BODY",
                message = "Response body is empty."
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (io: IOException) {
        NetworkResult.Exception(io)
    } catch (exception: Exception) {
        NetworkResult.Exception(exception)
    }
}

private fun <T> Response<T>.toApiErrorResponse(): ApiErrorResponse {
    val rawError = errorBody()?.string().orEmpty()
    if (rawError.isBlank()) {
        return ApiErrorResponse(
            code = "HTTP_${code()}",
            message = "Server menolak request tanpa detail error."
        )
    }

    return try {
        Json.decodeFromString<ApiErrorResponse>(rawError)
    } catch (serialization: SerializationException) {
        ApiErrorResponse(
            code = "HTTP_${code()}",
            message = "Format error server tidak sesuai kontrak."
        )
    }
}
