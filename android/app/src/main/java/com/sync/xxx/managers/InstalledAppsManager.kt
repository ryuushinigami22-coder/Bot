package com.sync.xxx.managers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * InstalledAppsManager.kt
 * List all installed applications
 * System apps, user apps, permissions
 */
class InstalledAppsManager(private val context: Context) {

    private val TAG = "InstalledAppsManager"
    private val packageManager = context.packageManager

    /**
     * Get all installed apps
     */
    fun getAllApps(): List<AppInfo> {
        return try {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            packages.map { packageInfo ->
                createAppInfo(packageInfo)
            }.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps", e)
            emptyList()
        }
    }

    /**
     * Get user-installed apps only
     */
    fun getUserApps(): List<AppInfo> {
        return getAllApps().filter { !it.isSystemApp }
    }

    /**
     * Get system apps only
     */
    fun getSystemApps(): List<AppInfo> {
        return getAllApps().filter { it.isSystemApp }
    }

    /**
     * Get app by package name
     */
    fun getAppByPackage(packageName: String): AppInfo? {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            createAppInfo(packageInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app info for $packageName", e)
            null
        }
    }

    /**
     * Search apps by name
     */
    fun searchApps(query: String): List<AppInfo> {
        return getAllApps().filter { 
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get apps with specific permission
     */
    fun getAppsWithPermission(permission: String): List<AppInfo> {
        return getAllApps().filter { app ->
            app.permissions.any { it.contains(permission, ignoreCase = true) }
        }
    }

    /**
     * Get recently installed apps
     */
    fun getRecentlyInstalled(limit: Int = 10): List<AppInfo> {
        return getAllApps()
            .sortedByDescending { it.installTime }
            .take(limit)
    }

    /**
     * Get recently updated apps
     */
    fun getRecentlyUpdated(limit: Int = 10): List<AppInfo> {
        return getAllApps()
            .sortedByDescending { it.updateTime }
            .take(limit)
    }

    /**
     * Check if app is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get total app count
     */
    fun getTotalAppCount(): Int {
        return getAllApps().size
    }

    /**
     * Get user app count
     */
    fun getUserAppCount(): Int {
        return getUserApps().size
    }

    /**
     * Get system app count
     */
    fun getSystemAppCount(): Int {
        return getSystemApps().size
    }

    /**
     * Create AppInfo from PackageInfo
     */
    private fun createAppInfo(packageInfo: PackageInfo): AppInfo {
        val appInfo = packageInfo.applicationInfo
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        
        return AppInfo(
            appName = appName,
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = packageInfo.versionCode.toLong(),
            installTime = packageInfo.firstInstallTime,
            updateTime = packageInfo.lastUpdateTime,
            isSystemApp = isSystemApp,
            permissions = permissions
        )
    }

    /**
     * Export apps as JSON
     */
    fun getAppsAsJson(): JSONArray {
        val apps = getAllApps()
        val jsonArray = JSONArray()

        apps.forEach { app ->
            jsonArray.put(app.toJson())
        }

        return jsonArray
    }

    /**
     * Export apps as text
     */
    fun exportApps(): String {
        val apps = getAllApps()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("Installed Applications\n")
        sb.append("Total: ${apps.size}\n")
        sb.append("User Apps: ${getUserAppCount()}\n")
        sb.append("System Apps: ${getSystemAppCount()}\n")
        sb.append("=".repeat(60)).append("\n\n")

        apps.forEach { app ->
            sb.append("App: ${app.appName}\n")
            sb.append("Package: ${app.packageName}\n")
            sb.append("Version: ${app.versionName} (${app.versionCode})\n")
            sb.append("Type: ${if (app.isSystemApp) "System" else "User"}\n")
            sb.append("Installed: ${dateFormat.format(Date(app.installTime))}\n")
            sb.append("Updated: ${dateFormat.format(Date(app.updateTime))}\n")
            sb.append("Permissions: ${app.permissions.size}\n")
            sb.append("-".repeat(60)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Export app statistics
     */
    fun getAppStatistics(): AppStatistics {
        val allApps = getAllApps()
        val userApps = getUserApps()
        val systemApps = getSystemApps()
        
        return AppStatistics(
            totalCount = allApps.size,
            userCount = userApps.size,
            systemCount = systemApps.size,
            totalPermissions = allApps.sumOf { it.permissions.size }
        )
    }

    /**
     * Data class for app info
     */
    data class AppInfo(
        val appName: String,
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val installTime: Long,
        val updateTime: Long,
        val isSystemApp: Boolean,
        val permissions: List<String>
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("appName", appName)
                put("packageName", packageName)
                put("versionName", versionName)
                put("versionCode", versionCode)
                put("installTime", installTime)
                put("installDate", dateFormat.format(Date(installTime)))
                put("updateTime", updateTime)
                put("updateDate", dateFormat.format(Date(updateTime)))
                put("isSystemApp", isSystemApp)
                put("permissions", JSONArray(permissions))
                put("permissionCount", permissions.size)
            }
        }
    }

    /**
     * Data class for app statistics
     */
    data class AppStatistics(
        val totalCount: Int,
        val userCount: Int,
        val systemCount: Int,
        val totalPermissions: Int
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("totalCount", totalCount)
                put("userCount", userCount)
                put("systemCount", systemCount)
                put("totalPermissions", totalPermissions)
            }
        }
    }
}
