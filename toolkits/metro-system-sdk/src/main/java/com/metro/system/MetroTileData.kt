package com.metro.system

/**
 * Live tile payload exported by pin-capable Metro apps via [MetroTileContract].
 */
data class MetroTileData(
    val title: String,
    val backgroundColorHex: String,
    val counter: Int? = null,
    val iconUri: String? = null,
    val imageUri: String? = null,
    val backFaceTitle: String? = null,
    val backFaceImageUri: String? = null,
    val deepLinkUri: String? = null,
    val photoGrid: MetroTilePhotoGrid? = null,
    val agenda: MetroTileAgenda? = null,
) {
    val hasFlipFace: Boolean
        get() = !backFaceTitle.isNullOrBlank() || !backFaceImageUri.isNullOrBlank()

    val hasPhotoGrid: Boolean
        get() = photoGrid?.hasContent == true

    val hasAgenda: Boolean
        get() = agenda?.hasContent == true
}
