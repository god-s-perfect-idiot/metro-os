package com.metro.launcher

import androidx.compose.ui.graphics.Color
import com.metro.launcher.ui.AppListSearchLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListSearchLogicTest {
    private val accent = Color(0xFF1BA1E2)

    @Test
    fun highlightMatch_emptyQuery_returnsPlainLabel() {
        val result = AppListSearchLogic.highlightMatch("Settings", "", accent)
        assertEquals("Settings", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun highlightMatch_firstOccurrence_caseInsensitive() {
        val result = AppListSearchLogic.highlightMatch("Bing Sports", "s", accent)
        assertEquals("Bing Sports", result.text)
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles.single()
        assertEquals(5, span.start)
        assertEquals(6, span.end)
        assertEquals(accent, span.item.color)
    }

    @Test
    fun highlightMatch_leadingMatch() {
        val result = AppListSearchLogic.highlightMatch("Settings", "s", accent)
        val span = result.spanStyles.single()
        assertEquals(0, span.start)
        assertEquals(1, span.end)
    }

    @Test
    fun highlightMatch_multiCharSubstring() {
        val result = AppListSearchLogic.highlightMatch("Nokia Creative Studio", "st", accent)
        val span = result.spanStyles.single()
        assertEquals(15, span.start)
        assertEquals(17, span.end)
    }

    @Test
    fun highlightMatch_noMatch_returnsPlainLabel() {
        val result = AppListSearchLogic.highlightMatch("Calculator", "xyz", accent)
        assertEquals("Calculator", result.text)
        assertEquals(0, result.spanStyles.size)
    }
}
