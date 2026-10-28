package com.example.data.tmdbembed

import com.example.data.unlimplay.UnlimplayMedia
import com.example.data.unlimplay.UnlimplayType

/**
 * TMDB-id based iframe providers. Both expose a simple movie/series endpoint
 * and use the same metadata that the app already gets from TMDB.
 */
enum class TmdbEmbedSource(
    val id: String,
    val label: String,
    val baseUrl: String
) {
    FACILITOPLAY("facilitoplay", "FACILITOPLAY", "https://facilitoplay.com"),
    TUKIPLAY("tukiplay", "TUKIPLAY", "https://tukiplay.com");

    fun embedUrl(media: UnlimplayMedia, season: Int = 1, episode: Int = 1): String {
        val path = when (media.type) {
            UnlimplayType.MOVIE -> when (this) {
                FACILITOPLAY -> "/e/pelicula/${media.id}"
                TUKIPLAY -> "/embed/movie/${media.id}"
            }
            UnlimplayType.ANIME -> when (this) {
                FACILITOPLAY -> "/e/anime/${media.id}/$season/$episode"
                TUKIPLAY -> "/embed/tv/${media.id}/$season/$episode"
            }
            UnlimplayType.SERIES -> when (this) {
                FACILITOPLAY -> "/e/serie/${media.id}/$season/$episode"
                TUKIPLAY -> "/embed/tv/${media.id}/$season/$episode"
            }
        }
        return "$baseUrl$path"
    }

    fun watchKey(media: UnlimplayMedia, season: Int, episode: Int): String =
        "$id:${media.type.path}:${media.id}:$season:$episode"
}
