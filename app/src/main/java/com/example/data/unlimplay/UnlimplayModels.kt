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

    fun embedUrl(baseUrl: String): String =
        "${baseUrl.trimEnd('/')}/f/embed/${type.path}?tmdb=$id"
}

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
