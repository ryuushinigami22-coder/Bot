package com.sync.xxx.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import org.json.JSONObject
import java.net.Inet4Address
import java.net.Inet6Address

/**
 * NetworkInfoManager.kt
 * Get detailed network information
 * Connection type, speed, IP addresses, capabilities
 */
class NetworkInfoManager(private val context: Context) {

    private val TAG = "NetworkInfoManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /**
     * Get network type name
     */
    fun getNetworkTypeName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "None"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
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
                ConnectivityManager.TYPE_MOBILE -> "Cellular"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                ConnectivityManager.TYPE_BLUETOOTH -> "Bluetooth"
                ConnectivityManager.TYPE_VPN -> "VPN"
                else -> "None"
            }
        }
    }

    /**
     * Get cellular network type (2G, 3G, 4G, 5G)
     */
    fun getCellularNetworkType(): String {
        return try {
            when (telephonyManager.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT -> "2G CDMA"
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G EVDO"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get link downstream bandwidth (Kbps)
     */
    fun getDownstreamBandwidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return 0
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0
            capabilities.linkDownstreamBandwidthKbps
        } else {
            0
        }
    }

    /**
     * Get link upstream bandwidth (Kbps)
     */
    fun getUpstreamBandwidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return 0
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0
            capabilities.linkUpstreamBandwidthKbps
        } else {
            0
        }
    }

    /**
     * Get IPv4 address
     */
    fun getIPv4Address(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return null
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
            
            linkProperties.linkAddresses.firstOrNull { 
                it.address is Inet4Address 
            }?.address?.hostAddress
        } else {
            null
        }
    }

    /**
     * Get IPv6 address
     */
    fun getIPv6Address(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return null
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
            
            linkProperties.linkAddresses.firstOrNull { 
                it.address is Inet6Address 
            }?.address?.hostAddress
        } else {
            null
        }
    }

    /**
     * Get DNS servers
     */
    fun getDNSServers(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return emptyList()
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return emptyList()
            
            linkProperties.dnsServers.map { it.hostAddress ?: "Unknown" }
        } else {
            emptyList()
        }
    }

    /**
     * Get interface name
     */
    fun getInterfaceName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return null
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
            linkProperties.interfaceName
        } else {
            null
        }
    }

    /**
     * Check if network is metered
     */
    fun isMetered(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }

    /**
     * Check if network has internet capability
     */
    fun hasInternetCapability(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            true
        }
    }

    /**
     * Check if network is validated (has internet access)
     */
    fun isValidated(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            true
        }
    }

    /**
     * Export network info as JSON
     */
    fun getNetworkInfoAsJson(): JSONObject {
        return JSONObject().apply {
            put("networkType", getNetworkTypeName())
            put("cellularType", getCellularNetworkType())
            put("downstreamBandwidthKbps", getDownstreamBandwidth())
            put("upstreamBandwidthKbps", getUpstreamBandwidth())
            put("ipv4Address", getIPv4Address())
            put("ipv6Address", getIPv6Address())
            put("dnsServers", getDNSServers())
            put("interfaceName", getInterfaceName())
            put("isMetered", isMetered())
            put("hasInternet", hasInternetCapability())
            put("isValidated", isValidated())
        }
    }

    /**
     * Export network info as text
     */
    fun exportNetworkInfo(): String {
        val sb = StringBuilder()

        sb.append("Network Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Connection ---\n")
        sb.append("Type: ${getNetworkTypeName()}\n")
        sb.append("Cellular Type: ${getCellularNetworkType()}\n")
        sb.append("Interface: ${getInterfaceName() ?: "Unknown"}\n")
        sb.append("Metered: ${if (isMetered()) "Yes" else "No"}\n")
        sb.append("Has Internet: ${if (hasInternetCapability()) "Yes" else "No"}\n")
        sb.append("Validated: ${if (isValidated()) "Yes" else "No"}\n\n")

        sb.append("--- Speed ---\n")
        sb.append("Downstream: ${getDownstreamBandwidth()} Kbps\n")
        sb.append("Upstream: ${getUpstreamBandwidth()} Kbps\n\n")

        sb.append("--- Addresses ---\n")
        sb.append("IPv4: ${getIPv4Address() ?: "None"}\n")
        sb.append("IPv6: ${getIPv6Address() ?: "None"}\n\n")

        sb.append("--- DNS Servers ---\n")
        val dnsServers = getDNSServers()
        if (dnsServers.isEmpty()) {
            sb.append("None\n")
        } else {
            dnsServers.forEachIndexed { index, dns ->
                sb.append("${index + 1}. $dns\n")
            }
        }

        return sb.toString()
    }

    companion object {
        /**
         * Get network type name
         */
        fun getNetworkTypeName(context: Context): String {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "None"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
                
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                when (networkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "WiFi"
                    ConnectivityManager.TYPE_MOBILE -> "Cellular"
                    else -> "None"
                }
            }
        }
    }
}
