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
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.metro.music.data.Album
import com.metro.music.data.Artist
import com.metro.music.data.LibraryLogic
import com.metro.music.data.LibrarySource
import com.metro.music.data.LocalLibraryRepository
import com.metro.music.data.ShowingFilter
import com.metro.music.data.Song
import com.metro.music.data.artworkModel
import com.metro.music.data.loadAlbumTintArgb
import com.metro.music.playback.MusicPlaybackService
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
    private var tintJob: Job? = null
    private var backdropArtwork: Any? = null
    private var playbackError: String? = null

    var hasAudioPermission by mutableStateOf(false)
        private set
    var localSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var ytSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var exploreResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var exploreQuery by mutableStateOf("")
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

    /** Letters the jump grid can offer for the pivot page on screen (empty = playlists/genres). */
    val collectionJumpLetters: Set<Char>
        get() = when (collectionPage) {
            COLLECTION_ARTISTS -> MetroJumpListLogic.activeLetters(artists.map { it.name })
            COLLECTION_ALBUMS -> MetroJumpListLogic.activeLetters(albums.map { it.title })
            COLLECTION_SONGS -> MetroJumpListLogic.activeLetters(visibleSongs.map { it.title })
            else -> emptySet()
        }

    fun refreshPermissions(context: Context) {
        hasAudioPermission = hasAudioPermission(context)
    }

    fun connectPlayer() {
        if (controller != null) return
        val token = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                controller = future.get()
                controller?.addListener(playerListener)
                syncFromPlayer()
                startPositionUpdates()
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun releasePlayer() {
        positionJob?.cancel()
        queueJob?.cancel()
        tintJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    fun reloadLibrary() {
        if (hasAudioPermission) {
            scope.launch {
                localSongs = withContext(Dispatchers.IO) { localRepo.loadSongs() }
                // Album grouping only exists once the scan lands, so re-resolve the backdrop art.
                refreshBackdrop(currentSong)
            }
        } else {
            localSongs = emptyList()
        }
        refreshYtLibrary()
    }

    fun refreshYtAuth() {
        ytConnected = authStore.connected
        if (ytConnected) {
            refreshYtLibrary()
        } else {
            ytSongs = emptyList()
            ytSyncMessage = null
        }
    }

    fun refreshYtLibrary() {
        if (!authStore.connected) {
            ytSongs = emptyList()
            ytSyncMessage = "Connect YouTube Music in settings"
            return
        }
        scope.launch {
            ytSyncing = true
            ytSyncMessage = "Syncing…"
            try {
                val result = withContext(Dispatchers.IO) { ytClient.librarySongs() }
                ytSongs = result.songs
                ytSyncMessage = when {
                    result.songs.isNotEmpty() -> "Synced ${result.songs.size} songs"
                    result.error != null -> result.error
                    else -> "No songs in YouTube Music library"
                }
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
    }

    fun searchExplore(query: String) {
        exploreQuery = query
        if (query.isBlank()) {
            exploreResults = emptyList()
            return
        }
        scope.launch {
            exploreResults = withContext(Dispatchers.IO) { ytClient.searchSongs(query) }
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

    private suspend fun resolvePlayable(song: Song): Pair<Song, androidx.media3.common.MediaItem>? {
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

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId
            updateCurrentSong(
                allSongs.firstOrNull { it.id == id }
                    ?: exploreResults.firstOrNull { it.id == id },
            )
            durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
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
        val id = ctrl.currentMediaItem?.mediaId
        updateCurrentSong(allSongs.firstOrNull { it.id == id })
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
