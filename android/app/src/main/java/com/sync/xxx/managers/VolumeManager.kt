package com.sync.xxx.managers

import android.content.Context
import android.media.AudioManager
import android.util.Log
import org.json.JSONObject

/**
 * VolumeManager.kt
 * Control device volume levels
 * Manage media, ring, notification, alarm, and call volumes
 */
class VolumeManager(private val context: Context) {

    private val TAG = "VolumeManager"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Get current volume for stream
     */
    fun getVolume(streamType: Int): Int {
        return audioManager.getStreamVolume(streamType)
    }

    /**
     * Get maximum volume for stream
     */
    fun getMaxVolume(streamType: Int): Int {
        return audioManager.getStreamMaxVolume(streamType)
    }

    /**
     * Set volume for stream
     */
    fun setVolume(streamType: Int, volume: Int, showUI: Boolean = false): Boolean {
        return try {
            val maxVolume = getMaxVolume(streamType)
            val clampedVolume = volume.coerceIn(0, maxVolume)
            
            val flags = if (showUI) AudioManager.FLAG_SHOW_UI else 0
            audioManager.setStreamVolume(streamType, clampedVolume, flags)
            
            Log.d(TAG, "Volume set for stream $streamType: $clampedVolume")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
            false
        }
    }

    /**
     * Get volume percentage (0-100)
     */
    fun getVolumePercentage(streamType: Int): Int {
        val currentVolume = getVolume(streamType)
        val maxVolume = getMaxVolume(streamType)
        return if (maxVolume > 0) {
            (currentVolume * 100 / maxVolume)
        } else {
            0
        }
    }

    /**
     * Set volume by percentage (0-100)
     */
    fun setVolumeByPercentage(streamType: Int, percentage: Int, showUI: Boolean = false): Boolean {
        val clampedPercentage = percentage.coerceIn(0, 100)
        val maxVolume = getMaxVolume(streamType)
        val volume = (clampedPercentage * maxVolume / 100)
        return setVolume(streamType, volume, showUI)
    }

    /**
     * Increase volume
     */
    fun increaseVolume(streamType: Int, showUI: Boolean = true): Boolean {
        return try {
            audioManager.adjustStreamVolume(
                streamType,
                AudioManager.ADJUST_RAISE,
                if (showUI) AudioManager.FLAG_SHOW_UI else 0
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error increasing volume", e)
            false
        }
    }

    /**
     * Decrease volume
     */
    fun decreaseVolume(streamType: Int, showUI: Boolean = true): Boolean {
        return try {
            audioManager.adjustStreamVolume(
                streamType,
                AudioManager.ADJUST_LOWER,
                if (showUI) AudioManager.FLAG_SHOW_UI else 0
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error decreasing volume", e)
            false
        }
    }

    /**
     * Set volume to maximum
     */
    fun setMaxVolume(streamType: Int, showUI: Boolean = false): Boolean {
        val maxVolume = getMaxVolume(streamType)
        return setVolume(streamType, maxVolume, showUI)
    }

    /**
     * Set volume to minimum (mute)
     */
    fun setMinVolume(streamType: Int, showUI: Boolean = false): Boolean {
        return setVolume(streamType, 0, showUI)
    }

    /**
     * Mute stream
     */
    fun mute(streamType: Int): Boolean {
        return setVolume(streamType, 0, false)
    }

    /**
     * Unmute stream (restore to 50%)
     */
    fun unmute(streamType: Int): Boolean {
        return setVolumeByPercentage(streamType, 50, false)
    }

    /**
     * Media volume controls
     */
    fun getMediaVolume(): Int = getVolume(AudioManager.STREAM_MUSIC)
    fun setMediaVolume(volume: Int, showUI: Boolean = false) = setVolume(AudioManager.STREAM_MUSIC, volume, showUI)
    fun setMediaVolumePercentage(percentage: Int, showUI: Boolean = false) = setVolumeByPercentage(AudioManager.STREAM_MUSIC, percentage, showUI)
    fun increaseMediaVolume(showUI: Boolean = true) = increaseVolume(AudioManager.STREAM_MUSIC, showUI)
    fun decreaseMediaVolume(showUI: Boolean = true) = decreaseVolume(AudioManager.STREAM_MUSIC, showUI)

    /**
     * Ring volume controls
     */
    fun getRingVolume(): Int = getVolume(AudioManager.STREAM_RING)
    fun setRingVolume(volume: Int, showUI: Boolean = false) = setVolume(AudioManager.STREAM_RING, volume, showUI)
    fun setRingVolumePercentage(percentage: Int, showUI: Boolean = false) = setVolumeByPercentage(AudioManager.STREAM_RING, percentage, showUI)
    fun increaseRingVolume(showUI: Boolean = true) = increaseVolume(AudioManager.STREAM_RING, showUI)
    fun decreaseRingVolume(showUI: Boolean = true) = decreaseVolume(AudioManager.STREAM_RING, showUI)

    /**
     * Notification volume controls
     */
    fun getNotificationVolume(): Int = getVolume(AudioManager.STREAM_NOTIFICATION)
    fun setNotificationVolume(volume: Int, showUI: Boolean = false) = setVolume(AudioManager.STREAM_NOTIFICATION, volume, showUI)
    fun setNotificationVolumePercentage(percentage: Int, showUI: Boolean = false) = setVolumeByPercentage(AudioManager.STREAM_NOTIFICATION, percentage, showUI)

    /**
     * Alarm volume controls
     */
    fun getAlarmVolume(): Int = getVolume(AudioManager.STREAM_ALARM)
    fun setAlarmVolume(volume: Int, showUI: Boolean = false) = setVolume(AudioManager.STREAM_ALARM, volume, showUI)
    fun setAlarmVolumePercentage(percentage: Int, showUI: Boolean = false) = setVolumeByPercentage(AudioManager.STREAM_ALARM, percentage, showUI)

    /**
     * Voice call volume controls
     */
    fun getCallVolume(): Int = getVolume(AudioManager.STREAM_VOICE_CALL)
    fun setCallVolume(volume: Int, showUI: Boolean = false) = setVolume(AudioManager.STREAM_VOICE_CALL, volume, showUI)
    fun setCallVolumePercentage(percentage: Int, showUI: Boolean = false) = setVolumeByPercentage(AudioManager.STREAM_VOICE_CALL, percentage, showUI)

    /**
     * Get ringer mode
     */
    fun getRingerMode(): Int {
        return audioManager.ringerMode
    }

    /**
     * Set ringer mode
     */
    fun setRingerMode(mode: Int): Boolean {
        return try {
            audioManager.ringerMode = mode
            Log.d(TAG, "Ringer mode set to $mode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting ringer mode", e)
            false
        }
    }

    /**
     * Set ringer to normal mode
     */
    fun setRingerNormal(): Boolean = setRingerMode(AudioManager.RINGER_MODE_NORMAL)

    /**
     * Set ringer to vibrate mode
     */
    fun setRingerVibrate(): Boolean = setRingerMode(AudioManager.RINGER_MODE_VIBRATE)

    /**
     * Set ringer to silent mode
     */
    fun setRingerSilent(): Boolean = setRingerMode(AudioManager.RINGER_MODE_SILENT)

    /**
     * Check if ringer is in silent mode
     */
    fun isSilent(): Boolean = getRingerMode() == AudioManager.RINGER_MODE_SILENT

    /**
     * Check if ringer is in vibrate mode
     */
    fun isVibrate(): Boolean = getRingerMode() == AudioManager.RINGER_MODE_VIBRATE

    /**
     * Check if ringer is in normal mode
     */
    fun isNormal(): Boolean = getRingerMode() == AudioManager.RINGER_MODE_NORMAL

    /**
     * Export volume status as JSON
     */
    fun getVolumeStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("media", JSONObject().apply {
                put("volume", getMediaVolume())
                put("max", getMaxVolume(AudioManager.STREAM_MUSIC))
                put("percentage", getVolumePercentage(AudioManager.STREAM_MUSIC))
            })
            put("ring", JSONObject().apply {
                put("volume", getRingVolume())
                put("max", getMaxVolume(AudioManager.STREAM_RING))
                put("percentage", getVolumePercentage(AudioManager.STREAM_RING))
            })
            put("notification", JSONObject().apply {
                put("volume", getNotificationVolume())
                put("max", getMaxVolume(AudioManager.STREAM_NOTIFICATION))
                put("percentage", getVolumePercentage(AudioManager.STREAM_NOTIFICATION))
            })
            put("alarm", JSONObject().apply {
                put("volume", getAlarmVolume())
                put("max", getMaxVolume(AudioManager.STREAM_ALARM))
                put("percentage", getVolumePercentage(AudioManager.STREAM_ALARM))
            })
            put("call", JSONObject().apply {
                put("volume", getCallVolume())
                put("max", getMaxVolume(AudioManager.STREAM_VOICE_CALL))
                put("percentage", getVolumePercentage(AudioManager.STREAM_VOICE_CALL))
            })
            put("ringerMode", getRingerMode())
            put("isSilent", isSilent())
            put("isVibrate", isVibrate())
            put("isNormal", isNormal())
        }
    }

