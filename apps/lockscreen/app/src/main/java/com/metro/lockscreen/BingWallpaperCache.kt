package com.metro.lockscreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Caches Bing's picture of the day for the lock fill.
 *
 * Source: `https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1`
 */
object BingWallpaperCache {
    private const val TAG = "BingWallpaper"
    private const val FILE_NAME = "bing_wallpaper.jpg"
    private const val API_URL =
        "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 20_000

    /** Refresh when the cached startdate is older than this (Bing rotates daily). */
    private val STALE_AFTER_MS = TimeUnit.HOURS.toMillis(18)

    fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    fun decode(context: Context, opts: BitmapFactory.Options? = null): Bitmap? {
        val f = file(context)
        if (!f.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(f.absolutePath, opts)
        }.getOrNull()
    }

    /**
     * Ensures a reasonably fresh Bing JPEG is on disk. Safe to call from a background thread.
     * Returns true when [file] exists afterward (fresh or leftover cache).
     */
    fun ensureFresh(context: Context): Boolean {
        val appContext = context.applicationContext
        val prefs = LockscreenPreferences(appContext)
        val cached = file(appContext)
        val ageMs = System.currentTimeMillis() - prefs.bingFetchedAtMs
        val needsRefresh = !cached.exists() ||
            prefs.bingStartDate.isEmpty() ||
            ageMs < 0L ||
            ageMs >= STALE_AFTER_MS

        if (!needsRefresh) return true

        val fetched = runCatching { downloadToday(appContext) }.getOrElse {
            Log.w(TAG, "Bing wallpaper fetch failed", it)
            false
        }
        return fetched || cached.exists()
    }

    private fun downloadToday(context: Context): Boolean {
        val metaJson = httpGetBytes(API_URL)?.toString(Charsets.UTF_8) ?: return false
        val images = JSONObject(metaJson).optJSONArray("images") ?: return false
        if (images.length() == 0) return false
        val image = images.getJSONObject(0)
        val startDate = image.optString("startdate").orEmpty()
        val relativeUrl = image.optString("url").orEmpty()
        if (relativeUrl.isEmpty()) return false

        val imageUrl = if (relativeUrl.startsWith("http")) {
            relativeUrl
        } else {
            "https://www.bing.com$relativeUrl"
        }
        val bytes = httpGetBytes(imageUrl) ?: return false
        val out = file(context)
        val tmp = File(out.parentFile, "$FILE_NAME.tmp")
        FileOutputStream(tmp).use { it.write(bytes) }
        if (!tmp.renameTo(out)) {
            tmp.copyTo(out, overwrite = true)
            tmp.delete()
        }
        val prefs = LockscreenPreferences(context)
        prefs.bingStartDate = startDate
        prefs.bingFetchedAtMs = System.currentTimeMillis()
        Log.i(TAG, "Cached Bing wallpaper startdate=$startDate bytes=${bytes.size}")
        return true
    }

    private fun httpGetBytes(url: String): ByteArray? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "metro-os-lockscreen/1.0")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "HTTP $code for $url")
                null
            } else {
                connection.inputStream.use { it.readBytes() }
            }
        } finally {
            connection.disconnect()
        }
    }
}
