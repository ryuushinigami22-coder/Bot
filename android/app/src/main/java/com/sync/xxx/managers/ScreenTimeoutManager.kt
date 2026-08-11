package com.sync.xxx.managers

import android.content.Context
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

/**
 * ScreenTimeoutManager.kt
 * Control screen timeout duration
 * Get and set screen sleep timeout
 */
class ScreenTimeoutManager(private val context: Context) {

    private val TAG = "ScreenTimeoutManager"

    /**
     * Get current screen timeout (milliseconds)
     */
    fun getCurrentTimeout(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screen timeout", e)
            -1
        }
    }

    /**
     * Get current timeout in seconds
     */
    fun getCurrentTimeoutSeconds(): Int {
        val timeoutMs = getCurrentTimeout()
        return if (timeoutMs > 0) timeoutMs / 1000 else -1
    }

    /**
     * Set screen timeout (milliseconds)
     */
    fun setTimeout(timeoutMs: Int): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                timeoutMs
            )
            Log.d(TAG, "Screen timeout set to ${timeoutMs}ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting screen timeout", e)
            false
        }
    }

    /**
     * Set timeout in seconds
     */
    fun setTimeoutSeconds(seconds: Int): Boolean {
        return setTimeout(seconds * 1000)
    }

    /**
     * Set timeout to 15 seconds
     */
    fun setTimeout15Seconds(): Boolean = setTimeout(15000)

    /**
     * Set timeout to 30 seconds
     */
    fun setTimeout30Seconds(): Boolean = setTimeout(30000)

    /**
     * Set timeout to 1 minute
     */
    fun setTimeout1Minute(): Boolean = setTimeout(60000)

    /**
     * Set timeout to 2 minutes
     */
    fun setTimeout2Minutes(): Boolean = setTimeout(120000)

    /**
     * Set timeout to 5 minutes
     */
    fun setTimeout5Minutes(): Boolean = setTimeout(300000)

    /**
     * Set timeout to 10 minutes
     */
    fun setTimeout10Minutes(): Boolean = setTimeout(600000)

    /**
     * Set timeout to 30 minutes
     */
    fun setTimeout30Minutes(): Boolean = setTimeout(1800000)

    /**
     * Set timeout to never (maximum value)
     */
    fun setTimeoutNever(): Boolean = setTimeout(Int.MAX_VALUE)

    /**
     * Increase timeout by duration (milliseconds)
     */
    fun increaseTimeout(durationMs: Int): Boolean {
        val current = getCurrentTimeout()
        if (current < 0) return false
        return setTimeout(current + durationMs)
    }

    /**
     * Decrease timeout by duration (milliseconds)
     */
    fun decreaseTimeout(durationMs: Int): Boolean {
        val current = getCurrentTimeout()
        if (current < 0) return false
        val newTimeout = (current - durationMs).coerceAtLeast(15000)
        return setTimeout(newTimeout)
    }

    /**
     * Check if system can write settings
     */
    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Get timeout description
     */
    fun getTimeoutDescription(): String {
        val timeoutMs = getCurrentTimeout()
        if (timeoutMs < 0) return "Unknown"
        
        val seconds = timeoutMs / 1000
        return when {
            timeoutMs == Int.MAX_VALUE -> "Never"
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    /**
     * Export timeout status as JSON
     */
    fun getTimeoutStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("timeoutMs", getCurrentTimeout())
            put("timeoutSeconds", getCurrentTimeoutSeconds())
            put("timeoutDescription", getTimeoutDescription())
            put("canWriteSettings", canWriteSettings())
        }
    }

    /**
     * Export timeout info as text
     */
    fun exportTimeoutInfo(): String {
        val sb = StringBuilder()

        sb.append("Screen Timeout\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Timeout: ${getTimeoutDescription()}\n")
        sb.append("Milliseconds: ${getCurrentTimeout()}ms\n")
        sb.append("Seconds: ${getCurrentTimeoutSeconds()}s\n")
        sb.append("Can Write Settings: ${if (canWriteSettings()) "Yes" else "No"}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Timeout presets (milliseconds)
         */
        const val TIMEOUT_15_SECONDS = 15000
        const val TIMEOUT_30_SECONDS = 30000
        const val TIMEOUT_1_MINUTE = 60000
        const val TIMEOUT_2_MINUTES = 120000
        const val TIMEOUT_5_MINUTES = 300000
        const val TIMEOUT_10_MINUTES = 600000
        const val TIMEOUT_30_MINUTES = 1800000
        const val TIMEOUT_NEVER = Int.MAX_VALUE

        /**
         * Get current screen timeout
         */
        fun getCurrentTimeout(context: Context): Int {
            return try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT
                )
            } catch (e: Exception) {
                -1
            }
        }

        /**
         * Set screen timeout
         */
        fun setTimeout(context: Context, timeoutMs: Int): Boolean {
            return try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    timeoutMs
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
