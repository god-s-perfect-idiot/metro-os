package com.metro.music

import com.metro.music.ytmusic.potoken.appendStreamPoToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure helpers used by the Metrolist-aligned stream unlock path.
 * WebView cipher/n-transform need instrumentation; covered via resolve smoke on device.
 */
class YtStreamUnlockTest {
    @Test
    fun videoBoundPot_appendsPotOnly() {
        val url = "https://rr.googlevideo.com/videoplayback?id=1&n=abc"
        val out = appendStreamPoToken(url, "video+pot")
        assertTrue(out.contains("pot="))
        assertFalse(out.contains("potc="))
        assertTrue(out.contains("n=abc"))
        assertEquals(out, appendStreamPoToken(out, "ignored"))
    }

    @Test
    fun videoBoundPot_doesNotStripExistingQuery() {
        val url = "https://rr.googlevideo.com/videoplayback?itag=140&mime=audio%2Fmp4"
        val out = appendStreamPoToken(url, "xyz")
        assertTrue(out.startsWith(url))
        assertFalse(out.contains("pot=xyz&pot=xyz"))
    }
}
