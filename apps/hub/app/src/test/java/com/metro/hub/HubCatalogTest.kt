package com.metro.hub

import com.metro.hub.data.FirestoreHubApp
import com.metro.hub.data.GitHubReleaseClient
import com.metro.hub.data.HubAppCatalog
import com.metro.hub.data.HubAppCategory
import com.metro.hub.data.ReleaseApkAsset
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
        assertNull(asset.githubRepo)
    }

    @Test
    fun firestoreAppMapsGithubRepoOntoAsset() {
        val app = FirestoreHubApp(
            id = "wordle",
            name = "Wordle",
            packageName = "com.example.wordle",
            description = "Game",
            versionName = "1.0.0",
            versionCode = 1,
            type = "core",
            creator = "Entropy",
            logoXml = null,
            logoPngBase64 = null,
            backgroundColor = "#1BA1E2",
            apkName = "wordle-debug.apk",
            apkUrl = "https://example.com/wordle-debug.apk",
            releaseUrl = null,
            githubRepo = "https://github.com/god-s-perfect-idiot/metro-wordle",
            sizeBytes = 1L,
            party = "second",
        )
        assertEquals(
            "https://github.com/god-s-perfect-idiot/metro-wordle",
            app.toReleaseApkAsset().githubRepo,
        )
    }

    @Test
    fun firestoreAppFallsBackToRegistryBrandWhenBackgroundMissing() {
        val app = FirestoreHubApp(
            id = "music",
            name = "Music",
            packageName = "com.metro.music",
            description = "Player",
            versionName = "1.0.0",
            versionCode = 1,
            type = "core",
            creator = "Entropy",
            logoXml = null,
            logoPngBase64 = null,
            backgroundColor = null,
            apkName = "music-debug.apk",
            apkUrl = "https://example.com/music-debug.apk",
            releaseUrl = null,
            githubRepo = null,
            sizeBytes = 1L,
            party = "first",
        )
        assertEquals("#E3008C", app.toReleaseApkAsset().backgroundColor)
    }

    @Test
    fun filterByQueryMatchesNameAndDescription() {
        val music = ReleaseApkAsset(
            name = "music-debug.apk",
            displayName = "Music",
            downloadUrl = "https://example.com/music-debug.apk",
            sizeBytes = 1L,
            category = HubAppCategory.Core,
            packageName = "com.metro.music",
            description = "Xbox Music–style player with local and streaming library.",
            publisher = "Entropy",
        )
        val launcher = ReleaseApkAsset(
            name = "launcher-debug.apk",
            displayName = "Launcher",
            downloadUrl = "https://example.com/launcher-debug.apk",
            sizeBytes = 1L,
            category = HubAppCategory.Shell,
            packageName = "com.metro.launcher",
            description = "Start screen, live tiles, and app list for metro-os.",
            publisher = "Entropy",
        )
        val assets = listOf(music, launcher)

        assertEquals(emptyList<ReleaseApkAsset>(), HubAppCatalog.filterByQuery(assets, "  "))
        assertEquals(listOf(music), HubAppCatalog.filterByQuery(assets, "music"))
        assertEquals(listOf(music), HubAppCatalog.filterByQuery(assets, "STREAMING"))
        assertEquals(listOf(launcher), HubAppCatalog.filterByQuery(assets, "live tiles"))
        assertEquals(listOf(music, launcher), HubAppCatalog.filterByQuery(assets, "entropy"))
        assertTrue(HubAppCatalog.filterByQuery(assets, "zzzz").isEmpty())
    }
}
