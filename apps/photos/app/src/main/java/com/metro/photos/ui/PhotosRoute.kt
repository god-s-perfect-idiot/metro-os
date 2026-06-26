package com.metro.photos.ui

import com.metro.photos.data.ViewerCollection

enum class PhotosRoute {
    Collection,
    AlbumDetail,
    Viewer,
}

enum class CollectionPivot(val index: Int) {
    All(0),
    Albums(1),
    Favorites(2),
    ;

    companion object {
        fun fromIndex(index: Int): CollectionPivot =
            entries.firstOrNull { it.index == index } ?: All
    }
}

data class ViewerContext(
    val collection: ViewerCollection,
    val bucketId: String? = null,
    val initialIndex: Int = 0,
)
