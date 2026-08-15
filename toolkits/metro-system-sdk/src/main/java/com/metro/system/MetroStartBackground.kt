package com.metro.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * WP8.1 Start background — cropped photo hosted by Settings and read by the launcher.
 *
 * Transparent / accent tiles act as viewport windows onto this fixed image while the tile
 * grid scrolls (parallax factor 0: wallpaper stays screen-locked).
 */
object MetroStartBackground {
    const val FILE_NAME = "start_background.jpg"
    const val MIME_TYPE = "image/jpeg"

    /** Max edge for the stored JPEG (keeps Start decode cheap on phone panels). */
    const val MAX_EDGE_PX = 1920

    val CONTENT_URI: Uri =
        Uri.parse(
            "content://${MetroContentProviderContract.AUTHORITY}/" +
                MetroContentProviderContract.PATH_START_BACKGROUND,
        )

    fun file(context: Context): File = File(context.applicationContext.filesDir, FILE_NAME)

    fun isEnabled(context: Context): Boolean =
        MetroPreferences(context).startBackgroundEnabled

    /**
     * Writes [bitmap] (scaled if needed), sets [MetroPreferences.startBackgroundEnabled], and
     * broadcasts [MetroBroadcasts.ACTION_THEME_CHANGED] so the launcher reloads.
     */
    fun save(context: Context, bitmap: Bitmap): Boolean {
        val appContext = context.applicationContext
        val scaled = scaleDownIfNeeded(bitmap)
        val outFile = file(appContext)
        val ok = runCatching {
            FileOutputStream(outFile).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            true
        }.getOrDefault(false)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        if (!ok) return false
        val prefs = MetroPreferences(appContext)
        prefs.startBackgroundEnabled = true
        prefs.broadcastThemeChanged()
        return true
    }

    /** Deletes the stored image, clears the preference flag, and broadcasts theme change. */
    fun clear(context: Context) {
        val appContext = context.applicationContext
        runCatching { file(appContext).delete() }
        val prefs = MetroPreferences(appContext)
        prefs.startBackgroundEnabled = false
        prefs.broadcastThemeChanged()
    }

    fun decode(context: Context, opts: BitmapFactory.Options? = null): Bitmap? {
        if (!isEnabled(context)) return null
        return runCatching {
            context.applicationContext.contentResolver.openInputStream(CONTENT_URI)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }

    /**
     * Accent-following tiles (Metro suite / Android system / no strong brand) reveal the Start
     * background. Fixed-brand packages stay opaque.
     */
    fun revealsThroughPackage(context: Context, packageName: String): Boolean {
        if (!isEnabled(context)) return false
        if (MetroAppRegistry.strongBrandHex(packageName) != null) return false
        return MetroAppDiscovery.isSystemApp(context.packageManager, packageName)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val maxEdge = maxOf(bitmap.width, bitmap.height)
        if (maxEdge <= MAX_EDGE_PX) return bitmap
        val scale = MAX_EDGE_PX.toFloat() / maxEdge.toFloat()
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
