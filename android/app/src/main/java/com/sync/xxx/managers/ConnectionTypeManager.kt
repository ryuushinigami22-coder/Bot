package com.sync.xxx.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import org.json.JSONObject

/**
 * ConnectionTypeManager.kt
 * Detect connection type and characteristics
 * WiFi, mobile (2G/3G/4G/5G), Ethernet, VPN, etc.
 */
class ConnectionTypeManager(private val context: Context) {

    private val TAG = "ConnectionTypeManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /**
     * Get primary connection type
     */
    fun getConnectionType(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "None"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                ConnectivityManager.TYPE_BLUETOOTH -> "Bluetooth"
                ConnectivityManager.TYPE_VPN -> "VPN"
                else -> "None"
            }
        }
    }

    /**
     * Check if connected via WiFi
     */
    fun isWiFi(): Boolean {
        return getConnectionType() == "WiFi"
    }

    /**
     * Check if connected via mobile
     */
    fun isMobile(): Boolean {
        return getConnectionType() == "Mobile"
    }

    /**
     * Check if connected via Ethernet
     */
    fun isEthernet(): Boolean {
        return getConnectionType() == "Ethernet"
    }

    /**
     * Check if connected via VPN
     */
    fun isVPN(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else {
            getConnectionType() == "VPN"
        }
    }

    /**
     * Get mobile network generation (2G, 3G, 4G, 5G)
     */
    fun getMobileGeneration(): String {
        if (!isMobile()) return "N/A"
        
        return try {
            when (telephonyManager.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting mobile generation", e)
            "Unknown"
        }
    }

    /**
     * Get detailed connection description
     */
    fun getConnectionDescription(): String {
        val type = getConnectionType()
        return if (type == "Mobile") {
            "$type (${getMobileGeneration()})"
        } else {
            type
        }
    }

    /**
     * Check if connection is metered
     */
    fun isMetered(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }

    /**
     * Check if connection is roaming
     */
    fun isRoaming(): Boolean {
        return telephonyManager.isNetworkRoaming
    }

    /**
     * Get all active transport types
     */
    fun getActiveTransports(): List<String> {
        val transports = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return emptyList()
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return emptyList()
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                transports.add("WiFi")
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                transports.add("Cellular")
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                transports.add("Ethernet")
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                transports.add("Bluetooth")
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                transports.add("VPN")
            }
        }
        
        return transports
    }

    /**
     * Export connection type info as JSON
     */
    fun getConnectionTypeAsJson(): JSONObject {
        return JSONObject().apply {
            put("primaryType", getConnectionType())
            put("description", getConnectionDescription())
            put("isWiFi", isWiFi())
            put("isMobile", isMobile())
            put("isEthernet", isEthernet())
            put("isVPN", isVPN())
            put("mobileGeneration", getMobileGeneration())
            put("isMetered", isMetered())
            put("isRoaming", isRoaming())
            put("activeTransports", getActiveTransports())
        }
    }

    /**
     * Export connection type info as text
     */
    fun exportConnectionTypeInfo(): String {
        val sb = StringBuilder()

        sb.append("Connection Type\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Primary Connection ---\n")
        sb.append("Type: ${getConnectionType()}\n")
        sb.append("Description: ${getConnectionDescription()}\n")
        sb.append("Metered: ${if (isMetered()) "Yes" else "No"}\n")
        sb.append("Roaming: ${if (isRoaming()) "Yes" else "No"}\n\n")

        sb.append("--- Connection Details ---\n")
        sb.append("WiFi: ${if (isWiFi()) "Yes" else "No"}\n")
        sb.append("Mobile: ${if (isMobile()) "Yes" else "No"}\n")
        if (isMobile()) {
            sb.append("Mobile Generation: ${getMobileGeneration()}\n")
        }
        sb.append("Ethernet: ${if (isEthernet()) "Yes" else "No"}\n")
        sb.append("VPN: ${if (isVPN()) "Yes" else "No"}\n\n")

        val transports = getActiveTransports()
        if (transports.isNotEmpty()) {
            sb.append("--- Active Transports ---\n")
            transports.forEachIndexed { index, transport ->
                sb.append("${index + 1}. $transport\n")
            }
        }

        return sb.toString()
    }

    companion object {
        /**
         * Get connection type
         */
        fun getConnectionType(context: Context): String {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "None"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
                
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                when (networkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "WiFi"
                    ConnectivityManager.TYPE_MOBILE -> "Mobile"
                    else -> "None"
                }
            }
        }
    }
}
