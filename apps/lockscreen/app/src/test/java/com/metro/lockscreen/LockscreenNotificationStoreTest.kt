package com.metro.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LockscreenNotificationStoreTest {
    @Before
    fun reset() {
        LockscreenNotificationStore.clear()
    }

    @Test
    fun aggregate_emptyWhenNull() {
        assertEquals(
            emptyMap<String, LockscreenNotificationStore.PackageStatus>(),
            LockscreenNotificationStore.aggregate(null as List<LockscreenNotificationStore.ActiveNotification>?),
        )
        assertEquals(
            emptyMap<String, LockscreenNotificationStore.PackageStatus>(),
            LockscreenNotificationStore.aggregate(emptyList()),
        )
    }

    @Test
    fun aggregate_countsEligibleAndSkipsOngoing() {
        val mail = active(
            packageName = "com.example.mail",
            number = 2,
            ongoing = false,
            title = "Ada",
            body = "Hello",
            postTime = 10L,
        )
        val ongoing = active(
            packageName = "com.example.mail",
            number = 1,
            ongoing = true,
            title = "ignored",
            body = null,
            postTime = 20L,
        )
        val result = LockscreenNotificationStore.aggregate(listOf(mail, ongoing))
        assertEquals(1, result.size)
        val status = result.getValue("com.example.mail")
        assertEquals(2, status.count)
        assertEquals("Ada", status.peekTitle)
        assertEquals("Hello", status.peekBody)
        assertEquals(0L, status.flipGeneration)
    }

    @Test
    fun onNotificationPosted_bumpsFlipGenerationForEligiblePackage() {
        val posted = active(
            packageName = "com.example.chat",
            number = 1,
            ongoing = false,
            title = "Sam",
            body = "Ping",
            postTime = 5L,
        )
        LockscreenNotificationStore.onNotificationPostedMapped(posted, listOf(posted))
        val status = LockscreenNotificationStore.statusFor("com.example.chat")
        assertEquals(1, status.count)
        assertEquals("Sam", status.peekTitle)
        assertEquals("Ping", status.peekBody)
        assertTrue(status.flipGeneration > 0L)

        val before = status.flipGeneration
        val newer = active(
            packageName = "com.example.chat",
            number = 1,
            ongoing = false,
            title = "Sam",
            body = "Pong",
            postTime = 6L,
        )
        LockscreenNotificationStore.onNotificationPostedMapped(newer, listOf(posted, newer))
        val after = LockscreenNotificationStore.statusFor("com.example.chat")
        assertTrue(after.flipGeneration > before)
        assertEquals("Pong", after.peekBody)
    }

    @Test
    fun replaceAll_preservesFlipGeneration() {
        val posted = active(
            packageName = "com.example.mail",
            number = 1,
            ongoing = false,
            title = "Ada",
            body = "Hi",
            postTime = 1L,
        )
        LockscreenNotificationStore.onNotificationPostedMapped(posted, listOf(posted))
        val gen = LockscreenNotificationStore.flipGenerationFor("com.example.mail")
        assertTrue(gen > 0L)

        LockscreenNotificationStore.replaceAllMapped(listOf(posted))
        assertEquals(gen, LockscreenNotificationStore.flipGenerationFor("com.example.mail"))
    }

    private fun active(
        packageName: String,
        number: Int,
        ongoing: Boolean,
        title: String,
        body: String?,
        postTime: Long,
        groupSummary: Boolean = false,
    ) = LockscreenNotificationStore.ActiveNotification(
        packageName = packageName,
        number = number,
        ongoing = ongoing,
        groupSummary = groupSummary,
        postTime = postTime,
        title = title,
        body = body,
    )
}
