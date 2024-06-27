package com.nomixcloner.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return handleUrl(context, request?.url.toString())
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun handleUrl(context: Context, url: String?): Boolean {
    url?.let {
        if (it.startsWith("tg:") || it.startsWith("https://t.me/")) {
            val telegramPackage = "org.telegram.messenger"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply {
                setPackage(telegramPackage)
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Telegram app is not installed, open Play Store
                val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$telegramPackage"))
                context.startActivity(playStoreIntent)
            }
            return true
        }
    }
    return false
}