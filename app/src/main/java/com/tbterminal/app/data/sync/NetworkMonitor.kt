package com.tbterminal.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface NetworkConnectivity {
    fun isOnline(): Boolean
    fun observeOnline(): Flow<Boolean>
}

class NetworkMonitor(
    private val context: Context
) : NetworkConnectivity {
    override fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        return connectivityManager.isOnline()
    }

    override fun observeOnline(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager

        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        trySend(connectivityManager.isOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(connectivityManager.isOnline())
            }

            override fun onLost(network: Network) {
                trySend(connectivityManager.isOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(networkCapabilities.hasInternet())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()

    private fun ConnectivityManager.isOnline(): Boolean {
        val activeNetwork = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasInternet()
    }

    private fun NetworkCapabilities.hasInternet(): Boolean {
        return hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
