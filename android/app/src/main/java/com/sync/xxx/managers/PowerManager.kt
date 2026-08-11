package com.sync.xxx.managers

import android.content.Context
import android.content.Intent
import android.os.PowerManager as AndroidPowerManager
import android.util.Log
import org.json.JSONObject
import java.io.DataOutputStream

/**
 * PowerManager.kt
 * Power management - reboot, shutdown, sleep
 * Requires root access for reboot/shutdown
 */
class PowerManager(private val context: Context) {

    private val TAG = "PowerManager"
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as AndroidPowerManager

    /**
     * Reboot device (requires root)
     */
    fun reboot(): Boolean {
        return try {
            executeRootCommand("reboot")
        } catch (e: Exception) {
            Log.e(TAG, "Error rebooting device", e)
            false
        }
    }

    /**
     * Shutdown device (requires root)
     */
    fun shutdown(): Boolean {
        return try {
            executeRootCommand("reboot -p")
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down device", e)
            false
        }
    }

    /**
     * Reboot to recovery (requires root)
     */
    fun rebootToRecovery(): Boolean {
        return try {
            executeRootCommand("reboot recovery")
        } catch (e: Exception) {
            Log.e(TAG, "Error rebooting to recovery", e)
            false
        }
    }

    /**
     * Reboot to bootloader (requires root)
     */
    fun rebootToBootloader(): Boolean {
        return try {
            executeRootCommand("reboot bootloader")
        } catch (e: Exception) {
            Log.e(TAG, "Error rebooting to bootloader", e)
            false
        }
    }

    /**
     * Put device to sleep
     */
    fun goToSleep(): Boolean {
        return try {
            Log.w(TAG, "goToSleep requires system permission")
            return false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error putting device to sleep", e)
            false
        }
    }

    /**
     * Wake up device
     */
    fun wakeUp(): Boolean {
        return try {
            Log.w(TAG, "wakeUp requires system permission")
            return false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error waking up device", e)
            false
        }
    }

    /**
     * Check if device is in interactive mode
     */
    fun isInteractive(): Boolean {
        return powerManager.isInteractive
    }

    /**
     * Check if device is in power save mode
     */
    fun isPowerSaveMode(): Boolean {
        return powerManager.isPowerSaveMode
    }

    /**
     * Check if device is in idle mode
     */
    fun isDeviceIdleMode(): Boolean {
        return powerManager.isDeviceIdleMode
    }

    /**
     * Acquire partial wake lock
     */
    fun acquireWakeLock(tag: String, timeout: Long = 0): AndroidPowerManager.WakeLock {
        val wakeLock = powerManager.newWakeLock(
            AndroidPowerManager.PARTIAL_WAKE_LOCK,
            "$TAG:$tag"
        )
        
        if (timeout > 0) {
            wakeLock.acquire(timeout)
        } else {
            wakeLock.acquire()
        }
        
        Log.d(TAG, "Wake lock acquired: $tag")
        return wakeLock
    }

    /**
     * Acquire screen wake lock
     */
    fun acquireScreenWakeLock(tag: String, timeout: Long = 0): AndroidPowerManager.WakeLock {
        val wakeLock = powerManager.newWakeLock(
            AndroidPowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            AndroidPowerManager.ACQUIRE_CAUSES_WAKEUP,
            "$TAG:$tag"
        )
        
        if (timeout > 0) {
            wakeLock.acquire(timeout)
        } else {
            wakeLock.acquire()
        }
        
        Log.d(TAG, "Screen wake lock acquired: $tag")
        return wakeLock
    }

    /**
     * Execute root command
     */
    private fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Log.d(TAG, "Root command executed successfully: $command")
                true
            } else {
                Log.e(TAG, "Root command failed with exit code $exitCode: $command")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root command: $command", e)
            false
        }
    }

    /**
     * Check if device has root access
     */
    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            
            outputStream.writeBytes("id\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking root access", e)
            false
        }
    }

    /**
     * Get power status as JSON
     */
    fun getPowerStatusAsJson(): JSONObject {
        return JSONObject().apply {
            put("isInteractive", isInteractive())
            put("isPowerSaveMode", isPowerSaveMode())
            put("isDeviceIdleMode", isDeviceIdleMode())
            put("hasRootAccess", hasRootAccess())
        }
    }

    /**
     * Export power info as text
     */
    fun exportPowerInfo(): String {
        val sb = StringBuilder()

        sb.append("Power Management\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Interactive: ${isInteractive()}\n")
        sb.append("Power Save Mode: ${isPowerSaveMode()}\n")
        sb.append("Device Idle Mode: ${isDeviceIdleMode()}\n")
        sb.append("Root Access: ${hasRootAccess()}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Wake lock level constants
         */
        const val PARTIAL_WAKE_LOCK = AndroidPowerManager.PARTIAL_WAKE_LOCK
        const val SCREEN_DIM_WAKE_LOCK = AndroidPowerManager.SCREEN_DIM_WAKE_LOCK
        const val SCREEN_BRIGHT_WAKE_LOCK = AndroidPowerManager.SCREEN_BRIGHT_WAKE_LOCK
        const val FULL_WAKE_LOCK = AndroidPowerManager.FULL_WAKE_LOCK

        /**
         * Check if device is interactive
         */
        fun isInteractive(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as AndroidPowerManager
            return powerManager.isInteractive
        }

        /**
         * Check if device has root access
         */
        fun hasRootAccess(): Boolean {
            return try {
                val process = Runtime.getRuntime().exec("su")
                val outputStream = DataOutputStream(process.outputStream)
                
                outputStream.writeBytes("id\n")
                outputStream.writeBytes("exit\n")
                outputStream.flush()
                
                val exitCode = process.waitFor()
                exitCode == 0
            } catch (e: Exception) {
                false
            }
        }
    }
}
