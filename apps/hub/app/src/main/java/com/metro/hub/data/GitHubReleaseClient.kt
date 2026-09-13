package com.metro.hub.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches the latest metro-os GitHub release and maps APK assets into [GitHubRelease].
 *
 * Per-app versionName / icons come from (in order):
 * 1. `hub-catalog.json` on the same release (preferred OTA sidecar)
 * 2. Live raw GitHub `build.gradle.kts` at the release tag
 * 3. APK PackageManager inspection after download (icons + authoritative version)
 */
class GitHubReleaseClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .cache(null)
        .build(),
    private val owner: String = "god-s-perfect-idiot",
    private val repo: String = "metro-os",
) {
    fun fetchLatestRelease(): GitHubRelease {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "metro-hub")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("GitHub release request failed: HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            var release = parseRelease(body)
            release = enrichWithCatalog(release)
            release = enrichVersionsFromGitHub(release)
            return release
        }
    }

    fun downloadToFile(downloadUrl: String, destination: java.io.File) {
        val request = Request.Builder()
            .url(downloadUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "metro-hub")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("APK download failed: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: error("Empty APK body")
            destination.parentFile?.mkdirs()
            destination.writeBytes(bytes)
        }
    }

    fun fetchText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "metro-hub")
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Request failed: HTTP ${response.code} for $url")
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun enrichWithCatalog(release: GitHubRelease): GitHubRelease {
        val catalogUrl = release.catalogUrl ?: return release
        val catalog = runCatching { parseCatalog(fetchText(catalogUrl)) }.getOrNull()
            ?: return release
        val apps = catalog.apps
        if (apps.isEmpty()) return release
        return release.copy(
            assets = release.assets.map { asset ->
                val entry = apps[asset.name] ?: apps[HubAppCatalog.assetId(asset.name)]
                if (entry == null) {
                    asset
                } else {
                    asset.copy(
                        packageName = entry.packageName ?: asset.packageName,
                        description = entry.description ?: asset.description,
                        publisher = entry.publisher ?: asset.publisher,
                        versionName = entry.versionName ?: asset.versionName,
                        versionCode = entry.versionCode ?: asset.versionCode,
                        iconUrl = entry.iconUrl ?: asset.iconUrl,
                    )
                }
            },
        )
    }

    /**
     * Pull each app's versionName from the tagged sources on GitHub when the
     * catalog did not already supply one.
     */
    private fun enrichVersionsFromGitHub(release: GitHubRelease): GitHubRelease {
        val ref = release.tagName.ifBlank { release.targetCommitish ?: "main" }
        val enriched = release.assets.map { asset ->
            if (!asset.versionName.isNullOrBlank()) return@map asset
            val path = HubAppCatalog.gradlePathForAsset(asset.name)
            val url = "https://raw.githubusercontent.com/$owner/$repo/$ref/$path"
            val version = runCatching { parseVersionName(fetchText(url), asset.name) }.getOrNull()
            if (version.isNullOrBlank()) asset else asset.copy(versionName = version)
        }
        return release.copy(assets = enriched)
    }

    companion object {
        private val VersionNameGradle =
            Regex("""versionName\s*=\s*"([^"]+)"""")
        private val VersionNameProperties =
            Regex("""(?m)^(?:projectVersionName|VERSION_NAME|versionName)\s*=\s*(\S+)""")

        fun parseRelease(json: String): GitHubRelease {
            val root = JSONObject(json)
            val tag = root.optString("tag_name").ifBlank { "unknown" }
            val name = root.optString("name").takeIf { it.isNotBlank() }
            val target = root.optString("target_commitish").takeIf { it.isNotBlank() }
            val assetsJson = root.optJSONArray("assets") ?: JSONArray()
            var catalogUrl: String? = null
            val assets = buildList {
                for (i in 0 until assetsJson.length()) {
                    val asset = assetsJson.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name")
                    val url = asset.optString("browser_download_url")
                    if (assetName.equals(HubAppCatalog.CatalogAssetName, ignoreCase = true)) {
                        catalogUrl = url.takeIf { it.isNotBlank() }
                        continue
                    }
                    if (!assetName.endsWith(".apk", ignoreCase = true)) continue
                    if (url.isBlank()) continue
                    add(
                        ReleaseApkAsset(
                            name = assetName,
                            displayName = HubAppCatalog.displayNameForAsset(assetName),
                            downloadUrl = url,
                            sizeBytes = asset.optLong("size"),
                            category = HubAppCatalog.categoryForAssetName(assetName),
                            packageName = HubAppCatalog.packageNameForAsset(assetName),
                            description = HubAppCatalog.descriptionForAsset(assetName),
                            publisher = HubAppCatalog.publisherForAsset(assetName),
                            glyphResId = HubAppCatalog.glyphResIdForAsset(assetName),
                        ),
                    )
                }
            }.sortedBy { it.displayName.lowercase() }
            return GitHubRelease(
                tagName = tag,
                name = name,
                targetCommitish = target,
                assets = assets,
                catalogUrl = catalogUrl,
            )
        }

        fun parseCatalog(json: String): HubCatalog {
            val root = JSONObject(json)
            val tag = root.optString("tag").takeIf { it.isNotBlank() }
            val appsJson = root.optJSONArray("apps") ?: JSONArray()
            val apps = buildMap {
                for (i in 0 until appsJson.length()) {
                    val app = appsJson.optJSONObject(i) ?: continue
                    val apk = app.optString("apk").ifBlank { app.optString("name") }
                    if (apk.isBlank()) continue
                    put(
                        apk,
                        HubCatalogApp(
                            packageName = app.optString("packageName").takeIf { it.isNotBlank() },
                            versionName = app.optString("versionName").takeIf { it.isNotBlank() },
                            versionCode = app.optInt("versionCode").takeIf { app.has("versionCode") && it > 0 },
                            description = app.optString("description").takeIf { it.isNotBlank() },
                            publisher = app.optString("publisher").takeIf { it.isNotBlank() },
                            iconUrl = app.optString("iconUrl").takeIf { it.isNotBlank() },
                        ),
                    )
                    val id = HubAppCatalog.assetId(apk)
                    putIfAbsent(
                        id,
                        HubCatalogApp(
                            packageName = app.optString("packageName").takeIf { it.isNotBlank() },
                            versionName = app.optString("versionName").takeIf { it.isNotBlank() },
                            versionCode = app.optInt("versionCode").takeIf { app.has("versionCode") && it > 0 },
                            description = app.optString("description").takeIf { it.isNotBlank() },
                            publisher = app.optString("publisher").takeIf { it.isNotBlank() },
                            iconUrl = app.optString("iconUrl").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
            return HubCatalog(tag = tag, apps = apps)
        }

        fun parseVersionName(source: String, assetName: String): String? {
            val id = HubAppCatalog.assetId(assetName)
            if (id == "keyboard") {
                return VersionNameProperties.find(source)?.groupValues?.getOrNull(1)
                    ?.substringBefore('-')
                    ?.takeIf { it.isNotBlank() }
            }
            return VersionNameGradle.find(source)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        }
    }
}
