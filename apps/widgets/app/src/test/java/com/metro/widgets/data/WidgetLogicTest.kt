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
    fun catalog_usesFourColumnsAndExpectedFootprints() {
        assertEquals(4, WidgetCatalog.COLUMNS)
        assertEquals(WidgetTileSize.TwoByFour, WidgetKind.Time.size)
        assertEquals(WidgetTileSize.OneByOne, WidgetKind.Battery.size)
        assertEquals(WidgetTileSize.TwoByTwo, WidgetKind.Notifier.size)
        assertEquals(WidgetTileSize.OneByOne, WidgetKind.AnalogClock.size)
        assertEquals(WidgetTileSize.OneByOne, WidgetKind.Torch.size)
        assertEquals(3, WidgetKind.AnalogClock.gridCol)
        assertEquals(2, WidgetKind.AnalogClock.gridRow)
        assertEquals(0, WidgetKind.Torch.gridCol)
        assertEquals(3, WidgetKind.Torch.gridRow)
        assertEquals(WidgetTileSize.OneByOne, WidgetKind.Lock.size)
        assertEquals(3, WidgetKind.Lock.gridCol)
        assertEquals(3, WidgetKind.Lock.gridRow)
        assertEquals(false, WidgetKind.Time.showTitle)
        assertEquals(false, WidgetKind.Battery.showTitle)
        assertEquals(false, WidgetKind.Notifier.showTitle)
        assertEquals(false, WidgetKind.AnalogClock.showTitle)
        assertEquals(false, WidgetKind.Torch.showTitle)
        assertEquals(false, WidgetKind.Lock.showTitle)
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
        assertEquals("com.example.app", queue.first().packageName)
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
    fun ignoredPackages_includeShell() {
        assertTrue(NotifierTrayStore.IgnoredPackages.contains("com.metro.launcher"))
        assertTrue(NotifierTrayStore.IgnoredPackages.contains("com.metro.widgets"))
    }

    @Test
    fun pinLogic_mapsCatalogSizesToStartStorage() {
        assertEquals("time", WidgetPinLogic.tileId(WidgetKind.Time))
        assertEquals("4x2", WidgetPinLogic.pinSizeStorageValue(WidgetKind.Time))
        assertEquals("2x2", WidgetPinLogic.pinSizeStorageValue(WidgetKind.Notifier))
        assertEquals("1x1", WidgetPinLogic.pinSizeStorageValue(WidgetKind.Battery))
        assertEquals("1x1", WidgetPinLogic.pinSizeStorageValue(WidgetKind.Torch))
        assertEquals(WidgetKind.Lock, WidgetPinLogic.kindForTileId("lock"))
        assertEquals(null, WidgetPinLogic.kindForTileId("unknown"))
    }

    @Test
    fun timeFace_formatsTwelveHourWithPeriod() {
        val afternoon = LocalDateTime.of(2024, 6, 15, 14, 5)
        val parts = TimeFaceLogic.parts(afternoon)
        assertEquals("2", parts.hour)
        assertEquals("05", parts.minute)
        assertEquals("PM", parts.period)
        assertEquals(62.5f, parts.hourHandDegrees, 0.001f)
        assertEquals(30f, parts.minuteHandDegrees, 0.001f)

        val morning = LocalDateTime.of(2024, 6, 15, 10, 46)
        val am = TimeFaceLogic.parts(morning)
        assertEquals("10", am.hour)
        assertEquals("46", am.minute)
        assertEquals("AM", am.period)
        assertEquals(323f, am.hourHandDegrees, 0.001f)
        assertEquals(276f, am.minuteHandDegrees, 0.001f)
    }

    @Test
    fun batterySnapshot_lowAtTwenty() {
        assertTrue(BatterySnapshot(percent = 20, charging = false).isLow)
        assertEquals(0.2f, BatterySnapshot(percent = 20, charging = false).fraction, 0.001f)
    }
}
