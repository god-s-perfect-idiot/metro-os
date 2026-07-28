package com.metro.files.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.files.data.FileBrowser
import com.metro.files.data.FileEntry
import com.metro.files.data.FileFilter
import com.metro.files.data.FileKind
import com.metro.files.data.FileOpener
import com.metro.files.data.FilesLogic
import com.metro.files.data.StorageAccess

class FilesState(context: Context) {
    /** Activity (or other) context for launching VIEW intents; app context for storage APIs. */
    private val launchContext = context
    private val appContext = context.applicationContext
    private val browser = FileBrowser(appContext)

    var permissionsChecked by mutableStateOf(false)
        private set
    var hasStorageAccess by mutableStateOf(false)
        private set

    var filter by mutableStateOf(FileFilter.ALL)
        private set

    /** null = volume root picker */
    var currentPath by mutableStateOf<String?>(null)
        private set

    var displaySegments by mutableStateOf<List<String>>(emptyList())
        private set

    var entries by mutableStateOf<List<FileEntry>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var openMessage by mutableStateOf<String?>(null)
        private set

    var generation by mutableIntStateOf(0)
        private set

    val atVolumeRoot: Boolean get() = currentPath == null

    fun refreshPermissions() {
        hasStorageAccess = StorageAccess.hasFullAccess(appContext)
        permissionsChecked = true
        if (hasStorageAccess) {
            reload()
        }
        bump()
    }

    fun setFilter(pivotIndex: Int) {
        val next = FileFilter.fromPivotIndex(pivotIndex)
        if (next == filter) return
        filter = next
        applyFilterToCachedListing()
        bump()
    }

    fun openEntry(entry: FileEntry) {
        openMessage = null
        when (entry.kind) {
            FileKind.VOLUME, FileKind.DIRECTORY -> {
                currentPath = entry.absolutePath
                displaySegments = browser.displaySegmentsFor(entry.absolutePath)
                reload()
            }
            FileKind.FILE -> {
                when (FileOpener.open(launchContext, entry)) {
                    FileOpener.OpenResult.Started -> Unit
                    FileOpener.OpenResult.NoHandler -> {
                        openMessage = "no app can open this file"
                    }
                    FileOpener.OpenResult.Missing -> {
                        openMessage = "file not found"
                        reload()
                    }
                    FileOpener.OpenResult.Denied -> {
                        openMessage = "could not open file"
                    }
                    FileOpener.OpenResult.NotAFile -> Unit
                }
                bump()
            }
        }
    }

    /** @return true if handled (navigated up), false if already at root */
    fun navigateUp(): Boolean {
        openMessage = null
        val path = currentPath ?: return false
        val parent = browser.parentPath(path)
        if (parent == null) {
            currentPath = null
            displaySegments = emptyList()
        } else {
            currentPath = parent
            displaySegments = browser.displaySegmentsFor(parent)
        }
        reload()
        return true
    }

    /** Jump to an ancestor (or self) via breadcrumb segment index. */
    fun navigateToSegment(segmentIndex: Int) {
        openMessage = null
        val path = currentPath ?: return
        val target = browser.absolutePathForSegment(path, segmentIndex) ?: return
        if (target == path) return
        currentPath = target
        displaySegments = browser.displaySegmentsFor(target)
        reload()
    }

    fun reload() {
        if (!hasStorageAccess) {
            entries = emptyList()
            bump()
            return
        }
        isLoading = true
        bump()
        val raw = try {
            val path = currentPath
            if (path == null) {
                browser.listVolumes()
            } else {
                browser.listDirectory(path)
            }
        } catch (_: SecurityException) {
            emptyList()
        }
        cachedRaw = raw
        entries = FilesLogic.filterEntries(raw, filter).let(FilesLogic::sortEntries)
        isLoading = false
        bump()
    }

    private var cachedRaw: List<FileEntry> = emptyList()

    private fun applyFilterToCachedListing() {
        entries = FilesLogic.filterEntries(cachedRaw, filter).let(FilesLogic::sortEntries)
    }

    private fun bump() {
        generation++
    }
}
