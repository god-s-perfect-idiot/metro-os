package com.metro.system

/**
 * Known Metro suite apps — labels and brand colors for surfaces that render
 * apps before they are installed (launcher tiles, app list, placeholders).
 */
object MetroAppRegistry {
    data class Entry(
        val label: String,
        val brandHex: String,
    )

    private val entries: Map<String, Entry> = mapOf(
        "com.metro.browser" to Entry("Internet Explorer", "#1BA1E2"),
        "com.metro.notes" to Entry("Notes", "#A200FF"),
        "com.metro.music" to Entry("Music", "#E3008C"),
        "com.metro.settings" to Entry("Settings", "#F09609"),
        "com.metro.store" to Entry("Store", "#7CB342"),
        "com.metro.photos" to Entry("Photos", "#EB3C00"),
        "com.metro.calendar" to Entry("Calendar", "#0078D7"),
        "com.metro.mail" to Entry("Mail", "#0078D7"),
        "com.metro.messaging" to Entry("Messaging", "#0078D7"),
        "com.metro.people" to Entry("People", "#0078D7"),
        "com.metro.calculator" to Entry("Calculator", "#0078D7"),
        "com.metro.clock" to Entry("Clock", "#0078D7"),
        "com.metro.files" to Entry("Files", "#0078D7"),
    )

    fun label(packageName: String): String? = entries[packageName]?.label

    fun brandHex(packageName: String): String? = entries[packageName]?.brandHex

    fun isKnown(packageName: String): Boolean = packageName in entries
}
