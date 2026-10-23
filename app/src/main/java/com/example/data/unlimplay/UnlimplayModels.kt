package com.example.data.unlimplay

import com.example.data.model.ItemType

enum class UnlimplayType(val path: String, val section: ItemType) {
    MOVIE("movie", ItemType.MOVIE),
    SERIES("series", ItemType.SERIES),
    ANIME("anime", ItemType.SERIES);

    companion object {
        fun fromMedia(mediaType: String?, genreIds: List<Int>, originalLanguage: String?): UnlimplayType {
            if (mediaType == "movie") return MOVIE
            val isAnimation = genreIds.contains(16)
            return if (isAnimation && originalLanguage == "ja") ANIME else SERIES
        }
    }
}

data class UnlimplayMedia(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val backdropPath: String? = null,
    val type: UnlimplayType,
    val year: String? = null,
    val overview: String? = null,
    val rating: Double? = null
) {
    val posterUrl: String?
        get() = posterPath?.let {
            if (it.startsWith("http")) it
            else "https://image.tmdb.org/t/p/w500${if (it.startsWith("/")) it else "/$it"}"
        }

    val backdropUrl: String?
        get() = backdropPath?.let {
            if (it.startsWith("http")) it
            else "https://image.tmdb.org/t/p/original${if (it.startsWith("/")) it else "/$it"}"
        }

    fun watchKey(): String = "unlimplay:${type.path}:$id"

    /** Mirrors the working example: movies use /movie/{id}; series use /tv/{id}/{season}/{episode}. */
    fun embedUrl(baseUrl: String, season: Int? = null, episode: Int? = null): String {
        val base = baseUrl.trimEnd('/')
        return if (type == UnlimplayType.MOVIE) {
            "$base/f/embed/movie/$id"
        } else {
            "$base/f/embed/tv/$id/${season ?: 1}/${episode ?: 1}"
        }
    }
}

data class UnlimplaySeason(
    val number: Int,
    val name: String,
    val episodeCount: Int
)

data class UnlimplayEpisode(
    val season: Int,
    val episode: Int,
    val title: String
)

data class UnlimplayPage(
    val items: List<UnlimplayMedia>,
    val page: Int,
    val totalPages: Int,
    val hasNext: Boolean
)

data class UnlimplayCatalogUiState(
    val items: List<UnlimplayMedia> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNext: Boolean = false,
    val loadedAt: Long = 0L
)

class UnlimplayException(message: String, val statusCode: Int? = null) : Exception(message)
