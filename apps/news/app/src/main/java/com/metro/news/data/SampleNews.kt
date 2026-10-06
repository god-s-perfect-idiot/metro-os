package com.metro.news.data

object SampleNews {
    fun snapshots(nowMs: Long = System.currentTimeMillis()): List<NewsFeedSnapshot> {
        return NewsCategory.entries.map { category ->
            NewsFeedSnapshot(
                category = category,
                stories = sampleStories(category, nowMs),
                fetchedAtMs = nowMs,
            )
        }
    }

    private fun sampleStories(category: NewsCategory, nowMs: Long): List<NewsStory> {
        val base = when (category) {
            NewsCategory.Top -> listOf(
                Triple(
                    "Metro cities expand late-night transit",
                    "Several metro systems announced extended weekend hours after ridership rebounded.",
                    null,
                ),
                Triple(
                    "Chipmakers race to ship next-gen phones",
                    "Flagship devices arrive with brighter screens and longer battery life.",
                    null,
                ),
                Triple(
                    "Markets steady as inflation cools",
                    "Investors watched central-bank signals after a quieter jobs report.",
                    null,
                ),
            )
            NewsCategory.World -> listOf(
                Triple(
                    "Summit talks focus on climate funding",
                    "Delegates debated how to share costs for coastal defenses and clean energy.",
                    null,
                ),
                Triple(
                    "Border towns reopen after storms",
                    "Rescue crews restored power and cleared roads after days of flooding.",
                    null,
                ),
            )
            NewsCategory.Technology -> listOf(
                Triple(
                    "Browsers tighten tracking defaults",
                    "New privacy settings block more third-party cookies without breaking logins.",
                    null,
                ),
                Triple(
                    "Open maps project hits 10 million edits",
                    "Volunteers mapped trails, transit stops, and small-business hours worldwide.",
                    null,
                ),
            )
            NewsCategory.Business -> listOf(
                Triple(
                    "Airlines cut fares for spring travel",
                    "Carriers added weekend routes as demand for short city breaks rose.",
                    null,
                ),
                Triple(
                    "Retailers lean into same-day pickup",
                    "Stores converted stockrooms into micro-fulfillment hubs near downtown.",
                    null,
                ),
            )
        }
        return base.mapIndexed { index, (title, summary, image) ->
            NewsStory(
                id = "${category.id}:sample-$index",
                title = title,
                summary = summary,
                link = "https://example.com/news/${category.id}/$index",
                source = category.sourceLabel,
                publishedAtMs = nowMs - (index + 1) * 3_600_000L,
                imageUrl = image,
                category = category,
            )
        }
    }
}
