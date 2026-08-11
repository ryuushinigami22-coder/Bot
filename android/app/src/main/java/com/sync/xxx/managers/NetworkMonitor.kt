package com.sync.xxx.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import org.json.JSONObject

/**
 * NetworkMonitor.kt
 * Monitor network connectivity changes
 * Detect network type, availability, and connectivity status
 */
class NetworkMonitor(private val context: Context) {

    private val TAG = "NetworkMonitor"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var onNetworkAvailableListener: ((Network) -> Unit)? = null
    private var onNetworkLostListener: ((Network) -> Unit)? = null
    private var onNetworkCapabilitiesChangedListener: ((Network, NetworkCapabilities) -> Unit)? = null

    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * Check if connected to WiFi
     */
    fun isWifiConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    /**
     * Check if connected to mobile data
     */
    fun isMobileDataConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected
        }
    }

    /**
     * Check if connected to Ethernet
     */
    fun isEthernetConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_ETHERNET && networkInfo.isConnected
        }
    }

    /**
     * Get current network type
     */
    fun getNetworkType(): String {
        return when {
            isWifiConnected() -> "WiFi"
            isMobileDataConnected() -> "Mobile Data"
            isEthernetConnected() -> "Ethernet"
            else -> "None"
        }
    }

    /**
     * Check if metered network
     */
    fun isMeteredNetwork(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }

    /**
     * Start monitoring network changes
     */
    fun startMonitoring(
        onAvailable: ((Network) -> Unit)? = null,
        onLost: ((Network) -> Unit)? = null,
        onCapabilitiesChanged: ((Network, NetworkCapabilities) -> Unit)? = null
    ) {
        if (networkCallback != null) {
            Log.w(TAG, "Already monitoring network")
            return
        }

        onNetworkAvailableListener = onAvailable
        onNetworkLostListener = onLost
        onNetworkCapabilitiesChangedListener = onCapabilitiesChanged

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                onNetworkAvailableListener?.invoke(network)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                onNetworkLostListener?.invoke(network)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                Log.d(TAG, "Network capabilities changed: $network")
                onNetworkCapabilitiesChangedListener?.invoke(network, capabilities)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
        Log.d(TAG, "Started monitoring network")
    }

    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
            onNetworkAvailableListener = null
            onNetworkLostListener = null
            onNetworkCapabilitiesChangedListener = null
            Log.d(TAG, "Stopped monitoring network")
        }
    }

    /**
     * Get network capabilities
     */
    fun getNetworkCapabilities(): NetworkCapabilities? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return null
            connectivityManager.getNetworkCapabilities(network)
        } else {
            null
        }
    }

    /**
     * Export network status as JSON
     */
    fun getNetworkStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("isNetworkAvailable", isNetworkAvailable())
            put("isWifiConnected", isWifiConnected())
            put("isMobileDataConnected", isMobileDataConnected())
            put("isEthernetConnected", isEthernetConnected())
            put("networkType", getNetworkType())
            put("isMeteredNetwork", isMeteredNetwork())
        }
    }

    /**
     * Export network info as text
     */
    fun exportNetworkInfo(): String {
        val sb = StringBuilder()

        sb.append("Network Status\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Connectivity ---\n")
        sb.append("Network Available: ${if (isNetworkAvailable()) "Yes" else "No"}\n")
        sb.append("Network Type: ${getNetworkType()}\n")
        sb.append("WiFi: ${if (isWifiConnected()) "Connected" else "Not Connected"}\n")
        sb.append("Mobile Data: ${if (isMobileDataConnected()) "Connected" else "Not Connected"}\n")
        sb.append("Ethernet: ${if (isEthernetConnected()) "Connected" else "Not Connected"}\n")
        sb.append("Metered: ${if (isMeteredNetwork()) "Yes" else "No"}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Check if network is available
         */
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo != null && networkInfo.isConnected
            }
        }

        /**
         * Check if connected to WiFi
         */
        fun isWifiConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
            }
        }
    }
}
