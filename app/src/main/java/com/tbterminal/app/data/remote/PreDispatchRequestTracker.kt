package com.tbterminal.app.data.remote

import java.io.IOException
import java.net.ProtocolException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Wraps an [IOException] thrown before OkHttp started transmitting the HTTP request
 * (i.e. before `requestHeadersStart`), proving the server never received the mutation.
 */
class PreDispatchIOException(
    cause: IOException,
) : IOException(cause.message ?: "Request gagal sebelum dikirim ke server", cause)

/**
 * Tracks per-call dispatch state via OkHttp [EventListener] so callers can distinguish
 * pre-dispatch connection failures (safe to fall back locally) from mid/post-dispatch
 * transport errors (ambiguous outcome).
 */
class PreDispatchRequestTracker {
    private class CallDispatchState(
        @Volatile var requestDispatched: Boolean = false,
    )

    private val states = ConcurrentHashMap<Call, CallDispatchState>()

    val eventListenerFactory: EventListener.Factory = EventListener.Factory { call ->
        val state = states.computeIfAbsent(call) { CallDispatchState() }
        object : EventListener() {
            override fun requestHeadersStart(call: Call) {
                state.requestDispatched = true
            }

            override fun requestHeadersEnd(call: Call, request: Request) {
                state.requestDispatched = true
            }

            override fun requestBodyStart(call: Call) {
                state.requestDispatched = true
            }

            override fun callEnd(call: Call) {
                states.remove(call)
            }

            override fun callFailed(call: Call, ioe: IOException) {
                states.remove(call)
            }

            override fun canceled(call: Call) {
                states.remove(call)
            }
        }
    }

    val interceptor: Interceptor = Interceptor { chain ->
        val call = chain.call()
        val state = states.computeIfAbsent(call) { CallDispatchState() }
        try {
            chain.proceed(chain.request())
        } catch (io: IOException) {
            if (!state.requestDispatched &&
                io !is PreDispatchIOException &&
                io !is SSLException &&
                io !is ProtocolException
            ) {
                throw PreDispatchIOException(io)
            }
            throw io
        } finally {
            states.remove(call)
        }
    }
}
