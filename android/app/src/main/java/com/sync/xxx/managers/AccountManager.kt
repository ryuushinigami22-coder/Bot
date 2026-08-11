package com.sync.xxx.managers

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager as AndroidAccountManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * AccountManager.kt
 * Access device accounts - Google, social media, email
 * Retrieve account information from AccountManager
 */
class AccountManager(private val context: Context) {

    private val TAG = "AccountManager"
    private val accountManager = AndroidAccountManager.get(context)

    /**
     * Check if accounts permission is granted
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.GET_ACCOUNTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get all accounts
     */
    fun getAllAccounts(): List<AccountInfo> {
        if (!hasPermission()) {
            Log.e(TAG, "Accounts permission not granted")
            return emptyList()
        }

        return try {
            val accounts = accountManager.accounts
            accounts.map { account ->
                AccountInfo(
                    name = account.name,
                    type = account.type
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting accounts", e)
            emptyList()
        }
    }

    /**
     * Get Google accounts
     */
    fun getGoogleAccounts(): List<AccountInfo> {
        return getAllAccounts().filter { 
            it.type == "com.google" || it.type.contains("google", ignoreCase = true)
        }
    }

    /**
     * Get accounts by type
     */
    fun getAccountsByType(type: String): List<AccountInfo> {
        return getAllAccounts().filter { it.type == type }
    }

    /**
     * Get all account types
     */
    fun getAllAccountTypes(): List<String> {
        return getAllAccounts().map { it.type }.distinct()
    }

    /**
     * Get total account count
     */
    fun getTotalAccountCount(): Int {
        return getAllAccounts().size
    }

    /**
     * Check if account exists
     */
    fun hasAccount(name: String, type: String): Boolean {
        return getAllAccounts().any { it.name == name && it.type == type }
    }

    /**
     * Export accounts as JSON
     */
    fun getAccountsAsJson(): JSONArray {
        val accounts = getAllAccounts()
        val jsonArray = JSONArray()

        accounts.forEach { account ->
            jsonArray.put(account.toJson())
        }

        return jsonArray
    }

    /**
     * Export accounts as text
     */
    fun exportAccounts(): String {
        val accounts = getAllAccounts()
        val sb = StringBuilder()

        sb.append("Device Accounts\n")
        sb.append("Total: ${accounts.size}\n")
        sb.append("=".repeat(60)).append("\n\n")

        val accountsByType = accounts.groupBy { it.type }
        
        accountsByType.forEach { (type, accts) ->
            sb.append("--- $type (${accts.size}) ---\n")
            accts.forEach { account ->
                sb.append("  • ${account.name}\n")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if accounts permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.GET_ACCOUNTS
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Common account types
         */
        fun getCommonAccountTypes(): Map<String, String> {
            return mapOf(
                "com.google" to "Google",
                "com.facebook.auth.login" to "Facebook",
                "com.twitter.android.auth.login" to "Twitter",
                "com.instagram.android" to "Instagram",
                "com.linkedin.android" to "LinkedIn",
                "com.whatsapp" to "WhatsApp",
                "com.snapchat.android" to "Snapchat",
                "com.tencent.mm" to "WeChat",
                "jp.naver.line.android" to "LINE",
                "com.telegram.messenger" to "Telegram"
            )
        }
    }

    /**
     * Data class for account info
     */
    data class AccountInfo(
        val name: String,
        val type: String
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("name", name)
                put("type", type)
                put("typeDisplay", getTypeDisplay(type))
            }
        }

        private fun getTypeDisplay(type: String): String {
            return getCommonAccountTypes()[type] ?: type
        }
    }
}
