package com.tbterminal.app.data.remote

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<T>
): NetworkResult<T> = executeSafeCall {
    val response = apiCall()
    if (!response.isSuccessful) {
        val apiError = response.toApiErrorResponse()
        val retryAfterSeconds = parseRetryAfterSeconds(response.headers()["Retry-After"])
        return@executeSafeCall NetworkResult.Error(
            code = apiError.code,
            message = UserFacingErrorMapper.message(
                httpStatus = response.code(),
                code = apiError.code,
                backendMessage = apiError.message,
                retryAfterSeconds = retryAfterSeconds,
            ),
            retryAfterSeconds = retryAfterSeconds,
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
}

suspend fun safeUnitApiCall(
    apiCall: suspend () -> Response<Unit>
): NetworkResult<Unit> = executeSafeCall {
    val response = apiCall()
    if (response.isSuccessful) {
        NetworkResult.Success(Unit)
    } else {
        val apiError = response.toApiErrorResponse()
        val retryAfterSeconds = parseRetryAfterSeconds(response.headers()["Retry-After"])
        NetworkResult.Error(
            code = apiError.code,
            message = UserFacingErrorMapper.message(
                httpStatus = response.code(),
                code = apiError.code,
                backendMessage = apiError.message,
                retryAfterSeconds = retryAfterSeconds,
            ),
            retryAfterSeconds = retryAfterSeconds,
        )
    }
}

private inline fun <T> executeSafeCall(
    block: () -> NetworkResult<T>
): NetworkResult<T> {
    return try {
        block()
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

private val errorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
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
        val decoded = errorJson.decodeFromString<ApiErrorResponse>(rawError)
        ApiErrorResponse(
            code = decoded.code.ifBlank { "HTTP_${code()}" },
            message = decoded.message.ifBlank { decoded.error.orEmpty() },
            error = decoded.error,
            details = decoded.details,
        )
    } catch (_: Exception) {
        val jsonObj = runCatching { errorJson.parseToJsonElement(rawError).jsonObject }.getOrNull()
        if (jsonObj != null) {
            val code = jsonObj["code"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                ?: "HTTP_${code()}"
            val message = jsonObj["message"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                ?: jsonObj["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                ?: "Server menolak request tanpa detail error."
            ApiErrorResponse(code = code, message = message)
        } else {
            ApiErrorResponse(
                code = "HTTP_${code()}",
                message = "Format error server tidak sesuai kontrak."
            )
        }
    }
}

internal fun parseRetryAfterSeconds(
    headerValue: String?,
    clockMillis: () -> Long = { System.currentTimeMillis() },
): Long? {
    val trimmed = headerValue?.trim()?.takeIf(String::isNotBlank) ?: return null
    trimmed.toLongOrNull()?.let { numericSeconds ->
        return if (numericSeconds >= 0L) numericSeconds.coerceAtLeast(1L) else null
    }
    val parsedDate = runCatching {
        ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME)
    }.getOrNull() ?: return null
    val diffMillis = parsedDate.toInstant().toEpochMilli() - clockMillis()
    return ((diffMillis + 999L) / 1000L).coerceAtLeast(1L)
}
