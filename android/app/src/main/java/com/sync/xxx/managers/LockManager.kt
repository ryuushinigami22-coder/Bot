package com.sync.xxx.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import org.json.JSONObject

/**
 * LockManager.kt
 * Lock device screen programmatically
 * Requires Device Admin permissions
 */
class LockManager(private val context: Context) {

    private val TAG = "LockManager"
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /**
     * Check if device admin is enabled
     */
    fun isDeviceAdminEnabled(adminComponent: ComponentName): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    /**
     * Lock device screen immediately
     */
    fun lockScreen(adminComponent: ComponentName): Boolean {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                Log.e(TAG, "Device admin not enabled")
                return false
            }

            devicePolicyManager.lockNow()
            Log.d(TAG, "Screen locked successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error locking screen", e)
            false
        }
    }

    /**
     * Check if screen is currently on
     */
    fun isScreenOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }

    /**
     * Set password quality requirement
     */
    fun setPasswordQuality(adminComponent: ComponentName, quality: Int): Boolean {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                Log.e(TAG, "Device admin not enabled")
                return false
            }

            devicePolicyManager.setPasswordQuality(adminComponent, quality)
            Log.d(TAG, "Password quality set successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting password quality", e)
            false
        }
    }

    /**
     * Set minimum password length
     */
    fun setPasswordMinLength(adminComponent: ComponentName, length: Int): Boolean {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                Log.e(TAG, "Device admin not enabled")
                return false
            }

            devicePolicyManager.setPasswordMinimumLength(adminComponent, length)
            Log.d(TAG, "Password min length set successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting password min length", e)
            false
        }
    }

    /**
     * Set maximum time to lock (milliseconds)
     */
    fun setMaxTimeToLock(adminComponent: ComponentName, timeMs: Long): Boolean {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                Log.e(TAG, "Device admin not enabled")
                return false
            }

            devicePolicyManager.setMaximumTimeToLock(adminComponent, timeMs)
            Log.d(TAG, "Max time to lock set successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting max time to lock", e)
            false
        }
    }

    /**
     * Reset password (requires device admin)
     */
    fun resetPassword(adminComponent: ComponentName, password: String, flags: Int = 0): Boolean {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                Log.e(TAG, "Device admin not enabled")
                return false
            }

            @Suppress("DEPRECATION")
            val result = devicePolicyManager.resetPassword(password, flags)
            if (result) {
                Log.d(TAG, "Password reset successfully")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting password", e)
            false
        }
    }

    /**
     * Check if password sufficient
     */
    fun isPasswordSufficient(adminComponent: ComponentName): Boolean {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                return false
            }

            devicePolicyManager.isActivePasswordSufficient
        } catch (e: Exception) {
            Log.e(TAG, "Error checking password", e)
            false
        }
    }

    /**
     * Get current failed password attempts
     */
    fun getCurrentFailedPasswordAttempts(adminComponent: ComponentName): Int {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                return -1
            }

            devicePolicyManager.currentFailedPasswordAttempts
        } catch (e: Exception) {
            Log.e(TAG, "Error getting failed password attempts", e)
            -1
        }
    }

    /**
     * Set maximum failed passwords for wipe
     */
    fun setMaximumFailedPasswordsForWipe(adminComponent: ComponentName, num: Int): Boolean {
        return try {
            if (!isDeviceAdminEnabled(adminComponent)) {
                Log.e(TAG, "Device admin not enabled")
                return false
            }

            devicePolicyManager.setMaximumFailedPasswordsForWipe(adminComponent, num)
            Log.d(TAG, "Max failed passwords set successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting max failed passwords", e)
            false
        }
    }

    /**
     * Export lock status as JSON
     */
    fun getLockStatusAsJson(adminComponent: ComponentName): JSONObject {
        return JSONObject().apply {
            put("isDeviceAdminEnabled", isDeviceAdminEnabled(adminComponent))
            put("isScreenOn", isScreenOn())
            put("isPasswordSufficient", isPasswordSufficient(adminComponent))
            put("failedPasswordAttempts", getCurrentFailedPasswordAttempts(adminComponent))
        }
    }

    companion object {
        /**
         * Password quality constants
         */
        const val PASSWORD_QUALITY_UNSPECIFIED = DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED
        const val PASSWORD_QUALITY_SOMETHING = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
        const val PASSWORD_QUALITY_NUMERIC = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC
        const val PASSWORD_QUALITY_NUMERIC_COMPLEX = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX
        const val PASSWORD_QUALITY_ALPHABETIC = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
        const val PASSWORD_QUALITY_ALPHANUMERIC = DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
        const val PASSWORD_QUALITY_COMPLEX = DevicePolicyManager.PASSWORD_QUALITY_COMPLEX

        /**
         * Check if device admin is enabled
         */
        fun isDeviceAdminEnabled(context: Context, adminComponent: ComponentName): Boolean {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return devicePolicyManager.isAdminActive(adminComponent)
        }
    }
}
