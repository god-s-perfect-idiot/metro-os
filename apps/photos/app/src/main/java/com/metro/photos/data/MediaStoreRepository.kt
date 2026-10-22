package com.metro.photos.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class MediaStoreRepository(private val context: Context) {
    private val contentResolver = context.contentResolver

    fun loadImages(): List<PhotoItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

        val results = mutableListOf<PhotoItem>()
        contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val dateTaken = cursor.getLong(dateTakenCol)
                val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                results += PhotoItem(
                    id = id,
                    uri = uri,
                    displayName = cursor.getString(nameCol).orEmpty(),
                    bucketId = cursor.getString(bucketIdCol).orEmpty(),
                    bucketName = cursor.getString(bucketNameCol).orEmpty(),
                    dateTakenMs = if (dateTaken > 0L) dateTaken else dateAdded,
                    mimeType = cursor.getString(mimeCol).orEmpty(),
                )
            }
        }
        return results
    }

    /**
     * Resolves a VIEW-intent URI into a [PhotoItem]. Prefers MediaStore metadata when the URI
     * is a media content URI; otherwise builds a transient item from the granted URI.
     */
    fun resolveViewUri(uri: Uri): PhotoItem {
        queryByUri(uri)?.let { return it }
        PhotoLogic.mediaStoreId(uri)?.let { id ->
            queryById(id)?.let { return it }
        }
        val mime = contentResolver.getType(uri).orEmpty().ifBlank { "image/*" }
        val name = uri.lastPathSegment.orEmpty().substringAfterLast('/').ifBlank { "photo" }
        val id = PhotoLogic.mediaStoreId(uri) ?: uri.toString().hashCode().toLong()
        return PhotoItem(
            id = id,
            uri = uri,
            displayName = name,
            bucketId = "",
            bucketName = "",
            dateTakenMs = 0L,
            mimeType = mime,
        )
    }

    /**
     * Deletes [uri] with no in-app confirm. Tries a direct MediaStore delete first; only
     * returns [PhotoDeleteResult.NeedUserConsent] when Android requires a system sheet.
     */
    fun deleteImage(uri: Uri): PhotoDeleteResult {
        return try {
            val rows = contentResolver.delete(uri, null, null)
            if (rows > 0) {
                PhotoDeleteResult.Deleted
            } else {
                PhotoDeleteResult.Failed
            }
        } catch (e: SecurityException) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    try {
                        val request = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
                        PhotoDeleteResult.NeedUserConsent(request.intentSender)
                    } catch (_: Exception) {
                        PhotoDeleteResult.Failed
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException -> {
                    PhotoDeleteResult.NeedUserConsent(e.userAction.actionIntent.intentSender)
                }
                else -> PhotoDeleteResult.Failed
            }
        } catch (_: Exception) {
            PhotoDeleteResult.Failed
        }
    }

    private fun queryByUri(uri: Uri): PhotoItem? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
        )
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                readPhoto(cursor, uri)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun queryById(id: Long): PhotoItem? {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val itemUri = ContentUris.withAppendedId(collection, id)
        return queryByUri(itemUri)
    }

    private fun readPhoto(cursor: android.database.Cursor, uri: Uri): PhotoItem {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val id = cursor.getLong(idCol)
        val dateTaken = cursor.getLong(dateTakenCol)
        val dateAdded = cursor.getLong(dateAddedCol) * 1000L
        return PhotoItem(
            id = id,
            uri = uri,
            displayName = cursor.getString(nameCol).orEmpty(),
            bucketId = cursor.getString(bucketIdCol).orEmpty(),
            bucketName = cursor.getString(bucketNameCol).orEmpty(),
            dateTakenMs = if (dateTaken > 0L) dateTaken else dateAdded,
            mimeType = cursor.getString(mimeCol).orEmpty(),
        )
    }
}
