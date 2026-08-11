package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.json.JSONObject

/**
 * LocationTracker.kt
 * GPS and Network location tracking
 * Real-time location updates
 */
class LocationTracker(private val context: Context) {

    private val TAG = "LocationTracker"
    
    private var locationManager: LocationManager? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationListener: ((Location) -> Unit)? = null
    
    private var isTracking = false
    private var lastKnownLocation: Location? = null
    
    // Update interval in milliseconds
    private var updateInterval: Long = 10000 // 10 seconds
    private var fastestInterval: Long = 5000 // 5 seconds
    
    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Start location tracking
     * @param callback Callback for location updates
     * @param intervalMs Update interval in milliseconds (default: 10000)
     */
    fun startTracking(callback: (Location) -> Unit, intervalMs: Long = 10000) {
        if (isTracking) {
            Log.w(TAG, "Already tracking")
            return
        }

        if (!hasPermissions()) {
            Log.e(TAG, "Location permissions not granted")
            return
        }

        locationListener = callback
        updateInterval = intervalMs
        fastestInterval = intervalMs / 2

        try {
            // Try using FusedLocationProvider first (more accurate)
            startFusedLocationTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start fused location tracking, falling back to LocationManager", e)
            // Fallback to LocationManager
            startLocationManagerTracking()
        }
    }

    /**
     * Stop location tracking
     */
    fun stopTracking() {
        if (!isTracking) {
            return
        }

        try {
            if (locationCallback != null) {
                fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            }
            
            locationManager?.removeUpdates(locationManagerListener)
            
            isTracking = false
            locationListener = null
            locationCallback = null
            
            Log.d(TAG, "Location tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking", e)
        }
    }

    /**
     * Start FusedLocationProvider tracking
     */
    private fun startFusedLocationTracking() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Location permission not granted")
        }

        val locationRequest = LocationRequest.create().apply {
            interval = updateInterval
            fastestInterval = fastestInterval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    lastKnownLocation = location
                    locationListener?.invoke(location)
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        isTracking = true
        Log.d(TAG, "Fused location tracking started")
    }

    /**
     * Start LocationManager tracking (fallback)
     */
    private fun startLocationManagerTracking() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                updateInterval,
                0f,
                locationManagerListener,
                Looper.getMainLooper()
            )

            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                updateInterval,
                0f,
                locationManagerListener,
                Looper.getMainLooper()
            )

            isTracking = true
            Log.d(TAG, "LocationManager tracking started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocationManager tracking", e)
        }
    }

    /**
     * LocationManager listener
     */
    private val locationManagerListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastKnownLocation = location
            locationListener?.invoke(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    /**
     * Get last known location
     */
    fun getLastLocation(): Location? {
        if (!hasPermissions()) {
            return null
        }

        if (lastKnownLocation != null) {
            return lastKnownLocation
        }

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                
                lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                return lastKnownLocation
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last location", e)
        }

        return null
    }

    /**
     * Check if tracking is active
     */
    fun isTracking(): Boolean = isTracking

    /**
     * Check if location permissions are granted
     */
    private fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopTracking()
    }

    companion object {
        /**
         * Convert Location to JSON
         */
        fun locationToJson(location: Location): JSONObject {
            return JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("altitude", location.altitude)
                put("accuracy", location.accuracy)
                put("bearing", location.bearing)
                put("speed", location.speed)
                put("time", location.time)
                put("provider", location.provider)
            }
        }

        /**
         * Format location as readable string
         */
        fun formatLocation(location: Location): String {
            return "Lat: ${location.latitude}, Lon: ${location.longitude}, " +
                   "Accuracy: ${location.accuracy}m, " +
                   "Provider: ${location.provider}"
        }

        /**
         * Check if location permissions are granted
         */
        fun hasPermissions(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
