package com.example.foodapp.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafepayWebViewScreen(
    ddcUrl: String,
    accessToken: String,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                // Add JS Interface to intercept postMessage from Cardinal Commerce
                addJavascriptInterface(object : Any() {
                    @JavascriptInterface
                    fun postMessage(message: String) {
                        Log.d("SafepayWebView", "Received message: \$message")
                        try {
                            // Strip surrounding quotes if it was double-encoded
                            val cleanMessage = if (message.startsWith("\"") && message.endsWith("\"")) {
                                message.substring(1, message.length - 1).replace("\\\"", "\"")
                            } else {
                                message
                            }
                            
                            val json = JSONObject(cleanMessage)
                            if (json.has("Status") && json.has("SessionId")) {
                                val sessionId = json.optString("SessionId")
                                if (sessionId.isNotEmpty()) {
                                    post { onSuccess(sessionId) }
                                } else {
                                    post { onFailure("Missing SessionId in 3DS message") }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            post { onFailure("Failed to parse 3DS message: \${e.message}") }
                        }
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        Log.d("SafepayWebView", "Loading URL: \$url")
                        
                        // Handle standard redirects just in case Cybersource does a hard redirect
                        if (url.contains("threeds/success")) {
                            // But wait, the success url is only hit AFTER enrollment if there is a challenge
                            // This WebView handles the DDC (Device Data Collection).
                            return true
                        }
                        if (url.contains("threeds/failure")) {
                            onFailure("3D Secure failed")
                            return true
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }

                // Generate the HTML to load Cardinal Commerce's DDC script
                val html = """
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                    </head>
                    <body>
                        <script>
                            window.addEventListener("message", function(event) {
                                // Forward messages from Cardinal iframe to our Android Kotlin interface
                                var msg = typeof event.data === 'string' ? event.data : JSON.stringify(event.data);
                                Android.postMessage(msg);
                            }, false);
                        </script>
                        <iframe name="cardinal_iframe" id="cardinal_iframe" style="display:none; width:0; height:0; border:0;"></iframe>
                        <form id="ddc-form" target="cardinal_iframe" method="POST" action="$ddcUrl">
                            <input type="hidden" name="JWT" value="$accessToken" />
                        </form>
                        <script>
                            document.getElementById('ddc-form').submit();
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL("https://sandbox.api.getsafepay.com", html, "text/html", "UTF-8", null)
            }
        }
    )
}
