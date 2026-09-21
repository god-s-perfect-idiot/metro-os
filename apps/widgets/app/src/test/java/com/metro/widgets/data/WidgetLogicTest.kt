package com.metro.widgets.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WidgetLogicTest {
    @Test
    fun batteryDigitsLabel_clampsWithoutPercent() {
        assertEquals("0", WidgetFormatters.batteryDigitsLabel(-5))
        assertEquals("100", WidgetFormatters.batteryDigitsLabel(150))
        assertEquals("78", WidgetFormatters.batteryDigitsLabel(78))
        assertEquals("78%", WidgetFormatters.batteryPercentLabel(78))
    }

    @Test
    fun bytesToGbLabel_formatsSmallAndLarge() {
        val oneGb = 1024L * 1024L * 1024L
        assertEquals("1.0 GB", WidgetFormatters.bytesToGbLabel(oneGb))
        assertEquals("2.5 GB", WidgetFormatters.bytesToGbLabel((oneGb * 2.5).toLong()))
        assertTrue(WidgetFormatters.bytesToGbLabel(oneGb * 32).endsWith(" GB"))
        assertEquals("32 GB", WidgetFormatters.bytesToGbLabel(oneGb * 32))
    }

    @Test
    fun storageLines_includeUnits() {
        val volume = StorageVolumeSnapshot(
            label = "phone",
            freeBytes = 1024L * 1024L * 1024L * 4,
            totalBytes = 1024L * 1024L * 1024L * 16,
        )
        assertEquals("4.0 GB free", WidgetFormatters.storageFreeLine(volume))
        assertEquals("12 GB used", WidgetFormatters.storageUsedLine(volume))
    }

    @Test
    fun catalog_usesFourColumnsAndExpectedFootprints() {
        assertEquals(4, WidgetCatalog.COLUMNS)
        assertEquals(WidgetTileSize.TwoByFour, WidgetKind.Time.size)
        assertEquals(WidgetTileSize.OneByOne, WidgetKind.Battery.size)
        assertEquals(WidgetTileSize.TwoByTwo, WidgetKind.Notifier.size)
        assertEquals(WidgetTileSize.TwoByFour, WidgetKind.StorageSense.size)
        assertEquals(false, WidgetKind.Time.showTitle)
        assertEquals(false, WidgetKind.Battery.showTitle)
        assertEquals(false, WidgetKind.Notifier.showTitle)
    }

    @Test
    fun notifierQueue_newestFirstDistinct() {
        fun peek(
            title: String,
            body: String?,
            post: Long,
            pkg: String = "com.example.app",
        ) = NotifierPeekLines(
            title = title,
            subtitle = null,
            body = body,
            appLabel = "Example",
            packageName = pkg,
            postTimeMs = post,
        )
        val queue = NotifierPeekLogic.buildTrayQueue(
            listOf(
                peek("A", "one", 1L),
                peek("B", "two", 3L),
                peek("A", "one", 2L),
                peek("C", null, 4L),
            ),
        )
        assertEquals(listOf("C", "B", "A"), queue.map { it.title })
    }

    @Test
    fun notifierPeek_normalizedPromotesBody() {
        val normalized = NotifierPeekLines(
            title = null,
            subtitle = null,
            body = "Only body",
            appLabel = "Mail",
            packageName = "com.metro.mail",
            postTimeMs = 1L,
        ).normalizedForFlip()
        assertEquals("Only body", normalized.title)
        assertTrue(normalized.hasContent)
    }

    @Test
    fun timeFace_formatsTwelveHourWithPeriod() {
        val afternoon = LocalDateTime.of(2024, 6, 15, 14, 5)
        val parts = TimeFaceLogic.parts(afternoon)
        assertEquals("2", parts.hour)
        assertEquals("05", parts.minute)
        assertEquals("PM", parts.period)

        val morning = LocalDateTime.of(2024, 6, 15, 10, 46)
        val am = TimeFaceLogic.parts(morning)
        assertEquals("10", am.hour)
        assertEquals("46", am.minute)
        assertEquals("AM", am.period)
    }

    @Test
    fun batterySnapshot_lowAtTwenty() {
        assertTrue(BatterySnapshot(percent = 20, charging = false).isLow)
        assertEquals(0.2f, BatterySnapshot(percent = 20, charging = false).fraction, 0.001f)
    }
}
