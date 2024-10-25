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
        "(webpackChunkdiscord_app.push([[''],{},e=>{m=[];for(let c in e.c)m.push(e.c[c])}]),m).find(m=>m?.exports?.default?.getToken!==void 0).exports.default.getToken()"
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
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    webView: WebView,
                    url: String,
                ): Boolean {
                    stopLoading()
                    if (url.endsWith("/app")) {
                        evaluateJavascript(jsCode) { value ->
                            if (value != null && !value.startsWith("Error")) {
                                val token =
                                    value.replace("\"", "")
                                onDataRetrieved(token)
                                Log.d("FetchedToken", "Success")
                            } else {
                                Log.d("FetchedToken", "Failed to fetch token: $value")
                            }
                        }
                    }
                    visibility = View.GONE
                    return false
                }
            }

            loadUrl(url)
        }
    })
}