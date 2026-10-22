package com.metro.system

/**
 * Packages whose Start tiles use gallery (Photos-style) or music (Xbox Music now-playing)
 * live faces, plus packages Conversations may surface from the shade. Settings owns the
 * lists via [MetroPreferences]; launcher / Conversations read them.
 *
 * Preference value is a comma-separated package list. When a key has never been written,
 * [DEFAULT_GALLERY_PACKAGES] / [DEFAULT_MUSIC_PACKAGES] / [DEFAULT_CONVERSATION_PACKAGES]
 * apply so suite behaviour matches the pre-settings hard-coded allowlists.
 */
object MetroConnectedApps {
    val DEFAULT_GALLERY_PACKAGES: Set<String> = setOf(
        "com.metro.photos",
        "com.google.android.apps.photos",
        "com.google.android.apps.photosgo",
        "com.sec.android.gallery3d",
        "com.samsung.android.gallery3d",
        "com.android.gallery3d",
        "com.miui.gallery",
        "com.oneplus.gallery",
        "com.sonyericsson.album",
        "com.huawei.photos",
    )

    val DEFAULT_MUSIC_PACKAGES: Set<String> = setOf(
        "com.metro.music",
        "com.google.android.apps.youtube.music",
        "com.google.android.music",
        "com.spotify.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "deezer.android.app",
        "com.aspiro.tidal",
        "com.soundcloud.android",
        "com.android.music",
        "com.sec.android.app.music",
        "com.samsung.android.app.music",
    )

    /**
     * Default chat + mail packages Conversations may show. SMS / suite Messaging are
     * omitted by default; users can add them via Settings → connected apps.
     */
    val DEFAULT_CONVERSATION_PACKAGES: Set<String> = setOf(
        "com.metro.mail",
        "com.google.android.gm",
        "com.google.android.gm.lite",
        "com.google.android.apps.gmail",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thoughtcrime.securesms",
        "com.facebook.orca",
        "com.instagram.android",
        "com.discord",
        "com.slack",
        "com.microsoft.teams",
        "com.skype.raider",
        "com.viber.voip",
        "jp.naver.line.android",
        "com.snapchat.android",
        "com.google.android.apps.dynamite",
        "com.tencent.mm",
    )

    fun encode(packages: Collection<String>): String =
        packages
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .joinToString(",")

    fun decode(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** Null [raw] → defaults; blank → empty (user cleared). */
    fun galleryPackagesOrDefault(raw: String?): Set<String> =
        if (raw == null) DEFAULT_GALLERY_PACKAGES else decode(raw)

    /** Null [raw] → defaults; blank → empty (user cleared). */
    fun musicPackagesOrDefault(raw: String?): Set<String> =
        if (raw == null) DEFAULT_MUSIC_PACKAGES else decode(raw)

    /** Null [raw] → defaults; blank → empty (user cleared). */
    fun conversationPackagesOrDefault(raw: String?): Set<String> =
        if (raw == null) DEFAULT_CONVERSATION_PACKAGES else decode(raw)

}
