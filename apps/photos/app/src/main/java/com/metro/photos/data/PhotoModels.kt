package com.metro.photos.data

import android.net.Uri

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val bucketId: String,
    val bucketName: String,
    val dateTakenMs: Long,
    val mimeType: String,
)

data class AlbumGroup(
    val bucketId: String,
    val name: String,
    val coverUri: Uri?,
    val count: Int,
    val photos: List<PhotoItem>,
)

data class DateGroup(
    val label: String,
    val sortKey: Long,
    val photos: List<PhotoItem>,
)

enum class ViewerCollection {
    All,
    Album,
    Favorites,
}
