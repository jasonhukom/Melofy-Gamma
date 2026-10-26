package com.melofy.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import org.json.JSONObject

/**
 * Thin WebView wrapper around the deployed Melofy web app.
 *
 * Google blocks completing sign-in inside a plain embedded WebView (it
 * returns "disallowed_useragent"), so any navigation to accounts.google.com
 * is intercepted here and opened in a Chrome Custom Tab instead. That tab
 * has its own separate cookies from this WebView, so completion is handed
 * back via the melofy://auth/complete deep link + a one-time code (see
 * js/firebaseAuth.js and accounts/handoff.py on the backend) rather than a
 * normal cookie-based redirect.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    companion object {
        // Point this at your deployed backend (e.g. https://your-app.vercel.app/)
        // or local development backend (10.0.2.2 maps to host machine 127.0.0.1 in Android emulator).
        private const val APP_URL = "http://10.0.2.2:8000/"
        private const val ANDROID_UA_SUFFIX = " MelofyAndroidApp/1.0"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.userAgentString = settings.userAgentString + ANDROID_UA_SUFFIX

        webView.webViewClient = MelofyWebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(APP_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /** Handles melofy://auth/complete?code=... by handing the code to the
     *  page already loaded in the WebView, which redeems it against
     *  /api/auth/exchange to establish a real session in this WebView's own
     *  storage. Nothing is reloaded, so playback is not interrupted. */
    private fun handleDeepLink(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme == "melofy" && uri.host == "auth") {
            val code = uri.getQueryParameter("code") ?: return
            val jsSafeCode = JSONObject.quote(code) // safely escaped JS string literal
            webView.evaluateJavascript(
                "if (window.completeAndroidHandoff) { window.completeAndroidHandoff($jsSafeCode); }",
                null
            )
        }
    }

    private inner class MelofyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (url.host == "accounts.google.com") {
                CustomTabsIntent.Builder().build().launchUrl(this@MainActivity, url)
                return true
            }
            return false
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            if (request.isForMainFrame) {
                val failingUrl = request.url.toString()
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <style>
                            body {
                                background-color: #0b0d13;
                                color: #f0f2f5;
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                justify-content: center;
                                height: 100vh;
                                margin: 0;
                                padding: 24px;
                                box-sizing: border-box;
                                text-align: center;
                            }
                            .logo {
                                font-size: 2rem;
                                font-weight: 800;
                                margin-bottom: 8px;
                                background: linear-gradient(135deg, #a855f7, #6366f1);
                                -webkit-background-clip: text;
                                -webkit-text-fill-color: transparent;
                            }
                            .badge {
                                display: inline-block;
                                background: rgba(255,255,255,0.08);
                                border: 1px solid rgba(255,255,255,0.15);
                                padding: 4px 12px;
                                border-radius: 9999px;
                                font-size: 0.8rem;
                                color: #a1a1aa;
                                margin-bottom: 20px;
                            }
                            h2 {
                                font-size: 1.25rem;
                                margin: 0 0 12px;
                                color: #ffffff;
                            }
                            p {
                                font-size: 0.95rem;
                                color: #94a3b8;
                                line-height: 1.5;
                                margin: 0 0 16px;
                                max-width: 320px;
                            }
                            code {
                                background: #181b24;
                                padding: 3px 6px;
                                border-radius: 4px;
                                font-size: 0.85rem;
                                color: #e2e8f0;
                                word-break: break-all;
                            }
                            .btn {
                                margin-top: 16px;
                                background: #6366f1;
                                color: white;
                                border: none;
                                border-radius: 9999px;
                                padding: 12px 28px;
                                font-size: 0.95rem;
                                font-weight: 600;
                                cursor: pointer;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="logo">Melofy</div>
                        <div class="badge">Android Companion</div>
                        <h2>Backend Connecting...</h2>
                        <p>Attempted to reach: <code>$failingUrl</code></p>
                        <p>To use Melofy, ensure your backend is deployed or running locally, and update <code>APP_URL</code> in <code>MainActivity.kt</code>.</p>
                        <button class="btn" onclick="location.reload()">Retry Connection</button>
                    </body>
                    </html>
                """.trimIndent()
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
