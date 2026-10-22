package com.metro.photos.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
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
import com.metro.photos.data.PhotoDeleteResult
import com.metro.photos.data.PhotoItem
import com.metro.photos.data.PhotoLogic
import com.metro.photos.data.ViewerCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotosState(context: Context) {
    private val appContext = context.applicationContext
    private val repository = MediaStoreRepository(appContext)
    private val favoritesStore = FavoritesStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null
    private var loadGeneration = 0
    private var pendingViewUri: Uri? = null

    var route by mutableStateOf(PhotosRoute.Collection)
        private set

    var pivot by mutableStateOf(CollectionPivot.All)
        private set

    var photos by mutableStateOf<List<PhotoItem>>(emptyList())
        private set

    var favoriteIds by mutableStateOf(favoritesStore.load())
        private set

    var dateGroups by mutableStateOf<List<DateGroup>>(emptyList())
        private set

    var albums by mutableStateOf<List<AlbumGroup>>(emptyList())
        private set

    var favoritePhotos by mutableStateOf<List<PhotoItem>>(emptyList())
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

    /** False until the first [refreshPermissions] completes — avoids flashing the permission gate. */
    var permissionsChecked by mutableStateOf(false)
        private set

    /**
     * True while MediaStore is being queried and the collection is still empty.
     * Resume refreshes keep existing photos on screen.
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * True when the current viewer session was opened from an external ACTION_VIEW intent.
     * Back from that viewer should exit the activity instead of returning to the hub.
     */
    var openedFromViewIntent by mutableStateOf(false)
        private set

    var pendingDeleteConsent by mutableStateOf<IntentSender?>(null)
        private set

    /**
     * Set when Back from an external VIEW session should close the activity.
     * [PhotosShell] / [MainActivity] observe and finish.
     */
    var exitAppRequested by mutableStateOf(false)
        private set

    val needsPermissionGate: Boolean
        get() = !hasMediaPermission && !skippedPermissions && pendingViewUri == null &&
            route != PhotosRoute.Viewer

    val viewerPhotos: List<PhotoItem>
        get() = PhotoLogic.viewerList(
            allPhotos = photos,
            favoriteIds = favoriteIds,
            collection = viewerContext.collection,
            bucketId = viewerContext.bucketId,
            singlePhoto = viewerContext.singlePhoto,
        )

    fun refreshPermissions(context: Context) {
        hasMediaPermission = hasMediaReadPermission(context)
        permissionsChecked = true
        // Without media permission, open the granted URI immediately (skip the gate).
        // With permission, [reloadPhotos] matches into the library after the query returns.
        if (pendingViewUri != null && !hasMediaPermission) {
            openPendingViewIfReady()
        }
    }

    fun onPermissionGranted() {
        hasMediaPermission = true
        reloadPhotos()
    }

    fun continueWithoutPhotos() {
        skippedPermissions = true
        loadGeneration++
        loadJob?.cancel()
        isLoading = false
        photos = emptyList()
        refreshDerived()
        openPendingViewIfReady()
    }

    fun reloadPhotos() {
        if (!hasMediaPermission) {
            openPendingViewIfReady()
            return
        }
        val generation = ++loadGeneration
        loadJob?.cancel()
        if (photos.isEmpty()) {
            isLoading = true
        }
        loadJob = scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    repository.loadImages()
                }
                if (generation != loadGeneration) return@launch
                photos = loaded
                favoriteIds = favoritesStore.load()
                refreshDerived()
            } finally {
                if (generation == loadGeneration) {
                    isLoading = false
                    openPendingViewIfReady()
                }
            }
        }
    }

    /**
     * Handles ACTION_VIEW / Open-with for a single image URI.
     * Opens the viewer once permissions are known; does not require the permission gate when
     * the URI was granted by the sender.
     */
    fun handleViewIntent(uri: Uri?) {
        if (uri == null) return
        pendingViewUri = uri
        openedFromViewIntent = true
        if (!permissionsChecked) return
        if (hasMediaPermission) {
            if (photos.isNotEmpty()) {
                openPendingViewIfReady()
            } else {
                reloadPhotos()
            }
        } else {
            openPendingViewIfReady()
        }
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
        openedFromViewIntent = false
        val list = PhotoLogic.viewerList(photos, favoriteIds, collection, bucketId)
        val index = list.indexOfFirst { it.id == photoId }.coerceAtLeast(0)
        viewerContext = ViewerContext(collection, bucketId, index)
        viewerIndex = index
        route = PhotosRoute.Viewer
    }

    fun updateViewerIndex(index: Int) {
        viewerIndex = index.coerceIn(0, (viewerPhotos.size - 1).coerceAtLeast(0))
        if (viewerContext.collection == ViewerCollection.Single) return
        viewerContext = viewerContext.copy(initialIndex = viewerIndex)
    }

    fun toggleFavorite(photoId: Long) {
        favoriteIds = favoritesStore.toggle(photoId)
        refreshDerived()
    }

    fun addFavorite(photoId: Long) {
        favoriteIds = favoritesStore.add(photoId)
        refreshDerived()
    }

    fun removeFavorite(photoId: Long) {
        favoriteIds = favoritesStore.remove(photoId)
        refreshDerived()
    }

    fun deleteCurrent() {
        val photo = viewerPhotos.getOrNull(viewerIndex) ?: return
        when (val result = repository.deleteImage(photo.uri)) {
            PhotoDeleteResult.Deleted -> onPhotoDeleted(photo.id)
            PhotoDeleteResult.Failed -> Unit
            is PhotoDeleteResult.NeedUserConsent -> pendingDeleteConsent = result.intentSender
        }
    }

    fun consumeDeleteConsent(): IntentSender? {
        val sender = pendingDeleteConsent
        pendingDeleteConsent = null
        return sender
    }

    fun onPhotoDeleted(photoId: Long) {
        favoritesStore.remove(photoId)
        favoriteIds = favoritesStore.load()
        photos = photos.filterNot { it.id == photoId }
        if (viewerContext.collection == ViewerCollection.Single) {
            viewerContext = viewerContext.copy(singlePhoto = null)
        }
        refreshDerived()
        val remaining = viewerPhotos
        when {
            remaining.isEmpty() -> navigateBack()
            viewerIndex >= remaining.size -> viewerIndex = remaining.lastIndex
        }
    }

    fun shareCurrent(context: Context) {
        val photo = viewerPhotos.getOrNull(viewerIndex) ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = photo.mimeType.ifBlank { "image/*" }
            putExtra(Intent.EXTRA_STREAM, photo.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("", photo.uri)
        }
        val chooser = Intent.createChooser(send, null)
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // No handler — ignore.
        }
    }

    private fun refreshDerived() {
        dateGroups = PhotoLogic.groupByMonth(photos)
        albums = PhotoLogic.groupByAlbum(photos)
        favoritePhotos = PhotoLogic.filterFavorites(photos, favoriteIds)
        if (viewerContext.collection == ViewerCollection.Single) {
            val single = viewerContext.singlePhoto
            if (single != null) {
                val refreshed = PhotoLogic.findByUri(photos, single.uri) ?: single
                viewerContext = viewerContext.copy(singlePhoto = refreshed)
            }
        }
    }

    fun isFavorite(photoId: Long): Boolean = favoriteIds.contains(photoId)

    fun consumeExitAppRequest(): Boolean {
        if (!exitAppRequested) return false
        exitAppRequested = false
        return true
    }

    fun navigateBack(): Boolean = when (route) {
        PhotosRoute.Viewer -> {
            if (openedFromViewIntent) {
                openedFromViewIntent = false
                route = PhotosRoute.Collection
                exitAppRequested = true
                true
            } else {
                route = if (viewerContext.collection == ViewerCollection.Album) {
                    PhotosRoute.AlbumDetail
                } else {
                    PhotosRoute.Collection
                }
                true
            }
        }
        PhotosRoute.AlbumDetail -> {
            route = PhotosRoute.Collection
            pivot = CollectionPivot.Albums
            true
        }
        PhotosRoute.Collection -> false
    }

    private fun openPendingViewIfReady() {
        val uri = pendingViewUri ?: return
        if (hasMediaPermission && isLoading) {
            // Wait for MediaStore load so we can match into the library collection.
            return
        }
        pendingViewUri = null
        val matched = PhotoLogic.findByUri(photos, uri)
        if (matched != null) {
            openViewer(ViewerCollection.All, matched.id)
            openedFromViewIntent = true
        } else {
            openResolvedView(repository.resolveViewUri(uri))
        }
    }

    private fun openResolvedView(photo: PhotoItem) {
        openedFromViewIntent = true
        viewerContext = ViewerContext(
            collection = ViewerCollection.Single,
            singlePhoto = photo,
            initialIndex = 0,
        )
        viewerIndex = 0
        route = PhotosRoute.Viewer
        skippedPermissions = true
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
