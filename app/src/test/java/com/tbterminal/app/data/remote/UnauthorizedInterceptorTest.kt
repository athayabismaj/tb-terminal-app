package com.tbterminal.app.data.remote

import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertFalse
import org.junit.Test

class UnauthorizedInterceptorTest {
    @Test
    fun authenticated401ClearsSession() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()
        try {
            val tokenStore = InMemoryTokenStore().apply {
                saveTokens("access-token", "refresh-token")
                saveSessionUser(SessionUser("Kasir", "KASIR", true, "2026-09-01", null))
            }
            val sessionManager = SessionManager(tokenStore)
            val client = OkHttpClient.Builder()
                .addInterceptor(UnauthorizedInterceptor(sessionManager))
                .build()
            val request = Request.Builder()
                .url(server.url("/api/protected"))
                .header("Authorization", "Bearer access-token")
                .build()

            client.newCall(request).execute().close()

            assertFalse(sessionManager.hasAccessToken())
        } finally {
            server.shutdown()
        }
    }
}

private class InMemoryTokenStore : TokenStore {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var user: SessionUser? = null

    override fun readAccessToken(): String? = accessToken
    override fun hasStoredAccessToken(): Boolean = !accessToken.isNullOrBlank()
    override fun readRefreshToken(): String? = refreshToken
    override fun saveAccessToken(token: String) { accessToken = token }
    override fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    override fun readSessionUser(): SessionUser? = user
    override fun saveSessionUser(user: SessionUser) { this.user = user }
    override fun clearAccessToken() {
        accessToken = null
        refreshToken = null
        user = null
    }
}
