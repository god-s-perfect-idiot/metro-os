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
import com.metro.hub.data.FirestoreHubApp
import com.metro.hub.data.FirestoreHubRepository
import com.metro.hub.data.GitHubRelease
import com.metro.hub.data.GitHubReleaseClient
import com.metro.hub.data.HubAppCatalog
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
    AppDetail,
    Search,
    ExtrasInfo,
}

/**
 * Catalog fetch UI phase.
 *
 * - [Idle] — no in-flight fetch, or a silent refresh over an already-populated list.
 * - [Loading] — blocking wait; list should show [com.metro.ui.MetroLoadingScreen] when empty.
 *
 * Never overlay a loader on existing app rows — refresh in place with no chrome.
 */
enum class CatalogLoadMode {
    Idle,
    Loading,
}

/** Which catalog snapshot [HubState.firestoreAssets] currently represents. */
private enum class CatalogSnapshot {
    None,
    Full,
    FirstParty,
    SecondParty,
    ThirdParty,
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
    private var catalogSnapshot = CatalogSnapshot.None

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

    var catalogLoadMode by mutableStateOf(CatalogLoadMode.Idle)
        private set

    /** True while a blocking catalog fetch is in flight. */
    val loadingRelease: Boolean
        get() = catalogLoadMode == CatalogLoadMode.Loading

    var releaseError by mutableStateOf<String?>(null)
        private set

    var listTitle by mutableStateOf("")
        private set

    var listFilter by mutableStateOf<HubAppCategory?>(null)
        private set

    var searchQuery by mutableStateOf("")
        private set

    /** Where app detail returns — list or search. */
    var appDetailParent by mutableStateOf(HubRoute.AppList)
        private set

    /** Asset name for the open app detail drill-in. */
    var selectedAssetName by mutableStateOf<String?>(null)
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

    /**
     * Up to [FEATURED_COUNT] apps picked at random from the full combined catalog
     * (first + second + third party). Kept across party-only list navigations.
     */
    var featuredAssets by mutableStateOf<List<ReleaseApkAsset>>(emptyList())
        private set

    private val hasCatalogData: Boolean
        get() = firestoreAssets.isNotEmpty() || release != null

    val allAssets: List<ReleaseApkAsset>
        get() = when {
            firestoreAssets.isNotEmpty() -> firestoreAssets
            // Second/third party must not fall back to the GitHub suite list.
            catalogSnapshot == CatalogSnapshot.SecondParty ||
                catalogSnapshot == CatalogSnapshot.ThirdParty -> emptyList()
            // First-party / full catalog: GitHub suite APKs are first-party only.
            else -> release?.assets.orEmpty()
        }

