package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * CellInfoManager.kt
 * Get cell tower information
 * MCC, MNC, LAC, CID, signal strength per cell
 */
class CellInfoManager(private val context: Context) {

    private val TAG = "CellInfoManager"
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /**
     * Get all cell info
     */
    fun getAllCellInfo(): List<CellInfo> {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Location permission not granted")
                return emptyList()
            }
            telephonyManager.allCellInfo ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all cell info", e)
            emptyList()
        }
    }

    /**
     * Get registered cell info (current cell)
     */
    fun getRegisteredCellInfo(): CellInfo? {
        val allCells = getAllCellInfo()
        return allCells.firstOrNull { it.isRegistered }
    }

    /**
     * Get cell count
     */
    fun getCellCount(): Int {
        return getAllCellInfo().size
    }

    /**
     * Get cell info as JSON array
     */
    fun getCellInfoAsJsonArray(): JSONArray {
        val array = JSONArray()
        val cells = getAllCellInfo()

        cells.forEach { cellInfo ->
            val cellObj = JSONObject()

            cellObj.put("isRegistered", cellInfo.isRegistered)
            cellObj.put("timestampMillis", cellInfo.timeStamp)

            when (cellInfo) {
                is CellInfoGsm -> {
                    cellObj.put("type", "GSM")
                    val identity = cellInfo.cellIdentity
                    val signalStrength = cellInfo.cellSignalStrength

                    cellObj.put("mcc", identity.mcc)
                    cellObj.put("mnc", identity.mnc)
                    cellObj.put("lac", identity.lac)
                    cellObj.put("cid", identity.cid)
                    cellObj.put("signalStrength", signalStrength.dbm)
                    cellObj.put("signalLevel", signalStrength.level)
                }
                is CellInfoLte -> {
                    cellObj.put("type", "LTE")
                    val identity = cellInfo.cellIdentity
                    val signalStrength = cellInfo.cellSignalStrength

                    cellObj.put("mcc", identity.mcc)
                    cellObj.put("mnc", identity.mnc)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        cellObj.put("earfcn", identity.earfcn)
                    }
                    cellObj.put("pci", identity.pci)
                    cellObj.put("tac", identity.tac)
                    cellObj.put("ci", identity.ci)
                    cellObj.put("signalStrength", signalStrength.dbm)
                    cellObj.put("signalLevel", signalStrength.level)
                }
                is CellInfoWcdma -> {
                    cellObj.put("type", "WCDMA")
                    val identity = cellInfo.cellIdentity
                    val signalStrength = cellInfo.cellSignalStrength

                    cellObj.put("mcc", identity.mcc)
                    cellObj.put("mnc", identity.mnc)
                    cellObj.put("lac", identity.lac)
                    cellObj.put("cid", identity.cid)
                    cellObj.put("signalStrength", signalStrength.dbm)
                    cellObj.put("signalLevel", signalStrength.level)
                }
                else -> {
                    cellObj.put("type", "Unknown")
                }
            }

            array.put(cellObj)
        }

        return array
    }

    /**
     * Export cell info as text
     */
    fun exportCellInfo(): String {
        val sb = StringBuilder()

        sb.append("Cell Tower Information\n")
        sb.append("=".repeat(60)).append("\n\n")

        val cells = getAllCellInfo()
        
        if (cells.isEmpty()) {
            sb.append("No cell information available\n")
            sb.append("(Requires ACCESS_FINE_LOCATION permission)\n")
            return sb.toString()
        }

        sb.append("Total Cells: ${cells.size}\n\n")

        cells.forEachIndexed { index, cellInfo ->
            sb.append("--- Cell ${index + 1} ---\n")
            sb.append("Registered: ${if (cellInfo.isRegistered) "Yes" else "No"}\n")
            sb.append("Timestamp: ${cellInfo.timeStamp}\n")

            when (cellInfo) {
                is CellInfoGsm -> {
                    val identity = cellInfo.cellIdentity
                    val signalStrength = cellInfo.cellSignalStrength

                    sb.append("Type: GSM\n")
                    sb.append("MCC: ${identity.mcc}\n")
                    sb.append("MNC: ${identity.mnc}\n")
                    sb.append("LAC: ${identity.lac}\n")
                    sb.append("CID: ${identity.cid}\n")
                    sb.append("Signal: ${signalStrength.dbm} dBm (Level ${signalStrength.level})\n")
                }
                is CellInfoLte -> {
                    val identity = cellInfo.cellIdentity
                    val signalStrength = cellInfo.cellSignalStrength

                    sb.append("Type: LTE\n")
                    sb.append("MCC: ${identity.mcc}\n")
                    sb.append("MNC: ${identity.mnc}\n")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sb.append("EARFCN: ${identity.earfcn}\n")
                    }
                    sb.append("PCI: ${identity.pci}\n")
                    sb.append("TAC: ${identity.tac}\n")
                    sb.append("CI: ${identity.ci}\n")
                    sb.append("Signal: ${signalStrength.dbm} dBm (Level ${signalStrength.level})\n")
                }
                is CellInfoWcdma -> {
                    val identity = cellInfo.cellIdentity
                    val signalStrength = cellInfo.cellSignalStrength

                    sb.append("Type: WCDMA\n")
                    sb.append("MCC: ${identity.mcc}\n")
                    sb.append("MNC: ${identity.mnc}\n")
                    sb.append("LAC: ${identity.lac}\n")
                    sb.append("CID: ${identity.cid}\n")
                    sb.append("Signal: ${signalStrength.dbm} dBm (Level ${signalStrength.level})\n")
                }
                else -> {
                    sb.append("Type: Unknown\n")
                }
            }

            if (index < cells.size - 1) sb.append("\n")
        }

        return sb.toString()
    }

    companion object {
        /**
         * Get cell count
         */
        fun getCellCount(context: Context): Int {
            return try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return 0
                }
                telephonyManager.allCellInfo?.size ?: 0
            } catch (e: Exception) {
                0
            }
        }
    }
}
