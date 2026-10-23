package com.example.data.nsrplay

/**
 * NSRPlay is an aggregator: every response is generated on demand from public
 * third-party providers. We only keep these objects in memory for the current
 * screen. Nothing is persisted or mirrored, as required by their API terms.
 */
enum class NsrMediaType(val apiValue: String) {
    MOVIE("movie"),
    SERIES("series");

    companion object {
        fun fromApi(value: String?): NsrMediaType =
            if (value.equals("series", ignoreCase = true) ||
                value.equals("tv", ignoreCase = true) ||
                value.equals("anime", ignoreCase = true)
            ) SERIES else MOVIE
    }
}

data class NsrMedia(
    val id: String,
    val slug: String,
    val title: String,
    val poster: String?,
    val rating: String?,
    val type: NsrMediaType,
    val provider: String?,
    val url: String?,
    val year: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val seasons: List<NsrSeason> = emptyList(),
    val servers: List<NsrServer> = emptyList()
) {
    val hasSeasons: Boolean get() = seasons.isNotEmpty()
    val posterOrNull: String? get() = poster?.takeIf { it.isNotBlank() }
}

data class NsrServer(
    val name: String,
    val server: String,
    val language: String,
    val embedUrl: String
)

data class NsrSeason(
    val number: Int,
    val name: String,
    val episodes: List<NsrEpisode>
)

data class NsrEpisode(
    val number: Int,
    val title: String,
    val season: Int
)

data class NsrCatalogPage(
    val items: List<NsrMedia>,
    val page: Int,
    val hasNextPage: Boolean
)

/** Stable key used to remember which NSRPlay titles were already opened. */
fun NsrMedia.watchKey(): String = "nsr:${type.apiValue}:$slug"
