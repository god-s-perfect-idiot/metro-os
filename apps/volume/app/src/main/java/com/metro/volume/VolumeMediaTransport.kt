package com.metro.volume

/**
 * Active media-session snapshot for the WP8.1 volume HUD music-transport chrome
 * (Universal Volume Control).
 */
data class VolumeMediaTransport(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val canPlayPause: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
) {
    val hasIdentity: Boolean
        get() = !title.isNullOrBlank() || !artist.isNullOrBlank()
}
