package com.sync.xxx.managers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * BluetoothManager.kt
 * Access Bluetooth information and paired devices
 * Scan for nearby devices, get connection info
 */
class BluetoothManager(private val context: Context) {

    private val TAG = "BluetoothManager"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    /**
     * Check if Bluetooth permission is granted
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if device supports Bluetooth
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Enable Bluetooth
     */
    fun enableBluetooth(): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return false
        }

        return try {
            bluetoothAdapter?.enable() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling Bluetooth", e)
            false
        }
    }

    /**
     * Disable Bluetooth
     */
    fun disableBluetooth(): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return false
        }

        return try {
            bluetoothAdapter?.disable() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling Bluetooth", e)
            false
        }
    }

    /**
     * Get paired/bonded devices
     */
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        if (!hasPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return emptyList()
        }

        return try {
            val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            pairedDevices.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown",
                    address = device.address,
                    bondState = getBondStateName(device.bondState),
                    type = getDeviceTypeName(device.type)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paired devices", e)
            emptyList()
        }
    }

    /**
     * Get Bluetooth adapter name
     */
    fun getAdapterName(): String? {
        if (!hasPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return null
        }

        return bluetoothAdapter?.name
    }

    /**
     * Get Bluetooth adapter address
     */
    fun getAdapterAddress(): String? {
        if (!hasPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return null
        }

        return try {
            bluetoothAdapter?.address
        } catch (e: Exception) {
            Log.e(TAG, "Error getting adapter address", e)
            null
        }
    }

    /**
     * Check if discovering
     */
    fun isDiscovering(): Boolean {
        return bluetoothAdapter?.isDiscovering == true
    }

    /**
     * Start device discovery
     */
    fun startDiscovery(): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return false
        }

        return try {
            bluetoothAdapter?.startDiscovery() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
            false
        }
    }

    /**
     * Stop device discovery
     */
    fun stopDiscovery(): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Bluetooth permission not granted")
            return false
        }

        return try {
            bluetoothAdapter?.cancelDiscovery() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
            false
        }
    }

    /**
     * Get bond state name
     */
    private fun getBondStateName(bondState: Int): String {
        return when (bondState) {
            BluetoothDevice.BOND_NONE -> "Not Bonded"
            BluetoothDevice.BOND_BONDING -> "Bonding"
            BluetoothDevice.BOND_BONDED -> "Bonded"
            else -> "Unknown"
        }
    }

    /**
     * Get device type name
     */
    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            else -> "Unknown"
        }
    }

    /**
     * Get total paired device count
     */
    fun getPairedDeviceCount(): Int {
        return getPairedDevices().size
    }

    /**
     * Export Bluetooth info as JSON
     */
    fun getBluetoothInfoAsJson(): JSONObject {
        return JSONObject().apply {
            put("isSupported", isBluetoothSupported())
            put("isEnabled", isBluetoothEnabled())
            put("adapterName", getAdapterName())
            put("adapterAddress", getAdapterAddress())
            put("isDiscovering", isDiscovering())
            put("pairedDeviceCount", getPairedDeviceCount())
        }
    }

    /**
     * Export paired devices as JSON
     */
    fun getPairedDevicesAsJson(): JSONArray {
        val devices = getPairedDevices()
        val jsonArray = JSONArray()
        devices.forEach { device ->
            jsonArray.put(device.toJson())
        }
        return jsonArray
    }

    /**
     * Export Bluetooth info as text
     */
    fun exportBluetoothInfo(): String {
        val sb = StringBuilder()

        sb.append("Bluetooth Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Adapter Info ---\n")
        sb.append("Supported: ${isBluetoothSupported()}\n")
        sb.append("Enabled: ${isBluetoothEnabled()}\n")
        sb.append("Name: ${getAdapterName() ?: "Unknown"}\n")
        sb.append("Address: ${getAdapterAddress() ?: "Unknown"}\n")
        sb.append("Discovering: ${isDiscovering()}\n\n")

        val pairedDevices = getPairedDevices()
        sb.append("--- Paired Devices (${pairedDevices.size}) ---\n")
        if (pairedDevices.isEmpty()) {
            sb.append("No paired devices\n")
        } else {
            pairedDevices.forEach { device ->
                sb.append("Name: ${device.name}\n")
                sb.append("Address: ${device.address}\n")
                sb.append("Type: ${device.type}\n")
                sb.append("Bond State: ${device.bondState}\n")
                sb.append("-".repeat(60)).append("\n")
            }
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if Bluetooth permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Data class for Bluetooth device info
     */
    data class BluetoothDeviceInfo(
        val name: String,
        val address: String,
        val bondState: String,
        val type: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("name", name)
                put("address", address)
                put("bondState", bondState)
                put("type", type)
            }
        }
    }
}
