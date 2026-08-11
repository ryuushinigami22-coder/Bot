package com.sync.xxx.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * SecureStorageManager.kt
 * Secure encrypted storage for sensitive data
 * AES encryption for preferences and data
 */
class SecureStorageManager(private val context: Context) {

    private val TAG = "SecureStorageManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)
    private val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val KEY_SIZE = 256

    /**
     * Generate encryption key
     */
    private fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE, SecureRandom())
        return keyGen.generateKey()
    }

    /**
     * Get or create encryption key
     */
    private fun getEncryptionKey(): SecretKey {
        val keyString = prefs.getString("encryption_key", null)
        
        return if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            val newKey = generateKey()
            val keyBytes = newKey.encoded
            val keyString = Base64.encodeToString(keyBytes, Base64.DEFAULT)
            prefs.edit().putString("encryption_key", keyString).apply()
            newKey
        }
    }

    /**
     * Encrypt string data
     */
    fun encrypt(plainText: String): String? {
        return try {
            val key = getEncryptionKey()
            val cipher = Cipher.getInstance(ALGORITHM)
            
            // Generate random IV
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = iv + encrypted
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting", e)
            null
        }
    }

    /**
     * Decrypt string data
     */
    fun decrypt(encryptedText: String): String? {
        return try {
            val key = getEncryptionKey()
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            
            // Extract IV and encrypted data
            val iv = combined.copyOfRange(0, 16)
            val encrypted = combined.copyOfRange(16, combined.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting", e)
            null
        }
    }

    /**
     * Store encrypted string
     */
    fun storeSecure(key: String, value: String): Boolean {
        return try {
            val encrypted = encrypt(value)
            if (encrypted != null) {
                prefs.edit().putString(key, encrypted).apply()
                Log.d(TAG, "Stored encrypted: $key")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing secure", e)
            false
        }
    }

    /**
     * Retrieve encrypted string
     */
    fun retrieveSecure(key: String): String? {
        return try {
            val encrypted = prefs.getString(key, null)
            if (encrypted != null) {
                decrypt(encrypted)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving secure", e)
            null
        }
    }

    /**
     * Delete secure data
     */
    fun deleteSecure(key: String): Boolean {
        return try {
            prefs.edit().remove(key).apply()
            Log.d(TAG, "Deleted: $key")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting secure", e)
            false
        }
    }

    /**
     * Clear all secure data
     */
    fun clearAll(): Boolean {
        return try {
            prefs.edit().clear().apply()
            Log.d(TAG, "Cleared all secure data")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all", e)
            false
        }
    }

    /**
     * Check if key exists
     */
    fun hasKey(key: String): Boolean {
        return prefs.contains(key)
    }

    /**
     * Get all keys
     */
    fun getAllKeys(): Set<String> {
        return prefs.all.keys
    }

    /**
     * Get storage info
     */
    fun getStorageInfo(): StorageInfo {
        val allKeys = getAllKeys()
        return StorageInfo(
            totalKeys = allKeys.size,
            keys = allKeys.toList(),
            hasEncryptionKey = prefs.contains("encryption_key")
        )
    }

    /**
     * Export storage info as JSON
     */
    fun getStorageInfoAsJson(): JSONObject {
        val info = getStorageInfo()
        return JSONObject().apply {
            put("totalKeys", info.totalKeys)
            put("keys", org.json.JSONArray(info.keys))
            put("hasEncryptionKey", info.hasEncryptionKey)
        }
    }

    /**
     * Export storage info as text
     */
    fun exportStorageInfo(): String {
        val info = getStorageInfo()
        val sb = StringBuilder()

        sb.append("Secure Storage Info\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Overview ---\n")
        sb.append("Total Keys: ${info.totalKeys}\n")
        sb.append("Encryption Key: ${if (info.hasEncryptionKey) "Present" else "Missing"}\n\n")

        if (info.keys.isNotEmpty()) {
            sb.append("--- Stored Keys ---\n")
            info.keys.forEachIndexed { index, key ->
                if (key != "encryption_key") {
                    sb.append("${index + 1}. $key\n")
                }
            }
        }

        return sb.toString()
    }

    /**
     * Storage info data class
     */
    data class StorageInfo(
        val totalKeys: Int,
        val keys: List<String>,
        val hasEncryptionKey: Boolean
    )

    companion object {
        /**
         * Store encrypted data (static)
         */
        fun storeSecure(context: Context, key: String, value: String): Boolean {
            return try {
                val manager = SecureStorageManager(context)
                manager.storeSecure(key, value)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Retrieve encrypted data (static)
         */
        fun retrieveSecure(context: Context, key: String): String? {
            return try {
                val manager = SecureStorageManager(context)
                manager.retrieveSecure(key)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Delete encrypted data (static)
         */
        fun deleteSecure(context: Context, key: String): Boolean {
            return try {
                val manager = SecureStorageManager(context)
                manager.deleteSecure(key)
            } catch (e: Exception) {
                false
            }
        }
    }
}
