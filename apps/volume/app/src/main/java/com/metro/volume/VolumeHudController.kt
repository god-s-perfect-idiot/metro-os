package com.metro.volume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferences

/**
 * Owns WP8.1 volume HUD state and writes to [AudioManager] streams.
 * Volume keys arrive from [VolumeAccessibilityService] via [onVolumeKey].
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
    var vibrateOn by mutableStateOf(true)
        private set
    var inCall by mutableStateOf(false)
        private set
    var accentColor by mutableStateOf(preferences.accentColor)
        private set

    private var lastInteractionMs by mutableLongStateOf(0L)
    private var ringerRestore = 5
    private var mediaRestore = 15
    private var callRestore = 5

    private val dismissRunnable = Runnable { dismiss() }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != "android.media.VOLUME_CHANGED_ACTION") return
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
            vibrateOn = vibrateOn,
            inCall = inCall,
            accentColor = accentColor,
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
        syncFromAudioManager()
        refreshTheme()
    }

    fun unregister() {
        handler.removeCallbacks(dismissRunnable)
        runCatching { appContext.unregisterReceiver(volumeReceiver) }
        runCatching { appContext.unregisterReceiver(themeReceiver) }
    }

    fun refreshTheme() {
        accentColor = preferences.accentColor
    }

    fun onVolumeKey(delta: Int): Boolean {
        return try {
            syncFromAudioManager()
            inCall = isInCallMode()
            activeStream = VolumeHudLogic.selectDefaultStream(
                inCall = inCall,
                musicActive = audioManager.isMusicActive,
            )

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
        if (level > 0) ringerRestore = level
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

    fun toggleVibrate() {
        vibrateOn = !vibrateOn
        try {
            if (vibrateOn) {
                if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                } else if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
            } else {
                audioManager.ringerMode = if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                    AudioManager.RINGER_MODE_NORMAL
                } else {
                    AudioManager.RINGER_MODE_SILENT
                }
            }
        } catch (_: SecurityException) {
        }
        touch()
    }

    fun dismiss() {
        handler.removeCallbacks(dismissRunnable)
        visible = false
        applyExpanded(false)
    }

    fun tickDismiss(nowMs: Long = System.currentTimeMillis()) {
        if (VolumeHudLogic.shouldDismiss(visible, lastInteractionMs, nowMs)) {
            dismiss()
        }
    }

    private fun show() {
        visible = true
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
        vibrateOn = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_VIBRATE,
            AudioManager.RINGER_MODE_NORMAL,
            -> true
            else -> false
        }
    }

    private fun readWp(kind: VolumeStreamKind): Int {
        val stream = androidStream(kind)
        val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
        val level = audioManager.getStreamVolume(stream)
        return VolumeHudLogic.androidToWp(level, max, kind.wpMax)
    }

    private fun setWpLevel(kind: VolumeStreamKind, wpLevel: Int) {
        val stream = androidStream(kind)
        val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
        val androidLevel = VolumeHudLogic.wpToAndroid(wpLevel, max, kind.wpMax)
        try {
            audioManager.setStreamVolume(stream, androidLevel, 0)
        } catch (_: SecurityException) {
        }
        when (kind) {
            VolumeStreamKind.Ringer -> ringerLevel = wpLevel.coerceIn(0, kind.wpMax)
            VolumeStreamKind.Media -> mediaLevel = wpLevel.coerceIn(0, kind.wpMax)
            VolumeStreamKind.Call -> callLevel = wpLevel.coerceIn(0, kind.wpMax)
        }
        if (kind == VolumeStreamKind.Ringer) {
            try {
                audioManager.ringerMode = when {
                    wpLevel <= 0 && vibrateOn -> AudioManager.RINGER_MODE_VIBRATE
                    wpLevel <= 0 -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
            } catch (_: SecurityException) {
            }
        }
    }

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
