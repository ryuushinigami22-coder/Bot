package com.sync.xxx.managers

import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellSignalStrength
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executor

/**
 * SignalStrengthManager.kt
 * Monitor cellular signal strength
 * Get signal level, dBm, ASU
 */
class SignalStrengthManager(private val context: Context) {

    private val TAG = "SignalStrengthManager"
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var signalStrengthListener: Any? = null
    private var currentSignalStrength: SignalStrength? = null
    private var onSignalStrengthChangedListener: ((SignalStrength) -> Unit)? = null

    /**
     * Get current signal strength level (0-4)
     */
    fun getSignalLevel(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentSignalStrength?.level ?: 0
            } else {
                // Fallback for older Android versions
                val cellInfoList = telephonyManager.allCellInfo
                if (cellInfoList != null && cellInfoList.isNotEmpty()) {
                    val cellInfo = cellInfoList[0]
                    getCellSignalStrength(cellInfo)?.level ?: 0
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signal level", e)
            0
        }
    }

    /**
     * Get signal strength in dBm
     */
    fun getSignalStrengthDbm(): Int {
        return try {
            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList != null && cellInfoList.isNotEmpty()) {
                val cellInfo = cellInfoList[0]
                getCellSignalStrength(cellInfo)?.dbm ?: -1
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signal strength dBm", e)
            -1
        }
    }

    /**
     * Get signal strength description
     */
    fun getSignalStrengthDescription(): String {
        return when (getSignalLevel()) {
            4 -> "Excellent"
            3 -> "Good"
            2 -> "Fair"
            1 -> "Poor"
            0 -> "None"
            else -> "Unknown"
        }
    }

    /**
     * Get cell signal strength from CellInfo
     */
    private fun getCellSignalStrength(cellInfo: CellInfo): CellSignalStrength? {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    cellInfo.cellSignalStrength
                }
                else -> {
                    // Use reflection for older versions
                    val method = cellInfo.javaClass.getMethod("getCellSignalStrength")
                    method.invoke(cellInfo) as? CellSignalStrength
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cell signal strength", e)
            null
        }
    }

    /**
     * Start monitoring signal strength changes
     */
    fun startMonitoring(onSignalStrengthChanged: (SignalStrength) -> Unit) {
        if (signalStrengthListener != null) {
            Log.w(TAG, "Already monitoring signal strength")
            return
        }

        onSignalStrengthChangedListener = onSignalStrengthChanged

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ uses TelephonyCallback
                val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        currentSignalStrength = signalStrength
                        Log.d(TAG, "Signal strength changed: level=${signalStrength.level}")
                        onSignalStrengthChangedListener?.invoke(signalStrength)
                    }
                }
                signalStrengthListener = callback
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
                Log.d(TAG, "Started monitoring signal strength (TelephonyCallback)")
            } else {
                // Older Android versions use PhoneStateListener
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        currentSignalStrength = signalStrength
                        Log.d(TAG, "Signal strength changed")
                        onSignalStrengthChangedListener?.invoke(signalStrength)
                    }
                }
                signalStrengthListener = listener
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                Log.d(TAG, "Started monitoring signal strength (PhoneStateListener)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting signal strength monitoring", e)
        }
    }

    /**
     * Stop monitoring signal strength changes
     */
    fun stopMonitoring() {
        signalStrengthListener?.let { listener ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    telephonyManager.unregisterTelephonyCallback(listener as TelephonyCallback)
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.listen(listener as PhoneStateListener, PhoneStateListener.LISTEN_NONE)
                }
                signalStrengthListener = null
                onSignalStrengthChangedListener = null
                Log.d(TAG, "Stopped monitoring signal strength")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping signal strength monitoring", e)
            }
        }
    }

    /**
     * Export signal strength as JSON
     */
    fun getSignalStrengthAsJson(): JSONObject {
        return JSONObject().apply {
            put("signalLevel", getSignalLevel())
            put("signalDescription", getSignalStrengthDescription())
            put("signalDbm", getSignalStrengthDbm())
        }
    }

    /**
     * Export signal strength info as text
     */
    fun exportSignalStrengthInfo(): String {
        val sb = StringBuilder()

        sb.append("Signal Strength\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Level: ${getSignalLevel()}/4\n")
        sb.append("Quality: ${getSignalStrengthDescription()}\n")
        sb.append("dBm: ${getSignalStrengthDbm()}\n")

        return sb.toString()
    }

    companion object {
        /**
         * Get signal level
         */
        fun getSignalLevel(context: Context): Int {
            return try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val cellInfoList = telephonyManager.allCellInfo
                if (cellInfoList != null && cellInfoList.isNotEmpty()) {
                    val cellInfo = cellInfoList[0]
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cellInfo.cellSignalStrength.level
                    } else {
                        0
                    }
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        }
    }
}
