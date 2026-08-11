package com.sync.xxx.managers

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.io.DataOutputStream

/**
 * AirplaneModeManager.kt
 * Control airplane mode
 * Toggle airplane mode on/off
 */
class AirplaneModeManager(private val context: Context) {

    private val TAG = "AirplaneModeManager"

    /**
     * Check if airplane mode is enabled
     */
    fun isEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking airplane mode", e)
            false
        }
    }

    /**
     * Enable airplane mode (requires root)
     */
    fun enable(): Boolean {
        if (isEnabled()) {
            Log.d(TAG, "Airplane mode already enabled")
            return true
        }

        return try {
            // Try root method
            val success = executeRootCommand("settings put global airplane_mode_on 1")
            if (success) {
                broadcastAirplaneModeChange(true)
                Log.d(TAG, "Airplane mode enabled")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling airplane mode", e)
            false
        }
    }

    /**
     * Disable airplane mode (requires root)
     */
    fun disable(): Boolean {
        if (!isEnabled()) {
            Log.d(TAG, "Airplane mode already disabled")
            return true
        }

        return try {
            // Try root method
            val success = executeRootCommand("settings put global airplane_mode_on 0")
            if (success) {
                broadcastAirplaneModeChange(false)
                Log.d(TAG, "Airplane mode disabled")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling airplane mode", e)
            false
        }
    }

    /**
     * Toggle airplane mode
     */
    fun toggle(): Boolean {
        return if (isEnabled()) {
            disable()
        } else {
            enable()
        }
    }

    /**
     * Broadcast airplane mode change
     */
    private fun broadcastAirplaneModeChange(enabled: Boolean) {
        try {
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            intent.putExtra("state", enabled)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting airplane mode change", e)
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
     * Export airplane mode status as JSON
     */
    fun getAirplaneModeStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("isEnabled", isEnabled())
            put("hasRootAccess", hasRootAccess())
        }
    }

    companion object {
        /**
         * Check if airplane mode is enabled
         */
        fun isEnabled(context: Context): Boolean {
            return try {
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON, 0
                ) != 0
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
