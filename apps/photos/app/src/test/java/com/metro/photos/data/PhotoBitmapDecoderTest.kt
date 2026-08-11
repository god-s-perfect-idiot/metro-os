package com.metro.photos.data

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhotoBitmapDecoderTest {
    @Test
    fun sampleSize_isOneWhenAlreadyWithinBudget() {
        assertEquals(1, PhotoBitmapDecoder.sampleSize(1920, 1080, 4096))
        assertEquals(1, PhotoBitmapDecoder.sampleSize(4096, 2160, 4096))
    }

    @Test
    fun sampleSize_isPowerOfTwoUntilWithinBudget() {
        assertEquals(2, PhotoBitmapDecoder.sampleSize(8000, 6000, 4096))
        assertEquals(4, PhotoBitmapDecoder.sampleSize(16000, 9000, 4096))
    }

    @Test
    fun sampleSize_handlesInvalidDimensions() {
        assertEquals(1, PhotoBitmapDecoder.sampleSize(0, 100, 4096))
        assertEquals(1, PhotoBitmapDecoder.sampleSize(100, 100, 0))
    }

    @Test
    fun targetSize_preservesAspectAndCapsLongEdge() {
        assertEquals(4000 to 3000, PhotoBitmapDecoder.targetSize(4000, 3000, 4096))
        assertEquals(4096 to 2304, PhotoBitmapDecoder.targetSize(8000, 4500, 4096))
    }

    @Test
    fun maxDecodeEdge_usesScreenTimesMaxZoomCapped() {
        assertEquals(4096, PhotoBitmapDecoder.maxDecodeEdge(1080))
        assertEquals(3600, PhotoBitmapDecoder.maxDecodeEdge(720))
        assertEquals(PhotoBitmapDecoder.MaxEdgePx, PhotoBitmapDecoder.maxDecodeEdge(0))
    }

    @Test
    fun thumbnailBucket_roundsUpTo64() {
        assertEquals(256, PhotoBitmapDecoder.thumbnailBucket(243))
        assertEquals(512, PhotoBitmapDecoder.thumbnailBucket(486))
        assertEquals(128, PhotoBitmapDecoder.thumbnailBucket(10))
    }

    @Test
    fun decodeFull_keepsOriginalDimensionsWithinBudget() {
        val context = RuntimeEnvironment.getApplication()
        val source = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        val file = File(context.cacheDir, "viewer-full.png")
        FileOutputStream(file).use { out ->
            source.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val decoded = PhotoBitmapDecoder.decodeFull(
            context.contentResolver,
            Uri.fromFile(file),
            maxEdgePx = 4096,
        )
        assertNotNull(decoded)
        assertEquals(640, decoded!!.width)
        assertEquals(480, decoded.height)
        assertTrue(decoded.width * decoded.height > 256 * 256)
    }
}
