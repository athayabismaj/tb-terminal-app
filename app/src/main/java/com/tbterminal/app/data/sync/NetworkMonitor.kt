package com.tbterminal.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

interface NetworkConnectivity {
    fun isOnline(): Boolean
    fun observeOnline(): Flow<Boolean>
}

class NetworkMonitor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NetworkConnectivity {
    @Volatile
    private var isObserving: Boolean = false

    @Volatile
    private var cachedOnline: Boolean? = null

    override fun isOnline(): Boolean {
        if (isObserving) {
            cachedOnline?.let { return it }
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        return connectivityManager.isOnline().also { online ->
            if (isObserving) {
                cachedOnline = online
            }
        }
    }

    override fun observeOnline(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager

        if (connectivityManager == null) {
            cachedOnline = false
            trySend(false)
            close()
            return@callbackFlow
        }

        val initialOnline = connectivityManager.isOnline()
        cachedOnline = initialOnline
        isObserving = true
        trySend(initialOnline)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val online = connectivityManager.isOnline()
                cachedOnline = online
                trySend(online)
            }

            override fun onLost(network: Network) {
                val online = connectivityManager.isOnline()
                cachedOnline = online
                trySend(online)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val online = networkCapabilities.hasInternet()
                cachedOnline = online
                trySend(online)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            isObserving = false
            cachedOnline = null
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged().flowOn(ioDispatcher)

    private fun ConnectivityManager.isOnline(): Boolean {
        val activeNetwork = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasInternet()
    }

    private fun NetworkCapabilities.hasInternet(): Boolean {
        return hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
