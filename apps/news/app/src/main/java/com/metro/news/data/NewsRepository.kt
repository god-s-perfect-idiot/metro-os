package com.metro.news.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class NewsRepository(
    context: Context,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build(),
) {
    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "rss").also { it.mkdirs() }
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isCategoryEnabled(category: NewsCategory): Boolean {
        return prefs.getBoolean(keyEnabled(category), true)
    }

    fun setCategoryEnabled(category: NewsCategory, enabled: Boolean) {
        prefs.edit().putBoolean(keyEnabled(category), enabled).apply()
    }

    suspend fun load(forceRefresh: Boolean = false): List<NewsFeedSnapshot> = withContext(Dispatchers.IO) {
        coroutineScope {
            NewsCategory.entries.map { category ->
                async { loadCategory(category, forceRefresh) }
            }.awaitAll()
        }
    }

    private fun loadCategory(category: NewsCategory, forceRefresh: Boolean): NewsFeedSnapshot {
        val now = System.currentTimeMillis()
        if (!isCategoryEnabled(category)) {
            return NewsFeedSnapshot(category, emptyList(), now)
        }

        val cachedXml = readCache(category)
        if (!forceRefresh && cachedXml != null) {
            val stories = runCatching { RssParser.parse(cachedXml, category, now) }.getOrDefault(emptyList())
            if (stories.isNotEmpty()) {
                return NewsFeedSnapshot(category, stories, now)
            }
        }

        val networkXml = runCatching { fetch(category.feedUrl) }.getOrNull()
        if (networkXml != null) {
            writeCache(category, networkXml)
            val stories = runCatching { RssParser.parse(networkXml, category, now) }.getOrDefault(emptyList())
            if (stories.isNotEmpty()) {
                return NewsFeedSnapshot(category, stories, now)
            }
        }

        if (cachedXml != null) {
            val stories = runCatching { RssParser.parse(cachedXml, category, now) }.getOrDefault(emptyList())
            if (stories.isNotEmpty()) {
                return NewsFeedSnapshot(category, stories, now)
            }
        }

        val sample = SampleNews.snapshots(now).first { it.category == category }
        return sample
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "metro-os-news/1.0")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun cacheFile(category: NewsCategory): File = File(cacheDir, "${category.id}.xml")

    private fun readCache(category: NewsCategory): String? {
        val file = cacheFile(category)
        if (!file.isFile) return null
        return runCatching { file.readText() }.getOrNull()
    }

    private fun writeCache(category: NewsCategory, xml: String) {
        runCatching { cacheFile(category).writeText(xml) }
    }

    private fun keyEnabled(category: NewsCategory): String = "enabled_${category.id}"

    companion object {
        private const val PREFS = "metro_news"
    }
}
