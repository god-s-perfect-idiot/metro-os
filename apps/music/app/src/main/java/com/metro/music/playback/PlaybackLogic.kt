package com.metro.music.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import com.metro.music.data.LibrarySource
import com.metro.music.data.Song

/**
 * Rebuilds a [Song] from the Media3 session after the UI process is killed.
 * The player keeps playing in [MusicPlaybackService]; now-playing must not wait on a library scan.
 */
object PlaybackLogic {
    const val EXTRA_SOURCE = "metro.music.source"
    const val EXTRA_YOUTUBE_VIDEO_ID = "metro.music.youtubeVideoId"
    const val EXTRA_SONG_URI = "metro.music.songUri"

    fun extrasFor(song: Song): Bundle = Bundle().apply {
        putString(EXTRA_SOURCE, song.source.name)
        song.youtubeVideoId?.let { putString(EXTRA_YOUTUBE_VIDEO_ID, it) }
        song.uri?.let { putString(EXTRA_SONG_URI, it.toString()) }
    }

    fun resolveCurrentSong(
        mediaItem: MediaItem?,
        durationMs: Long,
        libraryLookup: (String) -> Song?,
    ): Song? {
        if (mediaItem == null) return null
        val id = mediaItem.mediaId
        if (id.isNotEmpty()) {
            libraryLookup(id)?.let { return it }
        }
        return songFromMediaItem(mediaItem, durationMs)
    }

    fun songFromMediaItem(item: MediaItem, durationMs: Long): Song? {
        val id = item.mediaId
        if (id.isEmpty()) return null
        val meta = item.mediaMetadata
        val extras = meta.extras
        val source = sourceOf(id, extras)
        val youtubeId = extras?.getString(EXTRA_YOUTUBE_VIDEO_ID)
            ?: id.takeIf { it.startsWith("yt:") }?.removePrefix("yt:")
        return Song(
            id = id,
            title = meta.title?.toString()?.ifBlank { null } ?: "Unknown title",
            artist = meta.artist?.toString()?.ifBlank { null } ?: "Unknown artist",
            album = meta.albumTitle?.toString()?.ifBlank { null } ?: "Unknown album",
            durationMs = durationMs.coerceAtLeast(0L),
            uri = songUri(id, source, extras, item),
            artworkUri = meta.artworkUri,
            source = source,
            youtubeVideoId = youtubeId,
        )
    }

    private fun sourceOf(id: String, extras: Bundle?): LibrarySource {
        val named = extras?.getString(EXTRA_SOURCE)
        return when (named) {
            LibrarySource.YouTubeMusic.name -> LibrarySource.YouTubeMusic
            LibrarySource.Local.name -> LibrarySource.Local
            else -> if (id.startsWith("yt:")) LibrarySource.YouTubeMusic else LibrarySource.Local
        }
    }

    private fun songUri(
        id: String,
        source: LibrarySource,
        extras: Bundle?,
        item: MediaItem,
    ): Uri? {
        extras?.getString(EXTRA_SONG_URI)?.let { encoded ->
            return Uri.parse(encoded)
        }
        item.localConfiguration?.uri?.let { uri ->
            if (source == LibrarySource.Local) return uri
        }
        if (source == LibrarySource.Local && id.startsWith("local:")) {
            val mediaStoreId = id.removePrefix("local:").toLongOrNull() ?: return null
            return ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mediaStoreId,
            )
        }
        return null
    }
}
