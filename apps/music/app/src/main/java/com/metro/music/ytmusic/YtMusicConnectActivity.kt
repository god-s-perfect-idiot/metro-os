package com.metro.music.ytmusic

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.metro.ui.MetroSystemTheme
import com.metro.ui.metroNavBarPadding

/**
 * Loads music.youtube.com so the user can sign in; persists cookies for [YtMusicClient].
 */
class YtMusicConnectActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = YtMusicAuthStore(this)
        setContent {
            MetroSystemTheme {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean = false

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    val cookies = CookieManager.getInstance()
                                        .getCookie("https://music.youtube.com")
                                        .orEmpty()
                                    if (cookies.contains("SAPISID") || cookies.contains("__Secure-1PSID")) {
                                        auth.cookie = cookies
                                        auth.connected = true
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                }
                            }
                            loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F")
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .metroNavBarPadding(),
                )
            }
        }
    }
}
