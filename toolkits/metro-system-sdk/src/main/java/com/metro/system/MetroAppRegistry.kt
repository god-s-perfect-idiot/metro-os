package com.metro.system

/**
 * Known Metro suite apps — labels and optional strong tile brands for surfaces that
 * render apps before they are installed (launcher tiles, app list, placeholders).
 *
 * Start / app-list tile fills follow the system accent for Metro and Android system
 * apps unless [Entry.strongBrandHex] is set (rare WP-style fixed brand tiles).
 */
object MetroAppRegistry {
    data class Entry(
        val label: String,
        /** Legacy catalog color; not used for Start tiles unless also [strongBrandHex]. */
        val brandHex: String,
        /** When non-null, Start/app-list use this fill instead of the system accent. */
        val strongBrandHex: String? = null,
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
        "com.metro.people" to Entry("People", "#D34829"),
        "com.metro.dialer" to Entry("Phone", "#0078D7"),
        "com.metro.calculator" to Entry("Calculator", "#007500"),
        "com.metro.clock" to Entry("Clock", "#0078D7"),
        "com.metro.files" to Entry("Files", "#0078D7"),
    )

    fun label(packageName: String): String? = entries[packageName]?.label

    fun brandHex(packageName: String): String? = entries[packageName]?.brandHex

    fun strongBrandHex(packageName: String): String? = entries[packageName]?.strongBrandHex

    fun isKnown(packageName: String): Boolean = packageName in entries

    fun isMetroSuite(packageName: String): Boolean =
        isKnown(packageName) || packageName.startsWith("com.metro.")
}
