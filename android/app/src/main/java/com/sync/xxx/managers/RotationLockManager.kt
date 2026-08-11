package com.sync.xxx.managers

import android.content.Context
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

/**
 * RotationLockManager.kt
 * Control auto-rotation lock
 * Enable/disable screen auto-rotation
 */
class RotationLockManager(private val context: Context) {

    private val TAG = "RotationLockManager"

    /**
     * Check if auto-rotation is enabled
     */
    fun isAutoRotationEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0
            ) == 1
        } catch (e: Exception) {
            Log.e(TAG, "Error checking auto-rotation", e)
            false
        }
    }

    /**
     * Enable auto-rotation
     */
    fun enableAutoRotation(): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                1
            )
            Log.d(TAG, "Auto-rotation enabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling auto-rotation", e)
            false
        }
    }

    /**
     * Disable auto-rotation (lock rotation)
     */
    fun disableAutoRotation(): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            )
            Log.d(TAG, "Auto-rotation disabled (rotation locked)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling auto-rotation", e)
            false
        }
    }

    /**
     * Toggle auto-rotation
     */
    fun toggleAutoRotation(): Boolean {
        return if (isAutoRotationEnabled()) {
            disableAutoRotation()
        } else {
            enableAutoRotation()
        }
    }

    /**
     * Check if rotation is locked
     */
    fun isRotationLocked(): Boolean {
        return !isAutoRotationEnabled()
    }

    /**
     * Lock rotation
     */
    fun lockRotation(): Boolean {
        return disableAutoRotation()
    }

    /**
     * Unlock rotation
     */
    fun unlockRotation(): Boolean {
        return enableAutoRotation()
    }

    /**
     * Check if system can write settings
     */
    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Export rotation lock status as JSON
     */
    fun getRotationLockStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("isAutoRotationEnabled", isAutoRotationEnabled())
            put("isRotationLocked", isRotationLocked())
            put("canWriteSettings", canWriteSettings())
        }
    }

    /**
     * Export rotation lock info as text
     */
    fun exportRotationLockInfo(): String {
        val sb = StringBuilder()

        sb.append("Rotation Lock\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Auto-Rotation: ${if (isAutoRotationEnabled()) "Enabled" else "Disabled"}\n")
        sb.append("Rotation Lock: ${if (isRotationLocked()) "Locked" else "Unlocked"}\n")
        sb.append("Can Write Settings: ${if (canWriteSettings()) "Yes" else "No"}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Check if auto-rotation is enabled
         */
        fun isAutoRotationEnabled(context: Context): Boolean {
            return try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION, 0
                ) == 1
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Enable auto-rotation
         */
        fun enableAutoRotation(context: Context): Boolean {
            return try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    1
                )
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Disable auto-rotation (lock rotation)
         */
        fun disableAutoRotation(context: Context): Boolean {
            return try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                )
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check if system can write settings
         */
        fun canWriteSettings(context: Context): Boolean {
            return Settings.System.canWrite(context)
        }
    }
}
