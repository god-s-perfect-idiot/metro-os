package com.metro.system

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle

/**
 * Cross-app live PSS probe. Each suite app hosts [MetroMemoryProvider] via the SDK manifest
 * merge (`{applicationId}.metro.memory`). Feedback (and others) call [readPssBytes].
 */
object MetroMemoryContract {
    const val METHOD_GET_PSS = "getPss"
    const val EXTRA_PSS_BYTES = "pss_bytes"
    const val EXTRA_PSS_KB = "pss_kb"
    const val EXTRA_PACKAGE = "package"

    fun authorityFor(packageName: String): String = "$packageName.metro.memory"

    fun uriFor(packageName: String): Uri =
        Uri.parse("content://${authorityFor(packageName)}/memory")

    fun hasProvider(packageManager: PackageManager, packageName: String): Boolean {
        return packageManager.resolveContentProvider(authorityFor(packageName), 0) != null
    }

    /**
     * Returns total PSS in bytes for [packageName], or null if the app has no provider /
     * is not queryable. Calling this may briefly start the target process.
     */
    fun readPssBytes(resolver: ContentResolver, packageName: String): Long? {
        return try {
            val result: Bundle? = resolver.call(uriFor(packageName), METHOD_GET_PSS, null, null)
            val bytes = result?.getLong(EXTRA_PSS_BYTES, -1L) ?: -1L
            bytes.takeIf { it >= 0L }
        } catch (_: Exception) {
            null
        }
    }
}
