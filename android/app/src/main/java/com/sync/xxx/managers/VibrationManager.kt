package com.sync.xxx.managers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import org.json.JSONObject

/**
 * VibrationManager.kt
 * Control device vibration
 * Single vibration, patterns, and effects
 */
class VibrationManager(private val context: Context) {

    private val TAG = "VibrationManager"
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Check if device has vibrator
     */
    fun hasVibrator(): Boolean {
        return vibrator.hasVibrator()
    }

    /**
     * Vibrate for duration (milliseconds)
     */
    fun vibrate(durationMs: Long): Boolean {
        if (!hasVibrator()) {
            Log.e(TAG, "Device does not have vibrator")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
            Log.d(TAG, "Vibrated for ${durationMs}ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating", e)
            false
        }
    }

    /**
     * Vibrate with pattern
     * Pattern: array of [delay, vibrate, delay, vibrate, ...]
     * Example: [0, 500, 200, 500] = vibrate 500ms, pause 200ms, vibrate 500ms
     */
    fun vibratePattern(pattern: LongArray, repeat: Int = -1): Boolean {
        if (!hasVibrator()) {
            Log.e(TAG, "Device does not have vibrator")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, repeat)
            }
            Log.d(TAG, "Vibrated with pattern")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating with pattern", e)
            false
        }
    }

    /**
     * Cancel vibration
     */
    fun cancel(): Boolean {
        return try {
            vibrator.cancel()
            Log.d(TAG, "Vibration cancelled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration", e)
            false
        }
    }

    /**
     * Short vibration (100ms)
     */
    fun vibrateShort(): Boolean {
        return vibrate(100)
    }

    /**
     * Medium vibration (250ms)
     */
    fun vibrateMedium(): Boolean {
        return vibrate(250)
    }

    /**
     * Long vibration (500ms)
     */
    fun vibrateLong(): Boolean {
        return vibrate(500)
    }

    /**
     * Double tap vibration
     */
    fun vibrateDoubleTap(): Boolean {
        return vibratePattern(longArrayOf(0, 100, 100, 100))
    }

    /**
     * Triple tap vibration
     */
    fun vibrateTripleTap(): Boolean {
        return vibratePattern(longArrayOf(0, 100, 100, 100, 100, 100))
    }

    /**
     * Heartbeat vibration pattern
     */
    fun vibrateHeartbeat(): Boolean {
        return vibratePattern(longArrayOf(0, 100, 100, 100, 500, 100, 100, 100), -1)
    }

    /**
     * SOS vibration pattern
     */
    fun vibrateSOS(): Boolean {
        // SOS: ... --- ... (3 short, 3 long, 3 short)
        return vibratePattern(
            longArrayOf(
                0, 200, 200, 200, 200, 200, // 3 short
                400,
                600, 200, 600, 200, 600, // 3 long
                400,
                200, 200, 200, 200, 200 // 3 short
            )
        )
    }

    /**
     * Notification vibration
     */
    fun vibrateNotification(): Boolean {
        return vibratePattern(longArrayOf(0, 100, 50, 100))
    }

    /**
     * Alert vibration (continuous)
     */
    fun vibrateAlert(): Boolean {
        return vibratePattern(longArrayOf(0, 500, 500), 0)
    }

    /**
     * Success vibration
     */
    fun vibrateSuccess(): Boolean {
        return vibratePattern(longArrayOf(0, 50, 50, 50))
    }

    /**
     * Error vibration
     */
    fun vibrateError(): Boolean {
        return vibratePattern(longArrayOf(0, 300, 100, 300))
    }

    /**
     * Check if vibrator has amplitude control
     */
    fun hasAmplitudeControl(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }
    }

    /**
     * Vibrate with amplitude (0-255)
     * Requires Android 8.0+
     */
    fun vibrateWithAmplitude(durationMs: Long, amplitude: Int): Boolean {
        if (!hasVibrator()) {
            Log.e(TAG, "Device does not have vibrator")
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "Amplitude control requires Android 8.0+")
            return vibrate(durationMs)
        }

        return try {
            val clampedAmplitude = amplitude.coerceIn(0, 255)
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmplitude))
            Log.d(TAG, "Vibrated for ${durationMs}ms with amplitude $clampedAmplitude")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating with amplitude", e)
            false
        }
    }

    /**
     * Export vibration status as JSON
     */
    fun getVibrationStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("hasVibrator", hasVibrator())
            put("hasAmplitudeControl", hasAmplitudeControl())
        }
    }

    companion object {
        /**
         * Vibration effect constants (Android 8.0+)
         */
        const val DEFAULT_AMPLITUDE = VibrationEffect.DEFAULT_AMPLITUDE

        /**
         * Check if device has vibrator
         */
        fun hasVibrator(context: Context): Boolean {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            return vibrator.hasVibrator()
        }

        /**
         * Quick vibrate
         */
        fun vibrate(context: Context, durationMs: Long): Boolean {
            return try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
