package com.metro.photos.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoLogic {
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun sortByDateDesc(photos: List<PhotoItem>): List<PhotoItem> =
        photos.sortedByDescending { it.dateTakenMs }

    fun groupByMonth(photos: List<PhotoItem>): List<DateGroup> {
        if (photos.isEmpty()) return emptyList()
        return sortByDateDesc(photos)
            .groupBy { monthLabel(it.dateTakenMs) }
            .map { (label, groupPhotos) ->
                DateGroup(
                    label = label,
                    sortKey = groupPhotos.maxOf { it.dateTakenMs },
                    photos = sortByDateDesc(groupPhotos),
                )
            }
            .sortedByDescending { it.sortKey }
    }

    fun groupByAlbum(photos: List<PhotoItem>): List<AlbumGroup> {
        if (photos.isEmpty()) return emptyList()
        return photos
            .groupBy { it.bucketId }
            .map { (bucketId, bucketPhotos) ->
                val sorted = sortByDateDesc(bucketPhotos)
                val cover = sorted.firstOrNull()?.uri
                AlbumGroup(
                    bucketId = bucketId,
                    name = sorted.first().bucketName.ifBlank { "Camera Roll" },
                    coverUri = cover,
                    count = sorted.size,
                    photos = sorted,
                )
            }
            .sortedByDescending { it.count }
    }

    fun filterFavorites(photos: List<PhotoItem>, favoriteIds: Set<Long>): List<PhotoItem> =
        sortByDateDesc(photos.filter { favoriteIds.contains(it.id) })

    fun photosForAlbum(allPhotos: List<PhotoItem>, bucketId: String): List<PhotoItem> =
        sortByDateDesc(allPhotos.filter { it.bucketId == bucketId })

    fun viewerList(
        allPhotos: List<PhotoItem>,
        favoriteIds: Set<Long>,
        collection: ViewerCollection,
        bucketId: String?,
    ): List<PhotoItem> = when (collection) {
        ViewerCollection.All -> sortByDateDesc(allPhotos)
        ViewerCollection.Album -> photosForAlbum(allPhotos, bucketId.orEmpty())
        ViewerCollection.Favorites -> filterFavorites(allPhotos, favoriteIds)
    }

    fun monthLabel(epochMs: Long): String {
        if (epochMs <= 0L) return "Unknown"
        return monthFormat.format(Date(epochMs))
    }
}
