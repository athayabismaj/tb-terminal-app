package com.tbterminal.app.data.remote

import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.TokenStore
import java.net.HttpURLConnection
import okhttp3.Interceptor
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

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

        if (
            response.code == HttpURLConnection.HTTP_UNAUTHORIZED &&
            response.request.header("Authorization") != null
        ) {
            sessionManager.handleUnauthorized()
        }

        return response
    }
}

class RefreshTokenAuthenticator(
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager,
    private val refreshApi: TokenRefreshApi
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null
        val sentToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf(String::isNotBlank)
            ?: return null

        return synchronized(refreshLock) {
            val latestToken = tokenStore.readAccessToken()
            if (!latestToken.isNullOrBlank() && latestToken != sentToken) {
                return@synchronized response.request.withBearerToken(latestToken)
            }

            val refreshToken = tokenStore.readRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.handleUnauthorized()
                return@synchronized null
            }

            val refreshResponse = runCatching {
                refreshApi.refresh(RefreshTokenRequest(refreshToken)).execute()
            }.getOrNull() ?: return@synchronized null
            val refreshed = refreshResponse.body()?.data

            if (!refreshResponse.isSuccessful || refreshed == null) {
                if (refreshResponse.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    sessionManager.handleUnauthorized()
                }
                return@synchronized null
            }

            sessionManager.updateTokens(refreshed.token, refreshed.refreshToken)
            response.request.withBearerToken(refreshed.token)
        }
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
        const val MAX_AUTH_ATTEMPTS = 2
    }
}
