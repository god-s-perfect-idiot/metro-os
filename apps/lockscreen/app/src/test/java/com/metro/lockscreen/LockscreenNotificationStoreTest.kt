package com.metro.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Test

class LockscreenNotificationStoreTest {
    @Test
    fun aggregate_emptyWhenNull() {
        assertEquals(emptyMap<String, Int>(), LockscreenNotificationStore.aggregate(null))
        assertEquals(emptyMap<String, Int>(), LockscreenNotificationStore.aggregate(emptyArray()))
    }
}
