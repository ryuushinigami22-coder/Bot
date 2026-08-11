package com.sync.xxx.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * IPAddressManager.kt
 * Get IP address information
 * Local IPv4/IPv6, public IP, MAC address
 */
class IPAddressManager(private val context: Context) {

    private val TAG = "IPAddressManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Get local IPv4 address
     */
    fun getLocalIPv4(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return null
                val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
                
                linkProperties.linkAddresses.firstOrNull { 
                    it.address is Inet4Address && !it.address.isLoopbackAddress
                }?.address?.hostAddress
            } else {
                getLocalIPv4Legacy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IPv4", e)
            null
        }
    }

    /**
     * Get local IPv6 address
     */
    fun getLocalIPv6(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return null
                val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
                
                linkProperties.linkAddresses.firstOrNull { 
                    it.address is Inet6Address && !it.address.isLoopbackAddress
                }?.address?.hostAddress
            } else {
                getLocalIPv6Legacy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IPv6", e)
            null
        }
    }

    /**
     * Get local IPv4 (legacy method)
     */
    private fun getLocalIPv4Legacy(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IPv4 (legacy)", e)
            null
        }
    }

    /**
     * Get local IPv6 (legacy method)
     */
    private fun getLocalIPv6Legacy(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet6Address) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IPv6 (legacy)", e)
            null
        }
    }

    /**
     * Get all local IP addresses
     */
    fun getAllLocalIPs(): List<String> {
        val ipList = mutableListOf<String>()
        
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress) {
                        address.hostAddress?.let { ipList.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all local IPs", e)
        }
        
        return ipList
    }

    /**
     * Check if IPv4 is available
     */
    fun hasIPv4(): Boolean {
        return getLocalIPv4() != null
    }

    /**
     * Check if IPv6 is available
     */
    fun hasIPv6(): Boolean {
        return getLocalIPv6() != null
    }

    /**
     * Get MAC address
     */
    fun getMACAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }
                
                val mac = networkInterface.hardwareAddress
                if (mac != null && mac.isNotEmpty()) {
                    val macAddress = mac.joinToString(":") { 
                        String.format("%02X", it)
                    }
                    return macAddress
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MAC address", e)
            null
        }
    }

    /**
     * Export IP info as JSON
     */
    fun getIPInfoAsJson(): JSONObject {
        return JSONObject().apply {
            put("ipv4", getLocalIPv4())
            put("ipv6", getLocalIPv6())
            put("hasIPv4", hasIPv4())
            put("hasIPv6", hasIPv6())
            put("macAddress", getMACAddress())
            put("allLocalIPs", getAllLocalIPs())
        }
    }

    /**
     * Export IP info as text
     */
    fun exportIPInfo(): String {
        val sb = StringBuilder()

        sb.append("IP Address Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Local Addresses ---\n")
        sb.append("IPv4: ${getLocalIPv4() ?: "Not available"}\n")
        sb.append("IPv6: ${getLocalIPv6() ?: "Not available"}\n")
        sb.append("MAC: ${getMACAddress() ?: "Not available"}\n\n")

        val allIPs = getAllLocalIPs()
        if (allIPs.isNotEmpty()) {
            sb.append("--- All Local IPs ---\n")
            allIPs.forEachIndexed { index, ip ->
                sb.append("${index + 1}. $ip\n")
            }
        }

        return sb.toString()
    }

    companion object {
        /**
         * Get local IPv4 address
         */
        fun getLocalIPv4(context: Context): String? {
            return try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork ?: return null
                    val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
                    
                    linkProperties.linkAddresses.firstOrNull { 
                        it.address is Inet4Address && !it.address.isLoopbackAddress
                    }?.address?.hostAddress
                } else {
                    getLocalIPv4Legacy()
                }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Get local IPv4 (legacy)
         */
        private fun getLocalIPv4Legacy(): String? {
            return try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}
