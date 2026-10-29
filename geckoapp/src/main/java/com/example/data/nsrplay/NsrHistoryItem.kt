package com.example.data.nsrplay

/**
 * A title played through NSRPlay. It has no position because the provider's
 * web player does not expose one, but it still belongs in "Continuar viendo"
 * so the user can reopen it from the same row as IPTV history.
 */
data class NsrHistoryItem(
    val slug: String,
    val type: NsrMediaType,
    val title: String,
    val posterUrl: String? = null,
    val year: String? = null,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val watchKey: String
        get() = "nsr:${type.apiValue}:$slug"

    fun toMedia(): NsrMedia = NsrMedia(
        id = slug,
        slug = slug,
        title = title,
        poster = posterUrl,
        rating = null,
        type = type,
        provider = null,
        url = null,
        year = year
    )
}
