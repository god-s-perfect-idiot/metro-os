package com.metro.files.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilesLogicTest {
    @Test
    fun mimeType_commonExtensions() {
        assertEquals("image/jpeg", FilesLogic.mimeTypeForName("photo.JPG"))
        assertEquals("image/heic", FilesLogic.mimeTypeForName("img.heic"))
        assertEquals("audio/mpeg", FilesLogic.mimeTypeForName("track.mp3"))
        assertEquals("audio/opus", FilesLogic.mimeTypeForName("voice.opus"))
        assertEquals("video/mp4", FilesLogic.mimeTypeForName("clip.mp4"))
        assertEquals("video/mpeg", FilesLogic.mimeTypeForName("old.mpg"))
        assertEquals("application/pdf", FilesLogic.mimeTypeForName("doc.pdf"))
        assertEquals("application/epub+zip", FilesLogic.mimeTypeForName("book.epub"))
        assertEquals("application/octet-stream", FilesLogic.mimeTypeForName("noext"))
    }

    @Test
    fun filter_keepsFolders_filtersFiles() {
        val entries = listOf(
            FileEntry("d", "Pictures", "/Pictures", FileKind.DIRECTORY),
            FileEntry("f1", "a.jpg", "/a.jpg", FileKind.FILE),
            FileEntry("f2", "a.mp3", "/a.mp3", FileKind.FILE),
            FileEntry("f3", "notes.txt", "/notes.txt", FileKind.FILE),
        )
        val pictures = FilesLogic.filterEntries(entries, FileFilter.PICTURES)
        assertEquals(2, pictures.size)
        assertTrue(pictures.any { it.name == "Pictures" })
        assertTrue(pictures.any { it.name == "a.jpg" })
        assertFalse(pictures.any { it.name == "a.mp3" })

        val docs = FilesLogic.filterEntries(entries, FileFilter.DOCUMENTS)
        assertTrue(docs.any { it.name == "notes.txt" })
        assertFalse(docs.any { it.name == "a.jpg" })
    }

    @Test
    fun sort_volumesThenDirsThenFiles_alpha() {
        val entries = listOf(
            FileEntry("f", "zeta.txt", "/z", FileKind.FILE),
            FileEntry("d", "beta", "/b", FileKind.DIRECTORY),
            FileEntry("v", "phone", "/p", FileKind.VOLUME),
            FileEntry("d2", "alpha", "/a", FileKind.DIRECTORY),
        )
        val sorted = FilesLogic.sortEntries(entries)
        assertEquals(listOf("phone", "alpha", "beta", "zeta.txt"), sorted.map { it.name })
    }

    @Test
    fun formatSize_and_subtitle() {
        assertEquals("512 B", FilesLogic.formatSize(512))
        assertEquals("1.0 KB", FilesLogic.formatSize(1024))
        assertEquals("1.5 MB", FilesLogic.formatSize((1.5 * 1024 * 1024).toLong()))

        val file = FileEntry(
            id = "f",
            name = "a.txt",
            absolutePath = "/a.txt",
            kind = FileKind.FILE,
            sizeBytes = 2048,
            modifiedEpochMs = 1_400_000_000_000L,
        )
        val subtitle = FilesLogic.fileSubtitle(file, nowEpochMs = 1_400_000_000_000L)
        assertTrue(subtitle!!.contains("·"))
        assertTrue(subtitle.contains("KB"))

        val dir = FileEntry(
            id = "d",
            name = "Docs",
            absolutePath = "/Docs",
            kind = FileKind.DIRECTORY,
            childCount = 3,
        )
        assertEquals("3 items", FilesLogic.fileSubtitle(dir))
    }

    @Test
    fun iconKind_mapsExtensionsAndFolders() {
        assertEquals(
            FileIconKind.FOLDER,
            FilesLogic.iconKind(
                FileEntry("d", "Docs", "/Docs", FileKind.DIRECTORY, childCount = 4),
            ),
        )
        assertEquals(
            FileIconKind.PHONE,
            FilesLogic.iconKind(FileEntry("v", "phone", "/p", FileKind.VOLUME)),
        )
        assertEquals(
            FileIconKind.SD_CARD,
            FilesLogic.iconKind(FileEntry("v", "sd card", "/sd", FileKind.VOLUME)),
        )
        assertEquals(FileIconKind.SD_CARD, FilesLogic.volumeIconKind("sd card 2"))
        assertEquals(FileIconKind.PHONE, FilesLogic.volumeIconKind("phone"))
        assertEquals(FileIconKind.WORD, FilesLogic.iconKindForName("Report.DOCX"))
        assertEquals(FileIconKind.EXCEL, FilesLogic.iconKindForName("sheet.xlsx"))
        assertEquals(FileIconKind.POWERPOINT, FilesLogic.iconKindForName("deck.pptx"))
        assertEquals(FileIconKind.ONENOTE, FilesLogic.iconKindForName("notes.one"))
        assertEquals(FileIconKind.PDF, FilesLogic.iconKindForName("a.pdf"))
        assertEquals(FileIconKind.MUSIC, FilesLogic.iconKindForName("song.mp3"))
        assertEquals(FileIconKind.PICTURE, FilesLogic.iconKindForName("pic.jpg"))
        assertEquals(FileIconKind.VIDEO, FilesLogic.iconKindForName("clip.mp4"))
        assertEquals(FileIconKind.GENERIC, FilesLogic.iconKindForName("archive.zip"))
    }

    @Test
    fun folderCountLabel_capsAt99Plus() {
        assertEquals(null, FilesLogic.folderCountLabel(null))
        assertEquals("0", FilesLogic.folderCountLabel(0))
        assertEquals("24", FilesLogic.folderCountLabel(24))
        assertEquals("99", FilesLogic.folderCountLabel(99))
        assertEquals("99+", FilesLogic.folderCountLabel(100))
    }

    @Test
    fun pathDisplay_joinsSegments() {
        assertEquals("phone > Pictures > Camera", FilesLogic.pathDisplay(listOf("phone", "Pictures", "Camera")))
        assertEquals("", FilesLogic.pathDisplay(emptyList()))
    }

    @Test
    fun breadcrumbSegment_resolvesAbsolutePath() {
        val root = "/storage/emulated/0"
        val parts = listOf("Pictures", "Camera")
        assertEquals(root, FilesLogic.absolutePathForBreadcrumbSegment(root, parts, 0))
        assertEquals("$root/Pictures", FilesLogic.absolutePathForBreadcrumbSegment(root, parts, 1))
        assertEquals("$root/Pictures/Camera", FilesLogic.absolutePathForBreadcrumbSegment(root, parts, 2))
        assertEquals(null, FilesLogic.absolutePathForBreadcrumbSegment(root, parts, 3))
        assertEquals(null, FilesLogic.absolutePathForBreadcrumbSegment(root, parts, -1))
        assertEquals(root, FilesLogic.absolutePathForBreadcrumbSegment(root, emptyList(), 0))
    }

    @Test
    fun pivotIndex_roundTrip() {
        FileFilter.entries.forEach { filter ->
            assertEquals(filter, FileFilter.fromPivotIndex(filter.pivotIndex))
        }
        assertEquals(FileFilter.ALL, FileFilter.fromPivotIndex(99))
    }
}
