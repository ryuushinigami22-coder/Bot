package com.sync.xxx.managers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * SensorManager.kt
 * Access device sensors (accelerometer, gyroscope, light, proximity, etc.)
 * Monitor sensor data and availability
 */
class SensorManager(private val context: Context) : SensorEventListener {

    private val TAG = "SensorManager"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager

    private val sensorData = mutableMapOf<Int, FloatArray>()
    private var sensorListener: ((Int, FloatArray) -> Unit)? = null

    /**
     * Get all available sensors
     */
    fun getAllSensors(): List<SensorInfo> {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                type = sensor.type,
                typeName = getSensorTypeName(sensor.type),
                vendor = sensor.vendor,
                version = sensor.version,
                power = sensor.power,
                resolution = sensor.resolution,
                maxRange = sensor.maximumRange
            )
        }
    }

    /**
     * Check if specific sensor is available
     */
    fun hasSensor(sensorType: Int): Boolean {
        return sensorManager.getDefaultSensor(sensorType) != null
    }

    /**
     * Start listening to sensor
     */
    fun startListening(sensorType: Int, samplingPeriod: Int = AndroidSensorManager.SENSOR_DELAY_NORMAL): Boolean {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            Log.e(TAG, "Sensor type $sensorType not available")
            return false
        }

        return sensorManager.registerListener(this, sensor, samplingPeriod)
    }

    /**
     * Stop listening to sensor
     */
    fun stopListening(sensorType: Int) {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor != null) {
            sensorManager.unregisterListener(this, sensor)
        }
    }

    /**
     * Stop all sensor listeners
     */
    fun stopAllListeners() {
        sensorManager.unregisterListener(this)
        sensorData.clear()
    }

    /**
     * Set sensor data listener
     */
    fun setSensorListener(listener: (sensorType: Int, values: FloatArray) -> Unit) {
        sensorListener = listener
    }

    /**
     * Get latest sensor data
     */
    fun getSensorData(sensorType: Int): FloatArray? {
        return sensorData[sensorType]
    }

    /**
     * SensorEventListener implementation
     */
    override fun onSensorChanged(event: SensorEvent) {
        sensorData[event.sensor.type] = event.values.clone()
        sensorListener?.invoke(event.sensor.type, event.values)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d(TAG, "Sensor ${sensor.name} accuracy changed to $accuracy")
    }

    /**
     * Get sensor type name
     */
    private fun getSensorTypeName(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_PRESSURE -> "Pressure"
            Sensor.TYPE_TEMPERATURE -> "Temperature"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            Sensor.TYPE_HEART_RATE -> "Heart Rate"
            else -> "Unknown ($type)"
        }
    }

    /**
     * Get accelerometer data
     */
    fun getAccelerometer(): FloatArray? {
        return getSensorData(Sensor.TYPE_ACCELEROMETER)
    }

    /**
     * Get gyroscope data
     */
    fun getGyroscope(): FloatArray? {
        return getSensorData(Sensor.TYPE_GYROSCOPE)
    }

    /**
     * Get light sensor data
     */
    fun getLight(): Float? {
        return getSensorData(Sensor.TYPE_LIGHT)?.get(0)
    }

    /**
     * Get proximity sensor data
     */
    fun getProximity(): Float? {
        return getSensorData(Sensor.TYPE_PROXIMITY)?.get(0)
    }

    /**
     * Get magnetic field data
     */
    fun getMagneticField(): FloatArray? {
        return getSensorData(Sensor.TYPE_MAGNETIC_FIELD)
    }

    /**
     * Get pressure data
     */
    fun getPressure(): Float? {
        return getSensorData(Sensor.TYPE_PRESSURE)?.get(0)
    }

    /**
     * Export sensors as JSON
     */
    fun getSensorsAsJson(): JSONArray {
        val sensors = getAllSensors()
        val jsonArray = JSONArray()
        sensors.forEach { sensor ->
            jsonArray.put(sensor.toJson())
        }
        return jsonArray
    }

    /**
     * Export current sensor data as JSON
     */
    fun getSensorDataAsJson(): JSONObject {
        val jsonObject = JSONObject()
        sensorData.forEach { (type, values) ->
            jsonObject.put(getSensorTypeName(type), JSONArray(values.toList()))
        }
        return jsonObject
    }

    /**
     * Export sensor info as text
     */
    fun exportSensorInfo(): String {
        val sensors = getAllSensors()
        val sb = StringBuilder()

        sb.append("Device Sensors\n")
        sb.append("Total: ${sensors.size}\n")
        sb.append("=".repeat(60)).append("\n\n")

        val sensorsByType = sensors.groupBy { it.typeName }
        
        sensorsByType.forEach { (typeName, sensorList) ->
            sb.append("--- $typeName (${sensorList.size}) ---\n")
            sensorList.forEach { sensor ->
                sb.append("Name: ${sensor.name}\n")
                sb.append("Vendor: ${sensor.vendor}\n")
                sb.append("Version: ${sensor.version}\n")
                sb.append("Power: ${sensor.power} mA\n")
                sb.append("Max Range: ${sensor.maxRange}\n")
                sb.append("Resolution: ${sensor.resolution}\n")
                sb.append("-".repeat(60)).append("\n")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Get common sensors availability
     */
    fun getCommonSensorsAvailability(): Map<String, Boolean> {
        return mapOf(
            "Accelerometer" to hasSensor(Sensor.TYPE_ACCELEROMETER),
            "Gyroscope" to hasSensor(Sensor.TYPE_GYROSCOPE),
            "Magnetic Field" to hasSensor(Sensor.TYPE_MAGNETIC_FIELD),
            "Light" to hasSensor(Sensor.TYPE_LIGHT),
            "Proximity" to hasSensor(Sensor.TYPE_PROXIMITY),
            "Pressure" to hasSensor(Sensor.TYPE_PRESSURE),
            "Temperature" to hasSensor(Sensor.TYPE_TEMPERATURE),
            "Gravity" to hasSensor(Sensor.TYPE_GRAVITY),
            "Step Counter" to hasSensor(Sensor.TYPE_STEP_COUNTER),
            "Heart Rate" to hasSensor(Sensor.TYPE_HEART_RATE)
        )
    }

    /**
     * Data class for sensor info
     */
    data class SensorInfo(
        val name: String,
        val type: Int,
        val typeName: String,
        val vendor: String,
        val version: Int,
        val power: Float,
        val resolution: Float,
        val maxRange: Float
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("name", name)
                put("type", type)
                put("typeName", typeName)
                put("vendor", vendor)
                put("version", version)
                put("power", power)
                put("resolution", resolution)
                put("maxRange", maxRange)
            }
        }
    }
}
