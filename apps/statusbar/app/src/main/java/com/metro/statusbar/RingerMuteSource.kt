package com.metro.statusbar

import android.content.Context
import android.media.AudioManager

/**
 * Whether the device ringer is muted for the system tray mute glyph.
 * True when [AudioManager.STREAM_RING] volume is 0.
 */
object RingerMuteSource {
    fun isMuted(context: Context): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        return runCatching { audio.getStreamVolume(AudioManager.STREAM_RING) <= 0 }
            .getOrDefault(false)
    }
}
