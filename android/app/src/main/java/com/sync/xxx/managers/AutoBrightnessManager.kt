package com.sync.xxx.managers

import android.content.Context
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

/**
 * AutoBrightnessManager.kt
 * Control automatic brightness mode
 * Enable/disable adaptive brightness
 */
class AutoBrightnessManager(private val context: Context) {

    private val TAG = "AutoBrightnessManager"

    /**
     * Check if auto brightness is enabled
     */
    fun isEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Exception) {
            Log.e(TAG, "Error checking auto brightness", e)
            false
        }
    }

    /**
     * Enable auto brightness
     */
    fun enable(): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
            Log.d(TAG, "Auto brightness enabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling auto brightness", e)
            false
        }
    }

    /**
     * Disable auto brightness (use manual mode)
     */
    fun disable(): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Log.d(TAG, "Auto brightness disabled (manual mode)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling auto brightness", e)
            false
        }
    }

    /**
     * Toggle auto brightness
     */
    fun toggle(): Boolean {
        return if (isEnabled()) {
            disable()
        } else {
            enable()
        }
    }

    /**
     * Get brightness mode
     */
    fun getBrightnessMode(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting brightness mode", e)
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        }
    }

    /**
     * Get brightness mode name
     */
    fun getBrightnessModeName(): String {
        return when (getBrightnessMode()) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC -> "Automatic"
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL -> "Manual"
            else -> "Unknown"
        }
    }

    /**
     * Check if system can write settings
     */
    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Export auto brightness status as JSON
     */
    fun getAutoBrightnessStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("isEnabled", isEnabled())
            put("brightnessMode", getBrightnessMode())
            put("brightnessModeName", getBrightnessModeName())
            put("canWriteSettings", canWriteSettings())
        }
    }

    /**
     * Export auto brightness info as text
     */
    fun exportAutoBrightnessInfo(): String {
        val sb = StringBuilder()

        sb.append("Auto Brightness\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Auto Brightness: ${if (isEnabled()) "Enabled" else "Disabled"}\n")
        sb.append("Mode: ${getBrightnessModeName()}\n")
        sb.append("Can Write Settings: ${if (canWriteSettings()) "Yes" else "No"}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Brightness mode constants
         */
        const val MODE_MANUAL = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        const val MODE_AUTOMATIC = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

        /**
         * Check if auto brightness is enabled
         */
        fun isEnabled(context: Context): Boolean {
            return try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE
                ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Enable auto brightness
         */
        fun enable(context: Context): Boolean {
            return try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                )
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Disable auto brightness
         */
        fun disable(context: Context): Boolean {
            return try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
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
