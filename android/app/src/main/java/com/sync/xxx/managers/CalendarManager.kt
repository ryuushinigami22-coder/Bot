package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * CalendarManager.kt
 * Access calendar events and reminders
 * Read calendar data from device
 */
class CalendarManager(private val context: Context) {

    private val TAG = "CalendarManager"

    /**
     * Check if calendar permission is granted
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get all calendar events
     */
    fun getAllEvents(): List<CalendarEvent> {
        if (!hasPermission()) {
            Log.e(TAG, "Calendar permission not granted")
            return emptyList()
        }

        val events = mutableListOf<CalendarEvent>()
        val uri = CalendarContract.Events.CONTENT_URI

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            CalendarContract.Events.DTSTART + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
            val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
            val descIndex = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
            val locationIndex = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
            val calendarIndex = it.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

            while (it.moveToNext()) {
                events.add(
                    CalendarEvent(
                        id = it.getLong(idIndex),
                        title = it.getString(titleIndex) ?: "Untitled",
                        description = it.getString(descIndex) ?: "",
                        startTime = it.getLong(startIndex),
                        endTime = it.getLong(endIndex),
                        location = it.getString(locationIndex) ?: "",
                        calendar = it.getString(calendarIndex) ?: "Unknown"
                    )
                )
            }
        }

        Log.d(TAG, "Retrieved ${events.size} calendar events")
        return events
    }

    /**
     * Get upcoming events
     */
    fun getUpcomingEvents(limit: Int = 10): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        return getAllEvents()
            .filter { it.startTime >= now }
            .take(limit)
    }

    /**
     * Get past events
     */
    fun getPastEvents(limit: Int = 10): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        return getAllEvents()
            .filter { it.startTime < now }
            .sortedByDescending { it.startTime }
            .take(limit)
    }

    /**
     * Get events within time range
     */
    fun getEventsInTimeRange(startTime: Long, endTime: Long): List<CalendarEvent> {
        return getAllEvents().filter { event ->
            event.startTime in startTime..endTime
        }
    }

    /**
     * Get today's events
     */
    fun getTodayEvents(): List<CalendarEvent> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endTime = calendar.timeInMillis
        
        return getEventsInTimeRange(startTime, endTime)
    }

    /**
     * Search events by title
     */
    fun searchEvents(query: String): List<CalendarEvent> {
        return getAllEvents().filter { 
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get events by calendar name
     */
    fun getEventsByCalendar(calendarName: String): List<CalendarEvent> {
        return getAllEvents().filter { 
            it.calendar.equals(calendarName, ignoreCase = true) 
        }
    }

    /**
     * Get total event count
     */
    fun getTotalEventCount(): Int {
        return getAllEvents().size
    }

    /**
     * Export events as JSON
     */
    fun getEventsAsJson(): JSONArray {
        val events = getAllEvents()
        val jsonArray = JSONArray()

        events.forEach { event ->
            jsonArray.put(event.toJson())
        }

        return jsonArray
    }

    /**
     * Export events as text
     */
    fun exportEvents(): String {
        val events = getAllEvents()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("Calendar Events\n")
        sb.append("Total: ${events.size}\n")
        sb.append("=".repeat(60)).append("\n\n")

        events.forEach { event ->
            sb.append("Title: ${event.title}\n")
            sb.append("Calendar: ${event.calendar}\n")
            sb.append("Start: ${dateFormat.format(Date(event.startTime))}\n")
            sb.append("End: ${dateFormat.format(Date(event.endTime))}\n")
            if (event.location.isNotEmpty()) {
                sb.append("Location: ${event.location}\n")
            }
            if (event.description.isNotEmpty()) {
                sb.append("Description: ${event.description}\n")
            }
            sb.append("-".repeat(60)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Export events as CSV
     */
    fun exportToCsv(): String {
        val events = getAllEvents()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("Title,Calendar,Start,End,Location,Description\n")

        events.forEach { event ->
            sb.append("\"${event.title}\",")
            sb.append("\"${event.calendar}\",")
            sb.append("\"${dateFormat.format(Date(event.startTime))}\",")
            sb.append("\"${dateFormat.format(Date(event.endTime))}\",")
            sb.append("\"${event.location}\",")
            sb.append("\"${event.description}\"\n")
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if calendar permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Data class for calendar event
     */
    data class CalendarEvent(
        val id: Long,
        val title: String,
        val description: String,
        val startTime: Long,
        val endTime: Long,
        val location: String,
        val calendar: String
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("id", id)
                put("title", title)
                put("description", description)
                put("startTime", startTime)
                put("startDate", dateFormat.format(Date(startTime)))
                put("endTime", endTime)
                put("endDate", dateFormat.format(Date(endTime)))
                put("location", location)
                put("calendar", calendar)
            }
        }
    }
}
