package com.metro.news.ui

sealed class NewsRoute {
    data object Hub : NewsRoute()
    data class Article(val storyId: String) : NewsRoute()
    data object Topics : NewsRoute()
    data object Sources : NewsRoute()
}

fun NewsRoute.parentRoute(): NewsRoute = when (this) {
    NewsRoute.Hub -> NewsRoute.Hub
    is NewsRoute.Article -> NewsRoute.Hub
    NewsRoute.Topics -> NewsRoute.Hub
    NewsRoute.Sources -> NewsRoute.Hub
}
