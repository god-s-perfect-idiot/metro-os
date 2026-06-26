package com.metro.system

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle

/**
 * Tile widget provider contract — apps export live-tile data; launcher reads.
 *
 * Manifest metadata key: [METADATA_TILE_PROVIDER] = true on the tile ContentProvider.
 * Authority: `{packageName}.tiles`
 * Path: `/tile/{tileId}` (default tile id: [DEFAULT_TILE_ID])
 */
object MetroTileContract {
    const val METADATA_TILE_PROVIDER = "com.metro.tile.provider"
    const val DEFAULT_TILE_ID = "primary"

    object Columns {
        const val TITLE = "title"
        const val BACKGROUND_COLOR = "background_color"
        const val COUNTER = "counter"
        const val ICON_URI = "icon_uri"
        const val IMAGE_URI = "image_uri"
        const val BACK_FACE_TITLE = "back_face_title"
        const val BACK_FACE_IMAGE_URI = "back_face_image_uri"
        const val DEEP_LINK_URI = "deep_link_uri"
        const val PHOTO_GRID = "photo_grid"
        const val AGENDA = "agenda"
    }

    /** Max cells exported for wide (4×2 → 6×3) People-style photo grids. */
    const val MAX_PHOTO_GRID_CELLS = 18

    fun photoGridDimensions(colSpan: Int, rowSpan: Int): Pair<Int, Int>? = when {
        colSpan >= 4 && rowSpan >= 2 -> 6 to 3
        colSpan >= 2 && rowSpan >= 2 -> 3 to 3
        else -> null
    }

    fun authorityFor(packageName: String): String = "$packageName.tiles"

    fun tileUri(packageName: String, tileId: String = DEFAULT_TILE_ID): Uri =
        Uri.parse("content://${authorityFor(packageName)}/tile/$tileId")

    fun hasTileProvider(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            val info = packageManager.getProviderInfo(
                android.content.ComponentName(packageName, "${packageName}.tiles.TileProvider"),
                PackageManager.GET_META_DATA,
            )
            info.metaData?.getBoolean(METADATA_TILE_PROVIDER, false) == true
        } catch (_: PackageManager.NameNotFoundException) {
            // Provider may use a custom class name — probe authority via resolveContentProvider.
            packageManager.resolveContentProvider(authorityFor(packageName), 0) != null
        }
    }

    fun readTile(
        resolver: ContentResolver,
        packageName: String,
        tileId: String = DEFAULT_TILE_ID,
    ): MetroTileData? {
        val uri = tileUri(packageName, tileId)
        val extras = Bundle().apply { putString("tile_id", tileId) }
        return try {
            resolver.call(uri, "getTile", tileId, extras)?.toTileData()
        } catch (_: Exception) {
            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return null
                    cursor.toTileData()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun Bundle.toTileData(): MetroTileData? {
        val title = getString(Columns.TITLE) ?: return null
        val background = getString(Columns.BACKGROUND_COLOR) ?: MetroPreferences.DEFAULT_ACCENT_HEX
        return MetroTileData(
            title = title,
            backgroundColorHex = background,
            counter = if (containsKey(Columns.COUNTER)) getInt(Columns.COUNTER) else null,
            iconUri = getString(Columns.ICON_URI),
            imageUri = getString(Columns.IMAGE_URI),
            backFaceTitle = getString(Columns.BACK_FACE_TITLE),
            backFaceImageUri = getString(Columns.BACK_FACE_IMAGE_URI),
            deepLinkUri = getString(Columns.DEEP_LINK_URI),
            photoGrid = MetroTilePhotoGridCodec.decode(getString(Columns.PHOTO_GRID)),
            agenda = MetroTileAgendaCodec.decode(getString(Columns.AGENDA)),
        )
    }

    private fun android.database.Cursor.toTileData(): MetroTileData? {
        fun col(name: String): Int = getColumnIndex(name)
        val titleIdx = col(Columns.TITLE)
        if (titleIdx < 0) return null
        val title = getString(titleIdx) ?: return null
        val bgIdx = col(Columns.BACKGROUND_COLOR)
        val background = if (bgIdx >= 0) getString(bgIdx) else null
        val counterIdx = col(Columns.COUNTER)
        val gridIdx = col(Columns.PHOTO_GRID)
        return MetroTileData(
            title = title,
            backgroundColorHex = background ?: MetroPreferences.DEFAULT_ACCENT_HEX,
            counter = if (counterIdx >= 0 && !isNull(counterIdx)) getInt(counterIdx) else null,
            iconUri = col(Columns.ICON_URI).let { if (it >= 0) getString(it) else null },
            imageUri = col(Columns.IMAGE_URI).let { if (it >= 0) getString(it) else null },
            backFaceTitle = col(Columns.BACK_FACE_TITLE).let { if (it >= 0) getString(it) else null },
            backFaceImageUri = col(Columns.BACK_FACE_IMAGE_URI).let { if (it >= 0) getString(it) else null },
            deepLinkUri = col(Columns.DEEP_LINK_URI).let { if (it >= 0) getString(it) else null },
            photoGrid = if (gridIdx >= 0) MetroTilePhotoGridCodec.decode(getString(gridIdx)) else null,
            agenda = col(Columns.AGENDA).let {
                if (it >= 0) MetroTileAgendaCodec.decode(getString(it)) else null
            },
        )
    }
}
