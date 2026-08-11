package com.sync.xxx.managers

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import org.json.JSONObject

/**
 * FlashManager.kt
 * Control device flashlight/torch
 * Turn on/off flash LED
 */
class FlashManager(private val context: Context) {

    private val TAG = "FlashManager"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private var isFlashOn = false

    init {
        try {
            cameraId = getCameraIdWithFlash()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing flash manager", e)
        }
    }

    /**
     * Get camera ID that has flash
     */
    private fun getCameraIdWithFlash(): String? {
        return try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                if (hasFlash == true) {
                    return id
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting camera with flash", e)
            null
        }
    }

    /**
     * Check if device has flash
     */
    fun hasFlash(): Boolean {
        return cameraId != null
    }

    /**
     * Turn on flashlight
     */
    fun turnOn(): Boolean {
        if (!hasFlash()) {
            Log.e(TAG, "Device does not have flash")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId!!, true)
                isFlashOn = true
                Log.d(TAG, "Flashlight turned on")
                true
            } else {
                Log.e(TAG, "Flashlight requires Android 6.0+")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error turning on flashlight", e)
            false
        }
    }

    /**
     * Turn off flashlight
     */
    fun turnOff(): Boolean {
        if (!hasFlash()) {
            Log.e(TAG, "Device does not have flash")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId!!, false)
                isFlashOn = false
                Log.d(TAG, "Flashlight turned of")
                true
            } else {
                Log.e(TAG, "Flashlight requires Android 6.0+")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error turning off flashlight", e)
            false
        }
    }

    /**
     * Toggle flashlight
     */
    fun toggle(): Boolean {
        return if (isFlashOn) {
            turnOff()
        } else {
            turnOn()
        }
    }

    /**
     * Check if flashlight is on
     */
    fun isOn(): Boolean {
        return isFlashOn
    }

    /**
     * Turn on flashlight for duration (milliseconds)
     */
    fun turnOnForDuration(durationMs: Long): Boolean {
        if (!turnOn()) {
            return false
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            turnOff()
        }, durationMs)

        return true
    }

    /**
     * Flash light (blink once)
     */
    fun flash(durationMs: Long = 200): Boolean {
        return turnOnForDuration(durationMs)
    }

    /**
     * Flash SOS pattern
     */
    fun flashSOS(): Boolean {
        if (!hasFlash()) {
            return false
        }

        // SOS pattern: ... --- ... (3 short, 3 long, 3 short)
        val shortDuration = 200L
        val longDuration = 600L
        val pauseDuration = 200L

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            var delay = 0L

            // 3 short flashes
            repeat(3) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    turnOnForDuration(shortDuration)
                }, delay)
                delay += shortDuration + pauseDuration
            }

            // Pause between groups
            delay += 400L

            // 3 long flashes
            repeat(3) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    turnOnForDuration(longDuration)
                }, delay)
                delay += longDuration + pauseDuration
            }

            // Pause between groups
            delay += 400L

            // 3 short flashes
            repeat(3) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    turnOnForDuration(shortDuration)
                }, delay)
                delay += shortDuration + pauseDuration
            }
        }

        return true
    }

    /**
     * Export flash status as JSON
     */
    fun getFlashStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("hasFlash", hasFlash())
            put("isFlashOn", isOn())
            put("cameraId", cameraId)
        }
    }

    companion object {
        /**
         * Check if device has flash
         */
        fun hasFlash(context: Context): Boolean {
            return try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                for (id in cameraManager.cameraIdList) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    if (hasFlash == true) {
                        return true
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }
    }
}
