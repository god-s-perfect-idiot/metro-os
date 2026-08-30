package com.metro.lockscreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * User-chosen lock background JPEG, owned by this app (mirrors Settings Start background).
 */
object LockscreenCustomBackground {
    const val FILE_NAME = "lock_background.jpg"
    const val MAX_EDGE_PX = 1920

    fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    fun isSet(context: Context): Boolean =
        LockscreenPreferences(context).customBackgroundEnabled && file(context).exists()

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
        LockscreenPreferences(appContext).customBackgroundEnabled = true
        return true
    }

    fun clear(context: Context) {
        val appContext = context.applicationContext
        runCatching { file(appContext).delete() }
        LockscreenPreferences(appContext).customBackgroundEnabled = false
    }

    fun decode(context: Context, opts: BitmapFactory.Options? = null): Bitmap? {
        if (!isSet(context)) return null
        return runCatching {
            BitmapFactory.decodeFile(file(context).absolutePath, opts)
        }.getOrNull()
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
