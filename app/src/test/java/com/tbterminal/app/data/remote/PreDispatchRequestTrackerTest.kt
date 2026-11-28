package com.tbterminal.app.data.remote

import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.toRepositoryResult
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import kotlin.concurrent.thread
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.POST

class PreDispatchRequestTrackerTest {

    private interface ProbeApi {
        @POST("/api/sales/sessions/open")
        suspend fun openSession(): Response<ResponseBody>
    }

    private fun buildClient(
        socketFactory: SocketFactory? = null,
        connectTimeoutMs: Long = 1_000L,
        readTimeoutMs: Long = 1_000L,
    ): OkHttpClient {
        val tracker = PreDispatchRequestTracker()
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(1_000L, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .apply {
                if (socketFactory != null) {
                    socketFactory(socketFactory)
                }
            }
            .eventListenerFactory(tracker.eventListenerFactory)
            .addInterceptor(tracker.interceptor)
            .build()
    }

    private fun buildApi(client: OkHttpClient, port: Int): ProbeApi {
        return Retrofit.Builder()
            .baseUrl("http://127.0.0.1:$port/")
            .client(client)
            .build()
            .create(ProbeApi::class.java)
    }

    private suspend fun callAsRepoResult(api: ProbeApi): RepositoryResult<Unit> {
        return safeApiCall { api.openSession() }.toRepositoryResult { RepositoryResult.Success(Unit) }
    }

    @Test
    fun connectionRefused_isClassifiedAsPreDispatchAndAllowsFallback() = runTest {
        val unusedPort = ServerSocket(0).use { it.localPort }
        val api = buildApi(buildClient(), unusedPort)

        val result = callAsRepoResult(api) as RepositoryResult.Error
        assertEquals("CONNECTION_FAILED", result.code)
        assertTrue(ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result))
    }

    @Test
    fun connectionAcceptedThenDroppedBeforeHeaders_isClassifiedAsPreDispatchAndAllowsFallback() = runTest {
        val eofBeforeWriteFactory = object : SocketFactory() {
            private fun failingSocket(): Socket = object : Socket() {
                override fun connect(endpoint: SocketAddress?, timeout: Int) {
                    // Connect succeeds locally (like adb reverse), then fails before headers are sent.
                }

                override fun getOutputStream() = throw EOFException("Connection closed before request headers")
                override fun getInputStream() = throw EOFException("Connection closed before request headers")
            }

            override fun createSocket(): Socket = failingSocket()
            override fun createSocket(host: String?, port: Int) = failingSocket()
            override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int) = failingSocket()
            override fun createSocket(host: InetAddress?, port: Int) = failingSocket()
            override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int) = failingSocket()
        }
        val api = buildApi(buildClient(socketFactory = eofBeforeWriteFactory), 8080)

        val result = callAsRepoResult(api) as RepositoryResult.Error
        assertEquals("CONNECTION_FAILED", result.code)
        assertTrue(ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result))
    }

    @Test
    fun connectTimeout_isClassifiedAsPreDispatchConnectTimeoutAndAllowsFallback() = runTest {
        val connectTimeoutFactory = object : SocketFactory() {
            private fun timingOutSocket(): Socket = object : Socket() {
                override fun connect(endpoint: SocketAddress?, timeout: Int) {
                    throw SocketTimeoutException("connect timed out")
                }
            }

            override fun createSocket(): Socket = timingOutSocket()
            override fun createSocket(host: String?, port: Int) = timingOutSocket()
            override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int) = timingOutSocket()
            override fun createSocket(host: InetAddress?, port: Int) = timingOutSocket()
            override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int) = timingOutSocket()
        }
        val api = buildApi(buildClient(socketFactory = connectTimeoutFactory, connectTimeoutMs = 200L), 8080)

        val result = callAsRepoResult(api) as RepositoryResult.Error
        assertEquals("CONNECT_TIMEOUT", result.code)
        assertTrue(ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result))
    }

    @Test
    fun readTimeoutAfterRequestHeadersSent_isAmbiguousAndDoesNotFallback() = runTest {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val serverThread = thread(isDaemon = true) {
            runCatching {
                server.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    Thread.sleep(600L)
                }
            }
        }
        try {
            val api = buildApi(buildClient(readTimeoutMs = 150L), server.localPort)
            val result = callAsRepoResult(api) as RepositoryResult.Error
            assertEquals("NETWORK_TIMEOUT", result.code)
            assertFalse(ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result))
            assertTrue(ServerUnavailabilityClassifier.isServerUnavailable(result))
        } finally {
            runCatching { server.close() }
            serverThread.join(1_000L)
        }
    }

    @Test
    fun connectionDroppedAfterRequestHeadersSent_isAmbiguousAndDoesNotFallback() = runTest {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val serverThread = thread(isDaemon = true) {
            runCatching {
                server.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    // Close immediately after reading headers without sending response
                }
            }
        }
        try {
            val api = buildApi(buildClient(), server.localPort)
            val result = callAsRepoResult(api) as RepositoryResult.Error
            assertEquals("CONNECTION_INTERRUPTED", result.code)
            assertFalse(ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result))
            assertTrue(ServerUnavailabilityClassifier.isServerUnavailable(result))
        } finally {
            runCatching { server.close() }
            serverThread.join(1_000L)
        }
    }

    @Test
    fun http504GatewayTimeout_isAmbiguousAndDoesNotFallback() = runTest {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val serverThread = thread(isDaemon = true) {
            runCatching {
                server.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    val response = buildString {
                        append("HTTP/1.1 504 Gateway Timeout\r\n")
                        append("Content-Length: 0\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    client.getOutputStream().write(response.toByteArray())
                    client.getOutputStream().flush()
                }
            }
        }
        try {
            val api = buildApi(buildClient(), server.localPort)
            val result = callAsRepoResult(api) as RepositoryResult.Error
            assertEquals("HTTP_504", result.code)
            assertFalse(ServerUnavailabilityClassifier.isDefinitelyUnprocessedServerUnavailable(result))
            assertTrue(ServerUnavailabilityClassifier.isServerUnavailable(result))
        } finally {
            runCatching { server.close() }
            serverThread.join(1_000L)
        }
    }
}
