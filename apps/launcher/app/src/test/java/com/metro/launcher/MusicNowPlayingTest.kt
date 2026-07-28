package com.metro.launcher

import com.metro.launcher.data.MusicNowPlayingInfo
import com.metro.launcher.data.MusicTilePackages
import com.metro.launcher.data.TileNotificationInfo
import com.metro.launcher.data.TileNotificationStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicNowPlayingTest {
    @Test
    fun knownPackages_includeMetroAndYoutubeMusic() {
        assertTrue(MusicTilePackages.contains("com.metro.music"))
        assertTrue(MusicTilePackages.contains("com.google.android.apps.youtube.music"))
        assertTrue(MusicTilePackages.contains("com.spotify.music"))
        assertFalse(MusicTilePackages.contains("com.google.android.gm"))
    }

    @Test
    fun hasTrack_trueWhenArtOrMetadataPresent() {
        assertTrue(
            MusicNowPlayingInfo(
                packageName = "com.metro.music",
                title = "Song",
                artist = null,
                albumArtUri = null,
                isPlaying = false,
                canPlayPause = true,
                canSkipNext = false,
                canSkipPrevious = false,
                updatedAtMs = 1L,
            ).hasTrack,
        )
        assertTrue(
            MusicNowPlayingInfo(
                packageName = "com.metro.music",
                title = null,
                artist = null,
                albumArtUri = "file:///tmp/art.jpg",
                isPlaying = true,
                canPlayPause = true,
                canSkipNext = true,
                canSkipPrevious = true,
                updatedAtMs = 1L,
            ).hasTrack,
        )
        assertFalse(
            MusicNowPlayingInfo(
                packageName = "com.metro.music",
                title = null,
                artist = null,
                albumArtUri = null,
                isPlaying = true,
                canPlayPause = true,
                canSkipNext = false,
                canSkipPrevious = false,
                updatedAtMs = 1L,
            ).hasTrack,
        )
    }

    @Test
    fun merge_skipsNotificationFlipWhenMusicRichFace() {
        // Mirrors LauncherRepository: music now-playing counts as a rich front face.
        val merged = TileNotificationStore.mergeIntoDisplay(
            packageName = "com.google.android.apps.youtube.music",
            providerCounter = null,
            providerBackFaceTitle = null,
            hasRichFrontFace = true,
            info = TileNotificationInfo(
                packageName = "com.google.android.apps.youtube.music",
                count = 1,
                peekTitle = "Now playing",
                peekBody = "Track",
                updatedAtMs = 1L,
            ),
        )
        assertEquals(1, merged.counter)
        assertNull(merged.backFaceTitle)
        assertFalse(merged.hasFlipFace)
    }

    @Test
    fun changedMusicPackages_detectsDiff() {
        val a = mapOf(
            "com.metro.music" to MusicNowPlayingInfo(
                packageName = "com.metro.music",
                title = "A",
                artist = "Artist",
                albumArtUri = null,
                isPlaying = true,
                canPlayPause = true,
                canSkipNext = true,
                canSkipPrevious = true,
                updatedAtMs = 1L,
            ),
        )
        val b = mapOf(
            "com.metro.music" to a.getValue("com.metro.music").copy(isPlaying = false),
        )
        assertEquals(
            setOf("com.metro.music"),
            TileNotificationListenerService.changedMusicPackages(a, b),
        )
        assertTrue(
            TileNotificationListenerService.changedMusicPackages(a, a).isEmpty(),
        )
    }
}
