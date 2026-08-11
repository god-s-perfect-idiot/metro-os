package com.metro.music.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.metro.music.data.Album
import com.metro.music.data.Artist
import com.metro.music.data.LibraryLogic
import com.metro.music.data.LibrarySource
import com.metro.music.data.LocalLibraryRepository
import com.metro.music.data.Playlist
import com.metro.music.data.ShowingFilter
import com.metro.music.data.Song
import com.metro.music.data.artworkModel
import com.metro.music.data.loadAlbumTintArgb
import com.metro.music.playback.MusicPlaybackService
import com.metro.music.playback.PlaybackLogic
import com.metro.music.ytmusic.YtMusicAuthStore
import com.metro.music.ytmusic.YtMusicClient
import com.metro.ui.MetroJumpListLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MusicRoute {
    Hub,
    Collection,
    AlbumDetail,
    ArtistDetail,
    PlaylistDetail,
    Settings,
    Explore,
}

class MusicState(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val localRepo = LocalLibraryRepository(appContext)
    private val authStore = YtMusicAuthStore(appContext)
    private val ytClient = YtMusicClient(authStore)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var queueJob: Job? = null
    private var playlistJob: Job? = null
    private var libraryJob: Job? = null
    private var exploreJob: Job? = null
    private var tintJob: Job? = null
    private var backdropArtwork: Any? = null
    private var playbackError: String? = null

    var hasAudioPermission by mutableStateOf(false)
        private set
    var libraryLoading by mutableStateOf(true)
        private set
    var localSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var localPlaylists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var ytSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var ytPlaylists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var exploreResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var exploreQuery by mutableStateOf("")
    var exploreLoading by mutableStateOf(false)
        private set
    var showingFilter by mutableStateOf(ShowingFilter.All)
    var ytConnected by mutableStateOf(authStore.connected)
        private set
    var ytSyncing by mutableStateOf(false)
        private set
    var ytSyncMessage by mutableStateOf<String?>(null)
        private set
    var route by mutableStateOf(MusicRoute.Hub)
    /** Hub panes: 0 collection | 1 get music | 2 now playing */
    var hubPage by mutableIntStateOf(0)
    var collectionPage by mutableIntStateOf(0)
    /** Find-by-letter grid over the collection pivots. */
    var jumpListVisible by mutableStateOf(false)
    var jumpToLetter by mutableStateOf<Char?>(null)
    var selectedAlbum by mutableStateOf<Album?>(null)
    var selectedArtist by mutableStateOf<Artist?>(null)
    var selectedPlaylist by mutableStateOf<Playlist?>(null)
    var playlistSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var playlistLoading by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var currentSong by mutableStateOf<Song?>(null)
        private set
    /** Darkened album-art colour behind the hub while a track is loaded; null = plain background. */
    var nowPlayingBackdrop by mutableStateOf<Color?>(null)
        private set
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var loadingPlayback by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)

    val allSongs: List<Song>
        get() = localSongs + ytSongs

    val visibleSongs: List<Song>
        get() = LibraryLogic.filterSongs(allSongs, showingFilter)

    val artists: List<Artist>
        get() = LibraryLogic.artistsFrom(visibleSongs)

    val albums: List<Album>
        get() = LibraryLogic.albumsFrom(visibleSongs)

    val playlists: List<Playlist>
        get() = LibraryLogic.filterPlaylists(localPlaylists + ytPlaylists, showingFilter)

    /** Letters the jump grid can offer for the pivot page on screen (empty = genres). */
    val collectionJumpLetters: Set<Char>
        get() = when (collectionPage) {
            COLLECTION_ARTISTS -> MetroJumpListLogic.activeLetters(artists.map { it.name })
            COLLECTION_ALBUMS -> MetroJumpListLogic.activeLetters(albums.map { it.title })
            COLLECTION_SONGS -> MetroJumpListLogic.activeLetters(visibleSongs.map { it.title })
            COLLECTION_PLAYLISTS -> MetroJumpListLogic.activeLetters(playlists.map { it.title })
            else -> emptySet()
        }

    fun refreshPermissions(context: Context) {
        hasAudioPermission = hasAudioPermission(context)
    }

    fun connectPlayer() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (controllerFuture != future) return@addListener
                val ctrl = runCatching { future.get() }.getOrNull()
                if (ctrl == null) {
                    if (controllerFuture == future) controllerFuture = null
                    return@addListener
                }
                controller = ctrl
                ctrl.addListener(playerListener)
                syncFromPlayer()
                startPositionUpdates()
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun releasePlayer() {
        positionJob?.cancel()
        queueJob?.cancel()
        playlistJob?.cancel()
        libraryJob?.cancel()
        exploreJob?.cancel()
        tintJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    fun reloadLibrary() {
        if (hasAudioPermission) {
            libraryJob?.cancel()
            libraryLoading = true
            libraryJob = scope.launch {
                try {
                    localSongs = withContext(Dispatchers.IO) { localRepo.loadSongs() }
                    localPlaylists = withContext(Dispatchers.IO) { localRepo.loadPlaylists() }
                } finally {
                    if (isActive) libraryLoading = false
                }
                // Library was empty when the controller first connected; re-bind now playing.
                syncFromPlayer()
            }
        } else {
            libraryJob?.cancel()
            localSongs = emptyList()
            localPlaylists = emptyList()
            libraryLoading = false
        }
        refreshYtLibrary()
    }

    fun refreshYtAuth() {
        ytConnected = authStore.connected
        if (ytConnected) {
            refreshYtLibrary()
        } else {
            ytSongs = emptyList()
            ytPlaylists = emptyList()
            ytSyncMessage = null
        }
    }

    fun refreshYtLibrary() {
        if (!authStore.connected) {
            ytSongs = emptyList()
            ytPlaylists = emptyList()
            ytSyncMessage = "Connect YouTube Music in settings"
            return
        }
        scope.launch {
            ytSyncing = true
            ytSyncMessage = "Syncing…"
            try {
                val playlists = withContext(Dispatchers.IO) { ytClient.libraryPlaylists() }
                ytPlaylists = playlists.playlists
                val songs = withContext(Dispatchers.IO) { ytClient.librarySongs() }
                ytSongs = songs.songs
                ytSyncMessage = syncMessage(songs.songs.size, playlists.playlists.size, songs.error, playlists.error)
                syncFromPlayer()
            } finally {
                ytSyncing = false
            }
        }
    }

    fun openCollectionPivot(page: Int) {
        collectionPage = page.coerceIn(0, 4)
        route = MusicRoute.Collection
    }

    fun disconnectYt() {
        authStore.clear()
        ytConnected = false
        ytSongs = emptyList()
        ytPlaylists = emptyList()
    }

    fun openPlaylist(playlist: Playlist) {
        selectedPlaylist = playlist
        playlistSongs = emptyList()
        route = MusicRoute.PlaylistDetail
        playlistJob?.cancel()
        playlistJob = scope.launch {
            playlistLoading = true
            try {
                playlistSongs = withContext(Dispatchers.IO) { loadPlaylistSongs(playlist) }
            } finally {
                playlistLoading = false
            }
        }
    }

    fun searchExplore(query: String) {
        exploreQuery = query
        exploreJob?.cancel()
        if (query.isBlank()) {
            exploreResults = emptyList()
            exploreLoading = false
            return
        }
        exploreJob = scope.launch {
            exploreLoading = true
            try {
                exploreResults = withContext(Dispatchers.IO) { ytClient.searchSongs(query) }
            } finally {
                if (isActive) exploreLoading = false
            }
        }
    }

    /**
     * Starts the tapped song as soon as its stream resolves, then fills the rest of the queue in
     * the background — resolving every YouTube track up front would stall playback for a minute.
     */
    fun playSongs(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val ctrl = controller
        if (ctrl == null) {
            statusMessage = "Player not ready"
            return
        }
        val index = startIndex.coerceIn(0, songs.lastIndex)
        val start = songs[index]
        loadingPlayback = true
        statusMessage = null
        playbackError = null
        queueJob?.cancel()
        queueJob = scope.launch {
            try {
                val startItem = withContext(Dispatchers.IO) { resolvePlayable(start) }
                if (startItem == null) {
                    statusMessage = playbackError ?: "Unable to play"
                    return@launch
                }
                ctrl.setMediaItems(listOf(startItem.second), 0, 0L)
                ctrl.prepare()
                ctrl.play()
                updateCurrentSong(startItem.first)
                hubPage = HUB_NOW_PLAYING
                route = MusicRoute.Hub
            } finally {
                loadingPlayback = false
            }
            fillQueue(ctrl, songs, index)
        }
    }

    /**
     * Every YouTube track costs an Innertube round trip and its stream URL expires, so only a
     * short window either side of the tapped song is materialised.
     */
    private suspend fun fillQueue(ctrl: MediaController, songs: List<Song>, startIndex: Int) {
        val following = songs.drop(startIndex + 1).take(QUEUE_LOOKAHEAD)
        for (song in following) {
            val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
            ctrl.addMediaItem(item.second)
        }
        val preceding = songs.take(startIndex).takeLast(QUEUE_LOOKBEHIND)
        for ((offset, song) in preceding.withIndex()) {
            val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
            ctrl.addMediaItem(offset, item.second)
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    fun toggleShuffle() {
        shuffle = !shuffle
        controller?.shuffleModeEnabled = shuffle
    }

    fun cycleRepeat() {
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = repeatMode
    }

    fun openAlbum(album: Album) {
        selectedAlbum = album
        route = MusicRoute.AlbumDetail
    }

    fun openArtist(artist: Artist) {
        selectedArtist = artist
        route = MusicRoute.ArtistDetail
    }

    fun songsForAlbum(album: Album): List<Song> =
        visibleSongs.filter {
            it.album.equals(album.title, ignoreCase = true) &&
                it.artist.equals(album.artist, ignoreCase = true)
        }

    fun songsForArtist(artist: Artist): List<Song> =
        visibleSongs.filter { it.artist.equals(artist.name, ignoreCase = true) }

    /**
     * Cover of the song's *album* rather than the playing track's own thumbnail — the first track
     * carrying art speaks for the whole record, so the backdrop holds still as the album plays.
     */
    fun albumArtworkModel(song: Song): Any? {
        val cover = allSongs.firstOrNull {
            it.album.equals(song.album, ignoreCase = true) &&
                it.artist.equals(song.artist, ignoreCase = true) &&
                it.artworkUri != null
        } ?: song
        return cover.artworkModel()
    }

    private fun updateCurrentSong(song: Song?) {
        currentSong = song
        refreshBackdrop(song)
    }

    private fun refreshBackdrop(song: Song?) {
        val artwork = song?.let { albumArtworkModel(it) }
        if (artwork == backdropArtwork) return
        backdropArtwork = artwork
        tintJob?.cancel()
        if (artwork == null) {
            nowPlayingBackdrop = null
            return
        }
        tintJob = scope.launch {
            val argb = withContext(Dispatchers.IO) { loadAlbumTintArgb(appContext, artwork) }
            if (backdropArtwork == artwork) {
                nowPlayingBackdrop = argb?.let { Color(it) }
            }
        }
    }

    private suspend fun resolvePlayable(song: Song): Pair<Song, MediaItem>? {
        return when (song.source) {
            LibrarySource.Local -> {
                val uri = song.uri?.toString() ?: return null
                song to MusicPlaybackService.mediaItemFor(song, uri)
            }
            LibrarySource.YouTubeMusic -> {
                val videoId = song.youtubeVideoId ?: return null
                val result = ytClient.resolveStream(videoId)
                val url = result.url
                if (url == null) {
                    playbackError = result.error
                    return null
                }
                song to MusicPlaybackService.mediaItemFor(song, url)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
            updateCurrentSong(resolveSong(mediaItem))
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            syncFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            statusMessage = "Playback failed: ${error.errorCodeName}"
        }
    }

    private fun syncFromPlayer() {
        val ctrl = controller ?: return
        isPlaying = ctrl.isPlaying
        durationMs = ctrl.duration.coerceAtLeast(0L)
        positionMs = ctrl.currentPosition.coerceAtLeast(0L)
        shuffle = ctrl.shuffleModeEnabled
        repeatMode = ctrl.repeatMode
        updateCurrentSong(resolveSong(ctrl.currentMediaItem))
    }

    private fun resolveSong(mediaItem: MediaItem?): Song? =
        PlaybackLogic.resolveCurrentSong(mediaItem, durationMs) { songById(it) }

    private fun songById(id: String): Song? {
        return allSongs.firstOrNull { it.id == id }
            ?: exploreResults.firstOrNull { it.id == id }
            ?: playlistSongs.firstOrNull { it.id == id }
            ?: currentSong?.takeIf { it.id == id }
    }

    private suspend fun loadPlaylistSongs(playlist: Playlist): List<Song> = when (playlist.source) {
        LibrarySource.Local -> {
            val mediaId = playlist.localMediaStoreId ?: return emptyList()
            localRepo.loadPlaylistSongs(mediaId)
        }
        LibrarySource.YouTubeMusic -> {
            val ytId = playlist.youtubePlaylistId ?: return emptyList()
            ytClient.playlistSongs(ytId)
        }
    }

    private fun syncMessage(
        songCount: Int,
        playlistCount: Int,
        songError: String?,
        playlistError: String?,
    ): String = when {
        songCount > 0 && playlistCount > 0 -> "Synced $songCount songs, $playlistCount playlists"
        songCount > 0 -> "Synced $songCount songs"
        playlistCount > 0 -> "Synced $playlistCount playlists"
        songError != null -> songError
        playlistError != null -> playlistError
        else -> "No YouTube Music library songs found. Try search in get music."
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null) {
                    positionMs = ctrl.currentPosition.coerceAtLeast(0L)
                    val d = ctrl.duration
                    if (d > 0) durationMs = d
                }
                delay(500)
            }
        }
    }

    companion object {
        private const val QUEUE_LOOKAHEAD = 6
        private const val QUEUE_LOOKBEHIND = 3
        const val HUB_COLLECTION = 0
        const val HUB_GET_MUSIC = 1
        const val HUB_NOW_PLAYING = 2
        const val COLLECTION_ARTISTS = 0
        const val COLLECTION_ALBUMS = 1
        const val COLLECTION_SONGS = 2
        const val COLLECTION_PLAYLISTS = 3
        const val COLLECTION_GENRES = 4

        fun hasAudioPermission(context: Context): Boolean {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

        fun audioPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
    }
}
