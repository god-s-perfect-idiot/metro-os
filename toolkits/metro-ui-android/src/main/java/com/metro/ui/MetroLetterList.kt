package com.metro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * Pins a letter section marker at the top of a [androidx.compose.foundation.lazy.LazyColumn]
 * until the next section's header pushes it off — WP8.1 LongListSelector sticky group headers.
 *
 * Use whenever a list groups rows under alphabet markers (app list, People `all`, etc.).
 * Place section rows after each call with [androidx.compose.foundation.lazy.items].
 *
 * Give [content] an opaque theme background so list rows do not show through while pinned.
 *
 * @see MetroJumpList
 * @see MetroLetterTile
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.metroStickyLetterHeader(
    letter: Char,
    key: Any = "header-$letter",
    contentType: Any? = "metro-letter-header",
    content: @Composable () -> Unit,
) {
    stickyHeader(key = key, contentType = contentType) {
        content()
    }
}
