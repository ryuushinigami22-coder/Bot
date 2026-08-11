package com.sync.xxx.managers

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import org.json.JSONObject

/**
 * OrientationManager.kt
 * Control screen orientation programmatically
 * Lock/unlock orientation, detect current orientation
 */
class OrientationManager(private val context: Context) {

    private val TAG = "OrientationManager"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Get current screen orientation
     */
    fun getCurrentOrientation(): Int {
        return context.resources.configuration.orientation
    }

    /**
     * Check if current orientation is portrait
     */
    fun isPortrait(): Boolean {
        return getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT
    }

    /**
     * Check if current orientation is landscape
     */
    fun isLandscape(): Boolean {
        return getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * Get current rotation (0, 90, 180, 270 degrees)
     */
    fun getCurrentRotation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    /**
     * Set orientation (requires Activity context)
     */
    fun setOrientation(activity: Activity, orientation: Int): Boolean {
        return try {
            activity.requestedOrientation = orientation
            Log.d(TAG, "Orientation set to $orientation")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting orientation", e)
            false
        }
    }

    /**
     * Lock to portrait mode
     */
    fun lockPortrait(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    /**
     * Lock to landscape mode
     */
    fun lockLandscape(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    /**
     * Lock to reverse portrait mode
     */
    fun lockReversePortrait(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
    }

    /**
     * Lock to reverse landscape mode
     */
    fun lockReverseLandscape(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
    }

    /**
     * Lock to sensor portrait (portrait or reverse portrait based on sensor)
     */
    fun lockSensorPortrait(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
    }

    /**
     * Lock to sensor landscape (landscape or reverse landscape based on sensor)
     */
    fun lockSensorLandscape(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    }

    /**
     * Lock to current orientation
     */
    fun lockCurrent(activity: Activity): Boolean {
        val currentRotation = windowManager.defaultDisplay.rotation
        val orientation = when (currentRotation) {
            Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        return setOrientation(activity, orientation)
    }

    /**
     * Unlock orientation (auto-rotate based on sensor)
     */
    fun unlock(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_SENSOR)
    }

    /**
     * Set orientation to unspecified (system default)
     */
    fun setUnspecified(activity: Activity): Boolean {
        return setOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    /**
     * Get orientation name
     */
    fun getOrientationName(): String {
        return when (getCurrentOrientation()) {
            Configuration.ORIENTATION_PORTRAIT -> "Portrait"
            Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
            else -> "Undefined"
        }
    }

    /**
     * Get rotation name
     */
    fun getRotationName(): String {
        return when (getCurrentRotation()) {
            0 -> "0° (Normal Portrait)"
            90 -> "90° (Landscape Left)"
            180 -> "180° (Reverse Portrait)"
            270 -> "270° (Landscape Right)"
            else -> "Unknown"
        }
    }

    /**
     * Export orientation status as JSON
     */
    fun getOrientationStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("orientation", getCurrentOrientation())
            put("orientationName", getOrientationName())
            put("isPortrait", isPortrait())
            put("isLandscape", isLandscape())
            put("rotation", getCurrentRotation())
            put("rotationName", getRotationName())
        }
    }

    /**
     * Export orientation info as text
     */
    fun exportOrientationInfo(): String {
        val sb = StringBuilder()

        sb.append("Screen Orientation\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Current State ---\n")
        sb.append("Orientation: ${getOrientationName()}\n")
        sb.append("Rotation: ${getRotationName()}\n")
        sb.append("Is Portrait: ${isPortrait()}\n")
        sb.append("Is Landscape: ${isLandscape()}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Orientation constants
         */
        const val ORIENTATION_PORTRAIT = Configuration.ORIENTATION_PORTRAIT
        const val ORIENTATION_LANDSCAPE = Configuration.ORIENTATION_LANDSCAPE

        /**
         * Screen orientation mode constants
         */
        const val SCREEN_ORIENTATION_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        const val SCREEN_ORIENTATION_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        const val SCREEN_ORIENTATION_REVERSE_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        const val SCREEN_ORIENTATION_REVERSE_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        const val SCREEN_ORIENTATION_SENSOR = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        const val SCREEN_ORIENTATION_SENSOR_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        const val SCREEN_ORIENTATION_SENSOR_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        const val SCREEN_ORIENTATION_UNSPECIFIED = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        /**
         * Check if current orientation is portrait
         */
        fun isPortrait(context: Context): Boolean {
            return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }

        /**
         * Check if current orientation is landscape
         */
        fun isLandscape(context: Context): Boolean {
            return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        /**
         * Lock to portrait mode
         */
        fun lockPortrait(activity: Activity): Boolean {
            return try {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Lock to landscape mode
         */
        fun lockLandscape(activity: Activity): Boolean {
            return try {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Unlock orientation
         */
        fun unlock(activity: Activity): Boolean {
            return try {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
