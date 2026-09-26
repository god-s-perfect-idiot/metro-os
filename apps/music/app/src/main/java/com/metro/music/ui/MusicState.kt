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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.metro.music.data.Album
import com.metro.music.data.Artist
import com.metro.music.data.ArtistAbout
import com.metro.music.data.ArtistDiscoverLogic
import com.metro.music.data.Genre
import com.metro.music.data.LibraryLogic
import com.metro.music.data.LibrarySource
import com.metro.music.data.LocalLibraryRepository
import com.metro.music.data.PlayHistoryEntry
import com.metro.music.data.PlayHistoryLogic
import com.metro.music.data.PlayHistoryStore
import com.metro.music.data.Playlist
import com.metro.music.data.QueueLogic
import com.metro.music.data.ShowingFilter
import com.metro.music.data.Song
import com.metro.music.data.artworkModel
import com.metro.music.playback.MusicPlaybackService
import com.metro.music.playback.PlaybackLogic
import com.metro.music.ytmusic.ArtistAboutClient
import com.metro.music.ytmusic.YtMusicAuthStore
import com.metro.music.ytmusic.YtMusicClient
import com.metro.music.ytmusic.potoken.YtPoTokenSession
import com.metro.ui.MetroJumpListLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

enum class MusicRoute {
    Hub,
    Collection,
    AlbumDetail,
    ArtistDetail,
    PlaylistDetail,
    GenreDetail,
    Settings,
    Explore,
    Recent,
    Queue,
}

