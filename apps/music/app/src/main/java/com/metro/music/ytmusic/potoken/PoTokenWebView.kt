package com.metro.music.ytmusic.potoken

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Off-screen WebView that runs BotGuard and mints YouTube PO tokens.
 * Ported from NewPipe's PoTokenWebView (coroutines instead of RxJava).
 */
internal class PoTokenWebView private constructor(
    context: Context,
    private val okHttp: OkHttpClient,
    private val initDeferred: CompletableDeferred<PoTokenWebView>,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webView = WebView(context.applicationContext)
    private val generatorLock = Mutex()
    private val poTokenDeferreds = mutableMapOf<String, CompletableDeferred<String>>()

    @Volatile
    private var expirationMs: Long = -1L

    @Volatile
    private var closed = false

    val isExpired: Boolean
        get() = expirationMs < 0 || System.currentTimeMillis() >= expirationMs

    init {
        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = USER_AGENT
        // BotGuard network calls go through OkHttp; the page must not fetch on its own.
        webView.settings.blockNetworkLoads = true
        webView.addJavascriptInterface(this, JS_INTERFACE)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught")) {
                    val fmt = "\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"
                    Log.e(TAG, "Broken WebView JS: $fmt")
                    failInit(BadWebViewException(fmt))
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    suspend fun generatePoToken(identifier: String): String = generatorLock.withLock {
        ensureUsable()
        withTimeout(GENERATE_TIMEOUT_MS) {
            val deferred = CompletableDeferred<String>()
            synchronized(poTokenDeferreds) {
                poTokenDeferreds[identifier] = deferred
            }
            withContext(Dispatchers.Main) {
                val u8Identifier = PoTokenJsUtil.stringToU8(identifier)
                webView.evaluateJavascript(
                    """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }""",
                    null,
                )
            }
            deferred.await()
        }
    }

    fun close() {
        closed = true
        mainHandler.post {
            runCatching {
                webView.clearHistory()
                webView.clearCache(true)
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            }
        }
        synchronized(poTokenDeferreds) {
            poTokenDeferreds.values.forEach {
                it.completeExceptionally(PoTokenException("PoToken WebView closed"))
            }
            poTokenDeferreds.clear()
        }
    }

    private fun loadHtml(context: Context) {
        Thread {
            try {
                val html = context.assets.open("po_token.html").bufferedReader().use { it.readText() }
                val injected = html.replaceFirst(
                    "</script>",
                    "\n$JS_INTERFACE.downloadAndRunBotguard()</script>",
                )
                mainHandler.post {
                    webView.loadDataWithBaseURL(
                        "https://www.youtube.com",
                        injected,
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
            } catch (t: Throwable) {
                failInit(t)
            }
        }.start()
    }

    @JavascriptInterface
    fun downloadAndRunBotguard() {
        makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/Create",
            """[ "$REQUEST_KEY" ]""",
        ) { responseBody ->
            val parsedChallengeData = PoTokenJsUtil.parseChallengeData(responseBody)
            mainHandler.post {
                webView.evaluateJavascript(
                    """try {
                    data = $parsedChallengeData
                    runBotGuard(data).then(function (result) {
                        this.webPoSignalOutput = result.webPoSignalOutput
                        $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                    }, function (error) {
                        $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                    })
                } catch (error) {
                    $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                }""",
                    null,
                )
            }
        }
    }

    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        failInit(buildExceptionForJsError(error))
    }

    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/GenerateIT",
            """[ "$REQUEST_KEY", "$botguardResponse" ]""",
        ) { responseBody ->
            try {
                val (integrityToken, expirationTimeInSeconds) =
                    PoTokenJsUtil.parseIntegrityTokenData(responseBody)
                expirationMs = System.currentTimeMillis() +
                    (expirationTimeInSeconds * 1000L) - EXPIRATION_MARGIN_MS
                mainHandler.post {
                    webView.evaluateJavascript("this.integrityToken = $integrityToken") {
                        if (!initDeferred.isCompleted) {
                            initDeferred.complete(this@PoTokenWebView)
                        }
                        Log.i(TAG, "BotGuard ready (ttl=${expirationTimeInSeconds}s)")
                    }
                }
            } catch (t: Throwable) {
                failInit(t)
            }
        }
    }

    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        popDeferred(identifier)?.completeExceptionally(buildExceptionForJsError(error))
    }

    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
        val deferred = popDeferred(identifier) ?: return
        try {
            deferred.complete(PoTokenJsUtil.u8ToBase64(poTokenU8))
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
        }
    }

    private fun popDeferred(identifier: String): CompletableDeferred<String>? =
        synchronized(poTokenDeferreds) { poTokenDeferreds.remove(identifier) }

    private fun makeBotguardServiceRequest(
        url: String,
        data: String,
        handleResponseBody: (String) -> Unit,
    ) {
        Thread {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json+protobuf")
                    .header("x-goog-api-key", GOOGLE_API_KEY)
                    .header("x-user-agent", "grpc-web-javascript/0.1")
                    .post(data.toRequestBody("application/json+protobuf".toMediaType()))
                    .build()
                okHttp.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.code != 200) {
                        failInit(PoTokenException("Invalid response code: ${response.code}"))
                        return@use
                    }
                    handleResponseBody(body)
                }
            } catch (t: Throwable) {
                failInit(t)
            }
        }.start()
    }

    private fun ensureUsable() {
        if (closed) throw PoTokenException("PoToken WebView closed")
        if (isExpired) throw PoTokenException("PoToken integrity token expired")
    }

    private fun failInit(error: Throwable) {
        val wrapped = when (error) {
            is Exception -> error
            else -> Exception(error)
        }
        if (!initDeferred.isCompleted) {
            initDeferred.completeExceptionally(wrapped)
        }
        Log.e(TAG, "PoToken WebView init failed", wrapped)
        mainHandler.post { runCatching { close() } }
    }

    companion object {
        private const val TAG = "YtPoToken"
        private const val JS_INTERFACE = "PoTokenWebView"
        // Public BotGuard API key observed on YouTube web (same as NewPipe).
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val EXPIRATION_MARGIN_MS = 10 * 60 * 1000L
        private const val INIT_TIMEOUT_MS = 45_000L
        private const val GENERATE_TIMEOUT_MS = 20_000L

        suspend fun create(context: Context, okHttp: OkHttpClient): PoTokenWebView {
            val client = okHttp.newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
            val deferred = CompletableDeferred<PoTokenWebView>()
            withContext(Dispatchers.Main) {
                val web = PoTokenWebView(context.applicationContext, client, deferred)
                web.loadHtml(context.applicationContext)
            }
            return withTimeout(INIT_TIMEOUT_MS) { deferred.await() }
        }
    }
}
