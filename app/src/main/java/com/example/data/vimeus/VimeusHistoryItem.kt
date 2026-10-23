package com.example.data.vimeus

/**
 * Vimeus title played on this device. The embed player does not expose a
 * reliable playback position, so it appears in history without a minute mark.
 */
data class VimeusHistoryItem(
    val id: String,
    val type: VimeusType,
    val title: String,
    val posterUrl: String? = null,
    val year: String? = null,
    val apiEmbedUrl: String? = null,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val watchKey: String
        get() = "vimeus:${type.apiValue}:$id"

    fun toMedia(): VimeusMedia = VimeusMedia(
        id = id,
        tmdbId = id.toLongOrNull(),
        imdbId = id.takeIf { it.startsWith("tt") },
        title = title,
        poster = posterUrl,
        type = type,
        year = year,
        apiEmbedUrl = apiEmbedUrl
    )
}
