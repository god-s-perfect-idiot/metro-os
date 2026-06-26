package com.metro.photos.tiles

import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider
import java.io.File

class PhotosTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        if (tileId != com.metro.system.MetroTileContract.DEFAULT_TILE_ID) return null
        val ctx = context ?: return null
        return PhotosTileDataSource(ctx).buildTileData()
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val photoId = uri.lastPathSegment?.toLongOrNull() ?: return null
        if (!uri.path.orEmpty().startsWith("/photo/")) return null
        val ctx = context ?: return null
        val cache = File(ctx.cacheDir, "tile_photo_$photoId.jpg")
        if (!cache.exists() || cache.length() == 0L) return null
        return AssetFileDescriptor(
            ParcelFileDescriptor.open(cache, ParcelFileDescriptor.MODE_READ_ONLY),
            0,
            cache.length(),
        )
    }
}
