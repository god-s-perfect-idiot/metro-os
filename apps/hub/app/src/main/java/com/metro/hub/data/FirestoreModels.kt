package com.metro.hub.data

/**
 * Firestore Hub catalog document (collections: first-party, second-party, third-party).
 * Explore docs reference these or store a denormalized snapshot for featured UI.
 */
data class FirestoreHubApp(
    val id: String,
    val name: String,
    val packageName: String,
    val description: String,
    val versionName: String?,
    val versionCode: Int?,
    /** core | shell (first-party); free-form for others. */
    val type: String,
    val creator: String,
    val logoXml: String?,
    val logoPngBase64: String?,
    /** App tile / icon square fill as `#RRGGBB` (from launcher bg / brand). */
    val backgroundColor: String?,
    val apkName: String?,
    val apkUrl: String?,
    val releaseUrl: String?,
    val githubRepo: String?,
    val sizeBytes: Long?,
    val party: String,
)

data class FirestoreExploreEntry(
    val id: String,
    val title: String,
    val subtitle: String?,
    /** Source collection: first-party | second-party | third-party */
    val sourceCollection: String?,
    val sourceAppId: String?,
    val sortOrder: Int,
)

object HubFirestorePaths {
    const val FirstParty = "first-party"
    const val SecondParty = "second-party"
    const val ThirdParty = "third-party"
    const val Explore = "explore"
}
