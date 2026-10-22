package com.metro.music.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Injects the User-Agent / Origin / Referer / Cookie that minted a YouTube stream URL.
 */
@UnstableApi
class RequestHeaderDataSource(
    private val upstream: DataSource,
    private val defaultUserAgent: String,
) : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val minted = YtStreamPlayback.headersFor(dataSpec.uri)
        val headers = dataSpec.httpRequestHeaders.toMutableMap()
        headers["User-Agent"] = minted?.userAgent ?: defaultUserAgent
        minted?.origin?.takeIf { it.isNotBlank() }?.let { headers["Origin"] = it }
        minted?.referer?.takeIf { it.isNotBlank() }?.let { headers["Referer"] = it }
        minted?.cookie?.takeIf { it.isNotBlank() }?.let { headers["Cookie"] = it }
        return upstream.open(dataSpec.withRequestHeaders(headers))
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        upstream.close()
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val defaultUserAgent: String,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            RequestHeaderDataSource(upstreamFactory.createDataSource(), defaultUserAgent)
    }
}
