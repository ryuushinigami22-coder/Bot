package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager as AndroidSmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * SmsManager.kt
 * Read, send, and delete SMS messages
 * Complete SMS management functionality
 */
class SmsManager(private val context: Context) {

    private val TAG = "SmsManager"

    /**
     * Check if SMS permissions are granted
     */
    fun hasReadPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasSendPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get all SMS messages
     */
    fun getAllMessages(): List<SmsMessage> {
        if (!hasReadPermission()) {
            Log.e(TAG, "SMS read permission not granted")
            return emptyList()
        }

        val messages = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            null,
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
            val readIndex = it.getColumnIndex(Telephony.Sms.READ)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val address = it.getString(addressIndex) ?: "Unknown"
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)
                val type = it.getInt(typeIndex)
                val isRead = it.getInt(readIndex) == 1

                messages.add(
                    SmsMessage(
                        id = id,
                        address = address,
                        body = body,
                        timestamp = date,
                        type = getSmsType(type),
                        isRead = isRead
                    )
                )
            }
        }

        Log.d(TAG, "Retrieved ${messages.size} SMS messages")
        return messages
    }

    /**
     * Get inbox messages
     */
    fun getInboxMessages(): List<SmsMessage> {
        return getAllMessages().filter { it.type == SmsType.INBOX }
    }

    /**
     * Get sent messages
     */
    fun getSentMessages(): List<SmsMessage> {
        return getAllMessages().filter { it.type == SmsType.SENT }
    }

    /**
     * Get messages from specific number
     */
    fun getMessagesFrom(phoneNumber: String): List<SmsMessage> {
        return getAllMessages().filter { it.address == phoneNumber }
    }

    /**
     * Get unread messages
     */
    fun getUnreadMessages(): List<SmsMessage> {
        return getAllMessages().filter { !it.isRead }
    }

    /**
     * Search messages by text
     */
    fun searchMessages(query: String): List<SmsMessage> {
        return getAllMessages().filter { 
            it.body.contains(query, ignoreCase = true) || 
            it.address.contains(query, ignoreCase = true)
        }
    }

    /**
     * Send SMS message
     */
    fun sendSms(phoneNumber: String, message: String): Boolean {
        if (!hasSendPermission()) {
            Log.e(TAG, "SMS send permission not granted")
            return false
        }

        return try {
            val smsManager = AndroidSmsManager.getDefault()
            
            // Split message if too long
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            }
            
            Log.d(TAG, "SMS sent to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            false
        }
    }

    /**
     * Delete SMS message
     */
    fun deleteMessage(messageId: String): Boolean {
        try {
            val uri = Uri.parse("content://sms/$messageId")
            val deleted = context.contentResolver.delete(uri, null, null)
            Log.d(TAG, "Deleted SMS: $messageId, rows: $deleted")
            return deleted > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete SMS", e)
            return false
        }
    }

    /**
     * Delete all messages from number
     */
    fun deleteMessagesFrom(phoneNumber: String): Int {
        var deletedCount = 0
        val messages = getMessagesFrom(phoneNumber)
        
        messages.forEach { message ->
            if (deleteMessage(message.id)) {
                deletedCount++
            }
        }
        
        Log.d(TAG, "Deleted $deletedCount messages from $phoneNumber")
        return deletedCount
    }

    /**
     * Get SMS type from integer
     */
    private fun getSmsType(type: Int): SmsType {
        return when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> SmsType.INBOX
            Telephony.Sms.MESSAGE_TYPE_SENT -> SmsType.SENT
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> SmsType.DRAFT
            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> SmsType.OUTBOX
            Telephony.Sms.MESSAGE_TYPE_FAILED -> SmsType.FAILED
            else -> SmsType.UNKNOWN
        }
    }

    /**
     * Export messages as JSON
     */
    fun getMessagesAsJson(): JSONArray {
        val messages = getAllMessages()
        val jsonArray = JSONArray()

        messages.forEach { message ->
            jsonArray.put(message.toJson())
        }

        return jsonArray
    }

    /**
     * Export messages as text
     */
    fun exportMessages(): String {
        val messages = getAllMessages()
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        messages.forEach { message ->
            sb.append("=".repeat(60)).append("\n")
            sb.append("From/To: ${message.address}\n")
            sb.append("Type: ${message.type}\n")
            sb.append("Date: ${dateFormat.format(Date(message.timestamp))}\n")
            sb.append("Read: ${message.isRead}\n")
            sb.append("Message: ${message.body}\n")
        }

        return sb.toString()
    }

    /**
     * Get message count
     */
    fun getMessageCount(): Int {
        return getAllMessages().size
    }

    companion object {
        /**
         * Check if SMS permissions are granted
         */
        fun hasReadPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun hasSendPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * SMS type enum
     */
    enum class SmsType {
        INBOX,
        SENT,
        DRAFT,
        OUTBOX,
        FAILED,
        UNKNOWN
    }

    /**
     * Data class for SMS message
     */
    data class SmsMessage(
        val id: String,
        val address: String,
        val body: String,
        val timestamp: Long,
        val type: SmsType,
        val isRead: Boolean
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("id", id)
                put("address", address)
                put("body", body)
                put("timestamp", timestamp)
                put("date", dateFormat.format(Date(timestamp)))
                put("type", type.name)
                put("isRead", isRead)
            }
        }
    }
}
