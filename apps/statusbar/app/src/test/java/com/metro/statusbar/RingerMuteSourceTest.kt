package com.metro.statusbar

import android.content.Context
import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RingerMuteSourceTest {
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        @Suppress("DEPRECATION")
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Test
    fun isMuted_whenRingerVolumeIsZero() {
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        assertTrue(RingerMuteSource.isMuted(context))
    }

    @Test
    fun isMuted_falseWhenRingerHasVolume() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, max, 0)
        assertFalse(RingerMuteSource.isMuted(context))
    }
}
