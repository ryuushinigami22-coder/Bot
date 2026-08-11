package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager as AndroidWifiManager
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * WifiManager.kt
 * Access WiFi information and saved networks
 * Scan networks, get connection info
 */
class WifiManager(private val context: Context) {

    private val TAG = "WifiManager"
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as AndroidWifiManager

    /**
     * Check if location permission is granted (required for WiFi scanning)
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * Enable WiFi
     */
    fun enableWifi(): Boolean {
        return try {
            wifiManager.isWifiEnabled = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling WiFi", e)
            false
        }
    }

    /**
     * Disable WiFi
     */
    fun disableWifi(): Boolean {
        return try {
            wifiManager.isWifiEnabled = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling WiFi", e)
            false
        }
    }

    /**
     * Get current WiFi connection info
     */
    fun getCurrentConnection(): WifiConnectionInfo? {
        if (!hasPermission()) {
            Log.e(TAG, "Location permission not granted")
            return null
        }

        return try {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo.networkId == -1) {
                null
            } else {
                WifiConnectionInfo(
                    ssid = wifiInfo.ssid.removeSurrounding("\""),
                    bssid = wifiInfo.bssid ?: "Unknown",
                    frequency = wifiInfo.frequency,
                    linkSpeed = wifiInfo.linkSpeed,
                    rssi = wifiInfo.rssi,
                    ipAddress = intToIp(wifiInfo.ipAddress),
                    macAddress = wifiInfo.macAddress ?: "Unknown"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connection info", e)
            null
        }
    }

    /**
     * Scan for available WiFi networks
     */
    fun scanNetworks(): List<WifiNetworkInfo> {
        if (!hasPermission()) {
            Log.e(TAG, "Location permission not granted")
            return emptyList()
        }

        return try {
            wifiManager.startScan()
            val scanResults = wifiManager.scanResults
            scanResults.map { result ->
                WifiNetworkInfo(
                    ssid = result.SSID,
                    bssid = result.BSSID,
                    capabilities = result.capabilities,
                    frequency = result.frequency,
                    level = result.level,
                    isSecured = isNetworkSecured(result)
                )
            }.sortedByDescending { it.level }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning networks", e)
            emptyList()
        }
    }

    /**
     * Get saved/configured networks
     */
    fun getSavedNetworks(): List<SavedWifiNetwork> {
        if (!hasPermission()) {
            Log.e(TAG, "Location permission not granted")
            return emptyList()
        }

        return try {
            val configuredNetworks = wifiManager.configuredNetworks ?: emptyList()
            configuredNetworks.map { config ->
                SavedWifiNetwork(
                    ssid = config.SSID.removeSurrounding("\""),
                    networkId = config.networkId,
                    isHidden = config.hiddenSSID
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting saved networks", e)
            emptyList()
        }
    }

    /**
     * Check if network is secured
     */
    private fun isNetworkSecured(result: ScanResult): Boolean {
        return result.capabilities.contains("WPA") ||
                result.capabilities.contains("WEP") ||
                result.capabilities.contains("WPA2") ||
                result.capabilities.contains("WPA3")
    }

    /**
     * Convert IP address integer to string
     */
    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }

    /**
     * Get WiFi signal strength description
     */
    fun getSignalStrength(level: Int): String {
        return when {
            level >= -50 -> "Excellent"
            level >= -60 -> "Good"
            level >= -70 -> "Fair"
            level >= -80 -> "Weak"
            else -> "Very Weak"
        }
    }

    /**
     * Export current connection as JSON
     */
    fun getCurrentConnectionAsJson(): JSONObject? {
        val connection = getCurrentConnection()
        return connection?.toJson()
    }

    /**
     * Export scanned networks as JSON
     */
    fun getScannedNetworksAsJson(): JSONArray {
        val networks = scanNetworks()
        val jsonArray = JSONArray()
        networks.forEach { network ->
            jsonArray.put(network.toJson())
        }
        return jsonArray
    }

    /**
     * Export saved networks as JSON
     */
    fun getSavedNetworksAsJson(): JSONArray {
        val networks = getSavedNetworks()
        val jsonArray = JSONArray()
        networks.forEach { network ->
            jsonArray.put(network.toJson())
        }
        return jsonArray
    }

    /**
     * Export WiFi info as text
     */
    fun exportWifiInfo(): String {
        val sb = StringBuilder()

        sb.append("WiFi Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        // Current connection
        val connection = getCurrentConnection()
        if (connection != null) {
            sb.append("--- Current Connection ---\n")
            sb.append("SSID: ${connection.ssid}\n")
            sb.append("BSSID: ${connection.bssid}\n")
            sb.append("IP Address: ${connection.ipAddress}\n")
            sb.append("MAC Address: ${connection.macAddress}\n")
            sb.append("Frequency: ${connection.frequency} MHz\n")
            sb.append("Link Speed: ${connection.linkSpeed} Mbps\n")
            sb.append("Signal: ${connection.rssi} dBm (${getSignalStrength(connection.rssi)})\n\n")
        } else {
            sb.append("Not connected to WiFi\n\n")
        }

        // Saved networks
        val savedNetworks = getSavedNetworks()
        sb.append("--- Saved Networks (${savedNetworks.size}) ---\n")
        savedNetworks.forEach { network ->
            sb.append("  • ${network.ssid}${if (network.isHidden) " (Hidden)" else ""}\n")
        }
        sb.append("\n")

        // Available networks
        val availableNetworks = scanNetworks()
        sb.append("--- Available Networks (${availableNetworks.size}) ---\n")
        availableNetworks.take(10).forEach { network ->
            sb.append("  • ${network.ssid} - ${getSignalStrength(network.level)}")
            sb.append(" (${network.level} dBm)${if (network.isSecured) " 🔒" else ""}\n")
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if location permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Data class for current WiFi connection
     */
    data class WifiConnectionInfo(
        val ssid: String,
        val bssid: String,
        val frequency: Int,
        val linkSpeed: Int,
        val rssi: Int,
        val ipAddress: String,
        val macAddress: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("ssid", ssid)
                put("bssid", bssid)
                put("frequency", frequency)
                put("linkSpeed", linkSpeed)
                put("rssi", rssi)
                put("ipAddress", ipAddress)
                put("macAddress", macAddress)
            }
        }
    }

    /**
     * Data class for scanned WiFi network
     */
    data class WifiNetworkInfo(
        val ssid: String,
        val bssid: String,
        val capabilities: String,
        val frequency: Int,
        val level: Int,
        val isSecured: Boolean
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("ssid", ssid)
                put("bssid", bssid)
                put("capabilities", capabilities)
                put("frequency", frequency)
                put("level", level)
                put("isSecured", isSecured)
            }
        }
    }

    /**
     * Data class for saved WiFi network
     */
    data class SavedWifiNetwork(
        val ssid: String,
        val networkId: Int,
        val isHidden: Boolean
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("ssid", ssid)
                put("networkId", networkId)
                put("isHidden", isHidden)
            }
        }
    }
}
