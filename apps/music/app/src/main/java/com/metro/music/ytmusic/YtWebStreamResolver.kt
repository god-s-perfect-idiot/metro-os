package com.metro.music.ytmusic

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Loads a signed-in YouTube Music watch page in a throwaway WebView and captures a googlevideo
 * audio URL that includes GVS `pot=`. Those Ranges succeed past ~1 MiB; anonymous Innertube URLs
 * do not.
 */
class YtWebStreamResolver(
    context: Context,
    private val authStore: YtMusicAuthStore,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    fun resolveAudioUrl(videoId: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String? {
        if (videoId.isBlank() || !authStore.connected) return null
        val result = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        val webViewRef = AtomicReference<WebView?>(null)
        val nudge = object : Runnable {
            override fun run() {
                webViewRef.get()?.evaluateJavascript(PLAY_NUDGE_JS, null)
                if (result.get() == null && latch.count > 0L) {
                    mainHandler.postDelayed(this, NUDGE_INTERVAL_MS)
                }
            }
        }

        mainHandler.post {
            try {
                webViewRef.set(createWebView(videoId, result, latch))
                mainHandler.postDelayed(nudge, 800L)
            } catch (e: Exception) {
                Log.w(TAG, "WebView stream resolve failed to start", e)
                latch.countDown()
            }
        }

        val ok = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        mainHandler.removeCallbacks(nudge)
        mainHandler.post {
            webViewRef.getAndSet(null)?.let { view ->
                runCatching {
                    view.stopLoading()
                    view.removeJavascriptInterface(BRIDGE_NAME)
                    view.destroy()
                }
            }
        }
        val url = result.get()
        when {
            url != null -> Log.i(TAG, "WebView captured pot stream for $videoId")
            !ok -> Log.w(TAG, "WebView stream resolve timed out for $videoId")
            else -> Log.w(TAG, "WebView finished without a pot= audio URL for $videoId")
        }
        return url
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun createWebView(
        videoId: String,
        result: AtomicReference<String?>,
        latch: CountDownLatch,
    ): WebView {
        installCookies()
        // Application context alone can throw on some OEMs; wrap with a device theme.
        val themed = ContextThemeWrapper(appContext, android.R.style.Theme_DeviceDefault)
        return WebView(themed).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = MOBILE_UA
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            addJavascriptInterface(
                StreamBridge { url -> offer(url, result, latch) },
                BRIDGE_NAME,
            )

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): android.webkit.WebResourceResponse? {
                    offer(request?.url?.toString().orEmpty(), result, latch)
                    return null
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    view?.evaluateJavascript(HOOK_NETWORK_JS, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(HOOK_NETWORK_JS, null)
                    view?.evaluateJavascript(PLAY_NUDGE_JS, null)
                }
            }
            loadUrl("https://music.youtube.com/watch?v=$videoId")
        }
    }

    private fun offer(url: String, result: AtomicReference<String?>, latch: CountDownLatch) {
        if (!isPotAudioStreamUrl(url)) return
        if (result.compareAndSet(null, url)) {
            latch.countDown()
        }
    }

    private fun installCookies() {
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)
        for (piece in authStore.cookie.split(';')) {
            val cookie = piece.trim()
            if (cookie.isEmpty()) continue
            manager.setCookie("https://music.youtube.com", cookie)
            manager.setCookie("https://www.youtube.com", cookie)
            manager.setCookie("https://youtube.com", cookie)
        }
        manager.flush()
    }

    private class StreamBridge(private val onUrl: (String) -> Unit) {
        @JavascriptInterface
        fun onStream(url: String?) {
            if (!url.isNullOrBlank()) onUrl(url)
        }
    }

    companion object {
        private const val TAG = "YtWebStreamResolver"
        private const val BRIDGE_NAME = "MetroYtStream"
        const val DEFAULT_TIMEOUT_MS = 20_000L
        private const val NUDGE_INTERVAL_MS = 1_500L
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Mobile Safari/537.36"

        private val AUDIO_ITAGS = setOf("139", "140", "141", "249", "250", "251", "256", "258")

        private val PLAY_NUDGE_JS = """
            (function(){
              try {
                var v = document.querySelector('video');
                if (v) { v.muted = true; v.play().catch(function(){}); }
                var nodes = document.querySelectorAll('button, [role="button"], ytmusic-play-button-renderer');
                for (var i = 0; i < nodes.length; i++) {
                  var label = (nodes[i].getAttribute('aria-label') || nodes[i].textContent || '').toLowerCase();
                  if (label.indexOf('play') >= 0) { nodes[i].click(); break; }
                }
              } catch (e) {}
            })();
        """.trimIndent()

        private val HOOK_NETWORK_JS = """
            (function(){
              if (window.__metroYtHooked) return;
              window.__metroYtHooked = true;
              function report(u) {
                try {
                  if (u && u.indexOf('googlevideo.com/videoplayback') >= 0) {
                    window.MetroYtStream.onStream(String(u));
                  }
                } catch (e) {}
              }
              try {
                var open = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function(method, url) {
                  report(url);
                  return open.apply(this, arguments);
                };
              } catch (e) {}
              try {
                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                  var u = (typeof input === 'string') ? input : (input && input.url);
                  report(u);
                  return origFetch.apply(this, arguments);
                };
              } catch (e) {}
            })();
        """.trimIndent()

        fun isAudioStreamUrl(url: String): Boolean {
            if (!url.contains("googlevideo.com/videoplayback")) return false
            val lower = url.lowercase()
            if (lower.contains("mime=audio")) return true
            val itag = Regex("""[?&]itag=(\d+)""").find(url)?.groupValues?.getOrNull(1)
            return itag != null && itag in AUDIO_ITAGS
        }

        /** Only pot-bearing audio URLs survive past the ~1 MiB GVS preview window. */
        fun isPotAudioStreamUrl(url: String): Boolean {
            if (!isAudioStreamUrl(url)) return false
            return url.contains("pot=", ignoreCase = true) ||
                url.contains("potc=", ignoreCase = true)
        }
    }
}
