package com.sajeg.questrpc.composables

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.text.replace
import kotlin.text.startsWith

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SignInDiscord(onDataRetrieved: (token: String?) -> Unit) {
    val url = "https://discord.com/login"
    val jsCode =
        "localStorage.getItem('token')"
    AndroidView(factory = {
        WebView(it).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null) {
                        if (url.endsWith("/app")) {
                            evaluateJavascript(jsCode) { value ->
                                Log.d("FetchedTokenError", value)
                                if (value != null && !value.startsWith("Error")) {
                                    val token =
                                        value.substring(3, value.length - 3)
                                    onDataRetrieved(token)
                                    visibility = View.GONE
                                    Log.d("FetchedToken", token)
                                } else {
                                    Log.d("FetchedToken", "Failed to fetch token: $value")
                                }
                            }
                        }
                    }
                }
            }

            loadUrl(url)
        }
    })
}