package com.sync.xxx.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * CameraStreamManager.kt
 * Live camera streaming (front & back)
 * Captures frames and streams via websocket/HTTP
 */
class CameraStreamManager(private val context: Context) {

    private val TAG = "CameraStreamManager"
    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    
    private var isStreaming = false
    private var currentCamera = CameraFacing.BACK
    private var frameCallback: ((ByteArray) -> Unit)? = null
    
    enum class CameraFacing {
        FRONT, BACK
    }

    init {
        startBackgroundThread()
    }

    /**
     * Start camera streaming
     * @param facing Front or back camera
     * @param callback Callback for each frame (JPEG bytes)
     */
    fun startStreaming(facing: CameraFacing, callback: (ByteArray) -> Unit) {
        if (isStreaming) {
            Log.w(TAG, "Already streaming, stopping first")
            stopStreaming()
        }

        currentCamera = facing
        frameCallback = callback
        openCamera(facing)
    }

    /**
     * Stop camera streaming
     */
    fun stopStreaming() {
        isStreaming = false
        frameCallback = null
        closeCamera()
    }

    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        val newFacing = if (currentCamera == CameraFacing.BACK) CameraFacing.FRONT else CameraFacing.BACK
        val callback = frameCallback
        if (callback != null && isStreaming) {
            stopStreaming()
            startStreaming(newFacing, callback)
        }
    }

    /**
     * Open camera device
     */
    @SuppressLint("MissingPermission")
    private fun openCamera(facing: CameraFacing) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            return
        }

        try {
            val cameraId = getCameraId(facing)
            if (cameraId == null) {
                Log.e(TAG, "No camera found for facing: $facing")
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened: $cameraId")
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected")
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
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
     * Create camera preview session
     */
    private fun createCameraPreviewSession() {
        try {
            val device = cameraDevice ?: return

            // Create ImageReader for capturing frames
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        
                        // Send frame to callback
                        frameCallback?.invoke(bytes)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image", e)
                    } finally {
                        image.close()
                    }
                }
            }, backgroundHandler)

            val surface = imageReader?.surface ?: return

            // Create capture request
            val captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            // Create capture session
            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        cameraCaptureSession = session
                        try {
                            // Start repeating capture requests
                            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                            isStreaming = true
                            Log.d(TAG, "Camera streaming started")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start capture", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Camera configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create preview session", e)
        }
    }

    /**
     * Close camera
     */
    private fun closeCamera() {
        try {
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            imageReader?.close()
            imageReader = null
            
            Log.d(TAG, "Camera closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera", e)
        }
    }

    /**
     * Start background thread for camera operations
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
     * Cleanup resources
     */
    fun cleanup() {
        stopStreaming()
        stopBackgroundThread()
    }

    companion object {
        /**
         * Convert frame bytes to Base64 for transmission
         */
        fun frameToBase64(frameBytes: ByteArray): String {
            return Base64.encodeToString(frameBytes, Base64.NO_WRAP)
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