    /**
     * Export volume info as text
     */
    fun exportVolumeInfo(): String {
        val sb = StringBuilder()

        sb.append("Volume Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Volumes ---\n")
        sb.append("Media: ${getVolumePercentage(AudioManager.STREAM_MUSIC)}% (${getMediaVolume()}/${getMaxVolume(AudioManager.STREAM_MUSIC)})\n")
        sb.append("Ring: ${getVolumePercentage(AudioManager.STREAM_RING)}% (${getRingVolume()}/${getMaxVolume(AudioManager.STREAM_RING)})\n")
        sb.append("Notification: ${getVolumePercentage(AudioManager.STREAM_NOTIFICATION)}% (${getNotificationVolume()}/${getMaxVolume(AudioManager.STREAM_NOTIFICATION)})\n")
        sb.append("Alarm: ${getVolumePercentage(AudioManager.STREAM_ALARM)}% (${getAlarmVolume()}/${getMaxVolume(AudioManager.STREAM_ALARM)})\n")
        sb.append("Call: ${getVolumePercentage(AudioManager.STREAM_VOICE_CALL)}% (${getCallVolume()}/${getMaxVolume(AudioManager.STREAM_VOICE_CALL)})\n\n")

        sb.append("--- Ringer Mode ---\n")
        val ringerMode = when (getRingerMode()) {
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            else -> "Unknown"
        }
        sb.append("Mode: $ringerMode\n")

        return sb.toString()
    }

    companion object {
        /**
         * Stream type constants
         */
        const val STREAM_MUSIC = AudioManager.STREAM_MUSIC
        const val STREAM_RING = AudioManager.STREAM_RING
        const val STREAM_NOTIFICATION = AudioManager.STREAM_NOTIFICATION
        const val STREAM_ALARM = AudioManager.STREAM_ALARM
        const val STREAM_VOICE_CALL = AudioManager.STREAM_VOICE_CALL

        /**
         * Ringer mode constants
         */
        const val RINGER_MODE_NORMAL = AudioManager.RINGER_MODE_NORMAL
        const val RINGER_MODE_VIBRATE = AudioManager.RINGER_MODE_VIBRATE
        const val RINGER_MODE_SILENT = AudioManager.RINGER_MODE_SILENT
    }
}
