package com.metro.music.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** One locally persisted play — snapshot enough to list and re-queue after relaunch. */
data class PlayHistoryEntry(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri?,
    val artworkUri: Uri?,
    val source: LibrarySource,
    val youtubeVideoId: String?,
    val albumId: String?,
    val artistId: String?,
    val playedAtMs: Long,
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = uri,
        artworkUri = artworkUri,
        source = source,
        youtubeVideoId = youtubeVideoId,
        albumId = albumId,
        artistId = artistId,
    )

    companion object {
        fun from(song: Song, playedAtMs: Long): PlayHistoryEntry = PlayHistoryEntry(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            uri = song.uri,
            artworkUri = song.artworkUri,
            source = song.source,
            youtubeVideoId = song.youtubeVideoId,
            albumId = song.albumId,
            artistId = song.artistId,
            playedAtMs = playedAtMs,
        )
    }
}

/**
 * Pure helpers for local play history: newest first, one row per song id, capped length.
 */
object PlayHistoryLogic {
    const val MaxEntries = 100

    fun record(
        song: Song,
        playedAtMs: Long,
        existing: List<PlayHistoryEntry>,
    ): List<PlayHistoryEntry> {
        val entry = PlayHistoryEntry.from(song, playedAtMs)
        return (listOf(entry) + existing.filter { it.songId != song.id }).take(MaxEntries)
    }

    /** Prefer live library metadata; fall back to the stored snapshot. */
    fun resolveSongs(
        entries: List<PlayHistoryEntry>,
        library: List<Song>,
    ): List<Song> {
        val byId = library.associateBy { it.id }
        return entries.map { entry -> byId[entry.songId] ?: entry.toSong() }
    }
}

/** SharedPreferences-backed store for play history on this device. */
class PlayHistoryStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<PlayHistoryEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching { decode(raw) }.getOrDefault(emptyList())
    }

    fun save(entries: List<PlayHistoryEntry>) {
        prefs.edit().putString(KEY_ENTRIES, encode(entries)).apply()
    }

    companion object {
        private const val PREFS = "music_play_history"
        private const val KEY_ENTRIES = "entries"

        internal fun encode(entries: List<PlayHistoryEntry>): String {
            val array = JSONArray()
            entries.forEach { entry ->
                array.put(
                    JSONObject()
                        .put("id", entry.songId)
                        .put("title", entry.title)
                        .put("artist", entry.artist)
                        .put("album", entry.album)
                        .put("durationMs", entry.durationMs)
                        .put("uri", entry.uri?.toString())
                        .put("artworkUri", entry.artworkUri?.toString())
                        .put("source", entry.source.name)
                        .put("youtubeVideoId", entry.youtubeVideoId)
                        .put("albumId", entry.albumId)
                        .put("artistId", entry.artistId)
                        .put("playedAtMs", entry.playedAtMs),
                )
            }
            return array.toString()
        }

        internal fun decode(raw: String): List<PlayHistoryEntry> {
            val array = JSONArray(raw)
            return buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    if (id.isBlank()) continue
                    val sourceName = obj.optString("source", LibrarySource.Local.name)
                    val source = runCatching { LibrarySource.valueOf(sourceName) }
                        .getOrDefault(LibrarySource.Local)
                    add(
                        PlayHistoryEntry(
                            songId = id,
                            title = obj.optString("title"),
                            artist = obj.optString("artist"),
                            album = obj.optString("album"),
                            durationMs = obj.optLong("durationMs"),
                            uri = obj.optStringOrNull("uri")?.let(Uri::parse),
                            artworkUri = obj.optStringOrNull("artworkUri")?.let(Uri::parse),
                            source = source,
                            youtubeVideoId = obj.optStringOrNull("youtubeVideoId"),
                            albumId = obj.optStringOrNull("albumId"),
                            artistId = obj.optStringOrNull("artistId"),
                            playedAtMs = obj.optLong("playedAtMs"),
                        ),
                    )
                }
            }
        }

        private fun JSONObject.optStringOrNull(key: String): String? {
            if (!has(key) || isNull(key)) return null
            return optString(key).takeIf { it.isNotBlank() }
        }
    }
}
