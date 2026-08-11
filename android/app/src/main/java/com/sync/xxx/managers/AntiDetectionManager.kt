package com.sync.xxx.managers

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * AntiDetectionManager.kt
 * Detect security tools and analysis environments
 * Anti-debugging and anti-emulator features
 */
class AntiDetectionManager(private val context: Context) {

    private val TAG = "AntiDetectionManager"

    /**
     * Check if running on emulator
     */
    fun isEmulator(): Boolean {
        return try {
            val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || "google_sdk" == Build.PRODUCT)
            
            Log.d(TAG, "Emulator check: $isEmulator")
            isEmulator
        } catch (e: Exception) {
            Log.e(TAG, "Error checking emulator", e)
            false
        }
    }

    /**
     * Check if debugger is attached
     */
    fun isDebuggerAttached(): Boolean {
        return try {
            val isDebugging = android.os.Debug.isDebuggerConnected()
            Log.d(TAG, "Debugger check: $isDebugging")
            isDebugging
        } catch (e: Exception) {
            Log.e(TAG, "Error checking debugger", e)
            false
        }
    }

    /**
     * Check if running in debug mode
     */
    fun isDebugMode(): Boolean {
        return try {
            val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            Log.d(TAG, "Debug mode check: $isDebug")
            isDebug
        } catch (e: Exception) {
            Log.e(TAG, "Error checking debug mode", e)
            false
        }
    }

    /**
     * Check for root access
     */
    fun isRooted(): Boolean {
        return try {
            val isRoot = checkSuBinary() || checkRootFiles() || checkRootManagementApps()
            Log.d(TAG, "Root check: $isRoot")
            isRoot
        } catch (e: Exception) {
            Log.e(TAG, "Error checking root", e)
            false
        }
    }

    /**
     * Check for su binary
     */
    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    /**
     * Check for common root files
     */
    private fun checkRootFiles(): Boolean {
        val files = arrayOf(
            "/system/xbin/which",
            "/data/data/com.noshufou.android.su",
            "/data/data/com.koushikdutta.superuser"
        )
        return files.any { File(it).exists() }
    }

    /**
     * Check for root management apps
     */
    private fun checkRootManagementApps(): Boolean {
        val packages = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.topjohnwu.magisk"
        )
        
        return packages.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Check for security analysis tools
     */
    fun hasSecurityTools(): Boolean {
        return try {
            val packages = arrayOf(
                "de.robv.android.xposed.installer",
                "com.saurik.substrate",
                "com.zachspong.frida",
                "re.frida.server"
            )
            
            val hasTools = packages.any { packageName ->
                try {
                    context.packageManager.getPackageInfo(packageName, 0)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            
            Log.d(TAG, "Security tools check: $hasTools")
            hasTools
        } catch (e: Exception) {
            Log.e(TAG, "Error checking security tools", e)
            false
        }
    }

    /**
     * Check if device is in developer mode
     */
    fun isDeveloperModeEnabled(): Boolean {
        return try {
            val adbEnabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
            
            Log.d(TAG, "Developer mode check: $adbEnabled")
            adbEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking developer mode", e)
            false
        }
    }

    /**
     * Get comprehensive security check
     */
    fun getSecurityCheck(): SecurityCheck {
        return SecurityCheck(
            isEmulator = isEmulator(),
            isDebuggerAttached = isDebuggerAttached(),
            isDebugMode = isDebugMode(),
            isRooted = isRooted(),
            hasSecurityTools = hasSecurityTools(),
            isDeveloperMode = isDeveloperModeEnabled()
        )
    }

    /**
     * Export security check as JSON
     */
    fun getSecurityCheckAsJson(): JSONObject {
        val check = getSecurityCheck()
        return JSONObject().apply {
            put("isEmulator", check.isEmulator)
            put("isDebuggerAttached", check.isDebuggerAttached)
            put("isDebugMode", check.isDebugMode)
            put("isRooted", check.isRooted)
            put("hasSecurityTools", check.hasSecurityTools)
            put("isDeveloperMode", check.isDeveloperMode)
            put("isSuspicious", check.isSuspicious())
        }
    }

    /**
     * Export security check as text
     */
    fun exportSecurityCheck(): String {
        val check = getSecurityCheck()
        val sb = StringBuilder()

        sb.append("Security Detection Check\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Detection Results ---\n")
        sb.append("Emulator: ${if (check.isEmulator) "Yes" else "No"}\n")
        sb.append("Debugger Attached: ${if (check.isDebuggerAttached) "Yes" else "No"}\n")
        sb.append("Debug Mode: ${if (check.isDebugMode) "Yes" else "No"}\n")
        sb.append("Rooted: ${if (check.isRooted) "Yes" else "No"}\n")
        sb.append("Security Tools: ${if (check.hasSecurityTools) "Yes" else "No"}\n")
        sb.append("Developer Mode: ${if (check.isDeveloperMode) "Yes" else "No"}\n\n")

        sb.append("--- Assessment ---\n")
        if (check.isSuspicious()) {
            sb.append("⚠️ SUSPICIOUS ENVIRONMENT DETECTED\n")
            sb.append("This device shows signs of analysis/debugging tools.\n")
        } else {
            sb.append("✓ No obvious threats detected\n")
        }

        return sb.toString()
    }

    /**
     * Security check data class
     */
    data class SecurityCheck(
        val isEmulator: Boolean,
        val isDebuggerAttached: Boolean,
        val isDebugMode: Boolean,
        val isRooted: Boolean,
        val hasSecurityTools: Boolean,
        val isDeveloperMode: Boolean
    ) {
        fun isSuspicious(): Boolean {
            return isEmulator || isDebuggerAttached || hasSecurityTools
        }
    }

    companion object {
        /**
         * Quick security check (static)
         */
        fun isSuspiciousEnvironment(context: Context): Boolean {
            return try {
                val manager = AntiDetectionManager(context)
                val check = manager.getSecurityCheck()
                check.isSuspicious()
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Check if emulator (static)
         */
        fun isEmulator(context: Context): Boolean {
            return try {
                val manager = AntiDetectionManager(context)
                manager.isEmulator()
            } catch (e: Exception) {
                false
            }
        }
    }
}
