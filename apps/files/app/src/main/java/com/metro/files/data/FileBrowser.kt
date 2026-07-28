package com.metro.files.data

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File

/**
 * Lists storage volumes and directory children for the Files browser.
 * Requires all-files / broad storage access on modern Android.
 */
class FileBrowser(private val context: Context) {
    fun listVolumes(): List<FileEntry> {
        val volumes = mutableListOf<FileEntry>()
        val primary = Environment.getExternalStorageDirectory()
        if (primary != null) {
            volumes += FileEntry(
                id = "volume:phone",
                name = "phone",
                absolutePath = primary.absolutePath,
                kind = FileKind.VOLUME,
                childCount = countChildren(primary),
            )
        }

        val secondary = secondarySharedStorageRoots()
            .filter { it.absolutePath != primary?.absolutePath }
        secondary.forEachIndexed { index, root ->
            volumes += FileEntry(
                id = "volume:sd:$index",
                name = if (secondary.size == 1) "sd card" else "sd card ${index + 1}",
                absolutePath = root.absolutePath,
                kind = FileKind.VOLUME,
                childCount = countChildren(root),
            )
        }
        return FilesLogic.sortEntries(volumes)
    }

    fun listDirectory(absolutePath: String): List<FileEntry> {
        val dir = File(absolutePath)
        if (!dir.isDirectory || !dir.canRead()) return emptyList()
        val children = dir.listFiles() ?: return emptyList()
        val entries = children.mapNotNull { child ->
            when {
                child.isDirectory -> FileEntry(
                    id = "dir:${child.absolutePath}",
                    name = child.name,
                    absolutePath = child.absolutePath,
                    kind = FileKind.DIRECTORY,
                    modifiedEpochMs = child.lastModified(),
                    childCount = countChildren(child),
                )
                child.isFile -> FileEntry(
                    id = "file:${child.absolutePath}",
                    name = child.name,
                    absolutePath = child.absolutePath,
                    kind = FileKind.FILE,
                    sizeBytes = child.length(),
                    modifiedEpochMs = child.lastModified(),
                    mimeType = FilesLogic.mimeTypeForName(child.name),
                )
                else -> null
            }
        }
        return FilesLogic.sortEntries(entries)
    }

    fun parentPath(absolutePath: String): String? {
        val parent = File(absolutePath).parentFile ?: return null
        val volumeRoots = listVolumes().map { it.absolutePath }.toSet()
        if (absolutePath in volumeRoots) return null
        return parent.absolutePath
    }

    fun volumeLabelForPath(absolutePath: String): String? {
        return listVolumes().firstOrNull { absolutePath == it.absolutePath || absolutePath.startsWith(it.absolutePath + "/") }?.name
    }

    fun displaySegmentsFor(absolutePath: String): List<String> {
        val volumes = listVolumes()
        val volume = volumes.firstOrNull {
            absolutePath == it.absolutePath || absolutePath.startsWith(it.absolutePath + "/")
        } ?: return listOf(File(absolutePath).name)
        if (absolutePath == volume.absolutePath) {
            return listOf(volume.name)
        }
        val relative = absolutePath.removePrefix(volume.absolutePath).trimStart('/')
        return listOf(volume.name) + relative.split('/').filter { it.isNotEmpty() }
    }

    /** Absolute path for breadcrumb segment [segmentIndex] under [absolutePath]'s volume. */
    fun absolutePathForSegment(absolutePath: String, segmentIndex: Int): String? {
        val volume = listVolumes().firstOrNull {
            absolutePath == it.absolutePath || absolutePath.startsWith(it.absolutePath + "/")
        } ?: return null
        val relativeParts = if (absolutePath == volume.absolutePath) {
            emptyList()
        } else {
            absolutePath
                .removePrefix(volume.absolutePath)
                .trimStart('/')
                .split('/')
                .filter { it.isNotEmpty() }
        }
        return FilesLogic.absolutePathForBreadcrumbSegment(
            volumeRootPath = volume.absolutePath,
            relativeParts = relativeParts,
            segmentIndex = segmentIndex,
        )
    }

    private fun countChildren(dir: File): Int? {
        if (!dir.isDirectory || !dir.canRead()) return null
        return try {
            dir.list()?.size
        } catch (_: SecurityException) {
            null
        }
    }

    private fun secondarySharedStorageRoots(): List<File> {
        val roots = linkedSetOf<File>()
        val sm = context.getSystemService(StorageManager::class.java)
        sm?.storageVolumes?.forEach { volume ->
            if (volume.isPrimary) return@forEach
            val dir = if (android.os.Build.VERSION.SDK_INT >= 30) {
                volume.directory
            } else {
                @Suppress("DEPRECATION")
                volume.javaClass.methods
                    .firstOrNull { it.name == "getPathFile" && it.parameterTypes.isEmpty() }
                    ?.invoke(volume) as? File
                    ?: volume.javaClass.methods
                        .firstOrNull { it.name == "getPath" && it.parameterTypes.isEmpty() }
                        ?.invoke(volume)
                        ?.let { File(it.toString()) }
            }
            if (dir != null && dir.exists() && dir.canRead()) {
                roots += dir
            }
        }

        // Fallback: app-visible external dirs often include SD card app-specific paths;
        // walk up to the shared root when possible.
        context.getExternalFilesDirs(null).orEmpty().forEach { appDir ->
            if (appDir == null) return@forEach
            var probe: File? = appDir
            repeat(6) {
                val parent = probe?.parentFile ?: return@repeat
                probe = parent
            }
            // Typical: /storage/XXXX-XXXX/Android/data/... → /storage/XXXX-XXXX
            val candidate = appDir.absolutePath
                .substringBefore("/Android/data", missingDelimiterValue = "")
                .takeIf { it.isNotEmpty() }
                ?.let { File(it) }
            if (candidate != null && candidate.exists() && candidate.canRead()) {
                roots += candidate
            }
        }
        return roots.toList()
    }
}
