package com.metro.dialer.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log

/**
 * Plays the system default ringtone while an incoming call is ringing.
 *
 * Required because [MetroInCallService] declares `IN_CALL_SERVICE_RINGING` — Telecom
 * will not play a ringtone itself when that metadata is true.
 *
 * Uses [AudioManager.MODE_RINGTONE] + speaker so the tone is audible at ring volume
 * (playing through the in-call earpiece path is far too quiet).
 */
object IncomingRingtonePlayer {
    private const val TAG = "IncomingRingtone"

    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var previousMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeaker: Boolean = false

    fun start(context: Context) {
        if (player?.isPlaying == true) return
        stop()
        val appContext = context.applicationContext
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = am

        val uri = RingtoneManager.getActualDefaultRingtoneUri(
            appContext,
            RingtoneManager.TYPE_RINGTONE,
        ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (am.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        if (am.getStreamVolume(AudioManager.STREAM_RING) <= 0) return

        previousMode = am.mode
        @Suppress("DEPRECATION")
        previousSpeaker = am.isSpeakerphoneOn

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        requestFocus(am, attrs)

        am.mode = AudioManager.MODE_RINGTONE
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = true

        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(appContext, uri)
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }
            player = mediaPlayer
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start ringtone", e)
            restoreAudio()
        }
    }

    fun stop() {
        player?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: IllegalStateException) {
                // already stopped
            }
            try {
                mp.release()
            } catch (_: Exception) {
                // ignore
            }
        }
        player = null
        restoreAudio()
    }

    private fun requestFocus(am: AudioManager, attrs: AudioAttributes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
    }

    private fun restoreAudio() {
        val am = audioManager ?: return
        focusRequest?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.abandonAudioFocusRequest(it)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
        focusRequest = null
        am.mode = previousMode
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = previousSpeaker
        audioManager = null
    }
}
