package com.sync.xxx.managers

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * AppUsageManager.kt
 * Monitor app usage statistics
 * Track which apps are opened and for how long
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AppUsageManager(private val context: Context) {

    private val TAG = "AppUsageManager"
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Check if usage access permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        if (usageStatsManager == null) return false
        
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000,
            time
        )
        return stats != null && stats.isNotEmpty()
    }

    /**
     * Get app usage stats for time range
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     * @return List of usage stats
     */
    fun getUsageStats(startTime: Long, endTime: Long): List<UsageStats> {
        if (!hasUsageStatsPermission()) {
            Log.e(TAG, "Usage stats permission not granted")
            return emptyList()
        }

        return try {
            usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )?.filter { it.totalTimeInForeground > 0 } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting usage stats", e)
            emptyList()
        }
    }

    /**
     * Get usage stats for today
     */
    fun getTodayUsageStats(): List<UsageStats> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getUsageStats(startTime, endTime)
    }

    /**
     * Get usage stats for last N days
     */
    fun getUsageStatsForDays(days: Int): List<UsageStats> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000L)
        return getUsageStats(startTime, endTime)
    }

    /**
     * Get most used apps
     * @param limit Number of apps to return
     */
    fun getMostUsedApps(limit: Int = 10): List<AppUsageInfo> {
        val stats = getTodayUsageStats()
        return stats
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .map { stat ->
                AppUsageInfo(
                    packageName = stat.packageName,
                    appName = getAppName(stat.packageName),
                    totalTimeInForeground = stat.totalTimeInForeground,
                    lastTimeUsed = stat.lastTimeUsed,
                    firstTimeStamp = stat.firstTimeStamp
                )
            }
    }

    /**
     * Get recently used apps
     * @param limit Number of apps to return
     */
    fun getRecentlyUsedApps(limit: Int = 10): List<AppUsageInfo> {
        val stats = getTodayUsageStats()
        return stats
            .sortedByDescending { it.lastTimeUsed }
            .take(limit)
            .map { stat ->
                AppUsageInfo(
                    packageName = stat.packageName,
                    appName = getAppName(stat.packageName),
                    totalTimeInForeground = stat.totalTimeInForeground,
                    lastTimeUsed = stat.lastTimeUsed,
                    firstTimeStamp = stat.firstTimeStamp
                )
            }
    }

    /**
     * Get usage info for specific app
     */
    fun getAppUsageInfo(packageName: String): AppUsageInfo? {
        val stats = getTodayUsageStats()
        val stat = stats.find { it.packageName == packageName } ?: return null
        
        return AppUsageInfo(
            packageName = stat.packageName,
            appName = getAppName(stat.packageName),
            totalTimeInForeground = stat.totalTimeInForeground,
            lastTimeUsed = stat.lastTimeUsed,
            firstTimeStamp = stat.firstTimeStamp
        )
    }

    /**
     * Get total screen time today
     */
    fun getTotalScreenTime(): Long {
        val stats = getTodayUsageStats()
        return stats.sumOf { it.totalTimeInForeground }
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
     * Format usage stats as JSON
     */
    fun getUsageStatsAsJson(): JSONArray {
        val stats = getTodayUsageStats()
        val jsonArray = JSONArray()
        
        stats.forEach { stat ->
            val info = AppUsageInfo(
                packageName = stat.packageName,
                appName = getAppName(stat.packageName),
                totalTimeInForeground = stat.totalTimeInForeground,
                lastTimeUsed = stat.lastTimeUsed,
                firstTimeStamp = stat.firstTimeStamp
            )
            jsonArray.put(info.toJson())
        }
        
        return jsonArray
    }

    /**
     * Export usage stats to string
     */
    fun exportUsageStats(): String {
        val stats = getTodayUsageStats()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        sb.append("App Usage Stats - ${dateFormat.format(Date())}\n")
        sb.append("Total Screen Time: ${formatDuration(getTotalScreenTime())}\n")
        sb.append("=".repeat(60)).append("\n\n")
        
        stats.sortedByDescending { it.totalTimeInForeground }.forEach { stat ->
            sb.append("App: ${getAppName(stat.packageName)}\n")
            sb.append("Package: ${stat.packageName}\n")
            sb.append("Time Used: ${formatDuration(stat.totalTimeInForeground)}\n")
            sb.append("Last Used: ${dateFormat.format(Date(stat.lastTimeUsed))}\n")
            sb.append("-".repeat(60)).append("\n")
        }
        
        return sb.toString()
    }

    companion object {
        /**
         * Format duration in milliseconds to readable string
         */
        fun formatDuration(millis: Long): String {
            val hours = millis / (1000 * 60 * 60)
            val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (millis % (1000 * 60)) / 1000
            
            return when {
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    }

    /**
     * Data class for app usage info
     */
    data class AppUsageInfo(
        val packageName: String,
        val appName: String,
        val totalTimeInForeground: Long,
        val lastTimeUsed: Long,
        val firstTimeStamp: Long
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("packageName", packageName)
                put("appName", appName)
                put("totalTimeInForeground", totalTimeInForeground)
                put("totalTimeFormatted", formatDuration(totalTimeInForeground))
                put("lastTimeUsed", lastTimeUsed)
                put("lastTimeUsedFormatted", dateFormat.format(Date(lastTimeUsed)))
                put("firstTimeStamp", firstTimeStamp)
            }
        }
    }
}
