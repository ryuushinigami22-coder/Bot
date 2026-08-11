package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * MicrophoneManager.kt
 * Record audio from microphone
 * Background audio recording to file
 */
class MicrophoneManager(private val context: Context) {

    private val TAG = "MicrophoneManager"
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0

    /**
     * Start recording
     * @param outputPath Custom output path (optional)
     * @param audioSource Audio source (default: MIC)
     * @return True if recording started successfully
     */
    fun startRecording(
        outputPath: String? = null, 
        audioSource: Int = MediaRecorder.AudioSource.MIC
    ): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }

        if (!hasPermission()) {
            Log.e(TAG, "Audio recording permission not granted")
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
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                setAudioEncodingBitRate(128000) // 128 kbps
                setAudioSamplingRate(44100) // 44.1 kHz
                
                try {
                    prepare()
                    start()
                    isRecording = true
                    recordingStartTime = System.currentTimeMillis()
                    Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "MediaRecorder start failed", e)
                    releaseRecorder()
                    return false
                }
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorder()
            return false
        }
    }

    /**
     * Stop recording
     * @return Output file path if successful, null otherwise
     */
    fun stopRecording(): String? {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return null
        }

        try {
            mediaRecorder?.stop()
            Log.d(TAG, "Recording stopped, duration: ${getRecordingDuration()}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }

        val filePath = outputFile?.absolutePath
        
        releaseRecorder()
        isRecording = false
        recordingStartTime = 0
        
        return filePath
    }

    /**
     * Pause recording (Android 7.0+)
     */
    fun pauseRecording(): Boolean {
        if (!isRecording) {
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                Log.d(TAG, "Recording paused")
                true
            } else {
                Log.w(TAG, "Pause not supported on this Android version")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recorder", e)
            false
        }
    }

    /**
     * Resume recording (Android 7.0+)
     */
    fun resumeRecording(): Boolean {
        if (!isRecording) {
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                Log.d(TAG, "Recording resumed")
                true
            } else {
                Log.w(TAG, "Resume not supported on this Android version")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recorder", e)
            false
        }
    }

    /**
     * Get recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0
        }
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Get output file
     */
    fun getOutputFile(): File? = outputFile

    /**
     * Create output file
     */
    private fun createOutputFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "audio_$timestamp.m4a"
            
            val storageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "Recordings"
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
     * Check if audio recording permission is granted
     */
    private fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
        releaseRecorder()
    }

    companion object {
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
                Log.e("MicrophoneManager", "Failed to delete file", e)
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

        /**
         * Check if audio recording permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
