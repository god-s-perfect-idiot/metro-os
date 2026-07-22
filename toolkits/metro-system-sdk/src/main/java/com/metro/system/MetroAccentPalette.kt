package com.metro.system

/**
 * Official Windows Phone 8 / 8.1 accent colours (20).
 * Sources: WP8 AccentColors XAML / community HEX tables.
 */
data class MetroAccentOption(
    val name: String,
    val hex: String,
) {
    val colorArgb: Long
        get() {
            val n = hex.removePrefix("#")
            val argb = when (n.length) {
                6 -> "FF$n"
                8 -> n
                else -> "FF1BA1E2"
            }
            return argb.toLong(16)
        }
}

object MetroAccentPalette {
    val Cyan = MetroAccentOption("cyan", "#1BA1E2")
    val Lime = MetroAccentOption("lime", "#A4C400")
    val Green = MetroAccentOption("green", "#60A917")
    val Emerald = MetroAccentOption("emerald", "#008A00")
    val Teal = MetroAccentOption("teal", "#00ABA9")
    val Cobalt = MetroAccentOption("cobalt", "#0050EF")
    val Indigo = MetroAccentOption("indigo", "#6A00FF")
    val Violet = MetroAccentOption("violet", "#AA00FF")
    val Pink = MetroAccentOption("pink", "#F472D0")
    val Magenta = MetroAccentOption("magenta", "#D80073")
    val Crimson = MetroAccentOption("crimson", "#A20025")
    val Red = MetroAccentOption("red", "#E51400")
    val Orange = MetroAccentOption("orange", "#FA6800")
    val Amber = MetroAccentOption("amber", "#F0A30A")
    val Yellow = MetroAccentOption("yellow", "#E3C800")
    val Brown = MetroAccentOption("brown", "#825A2C")
    val Olive = MetroAccentOption("olive", "#6D8764")
    val Steel = MetroAccentOption("steel", "#647687")
    val Mauve = MetroAccentOption("mauve", "#76608A")
    val Taupe = MetroAccentOption("taupe", "#87794E")

    /** Grid order matching WP8.1 accent picker (4×5). */
    val all: List<MetroAccentOption> = listOf(
        Lime, Green, Emerald, Teal,
        Cyan, Cobalt, Indigo, Violet,
        Pink, Magenta, Crimson, Red,
        Orange, Amber, Yellow, Brown,
        Olive, Steel, Mauve, Taupe,
    )

    fun findByHex(hex: String): MetroAccentOption? {
        val normalized = normalizeHex(hex) ?: return null
        return all.firstOrNull { normalizeHex(it.hex) == normalized }
    }

    fun displayName(hex: String): String =
        findByHex(hex)?.name ?: "custom"

    fun normalizeHex(hex: String): String? {
        val n = hex.trim().removePrefix("#").uppercase()
        return when (n.length) {
            6 -> "#$n"
            8 -> "#${n.substring(2)}"
            else -> null
        }
    }
}
