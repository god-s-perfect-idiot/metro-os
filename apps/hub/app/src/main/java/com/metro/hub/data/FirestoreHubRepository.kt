package com.metro.hub.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Reads Hub catalog collections from Firestore (client cache + server).
 */
class FirestoreHubRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun fetchCollection(collection: String): List<FirestoreHubApp> {
        val snap = awaitQuery(collection)
        return snap.documents.mapNotNull { doc ->
            if (doc.id.startsWith("_")) return@mapNotNull null
            val data = doc.data ?: return@mapNotNull null
            FirestoreHubApp(
                id = doc.id,
                name = data.string("name") ?: doc.id,
                packageName = data.string("packageName") ?: "com.metro.${doc.id}",
                description = data.string("description") ?: "",
                versionName = data.string("versionName"),
                versionCode = data.intOrNull("versionCode"),
                type = data.string("type") ?: "core",
                creator = data.string("creator") ?: "Entropy",
                logoXml = data.string("logoXml"),
                logoPngBase64 = data.string("logoPngBase64"),
                backgroundColor = data.string("backgroundColor"),
                apkName = data.string("apkName"),
                apkUrl = data.string("apkUrl"),
                releaseUrl = data.string("releaseUrl"),
                githubRepo = data.string("githubRepo"),
                sizeBytes = data.longOrNull("sizeBytes"),
                party = data.string("party") ?: collection,
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun fetchExplore(): List<FirestoreExploreEntry> {
        val snap = awaitQuery(HubFirestorePaths.Explore)
        return snap.documents.mapNotNull { doc ->
            if (doc.id.startsWith("_")) return@mapNotNull null
            val data = doc.data ?: return@mapNotNull null
            FirestoreExploreEntry(
                id = doc.id,
                title = data.string("title") ?: doc.id,
                subtitle = data.string("subtitle"),
                sourceCollection = data.string("sourceCollection"),
                sourceAppId = data.string("sourceAppId"),
                sortOrder = data.intOrNull("sortOrder") ?: 0,
            )
        }.sortedBy { it.sortOrder }
    }

    private suspend fun awaitQuery(collection: String) =
        suspendCancellableCoroutine { cont ->
            db.collection(collection)
                .get(Source.DEFAULT)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}

private fun Map<String, Any?>.string(key: String): String? =
    this[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }

private fun Map<String, Any?>.intOrNull(key: String): Int? =
    when (val v = this[key]) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }

private fun Map<String, Any?>.longOrNull(key: String): Long? =
    when (val v = this[key]) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull()
        else -> null
    }

fun FirestoreHubApp.toReleaseApkAsset(): ReleaseApkAsset {
    val category = when (type.lowercase()) {
        "shell" -> HubAppCategory.Shell
        else -> when (party.lowercase()) {
            "second", "second-party" -> HubAppCategory.SecondParty
            "third", "third-party" -> HubAppCategory.ThirdParty
            else -> HubAppCategory.Core
        }
    }
    val apk = apkName ?: "$id-debug.apk"
    return ReleaseApkAsset(
        name = apk,
        displayName = name,
        downloadUrl = apkUrl.orEmpty(),
        sizeBytes = sizeBytes ?: 0L,
        category = category,
        packageName = packageName,
        description = description,
        publisher = creator,
        versionName = versionName,
        versionCode = versionCode,
        iconUrl = null,
        glyphResId = HubAppCatalog.glyphResIdForAsset(apk),
        logoXml = logoXml,
        logoPngBase64 = logoPngBase64,
        backgroundColor = backgroundColor
            ?: HubAppCatalog.backgroundColorForPackage(packageName),
        firestoreId = id,
        githubRepo = githubRepo,
    )
}
