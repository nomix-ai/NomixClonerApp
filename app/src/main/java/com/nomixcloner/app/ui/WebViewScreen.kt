package com.nomixcloner.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    showTopBar: Boolean = false,
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current

    BackHandler(onBack = onClose)

    Column {
        if (showTopBar) {
            TopAppBar(
                title = { Text("WebView") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
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