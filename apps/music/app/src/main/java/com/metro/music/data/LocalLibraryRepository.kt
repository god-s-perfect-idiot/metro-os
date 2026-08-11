package com.metro.music.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class LocalLibraryRepository(private val context: Context) {

    fun loadSongs(): List<Song> {
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC,
        )

        val songs = mutableListOf<Song>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC}!=0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
                val artUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId,
                )
                songs += Song(
                    id = "local:$id",
                    title = cursor.getString(titleCol).orEmpty().ifBlank { "Unknown title" },
                    artist = cursor.getString(artistCol).orEmpty().ifBlank { "Unknown artist" },
                    album = cursor.getString(albumCol).orEmpty().ifBlank { "Unknown album" },
                    durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                    uri = contentUri,
                    artworkUri = artUri,
                    source = LibrarySource.Local,
                    albumId = "local-album:$albumId",
                    artistId = null,
                )
            }
        }
        return songs
    }

    @Suppress("DEPRECATION")
    fun loadPlaylists(): List<Playlist> {
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        }
        val playlists = mutableListOf<Playlist>()
        runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.Audio.Playlists._ID,
                    MediaStore.Audio.Playlists.NAME,
                ),
                null,
                null,
                "${MediaStore.Audio.Playlists.NAME} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(nameCol).orEmpty().ifBlank { "Untitled playlist" }
                    playlists += Playlist(
                        id = "local-pl:$id",
                        title = title,
                        songCount = playlistMemberCount(id),
                        source = LibrarySource.Local,
                        localMediaStoreId = id,
                    )
                }
            }
        }
        return playlists
    }

    @Suppress("DEPRECATION")
    fun loadPlaylistSongs(playlistId: Long): List<Song> {
        val members = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        val songs = mutableListOf<Song>()
        runCatching {
            context.contentResolver.query(
                members,
                arrayOf(
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    MediaStore.Audio.Playlists.Members.TITLE,
                    MediaStore.Audio.Playlists.Members.ARTIST,
                    MediaStore.Audio.Playlists.Members.ALBUM,
                    MediaStore.Audio.Playlists.Members.DURATION,
                    MediaStore.Audio.Playlists.Members.ALBUM_ID,
                ),
                null,
                null,
                "${MediaStore.Audio.Playlists.Members.PLAY_ORDER} ASC",
            )?.use { cursor ->
                val audioIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM_ID)
                while (cursor.moveToNext()) {
                    val audioId = cursor.getLong(audioIdCol)
                    val albumId = cursor.getLong(albumIdCol)
                    songs += Song(
                        id = "local:$audioId",
                        title = cursor.getString(titleCol).orEmpty().ifBlank { "Unknown title" },
                        artist = cursor.getString(artistCol).orEmpty().ifBlank { "Unknown artist" },
                        album = cursor.getString(albumCol).orEmpty().ifBlank { "Unknown album" },
                        durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            audioId,
                        ),
                        artworkUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId,
                        ),
                        source = LibrarySource.Local,
                        albumId = "local-album:$albumId",
                        artistId = null,
                    )
                }
            }
        }
        return songs
    }

    @Suppress("DEPRECATION")
    private fun playlistMemberCount(playlistId: Long): Int {
        val members = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        return runCatching {
            context.contentResolver.query(
                members,
                arrayOf(MediaStore.Audio.Playlists.Members._ID),
                null,
                null,
                null,
            )?.use { it.count } ?: 0
        }.getOrDefault(0)
    }
}
