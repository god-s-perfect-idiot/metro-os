package com.metro.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * App-list search match highlighting — first case-insensitive substring match in accent.
 * Reference: references/images/applist_search_dark_blue.png
 */
object AppListSearchLogic {
    fun highlightMatch(
        label: String,
        query: String,
        matchColor: Color,
    ): AnnotatedString {
        val needle = query.trim()
        if (needle.isEmpty()) return AnnotatedString(label)
        val start = label.indexOf(needle, ignoreCase = true)
        if (start < 0) return AnnotatedString(label)
        val end = start + needle.length
        return buildAnnotatedString {
            append(label, 0, start)
            withStyle(SpanStyle(color = matchColor)) {
                append(label, start, end)
            }
            append(label, end, label.length)
        }
    }
}
