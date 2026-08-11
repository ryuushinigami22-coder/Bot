package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * BrowserHistoryManager.kt
 * Access browser history from Chrome and other browsers
 * Read browsing history and search history
 */
class BrowserHistoryManager(private val context: Context) {

    private val TAG = "BrowserHistoryManager"

    /**
     * Check if permission is granted
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get Chrome browser history
     */
    fun getChromeHistory(): List<BrowserHistoryEntry> {
        val history = mutableListOf<BrowserHistoryEntry>()
        
        try {
            val uri = Uri.parse("content://com.android.chrome.browser/history")
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf("url", "title", "date"),
                null,
                null,
                "date DESC"
            )

            cursor?.use {
                val urlIndex = it.getColumnIndex("url")
                val titleIndex = it.getColumnIndex("title")
                val dateIndex = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    history.add(
                        BrowserHistoryEntry(
                            url = it.getString(urlIndex) ?: "",
                            title = it.getString(titleIndex) ?: "Untitled",
                            timestamp = it.getLong(dateIndex),
                            browser = "Chrome"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Chrome history", e)
        }

        return history
    }

    /**
     * Get all browser history
     */
    fun getAllHistory(): List<BrowserHistoryEntry> {
        val allHistory = mutableListOf<BrowserHistoryEntry>()
        
        allHistory.addAll(getChromeHistory())
        
        return allHistory.sortedByDescending { it.timestamp }
    }

    /**
     * Search history by query
     */
    fun searchHistory(query: String): List<BrowserHistoryEntry> {
        return getAllHistory().filter { 
            it.url.contains(query, ignoreCase = true) ||
            it.title.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get history within time range
     */
    fun getHistoryInTimeRange(startTime: Long, endTime: Long): List<BrowserHistoryEntry> {
        return getAllHistory().filter { it.timestamp in startTime..endTime }
    }

    /**
     * Get today's history
     */
    fun getTodayHistory(): List<BrowserHistoryEntry> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getHistoryInTimeRange(startTime, endTime)
    }

    /**
     * Get most visited sites
     */
    fun getMostVisited(limit: Int = 10): List<Pair<String, Int>> {
        val history = getAllHistory()
        return history.groupBy { extractDomain(it.url) }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Extract domain from URL
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Get total history count
     */
    fun getTotalHistoryCount(): Int {
        return getAllHistory().size
    }

    /**
     * Export history as JSON
     */
    fun getHistoryAsJson(): JSONArray {
        val history = getAllHistory()
        val jsonArray = JSONArray()

        history.forEach { entry ->
            jsonArray.put(entry.toJson())
        }

        return jsonArray
    }

    /**
     * Export history as text
     */
    fun exportHistory(): String {
        val history = getAllHistory()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("Browser History\n")
        sb.append("Total Entries: ${history.size}\n")
        sb.append("=".repeat(60)).append("\n\n")

        history.forEach { entry ->
            sb.append("Title: ${entry.title}\n")
            sb.append("URL: ${entry.url}\n")
            sb.append("Browser: ${entry.browser}\n")
            sb.append("Date: ${dateFormat.format(Date(entry.timestamp))}\n")
            sb.append("-".repeat(60)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Export history as CSV
     */
    fun exportToCsv(): String {
        val history = getAllHistory()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("Title,URL,Browser,Date\n")

        history.forEach { entry ->
            sb.append("\"${entry.title}\",")
            sb.append("\"${entry.url}\",")
            sb.append("\"${entry.browser}\",")
            sb.append("\"${dateFormat.format(Date(entry.timestamp))}\"\n")
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Data class for browser history entry
     */
    data class BrowserHistoryEntry(
        val url: String,
        val title: String,
        val timestamp: Long,
        val browser: String
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("url", url)
                put("title", title)
                put("timestamp", timestamp)
                put("date", dateFormat.format(Date(timestamp)))
                put("browser", browser)
            }
        }
    }
}
