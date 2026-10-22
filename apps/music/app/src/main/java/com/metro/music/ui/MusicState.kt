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
import com.metro.music.data.ArtistAboutLogic
import com.metro.music.data.ArtistDiscoverLogic
import com.metro.music.data.Genre
import com.metro.music.data.LibraryLogic
import com.metro.music.data.LibrarySource
import com.metro.music.data.LocalLibraryRepository
import com.metro.music.data.MusicDirectory
import com.metro.music.data.MusicDirectoryStore
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
import com.metro.music.ytmusic.potoken.YtZemerPoToken
import com.metro.ui.MetroJumpListLogic
import kotlinx.coroutines.CancellationException
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
import java.util.concurrent.ConcurrentHashMap
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
    MusicDirectories,
    Explore,
    Recent,
    TopSongs,
    Queue,
}

class MusicState(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val localRepo = LocalLibraryRepository(appContext)
    private val musicDirectoryStore = MusicDirectoryStore(appContext)
    private val playHistoryStore = PlayHistoryStore(appContext)
    private val authStore = YtMusicAuthStore(appContext)
    private val ytHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val ytClient = YtMusicClient(
        authStore = authStore,
        http = ytHttp,
        zemerPoToken = YtZemerPoToken(),
        legacyPoTokenSession = YtPoTokenSession(appContext, ytHttp),
    )
    private val artistAboutClient = ArtistAboutClient(http = ytHttp, ytClient = ytClient)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    /** Resolves and starts the current track — must not be cancelled by window fill. */
    private var playbackJob: Job? = null
    /** Prefetch local look-ahead / look-behind around the current track. */
    private var windowJob: Job? = null
    /** Background Innertube resolves for upcoming YouTube tracks (URL cache only). */
    private var prefetchJob: Job? = null
    /** Bumped on every [playSongs] so a stale resolve cannot replace a newer skip. */
    private var playbackGeneration: Int = 0
    /** Fresh googlevideo URLs keyed by video id — skips re-resolve when still valid. */
    private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()
    private var playlistJob: Job? = null
    private var albumJob: Job? = null
    private var artistJob: Job? = null
    /** Resolves the Wikipedia artist hero used as the hub backdrop. */
    private var backdropJob: Job? = null
    private var libraryJob: Job? = null
    /** Artist-name → hero image URL (Wikipedia), so album tracks do not re-hit the network. */
    private val artistBackdropCache = ConcurrentHashMap<String, String>()
    private var exploreJob: Job? = null
    private var topSongsJob: Job? = null
    private var billboardJob: Job? = null
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
    var musicDirectories by mutableStateOf<List<MusicDirectory>>(emptyList())
        private set
    var excludedMusicDirectoryIds by mutableStateOf<Set<String>>(emptySet())
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
    var topSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var topSongsLoading by mutableStateOf(false)
        private set
    var topSongsError by mutableStateOf<String?>(null)
        private set
    var billboardSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var billboardLoading by mutableStateOf(false)
        private set
    var billboardError by mutableStateOf<String?>(null)
        private set
    var showingFilter by mutableStateOf(ShowingFilter.All)
    var ytConnected by mutableStateOf(authStore.connected)
        private set
    var ytSyncing by mutableStateOf(false)
        private set
    var ytSyncMessage by mutableStateOf<String?>(null)
        private set
    var route by mutableStateOf(MusicRoute.Hub)
    /** Hub panes: 0 collection | 1 get music | 2 now playing | 3 local */
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
    /** Discover tracks for the open album (YT), excluding songs already in collection. */
    var albumDiscoverSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var albumDiscoverLoading by mutableStateOf(false)
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
    /**
     * Clear artist photo behind the hub while a track is loaded (WP8.1 Xbox Music).
     * Never the playing song/album cover — that stays in the now-playing square. Null = black.
     */
    var nowPlayingBackdropArt by mutableStateOf<Any?>(null)
        private set
    /** Album cover for brand-title tint; independent of the artist backdrop. */
    var nowPlayingAlbumArt by mutableStateOf<Any?>(null)
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
        excludedMusicDirectoryIds = musicDirectoryStore.loadExcludedIds()
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
        playbackJob?.cancel()
        windowJob?.cancel()
        prefetchJob?.cancel()
        playlistJob?.cancel()
        albumJob?.cancel()
        artistJob?.cancel()
        libraryJob?.cancel()
        exploreJob?.cancel()
        val ctrl = controller
        if (ctrl != null) {
            ctrl.removeListener(playerListener)
            // If playback is not active, drop the session so the next cold open shows
            // "Nothing playing" instead of restoring a paused queue.
            runCatching {
                if (!ctrl.isPlaying) {
                    ctrl.stop()
                    ctrl.clearMediaItems()
                }
            }
        }
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
                    val excluded = excludedMusicDirectoryIds
                    musicDirectories = withContext(Dispatchers.IO) { localRepo.loadMusicDirectories() }
                    localSongs = withContext(Dispatchers.IO) { localRepo.loadSongs(excluded) }
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
            musicDirectories = emptyList()
            libraryLoading = false
        }
        refreshYtLibrary()
    }

    fun applyMusicDirectorySelection(selectedIds: Set<String>) {
        val excluded = musicDirectories.map { it.id }.toSet() - selectedIds
        excludedMusicDirectoryIds = excluded
        musicDirectoryStore.saveExcludedIds(excluded)
        reloadLibrary()
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

    /** Loads the YouTube Music Top Songs chart (up to 100). Safe to call on every open. */
    fun loadTopSongs(force: Boolean = false) {
        if (topSongsLoading) return
        if (!force && topSongs.isNotEmpty()) return
        topSongsJob?.cancel()
        topSongsJob = scope.launch {
            topSongsLoading = true
            topSongsError = null
            try {
                val songs = withContext(Dispatchers.IO) { ytClient.topSongs() }
                if (!isActive) return@launch
                topSongs = songs
                topSongsError = if (songs.isEmpty()) {
                    "Could not load top songs. Check your connection and try again."
                } else {
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    topSongsError = e.message ?: "Could not load top songs"
                }
            } finally {
                if (isActive) topSongsLoading = false
            }
        }
    }

    /** Loads Billboard Hot 100 resolved against YouTube Music. Lazy — call when the pivot opens. */
    fun loadBillboard100(force: Boolean = false) {
        if (billboardLoading) return
        if (!force && billboardSongs.isNotEmpty()) return
        billboardJob?.cancel()
        billboardJob = scope.launch {
            billboardLoading = true
            billboardError = null
            try {
                val songs = withContext(Dispatchers.IO) { ytClient.billboardHot100() }
                if (!isActive) return@launch
                billboardSongs = songs
                billboardError = if (songs.isEmpty()) {
                    "Could not load Billboard Hot 100. Check your connection and try again."
                } else {
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    billboardError = e.message ?: "Could not load Billboard Hot 100"
                }
            } finally {
                if (isActive) billboardLoading = false
            }
        }
    }

    /**
     * Opens now playing immediately with the tapped song, resolves the stream in the background,
     * then starts playback. The seek bar stays as loading dots until duration is known.
     *
     * Always replaces the Media3 window with a freshly resolved URI for the start track.
     * YouTube googlevideo URLs expire within minutes — seeking a prefetched next item is what
     * caused "first song keeps playing / next infinite buffers" on skip.
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
        // Same list instance = jump within the active queue (skip). A new list = fresh play.
        val queue = if (songs === playbackQueue) {
            songs
        } else {
            orderedPlaybackQueue = songs
            if (shuffle) QueueLogic.shuffleKeepingCurrent(songs, start.id) else songs
        }
        playbackQueue = queue
        val queueIndex = QueueLogic.indexOfSong(queue, start.id).coerceIn(0, queue.lastIndex)
        // Arm UI before pausing so chrome does not flash empty.
        loadingPlayback = true
        updateCurrentSong(start)
        hubPage = HUB_NOW_PLAYING
        route = MusicRoute.Hub
        val generation = ++playbackGeneration
        playbackJob?.cancel()
        windowJob?.cancel()
        prefetchJob?.cancel()
        // Stop the previous track immediately; setMediaItems below replaces the window.
        runCatching { ctrl.pause() }
        playbackJob = scope.launch {
            try {
                val startItem = withContext(Dispatchers.IO) { resolvePlayable(start) }
                if (generation != playbackGeneration || !isActive) return@launch
                if (startItem == null) {
                    statusMessage = playbackError ?: "Unable to play"
                    return@launch
                }
                if (generation != playbackGeneration || !isActive) return@launch
                ctrl.shuffleModeEnabled = false
                ctrl.setMediaItems(listOf(startItem.second), 0, 0L)
                ctrl.prepare()
                ctrl.play()
                updateCurrentSong(startItem.first)
            } finally {
                if (generation == playbackGeneration) loadingPlayback = false
            }
            if (generation == playbackGeneration && isActive) {
                windowJob?.cancel()
                windowJob = scope.launch {
                    fillQueueWindow(ctrl, queue, queueIndex)
                }
                prefetchUpcomingStreams(queue, queueIndex)
            }
        }
    }

    fun openQueue() {
        if (playbackQueue.isEmpty()) {
            rebuildQueueFromPlayer()
        }
        route = MusicRoute.Queue
    }

    /** Jump to [index] in [playbackQueue] with a fresh stream resolve. */
    fun playQueueIndex(index: Int) {
        val songs = playbackQueue
        if (index !in songs.indices) return
        playSongs(songs, index)
    }

    /**
     * Only local files are prefetched into Media3. YouTube stream URLs expire quickly; prefetching
     * them made skip land on a dead URL (infinite buffer) while the first cached item kept playing.
     */
    private suspend fun fillQueueWindow(ctrl: MediaController, songs: List<Song>, startIndex: Int) {
        val following = songs.drop(startIndex + 1).take(QUEUE_LOOKAHEAD)
            .filter { it.source == LibrarySource.Local }
        for (song in following) {
            coroutineContext.ensureActive()
            val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
            ctrl.addMediaItem(item.second)
        }
        val preceding = songs.take(startIndex).takeLast(QUEUE_LOOKBEHIND)
            .filter { it.source == LibrarySource.Local }
        for ((offset, song) in preceding.withIndex()) {
            coroutineContext.ensureActive()
            val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
            ctrl.addMediaItem(offset, item.second)
        }
    }

    /**
     * Append / prepend unresolved **local** songs so Media3 holds a short window. YouTube tracks
     * are resolved on demand in [playSongs] / skip.
     */
    private fun ensureQueueWindow() {
        val ctrl = controller ?: return
        if (ctrl.mediaItemCount == 0) return
        val songs = playbackQueue
        if (songs.isEmpty()) return
        val logicalIndex = QueueLogic.indexOfSong(songs, currentSong?.id)
        if (logicalIndex < 0) return
        val materialised = materialisedMediaIds(ctrl)
        val ahead = QueueLogic.missingAhead(songs, logicalIndex, QUEUE_LOOKAHEAD, materialised)
            .filter { it.source == LibrarySource.Local }
        val behind = QueueLogic.missingBehind(songs, logicalIndex, QUEUE_LOOKBEHIND, materialised)
            .filter { it.source == LibrarySource.Local }
        if (ahead.isEmpty() && behind.isEmpty()) return
        windowJob?.cancel()
        windowJob = scope.launch {
            for (song in ahead) {
                if (!isActive) return@launch
                if (materialisedMediaIds(ctrl).contains(song.id)) continue
                val item = withContext(Dispatchers.IO) { resolvePlayable(song) } ?: continue
                ctrl.addMediaItem(item.second)
            }
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
     * Drop every Media3 item except the current track, then refill local look-ahead / look-behind.
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
        windowJob?.cancel()
        windowJob = scope.launch {
            fillQueueWindow(ctrl, songs, logicalIndex)
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
            return
        }
        // If UI shows a different song than Media3 (stale window), rematerialise instead of
        // resuming the old cached item.
        val uiId = currentSong?.id
        val playerId = ctrl.currentMediaItem?.mediaId
        if (uiId != null && playerId != null && uiId != playerId) {
            val index = QueueLogic.indexOfSong(playbackQueue, uiId)
            if (index >= 0) {
                playSongs(playbackQueue, index)
                return
            }
        }
        ctrl.play()
    }

    fun skipNext() {
        val next = QueueLogic.upNext(playbackQueue, currentSong?.id) ?: return
        val index = QueueLogic.indexOfSong(playbackQueue, next.id)
        if (index < 0) return
        // Local tracks already in the Media3 window can skip instantly.
        if (next.source == LibrarySource.Local && seekMaterialised(next.id)) return
        // Always fresh-materialise YouTube (or unmaterialised local). Prefetched YT Media3
        // items expire; seekToNext left song A playing while the UI showed B buffering forever.
        playSongs(playbackQueue, index)
    }

    fun skipPrevious() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > PREVIOUS_RESTART_MS && ctrl.isCurrentMediaItemSeekable) {
            ctrl.seekTo(0L)
            return
        }
        val queue = playbackQueue
        val index = QueueLogic.indexOfSong(queue, currentSong?.id)
        if (index <= 0) return
        val previous = queue[index - 1]
        if (previous.source == LibrarySource.Local && seekMaterialised(previous.id)) return
        playSongs(queue, index - 1)
    }

    /** Seek to a Media3 item already in the short window. Returns false if missing. */
    private fun seekMaterialised(songId: String): Boolean {
        val ctrl = controller ?: return false
        for (i in 0 until ctrl.mediaItemCount) {
            if (ctrl.getMediaItemAt(i).mediaId == songId) {
                updateCurrentSong(songById(songId) ?: return false)
                ctrl.seekTo(i, 0L)
                ctrl.play()
                return true
            }
        }
        return false
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
        albumDiscoverSongs = emptyList()
        route = MusicRoute.AlbumDetail
        loadAlbumExtras(album)
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

    private fun loadAlbumExtras(album: Album) {
        albumJob?.cancel()
        albumJob = scope.launch {
            albumDiscoverLoading = true
            albumRemoteLoading = true
            val collection = songsForAlbum(album)
            try {
                val remote = withContext(Dispatchers.IO) { resolveAlbumRemoteSongs(album) }
                albumRemoteSongs = remote
                albumDiscoverSongs = ArtistDiscoverLogic.songsNotInCollection(remote, collection)
            } finally {
                albumDiscoverLoading = false
                albumRemoteLoading = false
            }
        }
    }

    private fun resolveAlbumRemoteSongs(album: Album): List<Song> {
        val browseId = album.youtubeBrowseId
        if (!browseId.isNullOrBlank()) {
            return ytClient.albumSongs(browseId)
        }
        val query = listOf(album.title, album.artist)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (query.isBlank()) return emptyList()
        val results = ytClient.searchSongs(query, limit = 40)
        return results.filter { song ->
            matchesAlbumDiscover(song, album)
        }.ifEmpty { results }
    }

    private fun matchesAlbumDiscover(song: Song, album: Album): Boolean {
        val title = album.title.trim()
        val artist = album.artist.trim()
        val albumOk = title.isEmpty() ||
            song.album.contains(title, ignoreCase = true) ||
            title.contains(song.album, ignoreCase = true) ||
            song.album.isBlank()
        val artistOk = artist.isEmpty() ||
            song.artist.contains(artist, ignoreCase = true) ||
            artist.contains(song.artist, ignoreCase = true)
        return albumOk && artistOk
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
                    val about = withContext(Dispatchers.IO) {
                        artistAboutClient.fetch(artist.name)
                    }
                    artistAbout = about
                    ArtistAboutLogic.heroImageUrl(about)?.let { url ->
                        artistBackdropCache[artist.name.trim().lowercase()] = url
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
     * carrying art speaks for the whole record, so brand tint holds still as the album plays.
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
        nowPlayingAlbumArt = song?.let { albumArtworkModel(it) }
        resolveArtistBackdrop(song?.artist)
        if (recordHistory && song != null) {
            recordPlay(song)
        }
    }

    /**
     * Hub wash uses a clear artist portrait (Wikipedia hero), not album art, so the now-playing
     * square does not repeat the same image. Holds the previous artist wash until a new URL
     * arrives when the artist changes; clears when playback stops or no hero exists.
     */
    private fun resolveArtistBackdrop(artistName: String?) {
        backdropJob?.cancel()
        val name = artistName?.trim().orEmpty()
        if (name.isEmpty() || name.equals("Unknown artist", ignoreCase = true)) {
            nowPlayingBackdropArt = null
            return
        }
        val key = name.lowercase()
        artistBackdropCache[key]?.let { cached ->
            nowPlayingBackdropArt = cached
            return
        }
        // Keep the prior artist wash on screen while Wikipedia resolves the next one.
        backdropJob = scope.launch {
            val url = withContext(Dispatchers.IO) {
                artistAboutClient.fetchHeroImageUrl(name)
            }
            if (!isActive) return@launch
            if (url != null) {
                artistBackdropCache[key] = url
            }
            val stillCurrent = currentSong?.artist?.trim()?.equals(name, ignoreCase = true) == true
            if (stillCurrent) {
                nowPlayingBackdropArt = url
            }
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
                val cached = streamUrlCache[videoId]
                val now = System.currentTimeMillis()
                if (cached != null && cached.expiresAtMs > now) {
                    return song to MusicPlaybackService.mediaItemFor(song, cached.url)
                }
                val result = ytClient.resolveStream(videoId)
                val url = result.url
                if (url == null) {
                    playbackError = result.error
                    streamUrlCache.remove(videoId)
                    return null
                }
                streamUrlCache[videoId] = CachedStreamUrl(
                    url = url,
                    expiresAtMs = now + STREAM_CACHE_TTL_MS,
                )
                song to MusicPlaybackService.mediaItemFor(song, url)
            }
        }
    }

    /**
     * Warm the URL cache for the next few YouTube tracks so skip does not wait on Innertube.
     * Does **not** push items into Media3 — those URLs expire and broke seekToNext.
     */
    private fun prefetchUpcomingStreams(songs: List<Song>, startIndex: Int) {
        prefetchJob?.cancel()
        val upcoming = songs.drop(startIndex + 1).take(YT_PREFETCH_COUNT)
            .filter { it.source == LibrarySource.YouTubeMusic && !it.youtubeVideoId.isNullOrBlank() }
        if (upcoming.isEmpty()) return
        prefetchJob = scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            for (song in upcoming) {
                ensureActive()
                val videoId = song.youtubeVideoId ?: continue
                val cached = streamUrlCache[videoId]
                if (cached != null && cached.expiresAtMs > now) continue
                val result = runCatching { ytClient.resolveStream(videoId) }.getOrNull() ?: continue
                val url = result.url ?: continue
                streamUrlCache[videoId] = CachedStreamUrl(
                    url = url,
                    expiresAtMs = System.currentTimeMillis() + STREAM_CACHE_TTL_MS,
                )
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
            // No YouTube look-ahead in Media3 — advance the logical queue ourselves.
            if (playbackState == Player.STATE_ENDED) {
                when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> {
                        val id = currentSong?.id
                        val index = QueueLogic.indexOfSong(playbackQueue, id)
                        if (index >= 0) playSongs(playbackQueue, index)
                    }
                    else -> {
                        val next = QueueLogic.upNext(playbackQueue, currentSong?.id)
                        if (next != null) {
                            skipNext()
                        } else if (repeatMode == Player.REPEAT_MODE_ALL && playbackQueue.isNotEmpty()) {
                            playSongs(playbackQueue, 0)
                        }
                    }
                }
            }
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
            currentSong?.youtubeVideoId?.let { streamUrlCache.remove(it) }
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
            ?: topSongs.firstOrNull { it.id == id }
            ?: billboardSongs.firstOrNull { it.id == id }
            ?: playlistSongs.firstOrNull { it.id == id }
            ?: playbackQueue.firstOrNull { it.id == id }
            ?: recentSongs.firstOrNull { it.id == id }
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
        /** How many upcoming YT tracks to resolve into [streamUrlCache] while one plays. */
        private const val YT_PREFETCH_COUNT = 2
        /** googlevideo signed URLs typically live ~6h; keep a short TTL to stay safe. */
        private const val STREAM_CACHE_TTL_MS = 4L * 60L * 1000L
        /** Restart current track instead of skipping previous when earlier than this. */
        private const val PREVIOUS_RESTART_MS = 3_000L
        const val HUB_COLLECTION = 0
        const val HUB_GET_MUSIC = 1
        const val HUB_NOW_PLAYING = 2
        const val HUB_LOCAL = 3
        const val HUB_PAGE_COUNT = 4
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

private data class CachedStreamUrl(
    val url: String,
    val expiresAtMs: Long,
)
