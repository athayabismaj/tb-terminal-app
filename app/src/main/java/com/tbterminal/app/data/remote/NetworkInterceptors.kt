package com.tbterminal.app.data.remote

import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.TokenStore
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.UnknownHostException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private val credentialCheckPaths = setOf(
    "/api/auth/unlock",
    "/api/auth/reset-pin",
    "/api/system/users/me/pin",
    "/api/system/users/me/password"
)

private val nonRevokingUnlockCodes = setOf(
    "INVALID_PIN",
    "WRONG_PIN",
    "PIN_MISMATCH",
    "UNLOCK_FAILED",
    "PIN_LOCKED"
)

private val nonRevokingCredentialCodes = setOf(
    "INVALID_PASSWORD",
    "WRONG_PASSWORD",
    "INVALID_PIN",
    "WRONG_PIN",
    "INVALID_CREDENTIALS"
)

private val interceptorErrorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private data class CredentialErrorBody(
    val code: String?,
    val message: String,
    val forceLogout: Boolean,
    val rawSnippet: String
)

private fun peekCredentialErrorBody(response: Response): CredentialErrorBody? {
    val path = response.request.url.encodedPath
    if (credentialCheckPaths.none { path.endsWith(it) }) {
        return null
    }
    val rawSnippet = runCatching { response.peekBody(4096).string() }.getOrDefault("")
    val root = rawSnippet.takeIf(String::isNotBlank)?.let { raw ->
        runCatching { interceptorErrorJson.parseToJsonElement(raw).jsonObject }.getOrNull()
    }
    val dataObj = runCatching { root?.get("data")?.jsonObject }.getOrNull()
    val code = root?.get("code")?.jsonPrimitive?.contentOrNull?.trim()?.uppercase()
    val message = (
        root?.get("message")?.jsonPrimitive?.contentOrNull
            ?: root?.get("error")?.jsonPrimitive?.contentOrNull
            ?: ""
        ).trim()
    val forceLogout = dataObj?.get("force_logout")?.jsonPrimitive?.booleanOrNull == true ||
        dataObj?.get("forceLogout")?.jsonPrimitive?.booleanOrNull == true
    return CredentialErrorBody(
        code = code,
        message = message,
        forceLogout = forceLogout,
        rawSnippet = rawSnippet
    )
}

internal fun isCredentialMaxAttemptsRevocation(response: Response): Boolean {
    if (response.code != HttpURLConnection.HTTP_UNAUTHORIZED) {
        return false
    }
    val info = peekCredentialErrorBody(response) ?: return false
    val lowerMsg = info.message.lowercase()
    val lowerRaw = info.rawSnippet.lowercase()
    return info.forceLogout ||
        info.code == "PIN_MAX_ATTEMPTS_EXCEEDED" ||
        lowerMsg.contains("batas percobaan") ||
        lowerRaw.contains("pin_max_attempts_exceeded") ||
        lowerRaw.contains("batas percobaan terlampaui")
}

internal fun isNonSessionRevokingCredentialError(response: Response): Boolean {
    if (isCredentialMaxAttemptsRevocation(response)) {
        return false
    }
    val path = response.request.url.encodedPath
    val info = peekCredentialErrorBody(response) ?: return false
    val allowedCodes = if (path.endsWith("/api/auth/unlock")) {
        nonRevokingUnlockCodes
    } else {
        nonRevokingCredentialCodes
    }
    if (info.code != null && info.code in allowedCodes) {
        return true
    }
    return allowedCodes.any { code -> info.rawSnippet.contains("\"$code\"", ignoreCase = true) }
}

class AuthInterceptor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenStore.readAccessToken()

        if (accessToken.isNullOrBlank()) {
            return chain.proceed(request)
        }

        val authorizedRequest = request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authorizedRequest)
    }
}

class UnauthorizedInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED && response.request.header("Authorization") != null) {
            when {
                isCredentialMaxAttemptsRevocation(response) ->
                    sessionManager.handleUnauthorized("Batas percobaan terlampaui. Silakan login kembali.")
                !isNonSessionRevokingCredentialError(response) ->
                    sessionManager.handleUnauthorized()
            }
        }

        return response
    }
}

class RefreshTokenAuthenticator(
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager,
    private val refreshApi: TokenRefreshApi
) : Authenticator {
    private val ambiguousConsumedTokens = LinkedHashSet<String>()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null
        if (isNonSessionRevokingCredentialError(response)) return null
        if (isCredentialMaxAttemptsRevocation(response)) {
            sessionManager.handleUnauthorized("Batas percobaan terlampaui. Silakan login kembali.")
            return null
        }

        val sentToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf(String::isNotBlank)
            ?: return null

        return synchronized(SHARED_REFRESH_LOCK) {
            val latestToken = tokenStore.readAccessToken()
            if (!latestToken.isNullOrBlank() && latestToken != sentToken) {
                return@synchronized response.request.withBearerToken(latestToken)
            }

            val refreshToken = tokenStore.readRefreshToken()
            if (refreshToken.isNullOrBlank() || ambiguousConsumedTokens.contains(refreshToken)) {
                sessionManager.handleUnauthorized()
                return@synchronized null
            }

            val refreshResponse = try {
                refreshApi.refresh(RefreshTokenRequest(refreshToken)).execute()
            } catch (io: IOException) {
                if (!io.isPreConnectFailure()) {
                    recordAmbiguousToken(refreshToken)
                    sessionManager.handleUnauthorized()
                }
                return@synchronized null
            } catch (_: Exception) {
                return@synchronized null
            }
            val refreshed = refreshResponse.body()?.data

            if (!refreshResponse.isSuccessful || refreshed == null) {
                if (refreshResponse.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    sessionManager.handleUnauthorized()
                }
                return@synchronized null
            }

            ambiguousConsumedTokens.remove(refreshToken)
            sessionManager.updateTokens(refreshed.token, refreshed.refreshToken)
            response.request.withBearerToken(refreshed.token)
        }
    }

    private fun recordAmbiguousToken(token: String) {
        if (ambiguousConsumedTokens.size >= MAX_TRACKED_AMBIGUOUS_TOKENS) {
            ambiguousConsumedTokens.iterator().next().let(ambiguousConsumedTokens::remove)
        }
        ambiguousConsumedTokens.add(token)
    }

    private fun Throwable.isPreConnectFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (
                current is UnknownHostException ||
                current is ConnectException ||
                current is NoRouteToHostException ||
                current is PortUnreachableException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun Request.withBearerToken(token: String): Request {
        return newBuilder().header("Authorization", "Bearer $token").build()
    }

    private companion object {
        val SHARED_REFRESH_LOCK = Any()
        const val MAX_AUTH_ATTEMPTS = 2
        const val MAX_TRACKED_AMBIGUOUS_TOKENS = 32
    }
}
