package com.sync.xxx.managers

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import org.json.JSONObject
import java.util.Calendar

/**
 * DataUsageManager.kt
 * Track mobile and WiFi data usage
 * Get bytes received/transmitted over time periods
 */
class DataUsageManager(private val context: Context) {

    private val TAG = "DataUsageManager"
    private val networkStatsManager: NetworkStatsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    } else {
        null
    }

    /**
     * Get mobile data usage (bytes)
     */
    fun getMobileDataUsage(startTime: Long, endTime: Long): Pair<Long, Long> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "Data usage tracking requires Android 6.0+")
            return Pair(0L, 0L)
        }

        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subscriberId = telephonyManager.subscriberId

            val networkStats = networkStatsManager?.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                subscriberId,
                startTime,
                endTime
            )

            var totalRx = 0L
            var totalTx = 0L

            val bucket = NetworkStats.Bucket()
            while (networkStats?.hasNextBucket() == true) {
                networkStats.getNextBucket(bucket)
                totalRx += bucket.rxBytes
                totalTx += bucket.txBytes
            }

            networkStats?.close()
            Pair(totalRx, totalTx)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting mobile data usage", e)
            Pair(0L, 0L)
        }
    }

    /**
     * Get WiFi data usage (bytes)
     */
    fun getWiFiDataUsage(startTime: Long, endTime: Long): Pair<Long, Long> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "Data usage tracking requires Android 6.0+")
            return Pair(0L, 0L)
        }

        return try {
            val networkStats = networkStatsManager?.querySummary(
                ConnectivityManager.TYPE_WIFI,
                "",
                startTime,
                endTime
            )

            var totalRx = 0L
            var totalTx = 0L

            val bucket = NetworkStats.Bucket()
            while (networkStats?.hasNextBucket() == true) {
                networkStats.getNextBucket(bucket)
                totalRx += bucket.rxBytes
                totalTx += bucket.txBytes
            }

            networkStats?.close()
            Pair(totalRx, totalTx)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi data usage", e)
            Pair(0L, 0L)
        }
    }

    /**
     * Get today's mobile data usage
     */
    fun getTodayMobileDataUsage(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getMobileDataUsage(startTime, endTime)
    }

    /**
     * Get today's WiFi data usage
     */
    fun getTodayWiFiDataUsage(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getWiFiDataUsage(startTime, endTime)
    }

    /**
     * Get this month's mobile data usage
     */
    fun getMonthMobileDataUsage(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getMobileDataUsage(startTime, endTime)
    }

    /**
     * Get this month's WiFi data usage
     */
    fun getMonthWiFiDataUsage(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getWiFiDataUsage(startTime, endTime)
    }

    /**
     * Format bytes to human-readable string
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Export data usage as JSON
     */
    fun getDataUsageAsJson(): JSONObject {
        val todayMobile = getTodayMobileDataUsage()
        val todayWiFi = getTodayWiFiDataUsage()
        val monthMobile = getMonthMobileDataUsage()
        val monthWiFi = getMonthWiFiDataUsage()

        return JSONObject().apply {
            put("today", JSONObject().apply {
                put("mobile", JSONObject().apply {
                    put("received", todayMobile.first)
                    put("transmitted", todayMobile.second)
                    put("total", todayMobile.first + todayMobile.second)
                    put("receivedFormatted", formatBytes(todayMobile.first))
                    put("transmittedFormatted", formatBytes(todayMobile.second))
                    put("totalFormatted", formatBytes(todayMobile.first + todayMobile.second))
                })
                put("wifi", JSONObject().apply {
                    put("received", todayWiFi.first)
                    put("transmitted", todayWiFi.second)
                    put("total", todayWiFi.first + todayWiFi.second)
                    put("receivedFormatted", formatBytes(todayWiFi.first))
                    put("transmittedFormatted", formatBytes(todayWiFi.second))
                    put("totalFormatted", formatBytes(todayWiFi.first + todayWiFi.second))
                })
            })
            put("month", JSONObject().apply {
                put("mobile", JSONObject().apply {
                    put("received", monthMobile.first)
                    put("transmitted", monthMobile.second)
                    put("total", monthMobile.first + monthMobile.second)
                    put("receivedFormatted", formatBytes(monthMobile.first))
                    put("transmittedFormatted", formatBytes(monthMobile.second))
                    put("totalFormatted", formatBytes(monthMobile.first + monthMobile.second))
                })
                put("wifi", JSONObject().apply {
                    put("received", monthWiFi.first)
                    put("transmitted", monthWiFi.second)
                    put("total", monthWiFi.first + monthWiFi.second)
                    put("receivedFormatted", formatBytes(monthWiFi.first))
                    put("transmittedFormatted", formatBytes(monthWiFi.second))
                    put("totalFormatted", formatBytes(monthWiFi.first + monthWiFi.second))
                })
            })
        }
    }

    /**
     * Export data usage info as text
     */
    fun exportDataUsageInfo(): String {
        val sb = StringBuilder()

        sb.append("Data Usage\n")
        sb.append("=".repeat(60)).append("\n\n")

        val todayMobile = getTodayMobileDataUsage()
        val todayWiFi = getTodayWiFiDataUsage()
        val monthMobile = getMonthMobileDataUsage()
        val monthWiFi = getMonthWiFiDataUsage()

        sb.append("--- Today ---\n")
        sb.append("Mobile:\n")
        sb.append("  Received: ${formatBytes(todayMobile.first)}\n")
        sb.append("  Transmitted: ${formatBytes(todayMobile.second)}\n")
        sb.append("  Total: ${formatBytes(todayMobile.first + todayMobile.second)}\n\n")
        sb.append("WiFi:\n")
        sb.append("  Received: ${formatBytes(todayWiFi.first)}\n")
        sb.append("  Transmitted: ${formatBytes(todayWiFi.second)}\n")
        sb.append("  Total: ${formatBytes(todayWiFi.first + todayWiFi.second)}\n\n")

        sb.append("--- This Month ---\n")
        sb.append("Mobile:\n")
        sb.append("  Received: ${formatBytes(monthMobile.first)}\n")
        sb.append("  Transmitted: ${formatBytes(monthMobile.second)}\n")
        sb.append("  Total: ${formatBytes(monthMobile.first + monthMobile.second)}\n\n")
        sb.append("WiFi:\n")
        sb.append("  Received: ${formatBytes(monthWiFi.first)}\n")
        sb.append("  Transmitted: ${formatBytes(monthWiFi.second)}\n")
        sb.append("  Total: ${formatBytes(monthWiFi.first + monthWiFi.second)}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Format bytes to human-readable string
         */
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}
