package com.metro.files.data

import java.util.Locale

enum class FileFilter(val pivotIndex: Int) {
    ALL(0),
    DOCUMENTS(1),
    MUSIC(2),
    PICTURES(3),
    VIDEOS(4),
    ;

    companion object {
        val PIVOT_TITLES = listOf("all", "documents", "music", "pictures", "videos")

        fun fromPivotIndex(index: Int): FileFilter =
            entries.firstOrNull { it.pivotIndex == index } ?: ALL
    }
}

enum class FileKind {
    VOLUME,
    DIRECTORY,
    FILE,
}

/**
 * Visual tile kind for list leading icons (WP8.1 Files-style).
 * Volumes use accent tiles with phone / SD glyphs; folders use accent + count;
 * files use a gray document tile + type badge.
 */
enum class FileIconKind {
    PHONE,
    SD_CARD,
    FOLDER,
    WORD,
    EXCEL,
    POWERPOINT,
    ONENOTE,
    PDF,
    TEXT,
    MUSIC,
    PICTURE,
    VIDEO,
    GENERIC,
}

data class FileEntry(
    val id: String,
    val name: String,
    val absolutePath: String,
    val kind: FileKind,
    val sizeBytes: Long = 0L,
    val modifiedEpochMs: Long = 0L,
    val childCount: Int? = null,
    val mimeType: String? = null,
)

data class FolderPathState(
    /** Absolute path of the folder being listed, or null for the volume root picker. */
    val absolutePath: String? = null,
    val displaySegments: List<String> = emptyList(),
)

object FilesLogic {
    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv",
        "odt", "ods", "odp", "html", "htm", "xml", "json", "md", "epub",
    )
    private val MUSIC_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "wma", "opus", "amr",
    )
    private val PICTURE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "tif", "tiff",
    )
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "webm", "3gp", "m4v", "mpeg", "mpg",
    )

    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.lastIndex) return ""
        return name.substring(dot + 1).lowercase(Locale.US)
    }

    fun iconKind(entry: FileEntry): FileIconKind {
        return when (entry.kind) {
            FileKind.VOLUME -> volumeIconKind(entry.name)
            FileKind.DIRECTORY -> FileIconKind.FOLDER
            FileKind.FILE -> iconKindForName(entry.name)
        }
    }

    /** Root volume tiles: phone storage vs SD card (WP8.1 Files). */
    fun volumeIconKind(name: String): FileIconKind {
        val normalized = name.lowercase(Locale.US)
        return if (normalized.startsWith("sd")) FileIconKind.SD_CARD else FileIconKind.PHONE
    }

    fun iconKindForName(name: String): FileIconKind {
        return when (extensionOf(name)) {
            "doc", "docx" -> FileIconKind.WORD
            "xls", "xlsx", "csv" -> FileIconKind.EXCEL
            "ppt", "pptx" -> FileIconKind.POWERPOINT
            "one" -> FileIconKind.ONENOTE
            "pdf" -> FileIconKind.PDF
            "txt", "rtf", "md", "log" -> FileIconKind.TEXT
            in MUSIC_EXTENSIONS -> FileIconKind.MUSIC
            in PICTURE_EXTENSIONS -> FileIconKind.PICTURE
            in VIDEO_EXTENSIONS -> FileIconKind.VIDEO
            else -> FileIconKind.GENERIC
        }
    }

    /** Cap folder badge like WP tiles: naked numeral, `99+` max. */
    fun folderCountLabel(count: Int?): String? {
        if (count == null || count < 0) return null
        return if (count > 99) "99+" else count.toString()
    }

    fun mimeTypeForName(name: String): String {
        return when (extensionOf(name)) {
            "pdf" -> "application/pdf"
            "txt", "md", "csv", "log" -> "text/plain"
            "rtf" -> "application/rtf"
            "html", "htm" -> "text/html"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "epub" -> "application/epub+zip"
            "odt" -> "application/vnd.oasis.opendocument.text"
            "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
            "odp" -> "application/vnd.oasis.opendocument.presentation"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "tif", "tiff" -> "image/tiff"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg", "oga" -> "audio/ogg"
            "wma" -> "audio/x-ms-wma"
            "opus" -> "audio/opus"
            "amr" -> "audio/amr"
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "3gp" -> "video/3gpp"
            "mpeg", "mpg" -> "video/mpeg"
            else -> "application/octet-stream"
        }
    }

    fun matchesFilter(name: String, kind: FileKind, filter: FileFilter): Boolean {
        if (kind == FileKind.VOLUME || kind == FileKind.DIRECTORY) return true
        if (filter == FileFilter.ALL) return true
        val ext = extensionOf(name)
        return when (filter) {
            FileFilter.ALL -> true
            FileFilter.DOCUMENTS -> ext in DOCUMENT_EXTENSIONS
            FileFilter.MUSIC -> ext in MUSIC_EXTENSIONS
            FileFilter.PICTURES -> ext in PICTURE_EXTENSIONS
            FileFilter.VIDEOS -> ext in VIDEO_EXTENSIONS
        }
    }

    fun filterEntries(entries: List<FileEntry>, filter: FileFilter): List<FileEntry> {
        return entries.filter { matchesFilter(it.name, it.kind, filter) }
    }

    fun sortEntries(entries: List<FileEntry>): List<FileEntry> {
        return entries.sortedWith(
            compareBy<FileEntry> {
                when (it.kind) {
                    FileKind.VOLUME -> 0
                    FileKind.DIRECTORY -> 1
                    FileKind.FILE -> 2
                }
            }.thenBy { it.name.lowercase(Locale.US) },
        )
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024.0 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return if (unit == 0) {
            "${bytes.toInt()} ${units[unit]}"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[unit])
        }
    }

    fun formatModified(epochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): String {
        if (epochMs <= 0L) return ""
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        val now = java.util.Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        return if (year == now.get(java.util.Calendar.YEAR)) {
            "$month/$day"
        } else {
            "$month/$day/$year"
        }
    }

    fun fileSubtitle(entry: FileEntry, nowEpochMs: Long = System.currentTimeMillis()): String? {
        return when (entry.kind) {
            FileKind.VOLUME -> null
            FileKind.DIRECTORY -> {
                val count = entry.childCount ?: return null
                if (count == 1) "1 item" else "$count items"
            }
            FileKind.FILE -> {
                val date = formatModified(entry.modifiedEpochMs, nowEpochMs)
                val size = formatSize(entry.sizeBytes)
                when {
                    date.isEmpty() -> size
                    else -> "$date · $size"
                }
            }
        }
    }

    fun pathDisplay(segments: List<String>): String {
        return segments.joinToString(" > ")
    }

    /**
     * Absolute path for breadcrumb segment [segmentIndex].
     * Segment 0 is the volume root; 1..n are folders under that root.
     */
    fun absolutePathForBreadcrumbSegment(
        volumeRootPath: String,
        relativeParts: List<String>,
        segmentIndex: Int,
    ): String? {
        if (segmentIndex < 0 || segmentIndex > relativeParts.size) return null
        if (segmentIndex == 0) return volumeRootPath
        val joined = relativeParts.take(segmentIndex).joinToString("/")
        return if (volumeRootPath.endsWith("/")) {
            volumeRootPath + joined
        } else {
            "$volumeRootPath/$joined"
        }
    }
}
