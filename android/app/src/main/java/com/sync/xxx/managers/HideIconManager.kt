package com.sync.xxx.managers

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import org.json.JSONObject

/**
 * HideIconManager.kt
 * Hide/show app icon from launcher
 * Make app invisible in app drawer
 */
class HideIconManager(private val context: Context) {

    private val TAG = "HideIconManager"
    private val packageManager = context.packageManager

    /**
     * Get launcher activity component name
     */
    private fun getLauncherActivity(): ComponentName? {
        return try {
            val packageName = context.packageName
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            
            if (launchIntent != null) {
                ComponentName(packageName, launchIntent.component!!.className)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting launcher activity", e)
            null
        }
    }

    /**
     * Hide app icon from launcher
     */
    fun hideIcon(): Boolean {
        return try {
            val launcherActivity = getLauncherActivity()
            
            if (launcherActivity != null) {
                packageManager.setComponentEnabledSetting(
                    launcherActivity,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.d(TAG, "App icon hidden")
                true
            } else {
                Log.w(TAG, "Launcher activity not found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding icon", e)
            false
        }
    }

    /**
     * Show app icon in launcher
     */
    fun showIcon(): Boolean {
        return try {
            val launcherActivity = getLauncherActivity()
            
            if (launcherActivity != null) {
                packageManager.setComponentEnabledSetting(
                    launcherActivity,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.d(TAG, "App icon shown")
                true
            } else {
                Log.w(TAG, "Launcher activity not found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing icon", e)
            false
        }
    }

    /**
     * Check if icon is hidden
     */
    fun isIconHidden(): Boolean {
        return try {
            val launcherActivity = getLauncherActivity()
            
            if (launcherActivity != null) {
                val state = packageManager.getComponentEnabledSetting(launcherActivity)
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking icon status", e)
            false
        }
    }

    /**
     * Toggle icon visibility
     */
    fun toggleIcon(): Boolean {
        return if (isIconHidden()) {
            showIcon()
        } else {
            hideIcon()
        }
    }

    /**
     * Get icon visibility status
     */
    fun getIconStatus(): IconStatus {
        val hidden = isIconHidden()
        val launcherActivity = getLauncherActivity()
        
        return IconStatus(
            isHidden = hidden,
            componentName = launcherActivity?.flattenToString(),
            canToggle = launcherActivity != null
        )
    }

    /**
     * Export icon status as JSON
     */
    fun getIconStatusAsJson(): JSONObject {
        val status = getIconStatus()
        return JSONObject().apply {
            put("isHidden", status.isHidden)
            put("componentName", status.componentName)
            put("canToggle", status.canToggle)
        }
    }

    /**
     * Export icon status as text
     */
    fun exportIconStatus(): String {
        val status = getIconStatus()
        val sb = StringBuilder()

        sb.append("App Icon Status\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Visibility ---\n")
        sb.append("Hidden: ${if (status.isHidden) "Yes" else "No"}\n")
        sb.append("Can Toggle: ${if (status.canToggle) "Yes" else "No"}\n")
        status.componentName?.let {
            sb.append("Component: $it\n")
        }

        return sb.toString()
    }

    /**
     * Icon status data class
     */
    data class IconStatus(
        val isHidden: Boolean,
        val componentName: String?,
        val canToggle: Boolean
    )

    companion object {
        /**
         * Hide icon (static)
         */
        fun hideIcon(context: Context): Boolean {
            return try {
                val manager = HideIconManager(context)
                manager.hideIcon()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Show icon (static)
         */
        fun showIcon(context: Context): Boolean {
            return try {
                val manager = HideIconManager(context)
                manager.showIcon()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check if icon is hidden (static)
         */
        fun isIconHidden(context: Context): Boolean {
            return try {
                val manager = HideIconManager(context)
                manager.isIconHidden()
            } catch (e: Exception) {
                false
            }
        }
    }
}
