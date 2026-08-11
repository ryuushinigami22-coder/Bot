package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * PermissionManager.kt
 * Manage runtime permissions
 * Check and request permissions
 */
class PermissionManager(private val context: Context) {

    private val TAG = "PermissionManager"

    /**
     * Check if permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return try {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permission", e)
            false
        }
    }

    /**
     * Check multiple permissions
     */
    fun arePermissionsGranted(permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(it) }
    }

    /**
     * Get denied permissions from list
     */
    fun getDeniedPermissions(permissions: List<String>): List<String> {
        return permissions.filter { !isPermissionGranted(it) }
    }

    /**
     * Get granted permissions from list
     */
    fun getGrantedPermissions(permissions: List<String>): List<String> {
        return permissions.filter { isPermissionGranted(it) }
    }

    /**
     * Check all dangerous permissions
     */
    fun getAllDangerousPermissions(): List<String> {
        return listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /**
     * Get permission status for all dangerous permissions
     */
    fun getAllPermissionStatuses(): Map<String, Boolean> {
        val permissions = getAllDangerousPermissions()
        return permissions.associateWith { isPermissionGranted(it) }
    }

    /**
     * Get permission summary
     */
    fun getPermissionSummary(): PermissionSummary {
        val allPermissions = getAllDangerousPermissions()
        val granted = getGrantedPermissions(allPermissions)
        val denied = getDeniedPermissions(allPermissions)
        
        return PermissionSummary(
            totalPermissions = allPermissions.size,
            grantedCount = granted.size,
            deniedCount = denied.size,
            grantedPermissions = granted,
            deniedPermissions = denied
        )
    }

    /**
     * Check specific permission categories
     */
    fun hasLocationPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
               isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun hasCameraPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.CAMERA)
    }

    fun hasMicrophonePermission(): Boolean {
        return isPermissionGranted(Manifest.permission.RECORD_AUDIO)
    }

    fun hasContactsPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_CONTACTS)
    }

    fun hasSmsPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_SMS) ||
               isPermissionGranted(Manifest.permission.SEND_SMS)
    }

    fun hasCallLogPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_CALL_LOG)
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            true // Storage permissions work differently on Android 11+
        } else {
            isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Export permission summary as JSON
     */
    fun getPermissionSummaryAsJson(): JSONObject {
        val summary = getPermissionSummary()
        return JSONObject().apply {
            put("totalPermissions", summary.totalPermissions)
            put("grantedCount", summary.grantedCount)
            put("deniedCount", summary.deniedCount)
            put("grantedPermissions", JSONArray(summary.grantedPermissions))
            put("deniedPermissions", JSONArray(summary.deniedPermissions))
        }
    }

    /**
     * Export permission summary as text
     */
    fun exportPermissionSummary(): String {
        val summary = getPermissionSummary()
        val sb = StringBuilder()

        sb.append("Permission Summary\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Overview ---\n")
        sb.append("Total Permissions: ${summary.totalPermissions}\n")
        sb.append("Granted: ${summary.grantedCount}\n")
        sb.append("Denied: ${summary.deniedCount}\n\n")

        if (summary.grantedPermissions.isNotEmpty()) {
            sb.append("--- Granted Permissions ---\n")
            summary.grantedPermissions.forEachIndexed { index, perm ->
                sb.append("${index + 1}. ${getPermissionName(perm)}\n")
            }
            sb.append("\n")
        }

        if (summary.deniedPermissions.isNotEmpty()) {
            sb.append("--- Denied Permissions ---\n")
            summary.deniedPermissions.forEachIndexed { index, perm ->
                sb.append("${index + 1}. ${getPermissionName(perm)}\n")
            }
        }

        return sb.toString()
    }

    /**
     * Get human-readable permission name
     */
    private fun getPermissionName(permission: String): String {
        return permission.substringAfterLast(".")
    }

    /**
     * Permission summary data class
     */
    data class PermissionSummary(
        val totalPermissions: Int,
        val grantedCount: Int,
        val deniedCount: Int,
        val grantedPermissions: List<String>,
        val deniedPermissions: List<String>
    )

    companion object {
        /**
         * Check if permission is granted (static)
         */
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return try {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Get permission summary (static)
         */
        fun getPermissionSummary(context: Context): PermissionSummary {
            val manager = PermissionManager(context)
            return manager.getPermissionSummary()
        }
    }
}
