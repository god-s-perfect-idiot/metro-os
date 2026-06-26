package com.metro.photos.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.photos.data.AlbumGroup
import com.metro.photos.data.DateGroup
import com.metro.photos.data.FavoritesStore
import com.metro.photos.data.MediaStoreRepository
import com.metro.photos.data.PhotoItem
import com.metro.photos.data.PhotoLogic
import com.metro.photos.data.ViewerCollection

class PhotosState(context: Context) {
    private val appContext = context.applicationContext
    private val repository = MediaStoreRepository(appContext)
    private val favoritesStore = FavoritesStore(appContext)

    var route by mutableStateOf(PhotosRoute.Collection)
        private set

    var pivot by mutableStateOf(CollectionPivot.All)
        private set

    var photos by mutableStateOf<List<PhotoItem>>(emptyList())
        private set

    var favoriteIds by mutableStateOf(favoritesStore.load())
        private set

    var selectedAlbum by mutableStateOf<AlbumGroup?>(null)
        private set

    var viewerContext by mutableStateOf(ViewerContext(ViewerCollection.All))
        private set

    var viewerIndex by mutableIntStateOf(0)
        private set

    var hasMediaPermission by mutableStateOf(false)
        private set

    var skippedPermissions by mutableStateOf(false)
        private set

    val needsPermissionGate: Boolean
        get() = !hasMediaPermission && !skippedPermissions

    val dateGroups: List<DateGroup>
        get() = PhotoLogic.groupByMonth(photos)

    val albums: List<AlbumGroup>
        get() = PhotoLogic.groupByAlbum(photos)

    val favoritePhotos: List<PhotoItem>
        get() = PhotoLogic.filterFavorites(photos, favoriteIds)

    val viewerPhotos: List<PhotoItem>
        get() = PhotoLogic.viewerList(
            allPhotos = photos,
            favoriteIds = favoriteIds,
            collection = viewerContext.collection,
            bucketId = viewerContext.bucketId,
        )

    fun refreshPermissions(context: Context) {
        hasMediaPermission = hasMediaReadPermission(context)
    }

    fun onPermissionGranted() {
        hasMediaPermission = true
        reloadPhotos()
    }

    fun continueWithoutPhotos() {
        skippedPermissions = true
        photos = emptyList()
    }

    fun reloadPhotos() {
        if (!hasMediaPermission) return
        photos = repository.loadImages()
        favoriteIds = favoritesStore.load()
    }

    fun setPivot(index: Int) {
        pivot = CollectionPivot.fromIndex(index)
    }

    fun openAlbum(album: AlbumGroup) {
        selectedAlbum = album
        route = PhotosRoute.AlbumDetail
    }

    fun openViewer(
        collection: ViewerCollection,
        photoId: Long,
        bucketId: String? = null,
    ) {
        val list = PhotoLogic.viewerList(photos, favoriteIds, collection, bucketId)
        val index = list.indexOfFirst { it.id == photoId }.coerceAtLeast(0)
        viewerContext = ViewerContext(collection, bucketId, index)
        viewerIndex = index
        route = PhotosRoute.Viewer
    }

    fun updateViewerIndex(index: Int) {
        viewerIndex = index.coerceIn(0, (viewerPhotos.size - 1).coerceAtLeast(0))
    }

    fun toggleFavorite(photoId: Long) {
        favoriteIds = favoritesStore.toggle(photoId)
    }

    fun isFavorite(photoId: Long): Boolean = favoriteIds.contains(photoId)

    fun navigateBack(): Boolean = when (route) {
        PhotosRoute.Viewer -> {
            route = if (viewerContext.collection == ViewerCollection.Album) {
                PhotosRoute.AlbumDetail
            } else {
                PhotosRoute.Collection
            }
            true
        }
        PhotosRoute.AlbumDetail -> {
            route = PhotosRoute.Collection
            pivot = CollectionPivot.Albums
            true
        }
        PhotosRoute.Collection -> false
    }

    private fun hasMediaReadPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
