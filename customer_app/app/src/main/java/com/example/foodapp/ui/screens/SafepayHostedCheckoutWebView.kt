package com.example.foodapp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.foodapp.theme.BrandPrimary

/**
 * Safepay Hosted Checkout WebView.
 *
 * Loads the Safepay Components checkout URL inside a WebView.
 * When the user completes payment, Safepay redirects to a URL
 * containing `action=complete` or `/checkout/external/success`.
 * When the user cancels, the URL contains `action=cancel`.
 *
 * We intercept these redirect URLs and call the appropriate callbacks.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafepayHostedCheckoutWebView(
    checkoutUrl: String,
    onSuccess: (tracker: String?, reference: String?) -> Unit,
    onCancel: () -> Unit,
    onFailure: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasCompleted by remember { mutableStateOf(false) }

    // Handle back button press — treat as cancellation
    BackHandler {
        if (!hasCompleted) {
            onCancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false

                            // Check for success redirect
                            if (url.contains("/checkout/external/success") ||
                                url.contains("action=complete") ||
                                url.contains("getsafepay.com/success") ||
                                url.contains("icelandapp://safepay/success")) {
                                if (!hasCompleted) {
                                    hasCompleted = true
                                    val uri = android.net.Uri.parse(url)
                                    val tracker = uri.getQueryParameter("tracker")
                                    val reference = uri.getQueryParameter("reference")
                                    onSuccess(tracker, reference)
                                }
                                return true
                            }

                            // Check for cancel redirect
                            if (url.contains("/checkout/external/cancel") ||
                                url.contains("action=cancel") ||
                                url.contains("getsafepay.com/cancel") ||
                                url.contains("getsafepay.com/failure") ||
                                url.contains("icelandapp://safepay/cancel")) {
                                if (!hasCompleted) {
                                    hasCompleted = true
                                    onCancel()
                                }
                                return true
                            }

                            // Handle external intents (like Google Pay or banking apps)
                            if (url.startsWith("intent://")) {
                                try {
                                    val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    val context = view?.context
                                    if (context != null && intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                        return true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            return false
                        }
                    }

                    loadUrl(checkoutUrl)
                }
            }
        )

        // Loading overlay
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = BrandPrimary)
                    Text(
                        text = "Loading Safepay Checkout...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // Close / Cancel button in the top-right
        IconButton(
            onClick = {
                if (!hasCompleted) {
                    onCancel()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.DarkGray
            )
        }
    }
}
