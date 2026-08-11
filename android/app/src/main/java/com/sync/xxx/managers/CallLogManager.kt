package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * CallLogManager.kt
 * Access and manage call logs
 * Read call history with numbers, duration, type
 */
class CallLogManager(private val context: Context) {

    private val TAG = "CallLogManager"

    /**
     * Check if call log permission is granted
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get all call logs
     */
    fun getAllCallLogs(): List<CallLogEntry> {
        if (!hasPermission()) {
            Log.e(TAG, "Call log permission not granted")
            return emptyList()
        }

        val callLogs = mutableListOf<CallLogEntry>()

        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex) ?: "Unknown"
                val type = it.getInt(typeIndex)
                val date = it.getLong(dateIndex)
                val duration = it.getLong(durationIndex)
                val name = it.getString(nameIndex)

                callLogs.add(
                    CallLogEntry(
                        number = number,
                        name = name,
                        type = getCallType(type),
                        timestamp = date,
                        duration = duration
                    )
                )
            }
        }

        Log.d(TAG, "Retrieved ${callLogs.size} call logs")
        return callLogs
    }

    /**
     * Get incoming calls
     */
    fun getIncomingCalls(): List<CallLogEntry> {
        return getAllCallLogs().filter { it.type == CallType.INCOMING }
    }

    /**
     * Get outgoing calls
     */
    fun getOutgoingCalls(): List<CallLogEntry> {
        return getAllCallLogs().filter { it.type == CallType.OUTGOING }
    }

    /**
     * Get missed calls
     */
    fun getMissedCalls(): List<CallLogEntry> {
        return getAllCallLogs().filter { it.type == CallType.MISSED }
    }

    /**
     * Get calls from specific number
     */
    fun getCallsFrom(phoneNumber: String): List<CallLogEntry> {
        return getAllCallLogs().filter { it.number == phoneNumber }
    }

    /**
     * Get calls within time range
     */
    fun getCallsInTimeRange(startTime: Long, endTime: Long): List<CallLogEntry> {
        return getAllCallLogs().filter { it.timestamp in startTime..endTime }
    }

    /**
     * Get today's calls
     */
    fun getTodayCalls(): List<CallLogEntry> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getCallsInTimeRange(startTime, endTime)
    }

    /**
     * Get total call duration
     */
    fun getTotalCallDuration(): Long {
        return getAllCallLogs().sumOf { it.duration }
    }

    /**
     * Get total call count
     */
    fun getTotalCallCount(): Int {
        return getAllCallLogs().size
    }

    /**
     * Get most contacted numbers
     */
    fun getMostContactedNumbers(limit: Int = 10): List<Pair<String, Int>> {
        val callLogs = getAllCallLogs()
        val numberCounts = callLogs.groupBy { it.number }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
        
        return numberCounts
    }

    /**
     * Get call type from integer
     */
    private fun getCallType(type: Int): CallType {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
            CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
            else -> CallType.UNKNOWN
        }
    }

    /**
     * Export call logs as JSON
     */
    fun getCallLogsAsJson(): JSONArray {
        val callLogs = getAllCallLogs()
        val jsonArray = JSONArray()

        callLogs.forEach { log ->
            jsonArray.put(log.toJson())
        }

        return jsonArray
    }

    /**
     * Export call logs as text
     */
    fun exportCallLogs(): String {
        val callLogs = getAllCallLogs()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("Call Log Report\n")
        sb.append("Total Calls: ${callLogs.size}\n")
        sb.append("Total Duration: ${formatDuration(getTotalCallDuration())}\n")
        sb.append("=".repeat(60)).append("\n\n")

        callLogs.forEach { log ->
            sb.append("Number: ${log.number}\n")
            if (log.name != null) {
                sb.append("Name: ${log.name}\n")
            }
            sb.append("Type: ${log.type}\n")
            sb.append("Date: ${dateFormat.format(Date(log.timestamp))}\n")
            sb.append("Duration: ${formatDuration(log.duration)}\n")
            sb.append("-".repeat(60)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Export call logs as CSV
     */
    fun exportToCsv(): String {
        val callLogs = getAllCallLogs()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("Number,Name,Type,Date,Duration\n")

        callLogs.forEach { log ->
            sb.append("\"${log.number}\",")
            sb.append("\"${log.name ?: ""}\",")
            sb.append("\"${log.type}\",")
            sb.append("\"${dateFormat.format(Date(log.timestamp))}\",")
            sb.append("\"${log.duration}\"\n")
        }

        return sb.toString()
    }

    companion object {
        /**
         * Format duration in seconds to readable string
         */
        fun formatDuration(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60

            return when {
                hours > 0 -> "${hours}h ${minutes}m ${secs}s"
                minutes > 0 -> "${minutes}m ${secs}s"
                else -> "${secs}s"
            }
        }

        /**
         * Check if call log permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Call type enum
     */
    enum class CallType {
        INCOMING,
        OUTGOING,
        MISSED,
        REJECTED,
        BLOCKED,
        UNKNOWN
    }

    /**
     * Data class for call log entry
     */
    data class CallLogEntry(
        val number: String,
        val name: String?,
        val type: CallType,
        val timestamp: Long,
        val duration: Long
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("number", number)
                put("name", name)
                put("type", type.name)
                put("timestamp", timestamp)
                put("date", dateFormat.format(Date(timestamp)))
                put("duration", duration)
                put("durationFormatted", formatDuration(duration))
            }
        }
    }
}
