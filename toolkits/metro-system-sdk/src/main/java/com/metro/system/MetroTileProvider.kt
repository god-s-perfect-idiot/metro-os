package com.metro.system

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle

/**
 * Base tile widget provider. Subclasses implement [buildTileData] and register in the manifest
 * with authority [MetroTileContract.authorityFor] and metadata [MetroTileContract.METADATA_TILE_PROVIDER].
 */
abstract class MetroTileProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val tileId = uri.lastPathSegment ?: return null
        if (!uri.path.orEmpty().startsWith("/tile/")) return null
        val data = buildTileData(tileId) ?: return null
        return data.toCursor()
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "getTile") return null
        val tileId = extras?.getString("tile_id")
            ?: arg?.takeIf { it.isNotBlank() }
            ?: MetroTileContract.DEFAULT_TILE_ID
        val data = buildTileData(tileId) ?: return null
        return data.toBundle()
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.metro.tile"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    protected abstract fun buildTileData(tileId: String): MetroTileData?

    private fun MetroTileData.toBundle(): Bundle = Bundle().apply {
        putString(MetroTileContract.Columns.TITLE, title)
        putString(MetroTileContract.Columns.BACKGROUND_COLOR, backgroundColorHex)
        counter?.let { putInt(MetroTileContract.Columns.COUNTER, it) }
        iconUri?.let { putString(MetroTileContract.Columns.ICON_URI, it) }
        imageUri?.let { putString(MetroTileContract.Columns.IMAGE_URI, it) }
        backFaceTitle?.let { putString(MetroTileContract.Columns.BACK_FACE_TITLE, it) }
        backFaceImageUri?.let { putString(MetroTileContract.Columns.BACK_FACE_IMAGE_URI, it) }
        deepLinkUri?.let { putString(MetroTileContract.Columns.DEEP_LINK_URI, it) }
        MetroTilePhotoGridCodec.encode(photoGrid)?.let {
            putString(MetroTileContract.Columns.PHOTO_GRID, it)
        }
    }

    private fun MetroTileData.toCursor(): Cursor {
        val columns = arrayOf(
            MetroTileContract.Columns.TITLE,
            MetroTileContract.Columns.BACKGROUND_COLOR,
            MetroTileContract.Columns.COUNTER,
            MetroTileContract.Columns.ICON_URI,
            MetroTileContract.Columns.IMAGE_URI,
            MetroTileContract.Columns.BACK_FACE_TITLE,
            MetroTileContract.Columns.BACK_FACE_IMAGE_URI,
            MetroTileContract.Columns.DEEP_LINK_URI,
            MetroTileContract.Columns.PHOTO_GRID,
        )
        val cursor = MatrixCursor(columns)
        cursor.addRow(
            arrayOf(
                title,
                backgroundColorHex,
                counter,
                iconUri,
                imageUri,
                backFaceTitle,
                backFaceImageUri,
                deepLinkUri,
                MetroTilePhotoGridCodec.encode(photoGrid),
            ),
        )
        return cursor
    }
}
