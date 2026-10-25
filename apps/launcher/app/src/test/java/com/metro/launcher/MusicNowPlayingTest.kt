package com.metro.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.MusicNowPlayingInfo
import com.metro.launcher.data.MusicTilePackages
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileNotificationInfo
import com.metro.launcher.data.TileNotificationStore
import com.metro.launcher.ui.musicTileTransportSize
import com.metro.launcher.ui.presentationTextColor
import com.metro.system.MetroMusicLiveTileStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicNowPlayingTest {
    @Test
    fun musicTileTransportSize_denseIsSmallerThanStandard() {
        assertTrue(
            musicTileTransportSize(PinnedTileSize.TwoByTwo, dense = true) <
                musicTileTransportSize(PinnedTileSize.TwoByTwo, dense = false),
        )
        assertTrue(
            musicTileTransportSize(PinnedTileSize.FourByTwo, dense = true) <
                musicTileTransportSize(PinnedTileSize.FourByTwo, dense = false),
        )
        assertEquals(28.dp, musicTileTransportSize(PinnedTileSize.TwoByTwo, dense = true))
        assertEquals(34.dp, musicTileTransportSize(PinnedTileSize.TwoByTwo, dense = false))
    }

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
    fun musicLiveTileStyle_defaultsCompactAndParses() {
        assertEquals(MetroMusicLiveTileStyle.CompactControls, MetroMusicLiveTileStyle.DEFAULT)
        assertEquals(
            MetroMusicLiveTileStyle.Presentation,
            MetroMusicLiveTileStyle.fromStorage("presentation"),
        )
        assertEquals(
            MetroMusicLiveTileStyle.SpaciousControls,
            MetroMusicLiveTileStyle.fromStorage("spacious"),
        )
        assertEquals(
            MetroMusicLiveTileStyle.CompactControls,
            MetroMusicLiveTileStyle.fromStorage(null),
        )
    }

    @Test
    fun presentationTextColor_lightensAccent() {
        val accent = Color(0xFF144514)
        val text = presentationTextColor(accent)
        assertTrue(text.red > accent.red)
        assertTrue(text.green > accent.green)
        assertTrue(text.blue > accent.blue)
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
