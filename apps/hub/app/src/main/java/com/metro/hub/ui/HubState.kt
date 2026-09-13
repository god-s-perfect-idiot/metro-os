package com.metro.hub.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.hub.data.ApkIconResolver
import com.metro.hub.data.ApkInstaller
import com.metro.hub.data.FirestoreExploreEntry
import com.metro.hub.data.FirestoreHubRepository
import com.metro.hub.data.GitHubRelease
import com.metro.hub.data.GitHubReleaseClient
import com.metro.hub.data.HubAppCategory
import com.metro.hub.data.HubFirestorePaths
import com.metro.hub.data.ReleaseApkAsset
import com.metro.hub.data.toReleaseApkAsset
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HubRoute {
    Hub,
    AppList,
}

class HubState(
    context: Context,
    private val client: GitHubReleaseClient = GitHubReleaseClient(),
    private val firestore: FirestoreHubRepository = FirestoreHubRepository(),
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val iconResolver = ApkIconResolver(appContext, client)
    private var fetchJob: Job? = null
    private var downloadJob: Job? = null
    private val iconJobs = ConcurrentHashMap<String, Job>()

    var generation by mutableIntStateOf(0)
        private set

    var route by mutableStateOf(HubRoute.Hub)
        private set

    var hubPage by mutableIntStateOf(HUB_HOME)

    var release by mutableStateOf<GitHubRelease?>(null)
        private set

    /** Preferred catalog source when Firestore has first-party docs. */
    var firestoreAssets by mutableStateOf<List<ReleaseApkAsset>>(emptyList())
        private set

    var exploreEntries by mutableStateOf<List<FirestoreExploreEntry>>(emptyList())
        private set

    var catalogSource by mutableStateOf("github")
        private set

    var loadingRelease by mutableStateOf(false)
        private set

    var releaseError by mutableStateOf<String?>(null)
        private set

    var listTitle by mutableStateOf("all metro apps")
        private set

    var listFilter by mutableStateOf<HubAppCategory?>(null)
        private set

    var downloadingAssetName by mutableStateOf<String?>(null)
        private set

    var downloadError by mutableStateOf<String?>(null)
        private set

    var pendingInstallFile by mutableStateOf<File?>(null)
        private set

    /** Cached icon PNG paths keyed by APK asset name. */
    var iconPaths by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    val visibleAssets: List<ReleaseApkAsset>
        get() {
            val all = when {
                firestoreAssets.isNotEmpty() -> firestoreAssets
                else -> release?.assets.orEmpty()
            }
            val filter = listFilter ?: return all
            return all.filter { asset ->
                when (filter) {
                    HubAppCategory.Core -> asset.category == HubAppCategory.Core
                    HubAppCategory.Shell -> asset.category == HubAppCategory.Shell
                    HubAppCategory.SecondParty -> asset.category == HubAppCategory.SecondParty
                    HubAppCategory.ThirdParty -> asset.category == HubAppCategory.ThirdParty
                }
            }
        }

    fun openAllApps() {
        listTitle = "all metro apps"
        listFilter = null
        downloadError = null
        route = HubRoute.AppList
        refreshRelease()
        bump()
    }

    fun openCategory(category: HubAppCategory) {
        listTitle = category.label
        listFilter = category
        downloadError = null
        route = HubRoute.AppList
        when (category) {
            HubAppCategory.SecondParty -> loadPartyCollection(HubFirestorePaths.SecondParty)
            HubAppCategory.ThirdParty -> loadPartyCollection(HubFirestorePaths.ThirdParty)
            else -> refreshRelease()
        }
        bump()
    }

    fun closeAppList() {
        route = HubRoute.Hub
        downloadError = null
        bump()
    }

    fun ensureReleaseLoaded(force: Boolean = false) {
        if (!force && ((firestoreAssets.isNotEmpty() || release != null) || loadingRelease)) return
        fetchJob?.cancel()
        loadingRelease = true
        releaseError = null
        bump()
        fetchJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { loadCatalog() }
            }
            result.onSuccess {
                releaseError = null
                prefetchVisibleIcons(visibleAssets)
            }.onFailure { err ->
                if (firestoreAssets.isEmpty() && release == null) {
                    releaseError = err.message ?: "Could not load the app catalog."
                }
            }
            loadingRelease = false
            bump()
        }
    }

    fun refreshRelease() {
        ensureReleaseLoaded(force = true)
    }

    fun onForeground() {
        refreshRelease()
    }

    fun iconPathFor(asset: ReleaseApkAsset): String? = iconPaths[asset.name]

    fun ensureIcon(asset: ReleaseApkAsset) {
        if (!asset.logoXml.isNullOrBlank() || !asset.logoPngBase64.isNullOrBlank()) return
        if (iconPaths.containsKey(asset.name)) return
        if (asset.iconUrl != null) return
        val cached = iconResolver.cachedIconFile(asset)
        if (cached.exists() && cached.length() > 0L) {
            iconPaths = iconPaths + (asset.name to cached.absolutePath)
            bump()
            return
        }
        if (asset.downloadUrl.isBlank()) return
        if (iconJobs.containsKey(asset.name)) return
        iconJobs[asset.name] = scope.launch {
            val file = runCatching { iconResolver.resolveIconFile(asset) }.getOrNull()
            if (file != null) {
                iconPaths = iconPaths + (asset.name to file.absolutePath)
                val apkVersion = runCatching { iconResolver.readVersionFromApk(asset) }.getOrNull()
                if (!apkVersion.isNullOrBlank()) {
                    patchAsset(asset.name) { it.copy(versionName = apkVersion) }
                }
                bump()
            }
            iconJobs.remove(asset.name)
        }
    }

    fun downloadAndInstall(asset: ReleaseApkAsset) {
        if (asset.downloadUrl.isBlank()) {
            downloadError = "No download URL for ${asset.displayName}."
            bump()
            return
        }
        downloadJob?.cancel()
        downloadingAssetName = asset.name
        downloadError = null
        pendingInstallFile = null
        bump()
        downloadJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dest = File(ApkInstaller.apkCacheDir(appContext), asset.name)
                    client.downloadToFile(asset.downloadUrl, dest)
                    dest
                }
            }
            result.onSuccess { file ->
                pendingInstallFile = file
                downloadError = null
                ensureIcon(asset)
            }.onFailure {
                pendingInstallFile = null
                downloadError = it.message ?: "Download failed."
            }
            downloadingAssetName = null
            bump()
        }
    }

    fun consumePendingInstall(): File? {
        val file = pendingInstallFile
        pendingInstallFile = null
        bump()
        return file
    }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }
    }

    private fun loadPartyCollection(collection: String) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { firestore.fetchCollection(collection) }
            }.onSuccess { docs ->
                firestoreAssets = docs.map { it.toReleaseApkAsset() }
                catalogSource = "firestore:$collection"
                bump()
            }
        }
    }

    private suspend fun loadCatalog() = coroutineScope {
        val fsFirst = async {
            runCatching { firestore.fetchCollection(HubFirestorePaths.FirstParty) }.getOrDefault(emptyList())
        }
        val fsSecond = async {
            runCatching { firestore.fetchCollection(HubFirestorePaths.SecondParty) }.getOrDefault(emptyList())
        }
        val fsThird = async {
            runCatching { firestore.fetchCollection(HubFirestorePaths.ThirdParty) }.getOrDefault(emptyList())
        }
        val fsExplore = async {
            runCatching { firestore.fetchExplore() }.getOrDefault(emptyList())
        }
        val github = async {
            runCatching { client.fetchLatestRelease() }.getOrNull()
        }

        val first = fsFirst.await()
        val second = fsSecond.await()
        val third = fsThird.await()
        exploreEntries = fsExplore.await()
        val gh = github.await()

        if (first.isNotEmpty()) {
            val byApk = gh?.assets?.associateBy { it.name }.orEmpty()
            firestoreAssets = (first + second + third).map { doc ->
                val mapped = doc.toReleaseApkAsset()
                val remote = byApk[mapped.name]
                if (remote != null && mapped.downloadUrl.isBlank()) {
                    mapped.copy(
                        downloadUrl = remote.downloadUrl,
                        sizeBytes = mapped.sizeBytes.takeIf { it > 0 } ?: remote.sizeBytes,
                    )
                } else if (remote != null && (mapped.versionName.isNullOrBlank())) {
                    mapped.copy(versionName = remote.versionName ?: mapped.versionName)
                } else {
                    mapped
                }
            }
            release = gh
            catalogSource = "firestore"
        } else if (gh != null) {
            firestoreAssets = emptyList()
            release = gh
            catalogSource = "github"
        } else {
            error("Firestore empty and GitHub release unavailable")
        }
    }

    private fun prefetchVisibleIcons(assets: List<ReleaseApkAsset>) {
        assets.forEach { asset ->
            val cached = iconResolver.cachedIconFile(asset)
            if (cached.exists() && cached.length() > 0L) {
                iconPaths = iconPaths + (asset.name to cached.absolutePath)
            }
        }
        bump()
    }

    private fun patchAsset(name: String, transform: (ReleaseApkAsset) -> ReleaseApkAsset) {
        if (firestoreAssets.isNotEmpty()) {
            firestoreAssets = firestoreAssets.map { asset ->
                if (asset.name == name) transform(asset) else asset
            }
        }
        val current = release ?: return
        release = current.copy(
            assets = current.assets.map { asset ->
                if (asset.name == name) transform(asset) else asset
            },
        )
    }

    private fun bump() {
        generation++
    }

    companion object {
        const val HUB_HOME = 0
        const val HUB_APPS = 1
        const val GITHUB_URL = "https://github.com/god-s-perfect-idiot/metro-os"
    }
}
