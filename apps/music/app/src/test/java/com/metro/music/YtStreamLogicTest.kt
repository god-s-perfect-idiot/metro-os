package com.metro.music

import com.metro.music.ytmusic.YtStreamLogic
import com.metro.music.ytmusic.YtWebStreamResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YtStreamLogicTest {
    @Test
    fun isTruncated_rejectsOneMegPreviewForLongTracks() {
        assertTrue(YtStreamLogic.isTruncated(1_048_576L, 210_000L))
        assertTrue(YtStreamLogic.isTruncated(1_048_576L, null))
    }

    @Test
    fun isTruncated_allowsShortTracksAndFullEncodes() {
        assertFalse(YtStreamLogic.isTruncated(1_048_576L, 60_000L))
        assertFalse(YtStreamLogic.isTruncated(5_000_000L, 210_000L))
        assertFalse(YtStreamLogic.isTruncated(null, 210_000L))
        assertFalse(YtStreamLogic.isTruncated(500_000L, null))
    }

    @Test
    fun ensureClen_appendsWhenMissing() {
        val url = "https://googlevideo.com/videoplayback?id=1"
        assertEquals(
            "https://googlevideo.com/videoplayback?id=1&clen=5000000",
            YtStreamLogic.ensureClen(url, 5_000_000L),
        )
    }

    @Test
    fun ensureClen_keepsExistingClen() {
        val url = "https://googlevideo.com/videoplayback?clen=9&id=1"
        assertEquals(url, YtStreamLogic.ensureClen(url, 5_000_000L))
    }

    @Test
    fun selectPlayable_skipsTruncatedAndStampsClen() {
        val selected = YtStreamLogic.selectPlayable(
            listOf(
                YtStreamLogic.AudioFormat(
                    url = "https://gv/ios",
                    bitrate = 160_000,
                    contentLength = 1_048_576L,
                    approxDurationMs = 200_000L,
                ),
                YtStreamLogic.AudioFormat(
                    url = "https://gv/vr?id=abc",
                    bitrate = 128_000,
                    contentLength = 4_000_000L,
                    approxDurationMs = 200_000L,
                ),
            ),
        )
        assertEquals("https://gv/vr?id=abc&clen=4000000", selected?.url)
        assertEquals(4_000_000L, selected?.contentLength)
    }

    @Test
    fun selectPlayable_returnsNullWhenOnlyTruncatedFormats() {
        assertNull(
            YtStreamLogic.selectPlayable(
                listOf(
                    YtStreamLogic.AudioFormat(
                        url = "https://gv/ios",
                        bitrate = 160_000,
                        contentLength = 1_048_576L,
                        approxDurationMs = 200_000L,
                    ),
                ),
            ),
        )
    }

    @Test
    fun isAudioStreamUrl_detectsGooglevideoAudio() {
        assertTrue(
            YtWebStreamResolver.isAudioStreamUrl(
                "https://rr1---sn-xx.googlevideo.com/videoplayback?itag=140&mime=audio%2Fmp4&pot=abc",
            ),
        )
        assertTrue(
            YtWebStreamResolver.isAudioStreamUrl(
                "https://rr1---sn-xx.googlevideo.com/videoplayback?itag=251&rn=1",
            ),
        )
        assertFalse(
            YtWebStreamResolver.isAudioStreamUrl(
                "https://rr1---sn-xx.googlevideo.com/videoplayback?itag=18&mime=video%2Fmp4",
            ),
        )
        assertFalse(YtWebStreamResolver.isAudioStreamUrl("https://music.youtube.com/watch?v=x"))
    }

    @Test
    fun isPotAudioStreamUrl_requiresPotParam() {
        assertTrue(
            YtWebStreamResolver.isPotAudioStreamUrl(
                "https://rr1---sn-xx.googlevideo.com/videoplayback?itag=140&mime=audio%2Fmp4&pot=abc",
            ),
        )
        assertFalse(
            YtWebStreamResolver.isPotAudioStreamUrl(
                "https://rr1---sn-xx.googlevideo.com/videoplayback?itag=140&mime=audio%2Fmp4",
            ),
        )
    }
}
