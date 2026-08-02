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
}
