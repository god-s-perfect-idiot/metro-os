package com.metro.news.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.news.data.NewsCategory
import com.metro.news.data.NewsFeedSnapshot
import com.metro.news.data.NewsRepository
import com.metro.news.data.NewsStory
import com.metro.news.data.SampleNews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NewsState(
    context: Context,
    private val repository: NewsRepository = NewsRepository(context),
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var route by mutableStateOf<NewsRoute>(NewsRoute.Hub)
        private set
    var hubPage by mutableIntStateOf(0)
    var loading by mutableStateOf(true)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var snapshots by mutableStateOf(SampleNews.snapshots())
        private set
    var enabledCategories by mutableStateOf(
        NewsCategory.entries.associateWith { repository.isCategoryEnabled(it) },
    )
        private set

    val storiesById: Map<String, NewsStory>
        get() = snapshots.flatMap { it.stories }.associateBy { it.id }

    fun storiesFor(category: NewsCategory): List<NewsStory> {
        if (enabledCategories[category] == false) return emptyList()
        return snapshots.firstOrNull { it.category == category }?.stories.orEmpty()
    }

    fun featuredStory(): NewsStory? {
        val top = storiesFor(NewsCategory.Top)
        return top.firstOrNull { !it.imageUrl.isNullOrBlank() } ?: top.firstOrNull()
            ?: NewsCategory.entries
                .asSequence()
                .mapNotNull { storiesFor(it).firstOrNull() }
                .firstOrNull()
    }

    fun ensureLoaded() {
        if (!loading && snapshots.isNotEmpty()) return
        scope.launch { reload(force = false) }
    }

    fun refresh() {
        scope.launch { reload(force = true) }
    }

    private suspend fun reload(force: Boolean) {
        if (force) refreshing = true else loading = true
        val result = runCatching { repository.load(forceRefresh = force) }
            .getOrElse { SampleNews.snapshots() }
        snapshots = result
        enabledCategories = NewsCategory.entries.associateWith { repository.isCategoryEnabled(it) }
        loading = false
        refreshing = false
    }

    fun openArticle(story: NewsStory) {
        route = NewsRoute.Article(story.id)
    }

    fun openTopics() {
        route = NewsRoute.Topics
    }

    fun openSources() {
        route = NewsRoute.Sources
    }

    fun goBack() {
        route = route.parentRoute()
    }

    fun setCategoryEnabled(category: NewsCategory, enabled: Boolean) {
        repository.setCategoryEnabled(category, enabled)
        enabledCategories = enabledCategories.toMutableMap().also { it[category] = enabled }
        if (!enabled) {
            snapshots = snapshots.map { snap ->
                if (snap.category == category) {
                    NewsFeedSnapshot(category, emptyList(), snap.fetchedAtMs)
                } else {
                    snap
                }
            }
        } else {
            refresh()
        }
    }

    fun openInBrowser(story: NewsStory) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(story.link)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }
    }

    fun shareStory(story: NewsStory) {
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, story.title)
                putExtra(Intent.EXTRA_TEXT, "${story.title}\n${story.link}")
            }
            val chooser = Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(chooser)
        }
    }

    companion object {
        const val HUB_HERO = 0
        const val HUB_HEADLINES = 1
        const val HUB_WORLD = 2
        const val HUB_TECH = 3
        const val HUB_BUSINESS = 4
        const val HUB_PAGE_COUNT = 5
    }
}
