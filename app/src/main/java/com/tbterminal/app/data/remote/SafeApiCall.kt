package com.tbterminal.app.data.remote

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
                message = UserFacingErrorMapper.message(
                    httpStatus = response.code(),
                    code = apiError.code,
                    backendMessage = apiError.message,
                )
            )
        }

        val body = response.body()
        if (body != null) {
            NetworkResult.Success(body)
        } else {
            NetworkResult.Error(
                code = "EMPTY_BODY",
                message = UserFacingErrorMapper.message(code = "EMPTY_BODY")
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (timeout: SocketTimeoutException) {
        NetworkResult.Error("NETWORK_TIMEOUT", UserFacingErrorMapper.message(code = "NETWORK_TIMEOUT"))
    } catch (unknownHost: UnknownHostException) {
        NetworkResult.Error("NETWORK_UNAVAILABLE", UserFacingErrorMapper.message(code = "NETWORK_UNAVAILABLE"))
    } catch (connect: ConnectException) {
        NetworkResult.Error("CONNECTION_FAILED", UserFacingErrorMapper.message(code = "CONNECTION_FAILED"))
    } catch (serialization: SerializationException) {
        NetworkResult.Error("INVALID_RESPONSE", UserFacingErrorMapper.message(code = "INVALID_RESPONSE"))
    } catch (io: IOException) {
        NetworkResult.Error("CONNECTION_INTERRUPTED", UserFacingErrorMapper.message(code = "CONNECTION_INTERRUPTED"))
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
