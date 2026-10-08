package eu.kanade.tachiyomi.data.updater

import dev.zacsweers.metro.Inject
import tachiyomi.domain.release.interactor.GetApplicationRelease

/**
 * App OTA / GitHub release checking is permanently disabled for Metron.
 * Updates ship via metro-os Hub releases — do not poll mihonapp/mihon.
 */
@Inject
class AppUpdateChecker {
    suspend fun checkForUpdate(forceCheck: Boolean = false): GetApplicationRelease.Result {
        return GetApplicationRelease.Result.NoNewUpdate
    }
}

/** Kept for About "what's new" / docs; points at the suite repo, not Mihon. */
const val GITHUB_REPO: String = "god-s-perfect-idiot/metro-os"

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases"
