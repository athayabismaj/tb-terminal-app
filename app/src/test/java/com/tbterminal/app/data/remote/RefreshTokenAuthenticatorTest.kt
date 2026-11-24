package com.tbterminal.app.data.remote

import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RefreshTokenAuthenticatorTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parallel401RequestsRefreshSingleUseTokenOnceAndReuseRotatedAccessToken() {
        val refreshCalls = AtomicInteger(0)
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/api/auth/refresh" -> {
                        val callNum = refreshCalls.incrementAndGet()
                        Thread.sleep(60)
                        val body = request.body.readUtf8()
                        if (callNum == 1 && body.contains("\"refresh-1\"")) {
                            MockResponse()
                                .setResponseCode(200)
                                .setBody("""{"success":true,"data":{"token":"access-2","refreshToken":"refresh-2"}}""")
                        } else {
                            MockResponse().setResponseCode(401)
                        }
                    }
                    request.getHeader("Authorization") == "Bearer access-2" -> {
                        MockResponse().setResponseCode(200).setBody("""{"ok":true}""")
                    }
                    else -> {
                        MockResponse().setResponseCode(401)
                    }
                }
            }
        }
        server.start()
        try {
            val tokenStore = TestRefreshTokenStore("access-1", "refresh-1")
            val sessionManager = SessionManager(tokenStore)
            val refreshApi = createRefreshApi(server.url("/").toString(), retryOnFailure = false)
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .authenticator(RefreshTokenAuthenticator(tokenStore, sessionManager, refreshApi))
                .build()

            val pool = Executors.newFixedThreadPool(5)
            val startLatch = CountDownLatch(1)
            val tasks = (1..5).map { idx ->
                Callable {
                    startLatch.await(3, TimeUnit.SECONDS)
                    client.newCall(
                        Request.Builder().url(server.url("/api/orders/$idx")).build()
                    ).execute().use { it.code }
                }
            }
            val futures = tasks.map { pool.submit(it) }
            startLatch.countDown()
            val codes = futures.map { it.get(10, TimeUnit.SECONDS) }
            pool.shutdown()

            assertEquals(listOf(200, 200, 200, 200, 200), codes)
            assertEquals(1, refreshCalls.get())
            assertEquals("access-2", tokenStore.readAccessToken())
            assertEquals("refresh-2", tokenStore.readRefreshToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun refresh401RevokesSessionImmediately() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()
        try {
            val tokenStore = TestRefreshTokenStore("access-1", "refresh-1")
            val sessionManager = SessionManager(tokenStore)
            val refreshApi = createRefreshApi(server.url("/").toString(), retryOnFailure = false)
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .authenticator(RefreshTokenAuthenticator(tokenStore, sessionManager, refreshApi))
                .build()

            val response = client.newCall(Request.Builder().url(server.url("/api/orders")).build()).execute()
            val code = response.code
            response.close()

            assertEquals(401, code)
            assertFalse(sessionManager.hasAccessToken())
            assertNull(tokenStore.readRefreshToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun postConnectRefreshDropDoesNotResendConsumedRefreshTokenAndClearsSession() {
        val apiServer = MockWebServer()
        val refreshServer = MockWebServer()
        apiServer.enqueue(MockResponse().setResponseCode(401))
        refreshServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        apiServer.start()
        refreshServer.start()
        try {
            val tokenStore = TestRefreshTokenStore("access-1", "refresh-single-use")
            val sessionManager = SessionManager(tokenStore)
            val refreshApi = createRefreshApi(refreshServer.url("/").toString(), retryOnFailure = false)
            val authenticator = RefreshTokenAuthenticator(tokenStore, sessionManager, refreshApi)
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .authenticator(authenticator)
                .build()

            val firstResponse = client.newCall(Request.Builder().url(apiServer.url("/api/protected")).build()).execute()
            assertEquals(401, firstResponse.code)
            firstResponse.close()

            assertEquals(1, refreshServer.requestCount)
            assertFalse(sessionManager.hasAccessToken())
        } finally {
            apiServer.shutdown()
            refreshServer.shutdown()
        }
    }

    @Test
    fun preConnectNetworkFailureOnRefreshPreservesSession() {
        val apiServer = MockWebServer()
        val closedRefreshServer = MockWebServer()
        closedRefreshServer.start()
        val unreachableRefreshUrl = closedRefreshServer.url("/").toString()
        closedRefreshServer.shutdown()

        apiServer.enqueue(MockResponse().setResponseCode(401))
        apiServer.start()
        try {
            val tokenStore = TestRefreshTokenStore("access-1", "refresh-offline")
            val sessionManager = SessionManager(tokenStore)
            val refreshApi = createRefreshApi(unreachableRefreshUrl, retryOnFailure = false)
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .authenticator(RefreshTokenAuthenticator(tokenStore, sessionManager, refreshApi))
                .build()

            val response = client.newCall(Request.Builder().url(apiServer.url("/api/protected")).build()).execute()
            assertEquals(401, response.code)
            response.close()

            assertTrue(sessionManager.hasAccessToken())
            assertEquals("refresh-offline", tokenStore.readRefreshToken())
        } finally {
            apiServer.shutdown()
        }
    }

    @Test
    fun unlockInvalidPin401SkipsRefreshAndCredentialMaxAttemptsRevokesWithoutRefreshing() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"success":false,"code":"INVALID_PIN","message":"PIN salah"}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"success":false,"code":"UNAUTHORIZED","message":"Batas percobaan terlampaui. Silakan login kembali."}""")
        )
        server.start()
        try {
            val tokenStore = TestRefreshTokenStore("access-1", "refresh-1")
            val sessionManager = SessionManager(tokenStore)
            val refreshApi = createRefreshApi(server.url("/").toString(), retryOnFailure = false)
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .authenticator(RefreshTokenAuthenticator(tokenStore, sessionManager, refreshApi))
                .build()

            client.newCall(Request.Builder().url(server.url("/api/auth/unlock")).build()).execute().use {
                assertEquals(401, it.code)
            }
            assertTrue(sessionManager.hasAccessToken())
            assertEquals(1, server.requestCount)

            client.newCall(Request.Builder().url(server.url("/api/system/users/me/password")).build()).execute().use {
                assertEquals(401, it.code)
            }
            assertFalse(sessionManager.hasAccessToken())
            assertEquals(2, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun sequentialRefreshUsesNewRotatedRefreshTokenAndNeverReusesOldOne() {
        val seenRefreshBodies = mutableListOf<String>()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/api/auth/refresh" -> {
                        val body = request.body.readUtf8()
                        seenRefreshBodies += body
                        when {
                            body.contains("\"refresh-1\"") -> MockResponse()
                                .setResponseCode(200)
                                .setBody("""{"success":true,"data":{"token":"access-2","refreshToken":"refresh-2"}}""")
                            body.contains("\"refresh-2\"") -> MockResponse()
                                .setResponseCode(200)
                                .setBody("""{"success":true,"data":{"token":"access-3","refreshToken":"refresh-3"}}""")
                            else -> MockResponse().setResponseCode(401)
                        }
                    }
                    request.path == "/api/orders/1" && request.getHeader("Authorization") == "Bearer access-2" ->
                        MockResponse().setResponseCode(200).setBody("""{"ok":true}""")
                    request.path == "/api/orders/2" && request.getHeader("Authorization") == "Bearer access-3" ->
                        MockResponse().setResponseCode(200).setBody("""{"ok":true}""")
                    else -> MockResponse().setResponseCode(401)
                }
            }
        }
        server.start()
        try {
            val tokenStore = TestRefreshTokenStore("access-1", "refresh-1")
            val sessionManager = SessionManager(tokenStore)
            val refreshApi = createRefreshApi(server.url("/").toString(), retryOnFailure = false)
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .authenticator(RefreshTokenAuthenticator(tokenStore, sessionManager, refreshApi))
                .build()

            client.newCall(Request.Builder().url(server.url("/api/orders/1")).build()).execute().use {
                assertEquals(200, it.code)
            }
            client.newCall(Request.Builder().url(server.url("/api/orders/2")).build()).execute().use {
                assertEquals(200, it.code)
            }

            assertEquals(2, seenRefreshBodies.size)
            assertTrue(seenRefreshBodies[0].contains("\"refresh-1\""))
            assertTrue(seenRefreshBodies[1].contains("\"refresh-2\""))
            assertEquals("access-3", tokenStore.readAccessToken())
            assertEquals("refresh-3", tokenStore.readRefreshToken())
        } finally {
            server.shutdown()
        }
    }

    private fun createRefreshApi(baseUrl: String, retryOnFailure: Boolean): TokenRefreshApi {
        val refreshClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(retryOnFailure)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(refreshClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TokenRefreshApi::class.java)
    }
}

private class TestRefreshTokenStore(
    private var access: String?,
    private var refresh: String?,
) : TokenStore {
    private var user: SessionUser? = SessionUser("Kasir", "kasir", true, "2026-01-01", null, userId = "u1")

    override fun readAccessToken(): String? = access
    override fun hasStoredAccessToken(): Boolean = !access.isNullOrBlank()
    override fun readRefreshToken(): String? = refresh
    override fun saveAccessToken(token: String) { access = token }
    override fun saveTokens(accessToken: String, refreshToken: String) {
        access = accessToken
        refresh = refreshToken
    }
    override fun readSessionUser(): SessionUser? = user
    override fun saveSessionUser(user: SessionUser) { this.user = user }
    override fun clearAccessToken() {
        access = null
        refresh = null
        user = null
    }
}
