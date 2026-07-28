package com.metro.launcher.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory now-playing snapshots keyed by package, fed by [com.metro.launcher.TileNotificationListenerService]
 * via [MediaSessionManager] (requires notification-listener access).
 */
object MusicNowPlayingStore {
    private val byPackage = ConcurrentHashMap<String, MusicNowPlayingInfo>()
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var sessionManager: MediaSessionManager? = null

    @Volatile
    private var listenerComponent: ComponentName? = null

    @Volatile
    private var appContext: Context? = null

    private val controllers = ConcurrentHashMap<String, MediaController>()

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { active ->
            rebuild(active)
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            refreshFromControllers()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refreshFromControllers()
        }

        override fun onSessionDestroyed() {
            refreshFromControllers()
        }
    }

    fun snapshot(packageName: String): MusicNowPlayingInfo? = byPackage[packageName]

    fun all(): Map<String, MusicNowPlayingInfo> = byPackage.toMap()

    fun addListener(listener: (packageName: String) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (packageName: String) -> Unit) {
        listeners -= listener
    }

    fun clear() {
        val packages = byPackage.keys.toList()
        detachAllControllers()
        byPackage.clear()
        packages.forEach { notifyListeners(it) }
    }

    /**
     * Bind to [MediaSessionManager] using the notification-listener [ComponentName].
     * Safe to call repeatedly when the listener connects.
     */
    fun bind(context: Context, listenerComponent: ComponentName) {
        val app = context.applicationContext
        appContext = app
        val manager = app.getSystemService(MediaSessionManager::class.java) ?: return
        val previous = this.listenerComponent
        if (sessionManager === manager && previous == listenerComponent) {
            rebuild(runCatching { manager.getActiveSessions(listenerComponent) }.getOrNull())
            return
        }
        unbind()
        sessionManager = manager
        this.listenerComponent = listenerComponent
        runCatching {
            manager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent,
                mainHandler,
            )
        }
        rebuild(runCatching { manager.getActiveSessions(listenerComponent) }.getOrNull())
    }

    fun unbind() {
        val manager = sessionManager
        val component = listenerComponent
        if (manager != null && component != null) {
            runCatching {
                manager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            }
        }
        sessionManager = null
        listenerComponent = null
        clear()
    }

    fun togglePlayPause(packageName: String) {
        val controller = controllers[packageName] ?: return
        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipToNext(packageName: String) {
        controllers[packageName]?.transportControls?.skipToNext()
    }

    fun skipToPrevious(packageName: String) {
        controllers[packageName]?.transportControls?.skipToPrevious()
    }

    internal fun rebuild(active: List<MediaController>?) {
        val context = appContext
        val eligible = active.orEmpty().filter { controller ->
            val pkg = controller.packageName ?: return@filter false
            context != null && MusicTilePackages.isMusicApp(context, pkg)
        }
        detachAllControllers()
        eligible.forEach { controller ->
            val pkg = controller.packageName ?: return@forEach
            controllers[pkg] = controller
            runCatching { controller.registerCallback(controllerCallback, mainHandler) }
        }
        publishFromControllers()
    }

    private fun refreshFromControllers() {
        mainHandler.post { publishFromControllers() }
    }

    private fun publishFromControllers() {
        val context = appContext ?: return
        val next = linkedMapOf<String, MusicNowPlayingInfo>()
        controllers.forEach { (packageName, controller) ->
            val info = controller.toNowPlaying(context) ?: return@forEach
            // Prefer the most recently updated session when duplicates exist.
            val existing = next[packageName]
            if (existing == null || info.updatedAtMs >= existing.updatedAtMs) {
                next[packageName] = info
            }
        }
        val changed = linkedSetOf<String>()
        changed += byPackage.keys
        changed += next.keys
        byPackage.clear()
        byPackage.putAll(next)
        changed.forEach { notifyListeners(it) }
    }

    private fun detachAllControllers() {
        controllers.values.forEach { controller ->
            runCatching { controller.unregisterCallback(controllerCallback) }
        }
        controllers.clear()
    }

    private fun notifyListeners(packageName: String) {
        listeners.forEach { it(packageName) }
    }

    private fun MediaController.toNowPlaying(context: Context): MusicNowPlayingInfo? {
        val metadata = metadata
        val state = playbackState
        val title = metadata.stringOrNull(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.stringOrNull(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata.stringOrNull(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.stringOrNull(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.stringOrNull(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val artUri = resolveAlbumArtUri(context, packageName, metadata)
        val playbackState = state?.state ?: PlaybackState.STATE_NONE
        val isActive = playbackState == PlaybackState.STATE_PLAYING ||
            playbackState == PlaybackState.STATE_PAUSED ||
            playbackState == PlaybackState.STATE_BUFFERING ||
            playbackState == PlaybackState.STATE_CONNECTING
        if (!isActive) return null
        // Paused with no identity would be an empty transport tile — skip.
        if (playbackState == PlaybackState.STATE_PAUSED &&
            title.isNullOrBlank() &&
            artist.isNullOrBlank() &&
            artUri == null
        ) {
            return null
        }
        val actions = state?.actions ?: 0L
        val canPlayPause = actions and (
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE
            ) != 0L || state != null
        val canNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
        val canPrev = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
        return MusicNowPlayingInfo(
            packageName = packageName,
            title = title,
            artist = artist,
            albumArtUri = artUri,
            isPlaying = playbackState == PlaybackState.STATE_PLAYING ||
                playbackState == PlaybackState.STATE_BUFFERING,
            canPlayPause = canPlayPause,
            canSkipNext = canNext,
            canSkipPrevious = canPrev,
            updatedAtMs = System.currentTimeMillis(),
        )
    }

    private fun MediaMetadata?.stringOrNull(key: String): String? =
        this?.getString(key)?.trim()?.takeIf { it.isNotEmpty() }

    private fun resolveAlbumArtUri(
        context: Context,
        packageName: String,
        metadata: MediaMetadata?,
    ): String? {
        if (metadata == null) return null
        val uriKeys = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        )
        for (key in uriKeys) {
            val raw = metadata.getString(key)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            if (raw.startsWith("http://") || raw.startsWith("https://") ||
                raw.startsWith("content://") || raw.startsWith("file://") ||
                raw.startsWith("android.resource://")
            ) {
                return raw
            }
            // Some players store a path without a scheme.
            if (raw.startsWith("/")) {
                return Uri.fromFile(File(raw)).toString()
            }
        }
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        return bitmap?.let { cacheAlbumArt(context, packageName, it) }
    }

    private fun cacheAlbumArt(context: Context, packageName: String, bitmap: Bitmap): String? {
        return runCatching {
            val dir = File(context.cacheDir, "music_tile_art").also { it.mkdirs() }
            val file = File(dir, "${packageName.hashCode()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            Uri.fromFile(file).toString()
        }.getOrNull()
    }
}
