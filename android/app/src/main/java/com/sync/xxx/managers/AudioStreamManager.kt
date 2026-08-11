package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AudioStreamManager.kt
 * Live microphone streaming
 * Captures audio and streams via websocket/HTTP
 */
class AudioStreamManager(private val context: Context) {

    private val TAG = "AudioStreamManager"
    
    private var audioRecord: AudioRecord? = null
    private var isStreaming = false
    private var streamingThread: Thread? = null
    private var audioCallback: ((ByteArray) -> Unit)? = null
    
    // Audio configuration
    private val sampleRate = 44100 // CD quality
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = 0
    
    init {
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = sampleRate * 2 // Fallback: 1 second buffer
            Log.w(TAG, "Using fallback buffer size: $bufferSize")
        }
    }

    /**
     * Start audio streaming
     * @param callback Callback for each audio chunk (PCM bytes)
     */
    fun startStreaming(callback: (ByteArray) -> Unit) {
        if (isStreaming) {
            Log.w(TAG, "Already streaming")
            return
        }

        if (!hasPermission()) {
            Log.e(TAG, "Audio recording permission not granted")
            return
        }

        try {
            audioCallback = callback
            
            // Initialize AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                audioRecord = null
                return
            }

            // Start recording
            audioRecord?.startRecording()
            isStreaming = true
            
            // Start streaming thread
            streamingThread = Thread {
                streamAudio()
            }
            streamingThread?.start()
            
            Log.d(TAG, "Audio streaming started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio streaming", e)
            cleanup()
        }
    }

    /**
     * Stop audio streaming
     */
    fun stopStreaming() {
        if (!isStreaming) {
            return
        }

        isStreaming = false
        
        try {
            streamingThread?.interrupt()
            streamingThread?.join(1000)
            streamingThread = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping streaming thread", e)
        }

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }

        audioCallback = null
        Log.d(TAG, "Audio streaming stopped")
    }

    /**
     * Stream audio data
     */
    private fun streamAudio() {
        val buffer = ByteArray(bufferSize)
        
        while (isStreaming) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    // Create a copy of the read data
                    val audioData = buffer.copyOf(bytesRead)
                    
                    // Send to callback
                    audioCallback?.invoke(audioData)
                } else if (bytesRead < 0) {
                    Log.e(TAG, "Error reading audio: $bytesRead")
                    break
                }
            } catch (e: Exception) {
                if (isStreaming) {
                    Log.e(TAG, "Error during audio streaming", e)
                }
                break
            }
        }
    }

    /**
     * Get current streaming status
     */
    fun isStreaming(): Boolean = isStreaming

    /**
     * Get audio configuration info
     */
    fun getAudioConfig(): AudioConfig {
        return AudioConfig(
            sampleRate = sampleRate,
            channels = 1, // Mono
            bitDepth = 16,
            bufferSize = bufferSize
        )
    }

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        stopStreaming()
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

    companion object {
        /**
         * Convert PCM audio bytes to Base64 for transmission
         */
        fun audioToBase64(audioBytes: ByteArray): String {
            return Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        }

        /**
         * Compress PCM audio using simple RLE (for transmission efficiency)
         */
        fun compressAudio(pcmData: ByteArray): ByteArray {
            val compressed = ByteArrayOutputStream()
            var i = 0
            while (i < pcmData.size) {
                val current = pcmData[i]
                var count = 1
                
                while (i + count < pcmData.size && pcmData[i + count] == current && count < 255) {
                    count++
                }
                
                compressed.write(count)
                compressed.write(current.toInt())
                i += count
            }
            return compressed.toByteArray()
        }

        /**
         * Convert PCM to WAV format (adds WAV header)
         */
        fun pcmToWav(pcmData: ByteArray, sampleRate: Int, channels: Int, bitDepth: Int): ByteArray {
            val wavData = ByteArrayOutputStream()
            val byteRate = sampleRate * channels * (bitDepth / 8)
            val blockAlign = (channels * (bitDepth / 8)).toShort()
            
            try {
                // Write WAV header
                wavData.write("RIFF".toByteArray())
                wavData.write(intToByteArray(36 + pcmData.size))
                wavData.write("WAVE".toByteArray())
                
                // Write fmt chunk
                wavData.write("fmt ".toByteArray())
                wavData.write(intToByteArray(16)) // Chunk size
                wavData.write(shortToByteArray(1)) // Audio format (PCM)
                wavData.write(shortToByteArray(channels.toShort()))
                wavData.write(intToByteArray(sampleRate))
                wavData.write(intToByteArray(byteRate))
                wavData.write(shortToByteArray(blockAlign))
                wavData.write(shortToByteArray(bitDepth.toShort()))
                
                // Write data chunk
                wavData.write("data".toByteArray())
                wavData.write(intToByteArray(pcmData.size))
                wavData.write(pcmData)
                
                return wavData.toByteArray()
            } catch (e: Exception) {
                Log.e("AudioStreamManager", "Error converting PCM to WAV", e)
                return pcmData
            }
        }

        /**
         * Convert int to byte array (little endian)
         */
        private fun intToByteArray(value: Int): ByteArray {
            val buffer = ByteBuffer.allocate(4)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(value)
            return buffer.array()
        }

        /**
         * Convert short to byte array (little endian)
         */
        private fun shortToByteArray(value: Short): ByteArray {
            val buffer = ByteBuffer.allocate(2)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putShort(value)
            return buffer.array()
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

    /**
     * Data class for audio configuration
     */
    data class AudioConfig(
        val sampleRate: Int,
        val channels: Int,
        val bitDepth: Int,
        val bufferSize: Int
    )
}
