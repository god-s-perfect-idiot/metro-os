package com.metro.music.data

import android.net.Uri
import com.metro.ui.MetroJumpListLogic

enum class LibrarySource {
    Local,
    YouTubeMusic,
}

enum class ShowingFilter {
    All,
    OnDevice,
    YouTubeMusic,
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri?,
    val artworkUri: Uri?,
    val source: LibrarySource,
    val youtubeVideoId: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    /** Local MediaStore genre tag when present; YT Music library items usually omit this. */
    val genre: String? = null,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val songCount: Int,
    val source: LibrarySource,
    /** YouTube Music browse id (`MPREb_…`) when this album came from search/discover. */
    val youtubeBrowseId: String? = null,
)

data class Artist(
    val id: String,
    val name: String,
    val songCount: Int,
    val source: LibrarySource,
)

data class Playlist(
    val id: String,
    val title: String,
    val songCount: Int,
    val source: LibrarySource,
    val artworkUri: Uri? = null,
    val youtubePlaylistId: String? = null,
    val localMediaStoreId: Long? = null,
)

data class Genre(
    val id: String,
    val name: String,
    val songCount: Int,
    val source: LibrarySource,
)

data class PlaybackQueueItem(
    val song: Song,
    val queueIndex: Int,
)

object LibraryLogic {
    fun filterSongs(songs: List<Song>, filter: ShowingFilter): List<Song> = when (filter) {
        ShowingFilter.All -> songs
        ShowingFilter.OnDevice -> songs.filter { it.source == LibrarySource.Local }
        ShowingFilter.YouTubeMusic -> songs.filter { it.source == LibrarySource.YouTubeMusic }
    }

    fun filterPlaylists(playlists: List<Playlist>, filter: ShowingFilter): List<Playlist> = when (filter) {
        ShowingFilter.All -> playlists
        ShowingFilter.OnDevice -> playlists.filter { it.source == LibrarySource.Local }
        ShowingFilter.YouTubeMusic -> playlists.filter { it.source == LibrarySource.YouTubeMusic }
    }

    fun artistsFrom(songs: List<Song>): List<Artist> =
        songs.groupBy { it.artist.ifBlank { "Unknown artist" } }
            .map { (name, group) ->
                Artist(
                    id = "artist:${name.lowercase()}",
                    name = name,
                    songCount = group.size,
                    source = if (group.all { it.source == LibrarySource.YouTubeMusic }) {
                        LibrarySource.YouTubeMusic
                    } else {
                        LibrarySource.Local
                    },
                )
            }
            .sortedBy { it.name.lowercase() }

    fun albumsFrom(songs: List<Song>): List<Album> =
        songs
            .filter { !isPlaceholderAlbumTitle(it.album) }
            .groupBy { (it.album.ifBlank { "Unknown album" }) to (it.artist.ifBlank { "Unknown artist" }) }
            .map { (key, group) ->
                val (album, artist) = key
                Album(
                    id = "album:${album.lowercase()}|${artist.lowercase()}",
                    title = album,
                    artist = artist,
                    artworkUri = group.firstNotNullOfOrNull { it.artworkUri },
                    songCount = group.size,
                    source = if (group.all { it.source == LibrarySource.YouTubeMusic }) {
                        LibrarySource.YouTubeMusic
                    } else {
                        LibrarySource.Local
                    },
                )
            }
            .sortedBy { it.title.lowercase() }

    /** Synthetic album titles from streaming rows that lack real album metadata. */
    fun isPlaceholderAlbumTitle(title: String?): Boolean {
        val t = title?.trim().orEmpty()
        if (t.isEmpty()) return true
        return t.equals("YouTube Music", ignoreCase = true) ||
            t.equals("Unknown album", ignoreCase = true) ||
            t.equals("Unknown", ignoreCase = true)
    }

    /** Only tagged genres — blank/missing tags are omitted (no synthetic “Unknown genre”). */
    fun genresFrom(songs: List<Song>): List<Genre> =
        songs
            .mapNotNull { song ->
                val name = song.genre?.trim().orEmpty()
                if (name.isEmpty()) null else song to name
            }
            .groupBy { (_, name) -> name.lowercase() }
            .map { (_, group) ->
                val name = group.first().second
                Genre(
                    id = "genre:${name.lowercase()}",
                    name = name,
                    songCount = group.size,
                    source = if (group.all { (song, _) -> song.source == LibrarySource.YouTubeMusic }) {
                        LibrarySource.YouTubeMusic
                    } else {
                        LibrarySource.Local
                    },
                )
            }
            .sortedBy { it.name.lowercase() }

    /**
     * Groups rows under find-by-letter keys — `#` section first, then `a`–`z` — so every
     * collection list orders identically to the jump grid it opens.
     */
    fun <T> groupByJumpKey(items: List<T>, label: (T) -> String): Map<Char, List<T>> =
        items
            .sortedBy { label(it).trim().lowercase() }
            .groupBy { MetroJumpListLogic.sortKey(label(it)) }
            .toSortedMap()

    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    fun formatRemaining(positionMs: Long, durationMs: Long): String {
        val remain = (durationMs - positionMs).coerceAtLeast(0)
        return "-${formatDuration(remain)}"
    }

    fun showingLabel(filter: ShowingFilter): String = when (filter) {
        ShowingFilter.All -> "showing all music"
        ShowingFilter.OnDevice -> "showing on this device"
        ShowingFilter.YouTubeMusic -> "showing youtube music"
    }
}