class MusicState(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val localRepo = LocalLibraryRepository(appContext)
    private val playHistoryStore = PlayHistoryStore(appContext)
    private val authStore = YtMusicAuthStore(appContext)
    private val ytHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val poTokenSession = YtPoTokenSession(appContext, ytHttp)
    private val ytClient = YtMusicClient(
        authStore = authStore,
        http = ytHttp,
        poTokenSession = poTokenSession,
    )
    private val artistAboutClient = ArtistAboutClient(ytHttp)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var queueJob: Job? = null
    private var playlistJob: Job? = null
    private var albumJob: Job? = null
    private var artistJob: Job? = null
    private var libraryJob: Job? = null
    private var exploreJob: Job? = null
    private var playbackError: String? = null
    private var playHistoryEntries by mutableStateOf<List<PlayHistoryEntry>>(emptyList())
    private var lastRecordedSongId: String? = null

    var hasAudioPermission by mutableStateOf(false)
        private set
    var libraryLoading by mutableStateOf(true)
        private set
    /**
     * First MediaStore pass finished (or skipped). Cold-start splash waits on this so the hub
     * does not mount mid-scan and stutter through brand/panorama enter.
     * Later "sync now" refreshes keep this true.
     */
    var hasCompletedInitialLoad by mutableStateOf(false)
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
    var selectedGenre by mutableStateOf<Genre?>(null)
    var playlistSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var playlistLoading by mutableStateOf(false)
        private set
    /** Remote tracks when opening a YouTube Music discover album with no local copies. */
    var albumRemoteSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var albumRemoteLoading by mutableStateOf(false)
        private set
    var artistDiscoverSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var artistDiscoverAlbums by mutableStateOf<List<Album>>(emptyList())
        private set
    var artistDiscoverLoading by mutableStateOf(false)
        private set
    var artistAbout by mutableStateOf<ArtistAbout?>(null)
        private set
    var artistAboutLoading by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var currentSong by mutableStateOf<Song?>(null)
        private set
    /**
     * Full logical play queue from the last [playSongs] call. Broader than the short window
     * materialised into Media3 (YouTube stream URLs expire).
     */
    var playbackQueue by mutableStateOf<List<Song>>(emptyList())
        private set
    /**
     * Unshuffled order from the last [playSongs] call. Restored when shuffle turns off so the
     * queue list matches the album/playlist again.
     */
    private var orderedPlaybackQueue: List<Song> = emptyList()
    /** Album cover Coil model behind the hub while a track is loaded; null = plain background. */
    var nowPlayingBackdropArt by mutableStateOf<Any?>(null)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var loadingPlayback by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)

    val upNextLabel: String
        get() = QueueLogic.upNextLabel(playbackQueue, currentSong?.id)

    val allSongs: List<Song>
        get() = localSongs + ytSongs

    /** Newest-first plays persisted on this device (library metadata when still available). */
    val recentSongs: List<Song>
        get() = PlayHistoryLogic.resolveSongs(playHistoryEntries, allSongs)

    val visibleSongs: List<Song>
        get() = LibraryLogic.filterSongs(allSongs, showingFilter)

    val artists: List<Artist>
        get() = LibraryLogic.artistsFrom(visibleSongs)

    val albums: List<Album>
        get() = LibraryLogic.albumsFrom(visibleSongs)

    val playlists: List<Playlist>
        get() = LibraryLogic.filterPlaylists(localPlaylists + ytPlaylists, showingFilter)

    val genres: List<Genre>
        get() = LibraryLogic.genresFrom(visibleSongs)

    /** Letters the jump grid can offer for the pivot page on screen. */
    val collectionJumpLetters: Set<Char>
        get() = when (collectionPage) {
            COLLECTION_ARTISTS -> MetroJumpListLogic.activeLetters(artists.map { it.name })
            COLLECTION_ALBUMS -> MetroJumpListLogic.activeLetters(albums.map { it.title })
            COLLECTION_SONGS -> MetroJumpListLogic.activeLetters(visibleSongs.map { it.title })
            COLLECTION_PLAYLISTS -> MetroJumpListLogic.activeLetters(playlists.map { it.title })
            COLLECTION_GENRES -> MetroJumpListLogic.activeLetters(genres.map { it.name })
            else -> emptySet()
        }

    fun refreshPermissions(context: Context) {
        hasAudioPermission = hasAudioPermission(context)
    }

    init {
        loadPlayHistory()
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
        albumJob?.cancel()
        artistJob?.cancel()
        libraryJob?.cancel()
        exploreJob?.cancel()
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
                    if (isActive) {
                        libraryLoading = false
                        hasCompletedInitialLoad = true
                    }
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
     * Opens now playing immediately with the tapped song, resolves the stream in the background,
     * then starts playback. The seek bar stays as loading dots until duration is known.
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
        statusMessage = null
        playbackError = null
        positionMs = 0L
        durationMs = 0L
        isPlaying = false
        orderedPlaybackQueue = songs
        playbackQueue = if (shuffle) {
            QueueLogic.shuffleKeepingCurrent(songs, start.id)
        } else {
            songs
        }
        val queue = playbackQueue
        val queueIndex = QueueLogic.indexOfSong(queue, start.id).coerceIn(0, queue.lastIndex)
        updateCurrentSong(start)
        hubPage = HUB_NOW_PLAYING
        route = MusicRoute.Hub
        loadingPlayback = true
        queueJob?.cancel()
        queueJob = scope.launch {
            try {
                val startItem = withContext(Dispatchers.IO) { resolvePlayable(start) }
                if (startItem == null) {
                    statusMessage = playbackError ?: "Unable to play"
                    return@launch
                }
                ctrl.shuffleModeEnabled = false
                ctrl.setMediaItems(listOf(startItem.second), 0, 0L)
                ctrl.prepare()
                ctrl.play()
                updateCurrentSong(startItem.first)
            } finally {
                loadingPlayback = false
            }
            fillQueue(ctrl, queue, queueIndex)
        }
    }

    fun openQueue() {
        if (playbackQueue.isEmpty()) {
            rebuildQueueFromPlayer()
        }
        route = MusicRoute.Queue
    }

    /**
     * Jump to [index] in [playbackQueue]. Prefers seeking an already-materialised Media3 item;
     * otherwise reloads the queue from that song.
     */
    fun playQueueIndex(index: Int) {
        val songs = playbackQueue
        if (index !in songs.indices) return
        val target = songs[index]
        val ctrl = controller
        if (ctrl != null) {
            for (i in 0 until ctrl.mediaItemCount) {
                if (ctrl.getMediaItemAt(i).mediaId == target.id) {
                    ctrl.seekToDefaultPosition(i)
                    ctrl.play()
                    hubPage = HUB_NOW_PLAYING
                    route = MusicRoute.Hub
                    return
                }
            }
        }
        playSongs(songs, index)
    }

    /**
     * Every YouTube track costs an Innertube round trip and its stream URL expires, so only a
     * short window either side of the tapped song is materialised. [ensureQueueWindow] tops it
     * up as playback advances through the full logical [playbackQueue].
     */
    private suspend fun fillQueue(ctrl: MediaController, songs: List<Song>, startIndex: Int) {
        val following = songs.drop(startIndex + 1).take(QUEUE_LOOKAHEAD)
        for (song in following) {
            coroutineContext.ensureActive()
            val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
            ctrl.addMediaItem(item.second)
        }
        val preceding = songs.take(startIndex).takeLast(QUEUE_LOOKBEHIND)
        for ((offset, song) in preceding.withIndex()) {
            coroutineContext.ensureActive()
            val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
            ctrl.addMediaItem(offset, item.second)
        }
    }

    /**
     * Append / prepend unresolved songs so Media3 always holds ~[QUEUE_LOOKAHEAD] ahead of the
     * current logical index. Skip and natural advance both rely on this sliding window.
     */
    private fun ensureQueueWindow() {
        val ctrl = controller ?: return
        val songs = playbackQueue
        if (songs.isEmpty()) return
        val logicalIndex = QueueLogic.indexOfSong(songs, currentSong?.id)
        if (logicalIndex < 0) return
        val materialised = materialisedMediaIds(ctrl)
        val ahead = QueueLogic.missingAhead(songs, logicalIndex, QUEUE_LOOKAHEAD, materialised)
        val behind = QueueLogic.missingBehind(songs, logicalIndex, QUEUE_LOOKBEHIND, materialised)
        if (ahead.isEmpty() && behind.isEmpty()) return
        queueJob?.cancel()
        queueJob = scope.launch {
            for (song in ahead) {
                if (!isActive) return@launch
                if (materialisedMediaIds(ctrl).contains(song.id)) continue
                val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
                ctrl.addMediaItem(item.second)
            }
            // Prepend in reverse so the earliest missing song ends up first.
            for (song in behind.asReversed()) {
                if (!isActive) return@launch
                if (materialisedMediaIds(ctrl).contains(song.id)) continue
                val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
                ctrl.addMediaItem(0, item.second)
            }
        }
    }

    private fun materialisedMediaIds(ctrl: MediaController): Set<String> = buildSet {
        for (i in 0 until ctrl.mediaItemCount) {
            add(ctrl.getMediaItemAt(i).mediaId)
        }
    }

    /**
     * Drop every Media3 item except the current track, then refill the look-ahead / look-behind
     * window from [playbackQueue]. Used after shuffle reorder so the player order matches.
     */
    private fun rematerializeWindowAroundCurrent() {
        val ctrl = controller ?: return
        val songs = playbackQueue
        val logicalIndex = QueueLogic.indexOfSong(songs, currentSong?.id)
        if (logicalIndex < 0 || ctrl.mediaItemCount <= 0) return
        val currentIndex = ctrl.currentMediaItemIndex.coerceAtLeast(0)
        while (ctrl.mediaItemCount > currentIndex + 1) {
            ctrl.removeMediaItem(ctrl.mediaItemCount - 1)
        }
        while (ctrl.currentMediaItemIndex > 0) {
            ctrl.removeMediaItem(0)
        }
        ctrl.shuffleModeEnabled = false
        queueJob?.cancel()
        queueJob = scope.launch {
            fillQueue(ctrl, songs, logicalIndex)
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun skipNext() {
        val ctrl = controller ?: return
        if (ctrl.hasNextMediaItem()) {
            ctrl.seekToNextMediaItem()
            return
        }
        // Media3 window exhausted but logical queue may still have tracks — jump / refill.
        val next = QueueLogic.upNext(playbackQueue, currentSong?.id) ?: return
        val index = QueueLogic.indexOfSong(playbackQueue, next.id)
        if (index >= 0) playQueueIndex(index)
    }

    fun skipPrevious() {
        val ctrl = controller ?: return
        // Near the start of a track, WP / most players restart; otherwise go previous.
        if (ctrl.currentPosition > PREVIOUS_RESTART_MS && ctrl.isCurrentMediaItemSeekable) {
            ctrl.seekTo(0L)
            return
        }
        if (ctrl.hasPreviousMediaItem()) {
            ctrl.seekToPreviousMediaItem()
            return
        }
        val queue = playbackQueue
        val index = QueueLogic.indexOfSong(queue, currentSong?.id)
        if (index > 0) playQueueIndex(index - 1)
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    fun toggleShuffle() {
        val enabling = !shuffle
        shuffle = enabling
        // Own shuffle on the logical queue — Media3 shuffle only sees the short window.
        controller?.shuffleModeEnabled = false
        val currentId = currentSong?.id
        if (enabling) {
            if (orderedPlaybackQueue.isEmpty()) {
                orderedPlaybackQueue = playbackQueue
            }
            val source = orderedPlaybackQueue.ifEmpty { playbackQueue }
            if (source.isEmpty()) return
            playbackQueue = QueueLogic.shuffleKeepingCurrent(source, currentId)
        } else {
            val restored = orderedPlaybackQueue
            if (restored.isNotEmpty()) {
                playbackQueue = restored
            }
        }
        rematerializeWindowAroundCurrent()
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
        albumRemoteSongs = emptyList()
        route = MusicRoute.AlbumDetail
        val browseId = album.youtubeBrowseId
        if (browseId != null && songsForAlbum(album).isEmpty()) {
            albumJob?.cancel()
            albumJob = scope.launch {
                albumRemoteLoading = true
                try {
                    albumRemoteSongs = withContext(Dispatchers.IO) {
                        ytClient.albumSongs(browseId)
                    }
                } finally {
                    albumRemoteLoading = false
                }
            }
        } else {
            albumJob?.cancel()
            albumRemoteLoading = false
        }
    }

    fun openArtist(artist: Artist) {
        selectedArtist = artist
        artistDiscoverSongs = emptyList()
        artistDiscoverAlbums = emptyList()
        artistAbout = null
        route = MusicRoute.ArtistDetail
        loadArtistExtras(artist)
    }

    fun albumsForArtist(artist: Artist): List<Album> =
        albums.filter { it.artist.equals(artist.name, ignoreCase = true) }

    fun songsForAlbumDetail(album: Album): List<Song> {
        val local = songsForAlbum(album)
        return local.ifEmpty { albumRemoteSongs }
    }

    private fun loadArtistExtras(artist: Artist) {
        artistJob?.cancel()
        artistJob = scope.launch {
            artistDiscoverLoading = true
            artistAboutLoading = true
            val collectionSongs = songsForArtist(artist)
            val collectionAlbums = albumsForArtist(artist)
            try {
                val discoverDeferred = launch {
                    val (songs, albums) = withContext(Dispatchers.IO) {
                        ytClient.searchSongs(artist.name, limit = 30) to
                            ytClient.searchAlbums(artist.name, limit = 25)
                    }
                    val name = artist.name
                    fun matchesArtist(candidate: String): Boolean {
                        if (candidate.isBlank() || candidate.equals("Unknown artist", true)) {
                            return false
                        }
                        return candidate.contains(name, ignoreCase = true) ||
                            name.contains(candidate, ignoreCase = true)
                    }
                    artistDiscoverSongs = ArtistDiscoverLogic.songsNotInCollection(
                        songs.filter { matchesArtist(it.artist) },
                        collectionSongs,
                    )
                    // Prefer artist-matching albums; if the subtitle parse missed the name,
                    // still show the raw album search hits so discover is never empty by accident.
                    val matchedAlbums = albums.filter { matchesArtist(it.artist) }
                    artistDiscoverAlbums = ArtistDiscoverLogic.albumsNotInCollection(
                        matchedAlbums.ifEmpty { albums },
                        collectionAlbums,
                    )
                }
                val aboutDeferred = launch {
                    artistAbout = withContext(Dispatchers.IO) {
                        artistAboutClient.fetch(artist.name)
                    }
                }
                discoverDeferred.join()
                aboutDeferred.join()
            } finally {
                artistDiscoverLoading = false
                artistAboutLoading = false
            }
        }
    }

    fun openGenre(genre: Genre) {
        selectedGenre = genre
        route = MusicRoute.GenreDetail
    }

    fun songsForAlbum(album: Album): List<Song> =
        visibleSongs.filter {
            it.album.equals(album.title, ignoreCase = true) &&
                it.artist.equals(album.artist, ignoreCase = true)
        }

    fun songsForArtist(artist: Artist): List<Song> =
        visibleSongs.filter { it.artist.equals(artist.name, ignoreCase = true) }

    fun songsForGenre(genre: Genre): List<Song> =
        visibleSongs.filter { it.genre.equals(genre.name, ignoreCase = true) }

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

    private fun updateCurrentSong(song: Song?, recordHistory: Boolean = true) {
        currentSong = song
        nowPlayingBackdropArt = song?.let { albumArtworkModel(it) }
        if (recordHistory && song != null) {
            recordPlay(song)
        }
    }

    private fun recordPlay(song: Song) {
        if (song.id == lastRecordedSongId) return
        lastRecordedSongId = song.id
        val updated = PlayHistoryLogic.record(song, System.currentTimeMillis(), playHistoryEntries)
        playHistoryEntries = updated
        scope.launch(Dispatchers.IO) { playHistoryStore.save(updated) }
    }

    private fun loadPlayHistory() {
        scope.launch {
            playHistoryEntries = withContext(Dispatchers.IO) { playHistoryStore.load() }
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
            // Slide the materialised window forward so skip/next never dead-ends mid-queue.
            ensureQueueWindow()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            syncFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            if (playbackQueue.isEmpty()) {
                rebuildQueueFromPlayer()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            isPlaying = false
            loadingPlayback = false
            statusMessage = "Playback failed: ${error.errorCodeName}"
            // Stop cleanly — an abrupt Source error previously aborted MediaCodec (SIGABRT).
            runCatching {
                controller?.pause()
                controller?.stop()
            }
        }
    }

    private fun syncFromPlayer() {
        val ctrl = controller ?: return
        isPlaying = ctrl.isPlaying
        durationMs = ctrl.duration.coerceAtLeast(0L)
        positionMs = ctrl.currentPosition.coerceAtLeast(0L)
        // Shuffle is owned by this state (full logical queue), not Media3's short window.
        repeatMode = ctrl.repeatMode
        updateCurrentSong(resolveSong(ctrl.currentMediaItem))
        if (playbackQueue.isEmpty()) {
            rebuildQueueFromPlayer()
        }
        ensureQueueWindow()
    }

    /** After process death, rebuild a best-effort queue from materialised Media3 items. */
    private fun rebuildQueueFromPlayer() {
        val ctrl = controller ?: return
        val count = ctrl.mediaItemCount
        if (count <= 0) return
        val items = buildList {
            for (i in 0 until count) {
                resolveSong(ctrl.getMediaItemAt(i))?.let { add(it) }
            }
        }
        if (items.isNotEmpty()) {
            playbackQueue = items
            if (orderedPlaybackQueue.isEmpty()) {
                orderedPlaybackQueue = items
            }
        }
    }

    private fun resolveSong(mediaItem: MediaItem?): Song? =
        PlaybackLogic.resolveCurrentSong(mediaItem, durationMs) { songById(it) }

    private fun songById(id: String): Song? {
        return allSongs.firstOrNull { it.id == id }
            ?: exploreResults.firstOrNull { it.id == id }
            ?: playlistSongs.firstOrNull { it.id == id }
            ?: playbackQueue.firstOrNull { it.id == id }
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
        /** Restart current track instead of skipping previous when earlier than this. */
        private const val PREVIOUS_RESTART_MS = 3_000L
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
