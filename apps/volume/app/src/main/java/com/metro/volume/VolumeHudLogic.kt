package com.metro.volume

import kotlin.math.roundToInt

/**
 * Pure helpers for WP8.1 volume scales ↔ Android stream levels and HUD timing.
 */
object VolumeHudLogic {
    fun androidToWp(androidLevel: Int, androidMax: Int, wpMax: Int): Int {
        if (wpMax <= 0 || androidMax <= 0) return 0
        return ((androidLevel.toFloat() / androidMax.toFloat()) * wpMax.toFloat())
            .roundToInt()
            .coerceIn(0, wpMax)
    }

    fun wpToAndroid(wpLevel: Int, androidMax: Int, wpMax: Int): Int {
        if (androidMax <= 0 || wpMax <= 0) return 0
        return ((wpLevel.coerceIn(0, wpMax).toFloat() / wpMax.toFloat()) * androidMax.toFloat())
            .roundToInt()
            .coerceIn(0, androidMax)
    }

    /**
     * Prefer the HUD's current WP level when it still maps to the live Android index.
     * Android stream maxima are often coarser than WP scales (e.g. ring 0–7 vs 0–10), so a
     * naive rematch collapses several WP ticks onto one hardware step and makes rockers stick
     * or jump (e.g. floor at 8/10, then a single up to 10/10).
     */
    fun androidToWpConsistent(
        androidLevel: Int,
        androidMax: Int,
        wpMax: Int,
        preferredWp: Int,
    ): Int {
        if (wpMax <= 0 || androidMax <= 0) return 0
        val level = androidLevel.coerceIn(0, androidMax)
        val preferred = preferredWp.coerceIn(0, wpMax)
        if (wpToAndroid(preferred, androidMax, wpMax) == level) {
            return preferred
        }
        return androidToWp(level, androidMax, wpMax)
    }

    /**
     * Simulate rocker steps while keeping the displayed WP level as source of truth whenever
     * the underlying Android index is unchanged (lossy scale).
     */
    fun stepWpAcrossAndroid(
        currentWp: Int,
        delta: Int,
        androidMax: Int,
        wpMax: Int,
    ): Int {
        val nextWp = stepLevel(currentWp, delta, wpMax)
        val androidLevel = wpToAndroid(nextWp, androidMax, wpMax)
        return androidToWpConsistent(androidLevel, androidMax, wpMax, nextWp)
    }

    fun selectDefaultStream(inCall: Boolean, musicActive: Boolean): VolumeStreamKind = when {
        inCall -> VolumeStreamKind.Call
        musicActive -> VolumeStreamKind.Media
        else -> VolumeStreamKind.Ringer
    }

    fun formatLevelDigits(level: Int): String = "%02d".format(level.coerceAtLeast(0))

    fun formatMaxSuffix(wpMax: Int): String = "/$wpMax"

    fun shouldDismiss(
        visible: Boolean,
        lastInteractionMs: Long,
        nowMs: Long,
        dismissMs: Long = VolumeHudSpec.DISMISS_MS,
    ): Boolean {
        if (!visible) return false
        return nowMs - lastInteractionMs >= dismissMs
    }

    fun stepLevel(current: Int, delta: Int, wpMax: Int): Int =
        (current + delta).coerceIn(0, wpMax)

    fun toggleMute(current: Int, restoreLevel: Int): Pair<Int, Int> {
        return if (current <= 0) {
            val restored = restoreLevel.coerceAtLeast(1)
            restored to restored
        } else {
            0 to current
        }
    }

    /**
     * Entering silent zeros every stream (after snapshotting restores).
     * Leaving restores prior levels; ringer is at least 1 so the device is audible again.
     */
    fun toggleSilentMode(
        currentlySilent: Boolean,
        ringerLevel: Int,
        mediaLevel: Int,
        callLevel: Int,
        ringerRestore: Int,
        mediaRestore: Int,
        callRestore: Int,
    ): SilentModeState {
        if (!currentlySilent) {
            return SilentModeState(
                silentModeOn = true,
                ringerLevel = 0,
                mediaLevel = 0,
                callLevel = 0,
                ringerRestore = if (ringerLevel > 0) ringerLevel else ringerRestore,
                mediaRestore = if (mediaLevel > 0) mediaLevel else mediaRestore,
                callRestore = if (callLevel > 0) callLevel else callRestore,
            )
        }
        return SilentModeState(
            silentModeOn = false,
            ringerLevel = ringerRestore.coerceAtLeast(1),
            mediaLevel = mediaRestore,
            callLevel = callRestore,
            ringerRestore = ringerRestore,
            mediaRestore = mediaRestore,
            callRestore = callRestore,
        )
    }
}

/** Snapshot of HUD levels after a silent-mode toggle. */
data class SilentModeState(
    val silentModeOn: Boolean,
    val ringerLevel: Int,
    val mediaLevel: Int,
    val callLevel: Int,
    val ringerRestore: Int,
    val mediaRestore: Int,
    val callRestore: Int,
)
