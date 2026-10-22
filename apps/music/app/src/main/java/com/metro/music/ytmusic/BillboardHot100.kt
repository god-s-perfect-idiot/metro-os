package com.metro.music.ytmusic

import android.util.Log
import com.metro.music.data.LibrarySource
import com.metro.music.data.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Billboard Hot 100 chart → playable YouTube Music tracks.
 *
 * Chart ranks/titles come from the public mhollingshead JSON mirror (updated weekly).
 * Each entry is resolved to a YT Music video via Innertube song search.
 */
object BillboardHot100 {
    private const val TAG = "BillboardHot100"
    private const val RECENT_URL =
        "https://raw.githubusercontent.com/mhollingshead/billboard-hot-100/main/recent.json"
    private const val RESOLVE_THREADS = 6

    data class ChartEntry(
        val rank: Int,
        val title: String,
        val artist: String,
    )

    fun fetchChart(http: OkHttpClient, limit: Int = 100): List<ChartEntry> {
        val request = Request.Builder().url(RECENT_URL).get().build()
        val text = http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "chart HTTP ${resp.code}")
                return emptyList()
            }
            resp.body?.string().orEmpty()
        }
        if (text.isBlank()) return emptyList()
        return parseChart(JSONObject(text), limit)
    }

    fun parseChart(root: JSONObject, limit: Int = 100): List<ChartEntry> {
        val data = root.optJSONArray("data") ?: return emptyList()
        val out = ArrayList<ChartEntry>(minOf(limit, data.length()))
        for (i in 0 until data.length()) {
            if (out.size >= limit) break
            val item = data.optJSONObject(i) ?: continue
            val title = item.optString("song").ifBlank { null } ?: continue
            val artist = item.optString("artist").ifBlank { "Unknown artist" }
            val rank = item.optInt("this_week", out.size + 1)
            out += ChartEntry(rank = rank, title = title, artist = artist)
        }
        return out.sortedBy { it.rank }
    }

    /**
     * Resolve chart rows to [Song]s in rank order. Failed searches are skipped so the
     * list stays playable; callers should tolerate fewer than 100 items.
     */
    fun resolveSongs(
        entries: List<ChartEntry>,
        searchSongs: (query: String, limit: Int) -> List<Song>,
    ): List<Song> {
        if (entries.isEmpty()) return emptyList()
        val pool = Executors.newFixedThreadPool(RESOLVE_THREADS)
        return try {
            val futures: List<Future<Song?>> = entries.map { entry ->
                pool.submit<Song?> {
                    resolveEntry(entry, searchSongs)
                }
            }
            futures.mapNotNull { future ->
                runCatching { future.get() }
                    .onFailure { Log.w(TAG, "resolve failed", it) }
                    .getOrNull()
            }
        } finally {
            pool.shutdownNow()
        }
    }

    private fun resolveEntry(
        entry: ChartEntry,
        searchSongs: (query: String, limit: Int) -> List<Song>,
    ): Song? {
        val query = "${entry.title} ${entry.artist}"
        val hits = runCatching { searchSongs(query, 5) }.getOrElse {
            Log.w(TAG, "search failed for rank=${entry.rank}", it)
            emptyList()
        }
        if (hits.isEmpty()) return null
        val matched = hits.firstOrNull { artistMatches(it.artist, entry.artist) } ?: hits.first()
        // Stable id keeps rank order even when the same video appears twice on the chart.
        return matched.copy(
            id = "bb:${entry.rank}:${matched.youtubeVideoId ?: matched.id}",
            source = LibrarySource.YouTubeMusic,
        )
    }

    /** Loose artist match — Billboard often uses "A Featuring B" / "A & B". */
    internal fun artistMatches(candidate: String, chartArtist: String): Boolean {
        val a = normalizeArtist(candidate)
        val b = normalizeArtist(chartArtist)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        if (a.contains(b) || b.contains(a)) return true
        val aTokens = a.split(' ').filter { it.length > 2 }.toSet()
        val bTokens = b.split(' ').filter { it.length > 2 }.toSet()
        if (aTokens.isEmpty() || bTokens.isEmpty()) return false
        return aTokens.intersect(bTokens).isNotEmpty()
    }

    private fun normalizeArtist(value: String): String =
        value.lowercase()
            .replace(Regex("""\s*(featuring|feat\.?|ft\.?|with|&|,)\s*"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
