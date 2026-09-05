package com.metro.volume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences
import kotlin.math.abs

/**
 * Owns WP8.1 volume HUD state and writes to [AudioManager] streams.
 * Volume keys arrive from [VolumeAccessibilityService] via [onVolumeKey].
 *
 * WP display ticks are the source of truth while the rocker/HUD is driving volume.
 * Android stream maxima are often coarser (and some OEMs reject or clamp
 * [AudioManager.setStreamVolume]), so we keep preferred WP levels across lossy
 * round-trips and verify/retry hardware writes.
 */
class VolumeHudController(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val preferences = MetroPreferences(appContext)
    private val handler = Handler(Looper.getMainLooper())

    var visible by mutableStateOf(false)
        private set
    var expanded by mutableStateOf(false)
        private set
    var activeStream by mutableStateOf(VolumeStreamKind.Ringer)
        private set
    var ringerLevel by mutableStateOf(0)
        private set
    var mediaLevel by mutableStateOf(0)
        private set
    var callLevel by mutableStateOf(0)
        private set
    var silentModeOn by mutableStateOf(false)
        private set
    var inCall by mutableStateOf(false)
        private set
    var accentColor by mutableStateOf(preferences.accentColor)
        private set
    var mediaTransport by mutableStateOf<VolumeMediaTransport?>(null)
        private set

    private var lastInteractionMs by mutableLongStateOf(0L)
    private var ringerRestore = 5
    private var mediaRestore = 15
    private var callRestore = 5

    private val dismissRunnable = Runnable { dismiss() }

    /** True while we are writing AudioManager so VOLUME_CHANGED does not remap our WP ticks. */
    private var writingStream = false

    /** Last Android index we successfully targeted per stream — used to ignore echo syncs. */
    private val lastWrittenAndroid = IntArray(3) { -1 }

    private val mediaTransportListener: (VolumeMediaTransport?) -> Unit = { next ->
        mediaTransport = next
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != "android.media.VOLUME_CHANGED_ACTION") return
            if (writingStream) return
            syncFromAudioManager()
        }
    }

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != MetroBroadcasts.ACTION_THEME_CHANGED) return
            refreshTheme()
        }
    }

    val snapshot: VolumeHudSnapshot
        get() = VolumeHudSnapshot(
            visible = visible,
            expanded = expanded,
            activeStream = activeStream,
            ringerLevel = ringerLevel,
            mediaLevel = mediaLevel,
            callLevel = callLevel,
            silentModeOn = silentModeOn,
            inCall = inCall,
            accentColor = accentColor,
            mediaTransport = mediaTransport,
        )

    fun register() {
        val volumeFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        val themeFilter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(volumeReceiver, volumeFilter, Context.RECEIVER_NOT_EXPORTED)
            appContext.registerReceiver(themeReceiver, themeFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(volumeReceiver, volumeFilter)
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(themeReceiver, themeFilter)
        }
        VolumeMediaSessionStore.addListener(mediaTransportListener)
        VolumeMediaSessionStore.bindSuiteMusic(appContext)
        mediaTransport = VolumeMediaSessionStore.snapshot()
        syncFromAudioManager()
        refreshTheme()
    }

    fun unregister() {
        handler.removeCallbacks(dismissRunnable)
        VolumeMediaSessionStore.removeListener(mediaTransportListener)
        VolumeMediaSessionStore.unbindSuiteMusic()
        runCatching { appContext.unregisterReceiver(volumeReceiver) }
        runCatching { appContext.unregisterReceiver(themeReceiver) }
    }

    fun refreshTheme() {
        preferences.pullThemeFromProvider()
        accentColor = preferences.accentColor
    }

    fun onVolumeKey(delta: Int): Boolean {
        return try {
            inCall = isInCallMode()
            mediaTransport = VolumeMediaSessionStore.snapshot()
            activeStream = VolumeHudLogic.selectDefaultStream(
                inCall = inCall,
                musicActive = audioManager.isMusicActive || mediaTransport != null,
            )
            // Sync only when the HUD was hidden so we pick up external changes once.
            // Re-syncing on every rocker press remaps WP ticks through a coarse Android
            // scale and can pin the display at a low value (e.g. 2/30 ← android 1/15)
            // when setStreamVolume fails or is delayed on some OEMs.
            if (!visible) {
                syncFromAudioManager()
            }

            val next = when (activeStream) {
                VolumeStreamKind.Ringer -> {
                    val level = VolumeHudLogic.stepLevel(ringerLevel, delta, VolumeStreamKind.Ringer.wpMax)
                    setWpLevel(VolumeStreamKind.Ringer, level)
                    level
                }
                VolumeStreamKind.Media -> {
                    val level = VolumeHudLogic.stepLevel(mediaLevel, delta, VolumeStreamKind.Media.wpMax)
                    setWpLevel(VolumeStreamKind.Media, level)
                    level
                }
                VolumeStreamKind.Call -> {
                    val level = VolumeHudLogic.stepLevel(callLevel, delta, VolumeStreamKind.Call.wpMax)
                    setWpLevel(VolumeStreamKind.Call, level)
                    level
                }
            }
            if (next > 0) {
                when (activeStream) {
                    VolumeStreamKind.Ringer -> ringerRestore = next
                    VolumeStreamKind.Media -> mediaRestore = next
                    VolumeStreamKind.Call -> callRestore = next
                }
                if (activeStream == VolumeStreamKind.Ringer && silentModeOn) {
                    silentModeOn = false
                }
            }
            show()
            true
        } catch (_: Throwable) {
            // Never let AudioManager / state errors crash the accessibility process —
            // a crashed key-filter service can leave volume rockers dead until reboot.
            false
        }
    }

    fun toggleExpanded() {
        if (!visible) return
        applyExpanded(!expanded)
        touch()
    }

    fun collapse() {
        if (!expanded) return
        applyExpanded(false)
        touch()
    }

    fun updateRingerLevel(level: Int) {
        setWpLevel(VolumeStreamKind.Ringer, level)
        if (level > 0) {
            ringerRestore = level
            if (silentModeOn) silentModeOn = false
        }
        activeStream = VolumeStreamKind.Ringer
        touch()
    }

    fun updateMediaLevel(level: Int) {
        setWpLevel(VolumeStreamKind.Media, level)
        if (level > 0) mediaRestore = level
        activeStream = VolumeStreamKind.Media
        touch()
    }

    fun updateCallLevel(level: Int) {
        setWpLevel(VolumeStreamKind.Call, level)
        if (level > 0) callRestore = level
        activeStream = VolumeStreamKind.Call
        touch()
    }

    fun toggleRingerMute() {
        val (next, restore) = VolumeHudLogic.toggleMute(ringerLevel, ringerRestore)
        ringerRestore = restore
        setWpLevel(VolumeStreamKind.Ringer, next)
        if (next > 0 && silentModeOn) silentModeOn = false
        activeStream = VolumeStreamKind.Ringer
        touch()
    }

    fun toggleMediaMute() {
        val (next, restore) = VolumeHudLogic.toggleMute(mediaLevel, mediaRestore)
        mediaRestore = restore
        setWpLevel(VolumeStreamKind.Media, next)
        activeStream = VolumeStreamKind.Media
        touch()
    }

    fun toggleSilentMode() {
        val next = VolumeHudLogic.toggleSilentMode(
            currentlySilent = silentModeOn,
            ringerLevel = ringerLevel,
            mediaLevel = mediaLevel,
            callLevel = callLevel,
            ringerRestore = ringerRestore,
            mediaRestore = mediaRestore,
            callRestore = callRestore,
        )
        silentModeOn = next.silentModeOn
        ringerRestore = next.ringerRestore
        mediaRestore = next.mediaRestore
        callRestore = next.callRestore
        // Batch Android writes so VOLUME_CHANGED cannot clear [silentModeOn] mid-toggle.
        // Android often reports VIBRATE/NORMAL at volume 0, which must not fight the HUD flag.
        writingStream = true
        try {
            setWpLevel(VolumeStreamKind.Ringer, next.ringerLevel, guardWrites = false)
            setWpLevel(VolumeStreamKind.Media, next.mediaLevel, guardWrites = false)
            setWpLevel(VolumeStreamKind.Call, next.callLevel, guardWrites = false)
            try {
                audioManager.ringerMode = if (next.silentModeOn) {
                    AudioManager.RINGER_MODE_SILENT
                } else {
                    AudioManager.RINGER_MODE_NORMAL
                }
            } catch (_: SecurityException) {
            }
        } finally {
            handler.post { writingStream = false }
        }
        touch()
    }

    fun openSoundSettings() {
        touch()
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
        dismiss()
    }

    fun togglePlayPause() {
        VolumeMediaSessionStore.togglePlayPause()
        touch()
    }

    fun skipToNext() {
        VolumeMediaSessionStore.skipToNext()
        touch()
    }

    fun skipToPrevious() {
        VolumeMediaSessionStore.skipToPrevious()
        touch()
    }

    fun dismiss() {
        handler.removeCallbacks(dismissRunnable)
        visible = false
        // Leave [expanded] as-is so the exit wipe keeps the current chrome;
        // [show] resets to collapsed when the HUD next creeps in.
    }

    fun tickDismiss(nowMs: Long = System.currentTimeMillis()) {
        if (VolumeHudLogic.shouldDismiss(visible, lastInteractionMs, nowMs)) {
            dismiss()
        }
    }

    private fun show() {
        val wasHidden = !visible
        visible = true
        if (wasHidden) {
            applyExpanded(false)
        }
        touch()
    }

    private fun touch() {
        lastInteractionMs = System.currentTimeMillis()
        handler.removeCallbacks(dismissRunnable)
        handler.postDelayed(dismissRunnable, VolumeHudSpec.DISMISS_MS)
    }

    private fun applyExpanded(value: Boolean) {
        if (expanded == value) return
        expanded = value
    }

    private fun syncFromAudioManager() {
        inCall = isInCallMode()
        ringerLevel = readWp(VolumeStreamKind.Ringer)
        mediaLevel = readWp(VolumeStreamKind.Media)
        callLevel = readWp(VolumeStreamKind.Call)
        if (ringerLevel > 0) ringerRestore = ringerLevel
        if (mediaLevel > 0) mediaRestore = mediaLevel
        if (callLevel > 0) callRestore = callLevel
        // While the HUD is up, silent mode is owned by the toggle — Android may report
        // VIBRATE or NORMAL at volume 0, which must not clear the accent highlight.
        if (!visible) {
            silentModeOn = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
        }
    }

    private fun readWp(kind: VolumeStreamKind): Int {
        val stream = androidStream(kind)
        val max = streamMax(stream)
        val level = audioManager.getStreamVolume(stream).coerceIn(0, max)
        val preferred = when (kind) {
            VolumeStreamKind.Ringer -> ringerLevel
            VolumeStreamKind.Media -> mediaLevel
            VolumeStreamKind.Call -> callLevel
        }
        val written = lastWrittenAndroid[kind.ordinal]
        // If hardware still matches the last index we wrote, keep the WP tick even when
        // several WP steps share one Android step (lossy scale).
        if (written >= 0 && level == written) {
            return preferred.coerceIn(0, kind.wpMax)
        }
        return VolumeHudLogic.androidToWpConsistent(level, max, kind.wpMax, preferred)
    }

    private fun setWpLevel(
        kind: VolumeStreamKind,
        wpLevel: Int,
        guardWrites: Boolean = true,
    ) {
        val stream = androidStream(kind)
        val max = streamMax(stream)
        val clampedWp = wpLevel.coerceIn(0, kind.wpMax)
        val androidLevel = VolumeHudLogic.wpToAndroid(clampedWp, max, kind.wpMax)
        // WP ticks update immediately — hardware may be coarser or briefly lag.
        when (kind) {
            VolumeStreamKind.Ringer -> ringerLevel = clampedWp
            VolumeStreamKind.Media -> mediaLevel = clampedWp
            VolumeStreamKind.Call -> callLevel = clampedWp
        }
        if (guardWrites) writingStream = true
        try {
            applyAndroidVolume(stream, androidLevel)
            lastWrittenAndroid[kind.ordinal] = androidLevel
            if (kind == VolumeStreamKind.Ringer) {
                try {
                    audioManager.ringerMode = when {
                        silentModeOn || clampedWp <= 0 -> AudioManager.RINGER_MODE_SILENT
                        else -> AudioManager.RINGER_MODE_NORMAL
                    }
                } catch (_: SecurityException) {
                }
            }
        } finally {
            // Clear after the VOLUME_CHANGED broadcast has been delivered on this looper.
            if (guardWrites) {
                handler.post { writingStream = false }
            }
        }
    }

    /**
     * Write [androidLevel] with setStreamVolume, then verify and nudge via
     * adjustStreamVolume when OEMs clamp or ignore the absolute set.
     */
    private fun applyAndroidVolume(stream: Int, androidLevel: Int) {
        try {
            audioManager.setStreamVolume(stream, androidLevel, 0)
        } catch (_: SecurityException) {
        }
        val actual = runCatching { audioManager.getStreamVolume(stream) }.getOrDefault(androidLevel)
        if (actual == androidLevel) return
        val delta = androidLevel - actual
        val direction = if (delta > 0) {
            AudioManager.ADJUST_RAISE
        } else {
            AudioManager.ADJUST_LOWER
        }
        repeat(abs(delta).coerceAtMost(streamMax(stream))) {
            try {
                audioManager.adjustStreamVolume(stream, direction, 0)
            } catch (_: SecurityException) {
                return
            }
        }
    }

    private fun streamMax(stream: Int): Int =
        audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)

    private fun androidStream(kind: VolumeStreamKind): Int = when (kind) {
        VolumeStreamKind.Ringer -> AudioManager.STREAM_RING
        VolumeStreamKind.Media -> AudioManager.STREAM_MUSIC
        VolumeStreamKind.Call -> AudioManager.STREAM_VOICE_CALL
    }

    private fun isInCallMode(): Boolean {
        val mode = audioManager.mode
        return mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION
    }
}
