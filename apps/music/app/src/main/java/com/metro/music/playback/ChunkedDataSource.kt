package com.metro.music.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Splits a read into sequential bounded HTTP requests.
 *
 * googlevideo answers 403 to a range-less or open-ended request and to any range wider than
 * roughly a megabyte, while media3 asks for the whole remainder of a progressive stream in one
 * go. This wrapper keeps every upstream request inside those limits and stitches the chunks back
 * into one continuous stream. Sources whose total length cannot be determined pass straight
 * through untouched.
 */
@UnstableApi
class ChunkedDataSource(
    private val upstream: DataSource,
    private val chunkSize: Long,
) : DataSource {

    private var passThrough = false
    private var spec: DataSpec? = null
    private var position = 0L
    private var bytesRemaining = 0L
    private var chunkRemaining = 0L
    private var chunkOpen = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        spec = dataSpec
        val total = totalLength(dataSpec)
        if (total == C.LENGTH_UNSET.toLong()) {
            passThrough = true
            return upstream.open(dataSpec)
        }
        passThrough = false
        position = dataSpec.position
        bytesRemaining = total
        openChunk()
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (passThrough) return upstream.read(buffer, offset, length)
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        if (chunkRemaining == 0L) {
            upstream.close()
            chunkOpen = false
            openChunk()
        }
        val toRead = minOf(length.toLong(), chunkRemaining).toInt()
        val read = upstream.read(buffer, offset, toRead)
        if (read == C.RESULT_END_OF_INPUT) return C.RESULT_END_OF_INPUT
        position += read
        chunkRemaining -= read
        bytesRemaining -= read
        return read
    }

    override fun getUri(): Uri? = if (passThrough) upstream.uri else spec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        if (passThrough || chunkOpen) upstream.close()
        passThrough = false
        chunkOpen = false
        chunkRemaining = 0L
        bytesRemaining = 0L
    }

    private fun openChunk() {
        val base = spec ?: return
        val length = minOf(chunkSize, bytesRemaining)
        upstream.open(
            base.buildUpon()
                .setPosition(position)
                .setLength(length)
                .build(),
        )
        chunkRemaining = length
        chunkOpen = true
    }

    /** googlevideo advertises the full size as `clen`; anything else stays pass-through. */
    private fun totalLength(dataSpec: DataSpec): Long {
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) return dataSpec.length
        val clen = runCatching { dataSpec.uri.getQueryParameter("clen") }
            .getOrNull()
            ?.toLongOrNull()
            ?: return C.LENGTH_UNSET.toLong()
        return (clen - dataSpec.position).coerceAtLeast(0L)
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val chunkSize: Long = DEFAULT_CHUNK_SIZE,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            ChunkedDataSource(upstreamFactory.createDataSource(), chunkSize)
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 512L * 1024L
    }
}
