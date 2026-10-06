package com.metro.news.data

enum class NewsCategory(
    val id: String,
    val title: String,
    val feedUrl: String,
    val sourceLabel: String,
    val sourceSubtitle: String,
) {
    Top(
        id = "top",
        title = "headlines",
        feedUrl = "https://feeds.bbci.co.uk/news/rss.xml",
        sourceLabel = "BBC News",
        sourceSubtitle = "Top stories",
    ),
    World(
        id = "world",
        title = "world",
        feedUrl = "https://feeds.bbci.co.uk/news/world/rss.xml",
        sourceLabel = "BBC World",
        sourceSubtitle = "International headlines",
    ),
    Technology(
        id = "technology",
        title = "technology",
        feedUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml",
        sourceLabel = "BBC Technology",
        sourceSubtitle = "Gadgets and science",
    ),
    Business(
        id = "business",
        title = "business",
        feedUrl = "https://feeds.bbci.co.uk/news/business/rss.xml",
        sourceLabel = "BBC Business",
        sourceSubtitle = "Markets and companies",
    ),
}

data class NewsStory(
    val id: String,
    val title: String,
    val summary: String,
    val link: String,
    val source: String,
    val publishedAtMs: Long,
    val imageUrl: String?,
    val category: NewsCategory,
    val author: String? = null,
)

data class NewsFeedSnapshot(
    val category: NewsCategory,
    val stories: List<NewsStory>,
    val fetchedAtMs: Long,
)
