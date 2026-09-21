package com.metro.widgets.tiles

import android.content.Context
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTilePeek
import com.metro.system.MetroTileWidgetFace
import com.metro.system.MetroTileWidgetFaceKind
import com.metro.system.MetroTileWidgetGlyph
import com.metro.widgets.data.NotifierAccess
import com.metro.widgets.data.NotifierTrayStore
import com.metro.widgets.data.WidgetKind
import com.metro.widgets.data.WidgetPinLogic
import com.metro.widgets.data.WidgetTelemetry

/**
 * Secondary-tile payloads for pinned catalog widgets.
 * Exports [MetroTileWidgetFace] + [MetroTileData.tapAction] so Start renders the face and
 * dispatches taps without launching the catalog (see metro-system-sdk AGENTS.md).
 */
class WidgetsTileDataSource(context: Context) {
    private val appContext = context.applicationContext

    fun buildTileData(tileId: String = MetroTileContract.DEFAULT_TILE_ID): MetroTileData? {
        val accentHex = MetroPreferences(appContext).accentColorHex
        if (tileId == MetroTileContract.DEFAULT_TILE_ID) {
            return MetroTileData(
                title = MetroAppRegistry.label(appContext.packageName) ?: "Widgets",
                backgroundColorHex = accentHex,
            )
        }
        val kind = WidgetPinLogic.kindForTileId(tileId) ?: return null
        return when (kind) {
            WidgetKind.Time -> MetroTileData(
                title = kind.title,
                backgroundColorHex = accentHex,
                widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.DIGITAL_CLOCK),
            )
            WidgetKind.AnalogClock -> MetroTileData(
                title = kind.title,
                backgroundColorHex = accentHex,
                widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.ANALOG_CLOCK),
            )
            WidgetKind.Battery -> {
                val battery = WidgetTelemetry.readBattery(appContext)
                MetroTileData(
                    title = kind.title,
                    backgroundColorHex = accentHex,
                    widgetFace = MetroTileWidgetFace(
                        kind = MetroTileWidgetFaceKind.BATTERY,
                        batteryPercent = battery.percent,
                    ),
                )
            }
            WidgetKind.Torch -> MetroTileData(
                title = kind.title,
                backgroundColorHex = accentHex,
                widgetFace = MetroTileWidgetFace(
                    kind = MetroTileWidgetFaceKind.GLYPH_TOGGLE,
                    glyph = MetroTileWidgetGlyph.TORCH,
                    toggleOn = WidgetTorchStore.isOn(appContext),
                    dimmed = !WidgetTorchStore.isAvailable(appContext),
                ),
                tapAction = WidgetsTileActions.ACTION_TAP,
            )
            WidgetKind.Lock -> MetroTileData(
                title = kind.title,
                backgroundColorHex = accentHex,
                widgetFace = MetroTileWidgetFace(
                    kind = MetroTileWidgetFaceKind.GLYPH,
                    glyph = MetroTileWidgetGlyph.LOCK,
                ),
                tapAction = WidgetsTileActions.ACTION_TAP,
            )
            WidgetKind.Notifier -> buildNotifierTile(kind, accentHex)
        }
    }

    private fun buildNotifierTile(kind: WidgetKind, accentHex: String): MetroTileData {
        val access = NotifierAccess.isEnabled(appContext)
        val snap = NotifierTrayStore.snapshot()
        val peeks = snap.peeks.map { peek ->
            val normalized = peek.normalizedForFlip()
            MetroTilePeek(
                title = normalized.title,
                subtitle = normalized.subtitle,
                body = normalized.body,
                footer = normalized.appLabel,
            )
        }.filter { it.hasContent }
        val idleTitle = when {
            !access -> "allow notification access"
            peeks.isEmpty() -> "no notifications"
            else -> null
        }
        return MetroTileData(
            title = kind.title,
            backgroundColorHex = accentHex,
            counter = snap.count.takeIf { it > 0 },
            widgetFace = MetroTileWidgetFace(kind = MetroTileWidgetFaceKind.PEEK_CYCLE),
            peeks = peeks.ifEmpty {
                listOfNotNull(idleTitle?.let { MetroTilePeek(title = it) })
            },
            tapAction = WidgetsTileActions.ACTION_TAP,
        )
    }
}
