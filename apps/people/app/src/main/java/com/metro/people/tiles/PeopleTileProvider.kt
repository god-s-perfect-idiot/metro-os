package com.metro.people.tiles

import android.content.ContentUris
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.ContactsContract
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider
import java.io.File

class PeopleTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        if (tileId != MetroTileContract.DEFAULT_TILE_ID) return null
        val ctx = context ?: return null
        return PeopleTileDataSource(ctx).buildTileData()
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val contactId = uri.lastPathSegment?.toLongOrNull() ?: return null
        if (!uri.path.orEmpty().startsWith("/photo/")) return null
        val ctx = context ?: return null
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val input = ContactsContract.Contacts.openContactPhotoInputStream(ctx.contentResolver, contactUri, true)
            ?: return null
        val cache = File(ctx.cacheDir, "tile_photo_$contactId.jpg")
        input.use { stream ->
            cache.outputStream().use { out -> stream.copyTo(out) }
        }
        if (!cache.exists() || cache.length() == 0L) return null
        return AssetFileDescriptor(
            ParcelFileDescriptor.open(cache, ParcelFileDescriptor.MODE_READ_ONLY),
            0,
            cache.length(),
        )
    }
}
