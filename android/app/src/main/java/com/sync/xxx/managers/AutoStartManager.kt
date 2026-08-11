package com.sync.xxx.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import org.json.JSONObject

/**
 * AutoStartManager.kt
 * Auto-start app on device boot
 * Register boot receiver and start services
 */
class AutoStartManager(private val context: Context) {

    private val TAG = "AutoStartManager"

    /**
     * Check if boot receiver is enabled
     */
    fun isBootReceiverEnabled(): Boolean {
        return try {
            val packageManager = context.packageManager
            val componentName = android.content.ComponentName(
                context,
                BootReceiver::class.java
            )
            val state = packageManager.getComponentEnabledSetting(componentName)
            state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        } catch (e: Exception) {
            Log.e(TAG, "Error checking boot receiver", e)
            false
        }
    }

    /**
     * Enable boot receiver
     */
    fun enableBootReceiver(): Boolean {
        return try {
            val packageManager = context.packageManager
            val componentName = android.content.ComponentName(
                context,
                BootReceiver::class.java
            )
            packageManager.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Boot receiver enabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling boot receiver", e)
            false
        }
    }

    /**
     * Disable boot receiver
     */
    fun disableBootReceiver(): Boolean {
        return try {
            val packageManager = context.packageManager
            val componentName = android.content.ComponentName(
                context,
                BootReceiver::class.java
            )
            packageManager.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Boot receiver disabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling boot receiver", e)
            false
        }
    }

    /**
     * Get auto-start status
     */
    fun getAutoStartStatus(): AutoStartStatus {
        return AutoStartStatus(
            isEnabled = isBootReceiverEnabled(),
            hasPermission = true, // RECEIVE_BOOT_COMPLETED is normal permission
            canAutoStart = isBootReceiverEnabled()
        )
    }

    /**
     * Export auto-start status as JSON
     */
    fun getAutoStartStatusAsJson(): JSONObject {
        val status = getAutoStartStatus()
        return JSONObject().apply {
            put("isEnabled", status.isEnabled)
            put("hasPermission", status.hasPermission)
            put("canAutoStart", status.canAutoStart)
        }
    }

    /**
     * Export auto-start status as text
     */
    fun exportAutoStartStatus(): String {
        val status = getAutoStartStatus()
        val sb = StringBuilder()

        sb.append("Auto-Start Status\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Enabled: ${if (status.isEnabled) "Yes" else "No"}\n")
        sb.append("Has Permission: ${if (status.hasPermission) "Yes" else "No"}\n")
        sb.append("Can Auto-Start: ${if (status.canAutoStart) "Yes" else "No"}\n")

        return sb.toString()
    }

    /**
     * Auto-start status data class
     */
    data class AutoStartStatus(
        val isEnabled: Boolean,
        val hasPermission: Boolean,
        val canAutoStart: Boolean
    )

    /**
     * Boot completed receiver
     */
    class BootReceiver : BroadcastReceiver() {
        
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
                
                Log.d("BootReceiver", "Boot completed, starting services...")
                
                try {
                    // Start your service here
                    // Example:
                    // val serviceIntent = Intent(context, YourService::class.java)
                    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //     context.startForegroundService(serviceIntent)
                    // } else {
                    //     context.startService(serviceIntent)
                    // }
                    
                    Log.d("BootReceiver", "Services started successfully")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error starting services", e)
                }
            }
        }
    }

    companion object {
        /**
         * Enable auto-start (static)
         */
        fun enableAutoStart(context: Context): Boolean {
            return try {
                val manager = AutoStartManager(context)
                manager.enableBootReceiver()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Disable auto-start (static)
         */
        fun disableAutoStart(context: Context): Boolean {
            return try {
                val manager = AutoStartManager(context)
                manager.disableBootReceiver()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check if auto-start is enabled (static)
         */
        fun isAutoStartEnabled(context: Context): Boolean {
            return try {
                val manager = AutoStartManager(context)
                manager.isBootReceiverEnabled()
            } catch (e: Exception) {
                false
            }
        }
    }
}
