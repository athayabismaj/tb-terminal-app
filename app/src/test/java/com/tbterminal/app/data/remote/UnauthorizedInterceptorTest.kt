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

    @Test
    fun unlock422And423KeepSessionIntact() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"success":false,"code":"INVALID_PIN","data":{"valid":false,"attempts_left":4,"locked_until":null,"force_logout":false}}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(423)
                .setBody("""{"success":false,"code":"PIN_LOCKED","data":{"valid":false,"attempts_left":0,"locked_until":"2026-11-19T10:00:30Z","force_logout":false}}""")
        )
        server.start()
        try {
            val tokenStore = InMemoryTokenStore().apply {
                saveTokens("access-token", "refresh-token")
                saveSessionUser(SessionUser("Kasir", "kasir", true, "2026-09-01", null))
            }
            val sessionManager = SessionManager(tokenStore)
            val client = OkHttpClient.Builder()
                .addInterceptor(UnauthorizedInterceptor(sessionManager))
                .build()

            repeat(2) {
                val request = Request.Builder()
                    .url(server.url("/api/auth/unlock"))
                    .header("Authorization", "Bearer access-token")
                    .build()
                client.newCall(request).execute().close()
            }

            org.junit.Assert.assertTrue(sessionManager.hasAccessToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun unlock401InvalidPinPreservesSessionWhileMaxAttemptsRevokesWithReason() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"success":false,"code":"INVALID_PIN","data":{"valid":false,"attempts_left":2}}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"success":false,"code":"UNAUTHORIZED","message":"Batas percobaan terlampaui. Silakan login kembali."}""")
        )
        server.start()
        try {
            val tokenStore = InMemoryTokenStore().apply {
                saveTokens("access-token", "refresh-token")
                saveSessionUser(SessionUser("Kasir", "kasir", true, "2026-09-01", null))
            }
            val sessionManager = SessionManager(tokenStore)
            val client = OkHttpClient.Builder()
                .addInterceptor(UnauthorizedInterceptor(sessionManager))
                .build()

            client.newCall(
                Request.Builder()
                    .url(server.url("/api/auth/unlock"))
                    .header("Authorization", "Bearer access-token")
                    .build()
            ).execute().close()
            org.junit.Assert.assertTrue(sessionManager.hasAccessToken())

            client.newCall(
                Request.Builder()
                    .url(server.url("/api/system/users/me/password"))
                    .header("Authorization", "Bearer access-token")
                    .build()
            ).execute().close()
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
