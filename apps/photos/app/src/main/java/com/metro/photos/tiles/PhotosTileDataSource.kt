package com.metro.photos.tiles

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.core.content.ContextCompat
import com.metro.photos.data.MediaStoreRepository
import com.metro.photos.data.PhotoLogic
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTileGridCell
import com.metro.system.MetroTilePhotoGrid
import java.io.File
import java.io.FileOutputStream

class PhotosTileDataSource(private val context: Context) {
    private val appContext = context.applicationContext
    private val authority = MetroTileContract.authorityFor(appContext.packageName)

    fun buildTileData(): MetroTileData {
        val accentHex = MetroPreferences(appContext).accentColorHex
        val cells = if (hasMediaPermission()) {
            val photos = PhotoLogic.sortByDateDesc(MediaStoreRepository(appContext).loadImages())
            PhotosTileLogic.cellsFromPhotos(photos, authority, accentHex, appContext)
        } else {
            PhotosTileLogic.fallbackCells(PhotosTileLogic.MAX_CELLS, accentHex)
        }
        return MetroTileData(
            title = MetroAppRegistry.label(appContext.packageName) ?: "Photos",
            backgroundColorHex = accentHex,
            photoGrid = MetroTilePhotoGrid(cells, cycle = true),
            deepLinkUri = null,
        )
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}

object PhotosTileLogic {
    const val MAX_CELLS = MetroTileContract.MAX_PHOTO_GRID_CELLS

    fun photoUri(authority: String, photoId: Long): String =
        "content://$authority/photo/$photoId"

    fun fallbackCells(count: Int, accentHex: String): List<MetroTileGridCell> {
        val shades = PeopleTileAccentShades.shades(accentHex, count)
        return shades.map { MetroTileGridCell(colorHex = it) }
    }

    fun cellsFromPhotos(
        photos: List<com.metro.photos.data.PhotoItem>,
        authority: String,
        accentHex: String,
        context: Context,
    ): List<MetroTileGridCell> {
        val recent = photos.take(MAX_CELLS)
        val cells = recent.mapIndexed { index, photo ->
            cacheThumbnail(context, photo.id, photo.uri)
            MetroTileGridCell(
                colorHex = PeopleTileAccentShades.colorForIndex(index, accentHex),
                imageUri = photoUri(authority, photo.id),
            )
        }
        return if (cells.size >= MAX_CELLS) {
            cells
        } else {
            cells + fallbackCells(MAX_CELLS - cells.size, accentHex)
        }
    }

    private fun cacheThumbnail(context: Context, photoId: Long, uri: android.net.Uri) {
        val cache = File(context.cacheDir, "tile_photo_$photoId.jpg")
        if (cache.exists() && cache.length() > 0L) return
        runCatching {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver,
                    photoId,
                    android.provider.MediaStore.Images.Thumbnails.MINI_KIND,
                    null,
                )
            }
            FileOutputStream(cache).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        }
    }
}

private object PeopleTileAccentShades {
    fun shades(accentHex: String, count: Int): List<String> {
        if (count <= 0) return emptyList()
        val parsed = runCatching { android.graphics.Color.parseColor(accentHex) }.getOrElse {
            android.graphics.Color.parseColor(MetroPreferences.DEFAULT_ACCENT_HEX)
        }
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(parsed, hsv)
        return List(count) { index ->
            val step = index.toFloat() / (count - 1).coerceAtLeast(1)
            val value = (0.45f + step * 0.55f).coerceIn(0.35f, 1f)
            val saturation = (hsv[1] * (0.7f + (index % 3) * 0.12f)).coerceIn(0.35f, 1f)
            toHex(hsv[0], saturation, value)
        }
    }

    fun colorForIndex(index: Int, accentHex: String): String =
        shades(accentHex, MAX_CELLS)[index % MAX_CELLS]

    private const val MAX_CELLS = MetroTileContract.MAX_PHOTO_GRID_CELLS

    private fun toHex(hue: Float, saturation: Float, value: Float): String {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        return String.format("#%06X", rgb and 0xFFFFFF)
    }
}
