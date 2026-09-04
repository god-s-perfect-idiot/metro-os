package com.metro.dialer.telecom

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Short DTMF key-click tones for the pre-call dial pad.
 *
 * Uses [AudioManager.STREAM_DTMF] so volume follows the system dial-pad stream.
 * Silent ringer mode suppresses tones. In-call DTMF stays on [MetroCallSession.playDtmf].
 */
object DialPadTonePlayer {
    private const val TAG = "DialPadTone"
    private const val Volume = 80
    private const val DurationMs = 150

    private var toneGenerator: ToneGenerator? = null

    fun play(context: Context, digit: Char) {
        val toneType = toneTypeFor(digit) ?: return
        val am = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return
        if (am.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        if (am.getStreamVolume(AudioManager.STREAM_DTMF) <= 0) return

        try {
            val generator = toneGenerator ?: ToneGenerator(AudioManager.STREAM_DTMF, Volume).also {
                toneGenerator = it
            }
            generator.startTone(toneType, DurationMs)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to play dial-pad tone", error)
            release()
        }
    }

    fun release() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }

    private fun toneTypeFor(digit: Char): Int? = when (digit) {
        '0' -> ToneGenerator.TONE_DTMF_0
        '1' -> ToneGenerator.TONE_DTMF_1
        '2' -> ToneGenerator.TONE_DTMF_2
        '3' -> ToneGenerator.TONE_DTMF_3
        '4' -> ToneGenerator.TONE_DTMF_4
        '5' -> ToneGenerator.TONE_DTMF_5
        '6' -> ToneGenerator.TONE_DTMF_6
        '7' -> ToneGenerator.TONE_DTMF_7
        '8' -> ToneGenerator.TONE_DTMF_8
        '9' -> ToneGenerator.TONE_DTMF_9
        '*' -> ToneGenerator.TONE_DTMF_S
        '#' -> ToneGenerator.TONE_DTMF_P
        '+' -> ToneGenerator.TONE_DTMF_0
        else -> null
    }
}
