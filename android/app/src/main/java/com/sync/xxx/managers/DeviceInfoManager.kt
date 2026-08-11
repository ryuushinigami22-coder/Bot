package com.sync.xxx.managers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import org.json.JSONObject
import java.util.*

/**
 * DeviceInfoManager.kt
 * Collect comprehensive device information
 * Hardware, software, and system details
 */
class DeviceInfoManager(private val context: Context) {

    private val TAG = "DeviceInfoManager"

    /**
     * Get all device information
     */
    fun getAllInfo(): DeviceInfo {
        return DeviceInfo(
            device = getDeviceInfo(),
            system = getSystemInfo(),
            display = getDisplayInfo(),
            storage = getStorageInfo(),
            network = getNetworkInfo(),
            identifier = getIdentifierInfo()
        )
    }

    /**
     * Get device hardware info
     */
    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "brand" to Build.BRAND,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "board" to Build.BOARD,
            "hardware" to Build.HARDWARE,
            "display" to Build.DISPLAY,
            "fingerprint" to Build.FINGERPRINT,
            "bootloader" to Build.BOOTLOADER,
            "radio" to (Build.getRadioVersion() ?: "Unknown")
        )
    }

    /**
     * Get system software info
     */
    private fun getSystemInfo(): Map<String, String> {
        return mapOf<String, String>(
            "androidVersion" to Build.VERSION.RELEASE,
            "sdkVersion" to Build.VERSION.SDK_INT.toString(),
            "buildId" to Build.ID,
            "buildTime" to Date(Build.TIME).toString(),
            "buildType" to Build.TYPE,
            "buildTags" to Build.TAGS,
            "buildHost" to Build.HOST.toString(),
            "buildUser" to Build.USER.toString()
            // TEMPORARY: Commented out to fix build - TODO: investigate type issue
            // "kernelVersion" to System.getProperty("os.version") ?: "Unknown",
            // "javaVm" to System.getProperty("java.vm.name") ?: "Unknown"
        )
    }

    /**
     * Get display info
     */
    private fun getDisplayInfo(): Map<String, String> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        return mapOf(
            "width" to metrics.widthPixels.toString(),
            "height" to metrics.heightPixels.toString(),
            "density" to metrics.density.toString(),
            "densityDpi" to metrics.densityDpi.toString(),
            "scaledDensity" to metrics.scaledDensity.toString(),
            "xdpi" to metrics.xdpi.toString(),
            "ydpi" to metrics.ydpi.toString(),
            "refreshRate" to windowManager.defaultDisplay.refreshRate.toString()
        )
    }

    /**
     * Get storage info
     */
    private fun getStorageInfo(): Map<String, String> {
        val internalStat = StatFs(Environment.getDataDirectory().path)
        val externalStat = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            StatFs(Environment.getExternalStorageDirectory().path)
        } else null

        val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong
        val internalAvailable = internalStat.availableBlocksLong * internalStat.blockSizeLong

        val map = mutableMapOf(
            "internalTotal" to formatBytes(internalTotal),
            "internalAvailable" to formatBytes(internalAvailable),
            "internalUsed" to formatBytes(internalTotal - internalAvailable)
        )

        if (externalStat != null) {
            val externalTotal = externalStat.blockCountLong * externalStat.blockSizeLong
            val externalAvailable = externalStat.availableBlocksLong * externalStat.blockSizeLong
            
            map["externalTotal"] = formatBytes(externalTotal)
            map["externalAvailable"] = formatBytes(externalAvailable)
            map["externalUsed"] = formatBytes(externalTotal - externalAvailable)
        }

        return map
    }

    /**
     * Get network info
     */
    @SuppressLint("HardwareIds")
    private fun getNetworkInfo(): Map<String, String> {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        val map = mutableMapOf<String, String>()

        if (telephonyManager != null) {
            map["networkOperatorName"] = telephonyManager.networkOperatorName ?: "Unknown"
            map["networkOperator"] = telephonyManager.networkOperator ?: "Unknown"
            map["simOperatorName"] = telephonyManager.simOperatorName ?: "Unknown"
            map["simOperator"] = telephonyManager.simOperator ?: "Unknown"
            map["simCountryIso"] = telephonyManager.simCountryIso ?: "Unknown"
            map["networkCountryIso"] = telephonyManager.networkCountryIso ?: "Unknown"
            map["phoneType"] = getPhoneTypeName(telephonyManager.phoneType)
            map["networkType"] = getNetworkTypeName(telephonyManager.networkType)
        }

        return map
    }

    /**
     * Get device identifiers
     */
    @SuppressLint("HardwareIds")
    private fun getIdentifierInfo(): Map<String, String> {
        return mapOf(
            "androidId" to (Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "Unknown"),
            "serialNumber" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { Build.getSerial() } catch (e: Exception) { "Unknown" }
            } else {
                @Suppress("DEPRECATION") Build.SERIAL
            })
        )
    }

    /**
     * Get phone type name
     */
    private fun getPhoneTypeName(type: Int): String {
        return when (type) {
            TelephonyManager.PHONE_TYPE_GSM -> "GSM"
            TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
            TelephonyManager.PHONE_TYPE_SIP -> "SIP"
            TelephonyManager.PHONE_TYPE_NONE -> "None"
            else -> "Unknown"
        }
    }

    /**
     * Get network type name
     */
    private fun getNetworkTypeName(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO 0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO A"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO B"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            else -> "Unknown"
        }
    }

    /**
     * Format bytes to readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Export device info as JSON
     */
    fun getDeviceInfoAsJson(): JSONObject {
        val info = getAllInfo()
        return info.toJson()
    }

    /**
     * Export device info as readable text
     */
    fun exportDeviceInfo(): String {
        val info = getAllInfo()
        val sb = StringBuilder()

        sb.append("=== DEVICE INFORMATION ===\n\n")

        sb.append("--- Device ---\n")
        info.device.forEach { (key, value) ->
            sb.append("$key: $value\n")
        }

        sb.append("\n--- System ---\n")
        info.system.forEach { (key, value) ->
            sb.append("$key: $value\n")
        }

        sb.append("\n--- Display ---\n")
        info.display.forEach { (key, value) ->
            sb.append("$key: $value\n")
        }

        sb.append("\n--- Storage ---\n")
        info.storage.forEach { (key, value) ->
            sb.append("$key: $value\n")
        }

        sb.append("\n--- Network ---\n")
        info.network.forEach { (key, value) ->
            sb.append("$key: $value\n")
        }

        sb.append("\n--- Identifiers ---\n")
        info.identifier.forEach { (key, value) ->
            sb.append("$key: $value\n")
        }

        return sb.toString()
    }

    /**
     * Data class for device information
     */
    data class DeviceInfo(
        val device: Map<String, String>,
        val system: Map<String, String>,
        val display: Map<String, String>,
        val storage: Map<String, String>,
        val network: Map<String, String>,
        val identifier: Map<String, String>
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("device", JSONObject(device))
                put("system", JSONObject(system))
                put("display", JSONObject(display))
                put("storage", JSONObject(storage))
                put("network", JSONObject(network))
                put("identifier", JSONObject(identifier))
            }
        }
    }
}
