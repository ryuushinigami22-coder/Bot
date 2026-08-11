package com.sync.xxx.managers

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import android.util.Log
import org.json.JSONObject
import java.io.DataOutputStream
import java.lang.reflect.Method

/**
 * MobileDataManager.kt
 * Control mobile data on/off
 * Requires root access or system permissions
 */
class MobileDataManager(private val context: Context) {

    private val TAG = "MobileDataManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /**
     * Check if mobile data is enabled
     */
    fun isEnabled(): Boolean {
        return try {
            val method: Method = connectivityManager.javaClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true
            method.invoke(connectivityManager) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "Error checking mobile data status", e)
            false
        }
    }

    /**
     * Enable mobile data (requires root)
     */
    fun enable(): Boolean {
        if (isEnabled()) {
            Log.d(TAG, "Mobile data already enabled")
            return true
        }

        return try {
            // Try reflection method first
            val success = setMobileDataViaReflection(true)
            if (success) {
                Log.d(TAG, "Mobile data enabled via reflection")
                return true
            }

            // Fallback to root method
            val rootSuccess = executeRootCommand("svc data enable")
            if (rootSuccess) {
                Log.d(TAG, "Mobile data enabled via root")
            }
            rootSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling mobile data", e)
            false
        }
    }

    /**
     * Disable mobile data (requires root)
     */
    fun disable(): Boolean {
        if (!isEnabled()) {
            Log.d(TAG, "Mobile data already disabled")
            return true
        }

        return try {
            // Try reflection method first
            val success = setMobileDataViaReflection(false)
            if (success) {
                Log.d(TAG, "Mobile data disabled via reflection")
                return true
            }

            // Fallback to root method
            val rootSuccess = executeRootCommand("svc data disable")
            if (rootSuccess) {
                Log.d(TAG, "Mobile data disabled via root")
            }
            rootSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling mobile data", e)
            false
        }
    }

    /**
     * Toggle mobile data
     */
    fun toggle(): Boolean {
        return if (isEnabled()) {
            disable()
        } else {
            enable()
        }
    }

    /**
     * Set mobile data via reflection
     */
    private fun setMobileDataViaReflection(enabled: Boolean): Boolean {
        return try {
            val method: Method = connectivityManager.javaClass.getDeclaredMethod(
                "setMobileDataEnabled",
                Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(connectivityManager, enabled)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Reflection method failed", e)
            false
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
     * Get data network type
     */
    fun getNetworkType(): String {
        return try {
            when (telephonyManager.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE -> "2G EDGE"
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G GPRS"
                TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            "Unknown"
        }
    }

    /**
     * Export mobile data status as JSON
     */
    fun getMobileDataStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("isEnabled", isEnabled())
            put("networkType", getNetworkType())
            put("hasRootAccess", hasRootAccess())
        }
    }

    /**
     * Export mobile data info as text
     */
    fun exportMobileDataInfo(): String {
        val sb = StringBuilder()

        sb.append("Mobile Data\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Mobile Data: ${if (isEnabled()) "Enabled" else "Disabled"}\n")
        sb.append("Network Type: ${getNetworkType()}\n")
        sb.append("Root Access: ${if (hasRootAccess()) "Available" else "Not Available"}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Check if mobile data is enabled
         */
        fun isEnabled(context: Context): Boolean {
            return try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val method: Method = connectivityManager.javaClass.getDeclaredMethod("getMobileDataEnabled")
                method.isAccessible = true
                method.invoke(connectivityManager) as Boolean
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
