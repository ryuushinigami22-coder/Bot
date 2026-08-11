package com.sync.xxx.managers

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject

/**
 * DNDManager.kt
 * Control Do Not Disturb mode
 * Manage interruption filter and DND settings
 */
class DNDManager(private val context: Context) {

    private val TAG = "DNDManager"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Check if DND access is granted
     */
    fun hasDNDAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            false
        }
    }

    /**
     * Get current interruption filter
     */
    fun getCurrentFilter(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter
        } else {
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }
    }

    /**
     * Set interruption filter
     */
    fun setInterruptionFilter(filter: Int): Boolean {
        if (!hasDNDAccess()) {
            Log.e(TAG, "DND access not granted")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.setInterruptionFilter(filter)
                Log.d(TAG, "Interruption filter set to $filter")
                true
            } else {
                Log.e(TAG, "DND requires Android 6.0+")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting interruption filter", e)
            false
        }
    }

    /**
     * Enable DND (All mode - block all interruptions)
     */
    fun enableAll(): Boolean {
        return setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
    }

    /**
     * Enable DND (Priority mode - allow priority interruptions)
     */
    fun enablePriority(): Boolean {
        return setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    /**
     * Enable DND (Alarms only mode)
     */
    fun enableAlarmsOnly(): Boolean {
        return setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
    }

    /**
     * Disable DND (allow all interruptions)
     */
    fun disable(): Boolean {
        return setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    /**
     * Check if DND is enabled (any mode)
     */
    fun isEnabled(): Boolean {
        val filter = getCurrentFilter()
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
               filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    /**
     * Check if in All mode (block all)
     */
    fun isInAllMode(): Boolean {
        return getCurrentFilter() == NotificationManager.INTERRUPTION_FILTER_NONE
    }

    /**
     * Check if in Priority mode
     */
    fun isInPriorityMode(): Boolean {
        return getCurrentFilter() == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    /**
     * Check if in Alarms Only mode
     */
    fun isInAlarmsOnlyMode(): Boolean {
        return getCurrentFilter() == NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    /**
     * Get current filter name
     */
    fun getFilterName(): String {
        return when (getCurrentFilter()) {
            NotificationManager.INTERRUPTION_FILTER_NONE -> "All Blocked"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority Only"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms Only"
            NotificationManager.INTERRUPTION_FILTER_ALL -> "All Allowed"
            else -> "Unknown"
        }
    }

    /**
     * Export DND status as JSON
     */
    fun getDNDStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("hasDNDAccess", hasDNDAccess())
            put("isEnabled", isEnabled())
            put("currentFilter", getCurrentFilter())
            put("filterName", getFilterName())
            put("isInAllMode", isInAllMode())
            put("isInPriorityMode", isInPriorityMode())
            put("isInAlarmsOnlyMode", isInAlarmsOnlyMode())
        }
    }

    /**
     * Export DND info as text
     */
    fun exportDNDInfo(): String {
        val sb = StringBuilder()

        sb.append("Do Not Disturb\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Access Granted: ${if (hasDNDAccess()) "Yes" else "No"}\n")
        sb.append("DND Enabled: ${if (isEnabled()) "Yes" else "No"}\n")
        sb.append("Mode: ${getFilterName()}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Interruption filter constants
         */
        const val FILTER_ALL = NotificationManager.INTERRUPTION_FILTER_ALL
        const val FILTER_PRIORITY = NotificationManager.INTERRUPTION_FILTER_PRIORITY
        const val FILTER_NONE = NotificationManager.INTERRUPTION_FILTER_NONE
        const val FILTER_ALARMS = NotificationManager.INTERRUPTION_FILTER_ALARMS

        /**
         * Check if DND access is granted
         */
        fun hasDNDAccess(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else {
                false
            }
        }

        /**
         * Enable DND (All mode)
         */
        fun enableAll(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!notificationManager.isNotificationPolicyAccessGranted) {
                        return false
                    }
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Disable DND
         */
        fun disable(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!notificationManager.isNotificationPolicyAccessGranted) {
                        return false
                    }
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
