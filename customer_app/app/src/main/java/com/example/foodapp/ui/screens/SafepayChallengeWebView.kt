package com.example.foodapp.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafepayChallengeWebView(
    challengeUrl: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
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

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        Log.d("SafepayChallenge", "Loading URL: $url")
                        
                        // Check if the bank redirect hit our success or failure callback URLs
                        if (url.contains("getsafepay.com/success")) {
                            onSuccess()
                            return true
                        }
                        if (url.contains("getsafepay.com/failure")) {
                            onFailure()
                            return true
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }

                loadUrl(challengeUrl)
            }
        }
    )
}
