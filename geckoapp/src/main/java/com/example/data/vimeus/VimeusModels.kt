package com.example.data.vimeus

import com.example.data.model.ItemType
import java.net.URLEncoder

enum class VimeusType(val apiValue: String, val section: ItemType, val embedPath: String) {
    MOVIE("movie", ItemType.MOVIE, "/e/movie"),
    SERIES("series", ItemType.SERIES, "/e/serie"),
    ANIME("anime", ItemType.SERIES, "/e/anime");

    companion object {
        fun fromApi(value: String?): VimeusType = when (value?.lowercase()) {
            "series", "serie" -> SERIES
            "anime", "animes" -> ANIME
            else -> MOVIE
        }
    }
}

data class VimeusMedia(
    val id: String,
    val tmdbId: Long?,
    val imdbId: String?,
    val title: String,
    val poster: String?,
    val backdrop: String? = null,
    val type: VimeusType,
    val totalSeasons: Int = 0,
    val totalEpisodes: Int = 0,
    val synopsis: String? = null,
    val year: String? = null,
    val apiEmbedUrl: String? = null
) {
    val posterUrl: String?
        get() = poster?.let { p ->
            if (p.startsWith("http://") || p.startsWith("https://")) p
            else "https://image.tmdb.org/t/p/original${if (p.startsWith("/")) p else "/$p"}"
        }

    fun watchKey(): String = "vimeus:${type.apiValue}:$id"
}

data class VimeusPage(
    val items: List<VimeusMedia>,
    val page: Int,
    val totalPages: Int,
    val hasNext: Boolean
)

data class VimeusEpisode(
    val season: Int,
    val episode: Int,
    val title: String? = null
)

data class VimeusCatalogUiState(
    val items: List<VimeusMedia> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNext: Boolean = false,
    val loadedAt: Long = 0L
)

class VimeusException(message: String, val statusCode: Int? = null) : Exception(message)

/** Builds the official embed URL, including the required view_key. */
fun VimeusMedia.embedUrl(baseUrl: String, viewKey: String, season: Int? = null, episode: Int? = null): String {
    val apiUrl = apiEmbedUrl?.takeIf { it.startsWith("http") }
    if (apiUrl != null) {
        if (season == null && episode == null) return apiUrl
        val extra = buildList {
            if (season != null && season > 0) add("se=$season")
            if (episode != null && episode > 0) add("ep=$episode")
        }.joinToString("&")
        if (extra.isBlank()) return apiUrl
        val separator = if (apiUrl.contains("?")) "&" else "?"
        return "$apiUrl$separator$extra"
    }

    val params = mutableListOf<String>()
    if (!imdbId.isNullOrBlank()) {
        params.add("imdb=${enc(imdbId)}")
    } else if (tmdbId != null) {
        params.add("tmdb=$tmdbId")
    }
    if (season != null && season > 0) params.add("se=$season")
    if (episode != null && episode > 0) params.add("ep=$episode")
    if (viewKey.isNotBlank()) params.add("view_key=${enc(viewKey)}")
    return "${baseUrl.trimEnd('/')}${type.embedPath}?${params.joinToString("&")}"
}

private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
