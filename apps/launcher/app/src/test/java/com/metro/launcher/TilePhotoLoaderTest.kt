package com.metro.launcher

import com.metro.launcher.data.TilePhotoLoader
import org.junit.Assert.assertEquals
import org.junit.Test

class TilePhotoLoaderTest {
    @Test
    fun sampleSize_downscalesToBudget() {
        assertEquals(1, TilePhotoLoader.sampleSize(1200, 800, 2048))
        assertEquals(2, TilePhotoLoader.sampleSize(4000, 3000, 2048))
        assertEquals(4, TilePhotoLoader.sampleSize(8000, 6000, 2048))
    }

    @Test
    fun sampleSize_handlesInvalidDimensions() {
        assertEquals(1, TilePhotoLoader.sampleSize(0, 100, 2048))
        assertEquals(1, TilePhotoLoader.sampleSize(100, 100, 0))
    }
}
