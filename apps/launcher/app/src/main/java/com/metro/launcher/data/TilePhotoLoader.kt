package com.metro.launcher.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.metro.system.MetroTileContract

/**
 * Resolves tile photo URIs and decodes from the original image bytes.
 *
 * [ContentResolver.loadThumbnail] is intentionally avoided — on MediaStore URIs it often
 * returns an embedded ~512px preview regardless of the requested size. The Photos tile
 * provider also serves pre-cached JPEGs; when the launcher has media access those URIs
 * are rewritten to MediaStore so we decode the full photo with sampling.
 */
object TilePhotoLoader {
    const val PHOTOS_PACKAGE = "com.metro.photos"

    /** Largest Start tile edge in dp (4×2 wide); Ken-Burns headroom applied separately. */
    const val MAX_TILE_EDGE_DP = 420f
    const val KEN_BURNS_OVERFLOW = 0.18f
    const val DECODE_MIN_PX = 768
    const val DECODE_MAX_PX = 2048

    fun decodeTargetPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        val kenBurnsScale = 1f + KEN_BURNS_OVERFLOW
        return (MAX_TILE_EDGE_DP * density * kenBurnsScale)
            .toInt()
            .coerceIn(DECODE_MIN_PX, DECODE_MAX_PX)
    }

    /**
     * Maps `content://com.metro.photos.tiles/photo/{mediaId}` to MediaStore when the
     * launcher can read images. People contact photo URIs use contact ids and are unchanged.
     */
    fun resolveLoadUri(context: Context, uri: Uri): Uri {
        if (!GalleryLiveTileStore.hasMediaPermission(context)) return uri
        if (uri.authority != MetroTileContract.authorityFor(PHOTOS_PACKAGE)) return uri
        val path = uri.path.orEmpty()
        if (!path.startsWith("/photo/")) return uri
        val mediaId = uri.lastPathSegment?.toLongOrNull() ?: return uri
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(collection, mediaId)
    }

    fun decode(context: Context, uri: Uri, targetPx: Int): android.graphics.Bitmap? {
        val loadUri = resolveLoadUri(context, uri)
        val resolver = context.contentResolver
        var edge = targetPx.coerceAtLeast(1)
        while (edge >= DECODE_MIN_PX / 2) {
            val bitmap = try {
                decodeOnce(resolver, loadUri, edge)
            } catch (_: OutOfMemoryError) {
                null
            }
            if (bitmap != null) return bitmap
            edge /= 2
        }
        return null
    }

    private fun decodeOnce(resolver: ContentResolver, uri: Uri, targetPx: Int): android.graphics.Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { decodeWithImageDecoder(resolver, uri, targetPx) }
                .getOrElse { decodeWithBitmapFactory(resolver, uri, targetPx) }
        } else {
            decodeWithBitmapFactory(resolver, uri, targetPx)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(
        resolver: ContentResolver,
        uri: Uri,
        targetPx: Int,
    ): android.graphics.Bitmap {
        val source = ImageDecoder.createSource(resolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val longest = maxOf(width, height)
            if (longest > targetPx) {
                val scale = targetPx.toFloat() / longest
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun decodeWithBitmapFactory(
        resolver: ContentResolver,
        uri: Uri,
        targetPx: Int,
    ): android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetPx)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            inScaled = false
        }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    internal fun sampleSize(width: Int, height: Int, maxEdgePx: Int): Int {
        val longest = maxOf(width, height)
        if (longest <= 0 || maxEdgePx <= 0 || longest <= maxEdgePx) return 1
        var sample = 1
        while (longest / sample > maxEdgePx) {
            sample *= 2
        }
        return sample
    }
}
