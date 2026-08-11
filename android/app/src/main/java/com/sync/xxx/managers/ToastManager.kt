package com.sync.xxx.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.util.Log

/**
 * ToastManager.kt
 * Display toast messages programmatically
 * Show short/long toasts with custom messages
 */
class ToastManager(private val context: Context) {

    private val TAG = "ToastManager"
    private val handler = Handler(Looper.getMainLooper())
    private var currentToast: Toast? = null

    /**
     * Show short toast (2 seconds)
     */
    fun showShort(message: String): Boolean {
        return try {
            handler.post {
                cancelCurrent()
                currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                currentToast?.show()
            }
            Log.d(TAG, "Showing short toast: $message")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing short toast", e)
            false
        }
    }

    /**
     * Show long toast (3.5 seconds)
     */
    fun showLong(message: String): Boolean {
        return try {
            handler.post {
                cancelCurrent()
                currentToast = Toast.makeText(context, message, Toast.LENGTH_LONG)
                currentToast?.show()
            }
            Log.d(TAG, "Showing long toast: $message")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing long toast", e)
            false
        }
    }

    /**
     * Show toast with custom duration (using repeated toasts)
     */
    fun showCustomDuration(message: String, durationMs: Long): Boolean {
        return try {
            val toastDuration = 2000L // Short toast duration
            val repetitions = (durationMs / toastDuration).toInt()
            
            handler.post {
                repeat(repetitions) { index ->
                    handler.postDelayed({
                        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                        toast.show()
                    }, index * toastDuration)
                }
            }
            Log.d(TAG, "Showing custom duration toast: $message for ${durationMs}ms")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing custom duration toast", e)
            false
        }
    }

    /**
     * Cancel current toast
     */
    fun cancelCurrent(): Boolean {
        return try {
            currentToast?.cancel()
            currentToast = null
            Log.d(TAG, "Cancelled current toast")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling toast", e)
            false
        }
    }

    /**
     * Show warning toast
     */
    fun showWarning(message: String): Boolean {
        return showLong("⚠️ $message")
    }

    /**
     * Show error toast
     */
    fun showError(message: String): Boolean {
        return showLong("❌ $message")
    }

    /**
     * Show success toast
     */
    fun showSuccess(message: String): Boolean {
        return showShort("✓ $message")
    }

    /**
     * Show info toast
     */
    fun showInfo(message: String): Boolean {
        return showShort("ℹ️ $message")
    }

    companion object {
        /**
         * Toast duration constants
         */
        const val LENGTH_SHORT = Toast.LENGTH_SHORT
        const val LENGTH_LONG = Toast.LENGTH_LONG

        /**
         * Quick show short toast
         */
        fun showShort(context: Context, message: String): Boolean {
            return try {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Quick show long toast
         */
        fun showLong(context: Context, message: String): Boolean {
            return try {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
