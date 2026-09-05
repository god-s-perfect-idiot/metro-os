package com.metro.volume

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.media3.session.MediaController as Media3Controller
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.metro.system.MetroPreferences
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Tracks the primary music session for the volume HUD transport chrome.
 *
 * Two feeds (either is enough):
 * 1. [MediaSessionManager] via [VolumeMediaNotificationListenerService] (any music app)
 * 2. Direct Media3 [SessionToken] to suite Music (`com.metro.music`) — no extra grant
 */
object VolumeMediaSessionStore {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<(VolumeMediaTransport?) -> Unit>()
    private val active = AtomicReference<VolumeMediaTransport?>(null)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var sessionManager: MediaSessionManager? = null

    @Volatile
    private var listenerComponent: ComponentName? = null

    private val platformControllers = linkedMapOf<String, MediaController>()

    @Volatile
    private var media3Controller: Media3Controller? = null

    @Volatile
    private var media3Future: com.google.common.util.concurrent.ListenableFuture<Media3Controller>? = null

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
            rebuildPlatform(sessions)
        }

    private val platformCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    private val media3Listener = object : androidx.media3.common.Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = refresh()
        override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
        override fun onMediaMetadataChanged(
            mediaMetadata: androidx.media3.common.MediaMetadata,
        ) = refresh()
        override fun onMediaItemTransition(
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int,
        ) = refresh()
    }

    fun snapshot(): VolumeMediaTransport? = active.get()

    fun addListener(listener: (VolumeMediaTransport?) -> Unit) {
        listeners += listener
        listener(active.get())
    }

    fun removeListener(listener: (VolumeMediaTransport?) -> Unit) {
        listeners -= listener
    }

    /** Soft-bind for suite Music Media3 session (safe without notification access). */
    fun bindSuiteMusic(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (media3Controller != null || media3Future != null) {
            refresh()
            return
        }
        val token = SessionToken(
            app,
            ComponentName(SUITE_MUSIC_PACKAGE, SUITE_MUSIC_SERVICE),
        )
        val future = Media3Controller.Builder(app, token).buildAsync()
        media3Future = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    media3Controller = controller
                    controller.addListener(media3Listener)
                    refresh()
                }.onFailure {
                    media3Future = null
                    media3Controller = null
                    refresh()
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun unbindSuiteMusic() {
        val future = media3Future
        media3Controller?.let { controller ->
            runCatching { controller.removeListener(media3Listener) }
        }
        if (future != null) {
            runCatching { Media3Controller.releaseFuture(future) }
        }
        media3Controller = null
        media3Future = null
    }

    fun bindNotificationListener(context: Context, listenerComponent: ComponentName) {
        val app = context.applicationContext
        appContext = app
        val manager = app.getSystemService(MediaSessionManager::class.java) ?: return
        val previous = this.listenerComponent
        if (sessionManager === manager && previous == listenerComponent) {
            rebuildPlatform(runCatching { manager.getActiveSessions(listenerComponent) }.getOrNull())
            return
        }
        unbindNotificationListener()
        sessionManager = manager
        this.listenerComponent = listenerComponent
        runCatching {
            manager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent,
                mainHandler,
            )
        }
        rebuildPlatform(runCatching { manager.getActiveSessions(listenerComponent) }.getOrNull())
    }

    fun unbindNotificationListener() {
        val manager = sessionManager
        if (manager != null) {
            runCatching { manager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        }
        sessionManager = null
        listenerComponent = null
        detachPlatformControllers()
        refresh()
    }

    fun togglePlayPause() {
        val transport = active.get() ?: return
        platformControllers[transport.packageName]?.let { controller ->
            val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            if (playing) controller.transportControls.pause() else controller.transportControls.play()
            return
        }
        if (transport.packageName == SUITE_MUSIC_PACKAGE) {
            val controller = media3Controller ?: return
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }

    fun skipToNext() {
        val transport = active.get() ?: return
        platformControllers[transport.packageName]?.transportControls?.skipToNext()
            ?: run {
                if (transport.packageName == SUITE_MUSIC_PACKAGE) {
                    media3Controller?.seekToNextMediaItem()
                }
            }
    }

    fun skipToPrevious() {
        val transport = active.get() ?: return
        platformControllers[transport.packageName]?.transportControls?.skipToPrevious()
            ?: run {
                if (transport.packageName == SUITE_MUSIC_PACKAGE) {
                    media3Controller?.seekToPreviousMediaItem()
                }
            }
    }

    private fun rebuildPlatform(activeSessions: List<MediaController>?) {
        val context = appContext
        val musicPackages = context?.let { MetroPreferences(it).musicAppPackages }.orEmpty()
        detachPlatformControllers()
        activeSessions.orEmpty().forEach { controller ->
            val pkg = controller.packageName ?: return@forEach
            if (musicPackages.isNotEmpty() && pkg !in musicPackages) return@forEach
            platformControllers[pkg] = controller
            runCatching { controller.registerCallback(platformCallback, mainHandler) }
        }
        refresh()
    }

    private fun detachPlatformControllers() {
        platformControllers.values.forEach { controller ->
            runCatching { controller.unregisterCallback(platformCallback) }
        }
        platformControllers.clear()
    }

    private fun refresh() {
        mainHandler.post { publish() }
    }

    private fun publish() {
        val candidates = mutableListOf<VolumeMediaTransport>()
        platformControllers.forEach { (pkg, controller) ->
            controller.toTransport(pkg)?.let { candidates += it }
        }
        media3Controller?.toTransport()?.let { candidates += it }
        val next = pickPrimary(candidates)
        val previous = active.getAndSet(next)
        if (previous != next) {
            listeners.forEach { it(next) }
        }
    }

    private fun pickPrimary(candidates: List<VolumeMediaTransport>): VolumeMediaTransport? {
        if (candidates.isEmpty()) return null
        return candidates.maxWithOrNull(
            compareBy<VolumeMediaTransport> { if (it.isPlaying) 1 else 0 }
                .thenBy { if (it.packageName == SUITE_MUSIC_PACKAGE) 1 else 0 }
                .thenBy { if (it.hasIdentity) 1 else 0 },
        )
    }

    private fun MediaController.toTransport(packageName: String): VolumeMediaTransport? {
        val metadata = metadata
        val state = playbackState
        val title = metadata.stringOrNull(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.stringOrNull(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata.stringOrNull(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.stringOrNull(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.stringOrNull(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val playbackState = state?.state ?: PlaybackState.STATE_NONE
        val isActive = playbackState == PlaybackState.STATE_PLAYING ||
            playbackState == PlaybackState.STATE_PAUSED ||
            playbackState == PlaybackState.STATE_BUFFERING ||
            playbackState == PlaybackState.STATE_CONNECTING
        if (!isActive) return null
        if (playbackState == PlaybackState.STATE_PAUSED &&
            title.isNullOrBlank() &&
            artist.isNullOrBlank()
        ) {
            return null
        }
        val actions = state?.actions ?: 0L
        val canPlayPause = actions and (
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE
            ) != 0L || state != null
        return VolumeMediaTransport(
            packageName = packageName,
            title = title,
            artist = artist,
            isPlaying = playbackState == PlaybackState.STATE_PLAYING ||
                playbackState == PlaybackState.STATE_BUFFERING,
            canPlayPause = canPlayPause,
            canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
        )
    }

    private fun Media3Controller.toTransport(): VolumeMediaTransport? {
        if (!isConnected) return null
        val state = playbackState
        val playing = isPlaying ||
            state == androidx.media3.common.Player.STATE_BUFFERING
        val idleOrEnded = state == androidx.media3.common.Player.STATE_IDLE ||
            state == androidx.media3.common.Player.STATE_ENDED
        if (idleOrEnded && !playing) return null
        val metadata = mediaMetadata
        val title = metadata.title?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val artist = metadata.artist?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: metadata.albumArtist?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (!playing && title.isNullOrBlank() && artist.isNullOrBlank()) return null
        return VolumeMediaTransport(
            packageName = SUITE_MUSIC_PACKAGE,
            title = title,
            artist = artist,
            isPlaying = playing,
            canPlayPause = true,
            canSkipNext = hasNextMediaItem(),
            canSkipPrevious = hasPreviousMediaItem() || currentPosition > 3_000L,
        )
    }

    private fun MediaMetadata?.stringOrNull(key: String): String? =
        this?.getString(key)?.trim()?.takeIf { it.isNotEmpty() }

    private const val SUITE_MUSIC_PACKAGE = "com.metro.music"
    private const val SUITE_MUSIC_SERVICE = "com.metro.music.playback.MusicPlaybackService"
}
