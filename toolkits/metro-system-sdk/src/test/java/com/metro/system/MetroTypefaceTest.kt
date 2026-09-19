package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Test

class MetroTypefaceTest {
    @Test
    fun fromStorage_knownValues() {
        assertEquals(MetroTypeface.MetroNoto, MetroTypeface.fromStorage("metro_noto"))
        assertEquals(MetroTypeface.SourceSans3, MetroTypeface.fromStorage("source_sans_3"))
        assertEquals(MetroTypeface.AlegreyaSans, MetroTypeface.fromStorage("alegreya_sans"))
    }

    @Test
    fun fromStorage_unknownFallsBackToDefault() {
        assertEquals(MetroTypeface.DEFAULT, MetroTypeface.fromStorage(null))
        assertEquals(MetroTypeface.DEFAULT, MetroTypeface.fromStorage(""))
        assertEquals(MetroTypeface.DEFAULT, MetroTypeface.fromStorage("comic_sans"))
    }

    @Test
    fun displayNames() {
        assertEquals("Metro Noto", MetroTypeface.MetroNoto.displayName)
        assertEquals("Source Sans 3", MetroTypeface.SourceSans3.displayName)
        assertEquals("Alegreya Sans", MetroTypeface.AlegreyaSans.displayName)
    }
}
