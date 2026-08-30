package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroConnectedAppsTest {
    @Test
    fun encode_sortsDistinctAndTrims() {
        assertEquals(
            "com.a,com.b",
            MetroConnectedApps.encode(listOf(" com.b ", "com.a", "com.a", "")),
        )
    }

    @Test
    fun decode_emptyAndNull() {
        assertTrue(MetroConnectedApps.decode(null).isEmpty())
        assertTrue(MetroConnectedApps.decode("").isEmpty())
        assertTrue(MetroConnectedApps.decode("  , ").isEmpty())
    }

    @Test
    fun decode_splitsPackages() {
        assertEquals(
            setOf("com.metro.music", "com.spotify.music"),
            MetroConnectedApps.decode("com.metro.music, com.spotify.music"),
        )
    }

    @Test
    fun galleryPackagesOrDefault_nullUsesDefaults() {
        assertEquals(
            MetroConnectedApps.DEFAULT_GALLERY_PACKAGES,
            MetroConnectedApps.galleryPackagesOrDefault(null),
        )
        assertTrue(MetroConnectedApps.galleryPackagesOrDefault("").isEmpty())
        assertEquals(
            setOf("com.metro.photos"),
            MetroConnectedApps.galleryPackagesOrDefault("com.metro.photos"),
        )
    }

    @Test
    fun musicPackagesOrDefault_nullUsesDefaults() {
        assertEquals(
            MetroConnectedApps.DEFAULT_MUSIC_PACKAGES,
            MetroConnectedApps.musicPackagesOrDefault(null),
        )
        assertTrue(MetroConnectedApps.musicPackagesOrDefault("").isEmpty())
    }
}
