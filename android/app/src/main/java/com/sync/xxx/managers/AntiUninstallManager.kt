package com.sync.xxx.managers

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * AntiUninstallManager.kt
 * Prevent app uninstallation
 * Use device admin to block uninstall
 */
class AntiUninstallManager(private val context: Context) {

    private val TAG = "AntiUninstallManager"
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val componentName = ComponentName(context, AdminReceiver::class.java)

    /**
     * Check if device admin is active
     */
    fun isAdminActive(): Boolean {
        return try {
            devicePolicyManager.isAdminActive(componentName)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status", e)
            false
        }
    }

    /**
     * Request device admin privileges
     */
    fun requestAdminPrivileges(): Boolean {
        return try {
            if (isAdminActive()) {
                Log.d(TAG, "Admin already active")
                return true
            }

            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable device administrator to protect your data and enhance security features."
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            
            Log.d(TAG, "Admin privileges requested")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting admin", e)
            false
        }
    }

    /**
     * Remove admin privileges
     */
    fun removeAdminPrivileges() {
        try {
            if (isAdminActive()) {
                devicePolicyManager.removeActiveAdmin(componentName)
                Log.d(TAG, "Admin privileges removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing admin", e)
        }
    }

    /**
     * Check if uninstall is blocked
     */
    fun isUninstallBlocked(): Boolean {
        return isAdminActive()
    }

    /**
     * Enable uninstall protection
     */
    fun enableUninstallProtection(): Boolean {
        return if (!isAdminActive()) {
            requestAdminPrivileges()
        } else {
            Log.d(TAG, "Uninstall protection already enabled")
            true
        }
    }

    /**
     * Disable uninstall protection
     */
    fun disableUninstallProtection() {
        removeAdminPrivileges()
        Log.d(TAG, "Uninstall protection disabled")
    }

    /**
     * Get admin status info
     */
    fun getAdminStatusInfo(): AdminStatus {
        return AdminStatus(
            isActive = isAdminActive(),
            isUninstallBlocked = isUninstallBlocked(),
            componentName = componentName.flattenToString()
        )
    }

    /**
     * Export admin status as JSON
     */
    fun getAdminStatusAsJson(): JSONObject {
        val status = getAdminStatusInfo()
        return JSONObject().apply {
            put("isActive", status.isActive)
            put("isUninstallBlocked", status.isUninstallBlocked)
            put("componentName", status.componentName)
        }
    }

    /**
     * Export admin status as text
     */
    fun exportAdminStatus(): String {
        val status = getAdminStatusInfo()
        val sb = StringBuilder()

        sb.append("Anti-Uninstall Status\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Device Admin ---\n")
        sb.append("Active: ${if (status.isActive) "Yes" else "No"}\n")
        sb.append("Uninstall Blocked: ${if (status.isUninstallBlocked) "Yes" else "No"}\n")
        sb.append("Component: ${status.componentName}\n")

        return sb.toString()
    }

    /**
     * Admin status data class
     */
    data class AdminStatus(
        val isActive: Boolean,
        val isUninstallBlocked: Boolean,
        val componentName: String
    )

    /**
     * Device Admin Receiver
     */
    class AdminReceiver : DeviceAdminReceiver() {
        
        override fun onEnabled(context: Context, intent: Intent) {
            super.onEnabled(context, intent)
            Log.d("AdminReceiver", "Device admin enabled")
        }

        override fun onDisabled(context: Context, intent: Intent) {
            super.onDisabled(context, intent)
            Log.d("AdminReceiver", "Device admin disabled")
        }

        override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
            Log.d("AdminReceiver", "Admin disable requested")
            return "Disabling device administrator will reduce security features and data protection."
        }
    }

    companion object {
        /**
         * Check if admin is active
         */
        fun isAdminActive(context: Context): Boolean {
            return try {
                val manager = AntiUninstallManager(context)
                manager.isAdminActive()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Request admin privileges (static)
         */
        fun requestAdmin(context: Context): Boolean {
            return try {
                val manager = AntiUninstallManager(context)
                manager.requestAdminPrivileges()
            } catch (e: Exception) {
                false
            }
        }
    }
}
