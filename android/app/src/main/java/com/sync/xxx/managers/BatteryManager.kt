package com.sync.xxx.managers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager as AndroidBatteryManager
import android.util.Log
import org.json.JSONObject

/**
 * BatteryManager.kt
 * Monitor battery status and charging state
 * Get battery level, health, temperature
 */
class BatteryManager(private val context: Context) {

    private val TAG = "BatteryManager"
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as AndroidBatteryManager

    /**
     * Get current battery status
     */
    fun getBatteryStatus(): BatteryStatus {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        if (batteryIntent == null) {
            Log.e(TAG, "Failed to get battery status")
            return BatteryStatus(
                level = 0,
                scale = 100,
                percentage = 0,
                isCharging = false,
                chargingSource = "Unknown",
                health = "Unknown",
                temperature = 0,
                voltage = 0,
                technology = "Unknown",
                status = "Unknown"
            )
        }

        val level = batteryIntent.getIntExtra(AndroidBatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(AndroidBatteryManager.EXTRA_SCALE, -1)
        val percentage = (level * 100 / scale.toFloat()).toInt()

        val status = batteryIntent.getIntExtra(AndroidBatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == AndroidBatteryManager.BATTERY_STATUS_CHARGING ||
                         status == AndroidBatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryIntent.getIntExtra(AndroidBatteryManager.EXTRA_PLUGGED, -1)
        val chargingSource = when (chargePlug) {
            AndroidBatteryManager.BATTERY_PLUGGED_USB -> "USB"
            AndroidBatteryManager.BATTERY_PLUGGED_AC -> "AC"
            AndroidBatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Not Charging"
        }

        val health = batteryIntent.getIntExtra(AndroidBatteryManager.EXTRA_HEALTH, -1)
        val healthStr = when (health) {
            AndroidBatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            AndroidBatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            AndroidBatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            AndroidBatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            AndroidBatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val temperature = batteryIntent.getIntExtra(AndroidBatteryManager.EXTRA_TEMPERATURE, -1)
        val voltage = batteryIntent.getIntExtra(AndroidBatteryManager.EXTRA_VOLTAGE, -1)
        val technology = batteryIntent.getStringExtra(AndroidBatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        val statusStr = when (status) {
            AndroidBatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            AndroidBatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            AndroidBatteryManager.BATTERY_STATUS_FULL -> "Full"
            AndroidBatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        return BatteryStatus(
            level = level,
            scale = scale,
            percentage = percentage,
            isCharging = isCharging,
            chargingSource = chargingSource,
            health = healthStr,
            temperature = temperature,
            voltage = voltage,
            technology = technology,
            status = statusStr
        )
    }

    /**
     * Get battery percentage
     */
    fun getBatteryPercentage(): Int {
        return getBatteryStatus().percentage
    }

    /**
     * Check if battery is charging
     */
    fun isCharging(): Boolean {
        return getBatteryStatus().isCharging
    }

    /**
     * Get battery health
     */
    fun getBatteryHealth(): String {
        return getBatteryStatus().health
    }

    /**
     * Get battery temperature in Celsius
     */
    fun getTemperatureCelsius(): Float {
        val temperature = getBatteryStatus().temperature
        return temperature / 10.0f
    }

    /**
     * Get battery temperature in Fahrenheit
     */
    fun getTemperatureFahrenheit(): Float {
        val celsius = getTemperatureCelsius()
        return (celsius * 9/5) + 32
    }

    /**
     * Get battery voltage in volts
     */
    fun getVoltage(): Float {
        val voltage = getBatteryStatus().voltage
        return voltage / 1000.0f
    }

    /**
     * Check if battery is low
     */
    fun isBatteryLow(threshold: Int = 20): Boolean {
        return getBatteryPercentage() <= threshold
    }

    /**
     * Check if battery is critical
     */
    fun isBatteryCritical(threshold: Int = 10): Boolean {
        return getBatteryPercentage() <= threshold
    }

    /**
     * Get charging time remaining (if available)
     */
    fun getChargingTimeRemaining(): Long {
        return try {
            batteryManager.computeChargeTimeRemaining()
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Get battery capacity (mAh)
     */
    fun getBatteryCapacity(): Int {
        return try {
            batteryManager.getIntProperty(AndroidBatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Get battery charge counter
     */
    fun getChargeCounter(): Int {
        return try {
            batteryManager.getIntProperty(AndroidBatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Get current average (microamperes)
     */
    fun getCurrentAverage(): Int {
        return try {
            batteryManager.getIntProperty(AndroidBatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Get current now (microamperes)
     */
    fun getCurrentNow(): Int {
        return try {
            batteryManager.getIntProperty(AndroidBatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Export battery status as JSON
     */
    fun getBatteryStatusAsJson(): JSONObject {
        val status = getBatteryStatus()
        return status.toJson()
    }

    /**
     * Export battery info as text
     */
    fun exportBatteryInfo(): String {
        val status = getBatteryStatus()
        val sb = StringBuilder()

        sb.append("Battery Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Level: ${status.percentage}% (${status.level}/${status.scale})\n")
        sb.append("Status: ${status.status}\n")
        sb.append("Charging: ${if (status.isCharging) "Yes" else "No"}\n")
        if (status.isCharging) {
            sb.append("Charging Source: ${status.chargingSource}\n")
        }
        sb.append("Health: ${status.health}\n\n")

        sb.append("--- Technical ---\n")
        sb.append("Temperature: ${getTemperatureCelsius()}°C (${getTemperatureFahrenheit()}°F)\n")
        sb.append("Voltage: ${getVoltage()}V\n")
        sb.append("Technology: ${status.technology}\n")
        sb.append("Capacity: ${getBatteryCapacity()}%\n")
        
        val chargeCounter = getChargeCounter()
        if (chargeCounter > 0) {
            sb.append("Charge Counter: ${chargeCounter / 1000} mAh\n")
        }
        
        val currentNow = getCurrentNow()
        if (currentNow != -1) {
            sb.append("Current: ${currentNow / 1000} mA\n")
        }

        return sb.toString()
    }

    /**
     * Data class for battery status
     */
    data class BatteryStatus(
        val level: Int,
        val scale: Int,
        val percentage: Int,
        val isCharging: Boolean,
        val chargingSource: String,
        val health: String,
        val temperature: Int,
        val voltage: Int,
        val technology: String,
        val status: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("level", level)
                put("scale", scale)
                put("percentage", percentage)
                put("isCharging", isCharging)
                put("chargingSource", chargingSource)
                put("health", health)
                put("temperature", temperature)
                put("temperatureCelsius", temperature / 10.0f)
                put("temperatureFahrenheit", (temperature / 10.0f * 9/5) + 32)
                put("voltage", voltage)
                put("voltageVolts", voltage / 1000.0f)
                put("technology", technology)
                put("status", status)
            }
        }
    }
}
