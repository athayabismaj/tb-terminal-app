package com.tbterminal.app.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

internal data class ParsedSyncError(
    val code: String?,
    val message: String,
)

internal object SyncErrorResponseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseHttpError(
        response: Response<*>,
        defaultMessage: String,
    ): ParsedSyncError {
        val rawBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
        val jsonObj = rawBody.takeIf(String::isNotBlank)?.let { raw ->
            runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
        }
        val code = jsonObj?.get("code")?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        val parsedMessage = jsonObj?.get("message")?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: jsonObj?.get("error")?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        val message = when {
            parsedMessage != null -> parsedMessage
            response.code() == 401 -> "Sesi login tidak valid. Login ulang sebelum sinkronisasi."
            response.code() == 403 -> "Peran akun tidak diizinkan untuk melakukan sinkronisasi ini."
            jsonObj == null && rawBody.isNotBlank() && !rawBody.trimStart().startsWith("{") -> rawBody.trim()
            else -> defaultMessage
        }
        return ParsedSyncError(code = code, message = message)
    }
}
