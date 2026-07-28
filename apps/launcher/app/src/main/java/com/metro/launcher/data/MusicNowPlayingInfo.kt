package com.metro.launcher.data

/**
 * Active media-session snapshot for a music app, mapped onto the Xbox Music–style Start face.
 *
 * [albumArtUri] is a content/file URI the launcher can decode (may be a cached bitmap dump when
 * the session only exposes a [android.graphics.Bitmap]).
 */
data class MusicNowPlayingInfo(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val albumArtUri: String?,
    val isPlaying: Boolean,
    val canPlayPause: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
    val updatedAtMs: Long,
) {
    val hasTrack: Boolean
        get() = !title.isNullOrBlank() || !artist.isNullOrBlank() || !albumArtUri.isNullOrBlank()
}
