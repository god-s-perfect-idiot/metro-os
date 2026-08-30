package com.metro.launcher.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileGridCell
import com.metro.system.MetroTilePhotoGrid

/**
 * Builds Photos-style (cycle) live-tile payloads from device MediaStore for apps on the
 * Settings → connected apps → Gallery apps list.
 */
object GalleryLiveTileStore {
    private const val CACHE_TTL_MS = 30_000L
    private const val MAX_CELLS = MetroTileContract.MAX_PHOTO_GRID_CELLS

    @Volatile
    private var cached: CachedGrid? = null

    private data class CachedGrid(
        val grid: MetroTilePhotoGrid,
        val builtAtMs: Long,
    )

    fun clearCache() {
        cached = null
    }

    /**
     * Cycle photo grid for a connected gallery app, or null when media permission is missing
     * and no usable cells can be built.
     */
    fun photoGrid(context: Context): MetroTilePhotoGrid? {
        val now = System.currentTimeMillis()
        cached?.let { hit ->
            if (now - hit.builtAtMs < CACHE_TTL_MS) return hit.grid
        }
        val accentHex = MetroPreferences(context).accentColorHex
        val grid = if (hasMediaPermission(context)) {
            val uris = queryRecentImageUris(context, MAX_CELLS)
            GalleryLiveTileLogic.gridFromUris(uris, accentHex)
        } else {
            null
        }
        if (grid != null) {
            cached = CachedGrid(grid, now)
        }
        return grid
    }

    fun hasMediaPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun queryRecentImageUris(context: Context, limit: Int): List<String> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder =
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
        val results = mutableListOf<String>()
        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext() && results.size < limit) {
                    val id = cursor.getLong(idCol)
                    results += ContentUris.withAppendedId(collection, id).toString()
                }
            }
        }
        return results
    }
}

object GalleryLiveTileLogic {
    fun gridFromUris(imageUris: List<String>, accentHex: String): MetroTilePhotoGrid {
        val shuffled = imageUris.filter { it.isNotBlank() }.shuffled()
        val cells = if (shuffled.isEmpty()) {
            accentFallbackCells(accentHex)
        } else {
            shuffled.mapIndexed { index, uri ->
                MetroTileGridCell(
                    colorHex = accentShade(index, accentHex),
                    imageUri = uri,
                )
            }
        }
        return MetroTilePhotoGrid(cells = cells, cycle = true)
    }

    fun accentFallbackCells(accentHex: String, count: Int = 4): List<MetroTileGridCell> =
        List(count) { index -> MetroTileGridCell(colorHex = accentShade(index, accentHex)) }

    private fun accentShade(index: Int, accentHex: String): String {
        // Keep shades deterministic without android.graphics (unit-test friendly).
        val normalized = accentHex.removePrefix("#").uppercase()
        if (normalized.length != 6) return MetroPreferences.DEFAULT_ACCENT_HEX
        val step = index % 4
        // Darken slightly for variety: multiply each channel toward black.
        val factor = when (step) {
            0 -> 0.55
            1 -> 0.7
            2 -> 0.85
            else -> 1.0
        }
        fun channel(hex: String): Int =
            ((hex.toInt(16) * factor).toInt()).coerceIn(0, 255)
        val r = channel(normalized.substring(0, 2))
        val g = channel(normalized.substring(2, 4))
        val b = channel(normalized.substring(4, 6))
        return String.format("#%02X%02X%02X", r, g, b)
    }
}
