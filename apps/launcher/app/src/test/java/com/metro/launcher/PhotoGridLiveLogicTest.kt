package com.metro.launcher

import com.metro.launcher.ui.PhotoGridLiveLogic
import com.metro.system.MetroTileGridCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhotoGridLiveLogicTest {
    private val accents = listOf(
        MetroTileGridCell(colorHex = "#111111"),
        MetroTileGridCell(colorHex = "#222222"),
        MetroTileGridCell(colorHex = "#333333"),
    )
    private val pool = (1..8).map { id ->
        MetroTileGridCell(
            colorHex = "#AABBCC",
            imageUri = "content://people/photo/$id",
            label = id.toString(),
        )
    }

    @Test
    fun poolIdentity_ignoresShuffleOrder() {
        val identity = PhotoGridLiveLogic.poolIdentity(pool + accents)
        val shuffled = PhotoGridLiveLogic.poolIdentity(pool.shuffled() + accents)
        assertEquals(identity, shuffled)
        assertEquals(pool.size, identity.size)
    }

    @Test
    fun contactPool_dedupesByUri() {
        val cells = pool.take(2) + pool.take(2) + accents
        assertEquals(2, PhotoGridLiveLogic.contactPool(cells).size)
    }

    @Test
    fun initialLayout_capsVisibleContactsAtFour() {
        val layout = PhotoGridLiveLogic.initialLayout(pool, accents, cellCount = 9, random = Random(1))
        assertEquals(9, layout.size)
        assertEquals(4, PhotoGridLiveLogic.visibleContactCount(layout))
        assertTrue(PhotoGridLiveLogic.isValidMosaic(layout))
    }

    @Test
    fun nextFlip_neverExceedsFourOrDuplicates() {
        var layout = PhotoGridLiveLogic.initialLayout(pool, accents, cellCount = 9, random = Random(4))
        val rng = Random(5)
        repeat(80) {
            val flip = PhotoGridLiveLogic.nextFlip(layout, pool, accents, rng)
            assertNotNull(flip)
            layout = layout.toMutableList().also { it[flip!!.first] = flip.second }
            assertTrue(PhotoGridLiveLogic.isValidMosaic(layout))
        }
    }

    @Test
    fun nextFlip_emptyPool_returnsNull() {
        val layout = PhotoGridLiveLogic.initialLayout(emptyList(), accents, cellCount = 9, random = Random(6))
        assertNull(PhotoGridLiveLogic.nextFlip(layout, emptyList(), accents, Random(7)))
    }

    @Test
    fun nextFlip_cyclesThroughEntirePool() {
        var layout = PhotoGridLiveLogic.initialLayout(pool, accents, cellCount = 9, random = Random(10))
        val seenKeys = mutableSetOf<String>()
        layout.mapNotNull { PhotoGridLiveLogic.contactKey(it) }.forEach { seenKeys += it }
        val rng = Random(11)
        repeat(60) {
            val flip = PhotoGridLiveLogic.nextFlip(layout, pool, accents, rng) ?: return@repeat
            layout = layout.toMutableList().also { it[flip.first] = flip.second }
            PhotoGridLiveLogic.contactKey(flip.second)?.let { seenKeys += it }
            assertTrue(PhotoGridLiveLogic.isValidMosaic(layout))
        }
        assertEquals(pool.size, seenKeys.size)
    }

    @Test
    fun nextFlip_smallPool_neverDuplicatesWhenReshuffling() {
        val smallPool = pool.take(4)
        var layout = PhotoGridLiveLogic.initialLayout(smallPool, accents, cellCount = 9, random = Random(12))
        val rng = Random(13)
        repeat(60) {
            val flip = PhotoGridLiveLogic.nextFlip(layout, smallPool, accents, rng) ?: return@repeat
            layout = layout.toMutableList().also { it[flip.first] = flip.second }
            assertTrue(PhotoGridLiveLogic.isValidMosaic(layout))
        }
    }
}
