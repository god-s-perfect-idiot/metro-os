package com.metro.music.data

import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer
import okio.buffer
import okio.source

/** Artwork request for an on-device track: embedded picture first, MediaStore thumbnail second. */
data class LocalArtwork(
    val songUri: Uri,
    val albumArtUri: Uri?,
)

fun Song.artworkModel(): Any? = when {
    source == LibrarySource.Local && uri != null -> LocalArtwork(uri, artworkUri)
    else -> artworkUri
}

object ArtworkUrls {
    /** Big enough to fill the now playing square on a 1080p phone without client upscaling. */
    const val PREFERRED_PX = 1024

    private val SIZE_PARAMS = Regex("=w\\d+-h\\d+")

    /**
     * Innertube hands back list-sized art (`…=w60-h60-l90-rj`). The requested render size lives
     * in the URL itself, so rewrite it instead of stretching a 60px square across the pane.
     */
    fun highRes(url: String, px: Int = PREFERRED_PX): String =
        SIZE_PARAMS.replace(url, "=w$px-h$px")
}

/**
 * MediaStore's `audio/albumart` provider only serves its cached low-resolution thumbnail, which
 * looks soft behind the now playing square, so read the full embedded picture out of the track
 * itself and fall back to the provider for files that carry no art of their own.
 */
class LocalArtworkFetcher(
    private val data: LocalArtwork,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        embeddedPicture()?.let { bytes ->
            return SourceResult(
                source = ImageSource(Buffer().apply { write(bytes) }, options.context),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }
        val fallback = data.albumArtUri ?: return null
        val stream = runCatching { options.context.contentResolver.openInputStream(fallback) }
            .getOrNull()
            ?: return null
        return SourceResult(
            source = ImageSource(stream.source().buffer(), options.context),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    private fun embeddedPicture(): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(options.context, data.songUri)
            retriever.embeddedPicture
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    class Factory : Fetcher.Factory<LocalArtwork> {
        override fun create(
            data: LocalArtwork,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = LocalArtworkFetcher(data, options)
    }
}
