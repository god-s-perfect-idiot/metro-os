package com.metro.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.metro.music.data.LibrarySource
import com.metro.music.data.Song
import com.metro.music.playback.MusicPlaybackService
import com.metro.music.playback.PlaybackLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaybackLogicTest {
    private val local = Song(
        id = "local:42",
        title = "Night Drive",
        artist = "Alpha",
        album = "One",
        durationMs = 90_000,
        uri = Uri.parse("content://media/external/audio/media/42"),
        artworkUri = Uri.parse("content://media/external/audio/albumart/7"),
        source = LibrarySource.Local,
    )
    private val yt = Song(
        id = "yt:abc",
        title = "Stream",
        artist = "Beta",
        album = "Two",
        durationMs = 120_000,
        uri = null,
        artworkUri = Uri.parse("https://img.example/art.jpg"),
        source = LibrarySource.YouTubeMusic,
        youtubeVideoId = "abc",
    )

    @Test
    fun resolve_prefersLibrarySongWhenCatalogHasLoaded() {
        val item = MusicPlaybackService.mediaItemFor(local, local.uri.toString())
        val resolved = PlaybackLogic.resolveCurrentSong(item, 12_000) { id ->
            local.takeIf { it.id == id }
        }
        assertSame(local, resolved)
    }

    @Test
    fun resolve_rebuildsFromSessionWhenLibraryIsStillEmpty() {
        val item = MusicPlaybackService.mediaItemFor(yt, "https://googlevideo.example/stream")
        val resolved = PlaybackLogic.resolveCurrentSong(item, 45_000) { null }

        requireNotNull(resolved)
        assertEquals(yt.id, resolved.id)
        assertEquals(yt.title, resolved.title)
        assertEquals(yt.artist, resolved.artist)
        assertEquals(yt.album, resolved.album)
        assertEquals(yt.artworkUri, resolved.artworkUri)
        assertEquals(LibrarySource.YouTubeMusic, resolved.source)
        assertEquals("abc", resolved.youtubeVideoId)
        assertEquals(45_000L, resolved.durationMs)
    }

    @Test
    fun songFromMediaItem_restoresLocalUriFromExtrasAfterControllerReconnect() {
        val item = MusicPlaybackService.mediaItemFor(local, local.uri.toString())
        // Controllers do not receive localConfiguration; extras must carry the content uri.
        val remote = MediaItem.Builder()
            .setMediaId(item.mediaId)
            .setMediaMetadata(item.mediaMetadata)
            .build()

        val restored = PlaybackLogic.songFromMediaItem(remote, 90_000)
        requireNotNull(restored)
        assertEquals(local.uri, restored.uri)
        assertEquals(LibrarySource.Local, restored.source)
        assertEquals(local.artworkUri, restored.artworkUri)
    }

    @Test
    fun songFromMediaItem_infersYtFromMediaIdWhenExtrasMissing() {
        val item = MediaItem.Builder()
            .setMediaId("yt:xyz")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Live")
                    .setArtist("Gamma")
                    .setAlbumTitle("YouTube Music")
                    .build(),
            )
            .build()

        val restored = PlaybackLogic.songFromMediaItem(item, 1_000)
        requireNotNull(restored)
        assertEquals(LibrarySource.YouTubeMusic, restored.source)
        assertEquals("xyz", restored.youtubeVideoId)
        assertEquals("Live", restored.title)
    }

    @Test
    fun resolve_nullMediaItemIsEmpty() {
        assertNull(PlaybackLogic.resolveCurrentSong(null, 0L) { error("should not lookup") })
    }
}
