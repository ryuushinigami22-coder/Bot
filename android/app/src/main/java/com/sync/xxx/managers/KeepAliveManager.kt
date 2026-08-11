package com.sync.xxx.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import org.json.JSONObject

/**
 * KeepAliveManager.kt
 * Keep service running continuously
 * Prevent service from being killed
 */
class KeepAliveManager(private val context: Context) {

    private val TAG = "KeepAliveManager"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /**
     * Setup keep-alive mechanism
     */
    fun setupKeepAlive(serviceClass: Class<*>): Boolean {
        return try {
            setupAlarmManager(serviceClass)
            Log.d(TAG, "Keep-alive setup completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up keep-alive", e)
            false
        }
    }

    /**
     * Setup alarm manager to restart service
     */
    private fun setupAlarmManager(serviceClass: Class<*>) {
        val intent = Intent(context, serviceClass)
        val pendingIntent = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule alarm to restart service every 15 minutes
        val intervalMillis = 15 * 60 * 1000L // 15 minutes
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel keep-alive mechanism
     */
    fun cancelKeepAlive(serviceClass: Class<*>): Boolean {
        return try {
            val intent = Intent(context, serviceClass)
            val pendingIntent = PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Keep-alive cancelled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling keep-alive", e)
            false
        }
    }

    /**
     * Acquire wake lock to prevent sleep
     */
    fun acquireWakeLock(): PowerManager.WakeLock? {
        return try {
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "KeepAlive::WakeLock"
            )
            wakeLock.acquire()
            Log.d(TAG, "Wake lock acquired")
            wakeLock
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
            null
        }
    }

    /**
     * Release wake lock
     */
    fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        try {
            wakeLock?.release()
            Log.d(TAG, "Wake lock released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }

    /**
     * Get keep-alive status
     */
    fun getKeepAliveStatus(): KeepAliveStatus {
        return KeepAliveStatus(
            isActive = true, // This would need service state tracking
            hasWakeLock = false, // This would need wake lock state tracking
            alarmSet = true // This would need alarm state tracking
        )
    }

    /**
     * Export keep-alive status as JSON
     */
    fun getKeepAliveStatusAsJson(): JSONObject {
        val status = getKeepAliveStatus()
        return JSONObject().apply {
            put("isActive", status.isActive)
            put("hasWakeLock", status.hasWakeLock)
            put("alarmSet", status.alarmSet)
        }
    }

    /**
     * Export keep-alive status as text
     */
    fun exportKeepAliveStatus(): String {
        val status = getKeepAliveStatus()
        val sb = StringBuilder()

        sb.append("Keep-Alive Status\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Active: ${if (status.isActive) "Yes" else "No"}\n")
        sb.append("Wake Lock: ${if (status.hasWakeLock) "Yes" else "No"}\n")
        sb.append("Alarm Set: ${if (status.alarmSet) "Yes" else "No"}\n")

        return sb.toString()
    }

    /**
     * Keep-alive status data class
     */
    data class KeepAliveStatus(
        val isActive: Boolean,
        val hasWakeLock: Boolean,
        val alarmSet: Boolean
    )

    /**
     * Base service with keep-alive functionality
     */
    abstract class KeepAliveService : Service() {
        
        private var wakeLock: PowerManager.WakeLock? = null
        
        override fun onCreate() {
            super.onCreate()
            Log.d("KeepAliveService", "Service created")
            
            val manager = KeepAliveManager(this)
            wakeLock = manager.acquireWakeLock()
            manager.setupKeepAlive(this::class.java)
        }
        
        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            Log.d("KeepAliveService", "Service started")
            return START_STICKY // Restart service if killed
        }
        
        override fun onDestroy() {
            super.onDestroy()
            Log.d("KeepAliveService", "Service destroyed")
            
            val manager = KeepAliveManager(this)
            manager.releaseWakeLock(wakeLock)
            
            // Restart service
            val restartIntent = Intent(this, this::class.java)
            startService(restartIntent)
        }
        
        override fun onBind(intent: Intent?): IBinder? {
            return null
        }
        
        override fun onTaskRemoved(rootIntent: Intent?) {
            super.onTaskRemoved(rootIntent)
            Log.d("KeepAliveService", "Task removed, restarting...")
            
            // Restart service when task is removed
            val restartIntent = Intent(this, this::class.java)
            startService(restartIntent)
        }
    }

    companion object {
        /**
         * Setup keep-alive (static)
         */
        fun setupKeepAlive(context: Context, serviceClass: Class<*>): Boolean {
            return try {
                val manager = KeepAliveManager(context)
                manager.setupKeepAlive(serviceClass)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Cancel keep-alive (static)
         */
        fun cancelKeepAlive(context: Context, serviceClass: Class<*>): Boolean {
            return try {
                val manager = KeepAliveManager(context)
                manager.cancelKeepAlive(serviceClass)
            } catch (e: Exception) {
                false
            }
        }
    }
}
