package com.sync.xxx.managers

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * SimCardManager.kt
 * Get SIM card information
 * Carrier name, phone number, IMSI, serial number
 */
class SimCardManager(private val context: Context) {

    private val TAG = "SimCardManager"
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /**
     * Check if SIM card is present
     */
    fun hasSim(): Boolean {
        return telephonyManager.simState != TelephonyManager.SIM_STATE_ABSENT
    }

    /**
     * Get SIM state
     */
    fun getSimState(): String {
        return when (telephonyManager.simState) {
            TelephonyManager.SIM_STATE_ABSENT -> "Absent"
            TelephonyManager.SIM_STATE_UNKNOWN -> "Unknown"
            TelephonyManager.SIM_STATE_READY -> "Ready"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
            else -> "Unknown"
        }
    }

    /**
     * Get carrier name
     */
    fun getCarrierName(): String {
        return telephonyManager.networkOperatorName ?: "Unknown"
    }

    /**
     * Get phone number
     */
    fun getPhoneNumber(): String? {
        return try {
            telephonyManager.line1Number
        } catch (e: Exception) {
            Log.e(TAG, "Error getting phone number", e)
            null
        }
    }

    /**
     * Get IMSI (International Mobile Subscriber Identity)
     */
    fun getIMSI(): String? {
        return try {
            telephonyManager.subscriberId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IMSI", e)
            null
        }
    }

    /**
     * Get SIM serial number (ICCID)
     */
    fun getSimSerialNumber(): String? {
        return try {
            telephonyManager.simSerialNumber
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SIM serial number", e)
            null
        }
    }

    /**
     * Get country code (ISO)
     */
    fun getCountryCode(): String {
        return telephonyManager.simCountryIso?.uppercase() ?: "Unknown"
    }

    /**
     * Get network operator
     */
    fun getNetworkOperator(): String {
        return telephonyManager.networkOperator ?: "Unknown"
    }

    /**
     * Get SIM operator
     */
    fun getSimOperator(): String {
        return telephonyManager.simOperator ?: "Unknown"
    }

    /**
     * Get SIM operator name
     */
    fun getSimOperatorName(): String {
        return telephonyManager.simOperatorName ?: "Unknown"
    }

    /**
     * Get all active subscriptions (for dual SIM devices)
     */
    fun getActiveSubscriptions(): List<SubscriptionInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscriptions", e)
            emptyList()
        }
    }

    /**
     * Get number of active SIM cards
     */
    fun getActiveSimCount(): Int {
        return getActiveSubscriptions().size
    }

    /**
     * Check if device has dual SIM
     */
    fun hasDualSim(): Boolean {
        return getActiveSimCount() >= 2
    }

    /**
     * Export SIM info as JSON
     */
    fun getSimInfoAsJson(): JSONObject {
        return JSONObject().apply {
            put("hasSim", hasSim())
            put("simState", getSimState())
            put("carrierName", getCarrierName())
            put("phoneNumber", getPhoneNumber())
            put("imsi", getIMSI())
            put("serialNumber", getSimSerialNumber())
            put("countryCode", getCountryCode())
            put("networkOperator", getNetworkOperator())
            put("simOperator", getSimOperator())
            put("simOperatorName", getSimOperatorName())
            put("activeSimCount", getActiveSimCount())
            put("hasDualSim", hasDualSim())
            
            // Add subscriptions array
            val subscriptionsArray = JSONArray()
            getActiveSubscriptions().forEach { sub ->
                val subObj = JSONObject().apply {
                    put("slotIndex", sub.simSlotIndex)
                    put("displayName", sub.displayName)
                    put("carrierName", sub.carrierName)
                    put("number", sub.number)
                    put("countryIso", sub.countryIso)
                }
                subscriptionsArray.put(subObj)
            }
            put("subscriptions", subscriptionsArray)
        }
    }

    /**
     * Export SIM info as text
     */
    fun exportSimInfo(): String {
        val sb = StringBuilder()

        sb.append("SIM Card Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("SIM Present: ${if (hasSim()) "Yes" else "No"}\n")
        sb.append("SIM State: ${getSimState()}\n")
        sb.append("Active SIMs: ${getActiveSimCount()}\n")
        sb.append("Dual SIM: ${if (hasDualSim()) "Yes" else "No"}\n\n")

        sb.append("--- Carrier ---\n")
        sb.append("Carrier: ${getCarrierName()}\n")
        sb.append("Operator: ${getSimOperatorName()}\n")
        sb.append("Country: ${getCountryCode()}\n\n")

        sb.append("--- Details ---\n")
        sb.append("Phone Number: ${getPhoneNumber() ?: "Not available"}\n")
        sb.append("IMSI: ${getIMSI() ?: "Not available"}\n")
        sb.append("Serial: ${getSimSerialNumber() ?: "Not available"}\n")
        sb.append("Network Operator: ${getNetworkOperator()}\n")
        sb.append("SIM Operator: ${getSimOperator()}\n\n")

        // Add subscription details if dual SIM
        val subscriptions = getActiveSubscriptions()
        if (subscriptions.isNotEmpty()) {
            sb.append("--- Subscriptions ---\n")
            subscriptions.forEachIndexed { index, sub ->
                sb.append("SIM ${index + 1}:\n")
                sb.append("  Slot: ${sub.simSlotIndex}\n")
                sb.append("  Name: ${sub.displayName}\n")
                sb.append("  Carrier: ${sub.carrierName}\n")
                sb.append("  Number: ${sub.number ?: "Not available"}\n")
                sb.append("  Country: ${sub.countryIso}\n")
                if (index < subscriptions.size - 1) sb.append("\n")
            }
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if SIM card is present
         */
        fun hasSim(context: Context): Boolean {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.simState != TelephonyManager.SIM_STATE_ABSENT
        }

        /**
         * Get carrier name
         */
        fun getCarrierName(context: Context): String {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.networkOperatorName ?: "Unknown"
        }
    }
}
