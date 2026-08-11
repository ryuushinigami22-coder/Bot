package com.sync.xxx.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.ProxyInfo
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI

/**
 * ProxyManager.kt
 * Detect and manage proxy configuration
 * Get proxy host, port, type
 */
class ProxyManager(private val context: Context) {

    private val TAG = "ProxyManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Check if proxy is enabled
     */
    fun isProxyEnabled(): Boolean {
        return getProxyHost() != null
    }

    /**
     * Get proxy host
     */
    fun getProxyHost(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val linkProperties = connectivityManager.getLinkProperties(network)
                linkProperties?.httpProxy?.host
            } else {
                System.getProperty("http.proxyHost")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting proxy host", e)
            null
        }
    }

    /**
     * Get proxy port
     */
    fun getProxyPort(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val linkProperties = connectivityManager.getLinkProperties(network)
                linkProperties?.httpProxy?.port ?: -1
            } else {
                System.getProperty("http.proxyPort")?.toIntOrNull() ?: -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting proxy port", e)
            -1
        }
    }

    /**
     * Get proxy address (host:port)
     */
    fun getProxyAddress(): String? {
        val host = getProxyHost() ?: return null
        val port = getProxyPort()
        return if (port > 0) "$host:$port" else host
    }

    /**
     * Get proxy exclusion list
     */
    fun getProxyExclusionList(): List<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val linkProperties = connectivityManager.getLinkProperties(network)
                val exclusionList = linkProperties?.httpProxy?.exclusionList
                exclusionList?.toList() ?: emptyList()
            } else {
                val exclusionString = System.getProperty("http.nonProxyHosts") ?: ""
                if (exclusionString.isNotEmpty()) {
                    exclusionString.split("|")
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting proxy exclusion list", e)
            emptyList()
        }
    }

    /**
     * Get system proxy for URI
     */
    fun getSystemProxy(uri: URI): Proxy? {
        return try {
            val proxyList = ProxySelector.getDefault()?.select(uri)
            proxyList?.firstOrNull { it.type() != Proxy.Type.DIRECT }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting system proxy", e)
            null
        }
    }

    /**
     * Check if using HTTP proxy
     */
    fun hasHttpProxy(): Boolean {
        val proxy = getSystemProxy(URI.create("http://www.google.com"))
        return proxy?.type() == Proxy.Type.HTTP
    }

    /**
     * Check if using SOCKS proxy
     */
    fun hasSocksProxy(): Boolean {
        val proxy = getSystemProxy(URI.create("http://www.google.com"))
        return proxy?.type() == Proxy.Type.SOCKS
    }

    /**
     * Get proxy type
     */
    fun getProxyType(): String {
        return when {
            hasSocksProxy() -> "SOCKS"
            hasHttpProxy() -> "HTTP"
            isProxyEnabled() -> "Unknown"
            else -> "None"
        }
    }

    /**
     * Export proxy info as JSON
     */
    fun getProxyInfoAsJson(): JSONObject {
        return JSONObject().apply {
            put("isEnabled", isProxyEnabled())
            put("host", getProxyHost())
            put("port", getProxyPort())
            put("address", getProxyAddress())
            put("type", getProxyType())
            put("exclusionList", getProxyExclusionList())
        }
    }

    /**
     * Export proxy info as text
     */
    fun exportProxyInfo(): String {
        val sb = StringBuilder()

        sb.append("Proxy Configuration\n")
        sb.append("=".repeat(60)).append("\n\n")

        sb.append("--- Status ---\n")
        sb.append("Proxy Enabled: ${if (isProxyEnabled()) "Yes" else "No"}\n")
        
        if (isProxyEnabled()) {
            sb.append("Type: ${getProxyType()}\n")
            sb.append("Host: ${getProxyHost() ?: "Unknown"}\n")
            sb.append("Port: ${getProxyPort()}\n")
            sb.append("Address: ${getProxyAddress() ?: "Unknown"}\n\n")

            val exclusionList = getProxyExclusionList()
            if (exclusionList.isNotEmpty()) {
                sb.append("--- Exclusion List ---\n")
                exclusionList.forEachIndexed { index, host ->
                    sb.append("${index + 1}. $host\n")
                }
            }
        }

        return sb.toString()
    }

    companion object {
        /**
         * Check if proxy is enabled
         */
        fun isProxyEnabled(context: Context): Boolean {
            return try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork
                    val linkProperties = connectivityManager.getLinkProperties(network)
                    linkProperties?.httpProxy?.host != null
                } else {
                    System.getProperty("http.proxyHost") != null
                }
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Get proxy host
         */
        fun getProxyHost(context: Context): String? {
            return try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork
                    val linkProperties = connectivityManager.getLinkProperties(network)
                    linkProperties?.httpProxy?.host
                } else {
                    System.getProperty("http.proxyHost")
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
