package com.sync.xxx.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * VPNManager.kt
 * Detect VPN connection
 * Check if device is using VPN
 */
class VPNManager(private val context: Context) {

    private val TAG = "VPNManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Check if VPN is active
     */
    fun isVPNActive(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkVPNModern()
        } else {
            checkVPNLegacy()
        }
    }

    /**
     * Check VPN using modern API (Android 6.0+)
     */
    private fun checkVPNModern(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VPN (modern)", e)
            false
        }
    }

    /**
     * Check VPN using legacy method
     */
    private fun checkVPNLegacy(): Boolean {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val name = networkInterface.name.lowercase()
                
                // Common VPN interface names
                if (name.contains("tun") || 
                    name.contains("ppp") || 
                    name.contains("pptp") ||
                    name.contains("vpn")) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VPN (legacy)", e)
            false
        }
    }

    /**
     * Get VPN interface name
     */
    fun getVPNInterfaceName(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val name = networkInterface.name.lowercase()
                
                if (name.contains("tun") || 
                    name.contains("ppp") || 
                    name.contains("pptp") ||
                    name.contains("vpn")) {
                    return networkInterface.name
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting VPN interface", e)
            null
        }
    }

    /**
     * Get all VPN interfaces
     */
    fun getVPNInterfaces(): List<String> {
        val vpnInterfaces = mutableListOf<String>()
        
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val name = networkInterface.name.lowercase()
                
                if (name.contains("tun") || 
                    name.contains("ppp") || 
                    name.contains("pptp") ||
                    name.contains("vpn")) {
                    vpnInterfaces.add(networkInterface.name)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting VPN interfaces", e)
        }
        
        return vpnInterfaces
    }

    /**
     * Check if connected through VPN network
     */
    fun isConnectedViaVPN(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return checkVPNLegacy()
        }
        
        return try {
            val networks = connectivityManager.allNetworks
            for (network in networks) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VPN connection", e)
            false
        }
    }

    /**
     * Get VPN connection type description
     */
    fun getVPNDescription(): String {
        return if (isVPNActive()) {
            val interfaceName = getVPNInterfaceName()
            if (interfaceName != null) {
                "Active ($interfaceName)"
            } else {
                "Active"
            }
        } else {
            "Not Active"
        }
    }

    /**
     * Export VPN info as JSON
     */
    fun getVPNInfoAsJson(): JSONObject {
        return JSONObject().apply {
            put("isActive", isVPNActive())
            put("isConnected", isConnectedViaVPN())
            put("description", getVPNDescription())
            put("interfaceName", getVPNInterfaceName())
            put("interfaces", getVPNInterfaces())
        }
    }

    /**
     * Export VPN info as text
     */
    fun exportVPNInfo(): String {
        val sb = StringBuilder()

        sb.append("VPN Status\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("VPN Active: ${if (isVPNActive()) "Yes" else "No"}\n")
        sb.append("Connected via VPN: ${if (isConnectedViaVPN()) "Yes" else "No"}\n")
        sb.append("Description: ${getVPNDescription()}\n\n")

        val interfaceName = getVPNInterfaceName()
        if (interfaceName != null) {
            sb.append("--- Interface ---\n")
            sb.append("Primary: $interfaceName\n\n")
        }

        val interfaces = getVPNInterfaces()
        if (interfaces.isNotEmpty()) {
            sb.append("--- All VPN Interfaces ---\n")
            interfaces.forEachIndexed { index, iface ->
                sb.append("${index + 1}. $iface\n")
            }
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if VPN is active
         */
        fun isVPNActive(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val network = connectivityManager.activeNetwork ?: return false
                    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                } catch (e: Exception) {
                    false
                }
            } else {
                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    while (interfaces.hasMoreElements()) {
                        val networkInterface = interfaces.nextElement()
                        val name = networkInterface.name.lowercase()
                        
                        if (name.contains("tun") || 
                            name.contains("ppp") || 
                            name.contains("pptp") ||
                            name.contains("vpn")) {
                            return true
                        }
                    }
                    false
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
}
