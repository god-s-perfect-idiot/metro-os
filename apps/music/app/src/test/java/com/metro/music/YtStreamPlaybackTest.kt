package com.metro.music

import android.net.Uri
import com.metro.music.playback.YtStreamPlayback
import com.metro.music.ytmusic.potoken.appendStreamPoToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YtStreamPlaybackTest {
    @Test
    fun register_storesUserAgentForUri() {
        YtStreamPlayback.clear()
        val url = "https://rr1---sn-xx.googlevideo.com/videoplayback?id=abc123&mh=zz&clen=5000000"
        YtStreamPlayback.register(url, "com.google.ios.youtube/20.10.4")
        assertEquals(
            "com.google.ios.youtube/20.10.4",
            YtStreamPlayback.userAgentFor(Uri.parse(url)),
        )
        assertEquals(
            "com.google.ios.youtube/20.10.4",
            YtStreamPlayback.userAgentFor(
                Uri.parse("https://rr2---sn-yy.googlevideo.com/videoplayback?id=abc123&mh=other"),
            ),
        )
        assertNull(YtStreamPlayback.userAgentFor(Uri.parse("https://other.example/a")))
        YtStreamPlayback.clear()
    }

    @Test
    fun appendStreamPoToken_addsPotOnly() {
        val url = "https://gv/videoplayback?id=1"
        val out = appendStreamPoToken(url, "abc+def")
        assertTrue(out.contains("pot=abc%2Bdef"))
        assertTrue(!out.contains("potc="))
        assertEquals(out, appendStreamPoToken(out, "ignored"))
    }
}
