package com.sync.xxx.managers

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import org.json.JSONObject

/**
 * BrightnessManager.kt
 * Control screen brightness programmatically
 * Get and set brightness levels
 */
class BrightnessManager(private val context: Context) {

    private val TAG = "BrightnessManager"

    /**
     * Get current screen brightness (0-255)
     */
    fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting brightness", e)
            -1
        }
    }

    /**
     * Get current brightness percentage (0-100)
     */
    fun getCurrentBrightnessPercentage(): Int {
        val brightness = getCurrentBrightness()
        return if (brightness >= 0) {
            (brightness * 100 / 255)
        } else {
            -1
        }
    }

    /**
     * Set screen brightness (0-255)
     */
    fun setBrightness(brightness: Int): Boolean {
        return try {
            val clampedBrightness = brightness.coerceIn(0, 255)
            
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clampedBrightness
            )
            
            Log.d(TAG, "Brightness set to $clampedBrightness")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting brightness", e)
            false
        }
    }

    /**
     * Set screen brightness by percentage (0-100)
     */
    fun setBrightnessByPercentage(percentage: Int): Boolean {
        val clampedPercentage = percentage.coerceIn(0, 100)
        val brightness = (clampedPercentage * 255 / 100)
        return setBrightness(brightness)
    }

    /**
     * Increase brightness
     */
    fun increaseBrightness(step: Int = 25): Boolean {
        val currentBrightness = getCurrentBrightness()
        if (currentBrightness < 0) return false
        
        val newBrightness = (currentBrightness + step).coerceAtMost(255)
        return setBrightness(newBrightness)
    }

    /**
     * Decrease brightness
     */
    fun decreaseBrightness(step: Int = 25): Boolean {
        val currentBrightness = getCurrentBrightness()
        if (currentBrightness < 0) return false
        
        val newBrightness = (currentBrightness - step).coerceAtLeast(0)
        return setBrightness(newBrightness)
    }

    /**
     * Set brightness to maximum
     */
    fun setMaxBrightness(): Boolean {
        return setBrightness(255)
    }

    /**
     * Set brightness to minimum
     */
    fun setMinBrightness(): Boolean {
        return setBrightness(0)
    }

    /**
     * Check if auto brightness is enabled
     */
    fun isAutoBrightnessEnabled(): Boolean {
        return try {
            val mode = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
            mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Exception) {
            Log.e(TAG, "Error checking auto brightness", e)
            false
        }
    }

    /**
     * Enable auto brightness
     */
    fun enableAutoBrightness(): Boolean {
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
    fun disableAutoBrightness(): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Log.d(TAG, "Auto brightness disabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling auto brightness", e)
            false
        }
    }

    /**
     * Check if system can write settings
     */
    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Get brightness status description
     */
    fun getBrightnessDescription(): String {
        val brightness = getCurrentBrightnessPercentage()
        return when {
            brightness < 0 -> "Unknown"
            brightness == 0 -> "Of"
            brightness <= 20 -> "Very Low"
            brightness <= 40 -> "Low"
            brightness <= 60 -> "Medium"
            brightness <= 80 -> "High"
            else -> "Very High"
        }
    }

    /**
     * Export brightness status as JSON
     */
    fun getBrightnessStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("brightness", getCurrentBrightness())
            put("brightnessPercentage", getCurrentBrightnessPercentage())
            put("brightnessDescription", getBrightnessDescription())
            put("isAutoBrightnessEnabled", isAutoBrightnessEnabled())
            put("canWriteSettings", canWriteSettings())
        }
    }

    /**
     * Export brightness info as text
     */
    fun exportBrightnessInfo(): String {
        val sb = StringBuilder()

        sb.append("Brightness Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Brightness: ${getCurrentBrightness()}/255\n")
        sb.append("Percentage: ${getCurrentBrightnessPercentage()}%\n")
        sb.append("Level: ${getBrightnessDescription()}\n")
        sb.append("Auto Brightness: ${if (isAutoBrightnessEnabled()) "Enabled" else "Disabled"}\n")
        sb.append("Can Write Settings: ${if (canWriteSettings()) "Yes" else "No"}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Brightness mode constants
         */
        const val BRIGHTNESS_MODE_MANUAL = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        const val BRIGHTNESS_MODE_AUTOMATIC = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

        /**
         * Get current brightness
         */
        fun getCurrentBrightness(context: Context): Int {
            return try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
            } catch (e: Exception) {
                -1
            }
        }

        /**
         * Set brightness
         */
        fun setBrightness(context: Context, brightness: Int): Boolean {
            return try {
                val clampedBrightness = brightness.coerceIn(0, 255)
                
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    clampedBrightness
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
