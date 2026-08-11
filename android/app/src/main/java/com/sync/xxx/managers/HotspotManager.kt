package com.sync.xxx.managers

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.DataOutputStream
import java.lang.reflect.Method

/**
 * HotspotManager.kt
 * Control WiFi hotspot (portable WiFi access point)
 * Enable/disable hotspot, configure SSID and password
 */
class HotspotManager(private val context: Context) {

    private val TAG = "HotspotManager"
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * Check if hotspot is enabled
     */
    fun isEnabled(): Boolean {
        return try {
            val method: Method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "Error checking hotspot status", e)
            false
        }
    }

    /**
     * Enable hotspot
     */
    fun enable(): Boolean {
        if (isEnabled()) {
            Log.d(TAG, "Hotspot already enabled")
            return true
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ requires different approach
                Log.w(TAG, "Hotspot control requires system permissions on Android 8.0+")
                enableViaRoot()
            } else {
                // Try reflection for older Android versions
                enableViaReflection()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling hotspot", e)
            false
        }
    }

    /**
     * Disable hotspot
     */
    fun disable(): Boolean {
        if (!isEnabled()) {
            Log.d(TAG, "Hotspot already disabled")
            return true
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ requires different approach
                Log.w(TAG, "Hotspot control requires system permissions on Android 8.0+")
                disableViaRoot()
            } else {
                // Try reflection for older Android versions
                disableViaReflection()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling hotspot", e)
            false
        }
    }

    /**
     * Toggle hotspot
     */
    fun toggle(): Boolean {
        return if (isEnabled()) {
            disable()
        } else {
            enable()
        }
    }

    /**
     * Enable hotspot via reflection (Android < 8.0)
     */
    private fun enableViaReflection(): Boolean {
        return try {
            // Disable WiFi first
            if (wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = false
                Thread.sleep(500)
            }

            val method: Method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            
            val config = getWifiApConfiguration()
            val result = method.invoke(wifiManager, config, true) as Boolean
            
            if (result) {
                Log.d(TAG, "Hotspot enabled via reflection")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Reflection method failed", e)
            false
        }
    }

    /**
     * Disable hotspot via reflection (Android < 8.0)
     */
    private fun disableViaReflection(): Boolean {
        return try {
            val method: Method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            
            val result = method.invoke(wifiManager, null, false) as Boolean
            
            if (result) {
                Log.d(TAG, "Hotspot disabled via reflection")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Reflection method failed", e)
            false
        }
    }

    /**
     * Enable hotspot via root (Android 8.0+)
     */
    private fun enableViaRoot(): Boolean {
        return executeRootCommand("svc wifi enable_softap")
    }

    /**
     * Disable hotspot via root (Android 8.0+)
     */
    private fun disableViaRoot(): Boolean {
        return executeRootCommand("svc wifi disable_softap")
    }

    /**
     * Get WiFi AP configuration
     */
    @Suppress("DEPRECATION")
    private fun getWifiApConfiguration(): WifiConfiguration? {
        return try {
            val method: Method = wifiManager.javaClass.getDeclaredMethod("getWifiApConfiguration")
            method.isAccessible = true
            method.invoke(wifiManager) as WifiConfiguration?
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi AP configuration", e)
            null
        }
    }

    /**
     * Set WiFi AP configuration
     */
    @Suppress("DEPRECATION")
    fun setHotspotConfig(ssid: String, password: String): Boolean {
        return try {
            val config = WifiConfiguration()
            config.SSID = ssid
            config.preSharedKey = password
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)

            val method: Method = wifiManager.javaClass.getMethod(
                "setWifiApConfiguration",
                WifiConfiguration::class.java
            )
            method.isAccessible = true
            
            val result = method.invoke(wifiManager, config) as Boolean
            
            if (result) {
                Log.d(TAG, "Hotspot configuration updated: SSID=$ssid")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error setting hotspot configuration", e)
            false
        }
    }

    /**
     * Get hotspot SSID
     */
    @Suppress("DEPRECATION")
    fun getHotspotSSID(): String? {
        return try {
            val config = getWifiApConfiguration()
            config?.SSID
        } catch (e: Exception) {
            Log.e(TAG, "Error getting hotspot SSID", e)
            null
        }
    }

    /**
     * Execute root command
     */
    private fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root command", e)
            false
        }
    }

    /**
     * Check if device has root access
     */
    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            
            outputStream.writeBytes("id\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Export hotspot status as JSON
     */
    fun getHotspotStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("isEnabled", isEnabled())
            put("ssid", getHotspotSSID())
            put("hasRootAccess", hasRootAccess())
            put("androidVersion", Build.VERSION.SDK_INT)
            put("requiresRoot", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        }
    }

    /**
     * Export hotspot info as text
     */
    fun exportHotspotInfo(): String {
        val sb = StringBuilder()

        sb.append("WiFi Hotspot\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Hotspot: ${if (isEnabled()) "Enabled" else "Disabled"}\n")
        sb.append("SSID: ${getHotspotSSID() ?: "Not configured"}\n")
        sb.append("Android Version: ${Build.VERSION.SDK_INT}\n")
        sb.append("Root Access: ${if (hasRootAccess()) "Available" else "Not Available"}\n")
        sb.append("Requires Root: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "Yes (Android 8.0+)" else "No"}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Check if hotspot is enabled
         */
        fun isEnabled(context: Context): Boolean {
            return try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val method: Method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
                method.isAccessible = true
                method.invoke(wifiManager) as Boolean
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check if device has root access
         */
        fun hasRootAccess(): Boolean {
            return try {
                val process = Runtime.getRuntime().exec("su")
                val outputStream = DataOutputStream(process.outputStream)
                
                outputStream.writeBytes("id\n")
                outputStream.writeBytes("exit\n")
                outputStream.flush()
                
                val exitCode = process.waitFor()
                exitCode == 0
            } catch (e: Exception) {
                false
            }
        }
    }
}
