package com.metro.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroMultiSelectLogicTest {
    @Test
    fun toggleSelection_addsAndRemoves() {
        var selected = setOf("a", "b")
        selected = if ("c" in selected) selected - "c" else selected + "c"
        assertEquals(setOf("a", "b", "c"), selected)
        selected = if ("a" in selected) selected - "a" else selected + "a"
        assertEquals(setOf("b", "c"), selected)
    }

    @Test
    fun itemIdentity_usesId() {
        val item = MetroMultiSelectItem(id = "com.metro.photos", title = "Photos")
        assertEquals("com.metro.photos", item.id)
        assertTrue(item.title.isNotBlank())
        assertFalse(item.id == item.title)
    }
}
