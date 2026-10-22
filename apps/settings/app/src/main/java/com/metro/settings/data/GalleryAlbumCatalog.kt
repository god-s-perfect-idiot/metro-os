package com.metro.settings.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

/**
 * MediaStore albums for Settings → connected apps → Gallery apps → Albums.
 */
data class GalleryAlbumEntry(
    val bucketId: String,
    val name: String,
    val coverUri: Uri?,
    val count: Int,
)

object GalleryAlbumCatalog {
    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasMediaPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, requiredPermission()) ==
            PackageManager.PERMISSION_GRANTED

    fun listAlbums(context: Context): List<GalleryAlbumEntry> {
        if (!hasMediaPermission(context)) return emptyList()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val sortOrder =
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

        data class Acc(var name: String, var coverId: Long, var count: Int)

        val byBucket = linkedMapOf<String, Acc>()
        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val bucketIdCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                    val bucketNameCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val bucketId = cursor.getString(bucketIdCol).orEmpty()
                        if (bucketId.isBlank()) continue
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(bucketNameCol).orEmpty().ifBlank { "Camera Roll" }
                        val existing = byBucket[bucketId]
                        if (existing == null) {
                            byBucket[bucketId] = Acc(name = name, coverId = id, count = 1)
                        } else {
                            existing.count += 1
                        }
                    }
                }
        }
        return byBucket.map { (bucketId, acc) ->
            GalleryAlbumEntry(
                bucketId = bucketId,
                name = acc.name,
                coverUri = ContentUris.withAppendedId(collection, acc.coverId),
                count = acc.count,
            )
        }.sortedBy { it.name.lowercase() }
    }
}
