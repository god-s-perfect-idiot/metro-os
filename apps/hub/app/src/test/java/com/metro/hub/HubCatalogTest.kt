package com.metro.hub

import com.metro.hub.data.FirestoreHubApp
import com.metro.hub.data.GitHubReleaseClient
import com.metro.hub.data.HubAppCatalog
import com.metro.hub.data.HubAppCategory
import com.metro.hub.data.toReleaseApkAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HubCatalogTest {
    @Test
    fun categorizesShellAndCoreAssets() {
        assertEquals(HubAppCategory.Shell, HubAppCatalog.categoryForAssetName("launcher-debug.apk"))
        assertEquals(HubAppCategory.Core, HubAppCatalog.categoryForAssetName("music-debug.apk"))
        assertEquals(HubAppCategory.Core, HubAppCatalog.categoryForAssetName("calculator-debug.apk"))
        assertEquals(HubAppCategory.Core, HubAppCatalog.categoryForAssetName("people-debug.apk"))
    }

    @Test
    fun displayNameStripsApkSuffix() {
        assertEquals("Launcher", HubAppCatalog.displayNameForAsset("launcher-debug.apk"))
        assertEquals("Lockscreen", HubAppCatalog.displayNameForAsset("lockscreen-release.apk"))
        assertEquals("com.metro.music", HubAppCatalog.packageNameForAsset("music-debug.apk"))
        assertTrue(HubAppCatalog.descriptionForAsset("music-debug.apk").isNotBlank())
    }

    @Test
    fun parseVersionNameFromGradle() {
        val source = """
            android {
                defaultConfig {
                    versionCode = 3
                    versionName = "1.2.0"
                }
            }
        """.trimIndent()
        assertEquals("1.2.0", GitHubReleaseClient.parseVersionName(source, "music-debug.apk"))
    }

    @Test
    fun parseCatalogMapsApkEntries() {
        val json = """
            {
              "tag": "alpha-8",
              "apps": [
                {
                  "apk": "music-debug.apk",
                  "packageName": "com.metro.music",
                  "versionName": "1.2.0",
                  "versionCode": 3,
                  "iconUrl": "https://example.com/music.png"
                }
              ]
            }
        """.trimIndent()
        val catalog = GitHubReleaseClient.parseCatalog(json)
        assertEquals("1.2.0", catalog.apps["music-debug.apk"]?.versionName)
        assertEquals("https://example.com/music.png", catalog.apps["music"]?.iconUrl)
    }

    @Test
    fun parseReleaseKeepsOnlyApksSorted() {
        val json = """
            {
              "tag_name": "alpha-8",
              "name": "alpha-8",
              "target_commitish": "main",
              "assets": [
                {
                  "name": "hub-catalog.json",
                  "browser_download_url": "https://example.com/hub-catalog.json",
                  "size": 10
                },
                {
                  "name": "notes.txt",
                  "browser_download_url": "https://example.com/notes.txt",
                  "size": 10
                },
                {
                  "name": "music-debug.apk",
                  "browser_download_url": "https://example.com/music-debug.apk",
                  "size": 100
                },
                {
                  "name": "launcher-debug.apk",
                  "browser_download_url": "https://example.com/launcher-debug.apk",
                  "size": 200
                }
              ]
            }
        """.trimIndent()
        val release = GitHubReleaseClient.parseRelease(json)
        assertEquals("alpha-8", release.tagName)
        assertEquals("https://example.com/hub-catalog.json", release.catalogUrl)
        assertEquals(2, release.assets.size)
        assertEquals("Launcher", release.assets[0].displayName)
        assertEquals("Music", release.assets[1].displayName)
        assertEquals("Entropy", release.assets[0].publisher)
        assertNull(release.assets[0].versionName)
        assertTrue(release.assets.all { it.name.endsWith(".apk") })
        assertTrue(release.assets.all { it.description.isNotBlank() })
    }

    @Test
    fun firestoreAppMapsBackgroundColorOntoAsset() {
        val app = FirestoreHubApp(
            id = "people",
            name = "People",
            packageName = "com.metro.people",
            description = "Contacts",
            versionName = "1.0.0",
            versionCode = 1,
            type = "core",
            creator = "Entropy",
            logoXml = null,
            logoPngBase64 = null,
            backgroundColor = "#D34829",
            apkName = "people-debug.apk",
            apkUrl = "https://example.com/people-debug.apk",
            releaseUrl = null,
            githubRepo = null,
            sizeBytes = 1L,
            party = "first",
        )
        val asset = app.toReleaseApkAsset()
        assertEquals("#D34829", asset.backgroundColor)
        assertEquals("People", asset.displayName)
    }
}
