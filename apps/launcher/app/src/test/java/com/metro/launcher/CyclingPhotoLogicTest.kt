package com.metro.launcher

import com.metro.launcher.ui.nextRandomCycleIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CyclingPhotoLogicTest {
    @Test
    fun nextRandomCycleIndex_singleCell_staysAtZero() {
        assertEquals(0, nextRandomCycleIndex(current = 0, size = 1, random = Random(1)))
        assertEquals(0, nextRandomCycleIndex(current = 3, size = 0, random = Random(1)))
    }

    @Test
    fun nextRandomCycleIndex_neverRepeatsCurrent() {
        val rng = Random(42)
        var current = 0
        repeat(40) {
            val next = nextRandomCycleIndex(current, size = 5, random = rng)
            assertTrue(next in 0 until 5)
            assertNotEquals(current, next)
            current = next
        }
    }
}