    val visibleAssets: List<ReleaseApkAsset>
        get() {
            val all = allAssets
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

    /** Catalog rows matching [searchQuery] (empty query → empty results, Music explore style). */
    val searchResults: List<ReleaseApkAsset>
        get() = HubAppCatalog.filterByQuery(allAssets, searchQuery)

    val selectedAsset: ReleaseApkAsset?
        get() {
            val name = selectedAssetName ?: return null
            return allAssets.find { it.name == name }
        }

    /** Home link: first-party suite apps only (Firestore `first-party`, GitHub fallback). */
    fun openAllApps(title: String) {
        listTitle = title
        listFilter = null
        selectedAssetName = null
        downloadError = null
        route = HubRoute.AppList
        loadFirstPartyCatalog()
        bump()
    }

    fun openCategory(category: HubAppCategory, title: String? = null) {
        listTitle = title ?: category.label
        listFilter = category
        selectedAssetName = null
        downloadError = null
        route = HubRoute.AppList
        when (category) {
            HubAppCategory.SecondParty -> loadPartyCollection(HubFirestorePaths.SecondParty)
            HubAppCategory.ThirdParty -> loadPartyCollection(HubFirestorePaths.ThirdParty)
            // Core / shell are first-party subtypes — do not mix in second/third party.
            HubAppCategory.Core, HubAppCategory.Shell -> loadFirstPartyCatalog()
        }
        bump()
    }

    fun openAppDetail(asset: ReleaseApkAsset) {
        appDetailParent = when (route) {
            HubRoute.Search -> HubRoute.Search
            HubRoute.Hub -> HubRoute.Hub
            else -> HubRoute.AppList
        }
        selectedAssetName = asset.name
        downloadError = null
        route = HubRoute.AppDetail
        ensureIcon(asset)
        bump()
    }

    fun closeAppDetail() {
        route = appDetailParent
        selectedAssetName = null
        downloadError = null
        bump()
    }

    fun closeAppList() {
        route = HubRoute.Hub
        selectedAssetName = null
        downloadError = null
        bump()
    }

    fun openSearch() {
        searchQuery = ""
        selectedAssetName = null
        downloadError = null
        route = HubRoute.Search
        ensureFullCatalog()
        bump()
    }

    fun closeSearch() {
        searchQuery = ""
        route = HubRoute.Hub
        bump()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        bump()
    }

    fun openExtrasInfo() {
        selectedAssetName = null
        downloadError = null
        route = HubRoute.ExtrasInfo
        ensureReleaseLoaded()
        bump()
    }

    fun closeExtrasInfo() {
        route = HubRoute.Hub
        bump()
    }

    fun goBack() {
        when (route) {
            HubRoute.AppDetail -> closeAppDetail()
            HubRoute.AppList -> closeAppList()
            HubRoute.Search -> closeSearch()
            HubRoute.ExtrasInfo -> closeExtrasInfo()
            HubRoute.Hub -> Unit
        }
    }

    /**
     * Load the catalog once if missing. Does not flash a loader over an already-populated list.
     */
    fun ensureReleaseLoaded(force: Boolean = false) {
        if (!force && (hasCatalogData || catalogLoadMode != CatalogLoadMode.Idle)) return
        startFullCatalogFetch(
            mode = if (hasCatalogData) CatalogLoadMode.Idle else CatalogLoadMode.Loading,
        )
    }

    /** App-bar refresh — keep existing rows visible with no loader overlay. */
    fun refreshRelease() {
        startFullCatalogFetch(
            mode = if (visibleAssets.isEmpty()) {
                CatalogLoadMode.Loading
            } else {
                CatalogLoadMode.Idle
            },
        )
    }

    fun onForeground() {
        // Soft refresh: update in the background without overlaying loaders on existing UI.
        if (catalogLoadMode == CatalogLoadMode.Loading) return
        startFullCatalogFetch(mode = CatalogLoadMode.Idle)
    }

    /** Navigate to a filtered/full list using a cached full catalog when possible. */
    private fun ensureFullCatalog() {
        if (catalogSnapshot == CatalogSnapshot.Full && hasCatalogData) {
            return
        }
        if (catalogSnapshot != CatalogSnapshot.Full) {
            // Drop party-only rows so we never paint them under a full-catalog wait.
            firestoreAssets = emptyList()
            catalogSnapshot = CatalogSnapshot.None
        }
        startFullCatalogFetch(mode = CatalogLoadMode.Loading)
    }

    private fun startFullCatalogFetch(mode: CatalogLoadMode) {
        fetchJob?.cancel()
        catalogLoadMode = mode
        releaseError = null
        bump()
        fetchJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { loadCatalog() }
            }
            result.onSuccess {
                catalogSnapshot = CatalogSnapshot.Full
                releaseError = null
                refreshFeaturedPicks(force = featuredAssets.isEmpty())
                prefetchVisibleIcons(visibleAssets)
            }.onFailure { err ->
                if (firestoreAssets.isEmpty() && release == null) {
                    releaseError = err.message ?: "Could not load the app catalog."
                }
            }
            catalogLoadMode = CatalogLoadMode.Idle
            bump()
        }
    }

    fun iconPathFor(asset: ReleaseApkAsset): String? = iconPaths[asset.name]

    fun ensureIcon(asset: ReleaseApkAsset) {
        // Metro catalog tiles use Firestore bg + logo/glyph — skip adaptive APK icons.
        if (!asset.logoXml.isNullOrBlank() || !asset.logoPngBase64.isNullOrBlank()) return
        if (!asset.backgroundColor.isNullOrBlank() || asset.glyphResId != null) return
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

    /** Shares the app's GitHub link (Firestore `githubRepo`, else the suite repo). */
    fun shareApp(asset: ReleaseApkAsset) {
        val url = asset.githubRepo?.takeIf { it.isNotBlank() } ?: GITHUB_URL
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, asset.displayName)
                putExtra(Intent.EXTRA_TEXT, url)
            }
            val chooser = Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(chooser)
        }
    }

    /**
     * First-party suite only — used by `metro os apps` and core/shell quick links.
     * Prefers Firestore `first-party`; falls back to GitHub latest-release APKs.
     */
    private fun loadFirstPartyCatalog() {
        if (catalogSnapshot == CatalogSnapshot.FirstParty && hasCatalogData) {
            return
        }
        fetchJob?.cancel()
        firestoreAssets = emptyList()
        catalogSnapshot = CatalogSnapshot.FirstParty
        catalogLoadMode = CatalogLoadMode.Loading
        releaseError = null
        bump()
        fetchJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val docs = firestore.fetchCollection(HubFirestorePaths.FirstParty)
                    val gh = runCatching { client.fetchLatestRelease() }.getOrNull()
                    docs to gh
                }
            }
            result.onSuccess { (docs, gh) ->
                if (gh != null) release = gh
                if (docs.isNotEmpty()) {
                    firestoreAssets = mergeFirestoreWithGitHub(docs, gh)
                    catalogSource = "firestore:first-party"
                    releaseError = null
                } else if (gh != null) {
                    firestoreAssets = emptyList()
                    catalogSource = "github"
                    releaseError = null
                } else if (visibleAssets.isEmpty()) {
                    releaseError = "Could not load the app catalog."
                }
                prefetchVisibleIcons(visibleAssets)
            }.onFailure { err ->
                if (visibleAssets.isEmpty()) {
                    releaseError = err.message ?: "Could not load the app catalog."
                }
            }
            catalogLoadMode = CatalogLoadMode.Idle
            bump()
        }
    }

    private fun loadPartyCollection(collection: String) {
        fetchJob?.cancel()
        // Replace the list — do not keep a stale full catalog under an overlay loader.
        firestoreAssets = emptyList()
        catalogSnapshot = when (collection) {
            HubFirestorePaths.FirstParty -> CatalogSnapshot.FirstParty
            HubFirestorePaths.SecondParty -> CatalogSnapshot.SecondParty
            HubFirestorePaths.ThirdParty -> CatalogSnapshot.ThirdParty
            else -> CatalogSnapshot.None
        }
        catalogLoadMode = CatalogLoadMode.Loading
        releaseError = null
        bump()
        fetchJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { firestore.fetchCollection(collection) }
            }
            result.onSuccess { docs ->
                firestoreAssets = docs.map { it.toReleaseApkAsset() }
                catalogSource = "firestore:$collection"
                releaseError = null
            }.onFailure { err ->
                if (visibleAssets.isEmpty()) {
                    releaseError = err.message ?: "Could not load the app catalog."
                }
            }
            catalogLoadMode = CatalogLoadMode.Idle
            bump()
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
            firestoreAssets = mergeFirestoreWithGitHub(first + second + third, gh)
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

    private fun mergeFirestoreWithGitHub(
        docs: List<FirestoreHubApp>,
        gh: GitHubRelease?,
    ): List<ReleaseApkAsset> {
        val byApk = gh?.assets?.associateBy { it.name }.orEmpty()
        return docs.map { doc ->
            val mapped = doc.toReleaseApkAsset()
            val remote = byApk[mapped.name]
            when {
                remote != null && mapped.downloadUrl.isBlank() -> mapped.copy(
                    downloadUrl = remote.downloadUrl,
                    sizeBytes = mapped.sizeBytes.takeIf { it > 0 } ?: remote.sizeBytes,
                )
                remote != null && mapped.versionName.isNullOrBlank() ->
                    mapped.copy(versionName = remote.versionName ?: mapped.versionName)
                else -> mapped
            }
        }
    }

    /**
     * Pick [FEATURED_COUNT] random apps from the full combined catalog.
     * When [force] is false and picks already exist, refresh metadata only.
     */
    private fun refreshFeaturedPicks(force: Boolean) {
        val pool = when {
            firestoreAssets.isNotEmpty() -> firestoreAssets
            else -> release?.assets.orEmpty()
        }
        if (pool.isEmpty()) {
            if (force) featuredAssets = emptyList()
            return
        }
        if (!force && featuredAssets.isNotEmpty()) {
            val refreshed = featuredAssets.mapNotNull { pick ->
                pool.find { it.name == pick.name }
            }
            featuredAssets = if (refreshed.size == featuredAssets.size) {
                refreshed
            } else {
                pool.shuffled().take(FEATURED_COUNT)
            }
            return
        }
        featuredAssets = pool.shuffled().take(FEATURED_COUNT)
    }

    private fun prefetchVisibleIcons(assets: List<ReleaseApkAsset>) {
        assets.forEach { asset ->
            if (!asset.logoXml.isNullOrBlank() || !asset.logoPngBase64.isNullOrBlank()) return@forEach
            if (!asset.backgroundColor.isNullOrBlank() || asset.glyphResId != null) return@forEach
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
        const val HUB_FEATURED = 2
        const val FEATURED_COUNT = 4
        const val GITHUB_URL = "https://github.com/god-s-perfect-idiot/metro-os"
        const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/godsperfectidiot"

        fun releaseUrlForTag(tag: String): String =
            "https://github.com/god-s-perfect-idiot/metro-os/releases/tag/$tag"
    }
}
