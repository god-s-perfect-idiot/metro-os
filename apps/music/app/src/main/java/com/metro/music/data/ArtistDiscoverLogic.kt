package com.metro.music.data

/**
 * Dedupes YouTube Music “discover” results against what the user already has in collection.
 */
object ArtistDiscoverLogic {
    fun songKey(title: String, artist: String): String =
        "${title.trim().lowercase()}|${artist.trim().lowercase()}"

    fun albumKey(title: String, artist: String): String =
        "${title.trim().lowercase()}|${artist.trim().lowercase()}"

    fun songsNotInCollection(discovered: List<Song>, collection: List<Song>): List<Song> {
        if (discovered.isEmpty()) return emptyList()
        val ids = collection.map { it.id }.toSet()
        val keys = collection.map { songKey(it.title, it.artist) }.toSet()
        return discovered.filter { song ->
            song.id !in ids && songKey(song.title, song.artist) !in keys
        }
    }

    fun albumsNotInCollection(discovered: List<Album>, collection: List<Album>): List<Album> {
        if (discovered.isEmpty()) return emptyList()
        val keys = collection.map { albumKey(it.title, it.artist) }.toSet()
        val browseIds = collection.mapNotNull { it.youtubeBrowseId }.toSet()
        return discovered.filter { album ->
            albumKey(album.title, album.artist) !in keys &&
                (album.youtubeBrowseId == null || album.youtubeBrowseId !in browseIds)
        }
    }
}
