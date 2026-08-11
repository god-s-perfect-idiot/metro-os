package com.metro.photos.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import kotlin.math.max

/**
 * Decodes the original MediaStore image (not a [ContentResolver.loadThumbnail]
 * preview). Thumbnails are often capped around 512px by the provider, which
 * looks soft in the full-screen viewer and under pinch-zoom.
 */
object PhotoBitmapDecoder {
    const val MaxEdgePx = 4096

    fun maxDecodeEdge(screenLongestPx: Int, maxScale: Float = ZoomLogic.MaxScale): Int {
        if (screenLongestPx <= 0) return MaxEdgePx
        return (screenLongestPx * maxScale).toInt().coerceIn(screenLongestPx, MaxEdgePx)
    }

    fun sampleSize(width: Int, height: Int, maxEdgePx: Int): Int {
        val longest = max(width, height)
        if (longest <= 0 || maxEdgePx <= 0 || longest <= maxEdgePx) return 1
        var sample = 1
        while (longest / sample > maxEdgePx) {
            sample *= 2
        }
        return sample
    }

    fun targetSize(width: Int, height: Int, maxEdgePx: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 1 to 1
        val longest = max(width, height)
        if (longest <= maxEdgePx) return width to height
        val scale = maxEdgePx.toFloat() / longest
        return (width * scale).toInt().coerceAtLeast(1) to
            (height * scale).toInt().coerceAtLeast(1)
    }

    fun thumbnailBucket(sidePx: Int): Int {
        val rounded = ((sidePx + 63) / 64) * 64
        return rounded.coerceIn(128, MaxEdgePx)
    }

    fun decodeFull(resolver: ContentResolver, uri: Uri, maxEdgePx: Int): Bitmap? {
        var edge = maxEdgePx.coerceAtLeast(1)
        while (edge >= 512) {
            val bitmap = try {
                decodeOnce(resolver, uri, edge)
            } catch (_: OutOfMemoryError) {
                null
            }
            if (bitmap != null) return bitmap
            edge /= 2
        }
        return null
    }

    private fun decodeOnce(resolver: ContentResolver, uri: Uri, maxEdgePx: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { decodeWithImageDecoder(resolver, uri, maxEdgePx) }
                .getOrElse { decodeWithBitmapFactory(resolver, uri, maxEdgePx) }
        } else {
            decodeWithBitmapFactory(resolver, uri, maxEdgePx)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(
        resolver: ContentResolver,
        uri: Uri,
        maxEdgePx: Int,
    ): Bitmap {
        val source = ImageDecoder.createSource(resolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (w, h) = targetSize(info.size.width, info.size.height, maxEdgePx)
            decoder.setTargetSize(w, h)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun decodeWithBitmapFactory(
        resolver: ContentResolver,
        uri: Uri,
        maxEdgePx: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: return null

        val orientation = resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        return applyExif(decoded, orientation)
    }

    internal fun applyExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

/**
 * Small LRU of full-resolution viewer bitmaps so pager swipes do not
 * re-decode the original JPEG/HEIC for the same URI.
 */
object FullImageCache {
    private const val MaxEntries = 3

    private val cache = object : LruCache<String, Bitmap>(MaxEntries) {}

    fun get(uri: Uri): Bitmap? = cache.get(uri.toString())

    fun put(uri: Uri, bitmap: Bitmap) {
        cache.put(uri.toString(), bitmap)
    }
}
