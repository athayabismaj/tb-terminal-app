package com.tbterminal.app.data.remote

import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.TokenStore
import java.net.HttpURLConnection
import okhttp3.Interceptor
import okhttp3.Response

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

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            sessionManager.handleUnauthorized()
        }

        return response
    }
}
