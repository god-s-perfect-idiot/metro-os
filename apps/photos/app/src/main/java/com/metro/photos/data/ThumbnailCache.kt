package com.metro.photos.data

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache

/**
 * Process-wide thumbnail cache so pivot switches and lazy recycling do not
 * re-decode MediaStore thumbnails for the same URI.
 */
object ThumbnailCache {
    private const val MaxEntries = 192

    private val cache = object : LruCache<String, Bitmap>(MaxEntries) {}

    fun get(uri: Uri, targetSizePx: Int): Bitmap? = cache.get(key(uri, targetSizePx))

    fun put(uri: Uri, targetSizePx: Int, bitmap: Bitmap) {
        cache.put(key(uri, targetSizePx), bitmap)
    }

    private fun key(uri: Uri, targetSizePx: Int): String = "${uri}@$targetSizePx"
}
