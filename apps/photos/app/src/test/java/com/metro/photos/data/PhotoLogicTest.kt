package com.metro.photos.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhotoLogicTest {
    private fun photo(
        id: Long,
        bucketId: String = "b1",
        bucketName: String = "Camera Roll",
        dateTakenMs: Long,
    ) = PhotoItem(
        id = id,
        uri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "img_$id.jpg",
        bucketId = bucketId,
        bucketName = bucketName,
        dateTakenMs = dateTakenMs,
        mimeType = "image/jpeg",
    )

    @Test
    fun sortByDateDesc_ordersNewestFirst() {
        val sorted = PhotoLogic.sortByDateDesc(
            listOf(
                photo(1, dateTakenMs = 1000L),
                photo(2, dateTakenMs = 3000L),
                photo(3, dateTakenMs = 2000L),
            ),
        )
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun groupByMonth_createsDescendingGroups() {
        val earlier = 1_704_067_200_000L // Jan 2024
        val later = 1_706_745_600_000L // Feb 2024
        val groups = PhotoLogic.groupByMonth(
            listOf(
                photo(1, dateTakenMs = earlier),
                photo(2, dateTakenMs = later),
                photo(3, dateTakenMs = earlier + 86_400_000L),
            ),
        )
        assertTrue(groups.size >= 2)
        if (groups.size >= 2) {
            assertTrue(groups[0].sortKey >= groups[1].sortKey)
        }
        val janGroup = groups.find { it.photos.any { p -> p.id == 1L || p.id == 3L } }
        assertEquals(2, janGroup?.photos?.size)
    }

    @Test
    fun groupByAlbum_usesBucketName() {
        val albums = PhotoLogic.groupByAlbum(
            listOf(
                photo(1, bucketId = "a", bucketName = "Screenshots", dateTakenMs = 1L),
                photo(2, bucketId = "a", bucketName = "Screenshots", dateTakenMs = 2L),
                photo(3, bucketId = "b", bucketName = "Camera Roll", dateTakenMs = 3L),
            ),
        )
        assertEquals(2, albums.size)
        val screenshots = albums.first { it.bucketId == "a" }
        assertEquals("Screenshots", screenshots.name)
        assertEquals(2, screenshots.count)
    }

    @Test
    fun filterFavorites_returnsOnlyMarkedIds() {
        val all = listOf(
            photo(1, dateTakenMs = 1L),
            photo(2, dateTakenMs = 2L),
            photo(3, dateTakenMs = 3L),
        )
        val favorites = PhotoLogic.filterFavorites(all, setOf(1L, 3L))
        assertEquals(listOf(3L, 1L), favorites.map { it.id })
    }

    @Test
    fun viewerList_scopesToAlbum() {
        val all = listOf(
            photo(1, bucketId = "a", dateTakenMs = 1L),
            photo(2, bucketId = "b", dateTakenMs = 2L),
        )
        val albumOnly = PhotoLogic.viewerList(all, emptySet(), ViewerCollection.Album, "a")
        assertEquals(listOf(1L), albumOnly.map { it.id })
    }
}
