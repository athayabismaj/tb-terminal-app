package com.tbterminal.app.data.sync

import com.tbterminal.app.data.remote.parseRetryAfterSeconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import retrofit2.Response

internal data class ParsedSyncError(
    val code: String?,
    val message: String,
    val retryAfterSeconds: Long? = null,
)

internal object SyncErrorResponseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseHttpError(
        response: Response<*>,
        defaultMessage: String,
        clockMillis: () -> Long = { System.currentTimeMillis() },
    ): ParsedSyncError {
        val rawBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
        val jsonObj = rawBody.takeIf(String::isNotBlank)?.let { raw ->
            runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
        }
        val nestedErrorObj = jsonObj?.get("error") as? JsonObject
        val code = (jsonObj?.get("code") as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
            ?: (nestedErrorObj?.get("code") as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
        val parsedMessage = (jsonObj?.get("message") as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
            ?: (nestedErrorObj?.get("message") as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
            ?: (jsonObj?.get("error") as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
        val retryAfterSeconds = parseRetryAfterSeconds(response.headers()["Retry-After"], clockMillis)
        val message = when {
            parsedMessage != null -> parsedMessage
            response.code() == 401 -> "Sesi login tidak valid. Login ulang sebelum sinkronisasi."
            response.code() == 403 -> "Peran akun tidak diizinkan untuk melakukan sinkronisasi ini."
            response.code() == 429 -> "Terlalu banyak permintaan sinkronisasi. Akan dicoba ulang otomatis."
            jsonObj == null && rawBody.isNotBlank() && !rawBody.trimStart().startsWith("{") -> rawBody.trim()
            else -> defaultMessage
        }
        return ParsedSyncError(code = code, message = message, retryAfterSeconds = retryAfterSeconds)
    }
}
