package com.sync.xxx.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ScreenRecorderManager.kt
 * Screen recording with MediaProjection API
 * Records screen activity to MP4 file
 */
class ScreenRecorderManager(private val context: Context) {

    private val TAG = "ScreenRecorderManager"
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    
    private var isRecording = false
    private var outputFile: File? = null
    
    private val screenDensity: Int
    private val screenWidth: Int
    private val screenHeight: Int
    
    init {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    /**
     * Create screen capture intent
     * Must be called from Activity to get permission
     */
    fun createScreenCaptureIntent(): Intent? {
        return mediaProjectionManager?.createScreenCaptureIntent()
    }

    /**
     * Start screen recording
     * @param resultCode Result code from permission request
     * @param data Intent data from permission request
     * @param outputPath Custom output path (optional)
     */
    @SuppressLint("MissingPermission")
    fun startRecording(resultCode: Int, data: Intent?, outputPath: String? = null): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }

        try {
            // Create output file
            outputFile = if (outputPath != null) {
                File(outputPath)
            } else {
                createOutputFile()
            }

            if (outputFile == null) {
                Log.e(TAG, "Failed to create output file")
                return false
            }

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile!!.absolutePath)
                setVideoSize(screenWidth, screenHeight)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(5 * 1024 * 1024) // 5 Mbps
                setVideoFrameRate(30)
                
                try {
                    prepare()
                } catch (e: Exception) {
                    Log.e(TAG, "MediaRecorder prepare failed", e)
                    return false
                }
            }

            // Get MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data!!)
            
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null")
                releaseRecorder()
                return false
            }

            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecord",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            if (virtualDisplay == null) {
                Log.e(TAG, "VirtualDisplay creation failed")
                releaseAll()
                return false
            }

            // Start recording
            mediaRecorder?.start()
            isRecording = true
            
            Log.d(TAG, "Screen recording started: ${outputFile?.absolutePath}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseAll()
            return false
        }
    }

    /**
     * Stop screen recording
     * @return Output file path if successful, null otherwise
     */
    fun stopRecording(): String? {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return null
        }

        try {
            mediaRecorder?.stop()
            Log.d(TAG, "Screen recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }

        val filePath = outputFile?.absolutePath
        
        releaseAll()
        isRecording = false
        
        return filePath
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Get output file path
     */
    fun getOutputFile(): File? = outputFile

    /**
     * Create output file
     */
    private fun createOutputFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "screen_$timestamp.mp4"
            
            val storageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "ScreenRecords"
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
     * Release MediaRecorder
     */
    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder", e)
        }
    }

    /**
     * Release VirtualDisplay
     */
    private fun releaseVirtualDisplay() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtual display", e)
        }
    }

    /**
     * Release MediaProjection
     */
    private fun releaseMediaProjection() {
        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media projection", e)
        }
    }

    /**
     * Release all resources
     */
    private fun releaseAll() {
        releaseRecorder()
        releaseVirtualDisplay()
        releaseMediaProjection()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
        releaseAll()
    }

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 1001

        /**
         * Delete recording file
         */
        fun deleteRecording(filePath: String): Boolean {
            return try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("ScreenRecorderManager", "Failed to delete file", e)
                false
            }
        }

        /**
         * Get file size in MB
         */
        fun getFileSize(filePath: String): Double {
            return try {
                val file = File(filePath)
                if (file.exists()) {
                    file.length() / (1024.0 * 1024.0)
                } else {
                    0.0
                }
            } catch (e: Exception) {
                0.0
            }
        }
    }
}
